package com.loopers.application.order.event;

import com.loopers.application.payment.event.PaymentCompletedEvent;
import com.loopers.application.payment.event.PaymentFailedEvent;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.product.StockReservationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class OrderCompletionListener {
    
    private final OrderService orderService;
    private final StockReservationService stockReservationService;

    @Autowired
    public OrderCompletionListener(OrderService orderService, StockReservationService stockReservationService) {
        this.orderService = orderService;
        this.stockReservationService = stockReservationService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("결제 완료 이벤트 수신 - orderId: {}, correlationId: {}, paymentId: {}, transactionKey: {}", 
                event.getOrderId(), event.getCorrelationId(), event.getPaymentId(), event.getTransactionKey());
        
        try {
            Order order = orderService.findById(event.getOrderId());
            
            stockReservationService.confirmReservation(event.getOrderId());
            log.info("재고 예약 확정 완료 - orderId: {}", event.getOrderId());
            
            order.completePayment();
            log.info("주문 완료 처리 - orderId: {}, status: COMPLETED", event.getOrderId());
            
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
}
