package com.loopers.infrastructure.dataplatform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.coupon.event.CouponProcessedEvent;
import com.loopers.application.order.event.OrderCreatedEvent;
import com.loopers.application.payment.event.PaymentCompletedEvent;
import com.loopers.application.payment.event.PaymentFailedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Component
@Slf4j
public class DataPlatformEventLogger {

    private final ObjectMapper objectMapper;

    @Autowired
    public DataPlatformEventLogger(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreated(OrderCreatedEvent event) {
        DataPlatformEvent platformEvent = DataPlatformEvent.orderEvent("order_created")
                .correlationId(event.getCorrelationId())
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .payload(Map.of(
                        "userCouponId", event.getUserCouponId(),
                        "paymentMethod", getPaymentMethod(event.getPaymentDetails())
                ))
                .build();

        logDataPlatformEvent(platformEvent);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCouponProcessed(CouponProcessedEvent event) {
        DataPlatformEvent platformEvent = DataPlatformEvent.orderEvent("coupon_processed")
                .correlationId(event.getCorrelationId())
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .payload(Map.of(
                        "couponApplied", event.isCouponApplied(),
                        "userCouponId", event.getUserCouponId()
                ))
                .build();

        logDataPlatformEvent(platformEvent);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        DataPlatformEvent platformEvent = DataPlatformEvent.orderEvent("payment_completed")
                .correlationId(event.getCorrelationId())
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .payload(Map.of(
                        "paymentId", event.getPaymentId(),
                        "transactionKey", event.getTransactionKey(),
                        "paymentMethod", event.getPaymentMethod().name(),
                        "paidAmount", event.getPaidAmount()
                ))
                .build();

        logDataPlatformEvent(platformEvent);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentFailed(PaymentFailedEvent event) {
        DataPlatformEvent platformEvent = DataPlatformEvent.orderEvent("payment_failed")
                .correlationId(event.getCorrelationId())
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .payload(Map.of(
                        "paymentMethod", event.getPaymentMethod().name(),
                        "attemptedAmount", event.getAttemptedAmount(),
                        "failureReason", event.getFailureReason()
                ))
                .build();

        logDataPlatformEvent(platformEvent);
    }

    private void logDataPlatformEvent(DataPlatformEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            log.info("DATA_PLATFORM: {}", json);
        } catch (Exception e) {
            log.error("데이터 플랫폼 이벤트 로깅 실패 - eventType: {}, correlationId: {}",
                    event.getEventType(), event.getCorrelationId(), e);
        }
    }

    private String getPaymentMethod(Object paymentDetails) {
        return paymentDetails.getClass().getSimpleName().replace("Details", "").toUpperCase();
    }
}
