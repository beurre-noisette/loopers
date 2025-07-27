package com.loopers.application.user;

import com.loopers.domain.user.User;

public record UserPointInfo(
        int point
) {
    public static UserPointInfo from(User user) {
        return new UserPointInfo(user.getPoint());
    }
}
