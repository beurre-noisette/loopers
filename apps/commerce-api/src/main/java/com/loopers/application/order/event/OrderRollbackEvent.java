package com.loopers.application.order.event;

import lombok.Builder;
import lombok.Getter;

import java.time.ZonedDateTime;

@Getter
@Builder
public class OrderRollbackEvent {
    private final String correlationId;
    private final Long orderId;
    private final Long userId;
    private final Long userCouponId;
    private final String rollbackReason;
    private final RollbackType rollbackType;
    private final ZonedDateTime occurredAt;
    
    public enum RollbackType {
        COUPON_USAGE_FAILED,
        PAYMENT_FAILED
    }
    
    public static OrderRollbackEvent forCouponFailure(
            String correlationId,
            Long orderId,
            Long userId,
            Long userCouponId,
            String reason
    ) {
        return OrderRollbackEvent.builder()
                .correlationId(correlationId)
                .orderId(orderId)
                .userId(userId)
                .userCouponId(userCouponId)
                .rollbackReason(reason)
                .rollbackType(RollbackType.COUPON_USAGE_FAILED)
                .occurredAt(ZonedDateTime.now())
                .build();
    }
}
