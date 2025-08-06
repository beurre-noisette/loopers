package com.loopers.domain.point;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PointConcurrencyTest {

    @Autowired
    private PointService pointService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    private Long testUserId;
    private BigDecimal initialBalance;

    @BeforeEach
    @Transactional
    void setUp() {
        testUserId = 999L;
        initialBalance = new BigDecimal("10000");

        Point point = pointService.createPointWithInitialAmount(
                testUserId, 
                initialBalance, 
                PointReference.welcomeBonus()
        );
    }

    @DisplayName("동일 사용자가 여러 기기에서 동시에 포인트를 사용해도 정합성이 보장된다")
    @Test
    void concurrentPointUse_shouldMaintainConsistency() throws InterruptedException {
        // arrange
        int threadCount = 10;
        BigDecimal amountPerThread = new BigDecimal("500"); // 각 스레드당 500원 사용

        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // act - 동시에 포인트 사용 시도
        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        pointService.usePoint(
                                testUserId,
                                amountPerThread,
                                PointReference.order(Thread.currentThread().threadId())
                        );
                        successCount.incrementAndGet();
                    } catch (CoreException e) {
                        if (e.getErrorType() == ErrorType.NOT_ENOUGH) {
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
        Point finalPoint = pointService.getPoint(testUserId);
        BigDecimal expectedBalance = initialBalance.subtract(
                amountPerThread.multiply(BigDecimal.valueOf(successCount.get()))
        );
        
        assertThat(finalPoint.getBalance()).isEqualByComparingTo(expectedBalance);
        assertThat(successCount.get() + failureCount.get()).isEqualTo(threadCount);
        assertThat(finalPoint.getBalance()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        
        System.out.println("성공: " + successCount.get() + ", 실패: " + failureCount.get() + 
                          ", 최종 잔액: " + finalPoint.getBalance());
    }

    @DisplayName("잔액보다 많은 동시 사용 요청 시 일부는 실패하고 잔액은 0이 된다")
    @Test
    void concurrentPointUse_withInsufficientBalance_shouldFailSomeAndMaintainZeroBalance() throws InterruptedException {
        // arrange
        int threadCount = 25;
        BigDecimal amountPerThread = new BigDecimal("1000"); // 각 스레드당 1000원 사용
        // 총 25000원 요청하지만 잔액은 10000원만 있음
        
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // act
        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        pointService.usePoint(
                                testUserId,
                                amountPerThread,
                                PointReference.order(Thread.currentThread().threadId())
                        );
                        successCount.incrementAndGet();
                    } catch (CoreException e) {
                        if (e.getErrorType() == ErrorType.NOT_ENOUGH) {
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
        Point finalPoint = pointService.getPoint(testUserId);
        
        // 잔액은 0원이 되어야 함
        assertThat(finalPoint.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        
        // 10000원 잔액으로 1000원씩 10번만 성공 가능
        assertThat(successCount.get()).isEqualTo(10);
        assertThat(failureCount.get()).isEqualTo(15);
        
        System.out.println("성공: " + successCount.get() + ", 실패: " + failureCount.get() + 
                          ", 최종 잔액: " + finalPoint.getBalance());
    }
}
