package com.loopers.application.like;

import com.loopers.application.product.ProductQuery;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandCommand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.like.LikeCommand;
import com.loopers.domain.like.TargetType;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductCommand;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserCommand;
import com.loopers.domain.user.UserRepository;
import com.loopers.domain.user.Gender;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("좋아요 + 캐시 무효화 통합 테스트")
class LikeCacheIntegrationTest {

    @Autowired
    private LikeFacade likeFacade;
    
    @Autowired
    private ProductQuery productQuery;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private BrandRepository brandRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private DatabaseCleanUp databaseCleanUp;
    
    @Autowired
    private RedisCleanUp redisCleanUp;
    
    private Product testProduct;
    private User testUser;
    
    @BeforeEach
    void setUp() {
        Brand testBrand = Brand.of(new BrandCommand.Create("Test Brand", "Test Description"));
        testBrand = brandRepository.save(testBrand);
        
        ProductCommand.Create productCommand = new ProductCommand.Create(
            "Test Product", "Test Product Description", BigDecimal.valueOf(10000), 100, testBrand.getId());
        testProduct = Product.of(productCommand, testBrand);
        testProduct = productRepository.save(testProduct);
        
        testUser = User.of(new UserCommand.Create("testuser", "test@example.com", "1990-01-01", Gender.MALE));
        testUser = userRepository.save(testUser);
    }
    
    @AfterEach
    void tearDown() {
        redisCleanUp.truncateAll();
        databaseCleanUp.truncateAllTables();
    }
    
    @Test
    @DisplayName("좋아요 등록 후 캐시된 상품 조회 시 like_count가 반영된다.")
    void whenLikeProduct_cacheIsEvictedAndLikeCountUpdated() throws InterruptedException {
        // arrange
        Long productId = testProduct.getId();
        String userId = testUser.getAccountId();
        
        // 1차 조회로 캐시 저장 (like_count = 0)
        ProductQuery.ProductDetailResult firstResult = productQuery.getProductDetailWithCache(productId);
        assertThat(firstResult.likeCount()).isEqualTo(0);
        
        // act - 좋아요 등록
        LikeCommand.Create likeCommand = new LikeCommand.Create(userId, TargetType.PRODUCT, productId);
        
        likeFacade.createLike(likeCommand);
        
        // 비동기 집계 처리 완료 대기
        Thread.sleep(500);
        
        ProductQuery.ProductDetailResult secondResult = productQuery.getProductDetailWithCache(productId);

        // assert - 캐시가 무효화되어 최신 like_count가 반영되어야 함
        assertThat(secondResult.likeCount()).isEqualTo(1);
        assertThat(secondResult.id()).isEqualTo(productId);
    }
    
    @Test
    @DisplayName("좋아요 취소 후 캐시된 상품 조회 시 like_count가 반영된다.")
    void whenCancelLike_cacheIsEvictedAndLikeCountUpdated() throws InterruptedException {
        // arrange
        Long productId = testProduct.getId();
        String userId = testUser.getAccountId();
        
        LikeCommand.Create likeCommand = new LikeCommand.Create(userId, TargetType.PRODUCT, productId);
        
        likeFacade.createLike(likeCommand);
        
        // 비동기 집계 처리 완료 대기
        Thread.sleep(500);
        
        // 캐시에 like_count = 1인 상태로 저장
        ProductQuery.ProductDetailResult beforeCancel = productQuery.getProductDetailWithCache(productId);

        // act - 좋아요 취소
        likeFacade.cancelLike(likeCommand);
        
        // 비동기 집계 처리 완료 대기
        Thread.sleep(500);
        
        ProductQuery.ProductDetailResult afterCancel = productQuery.getProductDetailWithCache(productId);
        
        // assert - 캐시가 무효화되어 like_count = 0으로 반영되어야 함
        assertThat(beforeCancel.likeCount()).isEqualTo(1);
        assertThat(afterCancel.likeCount()).isEqualTo(0);
        assertThat(afterCancel.id()).isEqualTo(productId);
    }
    
    @Test
    @DisplayName("여러 사용자가 좋아요 등록 시 캐시 무효화 정상 동작한다.")
    void whenMultipleUsersLike_cacheEvictionWorksCorrectly() throws InterruptedException {
        // arrange
        Long productId = testProduct.getId();
        
        // 추가 사용자 생성
        User user2 = User.of(new UserCommand.Create("testuser2", "test2@example.com", "1995-05-05", Gender.FEMALE));
        user2 = userRepository.save(user2);
        
        User user3 = User.of(new UserCommand.Create("testuser3", "test3@example.com", "1988-12-12", Gender.MALE));
        user3 = userRepository.save(user3);
        
        // act - 3명이 순차적으로 좋아요
        likeFacade.createLike(new LikeCommand.Create(testUser.getAccountId(), TargetType.PRODUCT, productId));
        Thread.sleep(500); // 비동기 집계 처리 완료 대기
        ProductQuery.ProductDetailResult result1 = productQuery.getProductDetailWithCache(productId);

        likeFacade.createLike(new LikeCommand.Create(user2.getAccountId(), TargetType.PRODUCT, productId));
        Thread.sleep(500); // 비동기 집계 처리 완료 대기
        ProductQuery.ProductDetailResult result2 = productQuery.getProductDetailWithCache(productId);

        likeFacade.createLike(new LikeCommand.Create(user3.getAccountId(), TargetType.PRODUCT, productId));
        Thread.sleep(500); // 비동기 집계 처리 완료 대기
        ProductQuery.ProductDetailResult finalResult = productQuery.getProductDetailWithCache(productId);
        
        // assert - 최종 확인
        assertThat(result1.likeCount()).isEqualTo(1);
        assertThat(result2.likeCount()).isEqualTo(2);
        assertThat(finalResult.likeCount()).isEqualTo(3);
    }
}
