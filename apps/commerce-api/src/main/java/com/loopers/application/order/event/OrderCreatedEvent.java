package com.loopers.application.order.event;

import com.loopers.domain.payment.PaymentMethod;
import lombok.Getter;

@Getter
public class OrderCreatedEvent {
    private final Long orderId;
    private final String userId;
    private final PaymentMethod paymentMethod;
    private final CardInfo cardInfo;
    
    public OrderCreatedEvent(Long orderId, String userId, PaymentMethod paymentMethod, CardInfo cardInfo) {
        this.orderId = orderId;
        this.userId = userId;
        this.paymentMethod = paymentMethod;
        this.cardInfo = cardInfo;
    }

    public record CardInfo(String cardType, String cardNo) {}
}
