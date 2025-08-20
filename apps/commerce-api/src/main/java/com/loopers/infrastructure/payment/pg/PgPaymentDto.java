package com.loopers.infrastructure.payment.pg;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public class PgPaymentDto {

    public record PaymentRequest(
            @JsonProperty("orderId") String orderId,
            @JsonProperty("cardType") CardType cardType,
            @JsonProperty("cardNo") String cardNo,
            @JsonProperty("amount") Long amount,
            @JsonProperty("callbackUrl") String callbackUrl
    ) {
        public static PaymentRequest of(
                String orderId,
                CardType cardType,
                String cardNo,
                BigDecimal amount,
                String callbackUrl
        ) {
            return new PaymentRequest(
                    orderId,
                    cardType,
                    cardNo,
                    amount.longValue(),
                    callbackUrl
            );
        }
    }

    public enum CardType {
        SAMSUNG,
        KB,
        HYUNDAI
    }

    public enum TransactionStatus {
        PENDING,
        SUCCESS,
        FAILED
    }

    public record PaymentResponse(
            @JsonProperty("transactionKey") String transactionKey,
            @JsonProperty("status") TransactionStatus status,
            @JsonProperty("reason") String reason
    ) {}

    public record TransactionDetailResponse(
            @JsonProperty("transactionKey") String transactionKey,
            @JsonProperty("orderId") String orderId,
            @JsonProperty("cardType") CardType cardType,
            @JsonProperty("cardNo") String cardNo,
            @JsonProperty("amount") Long amount,
            @JsonProperty("status") TransactionStatus status,
            @JsonProperty("reason") String reason
    ) {}

    public record OrderResponse(
            @JsonProperty("orderId") String orderId,
            @JsonProperty("transactions")List<PaymentResponse> transactions
    ) {}
}
