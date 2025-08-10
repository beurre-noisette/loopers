package com.loopers.domain.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandCommand;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderItems;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockManagementServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private StockManagementService stockManagementService;

    @DisplayName("재고 차감 시")
    @Nested
    class DecreaseStock {

        @DisplayName("주문 수량만큼 재고가 정상적으로 차감된다.")
        @Test
        void decreaseStock_success_decreasesCorrectQuantity() {
            // arrange
            Brand brand = Brand.of(new BrandCommand.Create("테스트 브랜드", "테스트 브랜드 설명"));
            Product product1 = createProduct(1L, "상품1", new BigDecimal("10000"), 100, brand);
            Product product2 = createProduct(2L, "상품2", new BigDecimal("5000"), 50, brand);
            List<Product> products = List.of(product1, product2);

            // Mock으로 락이 적용된 상품 조회 시뮬레이션
            Product lockedProduct1 = createProduct(1L, "상품1", new BigDecimal("10000"), 100, brand);
            Product lockedProduct2 = createProduct(2L, "상품2", new BigDecimal("5000"), 50, brand);
            
            OrderItem orderItem1 = new OrderItem(1L, 10, new BigDecimal("10000"));
            OrderItem orderItem2 = new OrderItem(2L, 5, new BigDecimal("5000"));
            OrderItems orderItems = OrderItems.from(List.of(orderItem1, orderItem2));

            when(productRepository.findByIdWithLock(1L)).thenReturn(Optional.of(lockedProduct1));
            when(productRepository.findByIdWithLock(2L)).thenReturn(Optional.of(lockedProduct2));
            when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // act
            stockManagementService.decreaseStock(products, orderItems);

            // assert
            assertAll(
                    () -> assertThat(lockedProduct1.getStock()).isEqualTo(90),
                    () -> assertThat(lockedProduct2.getStock()).isEqualTo(45)
            );

            verify(productRepository).findByIdWithLock(1L);
            verify(productRepository).findByIdWithLock(2L);
            verify(productRepository, times(2)).save(any(Product.class));
        }

        @DisplayName("주문 항목에 없는 상품은 처리하지 않는다.")
        @Test
        void decreaseStock_ignoresProductsNotInOrder() {
            // arrange
            Brand brand = Brand.of(new BrandCommand.Create("테스트 브랜드", "테스트 브랜드 설명"));
            Product product1 = createProduct(1L, "상품1", new BigDecimal("10000"), 100, brand);
            Product product2 = createProduct(2L, "상품2", new BigDecimal("5000"), 50, brand);
            List<Product> products = List.of(product1, product2);

            Product lockedProduct1 = createProduct(1L, "상품1", new BigDecimal("10000"), 100, brand);

            OrderItem orderItem1 = new OrderItem(1L, 10, new BigDecimal("10000"));
            OrderItems orderItems = OrderItems.from(List.of(orderItem1));

            when(productRepository.findByIdWithLock(1L)).thenReturn(Optional.of(lockedProduct1));
            when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // act
            stockManagementService.decreaseStock(products, orderItems);

            // assert
            assertThat(lockedProduct1.getStock()).isEqualTo(90);

            verify(productRepository).findByIdWithLock(1L);
            verify(productRepository, never()).findByIdWithLock(2L);
            verify(productRepository, times(1)).save(any(Product.class));
        }

        @DisplayName("재고가 0이 되어도 정상적으로 처리된다.")
        @Test
        void decreaseStock_success_whenStockBecomesZero() {
            // arrange
            Brand brand = Brand.of(new BrandCommand.Create("테스트 브랜드", "테스트 브랜드 설명"));
            Product product = createProduct(1L, "상품1", new BigDecimal("10000"), 10, brand);
            List<Product> products = List.of(product);

            Product lockedProduct = createProduct(1L, "상품1", new BigDecimal("10000"), 10, brand);

            OrderItem orderItem = new OrderItem(1L, 10, new BigDecimal("10000"));
            OrderItems orderItems = OrderItems.from(List.of(orderItem));

            when(productRepository.findByIdWithLock(1L)).thenReturn(Optional.of(lockedProduct));
            when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // act
            stockManagementService.decreaseStock(products, orderItems);

            // assert
            assertThat(lockedProduct.getStock()).isEqualTo(0);

            verify(productRepository).findByIdWithLock(1L);
            verify(productRepository).save(lockedProduct);
        }

        @DisplayName("상품이 존재하지 않으면 예외가 발생한다.")
        @Test
        void decreaseStock_throwsException_whenProductNotFound() {
            // arrange
            Brand brand = Brand.of(new BrandCommand.Create("테스트 브랜드", "테스트 브랜드 설명"));
            Product product = createProduct(1L, "상품1", new BigDecimal("10000"), 10, brand);
            List<Product> products = List.of(product);

            OrderItem orderItem = new OrderItem(1L, 5, new BigDecimal("10000"));
            OrderItems orderItems = OrderItems.from(List.of(orderItem));

            when(productRepository.findByIdWithLock(1L)).thenReturn(Optional.empty());

            // act & assert
            CoreException exception = assertThrows(CoreException.class,
                    () -> stockManagementService.decreaseStock(products, orderItems));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
            assertThat(exception.getMessage()).contains("상품을 찾을 수 없습니다");

            verify(productRepository).findByIdWithLock(1L);
            verify(productRepository, never()).save(any(Product.class));
        }

        @DisplayName("재고가 부족하면 상품 엔티티에서 예외가 발생한다.")
        @Test
        void decreaseStock_throwsException_whenInsufficientStock() {
            // arrange
            Brand brand = Brand.of(new BrandCommand.Create("테스트 브랜드", "테스트 브랜드 설명"));
            Product product = createProduct(1L, "상품1", new BigDecimal("10000"), 5, brand);
            List<Product> products = List.of(product);

            Product lockedProduct = createProduct(1L, "상품1", new BigDecimal("10000"), 5, brand);

            OrderItem orderItem = new OrderItem(1L, 10, new BigDecimal("10000"));
            OrderItems orderItems = OrderItems.from(List.of(orderItem));

            when(productRepository.findByIdWithLock(1L)).thenReturn(Optional.of(lockedProduct));

            // act & assert
            CoreException exception = assertThrows(CoreException.class,
                    () -> stockManagementService.decreaseStock(products, orderItems));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_FORMAT);
            assertThat(exception.getMessage()).contains("재고가 부족합니다");

            verify(productRepository).findByIdWithLock(1L);
            verify(productRepository, never()).save(any(Product.class));
        }
    }

    @DisplayName("락 기반 동시성 제어")
    @Nested
    class ConcurrencyControl {

        @DisplayName("각 상품에 대해 락이 적용된 조회가 수행된다.")
        @Test
        void decreaseStock_usesLockForEachProduct() {
            // arrange
            Brand brand = Brand.of(new BrandCommand.Create("테스트 브랜드", "테스트 브랜드 설명"));
            Product product1 = createProduct(1L, "상품1", new BigDecimal("10000"), 100, brand);
            Product product2 = createProduct(2L, "상품2", new BigDecimal("5000"), 50, brand);
            List<Product> products = List.of(product1, product2);

            Product lockedProduct1 = createProduct(1L, "상품1", new BigDecimal("10000"), 100, brand);
            Product lockedProduct2 = createProduct(2L, "상품2", new BigDecimal("5000"), 50, brand);

            OrderItem orderItem1 = new OrderItem(1L, 10, new BigDecimal("10000"));
            OrderItem orderItem2 = new OrderItem(2L, 5, new BigDecimal("5000"));
            OrderItems orderItems = OrderItems.from(List.of(orderItem1, orderItem2));

            when(productRepository.findByIdWithLock(1L)).thenReturn(Optional.of(lockedProduct1));
            when(productRepository.findByIdWithLock(2L)).thenReturn(Optional.of(lockedProduct2));
            when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // act
            stockManagementService.decreaseStock(products, orderItems);

            // assert - 락이 적용된 조회가 각 상품별로 호출되었는지 확인
            verify(productRepository).findByIdWithLock(1L);
            verify(productRepository).findByIdWithLock(2L);
            verify(productRepository, times(2)).save(any(Product.class));

            // 실제 재고 차감 확인
            assertThat(lockedProduct1.getStock()).isEqualTo(90);
            assertThat(lockedProduct2.getStock()).isEqualTo(45);
        }
    }

    private Product createProduct(Long id, String name, BigDecimal price, Integer stock, Brand brand) {
        try {
            java.lang.reflect.Constructor<Product> constructor = Product.class.getDeclaredConstructor(
                    String.class, String.class, BigDecimal.class, Integer.class, Brand.class);
            constructor.setAccessible(true);
            Product product = constructor.newInstance(name, "테스트 설명", price, stock, brand);
            
            // ID 설정을 위한 리플렉션
            Field idField = Product.class.getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(product, id);
            
            return product;
        } catch (Exception e) {
            throw new RuntimeException("테스트용 Product 객체 생성 실패", e);
        }
    }
}