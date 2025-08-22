package com.loopers.application.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.infrastructure.payment.pg.PgClient;
import com.loopers.infrastructure.payment.pg.PgPaymentDto;
import com.loopers.interfaces.api.ApiResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

@Component
@Slf4j
public class PaymentStatusCheckScheduler {

    private final PaymentService paymentService;
    private final PaymentFacade paymentFacade;
    private final PgClient pgClient;

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
    @Transactional
    public void checkProcessingPayments() {
        try {
            ZonedDateTime oneMinuteAgo = ZonedDateTime.now().minusMinutes(1);
            ZonedDateTime tenMinutesAgo = ZonedDateTime.now().minusMinutes(10);

            List<Payment> processingPayments = paymentService.findProcessingPaymentsBetween(
                    tenMinutesAgo, oneMinuteAgo);

            if (processingPayments.isEmpty()) {
                return;
            }

            log.info("PROCESSING 상태 결제 {}건 상태 확인 시작", processingPayments.size());

            for (Payment payment : processingPayments) {
                try {
                    checkAndUpdatePaymentStatus(payment);
                } catch (Exception e) {
                    log.error("결제 상태 확인 중 오류 발생 -> paymentId: {}, orderId: {}", 
                            payment.getId(), payment.getOrderId(), e);
                }
            }

            log.info("결제 상태 확인 완료 -> 처리된 건수: {}", processingPayments.size());

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

            ApiResponse<PgPaymentDto.TransactionDetailResponse> response =
                    pgClient.getTransactionByOrderId("looCommerce", String.valueOf(payment.getOrderId()));

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
    }
}
