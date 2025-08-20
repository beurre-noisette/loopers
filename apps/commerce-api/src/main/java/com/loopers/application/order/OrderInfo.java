package com.loopers.application.order;

import com.loopers.domain.discount.DiscountResult;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.payment.PaymentResult;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

public record OrderInfo(
        Long orderId,
        Long userId,
        OrderStatus status,
        BigDecimal originalAmount,
        BigDecimal pointDiscount,
        BigDecimal couponDiscount,
        BigDecimal finalAmount,
        List<OrderItem> orderItems,
        ZonedDateTime createdAt,
        ZonedDateTime updatedAt
) {
    public static OrderInfo from(Order order, DiscountResult discount) {
        return new OrderInfo(
                order.getId(),
                order.getUserId(),
                order.getStatus(),
                order.getTotalAmount(),
                discount.pointDiscount(),
                discount.couponDiscount(),
                discount.calculateFinalAmount(order.getTotalAmount()),
                order.getOrderItems().getItems(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
