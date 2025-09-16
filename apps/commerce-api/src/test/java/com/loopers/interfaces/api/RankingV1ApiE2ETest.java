package com.loopers.interfaces.api;

import com.loopers.application.product.ProductQuery;
import com.loopers.application.ranking.RankingQuery;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RankingV1ApiE2ETest {
    
    private static final String RANKINGS_ENDPOINT = "/api/v1/rankings";
    private static final String PRODUCTS_ENDPOINT = "/api/v1/products";
    
    private final TestRestTemplate testRestTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final DatabaseCleanUp databaseCleanUp;
    private final RedisCleanUp redisCleanUp;
    
    @Autowired
    public RankingV1ApiE2ETest(TestRestTemplate testRestTemplate,
                               RedisTemplate<String, String> redisTemplate,
                               ProductRepository productRepository,
                               BrandRepository brandRepository,
                               DatabaseCleanUp databaseCleanUp,
                               RedisCleanUp redisCleanUp) {
        this.testRestTemplate = testRestTemplate;
        this.redisTemplate = redisTemplate;
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
        this.databaseCleanUp = databaseCleanUp;
        this.redisCleanUp = redisCleanUp;
    }
    
    private Brand testBrand;
    private Product product1;
    private Product product2;
    private Product product3;
    private final LocalDate testDate = LocalDate.now();
    private final String testDateStr = testDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    
    @BeforeEach
    void setUp() {
        // 테스트 브랜드 생성
        BrandCommand.Create brandCommand = new BrandCommand.Create("테스트브랜드", "브랜드설명");
        testBrand = brandRepository.save(Brand.of(brandCommand));
        
        // 테스트 상품 생성
        ProductCommand.Create productCommand1 = new ProductCommand.Create(
            "상품1", "설명1", BigDecimal.valueOf(10000), 100, testBrand.getId()
        );
        product1 = productRepository.save(Product.of(productCommand1, testBrand));
        
        ProductCommand.Create productCommand2 = new ProductCommand.Create(
            "상품2", "설명2", BigDecimal.valueOf(20000), 200, testBrand.getId()
        );
        product2 = productRepository.save(Product.of(productCommand2, testBrand));
        
        ProductCommand.Create productCommand3 = new ProductCommand.Create(
            "상품3", "설명3", BigDecimal.valueOf(30000), 300, testBrand.getId()
        );
        product3 = productRepository.save(Product.of(productCommand3, testBrand));
        
        // 테스트용 랭킹 데이터를 Redis에 직접 설정
        String rankingKey = "ranking:all:" + testDateStr;
        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();
        zSetOps.add(rankingKey, "product:" + product1.getId(), 100.0);
        zSetOps.add(rankingKey, "product:" + product2.getId(), 80.0);
        zSetOps.add(rankingKey, "product:" + product3.getId(), 60.0);
    }
    
    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
        redisCleanUp.truncateAll();
    }
    
    @DisplayName("랭킹 조회 API 테스트")
    @Nested
    class GetRankings {
        
        @DisplayName("날짜 파라미터 없이 요청하면 오늘 날짜의 랭킹을 반환한다")
        @Test
        void returnTodayRankings_whenNoDateParameter() {
            // arrange & act
            ResponseEntity<ApiResponse<RankingQuery.RankingPageResult>> response = testRestTemplate.exchange(
                RANKINGS_ENDPOINT,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
            );
            
            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().data()).isNotNull(),
                () -> assertThat(response.getBody().data().rankings()).hasSize(3),
                () -> assertThat(response.getBody().data().rankings().get(0).rank()).isEqualTo(1),
                () -> assertThat(response.getBody().data().rankings().get(0).productId()).isEqualTo(product1.getId()),
                () -> assertThat(response.getBody().data().rankings().get(0).productName()).isEqualTo("상품1"),
                () -> assertThat(response.getBody().data().totalElements()).isEqualTo(3),
                () -> assertThat(response.getBody().data().date()).isEqualTo(testDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
            );
        }
        
        @DisplayName("특정 날짜의 랭킹을 조회할 수 있다")
        @Test
        void returnSpecificDateRankings_whenDateParameterProvided() {
            // arrange & act
            String url = RANKINGS_ENDPOINT + "?date=" + testDateStr;
            ResponseEntity<ApiResponse<RankingQuery.RankingPageResult>> response = testRestTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<RankingQuery.RankingPageResult>>() {}
            );
            
            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().rankings()).hasSize(3),
                () -> assertThat(response.getBody().data().date()).isEqualTo(testDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
            );
        }
        
        @DisplayName("페이징 파라미터가 동작한다")
        @Test
        void applyPaging_whenPageParametersProvided() {
            // arrange & act
            String url = RANKINGS_ENDPOINT + "?page=0&size=2";
            ResponseEntity<ApiResponse<RankingQuery.RankingPageResult>> response = testRestTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<RankingQuery.RankingPageResult>>() {}
            );
            
            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().rankings()).hasSize(2),
                () -> assertThat(response.getBody().data().pageSize()).isEqualTo(2),
                () -> assertThat(response.getBody().data().currentPage()).isEqualTo(0),
                () -> assertThat(response.getBody().data().rankings().get(0).rank()).isEqualTo(1),
                () -> assertThat(response.getBody().data().rankings().get(1).rank()).isEqualTo(2)
            );
        }
        
        @DisplayName("두 번째 페이지를 조회하면 순위가 이어진다")
        @Test
        void continueRanking_whenSecondPageRequested() {
            // arrange & act
            String url = RANKINGS_ENDPOINT + "?page=1&size=2";
            ResponseEntity<ApiResponse<RankingQuery.RankingPageResult>> response = testRestTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
            );
            
            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().rankings()).hasSize(1),
                () -> assertThat(response.getBody().data().rankings().get(0).rank()).isEqualTo(3),
                () -> assertThat(response.getBody().data().rankings().get(0).productId()).isEqualTo(product3.getId())
            );
        }
        
        @DisplayName("랭킹 데이터가 없는 날짜를 조회하면 빈 배열을 반환한다")
        @Test
        void returnEmptyArray_whenNoRankingData() {
            // arrange
            LocalDate futureDate = LocalDate.now().plusDays(10);
            String futureDateStr = futureDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            
            // act
            String url = RANKINGS_ENDPOINT + "?date=" + futureDateStr;
            ResponseEntity<ApiResponse<RankingQuery.RankingPageResult>> response = testRestTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
            );
            
            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().rankings()).isEmpty(),
                () -> assertThat(response.getBody().data().totalElements()).isEqualTo(0)
            );
        }
    }
    
    @DisplayName("상품 상세 조회 시 랭킹 정보 포함")
    @Nested
    class GetProductWithRanking {
        
        @DisplayName("랭킹에 있는 상품을 조회하면 순위 정보가 포함된다")
        @Test
        void includeRankingInfo_whenProductInRanking() {
            // arrange
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", "testUser");
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            // act
            ResponseEntity<ApiResponse<ProductQuery.ProductDetailResult>> response = testRestTemplate.exchange(
                PRODUCTS_ENDPOINT + "/" + product1.getId(),
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<>() {}
            );
            
            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().id()).isEqualTo(product1.getId()),
                () -> assertThat(response.getBody().data().name()).isEqualTo("상품1"),
                () -> assertThat(response.getBody().data().ranking()).isNotNull(),
                () -> assertThat(response.getBody().data().ranking().rank()).isEqualTo(1L)
            );
        }
        
        @DisplayName("랭킹에 없는 상품을 조회하면 ranking이 null이다")
        @Test
        void returnNullRanking_whenProductNotInRanking() {
            // arrange
            ProductCommand.Create newProductCommand = new ProductCommand.Create(
                "랭킹없는상품", "설명", BigDecimal.valueOf(50000), 50, testBrand.getId()
            );
            Product newProduct = productRepository.save(Product.of(newProductCommand, testBrand));
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", "testUser");
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            // act
            ResponseEntity<ApiResponse<ProductQuery.ProductDetailResult>> response = testRestTemplate.exchange(
                PRODUCTS_ENDPOINT + "/" + newProduct.getId(),
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<>() {}
            );
            
            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().id()).isEqualTo(newProduct.getId()),
                () -> assertThat(response.getBody().data().ranking()).isNull()
            );
        }
    }
}
