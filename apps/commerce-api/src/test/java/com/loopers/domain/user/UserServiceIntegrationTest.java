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

            // act
            User savedUser = userService.register(userId, email, birthDate, gender);

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

            userService.register(userId, email, birthDate, gender);

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                userService.register(userId, email, birthDate, gender);
            });

            // assert
            assertEquals(ErrorType.ALREADY_REGISTERED_USER, exception.getErrorType());
        }
    }
}
