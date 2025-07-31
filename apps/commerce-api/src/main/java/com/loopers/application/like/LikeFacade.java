package com.loopers.application.like;

import com.loopers.domain.like.*;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LikeFacade {

    private final UserService userService;
    private final LikeService likeService;

    @Autowired
    public LikeFacade(UserService userService, LikeService likeService) {
        this.userService = userService;
        this.likeService = likeService;
    }

    public void createLike(LikeCommand.Create command) {
        User user = userService.findByUserId(command.userId());

        Target target = createTarget(command.targetType(), command.targetId());

        likeService.createLike(user, target);
    }

    public void cancelLike(LikeCommand.Create command) {
        User user = userService.findByUserId(command.userId());

        Target target = createTarget(command.targetType(), command.targetId());

        likeService.cancelLike(user, target);
    }

    private Target createTarget(TargetType targetType, Long targetId) {
        return switch (targetType) {
            case PRODUCT -> ProductTarget.of(targetId);
        };
    }
}
