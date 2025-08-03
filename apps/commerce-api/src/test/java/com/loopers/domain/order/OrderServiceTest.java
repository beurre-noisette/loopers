package com.loopers.domain.order;

import com.loopers.domain.product.Product;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @DisplayName("주문 생성 시")
    @Nested
    class CreateOrder {

        @DisplayName("올바른 정보가 주어지면 주문이 성공적으로 생성된다.")
        @Test
        void createOrder_success() {
            // arrange
            String userId = "testUser";
            OrderItem orderItem1 = new OrderItem(1L, 2, new BigDecimal("10000"));
            OrderItem orderItem2 = new OrderItem(2L, 1, new BigDecimal("5000"));
            List<OrderItem> orderItems = List.of(orderItem1, orderItem2);

            Order expectedOrder = Order.create(userId, orderItems);
            when(orderRepository.save(any(Order.class))).thenReturn(expectedOrder);

            // act
            Order result = orderService.createOrder(userId, orderItems);

            // assert
            assertAll(
                    () -> assertThat(result.getUserId()).isEqualTo(userId),
                    () -> assertThat(result.getOrderItems()).hasSize(2),
                    () -> assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING),
                    () -> assertThat(result.getTotalAmount()).isEqualTo(new BigDecimal("25000"))
            );
            
            verify(orderRepository).save(any(Order.class));
        }

        @DisplayName("저장 메서드가 호출된다.")
        @Test
        void createOrder_callsRepositorySave() {
            // arrange
            String userId = "testUser";
            OrderItem orderItem = new OrderItem(1L, 1, new BigDecimal("10000"));
            List<OrderItem> orderItems = List.of(orderItem);

            Order expectedOrder = Order.create(userId, orderItems);
            when(orderRepository.save(any(Order.class))).thenReturn(expectedOrder);

            // act
            orderService.createOrder(userId, orderItems);

            // assert
            verify(orderRepository, times(1)).save(any(Order.class));
        }
    }

    @DisplayName("주문 조회 시")
    @Nested
    class FindOrder {

        @DisplayName("존재하는 주문 ID로 조회하면 주문이 반환된다.")
        @Test
        void findById_success_whenOrderExists() {
            // arrange
            Long orderId = 1L;
            OrderItem orderItem = new OrderItem(1L, 1, new BigDecimal("10000"));
            Order expectedOrder = Order.create("testUser", List.of(orderItem));
            
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(expectedOrder));

            // act
            Order result = orderService.findById(orderId);

            // assert
            assertThat(result).isEqualTo(expectedOrder);
            verify(orderRepository).findById(orderId);
        }

        @DisplayName("존재하지 않는 주문 ID로 조회하면 NOT_FOUND 예외가 발생한다.")
        @Test
        void findById_throwsNotFoundException_whenOrderNotExists() {
            // arrange
            Long orderId = 999L;
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            // act & assert
            CoreException exception = assertThrows(CoreException.class,
                    () -> orderService.findById(orderId));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
            assertThat(exception.getMessage()).contains("주문을 찾을 수 없습니다");
            verify(orderRepository).findById(orderId);
        }

        @DisplayName("사용자 ID로 주문 목록을 조회하면 해당 사용자의 모든 주문이 반환된다.")
        @Test
        void findByUserId_success() {
            // arrange
            String userId = "testUser";
            OrderItem orderItem1 = new OrderItem(1L, 1, new BigDecimal("10000"));
            OrderItem orderItem2 = new OrderItem(2L, 2, new BigDecimal("5000"));
            
            Order order1 = Order.create(userId, List.of(orderItem1));
            Order order2 = Order.create(userId, List.of(orderItem2));
            List<Order> expectedOrders = List.of(order1, order2);

            when(orderRepository.findByUserId(userId)).thenReturn(expectedOrders);

            // act
            List<Order> result = orderService.findByUserId(userId);

            // assert
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyElementsOf(expectedOrders);
            verify(orderRepository).findByUserId(userId);
        }

        @DisplayName("주문이 없는 사용자 ID로 조회하면 빈 리스트가 반환된다.")
        @Test
        void findByUserId_returnsEmptyList_whenNoOrdersExist() {
            // arrange
            String userId = "userWithNoOrders";
            when(orderRepository.findByUserId(userId)).thenReturn(List.of());

            // act
            List<Order> result = orderService.findByUserId(userId);

            // assert
            assertThat(result).isEmpty();
            verify(orderRepository).findByUserId(userId);
        }
    }

    @DisplayName("주문 아이템 생성 시")
    @Nested
    class CreateOrderItems {

        @DisplayName("올바른 정보가 주어지면 주문 아이템들이 성공적으로 생성된다.")
        @Test
        void createOrderItems_success() {
            // arrange
            Product product1 = mock(Product.class);
            Product product2 = mock(Product.class);
            
            when(product1.getId()).thenReturn(1L);
            when(product1.getPrice()).thenReturn(new BigDecimal("10000"));
            when(product2.getId()).thenReturn(2L);
            when(product2.getPrice()).thenReturn(new BigDecimal("5000"));
            
            List<Product> products = List.of(product1, product2);

            OrderCommand.CreateItem item1 = new OrderCommand.CreateItem(1L, 2);
            OrderCommand.CreateItem item2 = new OrderCommand.CreateItem(2L, 1);
            List<OrderCommand.CreateItem> items = List.of(item1, item2);

            // act
            List<OrderItem> result = orderService.createOrderItems(items, products);

            // assert
            assertAll(
                    () -> assertThat(result).hasSize(2),
                    () -> assertThat(result.get(0).productId()).isEqualTo(1L),
                    () -> assertThat(result.get(0).quantity()).isEqualTo(2),
                    () -> assertThat(result.get(0).unitPrice()).isEqualTo(new BigDecimal("10000")),
                    () -> assertThat(result.get(1).productId()).isEqualTo(2L),
                    () -> assertThat(result.get(1).quantity()).isEqualTo(1),
                    () -> assertThat(result.get(1).unitPrice()).isEqualTo(new BigDecimal("5000"))
            );
        }

        @DisplayName("존재하지 않는 상품 ID가 포함되면 예외가 발생한다.")
        @Test
        void createOrderItems_throwsException_whenProductNotFound() {
            // arrange
            Product product1 = mock(Product.class);
            when(product1.getId()).thenReturn(1L);
            when(product1.getPrice()).thenReturn(new BigDecimal("10000"));
            
            List<Product> products = List.of(product1);

            OrderCommand.CreateItem item1 = new OrderCommand.CreateItem(1L, 2);
            OrderCommand.CreateItem item2 = new OrderCommand.CreateItem(999L, 1); // 존재하지 않는 상품
            List<OrderCommand.CreateItem> items = List.of(item1, item2);

            // act & assert
            CoreException exception = assertThrows(CoreException.class,
                    () -> orderService.createOrderItems(items, products));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
            assertThat(exception.getMessage()).contains("상품을 찾을 수 없습니다");
        }
    }

    @DisplayName("총 주문 금액 계산 시")
    @Nested
    class CalculateTotalAmount {

        @DisplayName("여러 주문 아이템의 총 금액이 정확히 계산된다.")
        @Test
        void calculateTotalAmount_success() {
            // arrange
            OrderItem orderItem1 = new OrderItem(1L, 2, new BigDecimal("10000"));
            OrderItem orderItem2 = new OrderItem(2L, 3, new BigDecimal("5000"));
            OrderItem orderItem3 = new OrderItem(3L, 1, new BigDecimal("3000"));
            List<OrderItem> orderItems = List.of(orderItem1, orderItem2, orderItem3);

            // act
            BigDecimal result = orderService.calculateTotalAmount(orderItems);

            // assert
            assertThat(result).isEqualTo(new BigDecimal("38000"));
        }

        @DisplayName("주문 아이템이 하나일 때도 총 금액이 정확히 계산된다.")
        @Test
        void calculateTotalAmount_singleItem() {
            // arrange
            OrderItem orderItem = new OrderItem(1L, 5, new BigDecimal("2000"));
            List<OrderItem> orderItems = List.of(orderItem);

            // act
            BigDecimal result = orderService.calculateTotalAmount(orderItems);

            // assert
            assertThat(result).isEqualTo(new BigDecimal("10000"));
        }

        @DisplayName("주문 아이템이 없으면 0원이 반환된다.")
        @Test
        void calculateTotalAmount_emptyList() {
            // arrange
            List<OrderItem> orderItems = List.of();

            // act
            BigDecimal result = orderService.calculateTotalAmount(orderItems);

            // assert
            assertThat(result).isEqualTo(BigDecimal.ZERO);
        }
    }

}
