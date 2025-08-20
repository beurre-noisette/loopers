package com.loopers.infrastructure.product;

import com.loopers.domain.product.ReservationStatus;
import com.loopers.domain.product.StockReservation;
import com.loopers.domain.product.StockReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;

@Repository
public class StockReservationRepositoryImpl implements StockReservationRepository {
    
    private final StockReservationJpaRepository stockReservationJpaRepository;

    @Autowired
    public StockReservationRepositoryImpl(StockReservationJpaRepository stockReservationJpaRepository) {
        this.stockReservationJpaRepository = stockReservationJpaRepository;
    }

    @Override
    public StockReservation save(StockReservation stockReservation) {
        return stockReservationJpaRepository.save(stockReservation);
    }

    @Override
    public List<StockReservation> findByOrderId(Long orderId) {
        return stockReservationJpaRepository.findByOrderId(orderId);
    }

    @Override
    public List<StockReservation> findByProductId(Long productId) {
        return stockReservationJpaRepository.findByProductId(productId);
    }

    @Override
    public List<StockReservation> findExpiredReservations() {
        return stockReservationJpaRepository.findByStatusAndExpiresAtBefore(
                ReservationStatus.RESERVED,
                ZonedDateTime.now()
        );
    }

    @Override
    public void deleteByOrderId(Long orderId) {
        stockReservationJpaRepository.deleteByOrderId(orderId);
    }
}
