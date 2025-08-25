package com.loopers.domain.payment;

public sealed interface PaymentDetails
    permits PaymentDetails.Card, PaymentDetails.Point {
    
    record Card(
        String cardType,
        String cardNo
    ) implements PaymentDetails {}
    
    record Point(
    ) implements PaymentDetails {}
}
