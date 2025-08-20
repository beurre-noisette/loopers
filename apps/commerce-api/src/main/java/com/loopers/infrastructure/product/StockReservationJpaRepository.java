package com.loopers.infrastructure.product;

import com.loopers.domain.product.ReservationStatus;
import com.loopers.domain.product.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.ZonedDateTime;
import java.util.List;

public interface StockReservationJpaRepository extends JpaRepository<StockReservation,Long> {

    List<StockReservation> findByOrderId(Long orderId);

    List<StockReservation> findByProductId(Long productId);

    List<StockReservation> findByStatusAndExpiresAtBefore(ReservationStatus status, ZonedDateTime now);

    void deleteByOrderId(Long orderId);
}
