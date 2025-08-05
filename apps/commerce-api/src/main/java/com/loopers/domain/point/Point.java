package com.loopers.domain.point;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;

import java.math.BigDecimal;

@Entity
@Table(name = "points")
@Getter
public class Point extends BaseEntity {

    private Long userId;

    private BigDecimal balance;

    @Version
    private Long version;

    protected Point() {}

    private Point(Long userId, BigDecimal balance) {
        this.userId = userId;
        this.balance = balance;
    }

    public static Point create(Long userId) {
        if (userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 필수입니다.");
        }

        return new Point(userId, BigDecimal.ZERO);
    }

    public void charge(BigDecimal amount) {
        validatePositiveAmount(amount, "충전 금액은 0보다 커야 합니다.");

        this.balance = this.balance.add(amount);
    }

    public void use(BigDecimal amount) {
        validatePositiveAmount(amount, "사용 금액은 0보다 커야 합니다.");
        validateSufficientBalance(amount);

        this.balance = this.balance.subtract(amount);
    }

    private void validatePositiveAmount(BigDecimal amount, String message) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, message);
        }
    }

    private void validateSufficientBalance(BigDecimal amount) {
        if (!canUse(amount)) {
            throw new CoreException(ErrorType.NOT_ENOUGH, "잔액이 부족합니다.");
        }
    }

    private boolean canUse(BigDecimal amount) {
        return this.balance.compareTo(amount) >= 0;
    }

}
