package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.UserCoupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserCouponJpaRepository extends JpaRepository<UserCoupon, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT uc FROM UserCoupon uc WHERE uc.id = :id")
    Optional<UserCoupon> findByIdWithPessimisticLock(@Param("id") Long id);
}