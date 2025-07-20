package com.loopers.interfaces.api.point;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.user.UserV1Dto;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/points")
public class PointV1Controller {

    private final UserFacade userFacade;

    @Autowired
    public PointV1Controller(UserFacade userFacade) {
        this.userFacade = userFacade;
    }

    @GetMapping("")
    public ApiResponse<UserV1Dto.UserPointResponse> getUserPoints(@RequestHeader("X-USER-ID") String userId) {
        UserInfo userInfo = userFacade.getMyInfo(userId);
        return ApiResponse.success(UserV1Dto.UserPointResponse.from(userInfo));
    }

    @PostMapping("")
    public ApiResponse<UserV1Dto.UserPointResponse> chargePoints(
            @RequestHeader("X-USER-ID") String userId,
            @Valid @RequestBody UserV1Dto.UserPointChargeRequest request)
    {
        UserInfo updatedUser = userFacade.chargePoint(userId, request.amount());

        return ApiResponse.success(UserV1Dto.UserPointResponse.from(updatedUser));
    }
}
