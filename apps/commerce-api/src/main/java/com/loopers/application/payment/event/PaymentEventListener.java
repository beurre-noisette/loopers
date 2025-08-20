package com.loopers.application.payment.event;

import com.loopers.application.order.event.OrderCreatedEvent;
import com.loopers.application.payment.PaymentFacade;
import com.loopers.domain.payment.PaymentCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

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
        log.info("주문 생성 이벤트 수신 - orderId: {}, paymentMethod: {}", 
            event.getOrderId(), event.getPaymentMethod());
        
        try {
            PaymentCommand.CardInfo cardInfo = null;
            if (event.getCardInfo() != null) {
                cardInfo = new PaymentCommand.CardInfo(
                    event.getCardInfo().cardType(),
                    event.getCardInfo().cardNo()
                );
            }
            
            PaymentCommand.ProcessPayment command = new PaymentCommand.ProcessPayment(
                event.getOrderId(),
                event.getPaymentMethod(),
                cardInfo
            );
            
            paymentFacade.processPayment(event.getUserId(), command);
            
        } catch (Exception e) {
            log.error("결제 처리 중 오류 발생 - orderId: {}", event.getOrderId(), e);
        }
    }
}
