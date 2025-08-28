package com.loopers.application.like.event;

import com.loopers.application.product.ProductQuery;
import com.loopers.domain.like.TargetType;
import com.loopers.domain.product.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class LikeAggregateListener {

    private final ProductService productService;
    private final ProductQuery productQuery;

    @Autowired
    public LikeAggregateListener(ProductService productService, ProductQuery productQuery) {
        this.productService = productService;
        this.productQuery = productQuery;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleLikeCreated(LikeCreatedEvent event) {
        log.info("좋아요 생성 이벤트 수신 - userId: {}, targetType: {}, targetId: {}, correlationId: {}",
                event.getUserId(), event.getTargetType(), event.getTargetId(), event.getCorrelationId());

        try {
            increaseLikeCountByTargetType(event.getTargetType(), event.getTargetId());
            evictCacheByTargetType(event.getTargetType(), event.getTargetId());
            
            log.info("좋아요 집계 업데이트 완료 - targetType: {}, targetId: {}, correlationId: {}",
                    event.getTargetType(), event.getTargetId(), event.getCorrelationId());
                    
        } catch (Exception e) {
            log.error("좋아요 집계 업데이트 실패 - targetType: {}, targetId: {}, correlationId: {}",
                    event.getTargetType(), event.getTargetId(), event.getCorrelationId(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleLikeCancelled(LikeCancelledEvent event) {
        log.info("좋아요 취소 이벤트 수신 - userId: {}, targetType: {}, targetId: {}, correlationId: {}",
                event.getUserId(), event.getTargetType(), event.getTargetId(), event.getCorrelationId());

        try {
            decreaseLikeCountByTargetType(event.getTargetType(), event.getTargetId());
            evictCacheByTargetType(event.getTargetType(), event.getTargetId());
            
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
}
