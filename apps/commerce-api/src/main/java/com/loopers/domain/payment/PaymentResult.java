package com.loopers.domain.payment;


import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResult(
        Long paymentId,
        BigDecimal amount,
        PaymentStatus status,
        LocalDateTime processedAt,
        String message
) {
    public static PaymentResult success(Long paymentId, BigDecimal amount) {
        return new PaymentResult(paymentId, amount, PaymentStatus.SUCCESS,
                LocalDateTime.now(), "결제 성공");
    }
}
