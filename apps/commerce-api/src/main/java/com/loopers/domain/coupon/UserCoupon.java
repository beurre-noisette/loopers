package com.loopers.domain.coupon;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.ZonedDateTime;

@Entity
@Table(name = "user_coupons", 
       indexes = {
           @Index(name = "idx_user_coupons_user_id", columnList = "user_id"),
           @Index(name = "idx_user_coupons_coupon_id", columnList = "coupon_id")
       })
@Getter
public class UserCoupon extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Column(name = "is_used", nullable = false)
    private boolean used = false;

    @Column(name = "used_at")
    private ZonedDateTime usedAt;

    @Column(name = "order_id")
    private Long orderId;

    protected UserCoupon() {}

    private UserCoupon(Long userId, Coupon coupon) {
        this.userId = userId;
        this.coupon = coupon;
        this.used = false;
    }

    public static UserCoupon issue(Long userId, Coupon coupon) {
        if (userId == null || userId <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "유효하지 않은 사용자 ID입니다.");
        }

        if (coupon == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 정보는 필수입니다.");
        }

        return new UserCoupon(userId, coupon);
    }

    public void use(Long orderId) {
        if (this.used) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 사용된 쿠폰입니다.");
        }
        if (!coupon.isValidPeriod()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 유효 기간이 만료되었습니다.");
        }
        
        this.used = true;
        this.usedAt = ZonedDateTime.now();
        this.orderId = orderId;
    }

    public boolean canUse() {
        return !used && coupon.isValidPeriod();
    }

    public void rollback() {
        if (!this.used) {
            return;
        }
        
        this.used = false;
        this.usedAt = null;
        this.orderId = null;
    }

    @Override
    protected void guard() {
        if (userId == null || userId <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "유효하지 않은 사용자 ID입니다.");
        }

        if (coupon == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 정보는 필수입니다.");
        }
    }
}
