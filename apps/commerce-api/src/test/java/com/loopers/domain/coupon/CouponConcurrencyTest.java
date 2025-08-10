package com.loopers.domain.coupon;

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
import java.time.ZonedDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class CouponConcurrencyTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private UserCoupon testUserCoupon;

    @BeforeEach
    @Transactional
    void setUp() {
        Coupon coupon = Coupon.createFixedAmount(
                "동시성 테스트 쿠폰",
                new BigDecimal("5000"),
                new BigDecimal("10000"),
                ZonedDateTime.now().minusDays(1),
                ZonedDateTime.now().plusDays(30)
        );
        Coupon savedCoupon = couponRepository.save(coupon);

        // 사용자에게 쿠폰 발급
        testUserCoupon = couponService.issueCoupon(999L, savedCoupon.getId());
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("동일한 쿠폰을 여러 쓰레드에서 동시에 사용해도 단 한번만 사용된다.")
    @Test
    void concurrentCouponUse_shouldAllowOnlyOneSuccess() throws InterruptedException {
        // arrange
        int threadCount = 100;
        BigDecimal orderAmount = new BigDecimal("50000");

        CountDownLatch countDownLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // act
        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            for (int i = 0; i < threadCount; i++) {
                final Long orderId = (long) (i + 1);
                executor.submit(() -> {
                    try {
                        couponService.useCouponAndCalculateDiscount(
                                testUserCoupon.getId(),
                                orderId,
                                orderAmount
                        );
                        successCount.incrementAndGet();
                    } catch (CoreException e) {
                        if (e.getErrorType() == ErrorType.BAD_REQUEST) {
                            failureCount.incrementAndGet();
                        }
                    } finally {
                        countDownLatch.countDown();
                    }
                });
            }
        }

        countDownLatch.await();

        // assert
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(threadCount - 1);
        assertThat(successCount.get() + failureCount.get()).isEqualTo(threadCount);

        UserCoupon finalUserCoupon = couponRepository.findUserCouponById(testUserCoupon.getId()).orElseThrow();
        assertThat(finalUserCoupon.isUsed()).isTrue();
        assertThat(finalUserCoupon.getUsedAt()).isNotNull();
        assertThat(finalUserCoupon.getOrderId()).isNotNull();
    }

    @DisplayName("서로 다른 쿠폰을 동시에 사용하면 모두 성공한다.")
    @Test
    void concurrentDifferentCouponUse_shouldAllSucceed() throws InterruptedException {
        // arrange
        int threadCount = 5;
        BigDecimal orderAmount = new BigDecimal("50000");

        UserCoupon[] userCoupons = new UserCoupon[threadCount];
        for (int i = 0; i < threadCount; i++) {
            Coupon coupon = Coupon.createFixedAmount(
                    "쿠폰" + i,
                    new BigDecimal("3000"),
                    new BigDecimal("10000"),
                    ZonedDateTime.now().minusDays(1),
                    ZonedDateTime.now().plusDays(30)
            );
            Coupon savedCoupon = couponRepository.save(coupon);
            userCoupons[i] = couponService.issueCoupon((long) (i + 1), savedCoupon.getId());
        }


        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // act
        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                executor.submit(() -> {
                    try {
                        couponService.useCouponAndCalculateDiscount(
                                userCoupons[index].getId(),
                                (long)(index + 100),
                                orderAmount
                        );
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
        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(failureCount.get()).isEqualTo(0);

        for (UserCoupon userCoupon : userCoupons) {
            UserCoupon finalUserCoupon = couponRepository.findUserCouponById(userCoupon.getId()).orElseThrow();
            assertThat(finalUserCoupon.isUsed()).isTrue();
        }
    }
}
