package com.loopers.domain.payment;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

public record PaymentResult(
        Long paymentId,
        BigDecimal amount,
        PaymentStatus status,
        ZonedDateTime processedAt,
        String message
) {
    public static PaymentResult success(Long paymentId, BigDecimal amount) {
        return new PaymentResult(paymentId, amount, PaymentStatus.SUCCESS,
                ZonedDateTime.now(), "결제 성공");
    }
}
