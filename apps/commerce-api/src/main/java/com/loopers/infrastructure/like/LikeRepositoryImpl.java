package com.loopers.infrastructure.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.like.TargetType;
import com.loopers.domain.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class LikeRepositoryImpl implements LikeRepository {

    private final LikeJpaRepository likeJpaRepository;

    @Autowired
    public LikeRepositoryImpl(LikeJpaRepository likeJpaRepository) {
        this.likeJpaRepository = likeJpaRepository;
    }

    @Override
    public Like save(Like like) {
        return likeJpaRepository.save(like);
    }

    @Override
    public boolean existsByUserAndTarget(User user, TargetType targetType, Long targetId) {
        return likeJpaRepository.existsByUserAndTargetTypeAndTargetId(user, targetType, targetId);
    }

    @Override
    public int deleteByUserAndTarget(User user, TargetType targetType, Long targetId) {
        return likeJpaRepository.deleteByUserAndTargetTypeAndTargetId(user, targetType, targetId);
    }

    @Override
    public List<Like> findByUser(User user) {
        return likeJpaRepository.findByUser(user);
    }

    @Override
    public long countByTarget(TargetType targetType, Long targetId) {
        return likeJpaRepository.countByTargetTypeAndTargetId(targetType, targetId);
    }
}
