package com.loopers.domain.like;

import com.loopers.domain.user.User;
import org.springframework.beans.factory.annotation.Autowired;
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
        if (likeRepository.existsByUserAndTarget(user, target.getType(), target.getId())) {
            return;
        }

        Like like = Like.of(user, target);

        likeRepository.save(like);
    }

    @Transactional
    public void cancelLike(User user, Target target) {
        if (likeRepository.existsByUserAndTarget(user, target.getType(), target.getId())) {
            likeRepository.deleteByUserAndTarget(user, target.getType(), target.getId());
        }
    }
}
