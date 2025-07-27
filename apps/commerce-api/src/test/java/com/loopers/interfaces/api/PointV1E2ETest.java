package com.loopers.interfaces.api;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserCommand;
import com.loopers.domain.user.UserRepository;
import com.loopers.interfaces.api.point.PointV1Dto;
import com.loopers.interfaces.api.user.UserV1Dto;
import com.loopers.support.error.ErrorType;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class PointV1E2ETest {
    private static final String ENDPOINT = "/api/v1/points";

    private final TestRestTemplate testRestTemplate;
    private final DatabaseCleanUp databaseCleanUp;
    private final UserRepository userRepository;

    @Autowired
    public PointV1E2ETest(TestRestTemplate testRestTemplate, DatabaseCleanUp databaseCleanUp, UserRepository userRepository) {
        this.testRestTemplate = testRestTemplate;
        this.databaseCleanUp = databaseCleanUp;
        this.userRepository = userRepository;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("GET /api/v1/points")
    @Nested
    class GetPoints {
        @DisplayName("포인트 조회에 성공할 경우, 보유 포인트를 응답으로 반환한다.")
        @Test
        void returnUserPoint_whenUserExists() {
            // arrange
            String userId = "testUser";
            userRepository.save(User.of(new UserCommand.Create(userId, "test@gmail.com", "1996-08-16", Gender.MALE)));
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", userId);

            // act
            ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response =
                    testRestTemplate.exchange(
                            ENDPOINT,
                            HttpMethod.GET,
                            new HttpEntity<>(null, headers),
                            responseType);

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertEquals(0, response.getBody().data().point())
            );
        }

        @DisplayName("`X-USER-ID` 헤더가 없을 경우, `400 Bad Request` 응답을 반환한다.")
        @Test
        void return400BadRequest_whenNotGivenUserId() {
            // arrange


            // act
            ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response = testRestTemplate.exchange(
                    ENDPOINT,
                    HttpMethod.GET,
                    new HttpEntity<>(null),
                    responseType);

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @DisplayName("포인트 충전 요청이 왔을 때")
    @Nested
    class ChargePoint {
        @DisplayName("존재하는 유저가 1000원을 충전할 경우, 충전된 보유 총량을 응답으로 반환한다.")
        @Test
        void returnUserIdAndPoint_whenUserExists() {
            // arrange
            String userId = "testUser";
            userRepository.save(User.of(new UserCommand.Create(userId, "test@gmail.com", "1996-08-16", Gender.MALE)));

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", userId);

            UserV1Dto.UserPointChargeRequest request = new UserV1Dto.UserPointChargeRequest(1_000);

            // act
            ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response =
                    testRestTemplate.exchange(
                            ENDPOINT,
                            HttpMethod.POST,
                            new HttpEntity<>(request, headers),
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody().data().point()).isEqualTo(1_000)
            );
        }

        @DisplayName("존재하지 않는 유저로 요청할 경우, `404 Not Found` 응답을 반환한다.")
        @Test
        void return404NotFound_whenProvidedNonExistsUserId() {
            // arrange
            String nonExistsUserId = "nonExistsUser";
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", nonExistsUserId);
            UserV1Dto.UserPointChargeRequest request = new UserV1Dto.UserPointChargeRequest(1_000);

            // act
            ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response = testRestTemplate.exchange(
                    ENDPOINT,
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    responseType
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody().meta().errorCode()).isEqualTo(ErrorType.USER_NOT_FOUND.getCode());
        }

        @DisplayName("0원을 충전 요청할 경우, `400 Bad Request` 응답을 반환한다.")
        @Test
        void return400BadRequest_whenProvidedZeroAmount() {
            // arrange
            String userId = "testUser";
            userRepository.save(User.of(new UserCommand.Create(userId, "test@gmail.com", "1996-08-16", Gender.MALE)));
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", userId);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            UserV1Dto.UserPointChargeRequest request = new UserV1Dto.UserPointChargeRequest(0);

            // act
            ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response = testRestTemplate.exchange(
                    ENDPOINT,
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    responseType
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().meta().errorCode()).isEqualTo(ErrorType.BAD_REQUEST.getCode());
        }

        @DisplayName("음수로 충전 요청할 경우, `400 Bad Request` 응답을 반환한다.")
        @Test
        void return400BadRequest_whenProvidedNegativeAmount() {
            // arrange
            String userId = "testUser";
            userRepository.save(User.of(new UserCommand.Create(userId, "test@gmail.com", "1996-08-16", Gender.MALE)));
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", userId);
            headers.setContentType(MediaType.APPLICATION_JSON);

            UserV1Dto.UserPointChargeRequest request = new UserV1Dto.UserPointChargeRequest(-100);

            // act
            ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response = testRestTemplate.exchange(
                    ENDPOINT,
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    responseType
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().meta().errorCode()).isEqualTo(ErrorType.BAD_REQUEST.getCode());
        }

        @DisplayName("null 값으로 충전 요청할 경우, `400 Bad Request` 응답을 반환한다.")
        @Test
        void return400BadRequest_whenProvidedNullAmount() {
            // arrange
            String userId = "testUser";
            userRepository.save(User.of(new UserCommand.Create(userId, "test@gmail.com", "1996-08-16", Gender.MALE)));
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", userId);
            headers.setContentType(MediaType.APPLICATION_JSON);

            String request = "{\"amount\": null}";

            // act
            ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response = testRestTemplate.exchange(
                    ENDPOINT,
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    responseType
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().meta().errorCode()).isEqualTo(ErrorType.BAD_REQUEST.getCode());
        }
    }
}
