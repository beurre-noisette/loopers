package com.loopers.infrastructure.kafka.event;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@JsonTypeName("StockAdjusted")
public class StockAdjustedKafkaEvent extends BaseKafkaEvent {
    private Long productId;
    private Integer adjustedQuantity;
    private Integer currentStock;
    
    @Override
    public String getEventType() {
        return "StockAdjusted";
    }
}
