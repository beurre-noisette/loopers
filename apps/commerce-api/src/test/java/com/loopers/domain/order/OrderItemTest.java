package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class OrderItemTest {

    @DisplayName("주문 항목(OrderItem)을 생성할 때 ")
    @Nested
    class Create {
        
        @DisplayName("올바른 상품ID, 수량, 단가가 주어지면 정상적으로 생성된다.")
        @Test
        void createOrderItem_whenValidInputsProvided() {
            // arrange
            Long productId = 1L;
            int quantity = 3;
            BigDecimal unitPrice = new BigDecimal("15000");

            // act
            OrderItem orderItem = new OrderItem(productId, quantity, unitPrice);

            // assert
            assertAll(
                    () -> assertThat(orderItem.productId()).isEqualTo(productId),
                    () -> assertThat(orderItem.quantity()).isEqualTo(quantity),
                    () -> assertThat(orderItem.unitPrice()).isEqualTo(unitPrice),
                    () -> assertThat(orderItem.getTotalPrice()).isEqualTo(new BigDecimal("45000"))
            );
        }

        @DisplayName("상품ID가 null일 경우 생성에 실패하고 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwBadRequestException_whenProductIdIsNull() {
            // act & assert
            CoreException exception = assertThrows(CoreException.class,
                () -> new OrderItem(null, 1, new BigDecimal("10000")));
            
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("상품 ID는 필수값입니다");
        }

        @DisplayName("상품ID가 0 이하일 경우 생성에 실패하고 BAD_REQUEST 예외가 발생한다.")
        @ParameterizedTest
        @ValueSource(longs = {0L, -1L, -10L})
        void throwBadRequestException_whenProductIdIsZeroOrNegative(Long productId) {
            // act & assert
            CoreException exception = assertThrows(CoreException.class,
                () -> new OrderItem(productId, 1, new BigDecimal("10000")));
            
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("상품 ID는 필수값입니다");
        }

        @DisplayName("수량이 0 이하일 경우 생성에 실패하고 INVALID_INPUT_FORMAT 예외가 발생한다.")
        @ParameterizedTest
        @ValueSource(ints = {0, -1, -5})
        void throwInvalidInputFormatException_whenQuantityIsZeroOrNegative(int quantity) {
            // act & assert
            CoreException exception = assertThrows(CoreException.class,
                () -> new OrderItem(1L, quantity, new BigDecimal("10000")));
            
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_FORMAT);
            assertThat(exception.getMessage()).contains("주문 수량은 1개 이상이어야 합니다");
        }

        @DisplayName("단가가 null일 경우 생성에 실패하고 INVALID_INPUT_FORMAT 예외가 발생한다.")
        @Test
        void throwInvalidInputFormatException_whenUnitPriceIsNull() {
            // act & assert
            CoreException exception = assertThrows(CoreException.class,
                () -> new OrderItem(1L, 1, null));
            
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_FORMAT);
            assertThat(exception.getMessage()).contains("상품 단가는 0원보다 커야 합니다");
        }

        @DisplayName("단가가 0 이하일 경우 생성에 실패하고 INVALID_INPUT_FORMAT 예외가 발생한다.")
        @Test
        void throwInvalidInputFormatException_whenUnitPriceIsZeroOrNegative() {
            // act & assert
            CoreException exception = assertThrows(CoreException.class,
                () -> new OrderItem(1L, 1, BigDecimal.ZERO));
            
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_FORMAT);
            assertThat(exception.getMessage()).contains("상품 단가는 0원보다 커야 합니다");

            CoreException exception2 = assertThrows(CoreException.class,
                () -> new OrderItem(1L, 1, new BigDecimal("-1000")));
            
            assertThat(exception2.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_FORMAT);
            assertThat(exception2.getMessage()).contains("상품 단가는 0원보다 커야 합니다");
        }
    }

    @DisplayName("주문 항목 총액 계산 시 ")
    @Nested
    class TotalPriceCalculation {
        
        @DisplayName("수량과 단가를 곱한 값이 정확히 계산된다.")
        @Test
        void calculateTotalPrice_correctly() {
            // arrange & act
            OrderItem orderItem1 = new OrderItem(1L, 3, new BigDecimal("5000"));
            OrderItem orderItem2 = new OrderItem(2L, 10, new BigDecimal("1500"));
            OrderItem orderItem3 = new OrderItem(3L, 1, new BigDecimal("25000"));

            // assert
            assertAll(
                    () -> assertThat(orderItem1.getTotalPrice()).isEqualTo(new BigDecimal("15000")),
                    () -> assertThat(orderItem2.getTotalPrice()).isEqualTo(new BigDecimal("15000")),
                    () -> assertThat(orderItem3.getTotalPrice()).isEqualTo(new BigDecimal("25000"))
            );
        }

        @DisplayName("소수점이 있는 단가의 총액도 정확히 계산된다.")
        @Test
        void calculateTotalPrice_withDecimalUnitPrice() {
            // arrange & act
            OrderItem orderItem = new OrderItem(1L, 3, new BigDecimal("1234.56"));

            // assert
            assertThat(orderItem.getTotalPrice()).isEqualTo(new BigDecimal("3703.68"));
        }
    }

    @DisplayName("OrderItem의 불변성 테스트")
    @Nested
    class ImmutabilityTest {
        
        @DisplayName("생성 후 필드 값들이 변경되지 않는다.")
        @Test
        void orderItemFieldsAreImmutable() {
            // arrange
            Long productId = 1L;
            int quantity = 5;
            BigDecimal unitPrice = new BigDecimal("10000");

            // act
            OrderItem orderItem = new OrderItem(productId, quantity, unitPrice);

            // assert
            assertAll(
                    () -> assertThat(orderItem.productId()).isEqualTo(productId),
                    () -> assertThat(orderItem.quantity()).isEqualTo(quantity),
                    () -> assertThat(orderItem.unitPrice()).isEqualTo(unitPrice)
            );
        }
    }
}
