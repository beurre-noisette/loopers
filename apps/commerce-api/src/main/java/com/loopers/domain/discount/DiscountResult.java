package com.loopers.domain.discount;

import java.math.BigDecimal;

public record DiscountResult(
        BigDecimal pointDiscount,
        BigDecimal couponDiscount
) {
    public static DiscountResult of(BigDecimal pointDiscount, BigDecimal couponDiscount) {
        return new DiscountResult(
                pointDiscount != null ? pointDiscount : BigDecimal.ZERO,
                couponDiscount != null ? couponDiscount : BigDecimal.ZERO
        );
    }

    public static DiscountResult onlyPoint(BigDecimal pointDiscount) {
        return of(pointDiscount, BigDecimal.ZERO);
    }

    public static DiscountResult none() {
        return of(BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public BigDecimal getTotalDiscount() {
        return pointDiscount.add(couponDiscount);
    }

    public BigDecimal calculateFinalAmount(BigDecimal originalAmount) {
        return originalAmount.subtract(getTotalDiscount());
    }
}
