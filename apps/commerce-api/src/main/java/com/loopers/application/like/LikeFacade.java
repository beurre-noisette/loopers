package com.loopers.application.like;

import com.loopers.application.product.ProductQuery;
import com.loopers.domain.like.*;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class LikeFacade {

    private final UserService userService;
    private final LikeService likeService;
    private final ProductService productService;
    private final ProductQuery productQuery;

    @Autowired
    public LikeFacade(UserService userService, 
                     LikeService likeService, 
                     ProductService productService,
                     ProductQuery productQuery) {
        this.userService = userService;
        this.likeService = likeService;
        this.productService = productService;
        this.productQuery = productQuery;
    }

    @Transactional
    public void createLike(LikeCommand.Create command) {
        User user = userService.findByAccountId(command.accountId());

        Target target = createTarget(command.targetType(), command.targetId());

        boolean isCreated = likeService.createLike(user, target);
        
        if (isCreated) {
            increaseLikeCountByTargetType(command);

            evictCacheByTargetType(command);
        }
    }

    @Transactional
    public void cancelLike(LikeCommand.Create command) {
        User user = userService.findByAccountId(command.accountId());

        Target target = createTarget(command.targetType(), command.targetId());

        boolean isCancelled = likeService.cancelLike(user, target);
        
        if (isCancelled) {
            decreaseLikeCountByTargetType(command);
            
            evictCacheByTargetType(command);
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
    
    private void evictCacheByTargetType(LikeCommand.Create command) {
        switch (command.targetType()) {
            case PRODUCT -> {
                productQuery.evictProductDetailCache(command.targetId());
            }
        }
    }
}
