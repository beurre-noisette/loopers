package com.loopers.application.payment;

import com.loopers.domain.payment.PaymentMethod;
import com.loopers.domain.payment.PaymentProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class PaymentProcessorFactory {

    private final PaymentProcessor pointPaymentProcessor;
    private final PaymentProcessor pgPaymentProcessor;

    public PaymentProcessorFactory(
            @Qualifier("pointPaymentProcessor") PaymentProcessor pointPaymentProcessor,
            @Qualifier("pgPaymentProcessor") PaymentProcessor pgPaymentProcessor
    ) {
        this.pointPaymentProcessor = pointPaymentProcessor;
        this.pgPaymentProcessor = pgPaymentProcessor;
    }

    public PaymentProcessor getPaymentProcessor(PaymentMethod paymentMethod) {
        return switch (paymentMethod) {
            case POINT -> pointPaymentProcessor;
            case CARD -> pgPaymentProcessor;
        };
    }
}
