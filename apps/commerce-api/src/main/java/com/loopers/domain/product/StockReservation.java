package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.ZonedDateTime;

@Entity
@Table(name = "stock_reservations")
@Getter
public class StockReservation extends BaseEntity {

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    @Column(nullable = false)
    private ZonedDateTime expiresAt;

    protected StockReservation() {}

    private StockReservation (Long orderId, Long productId, Integer quantity, ZonedDateTime expiresAt) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.expiresAt = expiresAt;
        this.status = ReservationStatus.RESERVED;
    }

    public static StockReservation create(Long orderId, Long productId, Integer quantity) {
        ZonedDateTime expiresAt = ZonedDateTime.now().plusMinutes(30);

        return new StockReservation(orderId, productId, quantity, expiresAt);
    }

    public void confirm() {
        this.status = ReservationStatus.CONFIRMED;
    }

    public void release() {
        this.status = ReservationStatus.RELEASED;
    }
}
