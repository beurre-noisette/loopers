package com.loopers.domain.payment;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(name = "payments")
@Getter
public class Payment extends BaseEntity {

    @Column(nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column
    private String transactionKey;

    @Column
    private String failureReason;

    @Column
    private ZonedDateTime processedAt;

    protected Payment() {}

    private Payment(
            Long orderId,
            PaymentMethod method,
            BigDecimal amount,
            String transactionKey,
            PaymentStatus status
    ) {
        this.orderId = orderId;
        this.method = method;
        this.amount = amount;
        this.transactionKey = transactionKey;
        this.status = status;
        this.processedAt = ZonedDateTime.now();
    }

    public static Payment create(
            Long orderId,
            PaymentMethod method,
            BigDecimal amount,
            String transactionKey,
            PaymentStatus status
    ) {
        validateOrderId(orderId);
        validateAmount(amount);
        
        return new Payment(
                orderId,
                method,
                amount,
                transactionKey,
                status
        );
    }

    public void markSuccess(String transactionKey) {
        if (this.status == PaymentStatus.SUCCESS) {
            return;
        }
        
        this.status = PaymentStatus.SUCCESS;
        this.transactionKey = transactionKey;
        this.processedAt = ZonedDateTime.now();
    }

    public void markFailed(String reason) {
        if (this.status == PaymentStatus.FAILED) {
            return;
        }
        
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.processedAt = ZonedDateTime.now();
    }

    private static void validateOrderId(Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 ID는 필수값입니다.");
        }
    }

    private static void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 금액은 0보다 커야 합니다.");
        }
    }
}
