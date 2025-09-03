package com.loopers.infrastructure.kafka.event;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.ZonedDateTime;

@Getter
@SuperBuilder
@NoArgsConstructor
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "eventType"
)
public abstract class BaseKafkaEvent {
    private String eventId;
    private Long aggregateId;
    private ZonedDateTime occurredAt;
    
    public abstract String getEventType();
}
