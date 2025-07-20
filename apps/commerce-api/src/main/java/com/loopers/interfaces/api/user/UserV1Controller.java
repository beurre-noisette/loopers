package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.interfaces.api.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserV1Controller implements UserV1ApiSpec {

    private final UserFacade userFacade;

    @Autowired
    public UserV1Controller(UserFacade userFacade) {
        this.userFacade = userFacade;
    }

    @PostMapping("")
    public ApiResponse<UserV1Dto.UserResponse> signUp(@RequestBody UserV1Dto.UserRegisterRequest request) {
        UserInfo userInfo = userFacade.signUp(request.userId(), request.email(), request.birthDate(), request.gender());

        return ApiResponse.success(UserV1Dto.UserResponse.from(userInfo));
    }

    @GetMapping("/me")
    public ApiResponse<UserV1Dto.UserResponse> getMyInfo(@RequestHeader("X-USER-ID") String userId) {
        UserInfo userInfo = userFacade.getMyInfo(userId);

        return ApiResponse.success(UserV1Dto.UserResponse.from(userInfo));
    }
}
