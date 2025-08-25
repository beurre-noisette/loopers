package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderInfo;
import com.loopers.domain.payment.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;

public class OrderV1Dto {

    public record OrderItemRequest(
        Long productId,
        Integer quantity
    ) {}

    public record OrderCreateRequest(
        List<OrderItemRequest> items,
        BigDecimal pointToDiscount,
        Long userCouponId,
        PaymentMethod paymentMethod,
        CardInfoRequest cardInfo
    ) {}
    
    public record CardInfoRequest(
        String cardType,
        String cardNo
    ) {}

    public record OrderCreateResponse(
        Long orderId,
        BigDecimal totalAmount,
        String status
    ) {
        public static OrderCreateResponse from(OrderInfo orderInfo) {
            return new OrderCreateResponse(
                orderInfo.orderId(),
                orderInfo.finalAmount(),
                orderInfo.status().name()
            );
        }
    }


}
