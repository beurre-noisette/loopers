package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @DisplayName("상품 ID 리스트로 상품 조회 시")
    @Nested
    class FindProductsByIds {

        @DisplayName("올바른 상품 ID들이 주어지면 상품들이 성공적으로 조회된다.")
        @Test
        void findProductsByIdsSuccess_whenProvidedCorrectProductIds() {
            // arrange
            Product product1 = mock(Product.class);
            Product product2 = mock(Product.class);

            List<Long> productIds = List.of(1L, 2L);

            when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
            when(productRepository.findById(2L)).thenReturn(Optional.of(product2));

            // act
            List<Product> result = productService.findProductsByIds(productIds);

            // assert
            assertAll(
                    () -> assertThat(result).hasSize(2),
                    () -> assertThat(result.get(0)).isEqualTo(product1),
                    () -> assertThat(result.get(1)).isEqualTo(product2)
            );

            verify(productRepository).findById(1L);
            verify(productRepository).findById(2L);
        }

        @DisplayName("존재하지 않는 상품 ID가 포함되면 NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsNotFoundException_whenIncludeDoesntExistProductId() {
            // arrange
            List<Long> productIds = List.of(1L, 999L);

            Product product1 = mock(Product.class);

            when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            // act & assert
            CoreException exception = assertThrows(CoreException.class,
                    () -> productService.findProductsByIds(productIds));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }

        @DisplayName("빈 상품 아이디 리스트가 주어지면 빈 리스트가 반환된다.")
        @Test
        void returnsEmptyList_whenEmptyIdsProvided() {
            // arrange
            List<Long> productIds = List.of();

            // act
            List<Product> result = productService.findProductsByIds(productIds);

            // assert
            assertThat(result).isEmpty();
            verifyNoInteractions(productRepository);
        }
    }


    @DisplayName("상품 단건 조회 시")
    @Nested
    class FindById {

        @DisplayName("존재하는 상품 ID로 조회하면 상품이 반환된다.")
        @Test
        void findById_success_whenProductExists() {
            // arrange
            Long productId = 1L;
            Product expectedProduct = mock(Product.class);

            when(productRepository.findById(productId)).thenReturn(Optional.of(expectedProduct));

            // act
            Product result = productService.findById(productId);

            // assert
            assertThat(result).isEqualTo(expectedProduct);
            verify(productRepository).findById(productId);
        }

        @DisplayName("존재하지 않는 상품 ID로 조회하면 NOT_FOUND 예외가 발생한다.")
        @Test
        void findById_throwsNotFoundException_whenProductNotExists() {
            // arrange
            Long productId = 999L;
            when(productRepository.findById(productId)).thenReturn(Optional.empty());

            // act & assert
            CoreException exception = assertThrows(CoreException.class,
                    () -> productService.findById(productId));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
            assertThat(exception.getMessage()).contains("상품을 찾을 수 없습니다");
            verify(productRepository).findById(productId);
        }
    }

}
