package com.loopers.application.order.event;

import com.loopers.application.payment.event.PaymentCompletedEvent;
import com.loopers.application.payment.event.PaymentFailedEvent;
import com.loopers.application.product.ProductQuery;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.product.StockReservationResult;
import com.loopers.domain.product.StockReservationService;
import com.loopers.infrastructure.kafka.KafkaEventPublisher;
import com.loopers.infrastructure.kafka.event.OrderCompletedKafkaEvent;
import com.loopers.infrastructure.kafka.event.StockAdjustedKafkaEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
public class OrderCompletionListener {
    
    private final OrderService orderService;
    private final StockReservationService stockReservationService;
    private final KafkaEventPublisher kafkaEventPublisher;
    private final ProductQuery productQuery;

    @Autowired
    public OrderCompletionListener(OrderService orderService, 
                                  StockReservationService stockReservationService,
                                  KafkaEventPublisher kafkaEventPublisher,
                                  ProductQuery productQuery) {
        this.orderService = orderService;
        this.stockReservationService = stockReservationService;
        this.kafkaEventPublisher = kafkaEventPublisher;
        this.productQuery = productQuery;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("결제 완료 이벤트 수신 - orderId: {}, correlationId: {}, paymentId: {}, transactionKey: {}", 
                event.getOrderId(), event.getCorrelationId(), event.getPaymentId(), event.getTransactionKey());
        
        try {
            Order order = orderService.findById(event.getOrderId());
            
            List<StockReservationResult> stockResults = stockReservationService.confirmReservation(event.getOrderId());
            log.info("재고 예약 확정 완료 - orderId: {}", event.getOrderId());
            
            stockResults.forEach(result -> {
                StockAdjustedKafkaEvent kafkaEvent = StockAdjustedKafkaEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .aggregateId(result.productId())
                        .productId(result.productId())
                        .adjustedQuantity(-result.reservedQuantity())
                        .currentStock(result.currentStock())
                        .occurredAt(ZonedDateTime.now())
                        .build();
                
                kafkaEventPublisher.publishStockAdjustedEvent(kafkaEvent);
                log.debug("재고 확정 이벤트 발행 - productId: {}, adjustedQuantity: {}, currentStock: {}", 
                        result.productId(), -result.reservedQuantity(), result.currentStock());
                
                // 재고 0인 경우 캐시 무효화
                if (result.currentStock() == 0) {
                    productQuery.evictProductDetailCache(result.productId());
                    log.info("재고 소진으로 캐시 무효화 - productId: {}", result.productId());
                }
            });
            
            order.completePayment();
            log.info("주문 완료 처리 - orderId: {}, status: COMPLETED", event.getOrderId());
            
            publishOrderCompletedToKafka(event, order);
            
            log.info("주문 처리 완료 - orderId: {}, correlationId: {}", 
                    event.getOrderId(), event.getCorrelationId());
            
        } catch (Exception e) {
            log.error("주문 완료 처리 중 오류 발생 - orderId: {}, correlationId: {}", 
                    event.getOrderId(), event.getCorrelationId(), e);
        }
    }
    
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("결제 실패 이벤트 수신 - orderId: {}, correlationId: {}, reason: {}", 
                event.getOrderId(), event.getCorrelationId(), event.getFailureReason());
        
        try {
            Order order = orderService.findById(event.getOrderId());
            
            stockReservationService.releaseReservation(event.getOrderId());
            log.info("재고 예약 해제 완료 - orderId: {}", event.getOrderId());
            
            order.cancel("결제 실패: " + event.getFailureReason());
            log.info("주문 취소 완료 - orderId: {}, status: CANCELLED", event.getOrderId());

        } catch (Exception e) {
            log.error("주문 롤백 처리 중 오류 발생 - orderId: {}, correlationId: {}", 
                    event.getOrderId(), event.getCorrelationId(), e);
        }
    }
    
    private void publishOrderCompletedToKafka(PaymentCompletedEvent paymentEvent, Order order) {
        try {
            OrderCompletedKafkaEvent kafkaEvent = OrderCompletedKafkaEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .aggregateId(order.getId())
                    .occurredAt(ZonedDateTime.now())
                    .orderId(order.getId())
                    .userId(order.getUserId())
                    .items(order.getOrderItems().getItems())
                    .build();
                    
            kafkaEventPublisher.publishOrderCompletedEvent(kafkaEvent);
            
            log.info("Kafka 이벤트 발행 요청 - eventId: {}, orderId: {}, userId: {}, items: {}",
                    kafkaEvent.getEventId(), order.getId(), order.getUserId(), 
                    kafkaEvent.getItems().size());
                    
        } catch (Exception e) {
            log.error("Kafka 이벤트 발행 실패 - orderId: {}, correlationId: {}",
                    order.getId(), paymentEvent.getCorrelationId(), e);
        }
    }
}
