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

import java.util.Optional;

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

    @DisplayName("회원이 내 정보를 조회할 때")
    @Nested
    class GetUser {
        @DisplayName("해당 ID의 회원이 존재할 경우, 회원 정보가 반환된다.")
        @Test
        void returnUserInfo_whenUserExists() {
            // arrange
            String userId = "testUser";
            User savedUser = userService.register(userId, "test@gmail.com", "1996-08-16", Gender.MALE);

            // act
            Optional<User> foundUser = userService.findByUserId(userId);

            // assert
            assertThat(foundUser)
                    .isPresent()
                    .hasValueSatisfying(user -> {
                        assertThat(user.getUserId()).isEqualTo(savedUser.getUserId());
                        assertThat(user.getEmail()).isEqualTo(savedUser.getEmail());
                    });
            verify(userRepository).findByUserId(userId);
        }

        @DisplayName("해당 ID의 회원이 존재하지 않을 경우, null이 반환된다.")
        @Test
        void returnEmpty_whenUserDoesNotExist() {
            // arrange
            String nonExistUserId = "nonExistUserId";

            // act
            Optional<User> foundUser = userService.findByUserId(nonExistUserId);

            // assert
            assertThat(foundUser).isEmpty();
            verify(userRepository).findByUserId(nonExistUserId);
        }
    }

    @DisplayName("사용자의 포인트를 조회할 때")
    @Nested
    class GetUserPoint {
        @DisplayName("해당 ID의 회원이 존재할 경우, 보유 포인트가 반환된다.")
        @Test
        void returnUsersPoints_whenUserExists() {
            // arrange
            String userId = "testUser";
            User savedUser = userService.register(userId, "test@gmail.com", "1996-08-16", Gender.MALE);

            // act
            Optional<User> foundUser = userService.findByUserId(userId);

            // assert
            assertThat(foundUser)
                    .isPresent()
                    .hasValueSatisfying(user -> {
                        assertThat(user.getUserId()).isEqualTo(savedUser.getUserId());
                        assertThat(user.getPoint()).isEqualTo(0);
                    });
            verify(userRepository).findByUserId(userId);
        }

        @DisplayName("해당 ID 의 회원이 존재하지 않을 경우, null이 반환된다.")
        @Test
        void returnEmpty_whenUserDoesNotExist() {
            // arrange
            String nonExistUserId = "nonExistUserId";

            // act
            Optional<User> foundUser = userService.findByUserId(nonExistUserId);

            // assert
            assertThat(foundUser).isEmpty();
            verify(userRepository).findByUserId(nonExistUserId);
        }
    }

    @DisplayName("포인트를 충전할 때")
    @Nested
    class ChargePoints {
        @DisplayName("존재하지 않는 유저 ID 로 충전을 시도한 경우, 실패한다.")
        @Test
        void failToCharge_whenUserDoesNotExist() {
            // arrange
            String nonExistUserId = "nonExistUserId";

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                userService.chargePoint(nonExistUserId, 1_000);
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.USER_NOT_FOUND);
        }

        @DisplayName("존재하는 유저 ID로 충전을 시도하면 업데이트 된 User 엔티티를 반환한다")
        @Test
        void returnUpdatedUser_whenChargePointToExistingUser() {
            // arrange
            String userId = "testUser";
            User savedUser = userService.register(userId, "test@gmail.com", "1996-08-16", Gender.MALE);

            clearInvocations(userRepository);

            // act
            User updatedUser = userService.chargePoint(userId, 1_000);

            // assert
            assertThat(updatedUser.getUserId()).isEqualTo(savedUser.getUserId());
            assertThat(updatedUser.getPoint()).isEqualTo(1_000);
            verify(userRepository, times(1)).save(any(User.class));
        }
    }
}
