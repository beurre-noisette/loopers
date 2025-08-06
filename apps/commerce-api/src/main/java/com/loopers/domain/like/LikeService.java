package com.loopers.domain.like;

import com.loopers.domain.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LikeService {

    private final LikeRepository likeRepository;

    @Autowired
    public LikeService(LikeRepository likeRepository) {
        this.likeRepository = likeRepository;
    }

    @Transactional
    public void createLike(User user, Target target) {
        try {
            if (likeRepository.existsByUserAndTarget(user, target.getType(), target.getId())) {
                return;
            }
            
            Like like = Like.of(user, target);
            likeRepository.save(like);
        } catch (ObjectOptimisticLockingFailureException e) {
            // 낙관적 락 충돌 - 다른 스레드가 이미 처리했으므로 무시 (멱등성 보장)
        }
    }

    @Transactional
    public void cancelLike(User user, Target target) {
        try {
            likeRepository.deleteByUserAndTarget(user, target.getType(), target.getId());
        } catch (ObjectOptimisticLockingFailureException e) {
            // 낙관적 락 충돌 - 다른 스레드가 이미 처리했으므로 무시 (멱등성 보장)
        }
    }
}
