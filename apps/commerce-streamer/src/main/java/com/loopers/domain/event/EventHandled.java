package com.loopers.domain.event;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Entity
@Table(name = "event_handled", indexes = {
    @Index(name = "idx_event_handled_event_consumer", columnList = "eventId, consumerGroup", unique = true),
    @Index(name = "idx_event_handled_consumer", columnList = "consumerGroup")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventHandled extends BaseEntity {
    
    @Column(nullable = false)
    private String eventId;
    
    @Column(nullable = false)
    private String consumerGroup;
    
    @Column(nullable = false)
    private ZonedDateTime handledAt;
    
    @Builder
    public EventHandled(String eventId, String consumerGroup) {
        this.eventId = eventId;
        this.consumerGroup = consumerGroup;
        this.handledAt = ZonedDateTime.now();
    }
    
    public static EventHandled of(String eventId, String consumerGroup) {
        return EventHandled.builder()
                .eventId(eventId)
                .consumerGroup(consumerGroup)
                .build();
    }
}