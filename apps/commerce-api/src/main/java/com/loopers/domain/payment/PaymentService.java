package com.loopers.domain.payment;

import java.math.BigDecimal;

public interface PaymentService {

    void validatePaymentCapability(Long userId, BigDecimal amount);

    PaymentResult processPayment(Long userId, BigDecimal amount, PaymentReference reference);
}
