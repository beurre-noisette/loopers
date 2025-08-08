package com.loopers.domain.coupon;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;

@Entity
@Table(name = "coupons")
@Getter
public class Coupon extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "coupon_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private CouponType type;

    @Column(name = "discount_value", nullable = false)
    private BigDecimal discountValue;

    @Column(name = "min_order_amount")
    private BigDecimal minOrderAmount;

    @Column(name = "max_discount_amount")
    private BigDecimal maxDiscountAmount;

    @Column(name = "valid_from")
    private ZonedDateTime validFrom;

    @Column(name = "valid_until")
    private ZonedDateTime validUntil;

    protected Coupon() {}

    private Coupon(String name, CouponType type, BigDecimal discountValue,
                   BigDecimal minOrderAmount, BigDecimal maxDiscountAmount,
                   ZonedDateTime validFrom, ZonedDateTime validUntil) {
        this.name = name;
        this.type = type;
        this.discountValue = discountValue;
        this.minOrderAmount = minOrderAmount != null ? minOrderAmount : BigDecimal.ZERO;
        this.maxDiscountAmount = maxDiscountAmount;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
    }

    public static Coupon createFixedAmount(
            String name,
            BigDecimal discountAmount,
            BigDecimal minOrderAmount,
            ZonedDateTime validFrom,
            ZonedDateTime validUntil) {
        validateDiscountValue(discountAmount);

        return new Coupon(
                name,
                CouponType.FIXED_AMOUNT,
                discountAmount,
                minOrderAmount,
                null,
                validFrom,
                validUntil);
    }

    public static Coupon createFixedRate(
            String name,
            BigDecimal discountRate,
            BigDecimal minOrderAmount,
            BigDecimal maxDiscountAmount,
            ZonedDateTime validFrom,
            ZonedDateTime validUntil) {
        validatePercentageRate(discountRate);

        return new Coupon(
                name,
                CouponType.FIXED_RATE,
                discountRate,
                minOrderAmount,
                maxDiscountAmount,
                validFrom,
                validUntil);
    }

    public BigDecimal calculateDiscountAmount(BigDecimal orderAmount) {
        if (!isValidForAmount(orderAmount)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "최소 주문 금액을 충족하지 않습니다.");
        }

        if (!isValidPeriod()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 유효 기간이 아닙니다.");
        }

        return switch (type) {
            case FIXED_AMOUNT -> discountValue;
            case FIXED_RATE -> calculatePercentageDiscount(orderAmount);
        };
    }

    private BigDecimal calculatePercentageDiscount(BigDecimal orderAmount) {
        BigDecimal discount = orderAmount.multiply(discountValue).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        if (maxDiscountAmount != null && discount.compareTo(maxDiscountAmount) > 0) {
            return maxDiscountAmount;
        }

        return discount;
    }

    private boolean isValidForAmount(BigDecimal orderAmount) {
        return orderAmount.compareTo(minOrderAmount) >= 0;
    }

    protected boolean isValidPeriod() {
        ZonedDateTime now = ZonedDateTime.now();
        boolean isAfterValidFrom = validFrom == null || !now.isBefore(validFrom);
        boolean isBeforeValidUntil = validUntil == null || !now.isAfter(validUntil);
        return isAfterValidFrom && isBeforeValidUntil;
    }

    @Override
    protected void guard() {
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 이름은 필수입니다.");
        }

        if (type == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 타입은 필수입니다.");
        }

        if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인 값은 0보다 커야 합니다.");
        }
    }

    private static void validateDiscountValue(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인 금액은 0보다 커야 합니다.");
        }
    }

    private static void validatePercentageRate(BigDecimal rate) {
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0 || rate.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인율은 0보다 크고 100 이하여야 합니다.");
        }
    }
}
