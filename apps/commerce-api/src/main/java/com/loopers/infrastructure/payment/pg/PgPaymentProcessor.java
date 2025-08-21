package com.loopers.infrastructure.payment.pg;

import com.loopers.domain.payment.PaymentCommand;
import com.loopers.domain.payment.PaymentResult;
import com.loopers.domain.payment.PaymentProcessor;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.domain.payment.command.CardPaymentCommand;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
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
    public PaymentResult processPayment(Long userId, PaymentCommand command) {
        log.info("PG 결제 요청 시작 -> userId: {}, orderId: {}, amount: {}", 
            userId, command.getOrderId(), command.getAmount());

        return switch (command) {
            case CardPaymentCommand cardCommand -> {
                try {
                    validatePaymentCapability(userId, command);

                    PgPaymentDto.PaymentRequest request = PgPaymentDto.PaymentRequest.of(
                            String.valueOf(command.getOrderId()),
                            PgPaymentDto.CardType.valueOf(cardCommand.getCardType()),
                            cardCommand.getCardNo(),
                            command.getAmount(),
                            callbackUrl
                    );

                    ApiResponse<PgPaymentDto.PaymentResponse> response =
                            pgClient.requestPayment("looCommerce", request);

                    if (response.meta().result() == ApiResponse.Metadata.Result.SUCCESS) {
                        PgPaymentDto.PaymentResponse data = response.data();

                        log.info("PG 결제 요청 성공 -> transactionKey: {}, status: {}",
                                data.transactionKey(), data.status());

                        PaymentStatus domainStatus = convertToDomainStatus(data.status());

                        yield new PaymentResult(
                                null,
                                command.getAmount(),
                                domainStatus,
                                ZonedDateTime.now(),
                                String.format("PG 결제 요청 완료 -> transactionKey: %s, status: %s",
                                        data.transactionKey(), data.status()),
                                data.transactionKey()
                        );
                    } else {
                        log.error("PG 결제 요청 실패 -> message: {}", response.meta().message());
                        yield new PaymentResult(
                                null,
                                command.getAmount(),
                                PaymentStatus.FAILED,
                                ZonedDateTime.now(),
                                "PG 결제 요청 실패: " + response.meta().message(),
                                null
                        );
                    }
                } catch (Exception e) {
                    log.error("PG 결제 요청 중 오류 발생 -> userId: {}, orderId: {}",
                            userId, command.getOrderId(), e);

                    yield new PaymentResult(
                            null,
                            command.getAmount(),
                            PaymentStatus.FAILED,
                            ZonedDateTime.now(),
                            "PG 결제 요청 실패: " + e.getMessage(),
                            null
                    );
                }
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

    private PaymentStatus convertToDomainStatus(PgPaymentDto.TransactionStatus pgStatus) {
        return switch (pgStatus) {
            case PENDING -> PaymentStatus.PROCESSING;
            case SUCCESS -> PaymentStatus.SUCCESS;
            case FAILED -> PaymentStatus.FAILED;
        };
    }
}
