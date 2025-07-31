package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
public class Order extends BaseEntity {

    @Column(nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = OrderItemListConverter.class)
    private List<OrderItem> orderItems = new ArrayList<>();

    protected Order() {}

    private Order(String userId, List<OrderItem> orderItems) {
        this.userId = userId;
        this.orderItems = new ArrayList<>(orderItems);
        this.totalAmount = calculateTotalAmount();
        this.status = OrderStatus.PENDING;
    }

    public static Order create(String userId, List<OrderItem> orderItems) {
        validateUserId(userId);
        validateOrderItems(orderItems);

        return new Order(userId, orderItems);
    }

    public List<OrderItem> getOrderItems() {
        return Collections.unmodifiableList(this.orderItems);
    }

    public void complete() {
        if (this.status != OrderStatus.PENDING) {
            throw new CoreException(ErrorType.INVALID_INPUT_FORMAT, "대기 중인 주문만 완료할 수 있습니다.");
        }

        this.status = OrderStatus.COMPLETED;
    }

    public void cancel() {
        if (this.status == OrderStatus.COMPLETED) {
            throw new CoreException(ErrorType.INVALID_INPUT_FORMAT, "완료된 주문은 취소할 수 없습니다.");
        }

        this.status = OrderStatus.CANCELLED;
    }

    public BigDecimal calculateTotalAmount() {
        return this.orderItems.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 필수값입니다.");
        }
    }

    private static void validateOrderItems(List<OrderItem> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 항목은 최소 1개 이상이어야 합니다.");
        }
    }
}
