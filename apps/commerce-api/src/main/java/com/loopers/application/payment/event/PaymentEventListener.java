package com.loopers.application.payment.event;

import com.loopers.application.coupon.event.CouponProcessedEvent;
import com.loopers.application.payment.PaymentProcessorFactory;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.*;
import com.loopers.domain.payment.command.PaymentCommandFactory;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class PaymentEventListener {
    
    private final OrderService orderService;
    private final UserService userService;
    private final PaymentProcessorFactory paymentProcessorFactory;
    private final PaymentCommandFactory paymentCommandFactory;
    private final PaymentService paymentService;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public PaymentEventListener(OrderService orderService, UserService userService, PaymentProcessorFactory paymentProcessorFactory, PaymentCommandFactory paymentCommandFactory, PaymentService paymentService, ApplicationEventPublisher eventPublisher) {
        this.orderService = orderService;
        this.userService = userService;
        this.paymentProcessorFactory = paymentProcessorFactory;
        this.paymentCommandFactory = paymentCommandFactory;
        this.paymentService = paymentService;
        this.eventPublisher = eventPublisher;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleCouponProcessed(CouponProcessedEvent event) {
        log.info("쿠폰 처리 이벤트 수신 - orderId: {}, correlationId: {}, couponApplied: {}",
                event.getOrderId(), event.getCorrelationId(), event.isCouponApplied());
        
        Order order = orderService.findById(event.getOrderId());
        User user = userService.findById(event.getUserId());

        processPayment(event, order, user);
    }
    
    private void processPayment(CouponProcessedEvent event, Order order, User user) {
        try {
            PaymentCommand paymentCommand = paymentCommandFactory.create(
                    event.getOrderId(),
                    order.getTotalAmount(),
                    event.getPaymentDetails()
            );
            
            PaymentProcessor paymentProcessor = paymentProcessorFactory.getPaymentProcessor(paymentCommand.getMethod());
            PaymentResult result = paymentProcessor.processPayment(user.getId(), paymentCommand);
            
            Payment payment = paymentService.createPaymentFromResult(
                    event.getOrderId(),
                    paymentCommand.getMethod(),
                    result
            );

            switch (result.status()) {
                case PENDING -> {
                    log.info("PG사 결제 요청 접수 - orderId: {}, transactionKey: {}",
                            event.getOrderId(), payment.getTransactionKey());
                }
                case FAILED -> {
                    publishPaymentFailedEvent(event, result.message());
                }
                case SUCCESS -> {
                    // 포인트가 반환하는 성공 케이스의 PaymentResult
                    PaymentCompletedEvent paymentCompletedEvent = PaymentCompletedEvent.of(
                            event.getCorrelationId(),
                            order.getId(),
                            user.getId(),
                            result.paymentId(),
                            result.transactionKey(),
                            paymentCommand.getMethod(),
                            order.getTotalAmount()
                    );
                    eventPublisher.publishEvent(paymentCompletedEvent);
                }
            }
        } catch (Exception e) {
            log.error("결제 처리 중 오류 발생 - orderId: {}", event.getOrderId(), e);
            publishPaymentFailedEvent(event, e.getMessage());
        }
    }
    
    private void publishPaymentFailedEvent(CouponProcessedEvent event, String failureReason) {
        try {
            Order order = orderService.findById(event.getOrderId());
            
            PaymentFailedEvent failedEvent = PaymentFailedEvent.of(
                    event.getCorrelationId(),
                    event.getOrderId(),
                    event.getUserId(),
                    getPaymentMethodFromDetails(event.getPaymentDetails()),
                    order.getTotalAmount(),
                    failureReason
            );
            
            eventPublisher.publishEvent(failedEvent);
            
            log.info("결제 실패 이벤트 발행 - orderId: {}, correlationId: {}, reason: {}", 
                    event.getOrderId(), event.getCorrelationId(), failureReason);
        } catch (Exception e) {
            log.error("결제 실패 이벤트 발행 실패 - orderId: {}", event.getOrderId(), e);
        }
    }
    
    private PaymentMethod getPaymentMethodFromDetails(PaymentDetails paymentDetails) {
        return switch (paymentDetails) {
            case PaymentDetails.Point point -> PaymentMethod.POINT;
            case PaymentDetails.Card card -> PaymentMethod.CARD;
        };
    }
}
