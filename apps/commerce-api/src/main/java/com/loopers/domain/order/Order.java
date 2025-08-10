package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;

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
    @Convert(converter = OrderItemsConverter.class)
    private OrderItems orderItems;

    protected Order() {}

    private Order(String userId, OrderItems orderItems) {
        this.userId = userId;
        this.orderItems = orderItems;
        this.totalAmount = orderItems.calculateTotalAmount();
        this.status = OrderStatus.PENDING;
    }

    public static Order create(String userId, OrderItems orderItems) {
        validateUserId(userId);
        return new Order(userId, orderItems);
    }

    public OrderItems getOrderItems() {
        return this.orderItems;
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


    private static void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 필수값입니다.");
        }
    }
}
