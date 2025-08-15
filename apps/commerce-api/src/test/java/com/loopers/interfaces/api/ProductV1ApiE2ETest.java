package com.loopers.interfaces.api;

import com.loopers.application.product.ProductQuery;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandCommand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductCommand;
import com.loopers.domain.product.ProductRepository;
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
class ProductV1ApiE2ETest {
    private static final String ENDPOINT = "/api/v1/products";

    private final TestRestTemplate testRestTemplate;
    private final DatabaseCleanUp databaseCleanUp;
    private final RedisCleanUp redisCleanUp;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductRepository productRepository;

    private Brand testBrand;
    private Product testProduct1;
    private Product testProduct2;

    @Autowired
    public ProductV1ApiE2ETest(TestRestTemplate testRestTemplate, 
                              DatabaseCleanUp databaseCleanUp,
                              RedisCleanUp redisCleanUp) {
        this.testRestTemplate = testRestTemplate;
        this.databaseCleanUp = databaseCleanUp;
        this.redisCleanUp = redisCleanUp;
    }

    @BeforeEach
    void setUp() {
        testBrand = Brand.of(new BrandCommand.Create("Test Brand", "Test Brand Description"));
        testBrand = brandRepository.save(testBrand);

        ProductCommand.Create product1Command = new ProductCommand.Create(
            "Test Product 1", "Test Product 1 Description", BigDecimal.valueOf(10000), 100, testBrand.getId());
        testProduct1 = Product.of(product1Command, testBrand);
        testProduct1 = productRepository.save(testProduct1);

        ProductCommand.Create product2Command = new ProductCommand.Create(
            "Test Product 2", "Test Product 2 Description", BigDecimal.valueOf(15000), 50, testBrand.getId());
        testProduct2 = Product.of(product2Command, testBrand);
        testProduct2 = productRepository.save(testProduct2);
    }

    @AfterEach
    void tearDown() {
        redisCleanUp.truncateAll();
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("GET /api/v1/products")
    @Nested
    class GetProducts {
        @DisplayName("상품 목록 조회에 성공할 경우, 200 OK와 상품 목록을 반환한다.")
        @Test
        void returnProductList_whenGetProducts() {
            // act
            ParameterizedTypeReference<ApiResponse<ProductQuery.ProductListResult>> responseType = 
                new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<ProductQuery.ProductListResult>> response = testRestTemplate.exchange(
                ENDPOINT, 
                HttpMethod.GET, 
                null, 
                responseType
            );

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertNotNull(response.getBody()),
                () -> assertEquals("SUCCESS", response.getBody().meta().result().name()),
                () -> assertNotNull(response.getBody().data().products()),
                () -> assertTrue(response.getBody().data().products().size() >= 2),
                () -> assertEquals(0, response.getBody().data().currentPage()),
                () -> assertEquals(20, response.getBody().data().pageSize())
            );
        }

        @DisplayName("브랜드 ID로 필터링한 상품 목록 조회에 성공할 경우, 해당 브랜드의 상품만 반환한다.")
        @Test
        void returnFilteredProductList_whenGetProductsWithBrandId() {
            // act
            ParameterizedTypeReference<ApiResponse<ProductQuery.ProductListResult>> responseType = 
                new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<ProductQuery.ProductListResult>> response = testRestTemplate.exchange(
                ENDPOINT + "?brandId=" + testBrand.getId(), 
                HttpMethod.GET, 
                null, 
                responseType
            );

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertNotNull(response.getBody()),
                () -> assertEquals("SUCCESS", response.getBody().meta().result().name()),
                () -> assertNotNull(response.getBody().data().products()),
                () -> assertTrue(response.getBody().data().products().size() >= 2),
                () -> assertTrue(response.getBody().data().products().stream()
                    .allMatch(product -> product.brandId().equals(testBrand.getId())))
            );
        }

