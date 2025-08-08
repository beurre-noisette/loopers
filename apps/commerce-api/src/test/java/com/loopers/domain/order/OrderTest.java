package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    @DisplayName("주문(Order)을 생성할 때, ")
    @Nested
    class Create {
        
        @DisplayName("올바른 userId와 주문 항목들이 주어지면 정상적으로 생성된다.")
        @Test
        void createOrder_whenValidUserIdAndOrderItemsProvided() {
            // arrange
            String userId = "testUser";
            OrderItem orderItem1 = new OrderItem(1L, 2, new BigDecimal("10000"));
            OrderItem orderItem2 = new OrderItem(2L, 1, new BigDecimal("5000"));
            List<OrderItem> orderItems = List.of(orderItem1, orderItem2);

            // act
            OrderItems orderItemsCollection = OrderItems.from(orderItems);
            Order order = Order.create(userId, orderItemsCollection);

            // assert
            assertAll(
                    () -> assertThat(order.getUserId()).isEqualTo(userId),
                    () -> assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING),
                    () -> assertThat(order.getOrderItems().getItems()).hasSize(2),
                    () -> assertThat(order.getTotalAmount()).isEqualTo(new BigDecimal("25000")),
                    () -> assertThat(order.getOrderItems().calculateTotalAmount()).isEqualTo(new BigDecimal("25000"))
            );
        }

        @DisplayName("userId가 null일 경우 주문 생성에 실패하고 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwBadRequestException_whenUserIdIsNull() {
            // arrange
            OrderItem orderItem = new OrderItem(1L, 1, new BigDecimal("10000"));
            List<OrderItem> orderItems = List.of(orderItem);

            // act & assert
            OrderItems orderItemsCollection = OrderItems.from(orderItems);
            CoreException exception = assertThrows(CoreException.class, 
                () -> Order.create(null, orderItemsCollection));
            
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("사용자 ID는 필수값입니다");
        }

        @DisplayName("userId가 빈 문자열일 경우 주문 생성에 실패하고 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwBadRequestException_whenUserIdIsBlank() {
            // arrange
            OrderItem orderItem = new OrderItem(1L, 1, new BigDecimal("10000"));
            List<OrderItem> orderItems = List.of(orderItem);

            // act & assert
            OrderItems orderItemsCollection = OrderItems.from(orderItems);
            CoreException exception = assertThrows(CoreException.class, 
                () -> Order.create("", orderItemsCollection));
            
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("사용자 ID는 필수값입니다");
        }
    }

    @DisplayName("주문 상태를 변경할 때, ")
    @Nested
    class StatusChange {
        
        @DisplayName("PENDING 상태의 주문을 완료 처리하면 COMPLETED 상태로 변경된다.")
        @Test
        void completeOrder_whenOrderStatusIsPending() {
            // arrange
            OrderItem orderItem = new OrderItem(1L, 1, new BigDecimal("10000"));
            OrderItems orderItemsCollection = OrderItems.from(List.of(orderItem));
            Order order = Order.create("testUser", orderItemsCollection);

            // act
            order.complete();

            // assert
            assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        }

        @DisplayName("COMPLETED 상태의 주문을 완료 처리하려 하면 예외가 발생한다.")
        @Test
        void throwException_whenTryToCompleteAlreadyCompletedOrder() {
            // arrange
            OrderItem orderItem = new OrderItem(1L, 1, new BigDecimal("10000"));
            OrderItems orderItemsCollection = OrderItems.from(List.of(orderItem));
            Order order = Order.create("testUser", orderItemsCollection);
            order.complete(); // 이미 완료 상태로 변경

            // act & assert
            CoreException exception = assertThrows(CoreException.class, order::complete);
            
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_FORMAT);
            assertThat(exception.getMessage()).contains("대기 중인 주문만 완료할 수 있습니다");
        }

        @DisplayName("PENDING 상태의 주문을 취소하면 CANCELLED 상태로 변경된다.")
        @Test
        void cancelOrder_whenOrderStatusIsPending() {
            // arrange
            OrderItem orderItem = new OrderItem(1L, 1, new BigDecimal("10000"));
            OrderItems orderItemsCollection = OrderItems.from(List.of(orderItem));
            Order order = Order.create("testUser", orderItemsCollection);

            // act
            order.cancel();

            // assert
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @DisplayName("COMPLETED 상태의 주문을 취소하려 하면 예외가 발생한다.")
        @Test
        void throwException_whenTryToCancelCompletedOrder() {
            // arrange
            OrderItem orderItem = new OrderItem(1L, 1, new BigDecimal("10000"));
            OrderItems orderItemsCollection = OrderItems.from(List.of(orderItem));
            Order order = Order.create("testUser", orderItemsCollection);
            order.complete(); // 완료 상태로 변경

            // act & assert
            CoreException exception = assertThrows(CoreException.class, order::cancel);
            
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_FORMAT);
            assertThat(exception.getMessage()).contains("완료된 주문은 취소할 수 없습니다");
        }
    }

    @DisplayName("주문 총액 계산 시, ")
    @Nested
    class TotalAmountCalculation {
        
        @DisplayName("여러 주문 항목의 총액이 올바르게 계산된다.")
        @Test
        void calculateTotalAmount_withMultipleOrderItems() {
            // arrange
            OrderItem orderItem1 = new OrderItem(1L, 2, new BigDecimal("15000")); // 30000
            OrderItem orderItem2 = new OrderItem(2L, 3, new BigDecimal("8000"));  // 24000
            OrderItem orderItem3 = new OrderItem(3L, 1, new BigDecimal("12000")); // 12000
            List<OrderItem> orderItems = List.of(orderItem1, orderItem2, orderItem3);

            // act
            OrderItems orderItemsCollection = OrderItems.from(orderItems);
            Order order = Order.create("testUser", orderItemsCollection);

            // assert
            assertThat(orderItemsCollection.calculateTotalAmount()).isEqualTo(new BigDecimal("66000"));
            assertThat(order.getTotalAmount()).isEqualTo(new BigDecimal("66000"));
        }

        @DisplayName("단일 주문 항목의 총액이 올바르게 계산된다.")
        @Test
        void calculateTotalAmount_withSingleOrderItem() {
            // arrange
            OrderItem orderItem = new OrderItem(1L, 5, new BigDecimal("7000")); // 35000
            List<OrderItem> orderItems = List.of(orderItem);

            // act
            OrderItems orderItemsCollection = OrderItems.from(orderItems);
            Order order = Order.create("testUser", orderItemsCollection);

            // assert
            assertThat(orderItemsCollection.calculateTotalAmount()).isEqualTo(new BigDecimal("35000"));
        }
    }
}
