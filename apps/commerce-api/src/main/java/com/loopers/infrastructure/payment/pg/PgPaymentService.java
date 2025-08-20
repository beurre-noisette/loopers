package com.loopers.infrastructure.payment.pg;

import com.loopers.domain.payment.*;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Service("pgPaymentService")
@Slf4j
public class PgPaymentService implements PaymentService {

    private final PgClient pgClient;
    private final String callbackUrl;

    @Autowired
    public PgPaymentService(
            PgClient pgClient,
            @Value("${pg.callback.url:http://localhost:8080/api/v1/payments/callback}") String callbackUrl
    ) {
        this.pgClient = pgClient;
        this.callbackUrl = callbackUrl;
    }

    @Override
    public void validatePaymentCapability(Long userId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 금액은 0보다 커야 합니다.");
        }

        if (amount.compareTo(new BigDecimal("10000000")) > 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "1회 PG 결제 한도를 초과했습니다.");
        }
    }

    @Override
    public PaymentResult processPayment(Long userId, BigDecimal amount, PaymentReference reference) {
        log.info("PG 결제 요청 시작 -> userId: {}, orderId: {}, amount: {}", userId, reference.referenceId(),amount);

        try {
            PaymentCommand.CardInfo cardInfo = reference.cardInfo();
            
            if (cardInfo == null) {
                throw new CoreException(ErrorType.INVALID_INPUT_FORMAT, "카드 결제 시 카드 정보는 필수입니다.");
            }

            PgPaymentDto.PaymentRequest request = PgPaymentDto.PaymentRequest.of(
                    String.valueOf(reference.referenceId()),
                    PgPaymentDto.CardType.valueOf(cardInfo.cardType()),
                    cardInfo.cardNo(),
                    amount,
                    callbackUrl
            );

            ApiResponse<PgPaymentDto.PaymentResponse> response =
                    pgClient.requestPayment(userId.toString(), request);

            if (response.meta().result() == ApiResponse.Metadata.Result.SUCCESS) {
                PgPaymentDto.PaymentResponse data = response.data();

                log.info("PG 결제 요청 성공 -> transactionKey: {}, status: {}",
                        response.data().transactionKey(), response.data().status());

                PaymentStatus domainStatus = convertToDomainStatus(data.status());

                return new PaymentResult(
                        null,
                        amount,
                        domainStatus,
                        ZonedDateTime.now(),
                        String.format("PG 결제 요청 완료 -> transactionKey: %s, statsu %s",
                                data.transactionKey(), data.status())
                );
            } else {
                log.error("PG 결제 요청 실패 -> message: {}", response.meta().message());
                throw new CoreException(ErrorType.INTERNAL_ERROR, "PG 결제 요청 실패: " + response.meta().message());
            }
        } catch (Exception e) {
            log.error("PG 결제 요청 중 오류 발생 -> userId: {}, orderId: {}",
                    userId, reference.referenceId(), e);

            return new PaymentResult(
                    null,
                    amount,
                    PaymentStatus.FAILED,
                    ZonedDateTime.now(),
                    "PG 결제 요청 실패: " + e.getMessage()
            );
        }
    }

    private PaymentStatus convertToDomainStatus(PgPaymentDto.TransactionStatus pgStatus) {
        return switch (pgStatus) {
            case PENDING -> PaymentStatus.PROCESSING;
            case SUCCESS -> PaymentStatus.SUCCESS;
            case FAILED -> PaymentStatus.FAILED;
        };
    }
}
