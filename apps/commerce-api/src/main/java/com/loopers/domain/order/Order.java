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
    private Long userId;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = OrderItemsConverter.class)
    private OrderItems orderItems;

    @Column
    private String cancelReason;

    protected Order() {}

    private Order(Long userId, OrderItems orderItems) {
        this.userId = userId;
        this.orderItems = orderItems;
        this.totalAmount = orderItems.calculateTotalAmount();
        this.status = OrderStatus.PENDING;
    }

    public static Order create(Long userId, OrderItems orderItems) {
        validateUserId(userId);
        return new Order(userId, orderItems);
    }

    public void cancel(String reason) {
        if (this.status == OrderStatus.COMPLETED) {
            throw new CoreException(ErrorType.INVALID_INPUT_FORMAT, "완료된 주문은 취소할 수 없습니다.");
        }

        this.status = OrderStatus.CANCELLED;
        this.cancelReason = reason;
    }

    public void waitForPayment() {
        if (this.status != OrderStatus.PENDING) {
            throw new CoreException(ErrorType.INVALID_INPUT_FORMAT, "대기 중인 주문만 결제 대기 상태로 변경할 수 있습니다.");
        }

        this.status = OrderStatus.PAYMENT_WAITING;
    }

    public void processingPayment() {
        if (this.status != OrderStatus.PAYMENT_WAITING) {
            throw new CoreException(ErrorType.INVALID_INPUT_FORMAT, "결제 대기 중인 주문만 처리 중 상태로 변경할 수 있습니다.");
        }

        this.status = OrderStatus.PAYMENT_PROCESSING;
    }

    public void completePayment() {
        if (this.status != OrderStatus.PAYMENT_WAITING && this.status != OrderStatus.PAYMENT_PROCESSING) {
            throw new CoreException(ErrorType.INVALID_INPUT_FORMAT, "결제 대기/처리 중인 주문만 완료할 수 있습니다.");
        }

        this.status = OrderStatus.COMPLETED;
    }

    public void applyDiscount(BigDecimal discountAmount) {
        if (discountAmount == null || discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.INVALID_INPUT_FORMAT, "할인 금액은 0 이상이어야 합니다.");
        }
        
        if (discountAmount.compareTo(this.totalAmount) > 0) {
            throw new CoreException(ErrorType.INVALID_INPUT_FORMAT, "할인 금액이 주문 금액을 초과할 수 없습니다.");
        }
        
        this.totalAmount = this.totalAmount.subtract(discountAmount);
    }

    private static void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 필수값입니다.");
        }
    }
}
