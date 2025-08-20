package com.loopers.domain.payment;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

public class PaymentCommand {

    public record ProcessPayment(
            Long orderId,
            PaymentMethod method,
            CardInfo cardInfo
    ) {
        public ProcessPayment {
            if (orderId == null) {
                throw new CoreException(ErrorType.BAD_REQUEST, "주문 ID는 필수입니다.");
            }

            if (method == null) {
                method = PaymentMethod.POINT;
            }

            if (method == PaymentMethod.CARD && cardInfo == null) {
                throw new CoreException(ErrorType.BAD_REQUEST, "카드 결제 시 카드 정보는 필수입니다.");
            }
        }
    }

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
