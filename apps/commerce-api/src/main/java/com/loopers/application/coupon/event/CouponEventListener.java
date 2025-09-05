package com.loopers.application.coupon.event;

import com.loopers.application.order.event.OrderCreatedEvent;
import com.loopers.application.order.event.OrderRollbackEvent;
import com.loopers.application.payment.event.PaymentFailedEvent;
import com.loopers.domain.coupon.CouponService;
import com.loopers.support.error.CoreException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class CouponEventListener {
    
    private final CouponService couponService;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public CouponEventListener(CouponService couponService, ApplicationEventPublisher eventPublisher) {
        this.couponService = couponService;
        this.eventPublisher = eventPublisher;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("주문 생성 이벤트 수신 - orderId: {}, correlationId: {}, userCouponId: {}", 
                event.getOrderId(), event.getCorrelationId(), event.getUserCouponId());
        
        try {
            couponService.markCouponAsUsed(event.getUserCouponId(), event.getOrderId());
            
            log.info("쿠폰 사용 처리 완료 - orderId: {}, correlationId: {}, userCouponId: {}", 
                    event.getOrderId(), event.getCorrelationId(), event.getUserCouponId());
            
            CouponProcessedEvent couponEvent = event.getUserCouponId() != null ?
                    CouponProcessedEvent.success(
                            event.getCorrelationId(),
                            event.getOrderId(),
                            event.getUserId(),
                            event.getUserCouponId(),
                            event.getPaymentDetails()
                    ) :
                    CouponProcessedEvent.noCoupon(
                            event.getCorrelationId(),
                            event.getOrderId(),
                            event.getUserId(),
                            event.getPaymentDetails()
                    );
            
            eventPublisher.publishEvent(couponEvent);
            
            log.info("쿠폰 처리 완료, 결제 이벤트 발행 - orderId: {}, correlationId: {}", 
                    event.getOrderId(), event.getCorrelationId());
            
        } catch (CoreException e) {
            log.error("쿠폰 사용 실패, 주문 롤백 필요 - orderId: {}, correlationId: {}", 
                    event.getOrderId(), event.getCorrelationId(), e);
            
            OrderRollbackEvent rollbackEvent = OrderRollbackEvent.forCouponFailure(
                    event.getCorrelationId(),
                    event.getOrderId(),
                    event.getUserId(),
                    event.getUserCouponId(),
                    "쿠폰 사용 실패: " + e.getMessage()
            );
            
            eventPublisher.publishEvent(rollbackEvent);
            
            log.info("쿠폰 사용 실패로 인한 주문 롤백 이벤트 발행 완료 - orderId: {}, correlationId: {}",
                    event.getOrderId(), event.getCorrelationId());
        }
    }
    
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("결제 실패 이벤트 수신, 쿠폰 복구 시작 - orderId: {}, correlationId: {}", 
                event.getOrderId(), event.getCorrelationId());
        
        try {
            couponService.rollbackCouponUsage(event.getOrderId());
            log.info("쿠폰 복구 처리 완료 - orderId: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("쿠폰 복구 실패 - orderId: {}", event.getOrderId(), e);
        }
    }
    
    
}
