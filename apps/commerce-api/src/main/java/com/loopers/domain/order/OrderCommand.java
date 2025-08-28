package com.loopers.domain.order;

import com.loopers.domain.payment.PaymentDetails;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import java.util.List;

public class OrderCommand {

    public record Create(
            List<CreateItem> items,
            Long userCouponId,
            PaymentDetails paymentDetails
    ) {
        public Create {
            if (items == null || items.isEmpty()) {
                throw new CoreException(ErrorType.BAD_REQUEST, "주문 항목은 필수 입니다.");
            }

            if (paymentDetails == null) {
                throw new CoreException(ErrorType.BAD_REQUEST, "결제 정보는 필수입니다.");
            }
        }
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
