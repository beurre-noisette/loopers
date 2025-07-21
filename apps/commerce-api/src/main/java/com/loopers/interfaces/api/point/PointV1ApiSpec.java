package com.loopers.interfaces.api.point;

import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.user.UserV1Dto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Point V1 API", description = "포인트 조회, 충전")
public interface PointV1ApiSpec {

    @Operation(
            summary = "내 포인트 조회",
            description = "현재 인증된 사용자의 보유 포인트를 조회합니다."
    )
    ApiResponse<PointV1Dto.PointResponse> getMyPoint(
            @Schema(name = "사용자 ID", description = "X-USER-ID 헤더로 전달되는 현재 사용자 식별자")
            String userId
    );

    @Operation(
            summary = "포인트 충전",
            description = "유저의 포인트를 충전합니다."
    )
    ApiResponse<PointV1Dto.PointResponse> chargePoints(
            @Schema(name = "사용자 ID", description = "X-USER-ID 헤더로 전달되는 현재 사용자 식별자")
            String userId,
            @Schema(name = "충전할 포인트", description = "충전할 포인트 단, 0이하의 값은 불허합니다.")
            UserV1Dto.UserPointChargeRequest request
    );
}
