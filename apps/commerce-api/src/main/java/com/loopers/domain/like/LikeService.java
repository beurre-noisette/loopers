package com.loopers.domain.like;

import com.loopers.domain.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
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
    public boolean createLike(User user, Target target) {
        try {
            if (likeRepository.existsByUserAndTarget(user, target.getType(), target.getId())) {
                return false;
            }
            
            Like like = Like.of(user, target);
            likeRepository.save(like);

            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    @Transactional
    public boolean cancelLike(User user, Target target) {
        return likeRepository.deleteByUserAndTarget(user, target.getType(), target.getId()) > 0;
    }
}
