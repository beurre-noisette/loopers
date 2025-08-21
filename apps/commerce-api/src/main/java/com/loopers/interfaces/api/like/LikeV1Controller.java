package com.loopers.interfaces.api.like;

import com.loopers.application.like.LikeFacade;
import com.loopers.domain.like.LikeCommand;
import com.loopers.domain.like.TargetType;
import com.loopers.interfaces.api.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/like")
public class LikeV1Controller implements LikeV1ApiSpec {

    private final LikeFacade likeFacade;

    @Autowired
    public LikeV1Controller(LikeFacade likeFacade) {
        this.likeFacade = likeFacade;
    }

    @PostMapping("/products/{productId}")
    public ApiResponse<Object> createProductLike(
            @PathVariable Long productId,
            @RequestHeader("X-USER-ID") String accountId
    ) {
        LikeCommand.Create command = new LikeCommand.Create(accountId, TargetType.PRODUCT, productId);

        likeFacade.createLike(command);

        return ApiResponse.success();
    }

    @DeleteMapping("/products/{productId}")
    public ApiResponse<Object> cancelProductLike(
            @PathVariable Long productId,
            @RequestHeader("X-USER-ID") String accountId
    ) {
        LikeCommand.Create command = new LikeCommand.Create(accountId, TargetType.PRODUCT, productId);

        likeFacade.cancelLike(command);
        
        return ApiResponse.success();
    }
}
