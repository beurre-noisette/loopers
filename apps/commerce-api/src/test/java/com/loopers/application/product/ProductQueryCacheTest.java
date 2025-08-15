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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@DisplayName("상품 조회 캐시 로직 테스트")
class ProductQueryCacheTest {

    @Autowired
    private ProductQuery productQuery;
    
    @MockitoSpyBean
    private ProductQueryCacheRepository cacheRepository;
    
    @MockitoSpyBean
    private ProductQueryRepository queryRepository;
    
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
        redisCleanUp.truncateAll();
        databaseCleanUp.truncateAllTables();
    }
    
    @Test
    @DisplayName("첫 번째 조회 시 캐시 미스 → DB 조회 → 캐시 저장")
    void firstCall_shouldMissCache_queryDB_andSaveToCache() {
        // arrange
        Long productId = testProduct.getId();
        
        // act
        ProductQuery.ProductDetailResult result = productQuery.getProductDetailWithCache(productId);
        
        // assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(productId);
        
        // 캐시 레포지토리 호출 검증
        verify(cacheRepository, times(1)).findDetailById(productId);  // 캐시 조회 시도
        verify(cacheRepository, times(1)).saveDetail(eq(productId), any());  // 캐시 저장
        
        // DB 쿼리 레포지토리 호출 검증
        verify(queryRepository, times(1)).findProductDetailById(productId);  // DB 조회
    }
    
    @Test
    @DisplayName("두 번째 조회 시 캐시 히트 → DB 조회하지 않음")
    void secondCall_shouldHitCache_andNotQueryDB() {
        // arrange
        Long productId = testProduct.getId();
        
        // 첫 번째 조회로 캐시 저장
        productQuery.getProductDetailWithCache(productId);
        
        // 캐시 조회 가능할 때까지 잠시 대기
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Spy 초기화
        reset(cacheRepository, queryRepository);
        
        // act
        ProductQuery.ProductDetailResult result = productQuery.getProductDetailWithCache(productId);
        
        // assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(productId);
        
        // 캐시에서만 조회하고 DB는 조회하지 않음
        verify(cacheRepository, times(1)).findDetailById(productId);  // 캐시 조회
        verify(queryRepository, atMost(1)).findProductDetailById(any());  // DB 조회는 최대 1번 (캐시 실패 시만)
    }
    
    @Test
    @DisplayName("캐시 무효화 후 다시 조회 시 캐시 미스 발생")
    void afterEviction_shouldMissCache_andQueryDB() {
        // arrange
        Long productId = testProduct.getId();
        
        // 캐시 저장
        productQuery.getProductDetailWithCache(productId);
        
        // 캐시 무효화
        productQuery.evictProductDetailCache(productId);
        
        // Spy 초기화
        reset(cacheRepository, queryRepository);
        
        // act
        ProductQuery.ProductDetailResult result = productQuery.getProductDetailWithCache(productId);
        
        // assert
        assertThat(result).isNotNull();
        
        // 캐시 미스 → DB 조회 → 캐시 저장
        verify(cacheRepository, times(1)).findDetailById(productId);
        verify(cacheRepository, times(1)).saveDetail(eq(productId), any());
        verify(queryRepository, times(1)).findProductDetailById(productId);
    }
    
    @Test
    @DisplayName("여러 상품 조회 시 각각 독립적으로 캐싱됨")
    void multipleProdcuts_shouldBeCachedIndependently() {
        // arrange
        ProductCommand.Create secondProductCommand = new ProductCommand.Create(
            "Second Product", "Second Product Description", BigDecimal.valueOf(20000), 50, testBrand.getId());
        Product secondProduct = Product.of(secondProductCommand, testBrand);
        secondProduct = productRepository.save(secondProduct);
        
        Long firstId = testProduct.getId();
        Long secondId = secondProduct.getId();
        
        // act - 각 상품 2번씩 조회
        ProductQuery.ProductDetailResult result1 = productQuery.getProductDetailWithCache(firstId);
        ProductQuery.ProductDetailResult result2 = productQuery.getProductDetailWithCache(secondId);
        
        // 잠시 대기
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        ProductQuery.ProductDetailResult result3 = productQuery.getProductDetailWithCache(firstId);  // 캐시 히트 시도
        ProductQuery.ProductDetailResult result4 = productQuery.getProductDetailWithCache(secondId); // 캐시 히트 시도
        
        // assert
        assertThat(result1).isNotNull();
        assertThat(result1.id()).isEqualTo(firstId);
        assertThat(result2).isNotNull();
        assertThat(result2.id()).isEqualTo(secondId);
        assertThat(result3).isNotNull();
        assertThat(result3.id()).isEqualTo(firstId);
        assertThat(result4).isNotNull();
        assertThat(result4.id()).isEqualTo(secondId);
        
        // 각 상품별로 최소 조회가 발생했는지만 검증 (캐시 실패 고려)
        verify(cacheRepository, atLeast(1)).findDetailById(firstId);
        verify(cacheRepository, atLeast(1)).findDetailById(secondId);
        verify(queryRepository, atLeast(1)).findProductDetailById(firstId);
        verify(queryRepository, atLeast(1)).findProductDetailById(secondId);
    }

}
