package com.loopers.domain.payment;

public interface PaymentProcessor {

    void validatePaymentCapability(Long userId, PaymentCommand command);

    PaymentResult processPayment(Long userId, PaymentCommand command);
}
