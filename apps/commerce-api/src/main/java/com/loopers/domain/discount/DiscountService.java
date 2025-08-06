package com.loopers.domain.discount;

import com.loopers.domain.order.Order;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class DiscountService {

    public DiscountResult calculateDiscount(Order order, BigDecimal pointToUse) {
        if (pointToUse.compareTo(BigDecimal.ZERO) == 0) {
            return DiscountResult.none();
        }

        BigDecimal totalAmount = order.getTotalAmount();
        BigDecimal pointDiscount = calculatePointDiscount(totalAmount, pointToUse);

        // TODO 쿠폰 개념 도입시 of 메서드로 변경
        return DiscountResult.onlyPoint(pointDiscount);
    }

    private BigDecimal calculatePointDiscount(BigDecimal totalAmount, BigDecimal pointToUse) {
        return pointToUse.min(totalAmount);
    }
}
