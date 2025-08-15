package com.loopers.application.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("상품 조회 테스트")
class ProductQueryTest {

    @Mock
    private ProductQueryRepository productQueryRepository;

    @Mock
    private ProductQueryCacheRepository productQueryCacheRepository;

    private ProductQuery productQuery;

    @BeforeEach
    void setUp() {
        productQuery = new ProductQuery(productQueryRepository, productQueryCacheRepository);
    }

    @Test
    @DisplayName("상품 목록을 정렬 조건에 따라 조회한다")
    void getProducts_bySortCriteria() {
        // arrange
        Long brandId = 1L;
        String sort = "price_asc";
        int page = 0;
        int size = 20;

        Page<ProductQueryRepository.ProductQueryData> mockPage = getProductQueryData(page, size);

        given(productQueryRepository.findProducts(eq(brandId), eq(ProductSortType.PRICE_ASC), any(Pageable.class)))
                .willReturn(mockPage);

        // act
        ProductQuery.ProductListResult result = productQuery.getProducts(brandId, sort, page, size);

        // assert
        assertThat(result.products()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.currentPage()).isEqualTo(0);
        assertThat(result.pageSize()).isEqualTo(20);

        verify(productQueryRepository).findProducts(eq(brandId), eq(ProductSortType.PRICE_ASC), any(Pageable.class));
    }

    private @NotNull Page<ProductQueryRepository.ProductQueryData> getProductQueryData(int page, int size) {
        ProductQueryRepository.ProductQueryData product1 = new ProductQueryRepository.ProductQueryData(
                1L, "상품1", "설명1", BigDecimal.valueOf(10000), 100, 1L, "브랜드1", 5
        );
        ProductQueryRepository.ProductQueryData product2 = new ProductQueryRepository.ProductQueryData(
                2L, "상품2", "설명2", BigDecimal.valueOf(20000), 200, 1L, "브랜드1", 10
        );

        Page<ProductQueryRepository.ProductQueryData> mockPage =
                new PageImpl<>(List.of(product1, product2), PageRequest.of(page, size), 2);

        return mockPage;
    }

    @Test
    @DisplayName("정렬 조건이 없으면 기본값(LATEST)으로 조회한다")
    void getProductsWithDefaultSort_whenDoesntProvidedSortCriteria() {
        //arrange
        Long brandId = null;
        String sort = null;
        int page = 0;
        int size = 10;

        Page<ProductQueryRepository.ProductQueryData> mockPage =
                new PageImpl<>(List.of(), PageRequest.of(page, size), 0);

        given(productQueryRepository.findProducts(eq(null), eq(ProductSortType.LATEST), any(Pageable.class)))
                .willReturn(mockPage);

        // act
        ProductQuery.ProductListResult result = productQuery.getProducts(brandId, sort, page, size);

        // assert
        assertThat(result.products()).isEmpty();
        verify(productQueryRepository).findProducts(eq(null), eq(ProductSortType.LATEST), any(Pageable.class));
    }

    @Test
    @DisplayName("상품 상세 정보를 조회한다")
    void getProductDetail() {
        // arrange
        Long productId = 1L;
        ProductQueryRepository.ProductDetailQueryData mockData =
                new ProductQueryRepository.ProductDetailQueryData(
                        1L, "상품1", "설명1", BigDecimal.valueOf(10000), 100,
                        1L, "브랜드1", "브랜드 설명", 5
                );

        given(productQueryRepository.findProductDetailById(productId))
                .willReturn(Optional.of(mockData));

        // act
        ProductQuery.ProductDetailResult result = productQuery.getProductDetail(productId);

        // assert
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("상품1");
        assertThat(result.price()).isEqualTo(BigDecimal.valueOf(10000));
        assertThat(result.brand().name()).isEqualTo("브랜드1");
        assertThat(result.likeCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("존재하지 않는 상품의 상세정보를 조회하면 NOT FOUND 예외가 발생한다")
    void throwNotFoundException_whenProductDoesntExists() {
        // arrange
        Long productId = 999L;

        given(productQueryRepository.findProductDetailById(productId))
                .willReturn(Optional.empty());

        // act
        CoreException exception = assertThrows(CoreException.class,
                () -> productQuery.getProductDetail(productId));

        // assert
        assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
    }
}
