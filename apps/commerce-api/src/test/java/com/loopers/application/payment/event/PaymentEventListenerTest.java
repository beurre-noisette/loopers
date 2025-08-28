package com.loopers.application.payment.event;

import com.loopers.application.coupon.event.CouponProcessedEvent;
import com.loopers.application.payment.PaymentProcessorFactory;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderItems;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.*;
import com.loopers.domain.payment.command.PaymentCommandFactory;
import com.loopers.domain.payment.command.PointPaymentCommand;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserCommand;
import com.loopers.domain.user.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("결제 이벤트 리스너 테스트")
class PaymentEventListenerTest {

    @Mock
    private OrderService orderService;
    
    @Mock
    private UserService userService;
    
    @Mock
    private PaymentProcessorFactory paymentProcessorFactory;
    
    @Mock
    private PaymentCommandFactory paymentCommandFactory;
    
    @Mock
    private PaymentService paymentService;
    
    @Mock
    private ApplicationEventPublisher eventPublisher;
    
    @Mock
    private PaymentProcessor paymentProcessor;
    
    @InjectMocks
    private PaymentEventListener paymentEventListener;
    
    private User testUser;
    private Order testOrder;
    private PointPaymentCommand testPaymentCommand;
    private PaymentResult testPaymentResult;

    @DisplayName("쿠폰 처리 완료 이벤트 처리 시")
    @Nested
    class HandleCouponProcessed {
        
        @DisplayName("포인트 결제가 성공적으로 처리되고 성공 이벤트를 발행한다")
        @Test
        void successfullyProcessesPointPaymentAndPublishesSuccessEvent() {
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
            
            testPaymentCommand = new PointPaymentCommand(testOrder.getId(), testOrder.getTotalAmount());
            testPaymentResult = new PaymentResult(
                    123L, 
                    testOrder.getTotalAmount(), 
                    PaymentStatus.SUCCESS, 
                    ZonedDateTime.now(), 
                    "", 
                    "POINT_" + testOrder.getId()
            );
            
            when(orderService.findById(testOrder.getId())).thenReturn(testOrder);
            when(userService.findById(testUser.getId())).thenReturn(testUser);
            when(paymentCommandFactory.create(eq(testOrder.getId()), eq(testOrder.getTotalAmount()), any(PaymentDetails.Point.class)))
                    .thenReturn(testPaymentCommand);
            when(paymentProcessorFactory.getPaymentProcessor(PaymentMethod.POINT)).thenReturn(paymentProcessor);
            when(paymentProcessor.processPayment(testUser.getId(), testPaymentCommand)).thenReturn(testPaymentResult);
            when(paymentService.createPaymentFromResult(testOrder.getId(), PaymentMethod.POINT, testPaymentResult))
                    .thenReturn(mock(Payment.class));

            CouponProcessedEvent event = CouponProcessedEvent.noCoupon(
                    "correlation-123",
                    testOrder.getId(),
                    testUser.getId(),
                    new PaymentDetails.Point()
            );

            // act
            paymentEventListener.handleCouponProcessed(event);

            // assert
            verify(paymentProcessor).processPayment(testUser.getId(), testPaymentCommand);
            verify(eventPublisher).publishEvent(any(PaymentCompletedEvent.class));
        }

        @DisplayName("결제 처리 중 예외 발생 시 실패 이벤트를 발행한다")
        @Test
        void publishesFailedEventWhenPaymentProcessingFails() {
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
            
            testPaymentCommand = new PointPaymentCommand(testOrder.getId(), testOrder.getTotalAmount());
            
            when(orderService.findById(testOrder.getId())).thenReturn(testOrder);
            when(userService.findById(testUser.getId())).thenReturn(testUser);
            when(paymentCommandFactory.create(eq(testOrder.getId()), eq(testOrder.getTotalAmount()), any(PaymentDetails.Point.class)))
                    .thenReturn(testPaymentCommand);
            when(paymentProcessorFactory.getPaymentProcessor(PaymentMethod.POINT)).thenReturn(paymentProcessor);
            when(paymentProcessor.processPayment(testUser.getId(), testPaymentCommand))
                    .thenThrow(new RuntimeException("포인트 잔액 부족"));

            CouponProcessedEvent event = CouponProcessedEvent.noCoupon(
                    "correlation-456",
                    testOrder.getId(),
                    testUser.getId(),
                    new PaymentDetails.Point()
            );

            // act
            paymentEventListener.handleCouponProcessed(event);

            // assert
            verify(eventPublisher).publishEvent(any(PaymentFailedEvent.class));
        }
    }
}
