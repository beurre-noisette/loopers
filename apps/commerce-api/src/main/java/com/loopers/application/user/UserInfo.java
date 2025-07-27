package com.loopers.application.user;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;

import java.time.LocalDate;

public record UserInfo(
        String userId,
        String email,
        LocalDate birthDate,
        Gender gender,
        Integer point
) {
    public static UserInfo from(User user) {
        return new UserInfo(
                user.getUserId(),
                user.getEmail(),
                user.getBirthDate(),
                user.getGender(),
                user.getPoint()
        );
    }
}