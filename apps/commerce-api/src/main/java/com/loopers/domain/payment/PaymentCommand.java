package com.loopers.domain.payment;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
public abstract class PaymentCommand {
    protected final Long orderId;
    protected final PaymentMethod method;
    protected final BigDecimal amount;
    
    protected PaymentCommand(Long orderId, PaymentMethod method, BigDecimal amount) {
        if (orderId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 ID는 필수입니다.");
        }

        if (method == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 수단은 필수입니다.");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 금액은 0보다 커야 합니다.");
        }
        
        this.orderId = orderId;
        this.method = method;
        this.amount = amount;
    }

    public abstract void validate();
    
    public abstract Map<String, Object> toDetails();

    public record CardInfo(
            String cardType,
            String cardNo
    ) {
        public CardInfo {
            if (cardType == null || cardType.isBlank()) {
                throw new CoreException(ErrorType.BAD_REQUEST, "카드 타입은 필수입니다.");
            }

            if (cardNo == null || !cardNo.matches("\\d{4}-\\d{4}-\\d{4}-\\d{4}")) {
                throw new CoreException(ErrorType.BAD_REQUEST, "카드 번호는 xxxx-xxxx-xxxx-xxxx 형식이어야 합니다.");
            }
        }
    }
}
