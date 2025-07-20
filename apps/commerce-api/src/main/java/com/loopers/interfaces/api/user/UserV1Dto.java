package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserInfo;
import com.loopers.domain.user.Gender;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class UserV1Dto {

    public record UserResponse(
            String userId,
            String email,
            LocalDate birthDate,
            Gender gender
    ) {
        public static UserResponse from(UserInfo userInfo) {
            return new UserResponse(
                    userInfo.userId(),
                    userInfo.email(),
                    userInfo.birthDate(),
                    userInfo.gender()
            );
        }
    }

    public record UserPointResponse(
            String userId,
            Integer point
    ) {
        public static UserPointResponse from(UserInfo userInfo) {
            return new UserPointResponse(
                    userInfo.userId(),
                    userInfo.point()
            );
        }
    }

    public record UserRegisterRequest(
            @NotBlank
            @NotNull
            String userId,

            @NotBlank
            @NotNull
            String email,

            @NotBlank
            @NotNull
            String birthDate,

            @NotNull
            Gender gender
    ) {}

    public record UserPointChargeRequest (
            @Min(value = 1, message = "충전 금액은 1원 이상이어야 합니다.")
            int amount
    ) {}
}
