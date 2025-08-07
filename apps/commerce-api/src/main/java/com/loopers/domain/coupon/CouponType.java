package com.loopers.domain.coupon;

import lombok.Getter;

@Getter
public enum CouponType {
    FIXED_AMOUNT("정액 할인"),
    FIXED_RATE("정률 할인");

    private final String description;

    CouponType(String description) {
        this.description = description;
    }
}
