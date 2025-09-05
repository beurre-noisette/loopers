package com.loopers.domain.product;

public record StockReservationResult(
    Long productId,
    Integer reservedQuantity,
    Integer currentStock
) {
    public static StockReservationResult of(
            Long productId,
            Integer reservedQuantity,
            Integer currentStock
    ) {
        return new StockReservationResult(productId, reservedQuantity, currentStock);
    }
}
