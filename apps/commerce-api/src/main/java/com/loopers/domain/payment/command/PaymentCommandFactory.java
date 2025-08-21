package com.loopers.domain.payment.command;

import com.loopers.domain.payment.PaymentCommand;
import com.loopers.domain.payment.PaymentDetails;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PaymentCommandFactory {
    
    public PaymentCommand create(
            Long orderId,
            BigDecimal amount,
            PaymentDetails paymentDetails
    ) {
        if (paymentDetails == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 정보는 필수입니다.");
        }
        
        return switch (paymentDetails) {
            case PaymentDetails.Card card -> 
                new CardPaymentCommand(orderId, amount, card.cardType(), card.cardNo());
            case PaymentDetails.Point point -> 
                new PointPaymentCommand(orderId, amount);
        };
    }
}

