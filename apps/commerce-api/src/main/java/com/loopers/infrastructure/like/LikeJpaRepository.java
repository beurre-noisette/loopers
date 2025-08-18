package com.loopers.infrastructure.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.TargetType;
import com.loopers.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LikeJpaRepository extends JpaRepository<Like, Long> {

    boolean existsByUserAndTargetTypeAndTargetId(User user, TargetType targetType, Long targetId);

    int deleteByUserAndTargetTypeAndTargetId(User user, TargetType targetType, Long targetId);

    List<Like> findByUser(User user);

    long countByTargetTypeAndTargetId(TargetType targetType, Long targetId);
}
