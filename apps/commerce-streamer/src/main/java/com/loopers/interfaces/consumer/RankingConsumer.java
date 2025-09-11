package com.loopers.interfaces.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.event.IdempotencyService;
import com.loopers.domain.ranking.RankingScorePolicy;
import com.loopers.domain.ranking.RankingService;
import com.loopers.infrastructure.kafka.event.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class RankingConsumer {
    
    private static final String CONSUMER_GROUP = "ranking-consumer";
    
    private final RankingService rankingService;
    private final RankingScorePolicy rankingScorePolicy;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;
    
    public RankingConsumer(
            RankingService rankingService,
            RankingScorePolicy rankingScorePolicy,
            IdempotencyService idempotencyService,
            ObjectMapper objectMapper
    ) {
        this.rankingService = rankingService;
        this.rankingScorePolicy = rankingScorePolicy;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }
    
    @KafkaListener(
        topics = {"catalog-events", "order-events"},
        groupId = CONSUMER_GROUP,
        containerFactory = "BATCH_LISTENER_DEFAULT"
    )
    @Transactional
    public void consume(
            List<ConsumerRecord<String, String>> records,
            Acknowledgment ack
    ) {
        log.info("Ranking 컨슈머 - {} 개의 메시지 수신", records.size());
        
        Map<Long, Double> scoreDeltas = new HashMap<>();
        LocalDate today = LocalDate.now();
        
        try {
            for (ConsumerRecord<String, String> record : records) {
                processRecord(record, scoreDeltas);
            }
            
            scoreDeltas.forEach((productId, delta) -> {
                rankingService.incrementScore(productId, delta, today);
            });
            
            ack.acknowledge();
            log.info("Ranking 컨슈머 - {} 개의 메시지 처리 완료, {} 개의 상품 점수 업데이트", 
                    records.size(), scoreDeltas.size());
            
        } catch (Exception e) {
            log.error("Ranking 처리 중 오류 발생", e);
            throw e;
        }
    }
    
    private void processRecord(ConsumerRecord<String, String> record, Map<Long, Double> scoreDeltas) {
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
                case ProductViewedKafkaEvent viewEvent -> 
                    accumulateScore(scoreDeltas, viewEvent.getProductId(), rankingScorePolicy.calculateViewScore());
                case LikeChangedKafkaEvent likeEvent ->
                    accumulateScore(scoreDeltas, likeEvent.getProductId(), 
                            rankingScorePolicy.calculateLikeScore(likeEvent.getDeltaCount()));
                case OrderCompletedKafkaEvent orderEvent ->
                    processOrderEvent(orderEvent, scoreDeltas);
                case StockAdjustedKafkaEvent stockEvent ->
                    log.debug("재고 조정 이벤트는 랭킹에 반영하지 않음 - productId: {}", stockEvent.getProductId());
                default ->
                    log.debug("처리하지 않는 이벤트 타입 - type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("랭킹 점수 계산 실패 - eventId: {}, type: {}", 
                    eventId, event.getEventType(), e);
            throw new RuntimeException("랭킹 점수 계산 실패", e);
        }
    }
    
    private void processOrderEvent(
            OrderCompletedKafkaEvent event,
            Map<Long, Double> scoreDeltas
    ) {
        event.getItems().forEach(item -> {
            Long productId = item.getProductId();
            int quantity = item.getQuantity();
            Long amount = item.getPrice() * quantity;
            
            double score = rankingScorePolicy.calculateOrderScore(quantity, amount);
            accumulateScore(scoreDeltas, productId, score);
            
            log.debug("주문 점수 계산 - productId: {}, quantity: {}, amount: {}, score: {}", 
                    productId, quantity, amount, score);
        });
    }
    
    private void accumulateScore(
            Map<Long, Double> scoreDeltas,
            Long productId,
            double score
    ) {
        scoreDeltas.merge(productId, score, Double::sum);
    }
}
