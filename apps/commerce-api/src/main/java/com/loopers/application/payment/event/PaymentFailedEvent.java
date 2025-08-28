package com.loopers.application.payment.event;

import com.loopers.domain.payment.PaymentMethod;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Getter
@Builder
public class PaymentFailedEvent {
    private final String correlationId;
    private final Long orderId;
    private final Long userId;
    private final PaymentMethod paymentMethod;
    private final BigDecimal attemptedAmount;
    private final String failureReason;
    private final ZonedDateTime occurredAt;
    
    public static PaymentFailedEvent of(
            String correlationId,
            Long orderId,
            Long userId,
            PaymentMethod paymentMethod,
            BigDecimal attemptedAmount,
            String failureReason
    ) {
        return PaymentFailedEvent.builder()
                .correlationId(correlationId)
                .orderId(orderId)
                .userId(userId)
                .paymentMethod(paymentMethod)
                .attemptedAmount(attemptedAmount)
                .failureReason(failureReason)
                .occurredAt(ZonedDateTime.now())
                .build();
    }
}