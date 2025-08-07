package com.loopers.domain.discount;

import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.order.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class DiscountService {

    private final CouponService couponService;

    @Autowired
    public DiscountService(CouponService couponService) {
        this.couponService = couponService;
    }

    public DiscountResult calculateDiscount(Order order, BigDecimal pointToUse, Long userCouponId) {
        BigDecimal totalAmount = order.getTotalAmount();
        
        BigDecimal pointDiscount = calculatePointDiscount(totalAmount, pointToUse);
        BigDecimal couponDiscount = calculateCouponDiscount(userCouponId, order.getId(), totalAmount);

        return DiscountResult.of(pointDiscount, couponDiscount);
    }

    private BigDecimal calculatePointDiscount(BigDecimal totalAmount, BigDecimal pointToUse) {
        if (pointToUse == null || pointToUse.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return pointToUse.min(totalAmount);
    }

    private BigDecimal calculateCouponDiscount(Long userCouponId, Long orderId, BigDecimal orderAmount) {
        if (userCouponId == null) {
            return BigDecimal.ZERO;
        }

        return couponService.useCouponAndCalculateDiscount(userCouponId, orderId, orderAmount);
    }
}
