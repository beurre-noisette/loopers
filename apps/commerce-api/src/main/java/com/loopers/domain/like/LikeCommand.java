package com.loopers.domain.like;

public class LikeCommand {

    public record Create(
            String accountId,
            TargetType targetType,
            Long targetId
    ) {}
}
