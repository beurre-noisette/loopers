package com.loopers.application.payment.event;

import com.loopers.application.order.event.OrderCreatedEvent;
import com.loopers.application.payment.PaymentFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class PaymentEventListener {
    
    private final PaymentFacade paymentFacade;
    
    @Autowired
    public PaymentEventListener(PaymentFacade paymentFacade) {
        this.paymentFacade = paymentFacade;
    }
    
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("주문 생성 이벤트 수신 - orderId: {}, amount: {}", 
            event.getOrderId(), event.getTotalAmount());
        
        try {
            paymentFacade.processPaymentFromEvent(event);
        } catch (Exception e) {
            log.error("결제 처리 중 오류 발생 - orderId: {}", event.getOrderId(), e);
        }
    }
}
