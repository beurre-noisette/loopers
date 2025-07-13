package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @DisplayName("회원(User)을 생성할 때, ")
    @Nested
    class Create {
        @DisplayName("올바른 형식(영문 및 숫자 10자 이내)의 ID, 올바른 형식(xx@yy.zz)의 이메일, 올바른 형식(yyyy-MM-dd)의 생년월일이 주어지면 정상적으로 생성된다.")
        @Test
        void createUser_whenCorrectIdEmailAndBirthdayProvided() {
            // arrange
            String userId = "correctId";
            String email = "goodEmail@gmail.com";
            String birthDateStr = "1996-08-16";
            Gender gender = Gender.MALE;

            // act
            User user = new User(userId, email, birthDateStr, gender);

            // assert
            assertAll(
                    () -> assertThat(user.getId()).isNotNull(),
                    () -> assertThat(user.getUserId()).isEqualTo(userId),
                    () -> assertThat(user.getEmail()).isEqualTo(email),
                    () -> assertThat(user.getBirthDate()).isEqualTo(LocalDate.parse(birthDateStr)),
                    () -> assertThat(user.getGender()).isEqualTo(gender)
            );
        }

        @DisplayName("ID가 null일 경우 User 객체 생성에 실패하고 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwBadRequestException_whenUserIdIsNull() {
            // arrange
            String nullId = null;

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                new User(nullId, "email@gmail.com", "1996-08-16", Gender.MALE);
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("ID가 빈 칸일 경우 User 객체 생성에 실패하고 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwBadRequestException_whenUserIdIsEmpty() {
            // arrange
            String blankId = "";

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                new User(blankId, "email@gmail.com", "1996-08-16", Gender.MALE);
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("ID가 10자를 초과할 경우 User 객체 생성에 실패하고 INVALID_INPUT_FORMAT 예외가 발생한다.")
        @Test
        void throwInvalidInputFormatException_whenUserIdIsTooLong() {
            // arrange
            String tooLongUserId = "tooLongUserId";

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                new User(tooLongUserId, "email@gmail.com", "1996-08-16", Gender.MALE);
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_FORMAT);
        }

        @DisplayName("ID에 영문 및 숫자 이외의 문자가 포함 되었을 경우 User 객체 생성에 실패하고 INVALID_INPUT_FORMAT 예외가 발생한다.")
        @Test
        void throwInvalidInputFormatException_whenUserIdContainsInvalidChars() {
            // arrange
            String invalidUserId = "ㅎuserId";

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                new User(invalidUserId, "email@gmail.com", "1996-08-16", Gender.MALE);
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_FORMAT);
        }

        @DisplayName("이메일에 null이 들어올 경우 User 객체 생성에 실패하고 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwBadRequestException_whenEmailIsNull() {
            // arrange
            String nullEmail = null;

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                new User("correctId", nullEmail, "1996-08-16", Gender.MALE);
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("이메일에 비어있을 경우 User 객체 생성에 실패하고 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwBadRequestException_whenEmailIsEmpty() {
            // arrange
            String emptyEmail = "";

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                new User("correctId", emptyEmail, "1996-08-16", Gender.MALE);
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("올바르지 않은 이메일 형식이 들어올 경우 User 객체 생성에 실패하고 INVALID_INPUT_FORMAT 예외가 발생한다.")
        @Test
        void throwInvalidInputFormatException_whenEmailContainsInvalidChars() {
            // arrange
            String invalidEmail = "dd@dd";

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                new User("correctId", invalidEmail, "1996-08-16", Gender.MALE);
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_FORMAT);
        }

        @DisplayName("생년월일이 null일 경우 User 객체 생성에 실패하고 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwBadRequestException_whenBirthDateIsNull() {
            // arrange
            String nullBirthDate = null;

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                new User("correctId", "good@email.com", nullBirthDate, Gender.MALE);
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("생년월일이 yyyy-MM-dd 형식이 아닐 경우 User 객체 생성에 실패하고 INVALID_INPUT_FORMAT 예외가 발생한다.")
        @Test
        void throwInvalidInputFormatException_whenBirthDateFormatIsInvalid() {
            // arrange
            String invalidBirthDate = "1996/08/16";

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                new User("correctId", "good@email.com", invalidBirthDate,Gender.MALE);
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_FORMAT);
        }

        @DisplayName("존재하지 않는 월로 생년월일을 입력할 경우 User 객체 생성에 실패하고 INVALID_INPUT_FORMAT 예외가 발생한다.")
        @Test
        void throwInvalidInputFormatException_whenBirthDateHasInvalidMonth() {
            // arrange
            String invalidBirthDate = "1996-13-01";

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                new User("correctId", "good@email.com", invalidBirthDate, Gender.MALE);
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_FORMAT);
        }

        @DisplayName("존재하지 않는 날짜로 생년월일을 입력할 경우 User 객체 생성에 실패하고 INVALID_INPUT_FORMAT 예외가 발생한다.")
        @Test
        void throwInvalidInputFormatException_whenBirthDateHasInvalidDay() {
            // arrange
            String invalidBirthDate = "1996-02-31";

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                new User("correctId", "good@email.com", invalidBirthDate, Gender.MALE);
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_FORMAT);
        }
    }
}
