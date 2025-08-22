package com.loopers.domain.payment;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(Long id);

    Optional<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByTransactionKey(String transactionKey);

    List<Payment> findByStatusAndProcessedAtBefore(PaymentStatus status, ZonedDateTime cutoff);

    List<Payment> findByOrderIdAndStatus(Long orderId, PaymentStatus status);

    List<Payment> findByStatusAndCreatedAtBetween(PaymentStatus status, ZonedDateTime startTime, ZonedDateTime endTime);
}
