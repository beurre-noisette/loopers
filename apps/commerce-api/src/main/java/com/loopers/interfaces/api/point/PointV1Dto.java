package com.loopers.interfaces.api.point;

import com.loopers.application.user.UserPointInfo;

public class PointV1Dto {

    public record PointResponse(
            int point
    ) {
        public static PointResponse from(UserPointInfo userPointInfo) {
            return new PointResponse(
                    userPointInfo.point().intValue()
            );
        }
    }
}
