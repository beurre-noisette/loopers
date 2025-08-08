package com.loopers.domain.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderItems;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Sql(scripts = {"/brand-test-data.sql", "/product-test-data.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class StockConcurrencyTest {

    @Autowired
    private StockManagementService stockManagementService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("동시에 여러 스레드가 재고를 차감해도 정합성이 보장된다")
    @Test
    void concurrentStockDecrease_shouldMaintainConsistency() throws InterruptedException {
        // arrange
        int threadCount = 10;
        int quantityPerThread = 1;
        int initialStock = 100;

        Brand brand = brandRepository.findById(1L).orElseThrow();
        Product product = createTestProduct(brand, "테스트상품", initialStock);
        product = productRepository.save(product);

        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // act - 동시에 재고 차감 시도
        final Long productId = product.getId();
        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        List<Product> products = List.of(productRepository.findById(productId).orElseThrow());
                        OrderItem orderItem = new OrderItem(productId, quantityPerThread, products.get(0).getPrice());
                        OrderItems orderItems = OrderItems.from(List.of(orderItem));

                        stockManagementService.decreaseStock(products, orderItems);
                        successCount.incrementAndGet();
                    } catch (CoreException e) {
                        if (e.getErrorType() == ErrorType.INVALID_INPUT_FORMAT) {
                            failureCount.incrementAndGet();
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
        }

        // assert
        Product finalProduct = productRepository.findById(product.getId()).orElseThrow();
        int expectedStock = initialStock - (successCount.get() * quantityPerThread);

        assertThat(finalProduct.getStock()).isEqualTo(expectedStock);
        assertThat(successCount.get() + failureCount.get()).isEqualTo(threadCount);
        System.out.println("성공: " + successCount.get() + ", 실패: " + failureCount.get() + ", 최종 재고: " + finalProduct.getStock());
    }

    @DisplayName("재고보다 많은 동시 주문 시 일부는 실패하고 재고는 0이 된다")
    @Test
    void concurrentStockDecrease_withInsufficientStock_shouldFailSomeAndMaintainZeroStock() throws InterruptedException {
        // arrange
        int threadCount = 20;
        int quantityPerThread = 1;
        int initialStock = 10; // 부족한 재고

        Brand brand = brandRepository.findById(1L).orElseThrow();
        Product product = createTestProduct(brand, "부족재고상품", initialStock);
        product = productRepository.save(product);

        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // act
        final Long productId = product.getId();
        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        List<Product> products = List.of(productRepository.findById(productId).orElseThrow());
                        OrderItem orderItem = new OrderItem(productId, quantityPerThread, products.get(0).getPrice());
                        OrderItems orderItems = OrderItems.from(List.of(orderItem));

                        stockManagementService.decreaseStock(products, orderItems);
                        successCount.incrementAndGet();
                    } catch (CoreException e) {
                        if (e.getErrorType() == ErrorType.INVALID_INPUT_FORMAT) {
                            failureCount.incrementAndGet();
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
        }

        // assert
        Product finalProduct = productRepository.findById(product.getId()).orElseThrow();

        assertThat(successCount.get()).isEqualTo(initialStock);
        assertThat(failureCount.get()).isEqualTo(threadCount - initialStock);
        assertThat(finalProduct.getStock()).isEqualTo(0);

        System.out.println("성공: " + successCount.get() + ", 실패: " + failureCount.get() + ", 최종 재고: " + finalProduct.getStock());
    }

    private Product createTestProduct(Brand brand, String name, int stock) {
        try {
            Constructor<Product> constructor = Product.class.getDeclaredConstructor(
                    String.class, String.class, BigDecimal.class, Integer.class, Brand.class);
            constructor.setAccessible(true);
            return constructor.newInstance(name, "테스트 상품 설명", new BigDecimal("10000"), stock, brand);
        } catch (Exception e) {
            throw new RuntimeException("테스트용 Product 객체 생성 실패", e);
        }
    }
}
