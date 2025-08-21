package com.loopers.domain.coupon;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class CouponService {
    
    private final CouponRepository couponRepository;

    @Autowired
    public CouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }
    
    @Transactional
    public UserCoupon issueCoupon(Long userId, Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다."));
        
        UserCoupon userCoupon = UserCoupon.issue(userId, coupon);

        return couponRepository.save(userCoupon);
    }
    
    @Transactional
    public BigDecimal useCouponAndCalculateDiscount(Long userCouponId, Long orderId, BigDecimal orderAmount) {
        UserCoupon userCoupon = couponRepository.findByIdWithPessimisticLock(userCouponId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "발급된 쿠폰을 찾을 수 없습니다."));
        
        if (!userCoupon.canUse()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용할 수 없는 쿠폰입니다.");
        }
        
        Coupon coupon = userCoupon.getCoupon();
        BigDecimal discountAmount = coupon.calculateDiscountAmount(orderAmount);
        
        userCoupon.use(orderId);
        
        return discountAmount;
    }
    
    public UserCoupon findUserCoupon(Long userCouponId) {
        return couponRepository.findUserCouponById(userCouponId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "발급된 쿠폰을 찾을 수 없습니다."));
    }
    
    @Transactional
    public void rollbackCouponUsage(Long orderId) {
        UserCoupon userCoupon = couponRepository.findUserCouponByOrderId(orderId);
        
        if (userCoupon != null) {
            userCoupon.rollback();
            couponRepository.save(userCoupon);
        }
    }
}
