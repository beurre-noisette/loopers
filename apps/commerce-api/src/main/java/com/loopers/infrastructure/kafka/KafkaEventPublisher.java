package com.loopers.infrastructure.kafka;

import com.loopers.infrastructure.kafka.event.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class KafkaEventPublisher {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    private static final String CATALOG_EVENTS_TOPIC = "catalog-events";
    private static final String ORDER_EVENTS_TOPIC = "order-events";

    @Autowired
    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    private void publishEvent(String topic, String partitionKey, BaseKafkaEvent event) {
        CompletableFuture<SendResult<String, Object>> future = 
            kafkaTemplate.send(topic, partitionKey, event);
            
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Kafka 이벤트 발행 성공 - topic: {}, key: {}, eventType: {}, eventId: {}", 
                    topic, partitionKey, event.getEventType(), event.getEventId());
            } else {
                // At Least Once 보장: 실패 시 로깅 (재시도는 Kafka 설정으로 처리)
                log.error("Kafka 이벤트 발행 실패 - topic: {}, key: {}, eventType: {}, eventId: {}", 
                    topic, partitionKey, event.getEventType(), event.getEventId(), ex);
                // TODO: 실패한 이벤트를 별도 테이블에 저장하여 재처리 가능하도록
            }
        });
    }
    
    public void publishOrderCompletedEvent(OrderCompletedKafkaEvent event) {
        publishEvent(ORDER_EVENTS_TOPIC, event.getAggregateId().toString(), event);
    }
    
    public void publishLikeChangedEvent(LikeChangedKafkaEvent event) {
        publishEvent(CATALOG_EVENTS_TOPIC, event.getAggregateId().toString(), event);
    }
    
    public void publishStockAdjustedEvent(StockAdjustedKafkaEvent event) {
        publishEvent(CATALOG_EVENTS_TOPIC, event.getAggregateId().toString(), event);
    }
    
    public void publishProductViewedEvent(ProductViewedKafkaEvent event) {
        publishEvent(CATALOG_EVENTS_TOPIC, event.getAggregateId().toString(), event);
    }
}
