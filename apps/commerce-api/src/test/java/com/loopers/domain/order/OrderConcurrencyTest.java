package com.loopers.domain.order;

import com.loopers.application.order.OrderFacade;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.point.PointReference;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserCommand;
import com.loopers.domain.user.UserRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Constructor;
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
public class OrderConcurrencyTest {
    @Autowired
    private OrderFacade orderFacade;

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
    private User testUser;
    private Product productA;
    private Product productB;
    private Product productC;

    @BeforeEach
    @Transactional
    void setUp() {
        testBrand = brandRepository.findById(1L).orElseThrow();

        testUser = User.of(new UserCommand.Create(
                "testUser",
                "test@example.com",
                "1996-08-16",
                Gender.MALE
        ));
        testUser = userRepository.save(testUser);

        productA = createTestProduct("상품A");
        productA = productRepository.save(productA);

        productB = createTestProduct("상품B");
        productB = productRepository.save(productB);

        productC = createTestProduct("상품C");
        productC = productRepository.save(productC);

        pointService.createPointWithInitialAmount(
                testUser.getId(),
                new BigDecimal("500000"), // 50만원 충전
                PointReference.welcomeBonus()
        );
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("동일한 유저가 서로 다른 상품을 동시에 주문해도 포인트가 정상적으로 차감된다")
    @Test
    void concurrentOrdersByDifferentProducts_shouldDeductPointsCorrectly() throws InterruptedException {
        // arrange
        int threadCount = 3;
        BigDecimal initialBalance = pointService.getPoint(testUser.getId()).getBalance();

        // 각 주문의 예상 비용 계산
        BigDecimal expectedCostA = productA.getPrice().multiply(BigDecimal.valueOf(2)); // 상품A 2개
        BigDecimal expectedCostB = productB.getPrice().multiply(BigDecimal.valueOf(1)); // 상품B 1개
        BigDecimal expectedCostC = productC.getPrice().multiply(BigDecimal.valueOf(3)); // 상품C 3개
        BigDecimal totalExpectedCost = expectedCostA.add(expectedCostB).add(expectedCostC);

        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // act - 동시에 서로 다른 상품 주문
        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            // 스레드 1: 상품A 주문
            executor.submit(() -> {
                try {
                    OrderCommand.Create commandA = new OrderCommand.Create(
                            List.of(new OrderCommand.CreateItem(productA.getId(), 2)),
                            BigDecimal.ZERO,
                            null
                    );
                    orderFacade.createOrder(testUser.getUserId(), commandA);
                    successCount.incrementAndGet();
                } catch (CoreException e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });

            // 스레드 2: 상품B 주문
            executor.submit(() -> {
                try {
                    OrderCommand.Create commandB = new OrderCommand.Create(
                            List.of(new OrderCommand.CreateItem(productB.getId(), 1)),
                            BigDecimal.ZERO,
                            null
                    );
                    orderFacade.createOrder(testUser.getUserId(), commandB);
                    successCount.incrementAndGet();
                } catch (CoreException e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });

            // 스레드 3: 상품C 주문
            executor.submit(() -> {
                try {
                    OrderCommand.Create commandC = new OrderCommand.Create(
                            List.of(new OrderCommand.CreateItem(productC.getId(), 3)),
                            BigDecimal.ZERO,
                            null
                    );
                    orderFacade.createOrder(testUser.getUserId(), commandC);
                    successCount.incrementAndGet();
                } catch (CoreException e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });

            latch.await();
        }

        // assert
        assertThat(successCount.get()).isEqualTo(3);
        assertThat(failureCount.get()).isEqualTo(0);

        // 포인트 차감 검증
        BigDecimal finalBalance = pointService.getPoint(testUser.getId()).getBalance();
        BigDecimal expectedRemainingBalance = initialBalance.subtract(totalExpectedCost);

        assertThat(finalBalance).isEqualByComparingTo(expectedRemainingBalance);

        // 재고 차감 검증
        Product finalProductA = productRepository.findById(productA.getId()).orElseThrow();
        Product finalProductB = productRepository.findById(productB.getId()).orElseThrow();
        Product finalProductC = productRepository.findById(productC.getId()).orElseThrow();

        assertThat(finalProductA.getStock()).isEqualTo(98); // 100 - 2
        assertThat(finalProductB.getStock()).isEqualTo(99); // 100 - 1
        assertThat(finalProductC.getStock()).isEqualTo(97); // 100 - 3
    }

    @DisplayName("동일한 유저가 포인트 잔액을 초과하는 주문들을 동시에 시도하면 일부만 성공한다")
    @Test
    void concurrentOrdersExceedingBalance_shouldPartiallySucceed() throws InterruptedException {
        // arrange - 포인트를 적게 충전 (3만원만)
        User poorUser = User.of(new UserCommand.Create(
                "poorUser",
                "poor@example.com",
                "1995-01-01",
                Gender.FEMALE
        ));
        poorUser = userRepository.save(poorUser);

        pointService.createPointWithInitialAmount(
                poorUser.getId(),
                new BigDecimal("30000"), // 3만원만 충전
                PointReference.welcomeBonus()
        );

        int threadCount = 4; // 4개 주문 시도 (각각 1만원씩, 총 4만원 > 3만원)
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // act - 동시에 여러 주문 시도 (잔액 초과)
        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            for (int i = 0; i < threadCount; i++) {
                User finalPoorUser = poorUser;
                executor.submit(() -> {
                    try {
                        // 각 주문은 1만원씩 (상품 1개)
                        OrderCommand.Create command = new OrderCommand.Create(
                                List.of(new OrderCommand.CreateItem(productA.getId(), 1)),
                                BigDecimal.ZERO,
                                null
                        );
                        orderFacade.createOrder(finalPoorUser.getUserId(), command);
                        successCount.incrementAndGet();
                    } catch (CoreException e) {
                        if (e.getErrorType() == ErrorType.PAYMENT_INSUFFICIENT_POINT) {
                            failureCount.incrementAndGet();
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
        }

        // assert - 3개는 성공, 1개는 실패해야 함
        assertThat(successCount.get()).isEqualTo(3); // 3만원 / 1만원 = 3개 성공
        assertThat(failureCount.get()).isEqualTo(1); // 1개 실패
        assertThat(successCount.get() + failureCount.get()).isEqualTo(threadCount);

        // 포인트가 모두 소진되었는지 확인
        BigDecimal finalBalance = pointService.getPoint(poorUser.getId()).getBalance();
        assertThat(finalBalance).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private Product createTestProduct(String name) {
        try {
            Constructor<Product> constructor = Product.class.getDeclaredConstructor(
                    String.class, String.class, BigDecimal.class, Integer.class, Brand.class);
            constructor.setAccessible(true);
            return constructor.newInstance(name, "테스트 상품 설명", new BigDecimal("10000"), 100, testBrand);
        } catch (Exception e) {
            throw new RuntimeException("테스트용 Product 객체 생성 실패", e);
        }
    }
}
