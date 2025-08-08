package com.loopers.domain.product;

import com.loopers.application.order.OrderFacade;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.order.OrderCommand;
import com.loopers.domain.point.PointReference;
import com.loopers.domain.point.PointService;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserCommand;
import com.loopers.domain.user.UserRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Sql(scripts = {"/brand-test-data.sql", "/product-test-data.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class DeadlockPreventionTest {

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

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("다중 상품 주문 시 Product ID 정렬로 데드락을 방지한다.")
    @Test
    void preventDeadlock_byOrderingProductIds_inMultipleProductOrders() throws Exception {
        // arrange
        Brand brand = brandRepository.findById(1L).orElseThrow();
        
        User userA = createTestUser("userA", "userA@example.com", Gender.MALE);
        User userB = createTestUser("userB", "userB@example.com", Gender.FEMALE);

        Product product1 = createTestProduct(brand, "상품1", 50);
        Product product2 = createTestProduct(brand, "상품2", 50);
        Product product3 = createTestProduct(brand, "상품3", 50);
        
        product1 = productRepository.save(product1);
        product2 = productRepository.save(product2);
        product3 = productRepository.save(product3);

        // 각 사용자에게 충분한 포인트 충전
        pointService.createPointWithInitialAmount(userA.getId(), new BigDecimal("100000"), PointReference.welcomeBonus());
        pointService.createPointWithInitialAmount(userB.getId(), new BigDecimal("100000"), PointReference.welcomeBonus());

        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // act - 동시에 여러 상품을 다른 순서로 주문 시도
        final User finalUserA = userA;
        final User finalUserB = userB;
        final Product finalProduct1 = product1;
        final Product finalProduct2 = product2;
        final Product finalProduct3 = product3;
        
        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            for (int i = 0; i < threadCount; i++) {
                final int threadIndex = i;
                executor.submit(() -> {
                    try {
                        OrderCommand.Create command;
                        User user = (threadIndex % 2 == 0) ? finalUserA : finalUserB;
                        
                        if (threadIndex % 2 == 0) {
                            // 짝수 스레드: product1 → product2 → product3 순서
                            command = new OrderCommand.Create(
                                    List.of(
                                            new OrderCommand.CreateItem(finalProduct1.getId(), 1),
                                            new OrderCommand.CreateItem(finalProduct2.getId(), 1),
                                            new OrderCommand.CreateItem(finalProduct3.getId(), 1)
                                    ),
                                    BigDecimal.ZERO,
                                    null
                            );
                        } else {
                            // 홀수 스레드: product3 → product1 → product2 순서 (역순)
                            command = new OrderCommand.Create(
                                    List.of(
                                            new OrderCommand.CreateItem(finalProduct3.getId(), 1),
                                            new OrderCommand.CreateItem(finalProduct1.getId(), 1),
                                            new OrderCommand.CreateItem(finalProduct2.getId(), 1)
                                    ),
                                    BigDecimal.ZERO,
                                    null
                            );
                        }
                        
                        orderFacade.createOrder(user.getUserId(), command);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean completed = latch.await(10, TimeUnit.SECONDS);
            
            // assert
            assertThat(completed).isTrue(); // 타임아웃 없이 완료
            assertThat(successCount.get() + failureCount.get()).isEqualTo(threadCount);
            

            Product updatedProduct1 = productRepository.findById(finalProduct1.getId()).orElseThrow();
            Product updatedProduct2 = productRepository.findById(finalProduct2.getId()).orElseThrow();
            Product updatedProduct3 = productRepository.findById(finalProduct3.getId()).orElseThrow();
            
            assertThat(updatedProduct1.getStock()).isGreaterThanOrEqualTo(0);
            assertThat(updatedProduct2.getStock()).isGreaterThanOrEqualTo(0);
            assertThat(updatedProduct3.getStock()).isGreaterThanOrEqualTo(0);
            
            assertThat(updatedProduct1.getStock()).isLessThanOrEqualTo(50);
            assertThat(updatedProduct2.getStock()).isLessThanOrEqualTo(50);
            assertThat(updatedProduct3.getStock()).isLessThanOrEqualTo(50);
        }
    }

    private User createTestUser(String userId, String email, Gender gender) {
        User user = User.of(new UserCommand.Create(userId, email, "1990-01-01", gender));
        return userRepository.save(user);
    }

    private Product createTestProduct(Brand brand, String name, int stock) {
        try {
            Constructor<Product> constructor = Product.class.getDeclaredConstructor(
                    String.class, String.class, BigDecimal.class, Integer.class, Brand.class);
            constructor.setAccessible(true);
            return constructor.newInstance(name, "테스트 상품 설명", new BigDecimal("10"), stock, brand);
        } catch (Exception e) {
            throw new RuntimeException("테스트용 Product 객체 생성 실패", e);
        }
    }
}
