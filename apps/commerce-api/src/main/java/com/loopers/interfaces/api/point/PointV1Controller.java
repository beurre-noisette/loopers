package com.loopers.interfaces.api.point;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserPointInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.user.UserV1Dto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/points")
public class PointV1Controller implements PointV1ApiSpec {

    private final UserFacade userFacade;

    @Autowired
    public PointV1Controller(UserFacade userFacade) {
        this.userFacade = userFacade;
    }

    @GetMapping("")
    public ApiResponse<PointV1Dto.PointResponse> getMyPoint(@RequestHeader("X-USER-ID") String userId) {
        UserPointInfo userPointInfo = userFacade.getMyPoint(userId);

        return ApiResponse.success(PointV1Dto.PointResponse.from(userPointInfo));
    }

    @PostMapping("")
    public ApiResponse<PointV1Dto.PointResponse> chargePoints(
            @RequestHeader("X-USER-ID") String userId,
            @RequestBody UserV1Dto.UserPointChargeRequest request)
    {
        UserPointInfo userPointInfo = userFacade.chargePoint(userId, request.amount());

        return ApiResponse.success(PointV1Dto.PointResponse.from(userPointInfo));
    }
}
