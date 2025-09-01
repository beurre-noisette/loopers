package com.loopers.application.like.event;

import com.loopers.domain.like.TargetType;
import lombok.Builder;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.util.UUID;

@Getter
@Builder
public class LikeCancelledEvent {
    private final String correlationId;
    private final Long userId;
    private final TargetType targetType;
    private final Long targetId;
    private final ZonedDateTime occurredAt;
    
    public static LikeCancelledEvent of(
            Long userId,
            TargetType targetType,
            Long targetId
    ) {
        return LikeCancelledEvent.builder()
                .correlationId(UUID.randomUUID().toString())
                .userId(userId)
                .targetType(targetType)
                .targetId(targetId)
                .occurredAt(ZonedDateTime.now())
                .build();
    }
}
