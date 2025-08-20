package com.loopers.application.payment;

import com.loopers.domain.payment.PaymentMethod;
import com.loopers.domain.payment.PaymentService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class PaymentServiceFactory {

    private final PaymentService pointPaymentService;
    private final PaymentService pgPaymentService;

    public PaymentServiceFactory(
            @Qualifier("pointPaymentService") PaymentService pointPaymentService,
            @Qualifier("pgPaymentService") PaymentService pgPaymentService
    ) {
        this.pointPaymentService = pointPaymentService;
        this.pgPaymentService = pgPaymentService;
    }

    public PaymentService getPaymentService(PaymentMethod paymentMethod) {
        return switch (paymentMethod) {
            case POINT -> pointPaymentService;
            case CARD -> pgPaymentService;
        };
    }
}
