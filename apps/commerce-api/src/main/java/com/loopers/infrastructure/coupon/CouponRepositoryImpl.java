package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponRepository;
import com.loopers.domain.coupon.UserCoupon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class CouponRepositoryImpl implements CouponRepository {
    
    private final CouponJpaRepository couponJpaRepository;
    private final UserCouponJpaRepository userCouponJpaRepository;

    @Autowired
    public CouponRepositoryImpl(CouponJpaRepository couponJpaRepository,
                                UserCouponJpaRepository userCouponJpaRepository) {
        this.couponJpaRepository = couponJpaRepository;
        this.userCouponJpaRepository = userCouponJpaRepository;
    }
    
    @Override
    public Coupon save(Coupon coupon) {
        return couponJpaRepository.save(coupon);
    }
    
    @Override
    public Optional<Coupon> findById(Long id) {
        return couponJpaRepository.findById(id);
    }
    
    @Override
    public UserCoupon save(UserCoupon userCoupon) {
        return userCouponJpaRepository.save(userCoupon);
    }
    
    @Override
    public Optional<UserCoupon> findUserCouponById(Long id) {
        return userCouponJpaRepository.findById(id);
    }
    
    @Override
    public Optional<UserCoupon> findByIdWithPessimisticLock(Long userCouponId) {
        return userCouponJpaRepository.findByIdWithPessimisticLock(userCouponId);
    }
}
