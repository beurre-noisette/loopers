package com.loopers.interfaces.api;

import com.loopers.application.brand.BrandQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@SqlGroup({
    @Sql(scripts = "/brand-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
    @Sql(scripts = "/brand-cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
})
public class BrandV1ApiE2ETest {
    private static final String ENDPOINT = "/api/v1/brands";

    private final TestRestTemplate testRestTemplate;

    @Autowired
    public BrandV1ApiE2ETest(TestRestTemplate testRestTemplate) {
        this.testRestTemplate = testRestTemplate;
    }

    @DisplayName("GET /api/v1/brands/{brandId}")
    @Nested
    class Get {
        @DisplayName("브랜드 ID가 제공되고 해당 브랜드가 있을 경우 브랜드 정보를 반환한다.")
        @ParameterizedTest
        @CsvSource({
                "1, Apple, 'Think Different - 혁신적인 기술로 세상을 바꾸는 브랜드'",
                "2, Samsung, 'Galaxy of Innovation - 끝없는 혁신으로 더 나은 내일을 만드는 브랜드'",
                "3, Nike, 'Just Do It - 스포츠를 통해 모든 사람의 잠재력을 이끌어내는 브랜드'"
        })
        void returnBrand_whenProvidedBrandId(Long brandId, String expectedName, String expectedDescription) {
            // act
            ParameterizedTypeReference<ApiResponse<BrandQuery.BrandQueryResult>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<BrandQuery.BrandQueryResult>> response =
                    testRestTemplate.exchange(
                            ENDPOINT + "/" + brandId,
                            HttpMethod.GET,
                            new HttpEntity<>(null),
                            responseType
                            );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertNotNull(response.getBody()),
                    () -> assertThat(response.getBody().data().id()).isEqualTo(brandId),
                    () -> assertThat(response.getBody().data().name()).isEqualTo(expectedName),
                    () -> assertThat(response.getBody().data().description()).isEqualTo(expectedDescription)
            );
        }
        
        @DisplayName("존재하지 않는 브랜드 ID로 조회하면 404 Not Found 응답을 반환한다.")
        @Test
        void return404_whenBrandNotExists() {
            // arrange
            Long nonExistentBrandId = 999L;

            // act
            ParameterizedTypeReference<BrandQuery.BrandQueryResult> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<BrandQuery.BrandQueryResult> response =
                    testRestTemplate.exchange(
                            ENDPOINT + "/" + nonExistentBrandId,
                            HttpMethod.GET,
                            new HttpEntity<>(null),
                            responseType
                    );

            // assert
            assertTrue(response.getStatusCode().is4xxClientError());
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}
