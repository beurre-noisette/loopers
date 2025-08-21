package com.loopers.domain.coupon;

import java.util.Optional;

public interface CouponRepository {
    Coupon save(Coupon coupon);

    Optional<Coupon> findById(Long id);

    UserCoupon save(UserCoupon userCoupon);

    Optional<UserCoupon> findUserCouponById(Long id);

    Optional<UserCoupon> findByIdWithPessimisticLock(Long userCouponId);
    
    UserCoupon findUserCouponByOrderId(Long orderId);
}
