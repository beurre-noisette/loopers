package com.loopers.domain.product;

import java.util.List;

public interface StockReservationRepository {

    StockReservation save(StockReservation stockReservation);

    List<StockReservation> findByOrderId(Long orderId);

    List<StockReservation> findByProductId(Long productId);

    List<StockReservation> findExpiredReservations();

    void deleteByOrderId(Long orderId);
}
