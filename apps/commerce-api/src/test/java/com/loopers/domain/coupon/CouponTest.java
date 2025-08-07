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

class CouponTest {

    @DisplayName("정액 할인 쿠폰 생성 시")
    @Nested
    class FixedAmountCouponCreate {

        @DisplayName("유효한 정액 할인 쿠폰을 생성한다")
        @Test
        void createFixedAmountCoupon_whenValidInput() {
            // arrange
            String name = "5000원 할인 쿠폰";
            BigDecimal discountAmount = new BigDecimal("5000");
            BigDecimal minOrderAmount = new BigDecimal("30000");
            ZonedDateTime validFrom = ZonedDateTime.now();
            ZonedDateTime validUntil = ZonedDateTime.now().plusDays(30);

            // act
            Coupon coupon = Coupon.createFixedAmount(name, discountAmount, minOrderAmount, validFrom, validUntil);

            // assert
            assertThat(coupon.getName()).isEqualTo(name);
            assertThat(coupon.getType()).isEqualTo(CouponType.FIXED_AMOUNT);
            assertThat(coupon.getDiscountValue()).isEqualByComparingTo(discountAmount);
            assertThat(coupon.getMinOrderAmount()).isEqualByComparingTo(minOrderAmount);
            assertThat(coupon.getMaxDiscountAmount()).isNull();
        }

        @DisplayName("할인 금액이 0 이하면 BAD REQUEST 예외가 발생한다")
        @Test
        void throwBadRequestException_whenDiscountAmountIsZeroOrNegative() {
            // arrange
            BigDecimal invalidAmount = BigDecimal.ZERO;

            // act
            CoreException exception = assertThrows(CoreException.class, () ->
                    Coupon.createFixedAmount(
                            "쿠폰",
                            invalidAmount,
                            new BigDecimal("10000"),
                            ZonedDateTime.now(),
                            ZonedDateTime.now().plusDays(1))
            );

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("정률 할인 쿠폰 생성 시")
    @Nested
    class FixedRateCouponCreate {

        @DisplayName("유효한 정률 할인 쿠폰을 생성한다")
        @Test
        void createFixedRateCoupon_whenValidInput() {
            // arrange
            String name = "20% 할인 쿠폰";
            BigDecimal discountRate = new BigDecimal("20");
            BigDecimal minOrderAmount = new BigDecimal("50000");
            BigDecimal maxDiscountAmount = new BigDecimal("10000");
            ZonedDateTime validFrom = ZonedDateTime.now();
            ZonedDateTime validUntil = ZonedDateTime.now().plusDays(30);

            // act
            Coupon coupon = Coupon.createFixedRate(name, discountRate, minOrderAmount, maxDiscountAmount, validFrom, validUntil);

            // assert
            assertThat(coupon.getName()).isEqualTo(name);
            assertThat(coupon.getType()).isEqualTo(CouponType.FIXED_RATE);
            assertThat(coupon.getDiscountValue()).isEqualByComparingTo(discountRate);
            assertThat(coupon.getMinOrderAmount()).isEqualByComparingTo(minOrderAmount);
            assertThat(coupon.getMaxDiscountAmount()).isEqualByComparingTo(maxDiscountAmount);
        }

        @DisplayName("할인율이 100을 초과하면 BAD REQUEST 예외가 발생한다")
        @Test
        void throwBadRequestException_whenDiscountRateExceeds100() {
            // act
            CoreException exception = assertThrows(CoreException.class, () ->
                    Coupon.createFixedRate(
                            "쿠폰",
                            new BigDecimal("150"),
                            new BigDecimal("10000"),
                            new BigDecimal("5000"),
                            ZonedDateTime.now(),
                            ZonedDateTime.now().plusDays(1))
            );

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("할인 금액 계산 시")
    @Nested
    class DiscountCalculate {

        @DisplayName("정액 쿠폰의 할인 금액을 올바르게 계산한다")
        @Test
        void calculateDiscountAmount_whenFixedAmountCoupon() {
            // arrange
            Coupon coupon = Coupon.createFixedAmount(
                    "5000원 할인",
                    new BigDecimal("5000"),
                    new BigDecimal("30000"),
                    ZonedDateTime.now().minusDays(1),
                    ZonedDateTime.now().plusDays(1));
            BigDecimal orderAmount = new BigDecimal("50000");

            // act
            BigDecimal discount = coupon.calculateDiscountAmount(orderAmount);

            // assert
            assertThat(discount).isEqualByComparingTo(new BigDecimal("5000"));
        }

        @DisplayName("정률 쿠폰의 할인 금액을 올바르게 계산한다")
        @Test
        void calculateDiscountAmount_whenFixedRateCoupon() {
            // arrange
            Coupon coupon = Coupon.createFixedRate(
                    "20% 할인",
                    new BigDecimal("20"),
                    new BigDecimal("10000"),
                    new BigDecimal("15000"),
                    ZonedDateTime.now().minusDays(1),
                    ZonedDateTime.now().plusDays(1));
            BigDecimal orderAmount = new BigDecimal("50000");

            // act
            BigDecimal discount = coupon.calculateDiscountAmount(orderAmount);

            // assert (50000 * 20% = 10000)
            assertThat(discount).isEqualByComparingTo(new BigDecimal("10000.00"));
        }

        @DisplayName("정률 쿠폰이 최대 할인 한도를 초과하면 한도 금액을 반환한다")
        @Test
        void calculateDiscountAmount_whenExceedsMaxDiscountAmount() {
            // arrange
            Coupon coupon = Coupon.createFixedRate(
                    "20% 할인",
                    new BigDecimal("20"),
                    new BigDecimal("10000"),
                    new BigDecimal("5000"), // 최대 5000원
                    ZonedDateTime.now().minusDays(1),
                    ZonedDateTime.now().plusDays(1));
            BigDecimal orderAmount = new BigDecimal("50000");

            // act
            BigDecimal discount = coupon.calculateDiscountAmount(orderAmount);

            // assert (20%면 10000원이지만 최대 5000원으로 제한)
            assertThat(discount).isEqualByComparingTo(new BigDecimal("5000"));
        }

        @DisplayName("최소 주문 금액 미달 시 BAD REQUEST 예외가 발생한다")
        @Test
        void throwBadRequestException_whenOrderAmountBelowMinimum() {
            // arrange
            Coupon coupon = Coupon.createFixedAmount(
                    "5000원 할인",
                    new BigDecimal("5000"),
                    new BigDecimal("30000"),
                    ZonedDateTime.now().minusDays(1),
                    ZonedDateTime.now().plusDays(1));
            BigDecimal orderAmount = new BigDecimal("20000");

            // act
            CoreException exception = assertThrows(CoreException.class, () ->
                    coupon.calculateDiscountAmount(orderAmount)
            );

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("쿠폰 유효기간 만료 시 BAD REQUEST 예외가 발생한다")
        @Test
        void throwBadRequestException_whenCouponExpired() {
            // arrange
            Coupon coupon = Coupon.createFixedAmount(
                    "만료된 쿠폰",
                    new BigDecimal("5000"),
                    new BigDecimal("10000"),
                    ZonedDateTime.now().minusDays(5),
                    ZonedDateTime.now().minusDays(1));
            BigDecimal orderAmount = new BigDecimal("50000");

            // act
            CoreException exception = assertThrows(CoreException.class, () ->
                    coupon.calculateDiscountAmount(orderAmount)
            );

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
