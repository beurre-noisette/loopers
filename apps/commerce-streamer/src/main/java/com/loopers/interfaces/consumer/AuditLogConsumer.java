package com.loopers.interfaces.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.event.EventLog;
import com.loopers.domain.event.IdempotencyService;
import com.loopers.infrastructure.event.EventLogRepository;
import com.loopers.infrastructure.kafka.event.BaseKafkaEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
public class AuditLogConsumer {
    
    private static final String CONSUMER_GROUP = "audit-log-consumer";
    
    private final EventLogRepository eventLogRepository;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;
    
    public AuditLogConsumer(EventLogRepository eventLogRepository, 
                           IdempotencyService idempotencyService,
                           ObjectMapper objectMapper) {
        this.eventLogRepository = eventLogRepository;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }
    
    @KafkaListener(
        topics = {"catalog-events", "order-events"},
        groupId = CONSUMER_GROUP,
        containerFactory = "BATCH_LISTENER_DEFAULT"
    )
    @Transactional
    public void consume(List<ConsumerRecord<String, String>> records, Acknowledgment ack) {
        log.info("Audit Log 컨슈머 - {} 개의 메시지 수신", records.size());
        
        try {
            for (ConsumerRecord<String, String> record : records) {
                processRecord(record);
            }
            
            ack.acknowledge();
            log.info("Audit Log 컨슈머 - {} 개의 메시지 처리 완료", records.size());
            
        } catch (Exception e) {
            log.error("Audit Log 처리 중 오류 발생", e);
            throw e;
        }
    }
    
    private void processRecord(ConsumerRecord<String, String> record) {
        BaseKafkaEvent event;
        try {
            event = objectMapper.readValue(record.value(), BaseKafkaEvent.class);
        } catch (Exception e) {
            log.error("JSON 파싱 오류 - topic: {}, partition: {}, offset: {}, value: {}", 
                    record.topic(), record.partition(), record.offset(), record.value(), e);
            throw new RuntimeException("이벤트 파싱 실패", e);
        }
        
        String eventId = event.getEventId();
        
        if (!idempotencyService.tryMarkAsProcessed(eventId, CONSUMER_GROUP)) {
            log.debug("이미 처리된 이벤트 스킵 - eventId: {}", eventId);
            return;
        }
        
        try {
            String payload = objectMapper.writeValueAsString(event);
            
            EventLog eventLog = EventLog.builder()
                    .eventId(eventId)
                    .eventType(event.getEventType())
                    .topic(record.topic())
                    .partition(record.partition())
                    .offset(record.offset())
                    .payload(payload)
                    .aggregateId(event.getAggregateId())
                    .occurredAt(event.getOccurredAt())
                    .build();
            
            eventLogRepository.save(eventLog);
            
            log.debug("이벤트 로그 저장 완료 - eventId: {}, type: {}, aggregateId: {}", 
                    eventId, event.getEventType(), event.getAggregateId());
            
        } catch (Exception e) {
            log.error("이벤트 로그 저장 실패 - eventId: {}", eventId, e);
            throw new RuntimeException("이벤트 로그 저장 실패", e);
        }
    }
}
