package com.loopers.application.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandCommand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductCommand;
import com.loopers.domain.product.ProductRepository;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
    "datasource.redis.master.host=localhost",
    "datasource.redis.master.port=9999", // 존재하지 않는 포트로 Redis 연결 실패 유발
    "datasource.redis.replicas[0].host=localhost",
    "datasource.redis.replicas[0].port=9998" // 존재하지 않는 포트
})
@DisplayName("Redis 연결 실패 시나리오 테스트")
class ProductQueryRedisFailureTest {

    @Autowired
    private ProductQuery productQuery;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private BrandRepository brandRepository;
    
    @Autowired
    private DatabaseCleanUp databaseCleanUp;
    
    @Autowired
    private RedisCleanUp redisCleanUp;
    
    private Product testProduct;
    private Brand testBrand;
    
    @BeforeEach
    void setUp() {
        testBrand = Brand.of(new BrandCommand.Create("Test Brand", "Test Description"));
        testBrand = brandRepository.save(testBrand);
        
        ProductCommand.Create productCommand = new ProductCommand.Create(
            "Test Product", "Test Product Description", BigDecimal.valueOf(10000), 100, testBrand.getId());
        testProduct = Product.of(productCommand, testBrand);
        testProduct = productRepository.save(testProduct);
    }
    
    @AfterEach
    void tearDown() {
        try {
            redisCleanUp.truncateAll();
        } catch (Exception e) {
            // Redis 연결 실패 시에도 테스트는 정상 종료되어야 함
        }
        databaseCleanUp.truncateAllTables();
    }
    
    @Test
    @DisplayName("Redis 서버가 다운되어도 상품 상세 조회는 DB에서 정상 동작")
    void whenRedisIsDown_shouldStillQueryFromDB() {
        // arrange
        Long productId = testProduct.getId();
        
        // act - Redis가 연결되지 않는 상황에서 상품 조회
        ProductQuery.ProductDetailResult result = productQuery.getProductDetailWithCache(productId);
        
        // assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(productId);
        assertThat(result.name()).isEqualTo("Test Product");
        assertThat(result.description()).isEqualTo("Test Product Description");
        assertThat(result.price()).isEqualByComparingTo(BigDecimal.valueOf(10000));
        assertThat(result.stock()).isEqualTo(100);
        assertThat(result.brand()).isNotNull();
        assertThat(result.brand().name()).isEqualTo("Test Brand");
        assertThat(result.likeCount()).isEqualTo(0);
    }
    
    @Test
    @DisplayName("Redis 연결 실패 시에도 여러 상품 조회 가능")
    void whenRedisIsDown_shouldQueryMultipleProductsFromDB() {
        // arrange
        ProductCommand.Create secondProductCommand = new ProductCommand.Create(
            "Second Product", "Second Product Description", BigDecimal.valueOf(20000), 50, testBrand.getId());
        Product secondProduct = Product.of(secondProductCommand, testBrand);
        secondProduct = productRepository.save(secondProduct);
        
        Long firstId = testProduct.getId();
        Long secondId = secondProduct.getId();
        
        // act - Redis 연결 실패 상황에서 여러 상품 조회
        ProductQuery.ProductDetailResult result1 = productQuery.getProductDetailWithCache(firstId);
        ProductQuery.ProductDetailResult result2 = productQuery.getProductDetailWithCache(secondId);
        
        // assert
        assertThat(result1).isNotNull();
        assertThat(result1.id()).isEqualTo(firstId);
        assertThat(result1.name()).isEqualTo("Test Product");
        
        assertThat(result2).isNotNull();
        assertThat(result2.id()).isEqualTo(secondId);
        assertThat(result2.name()).isEqualTo("Second Product");
    }
    
    @Test
    @DisplayName("Redis 연결 실패 시에도 캐시 무효화 메서드는 예외 없이 동작")
    void whenRedisIsDown_cacheEvictionShouldNotThrowException() {
        // arrange
        Long productId = testProduct.getId();
        
        // act - 예외가 발생하지 않아야 함
        productQuery.evictProductDetailCache(productId);
        
        // assert
        ProductQuery.ProductDetailResult result = productQuery.getProductDetailWithCache(productId);
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(productId);
    }
    
}
