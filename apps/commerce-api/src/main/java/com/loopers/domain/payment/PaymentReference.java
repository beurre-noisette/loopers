package com.loopers.domain.payment;


public record PaymentReference(
        String referenceType,
        Long referenceId
) {
    public static PaymentReference order(Long orderId) {
        return new PaymentReference("ORDER", orderId);
    }
}
