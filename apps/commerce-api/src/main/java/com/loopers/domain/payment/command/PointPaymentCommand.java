package com.loopers.domain.payment.command;

import com.loopers.domain.payment.PaymentCommand;
import com.loopers.domain.payment.PaymentMethod;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class PointPaymentCommand extends PaymentCommand {
    
    public PointPaymentCommand(
            Long orderId,
            BigDecimal amount
    ) {
        super(orderId, PaymentMethod.POINT, amount);
        validate();
    }
    
    @Override
    public void validate() {
    }
    
    @Override
    public Map<String, Object> toDetails() {
        return new HashMap<>();
    }
}
