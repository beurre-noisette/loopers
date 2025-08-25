package com.loopers.domain.payment.command;

import com.loopers.domain.payment.PaymentCommand;
import com.loopers.domain.payment.PaymentMethod;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Getter
public class CardPaymentCommand extends PaymentCommand {
    private final String cardType;
    private final String cardNo;

    public CardPaymentCommand(
            Long orderId,
            BigDecimal amount,
            String cardType,
            String cardNo
    ) {
        super(orderId, PaymentMethod.CARD, amount);
        this.cardType = cardType;
        this.cardNo = cardNo;
        validate();
    }

    @Override
    public void validate() {
        if (cardType == null || cardType.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "카드 타입은 필수입니다.");
        }

        if (cardNo == null || !cardNo.matches("\\d{4}-\\d{4}-\\d{4}-\\d{4}")) {
            throw new CoreException(ErrorType.BAD_REQUEST, "카드 번호는 xxxx-xxxx-xxxx-xxxx 형식이어야 합니다.");
        }
    }
    @Override
    public Map<String, Object> toDetails() {
        Map<String, Object> details = new HashMap<>();
        details.put("cardType", cardType);
        details.put("cardNo", cardNo);
        return details;
    }
}
