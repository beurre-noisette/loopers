package com.loopers.application.payment;

import com.loopers.domain.payment.PaymentMethod;
import com.loopers.domain.payment.PaymentResult;
import com.loopers.domain.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public class PaymentInfo {

    public record ProcessResponse(
            Long orderId,
            PaymentMethod method,
            BigDecimal amount,
            PaymentStatus status,
            String message,
            ZonedDateTime processedAt
    ) {
        public static ProcessResponse from(Long orderId, PaymentMethod method, PaymentResult result) {
            return new ProcessResponse(
                    orderId,
                    method,
                    result.amount(),
                    result.status(),
                    result.message(),
                    result.processedAt()
            );
        }
    }
}
