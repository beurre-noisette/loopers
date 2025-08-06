package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import java.math.BigDecimal;
import java.util.List;

public class OrderCommand {

    public record Create(
            List<CreateItem> items,
            BigDecimal pointToDiscount
    ) {
        public Create {
            if (items == null || items.isEmpty()) {
                throw new CoreException(ErrorType.BAD_REQUEST, "주문 항목은 필수 입니다.");
            }

            if (pointToDiscount == null || pointToDiscount.compareTo(BigDecimal.ZERO) < 0) {
                pointToDiscount = BigDecimal.ZERO;
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
