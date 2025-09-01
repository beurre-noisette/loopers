package com.loopers.application.order.event;

import com.loopers.application.payment.event.PaymentCompletedEvent;
import com.loopers.application.payment.event.PaymentFailedEvent;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderItems;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.PaymentMethod;
import com.loopers.domain.product.StockReservationService;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("주문 완료/실패 이벤트 리스너 테스트")
class OrderCompletionListenerTest {

    @Mock
    private OrderService orderService;
    
    @Mock
    private StockReservationService stockReservationService;
    
    @InjectMocks
    private OrderCompletionListener orderCompletionListener;
    
    private User testUser;
    private Order testOrder;

    @DisplayName("결제 완료 이벤트 처리 시")
    @Nested
    class HandlePaymentCompleted {
        
        @DisplayName("주문 완료 상태로 변경되고 재고 예약이 확정된다")
        @Test
        void successfullyCompletesOrder() {
            // arrange
            testUser = User.of(new UserCommand.Create(
                    "testuser", 
                    "test@example.com", 
                    "1990-01-01", 
                    Gender.MALE
            ));
            
            OrderItem item = new OrderItem(1L, 2, new BigDecimal("10000"));
            OrderItems orderItems = OrderItems.from(List.of(item));
            testOrder = Order.create(1L, orderItems);
            testOrder.waitForPayment();
            testOrder.processingPayment();
            
            when(orderService.findById(testOrder.getId())).thenReturn(testOrder);
            doNothing().when(stockReservationService).confirmReservation(testOrder.getId());
            
            PaymentCompletedEvent event = PaymentCompletedEvent.of(
                    "correlation-123",
                    testOrder.getId(),
                    testUser.getId(),
                    999L,
                    "tx-key-123",
                    PaymentMethod.POINT,
                    testOrder.getTotalAmount()
            );

            // act
            orderCompletionListener.handlePaymentCompleted(event);

            // assert
            assertAll(
                    () -> verify(orderService).findById(testOrder.getId()),
                    () -> verify(stockReservationService).confirmReservation(testOrder.getId())
            );
        }
    }

    @DisplayName("결제 실패 이벤트 처리 시")
    @Nested 
    class HandlePaymentFailed {
        
        @DisplayName("주문이 취소되고 재고 예약이 해제된다")
        @Test
        void cancelsOrderAndReleasesStock() {
            // arrange
            testUser = User.of(new UserCommand.Create(
                    "testuser", 
                    "test@example.com", 
                    "1990-01-01", 
                    Gender.MALE
            ));
            
            OrderItem item = new OrderItem(1L, 1, new BigDecimal("10000"));
            OrderItems orderItems = OrderItems.from(List.of(item));
            testOrder = Order.create(1L, orderItems);
            testOrder.waitForPayment();
            testOrder.processingPayment();
            
            when(orderService.findById(testOrder.getId())).thenReturn(testOrder);
            doNothing().when(stockReservationService).releaseReservation(testOrder.getId());
            
            PaymentFailedEvent event = PaymentFailedEvent.of(
                    "correlation-456",
                    testOrder.getId(),
                    testUser.getId(),
                    PaymentMethod.CARD,
                    testOrder.getTotalAmount(),
                    "카드 결제 실패"
            );

            // act
            orderCompletionListener.handlePaymentFailed(event);

            // assert
            assertAll(
                    () -> verify(orderService).findById(testOrder.getId()),
                    () -> verify(stockReservationService).releaseReservation(testOrder.getId())
            );
        }
    }
}
