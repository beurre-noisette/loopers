package com.loopers.interfaces.api.user;

import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.interfaces.api.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
