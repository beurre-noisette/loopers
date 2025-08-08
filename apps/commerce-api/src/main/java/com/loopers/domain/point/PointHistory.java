package com.loopers.domain.point;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;

@Entity
@Table(name = "point_histories")
@Getter
public class PointHistory extends BaseEntity {

    private Long userId;

    private BigDecimal amount;

    private BigDecimal balanceAfter;

    @Enumerated(EnumType.STRING)
    private PointTransactionType type;

    @Embedded
    private PointReference reference;

    protected PointHistory() {}

    private PointHistory(
            Long userId,
            BigDecimal amount,
            BigDecimal balanceAfter,
            PointTransactionType type,
            PointReference reference
    ) {
        this.userId = userId;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.type = type;
        this.reference = reference;
    }

    public static PointHistory create(PointHistoryCommand.Create command) {
        return new PointHistory(
                command.userId(),
                command.amount(),
                command.balanceAfter(),
                command.type(),
                command.reference()
                );
    }
}
