package com.loopers.application.payment;

import com.loopers.application.order.event.OrderCreatedEvent;
import com.loopers.domain.discount.DiscountService;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.*;
import com.loopers.domain.payment.command.PaymentCommandFactory;
import com.loopers.domain.product.StockReservationService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;

@Component
@Slf4j
public class PaymentFacade {

    private final OrderService orderService;
    private final PaymentProcessorFactory paymentProcessorFactory;
    private final PaymentCommandFactory paymentCommandFactory;
    private final StockReservationService stockReservationService;
    private final UserService userService;
    private final PaymentService paymentService;
    private final DiscountService discountService;

    @Autowired
    public PaymentFacade(
            OrderService orderService,
            PaymentProcessorFactory paymentProcessorFactory,
            PaymentCommandFactory paymentCommandFactory,
            StockReservationService stockReservationService,
            UserService userService,
            PaymentService paymentService,
            DiscountService discountService
    ) {
        this.orderService = orderService;
        this.paymentProcessorFactory = paymentProcessorFactory;
        this.paymentCommandFactory = paymentCommandFactory;
        this.stockReservationService = stockReservationService;
        this.userService = userService;
        this.paymentService = paymentService;
        this.discountService = discountService;
    }

    @Transactional
    public void processPaymentFromEvent(OrderCreatedEvent event) {
        log.info("이벤트 기반 결제 처리 시작 - orderId: {}, amount: {}",
                event.getOrderId(), event.getTotalAmount());

        Order order = orderService.findById(event.getOrderId());
        
        try {
            PaymentCommand paymentCommand = paymentCommandFactory.create(
                    event.getOrderId(),
                    event.getTotalAmount(),
                    event.getPaymentDetails()
            );
            
            User user = userService.findByAccountId(event.getAccountId());
            PaymentProcessor paymentProcessor = paymentProcessorFactory.getPaymentProcessor(paymentCommand.getMethod());
            PaymentResult result = paymentProcessor.processPayment(user.getId(), paymentCommand);
            
            Payment payment = paymentService.createPaymentFromResult(
                    event.getOrderId(),
                    paymentCommand.getMethod(),
                    result
            );

            handlePaymentResult(order, payment, result);

        } catch (Exception e) {
            log.error("이벤트 기반 결제 처리 실패 - orderId: {}", event.getOrderId(), e);
            
            rollbackOrder(order, "결제 처리 중 시스템 오류: " + e.getMessage());
        }
    }

    @Transactional
    public void handlePaymentCallback(String transactionKey, String orderId, String status, String reason) {
        log.info("PG 콜백 처리 시작 - transactionKey: {}, orderId: {}, status: {}",
                transactionKey, orderId, status);

        try {
            Long orderIdInOurService = Long.parseLong(orderId);
            Order order = orderService.findById(orderIdInOurService);
            Payment payment = paymentService.findByOrderId(orderIdInOurService)
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND,
                            "결제 정보를 찾을 수 없습니다. orderId: " + orderId));

            PaymentStatus paymentStatus = PaymentStatus.valueOf(status.toUpperCase());
            PaymentResult result = new PaymentResult(
                    payment.getId(),
                    payment.getAmount(),
                    paymentStatus,
                    ZonedDateTime.now(),
                    reason != null ? reason : "",
                    transactionKey
            );

            handlePaymentResult(order, payment, result);

            log.info("PG 콜백 처리 완료 - transactionKey: {}, orderId: {}", transactionKey, orderId);

        } catch (Exception e) {
            log.error("PG 콜백 처리 실패 - transactionKey: {}, orderId: {}", transactionKey, orderId, e);
            throw e;
        }
    }

    private void handlePaymentResult(Order order, Payment payment, PaymentResult result) {
        switch (result.status()) {
            case SUCCESS -> {
                completeOrder(order);
                log.info("결제 성공 및 주문 완료 - orderId: {}, paymentId: {}, transactionKey: {}",
                        order.getId(), payment.getId(), result.transactionKey());
            }
            case PROCESSING -> {
                order.processingPayment();
                log.info("결제 처리 중 - orderId: {}, paymentId: {}, transactionKey: {}", 
                        order.getId(), payment.getId(), result.transactionKey());
            }
            case FAILED -> {
                log.error("결제 실패 - orderId: {}, paymentId: {}, reason: {}",
                        order.getId(), payment.getId(), result.message());
                rollbackOrder(order, result.message());
            }
        }
    }
    
    private void rollbackOrder(Order order, String failureReason) {
        log.info("주문 롤백 시작 - orderId: {}, reason: {}", order.getId(), failureReason);
        
        try {
            stockReservationService.releaseReservation(order.getId());
            log.info("재고 예약 해제 완료 - orderId: {}", order.getId());
            
            discountService.rollbackDiscount(order.getId());
            log.info("할인 수단 복구 완료 - orderId: {}", order.getId());
            
            order.cancel("결제 실패: " + failureReason);
            log.info("주문 취소 완료 - orderId: {}, status: CANCELLED", order.getId());
        } catch (Exception e) {
            log.error("주문 롤백 중 오류 발생 - orderId: {}", order.getId(), e);
            order.cancel("결제 실패 및 롤백 처리 중 오류: " + e.getMessage());
        }
    }

    private void completeOrder(Order order) {
        try {
            stockReservationService.confirmReservation(order.getId());

            order.completePayment();
        } catch (Exception e) {
            log.error("주문 완료 처리 중 오류 발생 - orderId: {}", order.getId(), e);
            throw e;
        }
    }

}
