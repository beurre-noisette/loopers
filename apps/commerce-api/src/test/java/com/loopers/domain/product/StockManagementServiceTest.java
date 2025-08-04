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

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class StockManagementServiceTest {

    private final StockManagementService stockManagementService = new StockManagementService();

    @DisplayName("재고 검증 시")
    @Nested
    class ValidateStock {

        @DisplayName("충분한 재고가 있으면 정상적으로 통과한다.")
        @Test
        void validateStock_success_whenSufficientStock() {
            // arrange
            Brand brand = Brand.of(new BrandCommand.Create("테스트 브랜드", "테스트 브랜드 설명"));
            Product product1 = createProduct(1L, "상품1", new BigDecimal("10000"), 100, brand);
            Product product2 = createProduct(2L, "상품2", new BigDecimal("5000"), 50, brand);
            List<Product> products = List.of(product1, product2);

            OrderItem orderItem1 = new OrderItem(1L, 10, new BigDecimal("10000"));
            OrderItem orderItem2 = new OrderItem(2L, 5, new BigDecimal("5000"));
            OrderItems orderItems = OrderItems.from(List.of(orderItem1, orderItem2));

            // act & assert
            assertDoesNotThrow(() -> stockManagementService.validateStock(products, orderItems));
        }

        @DisplayName("재고가 부족하면 INVALID_INPUT_FORMAT 예외가 발생한다.")
        @Test
        void throwsInvalidInputFormatException_whenInsufficientStock() {
            // arrange
            Brand brand = Brand.of(new BrandCommand.Create("테스트 브랜드", "테스트 브랜드 설명"));
            Product product = createProduct(1L, "상품1", new BigDecimal("10000"), 5, brand);
            List<Product> products = List.of(product);

            OrderItem orderItem = new OrderItem(1L, 10, new BigDecimal("10000"));
            OrderItems orderItems = OrderItems.from(List.of(orderItem));

            // act & assert
            CoreException exception = assertThrows(CoreException.class,
                    () -> stockManagementService.validateStock(products, orderItems));

            assertAll(
                    () -> assertThat(exception.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_FORMAT),
                    () -> assertThat(exception.getMessage()).contains("재고가 부족합니다"),
                    () -> assertThat(exception.getMessage()).contains("상품1"),
                    () -> assertThat(exception.getMessage()).contains("현재 재고: 5"),
                    () -> assertThat(exception.getMessage()).contains("요청 수량: 10")
            );
        }

        @DisplayName("정확히 재고와 같은 수량을 주문하면 정상적으로 통과한다.")
        @Test
        void validateStock_success_whenExactStock() {
            // arrange
            Brand brand = Brand.of(new BrandCommand.Create("테스트 브랜드", "테스트 브랜드 설명"));
            Product product = createProduct(1L, "상품1", new BigDecimal("10000"), 10, brand);
            List<Product> products = List.of(product);

            OrderItem orderItem = new OrderItem(1L, 10, new BigDecimal("10000"));
            OrderItems orderItems = OrderItems.from(List.of(orderItem));

            // act & assert
            assertDoesNotThrow(() -> stockManagementService.validateStock(products, orderItems));
        }

        @DisplayName("여러 상품 중 일부만 재고가 부족해도 예외가 발생한다.")
        @Test
        void throwsInvalidInputFormatException_whenSomeProductsInsufficientStock() {
            // arrange
            Brand brand = Brand.of(new BrandCommand.Create("테스트 브랜드", "테스트 브랜드 설명"));
            Product product1 = createProduct(1L, "상품1", new BigDecimal("10000"), 100, brand);
            Product product2 = createProduct(2L, "상품2", new BigDecimal("5000"), 3, brand);
            List<Product> products = List.of(product1, product2);

            OrderItem orderItem1 = new OrderItem(1L, 10, new BigDecimal("10000"));
            OrderItem orderItem2 = new OrderItem(2L, 5, new BigDecimal("5000"));
            OrderItems orderItems = OrderItems.from(List.of(orderItem1, orderItem2));

            // act
            CoreException exception = assertThrows(CoreException.class,
                    () -> stockManagementService.validateStock(products, orderItems));

            // assert
            assertAll(
                    () -> assertThat(exception.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_FORMAT),
                    () -> assertThat(exception.getMessage()).contains("재고가 부족합니다"),
                    () -> assertThat(exception.getMessage()).contains("상품2")
            );
        }

        @DisplayName("주문 항목에 없는 상품은 검증하지 않는다.")
        @Test
        void validateStock_ignoresProductsNotInOrder() {
            // arrange
            Brand brand = Brand.of(new BrandCommand.Create("테스트 브랜드", "테스트 브랜드 설명"));
            Product product1 = createProduct(1L, "상품1", new BigDecimal("10000"), 100, brand);
            Product product2 = createProduct(2L, "상품2", new BigDecimal("5000"), 0, brand); // 재고 0이지만 주문하지 않음
            List<Product> products = List.of(product1, product2);

            OrderItem orderItem1 = new OrderItem(1L, 10, new BigDecimal("10000"));
            OrderItems orderItems = OrderItems.from(List.of(orderItem1));

            // act & assert
            assertDoesNotThrow(() -> stockManagementService.validateStock(products, orderItems));
        }
    }

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

            OrderItem orderItem1 = new OrderItem(1L, 10, new BigDecimal("10000"));
            OrderItem orderItem2 = new OrderItem(2L, 5, new BigDecimal("5000"));
            OrderItems orderItems = OrderItems.from(List.of(orderItem1, orderItem2));

            // act
            stockManagementService.decreaseStock(products, orderItems);

            // assert
            assertAll(
                    () -> assertThat(product1.getStock()).isEqualTo(90),
                    () -> assertThat(product2.getStock()).isEqualTo(45)
            );
        }

        @DisplayName("주문 항목에 없는 상품의 재고는 차감되지 않는다.")
        @Test
        void decreaseStock_ignoresProductsNotInOrder() {
            // arrange
            Brand brand = Brand.of(new BrandCommand.Create("테스트 브랜드", "테스트 브랜드 설명"));
            Product product1 = createProduct(1L, "상품1", new BigDecimal("10000"), 100, brand);
            Product product2 = createProduct(2L, "상품2", new BigDecimal("5000"), 50, brand);
            List<Product> products = List.of(product1, product2);

            OrderItem orderItem1 = new OrderItem(1L, 10, new BigDecimal("10000"));
            OrderItems orderItems = OrderItems.from(List.of(orderItem1));

            // act
            stockManagementService.decreaseStock(products, orderItems);

            // assert
            assertAll(
                    () -> assertThat(product1.getStock()).isEqualTo(90),
                    () -> assertThat(product2.getStock()).isEqualTo(50) // 변경되지 않음
            );
        }

        @DisplayName("재고가 0이 되어도 정상적으로 처리된다.")
        @Test
        void decreaseStock_success_whenStockBecomesZero() {
            // arrange
            Brand brand = Brand.of(new BrandCommand.Create("테스트 브랜드", "테스트 브랜드 설명"));
            Product product = createProduct(1L, "상품1", new BigDecimal("10000"), 10, brand);
            List<Product> products = List.of(product);

            OrderItem orderItem = new OrderItem(1L, 10, new BigDecimal("10000"));
            OrderItems orderItems = OrderItems.from(List.of(orderItem));

            // act
            stockManagementService.decreaseStock(products, orderItems);

            // assert
            assertThat(product.getStock()).isEqualTo(0);
        }
    }

    @DisplayName("재고 검증과 차감의 통합 시나리오")
    @Nested
    class IntegratedScenario {

        @DisplayName("재고 검증 후 차감이 정상적으로 수행된다.")
        @Test
        void validateAndDecreaseStock_success() {
            // arrange
            Brand brand = Brand.of(new BrandCommand.Create("테스트 브랜드", "테스트 브랜드 설명"));
            Product product1 = createProduct(1L, "상품1", new BigDecimal("10000"), 20, brand);
            Product product2 = createProduct(2L, "상품2", new BigDecimal("5000"), 15, brand);
            List<Product> products = List.of(product1, product2);

            OrderItem orderItem1 = new OrderItem(1L, 8, new BigDecimal("10000"));
            OrderItem orderItem2 = new OrderItem(2L, 3, new BigDecimal("5000"));
            OrderItems orderItems = OrderItems.from(List.of(orderItem1, orderItem2));

            // act
            stockManagementService.validateStock(products, orderItems);
            stockManagementService.decreaseStock(products, orderItems);

            // assert
            assertAll(
                    () -> assertThat(product1.getStock()).isEqualTo(12),
                    () -> assertThat(product2.getStock()).isEqualTo(12)
            );
        }

        @DisplayName("재고 검증이 실패하면 차감하지 않는다.")
        @Test
        void validateStock_fails_noStockDecrease() {
            // arrange
            Brand brand = Brand.of(new BrandCommand.Create("테스트 브랜드", "테스트 브랜드 설명"));
            Product product = createProduct(1L, "상품1", new BigDecimal("10000"), 5, brand);
            List<Product> products = List.of(product);

            OrderItem orderItem = new OrderItem(1L, 10, new BigDecimal("10000"));
            OrderItems orderItems = OrderItems.from(List.of(orderItem));

            // act & assert
            assertThrows(CoreException.class,
                    () -> stockManagementService.validateStock(products, orderItems));

            // 검증 실패 후에도 재고는 변경되지 않음
            assertThat(product.getStock()).isEqualTo(5);
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
