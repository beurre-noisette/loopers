package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderStatus;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

public record OrderInfo(
        Long id,
        String userId,
        OrderStatus status,
        BigDecimal totalAmount,
        List<OrderItem> orderItems,
        ZonedDateTime createdAt,
        ZonedDateTime updatedAt
) {
    public static OrderInfo from(Order order) {
        return new OrderInfo(
                order.getId(),
                order.getUserId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getOrderItems(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}