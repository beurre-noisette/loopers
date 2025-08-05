package com.loopers.application.user;

import com.loopers.domain.point.Point;
import com.loopers.domain.user.User;

import java.math.BigDecimal;

public record UserPointInfo(
        String userId,
        BigDecimal point
) {
    public static UserPointInfo from(User user, Point point) {
        return new UserPointInfo(
                user.getUserId(),
                point.getBalance()
        );
    }
}
