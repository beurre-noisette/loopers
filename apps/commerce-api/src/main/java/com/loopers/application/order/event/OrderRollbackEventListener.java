package com.loopers.application.order.event;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.product.StockReservationResult;
import com.loopers.domain.product.StockReservationService;
import com.loopers.infrastructure.kafka.KafkaEventPublisher;
import com.loopers.infrastructure.kafka.event.StockAdjustedKafkaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderRollbackEventListener {
    
    private final OrderService orderService;
    private final StockReservationService stockReservationService;
    private final KafkaEventPublisher kafkaEventPublisher;
    
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleOrderRollback(OrderRollbackEvent event) {
        log.info("주문 롤백 이벤트 수신 - orderId: {}, correlationId: {}, rollbackType: {}, reason: {}", 
                event.getOrderId(), event.getCorrelationId(), event.getRollbackType(), event.getRollbackReason());
        
        try {
            Order order = orderService.findById(event.getOrderId());
            
            List<StockReservationResult> stockResults = stockReservationService.releaseReservation(event.getOrderId());
            log.info("재고 예약 해제 완료 - orderId: {}", event.getOrderId());
            
            stockResults.forEach(result -> {
                StockAdjustedKafkaEvent kafkaEvent = StockAdjustedKafkaEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .aggregateId(result.productId())
                        .productId(result.productId())
                        .adjustedQuantity(result.reservedQuantity())
                        .currentStock(result.currentStock())
                        .occurredAt(ZonedDateTime.now())
                        .build();
                
                kafkaEventPublisher.publishStockAdjustedEvent(kafkaEvent);
                log.debug("재고 복구 이벤트 발행 - productId: {}, adjustedQuantity: {}, currentStock: {}", 
                        result.productId(), result.reservedQuantity(), result.currentStock());
            });
            
            String cancelReason = String.format("%s: %s",
                    getRollbackTypeMessage(event.getRollbackType()), 
                    event.getRollbackReason());
            
            order.cancel(cancelReason);
            log.info("주문 취소 완료 - orderId: {}, status: CANCELLED, reason: {}", 
                    event.getOrderId(), cancelReason);
            
            handleRollbackTypeSpecificActions(event);
            
            log.info("주문 롤백 처리 완료 - orderId: {}, correlationId: {}", 
                    event.getOrderId(), event.getCorrelationId());
            
        } catch (Exception e) {
            log.error("주문 롤백 처리 중 오류 발생 - orderId: {}, correlationId: {}", 
                    event.getOrderId(), event.getCorrelationId(), e);
        }
    }
    
    private void handleRollbackTypeSpecificActions(OrderRollbackEvent event) {
        switch (event.getRollbackType()) {
            case COUPON_USAGE_FAILED -> {
                log.info("쿠폰 사용 실패로 인한 롤백 - 쿠폰 복구는 CouponEventListener에서 처리됨");
                // 쿠폰 복구는 CouponEventListener에서 PaymentFailedEvent를 통해 처리됨
            }
            case PAYMENT_FAILED -> {
                log.info("결제 실패로 인한 롤백 - 이미 PaymentFailedEvent를 통해 처리됨");
                // 이미 PaymentFailedEvent를 통해 필요한 롤백이 처리됨
            }
        }
    }
    
    private String getRollbackTypeMessage(OrderRollbackEvent.RollbackType rollbackType) {
        return switch (rollbackType) {
            case COUPON_USAGE_FAILED -> "쿠폰 사용 실패";
            case PAYMENT_FAILED -> "결제 실패";
        };
    }
}
