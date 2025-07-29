package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

@SpringBootTest
@SqlGroup({
        @Sql(scripts = {"/brand-test-data.sql", "/product-test-data.sql"},
             executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
        @Sql(scripts = {"/product-cleanup.sql", "/brand-cleanup.sql"},
             executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
})
class ProductServiceIntegrationTest {

    @Autowired
    private ProductService productService;

    @MockitoSpyBean
    private ProductRepository productRepository;

    @DisplayName("상품을 조회할 때")
    @Nested
    class GetProduct {
        @DisplayName("상품이 존재할 경우 브랜드 정보를 포함한 상품 정보를 반환한다.")
        @ParameterizedTest
        @CsvSource({
                "1, 'iPhone 15 Pro', '티타늄 소재로 제작된 프리미엄 스마트폰', 1490000.00, 50, 'Apple', 'Think Different - 혁신적인 기술로 세상을 바꾸는 브랜드'",
                "2, 'MacBook Pro 14', 'M3 Pro 칩셋이 탑재된 고성능 노트북', 2690000.00, 30, 'Apple', 'Think Different - 혁신적인 기술로 세상을 바꾸는 브랜드'",
                "3, 'AirPods Pro', '액티브 노이즈 캔슬링 무선 이어폰', 329000.00, 100, 'Apple', 'Think Different - 혁신적인 기술로 세상을 바꾸는 브랜드'",
                "4, 'Galaxy S24 Ultra', 'S펜이 내장된 프리미엄 안드로이드 스마트폰', 1598000.00, 40, 'Samsung', 'Galaxy of Innovation - 끝없는 혁신으로 더 나은 내일을 만드는 브랜드'",
                "5, 'Galaxy Book4 Pro', 'AMOLED 디스플레이 탑재 노트북', 1899000.00, 25, 'Samsung', 'Galaxy of Innovation - 끝없는 혁신으로 더 나은 내일을 만드는 브랜드'",
                "7, 'Air Jordan 1 High', '클래식한 농구화 디자인', 179000.00, 60, 'Nike', 'Just Do It - 스포츠를 통해 모든 사람의 잠재력을 이끌어내는 브랜드'"
        })
        @Transactional
        void returnProduct_whenProductExists(
                Long productId,
                String expectedName,
                String expectedDescription,
                BigDecimal expectedPrice,
                Integer expectedStock,
                String expectedBrandName,
                String expectedBrandDescription
        ) {
            // act
            Product foundProduct = productService.findById(productId);

            // assert
            assertAll(
                    () -> assertThat(foundProduct).isNotNull(),
                    () -> assertThat(foundProduct.getName()).isEqualTo(expectedName),
                    () -> assertThat(foundProduct.getDescription()).isEqualTo(expectedDescription),
                    () -> assertThat(foundProduct.getPrice()).isEqualTo(expectedPrice),
                    () -> assertThat(foundProduct.getStock()).isEqualTo(expectedStock),
                    () -> assertThat(foundProduct.getBrand().getName()).isEqualTo(expectedBrandName),
                    () -> assertThat(foundProduct.getBrand().getDescription()).isEqualTo(expectedBrandDescription)
            );
            verify(productRepository).findById(productId);
        }

        @DisplayName("상품이 존재하지 않을 경우 NOT FOUND 예외가 발생한다.")
        @Test
        void throwNotFoundException_whenProductDoesNotExist() {
            // arrange
            Long nonExistingProductId = 999L;

            // act
            CoreException exception = assertThrows(CoreException.class,
                    () -> productService.findById(nonExistingProductId));

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }
    }

}
