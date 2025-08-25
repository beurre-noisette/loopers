package com.loopers.domain.product;

import com.loopers.domain.order.OrderItems;
import com.loopers.infrastructure.product.StockReservationJpaRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StockReservationService {

    private final StockReservationRepository stockReservationRepository;
    private final StockReservationJpaRepository stockReservationJpaRepository;
    private final StockManagementService stockManagementService;

    @Autowired
    public StockReservationService(StockReservationRepository stockReservationRepository, StockReservationJpaRepository stockReservationJpaRepository, StockManagementService stockManagementService) {
        this.stockReservationRepository = stockReservationRepository;
        this.stockReservationJpaRepository = stockReservationJpaRepository;
        this.stockManagementService = stockManagementService;
    }

    @Transactional
    public void reserveStock(Long orderId, List<Product> products, OrderItems orderItems) {
        orderItems.validateStockAvailability(products);

        stockManagementService.decreaseStock(products, orderItems);

        orderItems.getItems().forEach(orderItem -> {
            StockReservation reservation = StockReservation.create(
                    orderId,
                    orderItem.productId(),
                    orderItem.quantity()
            );

            stockReservationRepository.save(reservation);
        });
    }

    @Transactional
    public void confirmReservation(Long orderId) {
        List<StockReservation> reservations = stockReservationRepository.findByOrderId(orderId);
        
        if(reservations.isEmpty()) {
            throw new CoreException(ErrorType.NOT_FOUND, "해당 주문의 예약된 재고를 찾을 수 없습니다.");
        }

        reservations.forEach(reservation -> {
            reservation.confirm();
            stockReservationJpaRepository.save(reservation);
        });
    }

    @Transactional
    public void releaseReservation(Long orderId) {
        List<StockReservation> reservations = stockReservationRepository.findByOrderId(orderId);

        if (reservations.isEmpty()) {
            return;
        }

        stockManagementService.increaseStock(reservations);

        reservations.forEach(reservation -> {
            reservation.release();
            stockReservationJpaRepository.save(reservation);
        });
    }

    @Transactional(readOnly = true)
    public List<StockReservation> getReservations(Long orderId) {
        return stockReservationRepository.findByOrderId(orderId);
    }
}
