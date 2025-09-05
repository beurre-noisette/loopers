package com.loopers.infrastructure.kafka.event;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@JsonTypeName("LikeChanged")
public class LikeChangedKafkaEvent extends BaseKafkaEvent {
    private Long productId;
    private Long userId;
    private String action;        // "CREATED" 또는 "CANCELLED"
    private Integer deltaCount;   // +1 또는 -1
    
    @Override
    public String getEventType() {
        return "LikeChanged";
    }
}
