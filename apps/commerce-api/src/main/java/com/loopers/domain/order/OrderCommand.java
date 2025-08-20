package com.loopers.domain.order;

import com.loopers.domain.payment.PaymentMethod;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import java.math.BigDecimal;
import java.util.List;

public class OrderCommand {

    public record Create(
            List<CreateItem> items,
            BigDecimal pointToDiscount,
            Long userCouponId,
            PaymentMethod paymentMethod,
            CardInfo cardInfo
    ) {
        public Create {
            if (items == null || items.isEmpty()) {
                throw new CoreException(ErrorType.BAD_REQUEST, "주문 항목은 필수 입니다.");
            }

            if (pointToDiscount == null || pointToDiscount.compareTo(BigDecimal.ZERO) < 0) {
                pointToDiscount = BigDecimal.ZERO;
            }

            if (paymentMethod == null) {
                throw new CoreException(ErrorType.BAD_REQUEST, "결제 방법은 필수입니다.");
            }

            if (paymentMethod == PaymentMethod.CARD && cardInfo == null) {
                throw new CoreException(ErrorType.BAD_REQUEST, "카드 결제 시 카드 정보는 필수입니다.");
            }

            if (paymentMethod == PaymentMethod.POINT && cardInfo != null) {
                throw new CoreException(ErrorType.BAD_REQUEST, "포인트 결제 시 카드 정보는 불필요합니다.");
            }
        }

        public record CardInfo(
            String cardType,
            String cardNo
        ) {}
    }

    public record CreateItem(
            Long productId,
            int quantity
    ) {
        public CreateItem {
            if (productId == null || productId <= 0) {
                throw new CoreException(ErrorType.BAD_REQUEST, "상품 ID는 필수입니다.");
            }

            if (quantity <= 0) {
                throw new CoreException(ErrorType.BAD_REQUEST, "수량은 1개 이상이어야 합니다.");
            }
        }
    }
}
