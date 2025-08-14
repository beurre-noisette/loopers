package com.loopers.application.like;

import com.loopers.domain.like.*;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LikeFacade {

    private final UserService userService;
    private final LikeService likeService;
    private final ProductService productService;

    @Autowired
    public LikeFacade(UserService userService, LikeService likeService, ProductService productService) {
        this.userService = userService;
        this.likeService = likeService;
        this.productService = productService;
    }

    @Transactional
    public void createLike(LikeCommand.Create command) {
        User user = userService.findByUserId(command.userId());

        Target target = createTarget(command.targetType(), command.targetId());

        boolean isCreated = likeService.createLike(user, target);
        
        if (isCreated) {
            increaseLikeCountByTargetType(command);
        }
    }

    @Transactional
    public void cancelLike(LikeCommand.Create command) {
        User user = userService.findByUserId(command.userId());

        Target target = createTarget(command.targetType(), command.targetId());

        boolean isCancelled = likeService.cancelLike(user, target);
        
        if (isCancelled) {
            decreaseLikeCountByTargetType(command);
        }
    }

    private Target createTarget(TargetType targetType, Long targetId) {
        return switch (targetType) {
            case PRODUCT -> ProductTarget.of(targetId);
        };
    }

    private void increaseLikeCountByTargetType(LikeCommand.Create command) {
        switch (command.targetType()) {
            case PRODUCT -> productService.increaseLikeCount(command.targetId());
        }
    }

    private void decreaseLikeCountByTargetType(LikeCommand.Create command) {
        switch (command.targetType()) {
            case PRODUCT -> productService.decreaseLikeCount(command.targetId());
        }
    }
}
