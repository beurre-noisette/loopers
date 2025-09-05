package com.loopers.interfaces.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.event.IdempotencyService;
import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.infrastructure.kafka.event.*;
import com.loopers.infrastructure.metrics.ProductMetricsRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
public class MetricsConsumer {
    
    private static final String CONSUMER_GROUP = "metrics-consumer";
    
    private final ProductMetricsRepository productMetricsRepository;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;
    
    public MetricsConsumer(ProductMetricsRepository productMetricsRepository, 
                          IdempotencyService idempotencyService,
                          ObjectMapper objectMapper) {
        this.productMetricsRepository = productMetricsRepository;
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
        log.info("Metrics 컨슈머 - {} 개의 메시지 수신", records.size());
        
        try {
            for (ConsumerRecord<String, String> record : records) {
                processRecord(record);
            }
            
            ack.acknowledge();
            log.info("Metrics 컨슈머 - {} 개의 메시지 처리 완료", records.size());
            
        } catch (Exception e) {
            log.error("Metrics 처리 중 오류 발생", e);
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
            switch (event) {
                case LikeChangedKafkaEvent likeEvent -> processLikeEvent(likeEvent);
                case OrderCompletedKafkaEvent orderEvent -> processOrderEvent(orderEvent);
                case StockAdjustedKafkaEvent stockEvent -> processStockEvent(stockEvent);
                case ProductViewedKafkaEvent viewEvent -> processViewEvent(viewEvent);
                default -> log.debug("처리하지 않는 이벤트 타입 - type: {}", event.getEventType());
            }
            
        } catch (Exception e) {
            log.error("메트릭 업데이트 실패 - eventId: {}, type: {}", 
                    eventId, event.getEventType(), e);
            throw new RuntimeException("메트릭 업데이트 실패", e);
        }
    }
    
    private void processLikeEvent(LikeChangedKafkaEvent event) {
        Long productId = event.getProductId();
        LocalDate today = LocalDate.now();
        
        ProductMetrics metrics = getOrCreateMetrics(productId, today);
        
        int delta = event.getDeltaCount();
        metrics.incrementLikeCount(delta);
        
        productMetricsRepository.save(metrics);
        log.debug("좋아요 메트릭 업데이트 - productId: {}, delta: {}, total: {}", 
                productId, delta, metrics.getLikeCount());
    }
    
    private void processOrderEvent(OrderCompletedKafkaEvent event) {
        LocalDate today = LocalDate.now();
        
        event.getItems().forEach(item -> {
            Long productId = item.getProductId();
            ProductMetrics metrics = getOrCreateMetrics(productId, today);
            
            Long amount = item.getPrice() * item.getQuantity();
            metrics.incrementSalesCount(item.getQuantity(), amount);
            
            productMetricsRepository.save(metrics);
            log.debug("판매 메트릭 업데이트 - productId: {}, quantity: {}, amount: {}, total: {}", 
                    productId, item.getQuantity(), amount, metrics.getSalesCount());
        });
    }
    
    private void processStockEvent(StockAdjustedKafkaEvent event) {
        log.debug("재고 조정 이벤트 - productId: {}, adjusted: {}, current: {}", 
                event.getProductId(), event.getAdjustedQuantity(), event.getCurrentStock());
    }
    
    private void processViewEvent(ProductViewedKafkaEvent event) {
        Long productId = event.getProductId();
        LocalDate today = LocalDate.now();
        
        ProductMetrics metrics = getOrCreateMetrics(productId, today);
        metrics.incrementViewCount();
        
        productMetricsRepository.save(metrics);
        log.debug("조회수 메트릭 업데이트 - productId: {}, total: {}", 
                productId, metrics.getViewCount());
    }
    
    private ProductMetrics getOrCreateMetrics(Long productId, LocalDate date) {
        return productMetricsRepository
                .findByProductIdAndMetricDateWithLock(productId, date)
                .orElseGet(() -> {
                    ProductMetrics newMetrics = ProductMetrics.builder()
                            .productId(productId)
                            .metricDate(date)
                            .build();
                    return productMetricsRepository.save(newMetrics);
                });
    }
}
