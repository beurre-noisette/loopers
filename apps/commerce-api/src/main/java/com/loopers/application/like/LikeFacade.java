package com.loopers.application.like;

import com.loopers.application.like.event.LikeCancelledEvent;
import com.loopers.application.like.event.LikeCreatedEvent;
import com.loopers.domain.like.*;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class LikeFacade {

    private final UserService userService;
    private final LikeService likeService;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public LikeFacade(UserService userService, 
                     LikeService likeService,
                     ApplicationEventPublisher eventPublisher) {
        this.userService = userService;
        this.likeService = likeService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void createLike(LikeCommand.Create command) {
        User user = userService.findByAccountId(command.accountId());

        Target target = createTarget(command.targetType(), command.targetId());

        boolean isCreated = likeService.createLike(user, target);
        
        if (isCreated) {
            LikeCreatedEvent event = LikeCreatedEvent.of(
                    user.getId(),
                    command.targetType(),
                    command.targetId()
            );
            eventPublisher.publishEvent(event);
            
            log.info("좋아요 생성 이벤트 발행 - userId: {}, targetType: {}, targetId: {}, correlationId: {}",
                    user.getId(), command.targetType(), command.targetId(), event.getCorrelationId());
        }
    }

    @Transactional
    public void cancelLike(LikeCommand.Create command) {
        User user = userService.findByAccountId(command.accountId());

        Target target = createTarget(command.targetType(), command.targetId());

        boolean isCancelled = likeService.cancelLike(user, target);
        
        if (isCancelled) {
            LikeCancelledEvent event = LikeCancelledEvent.of(
                    user.getId(),
                    command.targetType(),
                    command.targetId()
            );
            eventPublisher.publishEvent(event);
            
            log.info("좋아요 취소 이벤트 발행 - userId: {}, targetType: {}, targetId: {}, correlationId: {}",
                    user.getId(), command.targetType(), command.targetId(), event.getCorrelationId());
        }
    }

    private Target createTarget(TargetType targetType, Long targetId) {
        return switch (targetType) {
            case PRODUCT -> ProductTarget.of(targetId);
        };
    }
}
