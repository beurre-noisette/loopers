package com.loopers.domain.coupon;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserCouponTest {

    @DisplayName("쿠폰 발급 시")
    @Nested
    class Issue {

        @DisplayName("유효한 사용자 ID의 쿠폰으로 발급 시 성공한다.")
        @Test
        void issueCoupon_whenValidUserIdAndCoupon() {
            // arrange
            Long userId = 1L;
            Coupon coupon = createValidCoupon();

            // act
            UserCoupon userCoupon = UserCoupon.issue(userId, coupon);

            // assert
            assertThat(userCoupon.getUserId()).isEqualTo(userId);
            assertThat(userCoupon.getCoupon()).isEqualTo(coupon);
            assertThat(userCoupon.isUsed()).isFalse();
            assertThat(userCoupon.getUsedAt()).isNull();
            assertThat(userCoupon.getOrderId()).isNull();
        }

        @DisplayName("사용자 ID가 null이면 BAD REQUEST 예외가 발생한다")
        @Test
        void throwBadRequestException_whenUserIdIsNull() {
            // arrange
            Coupon coupon = createValidCoupon();

            // act
            CoreException exception = assertThrows(CoreException.class, () ->
                    UserCoupon.issue(null, coupon)
            );

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("사용자 ID가 0 이하면 BAD REQUEST 예외가 발생한다")
        @Test
        void throwBadRequestException_whenUserIdIsZeroOrNegative() {
            // arrange
            Coupon coupon = createValidCoupon();

            // act
            CoreException exception = assertThrows(CoreException.class, () ->
                    UserCoupon.issue(0L, coupon)
            );

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("쿠폰이 null이면 BAD REQUEST 예외가 발생한다")
        @Test
        void throwBadRequestException_whenCouponIsNull() {
            // act
            CoreException exception = assertThrows(CoreException.class, () ->
                    UserCoupon.issue(1L, null)
            );

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("쿠폰 사용 시")
    @Nested
    class Use {

        @DisplayName("미사용 쿠폰을 사용하면 성공한다")
        @Test
        void useCoupon_whenUnusedCoupon() {
            // arrange
            Coupon coupon = createValidCoupon();
            UserCoupon userCoupon = UserCoupon.issue(1L, coupon);
            Long orderId = 100L;

            // act
            userCoupon.use(orderId);

            // assert
            assertThat(userCoupon.isUsed()).isTrue();
            assertThat(userCoupon.getUsedAt()).isNotNull();
            assertThat(userCoupon.getOrderId()).isEqualTo(orderId);
        }

        @DisplayName("이미 사용된 쿠폰을 재사용하면 BAD REQUEST 예외가 발생한다")
        @Test
        void throwBadRequestException_whenAlreadyUsedCoupon() {
            // arrange
            Coupon coupon = createValidCoupon();
            UserCoupon userCoupon = UserCoupon.issue(1L, coupon);
            userCoupon.use(100L); // 먼저 사용

            // act
            CoreException exception = assertThrows(CoreException.class, () ->
                    userCoupon.use(200L) // 재사용 시도
            );

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(userCoupon.isUsed()).isTrue();
        }

        @DisplayName("만료된 쿠폰을 사용하면 BAD REQUEST 예외가 발생한다")
        @Test
        void throwBadRequestException_whenExpiredCoupon() {
            // arrange
            Coupon expiredCoupon = createExpiredCoupon();
            UserCoupon userCoupon = UserCoupon.issue(1L, expiredCoupon);

            // act
            CoreException exception = assertThrows(CoreException.class, () ->
                    userCoupon.use(100L)
            );

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("사용 가능 여부 확인 시")
    @Nested
    class CanUse {

        @DisplayName("미사용이고 유효기간 내 쿠폰은 사용 가능하다")
        @Test
        void canUse_whenUnusedAndValidCoupon() {
            // arrange
            Coupon coupon = createValidCoupon();
            UserCoupon userCoupon = UserCoupon.issue(1L, coupon);

            // act
            boolean canUse = userCoupon.canUse();

            // assert
            assertThat(canUse).isTrue();
        }

        @DisplayName("이미 사용된 쿠폰은 사용 불가능하다")
        @Test
        void cannotUse_whenAlreadyUsedCoupon() {
            // arrange
            Coupon coupon = createValidCoupon();
            UserCoupon userCoupon = UserCoupon.issue(1L, coupon);
            userCoupon.use(100L);

            // act
            boolean canUse = userCoupon.canUse();

            // assert
            assertThat(canUse).isFalse();
        }

        @DisplayName("만료된 쿠폰은 사용 불가능하다")
        @Test
        void cannotUse_whenExpiredCoupon() {
            // arrange
            Coupon expiredCoupon = createExpiredCoupon();
            UserCoupon userCoupon = UserCoupon.issue(1L, expiredCoupon);

            // act
            boolean canUse = userCoupon.canUse();

            // assert
            assertThat(canUse).isFalse();
        }
    }

    private Coupon createValidCoupon() {
        return Coupon.createFixedAmount(
                "유효한 쿠폰",
                new BigDecimal("5000"),
                new BigDecimal("10000"),
                ZonedDateTime.now().minusDays(1),
                ZonedDateTime.now().plusDays(30)
        );
    }

    private Coupon createExpiredCoupon() {
        return Coupon.createFixedAmount(
                "만료된 쿠폰",
                new BigDecimal("5000"),
                new BigDecimal("10000"),
                ZonedDateTime.now().minusDays(10),
                ZonedDateTime.now().minusDays(1)
        );
    }

}
