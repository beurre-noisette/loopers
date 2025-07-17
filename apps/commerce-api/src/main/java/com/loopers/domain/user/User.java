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
    private String userId;

    @Column(unique = true, nullable = false)
    private String email;

    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    private Integer point;

    protected User() {
    }

    public User(String userId, String email, String birthDateStr, Gender gender, Integer point) {
        validateUserId(userId);
        validateEmail(email);
        this.birthDate = validateAndParseBirthDate(birthDateStr);
        validateGender(gender);

        this.userId = userId;
        this.email = email;
        this.gender = gender;
        this.point = point;
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "userId는 비어있을 수 없습니다.");
        }

        if (!userId.matches("^[a-zA-Z0-9]{1,10}$")) {
            throw new CoreException(ErrorType.INVALID_INPUT_FORMAT, "10자를 초과하는 userId는 생성할 수 없습니다.");
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "email은 비어있을 수 없습니다.");
        }

        if (!email.matches("^[^@]+@[^@]+\\.[^@]+$")) {
            throw new CoreException(ErrorType.INVALID_INPUT_FORMAT, "올바른 이메일 형식이 아닙니다. 올바른 이메일 형식은 xx@yy.zz와 같습니다.");
        }
    }

    private LocalDate validateAndParseBirthDate(String birthDateStr) {
        if (birthDateStr == null || birthDateStr.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일은 비어있을 수 없습니다.");
        }

        try {
            return LocalDate.parse(birthDateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new CoreException(ErrorType.INVALID_INPUT_FORMAT, "생년월일은 yyyy-MM-dd 형식이어야 합니다.");
        }
    }
    
    private void validateGender(Gender gender) {
        if (gender == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "성별은 필수값입니다.");
        }
    }
}
