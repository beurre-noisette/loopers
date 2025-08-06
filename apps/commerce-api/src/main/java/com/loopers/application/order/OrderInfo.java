package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.payment.PaymentResult;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

public record OrderInfo(
        Long orderId,
        String userId,
        OrderStatus status,
        BigDecimal originalAmount,
        BigDecimal pointUsed,
        BigDecimal finalAmount,
        Long paymentId,
        List<OrderItem> orderItems,
        ZonedDateTime createdAt,
        ZonedDateTime updatedAt
) {
    public static OrderInfo from(Order order, PaymentResult payment, BigDecimal pointUsed) {
        return new OrderInfo(
                order.getId(),
                order.getUserId(),
                order.getStatus(),
                order.getTotalAmount(),
                pointUsed,
                payment.amount(),
                payment.paymentId(),
                order.getOrderItems().getItems(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
