package com.loopers.domain.product;

import com.loopers.domain.order.OrderCommand;
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

    @DisplayName("주문을 위한 상품 조회 시")
    @Nested
    class FindProductsForOrder {

        @DisplayName("올바른 상품 ID들이 주어지면 상품들이 성공적으로 조회된다.")
        @Test
        void findProductsForOrder_success() {
            // arrange
            Product product1 = mock(Product.class);
            Product product2 = mock(Product.class);

            OrderCommand.CreateItem item1 = new OrderCommand.CreateItem(1L, 2);
            OrderCommand.CreateItem item2 = new OrderCommand.CreateItem(2L, 1);
            List<OrderCommand.CreateItem> items = List.of(item1, item2);

            when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
            when(productRepository.findById(2L)).thenReturn(Optional.of(product2));

            // act
            List<Product> result = productService.findProductsForOrder(items);

            // assert
            assertAll(
                    () -> assertThat(result).hasSize(2),
                    () -> assertThat(result.get(0)).isEqualTo(product1),
                    () -> assertThat(result.get(1)).isEqualTo(product2)
            );

            verify(productRepository).findById(1L);
            verify(productRepository).findById(2L);
        }

        @DisplayName("존재하지 않는 상품 ID가 포함되면 Not Found 예외가 발생한다.")
        @Test
        void findProductsForOrder_throwsNotFoundException_whenProductNotFound() {
            // arrange
            OrderCommand.CreateItem item1 = new OrderCommand.CreateItem(1L, 2);
            OrderCommand.CreateItem item2 = new OrderCommand.CreateItem(999L, 1);
            List<OrderCommand.CreateItem> items = List.of(item1, item2);

            Product product1 = mock(Product.class);

            when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            // act & assert
            CoreException exception = assertThrows(CoreException.class,
                    () -> productService.findProductsForOrder(items));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
            assertThat(exception.getMessage()).contains("상품을 찾을 수 없습니다");
        }
    }

    @DisplayName("재고 검증 및 차감 시")
    @Nested
    class ValidateAndDecreaseStocks {

        @DisplayName("충분한 재고가 있으면 재고가 성공적으로 차감된다.")
        @Test
        void validateAndDecreaseStocks_success() {
            // arrange
            Product product1 = mock(Product.class);
            Product product2 = mock(Product.class);
            
            when(product1.getId()).thenReturn(1L);
            when(product1.getStock()).thenReturn(10);
            when(product2.getId()).thenReturn(2L);
            when(product2.getStock()).thenReturn(5);
            
            List<Product> products = List.of(product1, product2);

            OrderCommand.CreateItem item1 = new OrderCommand.CreateItem(1L, 3);
            OrderCommand.CreateItem item2 = new OrderCommand.CreateItem(2L, 2);
            List<OrderCommand.CreateItem> items = List.of(item1, item2);

            // act
            assertDoesNotThrow(() -> productService.validateAndDecreaseStocks(products, items));

            // assert
            verify(product1).decreaseStock(3);
            verify(product2).decreaseStock(2);
        }

        @DisplayName("재고가 부족하면 예외가 발생한다.")
        @Test
        void validateAndDecreaseStocks_throwsException_whenInsufficientStock() {
            // arrange
            Product product1 = mock(Product.class);
            when(product1.getId()).thenReturn(1L);
            when(product1.getName()).thenReturn("상품 1");
            when(product1.getStock()).thenReturn(2); // 재고 2개
            
            List<Product> products = List.of(product1);

            OrderCommand.CreateItem item1 = new OrderCommand.CreateItem(1L, 5); // 5개 요청 (재고 부족)
            List<OrderCommand.CreateItem> items = List.of(item1);

            // act & assert
            CoreException exception = assertThrows(CoreException.class,
                    () -> productService.validateAndDecreaseStocks(products, items));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_FORMAT);
            assertThat(exception.getMessage()).contains("재고가 부족합니다");
            assertThat(exception.getMessage()).contains("상품 1");
            assertThat(exception.getMessage()).contains("현재 재고: 2");
            assertThat(exception.getMessage()).contains("요청 수량: 5");

            verify(product1, never()).decreaseStock(anyInt());
        }

        @DisplayName("일부 상품의 재고가 부족하면 모든 재고 차감이 중단된다.")
        @Test
        void validateAndDecreaseStocks_allOrNothing_whenPartialInsufficientStock() {
            // arrange
            Product product1 = mock(Product.class);
            Product product2 = mock(Product.class);
            
            when(product1.getId()).thenReturn(1L);
            when(product1.getStock()).thenReturn(10); // 충분한 재고
            when(product2.getId()).thenReturn(2L);
            when(product2.getName()).thenReturn("상품 2");
            when(product2.getStock()).thenReturn(1);  // 부족한 재고
            
            List<Product> products = List.of(product1, product2);

            OrderCommand.CreateItem item1 = new OrderCommand.CreateItem(1L, 3);
            OrderCommand.CreateItem item2 = new OrderCommand.CreateItem(2L, 5); // 재고 부족
            List<OrderCommand.CreateItem> items = List.of(item1, item2);

            // act & assert
            CoreException exception = assertThrows(CoreException.class,
                    () -> productService.validateAndDecreaseStocks(products, items));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_FORMAT);
            assertThat(exception.getMessage()).contains("재고가 부족합니다");

            verify(product1, never()).decreaseStock(anyInt());
            verify(product2, never()).decreaseStock(anyInt());
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
