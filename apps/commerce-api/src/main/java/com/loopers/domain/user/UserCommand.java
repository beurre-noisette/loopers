package com.loopers.domain.user;

public class UserCommand {

    public record Create(
            String accountId,
            String email,
            String birthDate,
            Gender gender
    ) {}
}
