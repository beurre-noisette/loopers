package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.domain.user.UserCommand;
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
    public ApiResponse<UserV1Dto.UserResponse> signUp(@RequestBody UserV1Dto.UserSignUpRequest request) {
        UserCommand.Create command = request.toCommand();

        UserInfo userInfo = userFacade.signUp(command);

        return ApiResponse.success(UserV1Dto.UserResponse.from(userInfo));
    }

    @GetMapping("/me")
    public ApiResponse<UserV1Dto.UserResponse> getMyInfo(@RequestHeader("X-USER-ID") String accountId) {
        UserInfo userInfo = userFacade.getMyInfo(accountId);

        return ApiResponse.success(UserV1Dto.UserResponse.from(userInfo));
    }
}
