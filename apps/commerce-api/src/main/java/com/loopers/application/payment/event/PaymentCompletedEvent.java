package com.loopers.application.payment.event;

import com.loopers.domain.payment.PaymentMethod;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Getter
@Builder
public class PaymentCompletedEvent {
    private final String correlationId;
    private final Long orderId;
    private final Long userId;
    private final Long paymentId;
    private final String transactionKey;
    private final PaymentMethod paymentMethod;
    private final BigDecimal paidAmount;
    private final ZonedDateTime occurredAt;
    
    public static PaymentCompletedEvent of(
            String correlationId,
            Long orderId,
            Long userId,
            Long paymentId,
            String transactionKey,
            PaymentMethod paymentMethod,
            BigDecimal paidAmount
    ) {
        return PaymentCompletedEvent.builder()
                .correlationId(correlationId)
                .orderId(orderId)
                .userId(userId)
                .paymentId(paymentId)
                .transactionKey(transactionKey)
                .paymentMethod(paymentMethod)
                .paidAmount(paidAmount)
                .occurredAt(ZonedDateTime.now())
                .build();
    }
}