package com.loopers.infrastructure.kafka.event;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.loopers.domain.order.OrderItem;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@SuperBuilder
@NoArgsConstructor
@JsonTypeName("OrderCompleted")
public class OrderCompletedKafkaEvent extends BaseKafkaEvent {
    private Long orderId;
    private Long userId;
    private List<OrderItem> items;
    
    @Override
    public String getEventType() {
        return "OrderCompleted";
    }
}
