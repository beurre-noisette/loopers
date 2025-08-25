package com.loopers.application.order.event;

import com.loopers.domain.payment.PaymentDetails;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class OrderCreatedEvent {
    private final Long orderId;
    private final String accountId;
    private final BigDecimal totalAmount;
    private final PaymentDetails paymentDetails;
    
    public OrderCreatedEvent(
            Long orderId,
            String accountId,
            BigDecimal totalAmount,
            PaymentDetails paymentDetails
    ) {
        this.orderId = orderId;
        this.accountId = accountId;
        this.totalAmount = totalAmount;
        this.paymentDetails = paymentDetails;
    }
}
