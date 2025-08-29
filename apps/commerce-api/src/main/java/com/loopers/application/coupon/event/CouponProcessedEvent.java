package com.loopers.application.coupon.event;

import com.loopers.domain.payment.PaymentDetails;
import lombok.Builder;
import lombok.Getter;

import java.time.ZonedDateTime;

@Getter
@Builder
public class CouponProcessedEvent {
    private final String correlationId;
    private final Long orderId;
    private final Long userId;
    private final Long userCouponId;
    private final PaymentDetails paymentDetails;
    private final boolean couponApplied;
    private final ZonedDateTime occurredAt;
    
    public static CouponProcessedEvent success(
            String correlationId,
            Long orderId,
            Long userId,
            Long userCouponId,
            PaymentDetails paymentDetails
    ) {
        return CouponProcessedEvent.builder()
                .correlationId(correlationId)
                .orderId(orderId)
                .userId(userId)
                .userCouponId(userCouponId)
                .paymentDetails(paymentDetails)
                .couponApplied(userCouponId != null)
                .occurredAt(ZonedDateTime.now())
                .build();
    }
    
    public static CouponProcessedEvent noCoupon(
            String correlationId,
            Long orderId,
            Long userId,
            PaymentDetails paymentDetails
    ) {
        return CouponProcessedEvent.builder()
                .correlationId(correlationId)
                .orderId(orderId)
                .userId(userId)
                .userCouponId(null)
                .paymentDetails(paymentDetails)
                .couponApplied(false)
                .occurredAt(ZonedDateTime.now())
                .build();
    }
    
}
