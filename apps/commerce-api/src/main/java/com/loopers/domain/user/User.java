package com.loopers.domain.user;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Entity
@Table(name = "member")
@Getter
public class User extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String accountId;

    @Column(unique = true, nullable = false)
    private String email;

    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    protected User() {
    }

    private User(String accountId, String email, LocalDate birthDate, Gender gender) {
        this.accountId = accountId;
        this.email = email;
        this.birthDate = birthDate;
        this.gender = gender;
    }

    public static User of(UserCommand.Create command) {
        validateAccountId(command.accountId());
        validateEmail(command.email());
        validateGender(command.gender());

        return new User(command.accountId(), command.email(), validateAndParseBirthDate(command.birthDate()), command.gender());
    }

    private static void validateAccountId(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "accountId는 비어있을 수 없습니다.");
        }

        if (!accountId.matches("^[a-zA-Z0-9]{1,10}$")) {
            throw new CoreException(ErrorType.INVALID_INPUT_FORMAT, "10자를 초과하는 accountId는 생성할 수 없습니다.");
        }
    }

    private static void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "email은 비어있을 수 없습니다.");
        }

        if (!email.matches("^[a-zA-Z0-9]([a-zA-Z0-9._-]*[a-zA-Z0-9])?@[a-zA-Z0-9]([a-zA-Z0-9.-]*[a-zA-Z0-9])?\\.[a-zA-Z]{2,}$")) {
            throw new CoreException(ErrorType.INVALID_INPUT_FORMAT, "올바른 이메일 형식이 아닙니다. 올바른 이메일 형식은 xx@yy.zz와 같습니다.");
        }
    }

    private static LocalDate validateAndParseBirthDate(String birthDateStr) {
        if (birthDateStr == null || birthDateStr.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일은 비어있을 수 없습니다.");
        }

        try {
            return LocalDate.parse(birthDateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new CoreException(ErrorType.INVALID_INPUT_FORMAT, "생년월일은 yyyy-MM-dd 형식이어야 합니다.");
        }
    }
    
    private static void validateGender(Gender gender) {
        if (gender == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "성별은 필수값입니다.");
        }
    }
}
