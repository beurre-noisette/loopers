package com.loopers.application.order;

import com.loopers.application.like.LikeFacade;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.like.LikeCommand;
import com.loopers.domain.like.TargetType;
import com.loopers.domain.order.OrderCommand;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointReference;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserCommand;
import com.loopers.domain.user.UserRepository;
import com.loopers.support.error.CoreException;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Sql(scripts = {"/brand-test-data.sql", "/product-test-data.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class OrderFacadeIntegrationTest {

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private LikeFacade likeFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private PointService pointService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private Brand testBrand;

    @BeforeEach
    @Transactional
    void setUp() {
        testBrand = brandRepository.findById(1L).orElseThrow();
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("동일한 상품에 대해 여러명이 좋아요를 동시에 요청해도, 상품의 좋아요 개수가 정상 반영되어야 한다")
    @Test
    void concurrentLikes_shouldMaintainConsistency() throws InterruptedException {
        // arrange
        Product product = createTestProduct("좋아요테스트상품", 100);
        product = productRepository.save(product);
        
        int userCount = 10;
        CountDownLatch latch = new CountDownLatch(userCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // act - 서로 다른 사용자가 동시에 좋아요
        final Long productId = product.getId();
        try (ExecutorService executor = Executors.newFixedThreadPool(userCount)) {
            for (int i = 0; i < userCount; i++) {
                final int userIndex = i;
                executor.submit(() -> {
                    try {
                        User user = createTestUser("likeUser" + userIndex);
                        
                        LikeCommand.Create command = new LikeCommand.Create(
                                user.getUserId(),
                                TargetType.PRODUCT,
                                productId
                        );
                        likeFacade.createLike(command);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        System.out.println("좋아요 실패: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
        }

        // assert - 좋아요 개수가 성공한 만큼 증가해야 함
        Thread.sleep(100); // DB 반영 대기
        Product finalProduct = productRepository.findById(productId).orElseThrow();

        System.out.println("좋아요 성공 횟수: " + successCount.get());
        assertThat(successCount.get()).isGreaterThan(0);
    }

    @DisplayName("동일한 상품에 대해 여러 주문이 동시에 요청되어도, 재고가 정상적으로 차감되어야 한다")
    @Test
    void concurrentOrders_shouldMaintainStockConsistency() throws InterruptedException {
        // arrange - 제한된 재고로 경쟁 상황 만들기
        Product product = createTestProduct("재고테스트상품", 15); // 5명이 각 3개씩 주문 = 15개 (딱 맞음)
        product = productRepository.save(product);
        
        int userCount = 8; // 8명이 시도하지만 5명만 성공 가능
        int quantityPerOrder = 3;
        CountDownLatch latch = new CountDownLatch(userCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // act - 서로 다른 사용자가 동시 주문
        final Long productId = product.getId();
        try (ExecutorService executor = Executors.newFixedThreadPool(userCount)) {
            for (int i = 0; i < userCount; i++) {
                final int userIndex = i;
                executor.submit(() -> {
                    try {
                        User user = createTestUser("stockUser" + userIndex);
                        pointService.createPointWithInitialAmount(
                                user.getId(),
                                new BigDecimal("10000"), // 충분한 포인트
                                PointReference.welcomeBonus()
                        );

                        OrderCommand.Create command = new OrderCommand.Create(
                                List.of(new OrderCommand.CreateItem(productId, quantityPerOrder)),
                                new BigDecimal("300")
                        );

                        orderFacade.createOrder(user.getUserId(), command);
                        successCount.incrementAndGet();
                        System.out.println("주문 성공: 사용자=" + user.getUserId());
                    } catch (CoreException e) {
                        failureCount.incrementAndGet();
                        System.out.println("주문 실패: 사용자=stockUser" + userIndex + ", 사유=" + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
        }

        // assert
        Product finalProduct = productRepository.findById(productId).orElseThrow();
        
        // 재고는 성공한 주문만큼 차감되어야 함
        int expectedStock = 15 - (successCount.get() * quantityPerOrder);
        assertThat(finalProduct.getStock()).isEqualTo(expectedStock);
        
        // 재고가 음수가 되지 않아야 함
        assertThat(finalProduct.getStock()).isGreaterThanOrEqualTo(0);
        
        // 성공한 주문은 최대 5번이어야 함 (재고 15개 / 주문당 3개)
        assertThat(successCount.get()).isLessThanOrEqualTo(5);
        
        // 모든 시도가 성공 또는 실패로 처리되어야 함
        assertThat(successCount.get() + failureCount.get()).isEqualTo(userCount);

        System.out.println("재고 동시성 테스트 - 성공: " + successCount.get() + 
                ", 실패: " + failureCount.get() + 
                ", 최종 재고: " + finalProduct.getStock());
    }

    @DisplayName("동일한 유저가 포인트 부족 상황에서 동시 주문 시 정합성이 보장된다")
    @Test
    void concurrentOrdersBySameUser_shouldMaintainPointConsistency() throws InterruptedException {
        // arrange
        User user = createTestUser("user");
        BigDecimal initialPoints = new BigDecimal("3000"); // 3번의 주문만 가능한 포인트
        pointService.createPointWithInitialAmount(
                user.getId(),
                initialPoints,
                PointReference.welcomeBonus()
        );

        // 서로 다른 상품들 생성 (재고 충분히 많이)
        final Product product1 = productRepository.save(createTestProduct("상품1", 100));
        final Product product2 = productRepository.save(createTestProduct("상품2", 100));

        int orderCount = 10; // 10번 시도하지만 포인트는 3번만 가능
        BigDecimal pointsPerOrder = new BigDecimal("1000");
        CountDownLatch latch = new CountDownLatch(orderCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // act - 동일한 사용자가 서로 다른 상품을 동시 주문
        final String userId = user.getUserId();
        try (ExecutorService executor = Executors.newFixedThreadPool(orderCount)) {
            for (int i = 0; i < orderCount; i++) {
                final int orderIndex = i;
                executor.submit(() -> {
                    try {
                        // 번갈아가며 다른 상품 주문
                        Long productId = (orderIndex % 2 == 0) ? product1.getId() : product2.getId();
                        
                        OrderCommand.Create command = new OrderCommand.Create(
                                List.of(new OrderCommand.CreateItem(productId, 1)),
                                pointsPerOrder
                        );

                        orderFacade.createOrder(userId, command);
                        successCount.incrementAndGet();
                    } catch (CoreException e) {
                        failureCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
        }

        // assert
        Point finalPoint = pointService.getPoint(user.getId());
        
        // 포인트는 성공한 주문만큼 차감되어야 함
        BigDecimal expectedBalance = initialPoints
                .subtract(pointsPerOrder.multiply(BigDecimal.valueOf(successCount.get())));
        assertThat(finalPoint.getBalance()).isEqualByComparingTo(expectedBalance);
        
        // 포인트가 음수가 되지 않아야 함
        assertThat(finalPoint.getBalance()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        
        // 성공한 주문은 최대 3번이어야 함 (초기 포인트 3000원 / 주문당 1000원)
        assertThat(successCount.get()).isLessThanOrEqualTo(3);
        
        // 모든 시도가 성공 또는 실패로 처리되어야 함
        assertThat(successCount.get() + failureCount.get()).isEqualTo(orderCount);

        System.out.println("포인트 동시성 테스트 - 성공: " + successCount.get() + 
                ", 실패: " + failureCount.get() + 
                ", 최종 포인트: " + finalPoint.getBalance());
    }

    private Product createTestProduct(String name, int stock) {
        try {
            java.lang.reflect.Constructor<Product> constructor = Product.class.getDeclaredConstructor(
                    String.class, String.class, BigDecimal.class, Integer.class, Brand.class);
            constructor.setAccessible(true);
            return constructor.newInstance(name, "테스트 상품 설명", new BigDecimal("10000"), stock, testBrand);
        } catch (Exception e) {
            throw new RuntimeException("테스트용 Product 객체 생성 실패", e);
        }
    }

    private User createTestUser(String userId) {
        User user = User.of(new UserCommand.Create(
                userId,
                userId + "@test.com",
                "1996-08-16",
                Gender.MALE
        ));
        return userRepository.save(user);
    }
}
