package com.loopers.domain.event;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Entity
@Table(name = "event_log", indexes = {
    @Index(name = "idx_event_log_event_id", columnList = "eventId", unique = true),
    @Index(name = "idx_event_log_aggregate", columnList = "aggregateId"),
    @Index(name = "idx_event_log_occurred", columnList = "occurredAt")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventLog extends BaseEntity {
    
    @Column(nullable = false, unique = true)
    private String eventId;
    
    @Column(nullable = false)
    private String eventType;
    
    @Column(nullable = false)
    private String topic;
    
    @Column(name = "partition_num")
    private Integer partition;
    
    @Column(name = "kafka_offset")
    private Long offset;
    
    @Column(columnDefinition = "TEXT")
    private String payload;
    
    @Column(nullable = false)
    private Long aggregateId;
    
    @Column(nullable = false)
    private ZonedDateTime occurredAt;
    
    @Column(nullable = false)
    private ZonedDateTime processedAt;
    
    @Builder
    public EventLog(String eventId,
                    String eventType,
                    String topic,
                    Integer partition,
                    Long offset,
                    String payload,
                    Long aggregateId,
                    ZonedDateTime occurredAt
    ) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.topic = topic;
        this.partition = partition;
        this.offset = offset;
        this.payload = payload;
        this.aggregateId = aggregateId;
        this.occurredAt = occurredAt;
        this.processedAt = ZonedDateTime.now();
    }
}
