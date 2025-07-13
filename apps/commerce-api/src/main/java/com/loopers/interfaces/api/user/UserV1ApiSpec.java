package com.loopers.interfaces.api.user;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "User V1 API", description = "회원가입")
public interface UserV1ApiSpec {

    @Operation(
            summary = "회원 가입",
            description = "회원 가입을 진행합니다."
    )
    ApiResponse<UserV1Dto.UserResponse> register(
            @Schema(name = "회원가입 요청", description = "회원가입 할 사용자 정보")
            UserV1Dto.UserRegisterRequest request
    );
}
