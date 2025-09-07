package com.loopers.application.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import com.loopers.infrastructure.payment.pg.PgClient;
import com.loopers.infrastructure.payment.pg.PgPaymentDto;
import com.loopers.interfaces.api.ApiResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;

@Component
@Slf4j
public class PaymentStatusCheckScheduler {

    private final PaymentService paymentService;
    private final PaymentFacade paymentFacade;
    private final PgClient pgClient;
    
    @Value("${scheduler.payment.batch-size:50}")
    private int batchSize;
    
    @Value("${scheduler.payment.check-delay-minutes:1}")
    private int checkDelayMinutes;
    
    @Value("${scheduler.payment.max-delay-minutes:10}")
    private int maxDelayMinutes;

    @Autowired
    public PaymentStatusCheckScheduler(
            PaymentService paymentService,
            PaymentFacade paymentFacade,
            PgClient pgClient
    ) {
        this.paymentService = paymentService;
        this.paymentFacade = paymentFacade;
        this.pgClient = pgClient;
    }

    @Scheduled(fixedDelay = 30000)
    public void checkPendingPayments() {
        try {
            ZonedDateTime checkDelayAgo = ZonedDateTime.now().minusMinutes(checkDelayMinutes);
            ZonedDateTime maxDelayAgo = ZonedDateTime.now().minusMinutes(maxDelayMinutes);

            List<Payment> pendingPayments = paymentService.findPendingPaymentsBetween(
                    maxDelayAgo, checkDelayAgo);

            if (pendingPayments.isEmpty()) {
                log.debug("상태 확인이 필요한 PENDING 결제가 없습니다.");
                return;
            }

            List<Payment> batchPayments = pendingPayments.stream()
                    .limit(batchSize)
                    .toList();

            log.info("PENDING 상태 결제 {}건 상태 확인 시작 (전체: {}건, 배치: {}건)", 
                    batchPayments.size(), pendingPayments.size(), batchSize);

            int successCount = 0;
            int errorCount = 0;

            for (Payment payment : batchPayments) {
                try {
                    checkAndUpdatePaymentStatus(payment);
                    successCount++;
                } catch (Exception e) {
                    errorCount++;
                    log.error("결제 상태 확인 중 오류 발생 -> paymentId: {}, orderId: {}", 
                            payment.getId(), payment.getOrderId(), e);
                }
            }

            log.info("결제 상태 확인 완료 -> 성공: {}건, 실패: {}건", successCount, errorCount);

        } catch (Exception e) {
            log.error("결제 상태 확인 스케줄러 실행 중 오류 발생", e);
        }
    }


    @CircuitBreaker(name = "pgCircuit", fallbackMethod = "fallbackStatusCheck")
    @Retry(name = "pgRetry")
    private void checkAndUpdatePaymentStatus(Payment payment) {
        try {
            log.debug("PG 결제 상태 확인 시작 -> paymentId: {}, orderId: {}", 
                    payment.getId(), payment.getOrderId());

            ApiResponse<PgPaymentDto.TransactionDetailResponse> response;
            if (payment.getTransactionKey() != null) {
                response = pgClient.getTransaction("looCommerce", payment.getTransactionKey());
            } else {
                response = pgClient.getTransactionByOrderId("looCommerce", String.format("%07d", payment.getOrderId()));
            }

            if (response.meta().result() == ApiResponse.Metadata.Result.SUCCESS) {
                PgPaymentDto.TransactionDetailResponse pgTransaction = response.data();
                
                log.info("PG 결제 상태 확인 성공 -> paymentId: {}, orderId: {}, pgStatus: {}, transactionKey: {}", 
                        payment.getId(), payment.getOrderId(), pgTransaction.status(), pgTransaction.transactionKey());

                paymentFacade.handlePaymentCallback(
                        pgTransaction.transactionKey(),
                        pgTransaction.orderId(),
                        pgTransaction.status().name(),
                        pgTransaction.reason()
                );

            } else {
                log.warn("PG 결제 상태 확인 실패 -> paymentId: {}, orderId: {}, message: {}", 
                        payment.getId(), payment.getOrderId(), response.meta().message());
            }

        } catch (Exception e) {
            log.error("PG 결제 상태 확인 중 오류 -> paymentId: {}, orderId: {}", 
                    payment.getId(), payment.getOrderId(), e);
            throw e;
        }
    }

    private void fallbackStatusCheck(Payment payment, Exception exception) {
        log.warn("PG 상태 확인 실패로 Fallback 처리 -> paymentId: {}, orderId: {}, error: {}", 
                payment.getId(), payment.getOrderId(), exception.getMessage());
        
        ZonedDateTime urgentReviewThreshold = ZonedDateTime.now().minusMinutes(30);
        
        if (payment.getCreatedAt().isBefore(urgentReviewThreshold)) {
            log.error("URGENT: 결제 장기 타임아웃 - 수동 검토 필요 -> paymentId: {}, orderId: {}, 생성시간: {}", 
                    payment.getId(), payment.getOrderId(), payment.getCreatedAt());
            
            // 실제 PaymentFailedEvent는 수동 검토 후 처리
        }
    }
}
