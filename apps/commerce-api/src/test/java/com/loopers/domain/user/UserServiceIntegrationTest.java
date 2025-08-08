package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserServiceIntegrationTest {
    @Autowired
    private UserService userService;

    @MockitoSpyBean
    private UserRepository userRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("회원 가입 시")
    @Nested
    class Register {
        @Test
        @DisplayName("User 저장이 수행된다.")
        void saveUser_whenUserRegister() {
            // arrange
            String userId = "testUser";
            String email = "test@gmail.com";
            String birthDate = "1996-08-16";
            Gender gender = Gender.MALE;

            UserCommand.Create command = new UserCommand.Create(userId, email, birthDate, gender);

            // act
            User savedUser = userService.signUp(command);

            // assert
            assertThat(savedUser.getUserId()).isEqualTo(userId);
            assertThat(savedUser.getEmail()).isEqualTo(email);
            verify(userRepository).save(any(User.class));
            verify(userRepository).existsByUserId(userId);
        }

        @Test
        @DisplayName("이미 존재하는 회원이라면 ALREADY_REGISTERED_USER 예외를 발생한다.")
        void throwAlreadyRegisteredUserException_whenProvidedDuplicateUserId() {
            // arrange
            String userId = "duplicate";
            String email = "test@gmail.com";
            String birthDate = "1996-08-16";
            Gender gender = Gender.MALE;

            UserCommand.Create command = new UserCommand.Create(userId, email, birthDate, gender);

            userService.signUp(command);

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                userService.signUp(command);
            });

            // assert
            assertEquals(ErrorType.ALREADY_REGISTERED_USER, exception.getErrorType());
        }
    }

    @DisplayName("회원이 내 정보를 조회할 때")
    @Nested
    class GetUser {
        @DisplayName("해당 ID의 회원이 존재할 경우, 회원 정보가 반환된다.")
        @Test
        void returnUserInfo_whenUserExists() {
            // arrange
            String userId = "testUser";
            UserCommand.Create command = new UserCommand.Create(userId, "test@gmail.com", "1996-08-16", Gender.MALE);
            User savedUser = userService.signUp(command);

            // act
            User foundUser = userService.findByUserId(userId);

            // assert
            assertThat(foundUser).isNotNull();
            assertThat(foundUser.getUserId()).isEqualTo(savedUser.getUserId());
            assertThat(foundUser.getEmail()).isEqualTo(savedUser.getEmail());
            verify(userRepository).findByUserId(userId);
        }

        @DisplayName("해당 ID의 회원이 존재하지 않을 경우, USER_NOT_FOUND 예외를 발생시킨다.")
        @Test
        void throwUserNotFoundException_whenUserDoesNotExist() {
            // arrange
            String nonExistUserId = "nonExistUserId";

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                userService.findByUserId(nonExistUserId);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.USER_NOT_FOUND);
            verify(userRepository).findByUserId(nonExistUserId);
        }
    }

}
