package com.loopers.domain.order;

import com.loopers.domain.product.Product;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandCommand;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class OrderItemsTest {

    @DisplayName("OrderItems를 생성할 때, ")
    @Nested
    class Creation {

        @DisplayName("유효한 OrderItem 리스트로 생성하면 성공한다.")
        @Test
        void createOrderItems_whenValidOrderItemListProvided() {
            // arrange
            OrderItem orderItem1 = new OrderItem(1L, 2, new BigDecimal("10000"));
            OrderItem orderItem2 = new OrderItem(2L, 1, new BigDecimal("5000"));
            List<OrderItem> items = List.of(orderItem1, orderItem2);

            // act
            OrderItems orderItems = OrderItems.from(items);

            // assert
            assertAll(
                    () -> assertThat(orderItems.getItems()).hasSize(2),
                    () -> assertThat(orderItems.size()).isEqualTo(2),
                    () -> assertThat(orderItems.getItems()).containsExactly(orderItem1, orderItem2)
            );
        }

        @DisplayName("null 리스트로 생성하면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwBadRequestException_whenNullListProvided() {
            // act & assert
            CoreException exception = assertThrows(CoreException.class,
                    () -> OrderItems.from(null));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("주문 항목은 최소 1개 이상이어야 합니다");
        }

        @DisplayName("빈 리스트로 생성하면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwBadRequestException_whenEmptyListProvided() {
            // act & assert
            CoreException exception = assertThrows(CoreException.class,
                    () -> OrderItems.from(List.of()));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("주문 항목은 최소 1개 이상이어야 합니다");
        }
    }

    @DisplayName("총액을 계산할 때, ")
    @Nested
    class TotalAmountCalculation {

        @DisplayName("여러 OrderItem의 총액이 올바르게 계산된다.")
        @Test
        void calculateTotalAmount_withMultipleItems() {
            // arrange
            OrderItem orderItem1 = new OrderItem(1L, 2, new BigDecimal("15000")); // 30000
            OrderItem orderItem2 = new OrderItem(2L, 3, new BigDecimal("8000"));  // 24000
            OrderItem orderItem3 = new OrderItem(3L, 1, new BigDecimal("12000")); // 12000
            OrderItems orderItems = OrderItems.from(List.of(orderItem1, orderItem2, orderItem3));

            // act
            BigDecimal totalAmount = orderItems.calculateTotalAmount();

            // assert
            assertThat(totalAmount).isEqualTo(new BigDecimal("66000"));
        }

        @DisplayName("단일 OrderItem의 총액이 올바르게 계산된다.")
        @Test
        void calculateTotalAmount_withSingleItem() {
            // arrange
            OrderItem orderItem = new OrderItem(1L, 5, new BigDecimal("7000")); // 35000
            OrderItems orderItems = OrderItems.from(List.of(orderItem));

            // act
            BigDecimal totalAmount = orderItems.calculateTotalAmount();

            // assert
            assertThat(totalAmount).isEqualTo(new BigDecimal("35000"));
        }
    }

    @DisplayName("상품별 수량 맵을 제공할 때, ")
    @Nested
    class ProductQuantityMapping {

        @DisplayName("각 상품별 주문 수량이 올바른 맵으로 반환된다.")
        @Test
        void getProductQuantityMap_returnsCorrectMapping() {
            // arrange
            OrderItem orderItem1 = new OrderItem(1L, 2, new BigDecimal("10000"));
            OrderItem orderItem2 = new OrderItem(2L, 3, new BigDecimal("5000"));
            OrderItem orderItem3 = new OrderItem(3L, 1, new BigDecimal("15000"));
            OrderItems orderItems = OrderItems.from(List.of(orderItem1, orderItem2, orderItem3));

            // act
            Map<Long, Integer> quantityMap = orderItems.getProductQuantityMap();

            // assert
            assertAll(
                    () -> assertThat(quantityMap).hasSize(3),
                    () -> assertThat(quantityMap.get(1L)).isEqualTo(2),
                    () -> assertThat(quantityMap.get(2L)).isEqualTo(3),
                    () -> assertThat(quantityMap.get(3L)).isEqualTo(1)
            );
        }
    }

    @DisplayName("팩토리 메서드로 생성할 때, ")
    @Nested
    class FactoryMethodCreation {

        @DisplayName("Command와 Product 리스트로 OrderItems를 생성한다.")
        @Test
        void createOrderItems_fromCommandAndProducts() {
            // arrange
            BrandCommand.Create brandCommand = new BrandCommand.Create("테스트 브랜드", "테스트 브랜드 설명");
            Brand brand = Brand.of(brandCommand);
            Product product1 = createProduct(1L, "상품1", new BigDecimal("10000"), brand);
            Product product2 = createProduct(2L, "상품2", new BigDecimal("5000"), brand);
            List<Product> products = List.of(product1, product2);

            OrderCommand.CreateItem item1 = new OrderCommand.CreateItem(1L, 2);
            OrderCommand.CreateItem item2 = new OrderCommand.CreateItem(2L, 3);
            List<OrderCommand.CreateItem> commandItems = List.of(item1, item2);

            // act
            OrderItems orderItems = OrderItems.create(commandItems, products);

            // assert
            assertAll(
                    () -> assertThat(orderItems.size()).isEqualTo(2),
                    () -> assertThat(orderItems.calculateTotalAmount()).isEqualTo(new BigDecimal("35000")),
                    () -> assertThat(orderItems.getProductQuantityMap().get(1L)).isEqualTo(2),
                    () -> assertThat(orderItems.getProductQuantityMap().get(2L)).isEqualTo(3)
            );
        }

        @DisplayName("존재하지 않는 상품 ID로 생성하면 NOT_FOUND 예외가 발생한다.")
        @Test
        void throwNotFoundException_whenProductDoesntExists() {
            // arrange
            BrandCommand.Create brandCommand = new BrandCommand.Create("테스트 브랜드", "테스트 브랜드 설명");
            Brand brand = Brand.of(brandCommand);
            Product product = createProduct(1L, "상품1", new BigDecimal("10000"), brand);
            List<Product> products = List.of(product);

            OrderCommand.CreateItem item = new OrderCommand.CreateItem(999L, 2);
            List<OrderCommand.CreateItem> commandItems = List.of(item);

            // act
            CoreException exception = assertThrows(CoreException.class,
                    () -> OrderItems.create(commandItems, products));

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
            assertThat(exception.getMessage()).contains("상품을 찾을 수 없습니다. ID: 999");
        }
    }

    private Product createProduct(Long id, String name, BigDecimal price, Brand brand) {
        try {
            java.lang.reflect.Constructor<Product> constructor = Product.class.getDeclaredConstructor(
                    String.class, String.class, BigDecimal.class, Integer.class, Brand.class);
            constructor.setAccessible(true);
            Product product = constructor.newInstance(name, "테스트 설명", price, 100, brand);
            
            // ID 설정을 위한 리플렉션
            java.lang.reflect.Field idField = Product.class.getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(product, id);
            
            return product;
        } catch (Exception e) {
            throw new RuntimeException("테스트용 Product 객체 생성 실패", e);
        }
    }
}
