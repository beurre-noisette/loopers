package com.loopers.domain.like;

public class LikeCommand {

    public record Create(
            String userId,
            TargetType targetType,
            Long targetId
    ) {}
}
