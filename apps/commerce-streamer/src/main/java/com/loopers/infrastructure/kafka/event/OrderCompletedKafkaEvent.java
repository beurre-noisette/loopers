package com.loopers.infrastructure.kafka.event;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@SuperBuilder
@NoArgsConstructor
@JsonTypeName("OrderCompleted")
public class OrderCompletedKafkaEvent extends BaseKafkaEvent {
    private Long orderId;
    private Long userId;
    private List<OrderLineItem> items;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderLineItem {
        private Long productId;
        private Integer quantity;
        private Long price;
    }
    
    @Override
    public String getEventType() {
        return "OrderCompleted";
    }
}