        @DisplayName("가격 오름차순 정렬로 상품 목록 조회에 성공할 경우, 가격이 낮은 순서로 반환한다.")
        @Test
        void returnSortedProductList_whenGetProductsWithPriceAscSort() {
            // act
            ParameterizedTypeReference<ApiResponse<ProductQuery.ProductListResult>> responseType = 
                new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<ProductQuery.ProductListResult>> response = testRestTemplate.exchange(
                ENDPOINT + "?sort=price_asc", 
                HttpMethod.GET, 
                null, 
                responseType
            );

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertNotNull(response.getBody()),
                () -> assertEquals("SUCCESS", response.getBody().meta().result().name()),
                () -> assertNotNull(response.getBody().data().products()),
                () -> assertTrue(response.getBody().data().products().size() >= 2)
            );
        }

        @DisplayName("페이지네이션이 적용된 상품 목록 조회에 성공할 경우, 지정된 페이지와 크기로 반환한다.")
        @Test
        void returnPaginatedProductList_whenGetProductsWithPagination() {
            // act
            ParameterizedTypeReference<ApiResponse<ProductQuery.ProductListResult>> responseType = 
                new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<ProductQuery.ProductListResult>> response = testRestTemplate.exchange(
                ENDPOINT + "?page=0&size=1", 
                HttpMethod.GET, 
                null, 
                responseType
            );

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertNotNull(response.getBody()),
                () -> assertEquals("SUCCESS", response.getBody().meta().result().name()),
                () -> assertNotNull(response.getBody().data().products()),
                () -> assertEquals(1, response.getBody().data().pageSize()),
                () -> assertEquals(0, response.getBody().data().currentPage())
            );
        }
    }

    @DisplayName("GET /api/v1/products/{productId}")
    @Nested
    class GetProductDetail {
        @DisplayName("상품 상세 조회(캐시 사용)에 성공할 경우, 200 OK와 상품 상세 정보를 반환한다.")
        @Test
        void returnProductDetail_whenGetProductDetailWithCache() {
            // act
            ParameterizedTypeReference<ApiResponse<ProductQuery.ProductDetailResult>> responseType = 
                new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<ProductQuery.ProductDetailResult>> response = testRestTemplate.exchange(
                ENDPOINT + "/" + testProduct1.getId(), 
                HttpMethod.GET, 
                null, 
                responseType
            );

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertNotNull(response.getBody()),
                () -> assertEquals("SUCCESS", response.getBody().meta().result().name()),
                () -> assertNotNull(response.getBody().data()),
                () -> assertEquals(testProduct1.getId(), response.getBody().data().id()),
                () -> assertEquals(testProduct1.getName(), response.getBody().data().name()),
                () -> assertEquals(0, testProduct1.getPrice().compareTo(response.getBody().data().price())),
                () -> assertEquals(testProduct1.getStock(), response.getBody().data().stock()),
                () -> assertNotNull(response.getBody().data().brand()),
                () -> assertEquals(testBrand.getId(), response.getBody().data().brand().id()),
                () -> assertEquals(testBrand.getName(), response.getBody().data().brand().name())
            );
        }

        @DisplayName("존재하지 않는 상품 상세 조회 시, 404 Not Found 응답을 반환한다.")
        @Test
        void return404NotFound_whenGetNonExistentProductDetail() {
            // act
            ParameterizedTypeReference<ApiResponse<ProductQuery.ProductDetailResult>> responseType = 
                new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<ProductQuery.ProductDetailResult>> response = testRestTemplate.exchange(
                ENDPOINT + "/99999", // 존재하지 않는 상품 ID
                HttpMethod.GET, 
                null, 
                responseType
            );

            // assert
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }
    }

    @DisplayName("GET /api/v1/products/{productId}/no-cache")
    @Nested
    class GetProductDetailNoCache {
        @DisplayName("상품 상세 조회(캐시 미사용)에 성공할 경우, 200 OK와 상품 상세 정보를 반환한다.")
        @Test
        void returnProductDetail_whenGetProductDetailNoCache() {
            // act
            ParameterizedTypeReference<ApiResponse<ProductQuery.ProductDetailResult>> responseType = 
                new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<ProductQuery.ProductDetailResult>> response = testRestTemplate.exchange(
                ENDPOINT + "/" + testProduct1.getId() + "/no-cache", 
                HttpMethod.GET, 
                null, 
                responseType
            );

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertNotNull(response.getBody()),
                () -> assertEquals("SUCCESS", response.getBody().meta().result().name()),
                () -> assertNotNull(response.getBody().data()),
                () -> assertEquals(testProduct1.getId(), response.getBody().data().id()),
                () -> assertEquals(testProduct1.getName(), response.getBody().data().name()),
                () -> assertEquals(0, testProduct1.getPrice().compareTo(response.getBody().data().price())),
                () -> assertEquals(testProduct1.getStock(), response.getBody().data().stock()),
                () -> assertNotNull(response.getBody().data().brand()),
                () -> assertEquals(testBrand.getId(), response.getBody().data().brand().id()),
                () -> assertEquals(testBrand.getName(), response.getBody().data().brand().name())
            );
        }

        @DisplayName("존재하지 않는 상품 상세 조회(캐시 미사용) 시, 404 Not Found 응답을 반환한다.")
        @Test
        void return404NotFound_whenGetNonExistentProductDetailNoCache() {
            // act
            ParameterizedTypeReference<ApiResponse<ProductQuery.ProductDetailResult>> responseType = 
                new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<ProductQuery.ProductDetailResult>> response = testRestTemplate.exchange(
                ENDPOINT + "/99999/no-cache", // 존재하지 않는 상품 ID
                HttpMethod.GET, 
                null, 
                responseType
            );

            // assert
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }
    }
}
