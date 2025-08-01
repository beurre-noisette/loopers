package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
            UserCommand.Create command = new UserCommand.Create(userId, email, birthDateStr, gender);

            // act
            User user = User.of(command);

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
            UserCommand.Create command = new UserCommand.Create(null, "email@gmail.com", "1996-08-16", Gender.MALE);

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                User.of(command);
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("ID가 빈 칸일 경우 User 객체 생성에 실패하고 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwBadRequestException_whenUserIdIsEmpty() {
            // arrange
            UserCommand.Create command = new UserCommand.Create("", "email@gmail.com", "1996-08-16", Gender.MALE);

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                User.of(command);
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("ID가 10자를 초과할 경우 User 객체 생성에 실패하고 INVALID_INPUT_FORMAT 예외가 발생한다.")
        @Test
        void throwInvalidInputFormatException_whenUserIdIsTooLong() {
            // arrange
            UserCommand.Create command = new UserCommand.Create("toooooLongUserId", "email@gmail.com", "1996-08-16", Gender.MALE);

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                User.of(command);
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_FORMAT);
        }

        @DisplayName("ID에 영문 및 숫자 이외의 문자가 포함 되었을 경우 User 객체 생성에 실패하고 INVALID_INPUT_FORMAT 예외가 발생한다.")
        @Test
        void throwInvalidInputFormatException_whenUserIdContainsInvalidChars() {
            // arrange
            UserCommand.Create command = new UserCommand.Create("ㅎuserId", "email@gmail.com", "1996-08-16", Gender.MALE);

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                User.of(command);
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_FORMAT);
        }

        @DisplayName("이메일에 null이 들어올 경우 User 객체 생성에 실패하고 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwBadRequestException_whenEmailIsNull() {
            // arrange
            UserCommand.Create command = new UserCommand.Create("userId", null, "1996-08-16", Gender.MALE);

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                User.of(command);
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("이메일에 비어있을 경우 User 객체 생성에 실패하고 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwBadRequestException_whenEmailIsEmpty() {
            // arrange
            UserCommand.Create command = new UserCommand.Create("userId", "", "1996-08-16", Gender.MALE);

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
               User.of(command);
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("올바르지 않은 이메일 형식이 들어올 경우 User 객체 생성에 실패하고 INVALID_INPUT_FORMAT 예외가 발생한다.")
        @ParameterizedTest
        @ValueSource(strings = {
                "test....@test.kr",     // 연속된 점
                "testgmail.com",        // @ 기호 없음
                "test@",                // 도메인 없음
                ".test@gmail.com",      // 점으로 시작
                "test.@gmail.com",      // @ 앞에서 점으로 끝남
                "test@gmail.",          // 도메인이 점으로 끝남
                "test@@gmail.com",      // @ 기호 중복
                "test@gmail",           // 최상위 도메인 없음
                "@gmail.com",           // 로컬 부분 없음
                "test@.com",            // 도메인이 점으로 시작
                "test@gmail..com",      // 도메인에 연속된 점
                "test @gmail.com",      // 공백 포함
                "test@gmai l.com"       // 도메인에 공백 포함
        })
        void throwInvalidInputFormatException_whenEmailFormatIsInvalid(String invalidEmail) {
            // arrange
            UserCommand.Create command = new UserCommand.Create("userId", invalidEmail, "1996-08-16", Gender.MALE);

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                User.of(command);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_FORMAT);
        }

        @DisplayName("올바른 이메일 형식이 들어올 경우 User 객체가 정상적으로 생성된다.")
        @ParameterizedTest
        @ValueSource(strings = {
                "test@gmail.com",
                "user123@example.org",
                "my.email@domain.co.kr",
                "a@b.cc",
                "test123@test-domain.com",
                "user_name@company.io"
        })
        void createUser_whenEmailFormatIsValid(String validEmail) {
            // arrange
            UserCommand.Create command = new UserCommand.Create("userId", validEmail, "1996-08-16", Gender.MALE);

            // act
            User user = User.of(command);

            // assert
            assertThat(user.getEmail()).isEqualTo(validEmail);
        }

        @DisplayName("생년월일이 null일 경우 User 객체 생성에 실패하고 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwBadRequestException_whenBirthDateIsNull() {
            // arrange
            UserCommand.Create command = new UserCommand.Create("userId", "email@gmail.com", null, Gender.MALE);

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                User.of(command);
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("생년월일이 yyyy-MM-dd 형식이 아닐 경우 User 객체 생성에 실패하고 INVALID_INPUT_FORMAT 예외가 발생한다.")
        @Test
        void throwInvalidInputFormatException_whenBirthDateFormatIsInvalid() {
            // arrange
            UserCommand.Create command = new UserCommand.Create("userId", "email@gmail.com", "1996/08/16", Gender.MALE);

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                User.of(command);
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_FORMAT);
        }

        @DisplayName("존재하지 않는 월로 생년월일을 입력할 경우 User 객체 생성에 실패하고 INVALID_INPUT_FORMAT 예외가 발생한다.")
        @Test
        void throwInvalidInputFormatException_whenBirthDateHasInvalidMonth() {
            // arrange
            UserCommand.Create command = new UserCommand.Create("userId", "email@gmail.com", "1996-13-01", Gender.MALE);

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                User.of(command);
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_FORMAT);
        }

        @DisplayName("존재하지 않는 날짜로 생년월일을 입력할 경우 User 객체 생성에 실패하고 INVALID_INPUT_FORMAT 예외가 발생한다.")
        @Test
        void throwInvalidInputFormatException_whenBirthDateHasInvalidDay() {
            // arrange
            UserCommand.Create command = new UserCommand.Create("userId", "email@gmail.com", "1996-02-31", Gender.MALE);

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                User.of(command);
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_FORMAT);
        }
    }

    @DisplayName("포인트를 충전할 때")
    @Nested
    class ChargePoint {
        @DisplayName("0 이하의 정수로 포인트를 충전 시 실패한다.")
        @ParameterizedTest
        @ValueSource(ints = {
                -1000,
                -100,
                -1,
                0
        })
        void throwInvalidInputFormatException_whenChargePointIsUnderZero(int invalidAmount) {
            // arrange
            String userId = "testUser";
            String email = "test@gmail.com";
            String birthDate = "1996-08-16";
            Gender gender = Gender.MALE;
            UserCommand.Create command = new UserCommand.Create(userId, email, birthDate, gender);

            User user = User.of(command);

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                user.chargePoint(invalidAmount);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_FORMAT);
        }
    }
}
