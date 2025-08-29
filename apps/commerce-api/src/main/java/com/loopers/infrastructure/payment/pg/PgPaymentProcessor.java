package com.loopers.infrastructure.payment.pg;

import com.loopers.domain.payment.PaymentCommand;
import com.loopers.domain.payment.PaymentProcessor;
import com.loopers.domain.payment.PaymentResult;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.domain.payment.command.CardPaymentCommand;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Service("pgPaymentProcessor")
@Slf4j
public class PgPaymentProcessor implements PaymentProcessor {

    private final PgClient pgClient;
    private final String callbackUrl;

    @Autowired
    public PgPaymentProcessor(
            PgClient pgClient,
            @Value("${pg.callback.url:http://host.docker.internal:8080/api/v1/payments/callback}") String callbackUrl
    ) {
        this.pgClient = pgClient;
        this.callbackUrl = callbackUrl;
    }

    @Override
    public void validatePaymentCapability(Long userId, PaymentCommand command) {
        if (command.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 금액은 0보다 커야 합니다.");
        }

        if (command.getAmount().compareTo(new BigDecimal("10000000")) > 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "1회 PG 결제 한도를 초과했습니다.");
        }

        if (!(command instanceof CardPaymentCommand)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "PG 결제 서비스는 카드 결제만 지원합니다.");
        }
    }

    @Override
    @CircuitBreaker(name = "pgCircuit", fallbackMethod = "fallbackPayment")
    @Retry(name = "pgRetry")
    public PaymentResult processPayment(Long userId, PaymentCommand command) {
        log.info("PG 결제 요청 시작 -> userId: {}, orderId: {}, amount: {}",
                userId, command.getOrderId(), command.getAmount());

        return switch (command) {
            case CardPaymentCommand cardCommand -> {
                validatePaymentCapability(userId, command);

                yield requestPgPayment(cardCommand);
            }
            default -> {
                log.error("PG 결제 서비스는 현재 카드 결제만 지원합니다 -> command: {}", command.getClass().getSimpleName());
                yield new PaymentResult(
                        null,
                        command.getAmount(),
                        PaymentStatus.FAILED,
                        ZonedDateTime.now(),
                        "PG 결제 서비스는 현재 카드 결제만 지원합니다.",
                        null
                );
            }
        };
    }

    private PaymentResult requestPgPayment(CardPaymentCommand cardCommand) {
        PgPaymentDto.PaymentRequest request = PgPaymentDto.PaymentRequest.of(
                String.valueOf(cardCommand.getOrderId()),
                PgPaymentDto.CardType.valueOf(cardCommand.getCardType()),
                cardCommand.getCardNo(),
                cardCommand.getAmount(),
                callbackUrl
        );

        ApiResponse<PgPaymentDto.PaymentResponse> response =
                pgClient.requestPayment("looCommerce", request);

        PgPaymentDto.PaymentResponse data = response.data();

        log.info("PG 결제 요청 성공 -> transactionalKey: {}, status: {}",
                data.transactionKey(), data.status());

        return new PaymentResult(
                null,
                cardCommand.getAmount(),
                PaymentStatus.PENDING,
                ZonedDateTime.now(),
                String.format("PG 결제 요청 완료 -> transactionKey: %s, status: %s",
                        data.transactionKey(), data.status()),
                data.transactionKey()
        );
    }


    private PaymentResult fallbackPayment(Long userId, PaymentCommand command, Exception exception) {
        log.warn("PG 결제 시스템 장애로 인한 Fallback 처리 -> userId: {}, orderId: {}, error: {}",
                userId, command.getOrderId(), exception.getMessage());

        return new PaymentResult(
                null,
                command.getAmount(),
                PaymentStatus.FAILED,
                ZonedDateTime.now(),
                "PG 시스템 장애로 결제 요청 실패. 잠시 후 다시 시도해주세요.",
                null
        );
    }
}
