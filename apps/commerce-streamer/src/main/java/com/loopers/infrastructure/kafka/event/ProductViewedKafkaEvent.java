package com.loopers.infrastructure.kafka.event;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@JsonTypeName("ProductViewed")
public class ProductViewedKafkaEvent extends BaseKafkaEvent {
    private Long productId;
    
    @Override
    public String getEventType() {
        return "ProductViewed";
    }
}
