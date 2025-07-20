package com.loopers.interfaces.api.user;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "User V1 API", description = "회원가입, 내 정보 조회")
public interface UserV1ApiSpec {

    @Operation(
            summary = "회원 가입",
            description = "회원 가입을 진행합니다."
    )
    ApiResponse<UserV1Dto.UserResponse> register(
            @Schema(name = "회원가입 요청", description = "회원가입 할 사용자 정보")
            UserV1Dto.UserRegisterRequest request
    );

    @Operation(
            summary = "내 정보 조회",
            description = "현재 인증된 사용자의 정보를 조회합니다."
    )
    ApiResponse<UserV1Dto.UserResponse> getUser(
            @Schema(name = "사용자 ID", description = "X-USER-ID 헤더로 전달되는 현재 사용자 식별자")
            String userId
    );
}
