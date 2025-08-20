package com.loopers.domain.payment;


public record PaymentReference(
        String referenceType,
        Long referenceId,
        PaymentCommand.CardInfo cardInfo
) {
    public static PaymentReference order(Long orderId) {
        return new PaymentReference("ORDER", orderId, null);
    }
    
    public static PaymentReference orderWithCard(Long orderId, PaymentCommand.CardInfo cardInfo) {
        return new PaymentReference("ORDER", orderId, cardInfo);
    }
}
