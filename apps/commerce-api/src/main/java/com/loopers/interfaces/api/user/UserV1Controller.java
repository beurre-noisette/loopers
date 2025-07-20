package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
public class UserV1Controller implements UserV1ApiSpec {

    private final UserFacade userFacade;

    @Autowired
    public UserV1Controller(UserFacade userFacade) {
        this.userFacade = userFacade;
    }

    @PostMapping("")
    public ApiResponse<UserV1Dto.UserResponse> register(@RequestBody UserV1Dto.UserRegisterRequest request) {
        UserInfo userInfo = userFacade.register(request.userId(), request.email(), request.birthDate(), request.gender());

        return ApiResponse.success(UserV1Dto.UserResponse.from(userInfo));
    }

    @GetMapping("")
    public ApiResponse<UserV1Dto.UserResponse> getUser(@RequestHeader("X-USER-ID") String userId) {
        Optional<UserInfo> foundUser = userFacade.findByUserId(userId);

        if (foundUser.isEmpty()) {
            throw new CoreException(ErrorType.USER_NOT_FOUND, userId);
        }

        return ApiResponse.success(UserV1Dto.UserResponse.from(foundUser.get()));
    }
}
