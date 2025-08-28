package com.loopers.application.payment;

import com.loopers.application.payment.event.PaymentCompletedEvent;
import com.loopers.application.payment.event.PaymentFailedEvent;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentResult;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.payment.PaymentStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;

@Component
@Slf4j
public class PaymentFacade {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public PaymentFacade(
            OrderService orderService,
            PaymentService paymentService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void handlePaymentCallback(String transactionKey, String orderId, String status, String reason) {
        log.info("PG 콜백 처리 시작 - transactionKey: {}, orderId: {}, status: {}",
                transactionKey, orderId, status);

        try {
            Long orderIdInOurService = Long.parseLong(orderId);
            Order order = orderService.findById(orderIdInOurService);
            Payment payment = paymentService.findByOrderId(orderIdInOurService);

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
                payment.markSuccess(result.transactionKey());
                PaymentCompletedEvent completedEvent = PaymentCompletedEvent.of(
                        "PG-CALLBACK-" + result.transactionKey(),
                        order.getId(),
                        order.getUserId(),
                        payment.getId(),
                        result.transactionKey(),
                        payment.getMethod(),
                        payment.getAmount()
                );
                eventPublisher.publishEvent(completedEvent);
                log.info("결제 성공 이벤트 발행 - orderId: {}, paymentId: {}, transactionKey: {}",
                        order.getId(), payment.getId(), result.transactionKey());
            }
            case FAILED -> {
                payment.markFailed(result.transactionKey());
                PaymentFailedEvent failedEvent = PaymentFailedEvent.of(
                        "PG-CALLBACK-" + result.transactionKey(),
                        order.getId(),
                        order.getUserId(),
                        payment.getMethod(),
                        payment.getAmount(),
                        result.message()
                );
                eventPublisher.publishEvent(failedEvent);
                log.error("결제 실패 이벤트 발행 - orderId: {}, paymentId: {}, reason: {}",
                        order.getId(), payment.getId(), result.message());
            }
        }
    }

}
