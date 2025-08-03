package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import java.math.BigDecimal;

public record OrderItem(
        Long productId,
        int quantity,
        BigDecimal unitPrice
) {
    public OrderItem {
        validateProductId(productId);
        validateQuantity(quantity);
        validateUnitPrice(unitPrice);
    }

    public BigDecimal getTotalPrice() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    private static void validateProductId(Long productId) {
        if (productId == null || productId <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 ID는 필수값입니다.");
        }
    }

    private static void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new CoreException(ErrorType.INVALID_INPUT_FORMAT, "주문 수량은 1개 이상이어야 합니다.");
        }
    }

    private static void validateUnitPrice(BigDecimal unitPrice) {
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(ErrorType.INVALID_INPUT_FORMAT,
                    "상품 단가는 0원보다 커야 합니다.");
        }
    }
}
