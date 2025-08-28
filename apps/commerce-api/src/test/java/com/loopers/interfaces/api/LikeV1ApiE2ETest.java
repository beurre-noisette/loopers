package com.loopers.interfaces.api;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandCommand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductCommand;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserCommand;
import com.loopers.domain.user.UserRepository;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LikeV1ApiE2ETest {
    private static final String LIKE_ENDPOINT = "/api/v1/like";

    private final TestRestTemplate testRestTemplate;
    private final DatabaseCleanUp databaseCleanUp;
    private final RedisCleanUp redisCleanUp;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductRepository productRepository;

    private User testUser;
    private Product testProduct;

    @Autowired
    public LikeV1ApiE2ETest(TestRestTemplate testRestTemplate, 
                           DatabaseCleanUp databaseCleanUp,
                           RedisCleanUp redisCleanUp) {
        this.testRestTemplate = testRestTemplate;
        this.databaseCleanUp = databaseCleanUp;
        this.redisCleanUp = redisCleanUp;
    }

    @BeforeEach
    void setUp() {
        testUser = User.of(new UserCommand.Create("testuser", "test@example.com", "1990-01-01", Gender.MALE));
        testUser = userRepository.save(testUser);

        Brand testBrand = Brand.of(new BrandCommand.Create("Test Brand", "Test Description"));
        testBrand = brandRepository.save(testBrand);

        ProductCommand.Create productCommand = new ProductCommand.Create(
            "Test Product", "Test Product Description", BigDecimal.valueOf(10000), 100, testBrand.getId());
        testProduct = Product.of(productCommand, testBrand);
        testProduct = productRepository.save(testProduct);
    }

    @AfterEach
    void tearDown() {
        redisCleanUp.truncateAll();
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("POST /api/v1/like/products/{productId}")
    @Nested
    class CreateLike {
        @DisplayName("상품 좋아요 등록에 성공할 경우, 200 OK와 success 응답을 반환한다.")
        @Test
        void returnSuccess_whenCreateLike() {
            // arrange
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", testUser.getAccountId());
            String endpoint = LIKE_ENDPOINT + "/products/" + testProduct.getId();

            // act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                endpoint, 
                HttpMethod.POST, 
                new HttpEntity<>(null, headers), 
                responseType
            );

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertNotNull(response.getBody()),
                () -> assertEquals("SUCCESS", response.getBody().meta().result().name())
            );
        }

        @DisplayName("존재하지 않는 상품에 좋아요를 등록할 경우에도 좋아요는 성공한다")
        @Test
        void returnSuccess_evenWhenProductNotExists() {
            // arrange
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", testUser.getAccountId());
            String endpoint = LIKE_ENDPOINT + "/products/99999"; // 존재하지 않는 상품 ID

            // act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                endpoint, 
                HttpMethod.POST, 
                new HttpEntity<>(null, headers), 
                responseType
            );

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertNotNull(response.getBody()),
                () -> assertEquals("SUCCESS", response.getBody().meta().result().name())
            );
        }
    }

    @DisplayName("DELETE /api/v1/like/products/{productId}")
    @Nested
    class CancelLike {
        @DisplayName("상품 좋아요 취소에 성공할 경우, 200 OK와 success 응답을 반환한다.")
        @Test
        void returnSuccess_whenCancelLike() {
            // arrange
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", testUser.getAccountId());
            String endpoint = LIKE_ENDPOINT + "/products/" + testProduct.getId();
            
            testRestTemplate.exchange(
                endpoint, 
                HttpMethod.POST, 
                new HttpEntity<>(null, headers), 
                new ParameterizedTypeReference<ApiResponse<Object>>() {}
            );

            // act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                endpoint, 
                HttpMethod.DELETE, 
                new HttpEntity<>(null, headers), 
                responseType
            );

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertNotNull(response.getBody()),
                () -> assertEquals("SUCCESS", response.getBody().meta().result().name())
            );
        }
    }
}
