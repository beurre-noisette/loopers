package com.loopers.domain.point;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import java.math.BigDecimal;

public class PointHistoryCommand {

    public record Create(
            Long userId,
            BigDecimal amount,
            BigDecimal balanceAfter,
            PointTransactionType type,
            PointReference reference
    ) {
        public Create {
            if (userId == null) {
                throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 필수입니다.");
            }

            if (amount == null) {
                throw new CoreException(ErrorType.BAD_REQUEST, "금액은 필수입니다.");
            }

            if (balanceAfter == null) {
                throw new CoreException(ErrorType.BAD_REQUEST, "변경 후 잔액은 필수입니다.");
            }

            if (type == null) {
                throw new CoreException(ErrorType.BAD_REQUEST, "거래 타입은 필수입니다.");
            }

            if (reference == null) {
                throw new CoreException(ErrorType.BAD_REQUEST, "참조 정보는 필수입니다.");
            }
        }
    }
}
