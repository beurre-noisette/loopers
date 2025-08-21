package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserInfo;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class UserV1Dto {

    public record UserResponse(
            String accountId,
            String email,
            LocalDate birthDate,
            Gender gender
    ) {
        public static UserResponse from(UserInfo userInfo) {
            return new UserResponse(
                    userInfo.accountId(),
                    userInfo.email(),
                    userInfo.birthDate(),
                    userInfo.gender()
            );
        }
    }

    public record UserSignUpRequest(
            @NotBlank
            @NotNull
            String accountId,

            @NotBlank
            @NotNull
            String email,

            @NotBlank
            @NotNull
            String birthDate,

            @NotNull
            Gender gender
    ) {
        public UserCommand.Create toCommand() {
            return new UserCommand.Create(
                    accountId,
                    email,
                    birthDate,
                    gender
            );
        }
    }

    public record UserPointChargeRequest (
            int amount
    ) {}
}
