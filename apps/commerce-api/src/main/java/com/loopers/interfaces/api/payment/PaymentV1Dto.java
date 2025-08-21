package com.loopers.interfaces.api.payment;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PaymentV1Dto {

    public record CallbackRequest(
            @JsonProperty("transactionKey") String transactionKey,
            @JsonProperty("orderId") String orderId,
            @JsonProperty("cardType") String cardType,
            @JsonProperty("cardNo") String cardNo,
            @JsonProperty("amount") Long amount,
            @JsonProperty("status") String status,
            @JsonProperty("reason") String reason
    ) {}

    public record CallbackResponse(
            @JsonProperty("result") String result,
            @JsonProperty("message") String message
    ) {}
}
