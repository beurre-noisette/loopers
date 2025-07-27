package com.loopers.domain.user;

public class UserCommand {

    public record Create(
            String userId,
            String email,
            String birthDate,
            Gender gender
    ) {}
}
