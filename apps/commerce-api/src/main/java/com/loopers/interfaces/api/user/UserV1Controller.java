package com.loopers.interfaces.api.user;

import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
public class UserV1Controller implements UserV1ApiSpec {

    private final UserService userService;

    @Autowired
    public UserV1Controller(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("")
    public ApiResponse<UserV1Dto.UserResponse> register(@RequestBody UserV1Dto.UserRegisterRequest request) {
        User registerUser = userService.register(request.userId(), request.email(), request.birthDate(), request.gender());

        return ApiResponse.success(UserV1Dto.UserResponse.from(registerUser));
    }

    @GetMapping("")
    public ApiResponse<UserV1Dto.UserResponse> getUser(@RequestHeader("X-USER-ID") String userId) {
        Optional<User> foundUser = userService.findByUserId(userId);

        if (foundUser.isEmpty()) {
            throw new CoreException(ErrorType.USER_NOT_FOUND, userId);
        }

        return ApiResponse.success(UserV1Dto.UserResponse.from(foundUser.get()));
    }

    @GetMapping("/points")
    public ApiResponse<UserV1Dto.UserPointResponse> getUserPoints(@RequestHeader("X-USER-ID") String userId) {
        Optional<User> foundUser = userService.findByUserId(userId);

        if (foundUser.isEmpty()) {
            throw new CoreException(ErrorType.USER_NOT_FOUND, userId);
        }

        return ApiResponse.success(UserV1Dto.UserPointResponse.from(foundUser.get()));
    }
}
