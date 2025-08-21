package com.loopers.domain.discount;

import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.order.Order;
import com.loopers.domain.point.PointService;
import com.loopers.domain.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class DiscountService {

    private final CouponService couponService;
    private final PointService pointService;

    @Autowired
    public DiscountService(CouponService couponService, PointService pointService) {
        this.couponService = couponService;
        this.pointService = pointService;
    }

    public DiscountResult calculateDiscount(User user, Order order, BigDecimal pointToUse, Long userCouponId) {
        BigDecimal totalAmount = order.getTotalAmount();
        
        BigDecimal pointDiscount = calculateAndApplyPointDiscount(user, totalAmount, pointToUse, order.getId());
        BigDecimal couponDiscount = calculateCouponDiscount(userCouponId, order.getId(), totalAmount);

        return DiscountResult.of(pointDiscount, couponDiscount);
    }

    private BigDecimal calculateAndApplyPointDiscount(User user, BigDecimal totalAmount, BigDecimal pointToUse, Long orderId) {
        if (pointToUse == null || pointToUse.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal actualPointDiscount = pointToUse.min(totalAmount);

        pointService.usePointForDiscount(user.getId(), actualPointDiscount, orderId);

        return actualPointDiscount;
    }

    private BigDecimal calculateCouponDiscount(Long userCouponId, Long orderId, BigDecimal orderAmount) {
        if (userCouponId == null) {
            return BigDecimal.ZERO;
        }

        return couponService.useCouponAndCalculateDiscount(userCouponId, orderId, orderAmount);
    }

    public void rollbackDiscount(Long orderId) {
        rollbackPointDiscount(orderId);
        rollbackCouponDiscount(orderId);
    }

    private void rollbackPointDiscount(Long orderId) {
        pointService.refundOrderDiscount(orderId);
    }

    private void rollbackCouponDiscount(Long orderId) {
        couponService.rollbackCouponUsage(orderId);
    }
}
