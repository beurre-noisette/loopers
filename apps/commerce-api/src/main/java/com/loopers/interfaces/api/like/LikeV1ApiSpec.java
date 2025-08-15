package com.loopers.interfaces.api.like;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Like API", description = "좋아요 관련 API")
public interface LikeV1ApiSpec {

    @Operation(summary = "상품 좋아요 등록", description = "특정 상품에 좋아요를 등록합니다.")
    ApiResponse<Object> createProductLike(
        @Parameter(description = "상품 ID", required = true) @PathVariable Long productId,
        @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-USER-ID") String userId
    );

    @Operation(summary = "상품 좋아요 취소", description = "특정 상품의 좋아요를 취소합니다.")
    ApiResponse<Object> cancelProductLike(
        @Parameter(description = "상품 ID", required = true) @PathVariable Long productId,
        @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-USER-ID") String userId
    );
}
