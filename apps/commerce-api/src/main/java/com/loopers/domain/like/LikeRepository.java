package com.loopers.domain.like;

import com.loopers.domain.user.User;

import java.util.List;

public interface LikeRepository {

    Like save(Like like);

    boolean existsByUserAndTarget(User user, TargetType targetType, Long targetId);

    int deleteByUserAndTarget(User user, TargetType targetType, Long targetId);

    List<Like> findByUser(User user);

    long countByTarget(TargetType targetType, Long targetId);
}
