package com.loopers.application.like.event;

import com.loopers.application.product.ProductQuery;
import com.loopers.domain.like.TargetType;
import com.loopers.domain.product.ProductService;
import com.loopers.infrastructure.kafka.KafkaEventPublisher;
import com.loopers.infrastructure.kafka.event.LikeChangedKafkaEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.ZonedDateTime;
import java.util.UUID;

@Slf4j
@Component
public class LikeAggregateListener {

    private final ProductService productService;
    private final ProductQuery productQuery;
    private final KafkaEventPublisher kafkaEventPublisher;

    @Autowired
    public LikeAggregateListener(ProductService productService, 
                                ProductQuery productQuery,
                                KafkaEventPublisher kafkaEventPublisher) {
        this.productService = productService;
        this.productQuery = productQuery;
        this.kafkaEventPublisher = kafkaEventPublisher;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void handleLikeCreated(LikeCreatedEvent event) {
        log.info("좋아요 생성 이벤트 수신 - userId: {}, targetType: {}, targetId: {}, correlationId: {}",
                event.getUserId(), event.getTargetType(), event.getTargetId(), event.getCorrelationId());

        try {
            increaseLikeCountByTargetType(event.getTargetType(), event.getTargetId());
            evictCacheByTargetType(event.getTargetType(), event.getTargetId());
            
            if (event.getTargetType() == TargetType.PRODUCT) {
                publishLikeChangedToKafka(event, "CREATED", 1);
            }
            
            log.info("좋아요 집계 업데이트 완료 - targetType: {}, targetId: {}, correlationId: {}",
                    event.getTargetType(), event.getTargetId(), event.getCorrelationId());
                    
        } catch (Exception e) {
            log.error("좋아요 집계 업데이트 실패 - targetType: {}, targetId: {}, correlationId: {}",
                    event.getTargetType(), event.getTargetId(), event.getCorrelationId(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void handleLikeCancelled(LikeCancelledEvent event) {
        log.info("좋아요 취소 이벤트 수신 - userId: {}, targetType: {}, targetId: {}, correlationId: {}",
                event.getUserId(), event.getTargetType(), event.getTargetId(), event.getCorrelationId());

        try {
            decreaseLikeCountByTargetType(event.getTargetType(), event.getTargetId());
            evictCacheByTargetType(event.getTargetType(), event.getTargetId());
            
            if (event.getTargetType() == TargetType.PRODUCT) {
                publishLikeChangedToKafka(event, "CANCELLED", -1);
            }
            
            log.info("좋아요 취소 집계 업데이트 완료 - targetType: {}, targetId: {}, correlationId: {}",
                    event.getTargetType(), event.getTargetId(), event.getCorrelationId());
                    
        } catch (Exception e) {
            log.error("좋아요 취소 집계 업데이트 실패 - targetType: {}, targetId: {}, correlationId: {}",
                    event.getTargetType(), event.getTargetId(), event.getCorrelationId(), e);
        }
    }

    private void increaseLikeCountByTargetType(TargetType targetType, Long targetId) {
        switch (targetType) {
            case PRODUCT -> productService.increaseLikeCount(targetId);
        }
    }

    private void decreaseLikeCountByTargetType(TargetType targetType, Long targetId) {
        switch (targetType) {
            case PRODUCT -> productService.decreaseLikeCount(targetId);
        }
    }

    private void evictCacheByTargetType(TargetType targetType, Long targetId) {
        switch (targetType) {
            case PRODUCT -> productQuery.evictProductDetailCache(targetId);
        }
    }

    private void publishLikeChangedToKafka(Object event, String action, Integer deltaCount) {
        try {
            String correlationId;
            Long userId;
            Long targetId;
            
            if (event instanceof LikeCreatedEvent createdEvent) {
                correlationId = createdEvent.getCorrelationId();
                userId = createdEvent.getUserId();
                targetId = createdEvent.getTargetId();
            } else if (event instanceof LikeCancelledEvent cancelledEvent) {
                correlationId = cancelledEvent.getCorrelationId();
                userId = cancelledEvent.getUserId();
                targetId = cancelledEvent.getTargetId();
            } else {
                return;
            }
            
            LikeChangedKafkaEvent kafkaEvent = LikeChangedKafkaEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .aggregateId(targetId)
                    .occurredAt(ZonedDateTime.now())
                    .productId(targetId)
                    .userId(userId)
                    .action(action)
                    .deltaCount(deltaCount)
                    .build();
                    
            kafkaEventPublisher.publishLikeChangedEvent(kafkaEvent);
            
            log.info("Kafka 이벤트 발행 요청 - eventId: {}, productId: {}, action: {}, deltaCount: {}",
                    kafkaEvent.getEventId(), targetId, action, deltaCount);
                    
        } catch (Exception e) {
            log.error("Kafka 이벤트 발행 실패 - action: {}, targetId: {}",
                    action, event, e);
        }
    }
}
