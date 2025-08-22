package com.loopers.interfaces.api;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserCommand;
import com.loopers.domain.user.UserRepository;
import com.loopers.interfaces.api.user.UserV1Dto;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserV1ApiE2ETest {
    private static final String ENDPOINT = "/api/v1/users";

    private final TestRestTemplate testRestTemplate;
    private final DatabaseCleanUp databaseCleanUp;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    public UserV1ApiE2ETest(TestRestTemplate testRestTemplate, DatabaseCleanUp databaseCleanUp) {
        this.testRestTemplate = testRestTemplate;
        this.databaseCleanUp = databaseCleanUp;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("POST /api/v1/users")
    @Nested
    class RegisterUser {
        @DisplayName("회원 가입이 성공할 경우, 생성된 유저 정보를 응답으로 반환한다.")
        @Test
        void returnUser_whenRegisterUser() {
            // arrange
            UserV1Dto.UserSignUpRequest request = new UserV1Dto.UserSignUpRequest(
                    "testUser",
                    "test@gmail.com",
                    "1996-08-16",
                    Gender.MALE
            );

            // act
            ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response =
                    testRestTemplate.exchange(ENDPOINT, HttpMethod.POST, new HttpEntity<>(request), responseType);

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertNotNull(response.getBody()),
                    () -> assertEquals(request.accountId(), response.getBody().data().accountId()),
                    () -> assertEquals(request.email(), response.getBody().data().email()),
                    () -> {
                        LocalDate expectedDate = LocalDate.parse(request.birthDate());
                        assertThat(response.getBody().data().birthDate()).isEqualTo(expectedDate);
                    },
                    () -> assertEquals(request.gender(), response.getBody().data().gender())
            );
        }

        @DisplayName("회원 가입 시에 성별이 없을 경우, `400 Bad Request` 응답을 반환한다.")
        @Test
        void return400BadRequest_whenNotGivenGenderForRegisterUser() {
            // arrange
            UserV1Dto.UserSignUpRequest request = new UserV1Dto.UserSignUpRequest(
                    "testUser",
                    "test@gmail.com",
                    "1996-08-16",
                    null
            );

            // act
            ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response =
                    testRestTemplate.exchange(ENDPOINT, HttpMethod.POST, new HttpEntity<>(request), responseType);

            // assert
            assertTrue(response.getStatusCode().is4xxClientError());
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }

    @DisplayName("GET /api/v1/users")
    @Nested
    class GetUser {
        @DisplayName("내 정보 조회에 성공할 경우, 해당하는 유저 정보를 응답으로 반환한다.")
        @Test
        void returnUser_whenGetUser() {
            // arrange
            String accountId = "testUser";
            User savedUser = userRepository.save(User.of(new UserCommand.Create(accountId, "test@gmail.com", "1996-08-16", Gender.MALE)));
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", accountId);

            // act
            ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response =
                    testRestTemplate.exchange(
                            ENDPOINT + "/me",
                            HttpMethod.GET,
                            new HttpEntity<>(null, headers),
                            responseType);

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertEquals(accountId, response.getBody().data().accountId()),
                    () -> assertEquals(savedUser.getEmail(), response.getBody().data().email())
            );
        }

        @DisplayName("존재하지 않는 ID로 조회할 경우, 404 Not Found 응답을 반환한다.")
        @Test
        void return404NotFound_whenProvidedNonExistsUserId() {
            String nonExistentUserId = "nonExistent";
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", nonExistentUserId);

            // act
            ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response =
                    testRestTemplate.exchange(
                            ENDPOINT + "/me",
                            HttpMethod.GET,
                            new HttpEntity<>(null, headers),
                            responseType);

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}
