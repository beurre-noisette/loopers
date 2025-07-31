package com.loopers.infrastructure.product.query;

import com.loopers.application.product.ProductQueryRepository;
import com.loopers.application.product.ProductSortType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("상품 조회 Repository 통합 테스트")
@SpringBootTest
@Sql(value = "/product-query-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ProductQueryRepositoryImplTest {
    
    @Autowired
    private ProductQueryRepository productQueryRepository;
    
    @Autowired
    private DatabaseCleanUp databaseCleanUp;
    
    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }
    
    @DisplayName("전체 상품을 최신순으로 조회한다")
    @Test
    void findProductsOrderByLatest() {
        // arrange
        PageRequest pageable = PageRequest.of(0, 10);
        
        // act
        Page<ProductQueryRepository.ProductQueryData> result = 
            productQueryRepository.findProducts(null, ProductSortType.LATEST, pageable);
        
        // assert
        assertThat(result.getTotalElements()).isEqualTo(6);
        assertThat(result.getContent()).hasSize(6);
        assertThat(result.getContent().get(0).id()).isEqualTo(6L);
        assertThat(result.getContent().get(1).id()).isEqualTo(5L);
        assertThat(result.getContent().get(2).id()).isEqualTo(4L);
    }
    
    @DisplayName("특정 브랜드의 상품만 조회한다")
    @Test
    void findSpecificProductByBrandId() {
        // arrange
        Long brandId = 1L;
        PageRequest pageable = PageRequest.of(0, 10);
        
        // act
        Page<ProductQueryRepository.ProductQueryData> result = 
            productQueryRepository.findProducts(brandId, ProductSortType.LATEST, pageable);
        
        // assert
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent())
            .hasSize(3)
            .allMatch(product -> product.brandId().equals(brandId))
            .allMatch(product -> product.brandName().equals("나이키"));
    }
    
    @DisplayName("상품을 가격 오름차순으로 정렬하여 조회한다.")
    @Test
    void findProductsOrderByPriceAsc() {
        // arrange
        PageRequest pageable = PageRequest.of(0, 10);
        
        // act
        Page<ProductQueryRepository.ProductQueryData> result = 
            productQueryRepository.findProducts(null, ProductSortType.PRICE_ASC, pageable);
        
        // assert
        assertThat(result.getContent()).hasSize(6);
        
        // 가격 오름차순 확인
        assertThat(result.getContent().get(0).price()).isEqualByComparingTo("90000");
        assertThat(result.getContent().get(1).price()).isEqualByComparingTo("100000");
        assertThat(result.getContent().get(2).price()).isEqualByComparingTo("120000");
    }
    
    @DisplayName("페이징이 올바르게 동작한다")
    @Test
    void findProductsWithPaging() {
        // arrange
        PageRequest firstPage = PageRequest.of(0, 2);
        PageRequest secondPage = PageRequest.of(1, 2);
        
        // act
        Page<ProductQueryRepository.ProductQueryData> firstResult = 
            productQueryRepository.findProducts(null, ProductSortType.LATEST, firstPage);
        Page<ProductQueryRepository.ProductQueryData> secondResult = 
            productQueryRepository.findProducts(null, ProductSortType.LATEST, secondPage);
        
        // assert
        assertThat(firstResult.getTotalElements()).isEqualTo(6);
        assertThat(firstResult.getTotalPages()).isEqualTo(3);
        assertThat(firstResult.getContent()).hasSize(2);
        
        assertThat(secondResult.getContent()).hasSize(2);
        assertThat(secondResult.getNumber()).isEqualTo(1);
    }
    
    @DisplayName("상품의 식별자로 상품 상세 정보를 조회한다")
    @Test
    void findProductDetailById() {
        // arrange
        Long productId = 1L;
        
        // act
        Optional<ProductQueryRepository.ProductDetailQueryData> result = 
            productQueryRepository.findProductDetailById(productId);
        
        // assert
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(1L);
        assertThat(result.get().name()).isEqualTo("나이키 에어맥스");
        assertThat(result.get().brandName()).isEqualTo("나이키");
        assertThat(result.get().brandDescription()).isEqualTo("스포츠 브랜드");
    }
    
    @DisplayName("존재하지 않는 상품 조회 시 빈 Optional을 반환한다")
    @Test
    void returnEmpty_whenProductDoesNotExist() {
        // arrange
        Long productId = 999L;
        
        // act
        Optional<ProductQueryRepository.ProductDetailQueryData> result = 
            productQueryRepository.findProductDetailById(productId);
        
        // assert
        assertThat(result).isEmpty();
    }

    @DisplayName("상품 목록 조회 시 좋아요 수가 포함된다.")
    @Test
    void findProductsWithLikeCount() {
        // arrange
        PageRequest pageable = PageRequest.of(0, 10);

        // act
        Page<ProductQueryRepository.ProductQueryData> result =
                productQueryRepository.findProducts(
                        null,
                        ProductSortType.LATEST,
                        pageable);

        Map<Long, ProductQueryRepository.ProductQueryData> productMap =
                result.getContent().stream().collect(
                        Collectors.toMap(
                                ProductQueryRepository.ProductQueryData::id,
                                Function.identity()
                        ));

        // assert
        assertAll (
                () -> assertThat(result.getContent()).hasSize(6),
                () -> assertThat(productMap.get(1L).likeCount()).isEqualTo(3L),
                () -> assertThat(productMap.get(2L).likeCount()).isEqualTo(2L),
                () -> assertThat(productMap.get(3L).likeCount()).isEqualTo(0L),
                () -> assertThat(productMap.get(4L).likeCount()).isEqualTo(1L),
                () -> assertThat(productMap.get(5L).likeCount()).isEqualTo(0L),
                () -> assertThat(productMap.get(6L).likeCount()).isEqualTo(0L)
        );
    }

    @DisplayName("좋아요 수 기준으로 정렬하여 조회한다.")
    @Test
    void findProductsOrderByLikesDesc() {
        // arrange
        PageRequest pageable = PageRequest.of(0, 10);

        // act
        Page<ProductQueryRepository.ProductQueryData> result =
                productQueryRepository.findProducts(
                        null,
                        ProductSortType.LIKES_DESC,
                        pageable
                );

        List<ProductQueryRepository.ProductQueryData> products = result.getContent();

        // assert
        assertAll (
                () -> assertThat(products.getFirst().id()).isEqualTo(1L),
                () -> assertThat(products.getFirst().likeCount()).isEqualTo(3L),

                () -> assertThat(products.get(1).id()).isEqualTo(2L),
                () -> assertThat(products.get(1).likeCount()).isEqualTo(2L),

                () -> assertThat(products.get(2).id()).isEqualTo(4L),
                () -> assertThat(products.get(2).likeCount()).isEqualTo(1L)
        );

        assertAll(
                () -> assertThat(products.get(3).likeCount()).isEqualTo(0L),
                () -> assertThat(products.get(4).likeCount()).isEqualTo(0L),
                () -> assertThat(products.get(5).likeCount()).isEqualTo(0L)
        );
    }

    @DisplayName("상품 상세 조회 시 해당 상품의 좋아요 수가 포함된다.")
    @Test
    void findProductDetailWithLikeCount() {
        // arrange
        Long productId = 1L;

        // act
        Optional<ProductQueryRepository.ProductDetailQueryData> result =
                productQueryRepository.findProductDetailById(productId);

        // assert
        assertAll(
                () -> assertThat(result).isPresent(),
                () -> assertThat(result.get().id()).isEqualTo(productId),
                () -> assertThat(result.get().likeCount()).isEqualTo(3L)
        );
    }
}
