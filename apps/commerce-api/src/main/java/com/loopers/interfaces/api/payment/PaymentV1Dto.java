package com.loopers.interfaces.api.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.loopers.domain.payment.PaymentCommand;
import com.loopers.domain.payment.PaymentMethod;

public class PaymentV1Dto {

    public record ProcessRequest(
            @JsonProperty("orderId") Long orderId,
            @JsonProperty("method") String method,
            @JsonProperty("cardInfo") CardInfoRequest cardInfo
    ) {
        public PaymentCommand.ProcessPayment toCommand() {
            PaymentMethod paymentMethod = PaymentMethod.valueOf(method.toUpperCase());

            PaymentCommand.CardInfo cardInfo = null;
            if (this.cardInfo != null) {
                cardInfo = new PaymentCommand.CardInfo(
                        this.cardInfo.cardType(),
                        this.cardInfo.cardNo()
                );
            }

            return new PaymentCommand.ProcessPayment(orderId, paymentMethod, cardInfo);
        }
    }

    public record CardInfoRequest(
            @JsonProperty("cardType") String cardType,
            @JsonProperty("cardNo") String cardNo
    ) {}
}
