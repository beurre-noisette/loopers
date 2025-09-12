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
        int successCount = 0;
        int failureCount = 0;
        
        // 개별 메시지 처리 - 실패해도 계속 진행
        for (ConsumerRecord<String, String> record : records) {
            try {
                processRecord(record, scoreDeltas);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                log.error("개별 메시지 처리 실패, 스킵 - topic: {}, partition: {}, offset: {}, error: {}",
                        record.topic(), record.partition(), record.offset(), e.getMessage());
                // 상세 에러는 debug 레벨로
                log.debug("메시지 처리 실패 상세", e);
            }
        }
        
        // 성공적으로 처리된 점수만 Redis에 반영
        if (!scoreDeltas.isEmpty()) {
            try {
                scoreDeltas.forEach((productId, delta) -> {
                    rankingService.incrementScore(productId, delta, today);
                });
                log.info("Redis 점수 업데이트 완료 - {} 개 상품", scoreDeltas.size());
            } catch (Exception e) {
                log.error("Redis 업데이트 실패 - 점수 업데이트를 재시도해야 합니다", e);
                // Redis 실패는 전체 배치에 영향을 주므로 예외를 던짐
                throw new RuntimeException("Redis 점수 업데이트 실패", e);
            }
        }
        
        // 처리 완료 후 커밋
        ack.acknowledge();
        
        // 처리 결과 로깅
        log.info("Ranking 컨슈머 처리 완료 - 전체: {}, 성공: {}, 실패: {}, 점수 업데이트: {} 개 상품",
                records.size(), successCount, failureCount, scoreDeltas.size());
        
        // 실패율이 높으면 경고
        if (failureCount > 0) {
            double failureRate = (double) failureCount / records.size() * 100;
            if (failureRate > 10) {
                log.warn("높은 실패율 감지 - 실패율: {:.2f}% ({}/{})", 
                        failureRate, failureCount, records.size());
            }
        }
    }
    
    private void processRecord(ConsumerRecord<String, String> record, Map<Long, Double> scoreDeltas) {
        BaseKafkaEvent event;
        try {
            event = objectMapper.readValue(record.value(), BaseKafkaEvent.class);
        } catch (Exception e) {
            // JSON 파싱 실패는 복구 불가능하므로 예외를 던짐
            throw new IllegalArgumentException(
                    String.format("JSON 파싱 실패 - topic: %s, offset: %d", 
                            record.topic(), record.offset()), e);
        }
        
        String eventId = event.getEventId();
        
        // 멱등성 체크
        if (!idempotencyService.tryMarkAsProcessed(eventId, CONSUMER_GROUP)) {
            log.debug("이미 처리된 이벤트 스킵 - eventId: {}", eventId);
            return;
        }
        
        // 이벤트 타입별 처리
        switch (event) {
            case ProductViewedKafkaEvent viewEvent -> {
                validateAndAccumulate(scoreDeltas, viewEvent.getProductId(), 
                        rankingScorePolicy.calculateViewScore(), "VIEW");
            }
            case LikeChangedKafkaEvent likeEvent -> {
                validateAndAccumulate(scoreDeltas, likeEvent.getProductId(),
                        rankingScorePolicy.calculateLikeScore(likeEvent.getDeltaCount()), "LIKE");
            }
            case OrderCompletedKafkaEvent orderEvent -> {
                processOrderEvent(orderEvent, scoreDeltas);
            }
            case StockAdjustedKafkaEvent stockEvent -> {
                log.debug("재고 조정 이벤트는 랭킹에 반영하지 않음 - productId: {}", stockEvent.getProductId());
            }
            default -> {
                log.debug("처리하지 않는 이벤트 타입 - type: {}", event.getEventType());
            }
        }
    }
    
    private void validateAndAccumulate(Map<Long, Double> scoreDeltas, Long productId, 
                                      double score, String eventType) {
        if (productId == null) {
            throw new IllegalArgumentException(eventType + " 이벤트의 productId가 null입니다");
        }
        if (Double.isNaN(score) || Double.isInfinite(score)) {
            throw new IllegalArgumentException(
                    String.format("%s 이벤트의 점수가 유효하지 않습니다: %f", eventType, score));
        }
        accumulateScore(scoreDeltas, productId, score);
    }
    
    private void processOrderEvent(
            OrderCompletedKafkaEvent event,
            Map<Long, Double> scoreDeltas
    ) {
        if (event.getItems() == null || event.getItems().isEmpty()) {
            log.warn("주문 이벤트에 상품 정보가 없습니다 - orderId: {}", event.getAggregateId());
            return;
        }
        
        event.getItems().forEach(item -> {
            try {
                Long productId = item.getProductId();
                if (productId == null) {
                    log.warn("주문 항목의 productId가 null입니다 - orderId: {}", event.getAggregateId());
                    return;
                }
                
                int quantity = item.getQuantity();
                Long amount = item.getPrice() != null ? item.getPrice() * quantity : null;
                
                double score = rankingScorePolicy.calculateOrderScore(quantity, amount);
                validateAndAccumulate(scoreDeltas, productId, score, "ORDER");
                
                log.debug("주문 점수 계산 - productId: {}, quantity: {}, amount: {}, score: {}", 
                        productId, quantity, amount, score);
            } catch (Exception e) {
                log.error("주문 항목 처리 실패 - orderId: {}, productId: {}", 
                        event.getAggregateId(), item.getProductId(), e);
                // 개별 주문 항목 실패는 전체를 실패시키지 않음
            }
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
