package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentJpaRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByTransactionKey(String transactionKey);

    @Query( "SELECT p " +
            "FROM Payment p " +
            "WHERE p.status = :status " +
            "  AND p.processedAt < :cutoff")
    List<Payment> findByStatusAndProcessedAtBefore(
            @Param("status") PaymentStatus status, 
            @Param("cutoff") ZonedDateTime cutoff
    );

    List<Payment> findByOrderIdAndStatus(Long orderId, PaymentStatus status);

    @Query( "SELECT p " +
            "FROM Payment p " +
            "WHERE p.status = :status " +
            "  AND p.createdAt BETWEEN :startTime AND :endTime " +
            "ORDER BY p.createdAt ASC")
    List<Payment> findByStatusAndCreatedAtBetween(
            @Param("status") PaymentStatus status,
            @Param("startTime") ZonedDateTime startTime,
            @Param("endTime") ZonedDateTime endTime
    );
}
