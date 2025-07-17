package com.loopers.interfaces.api.user;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
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
        public static UserResponse from(User user) {
            return new UserResponse(
                    user.getUserId(),
                    user.getEmail(),
                    user.getBirthDate(),
                    user.getGender()
            );
        }
    }

    public record UserPointResponse(
            String userId,
            Integer point
    ) {
        public static UserPointResponse from(User user) {
            return new UserPointResponse(
                    user.getUserId(),
                    user.getPoint()
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
}
