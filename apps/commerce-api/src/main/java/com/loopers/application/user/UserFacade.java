package com.loopers.application.user;

import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointPolicy;
import com.loopers.domain.point.PointReference;
import com.loopers.domain.point.PointService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserCommand;
import com.loopers.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;


@RequiredArgsConstructor
@Component
public class UserFacade {
    private final UserService userService;
    private final PointService pointService;

    public UserInfo signUp(UserCommand.Create command) {
        User user = userService.signUp(command);

        PointPolicy.PointCreationPolicy pointPolicy = PointPolicy.UserRegistration.getCreationPolicy();

        pointService.createPointWithInitialAmount(
                user.getId(),
                pointPolicy.initialAmount(),
                PointReference.welcomeBonus()
        );

        return UserInfo.from(user);
    }

    public UserInfo getMyInfo(String userId) {
        User user = userService.findByUserId(userId);

        return UserInfo.from(user);
    }

    public UserPointInfo getMyPoint(String userId) {
        User user = userService.findByUserId(userId);
        Point point = pointService.getPoint(user.getId());

        return UserPointInfo.from(user, point);
    }

    public UserPointInfo chargePoint(String userId, int amount) {
        User user  = userService.findByUserId(userId);
        pointService.chargePoint(user.getId(), BigDecimal.valueOf(amount), PointReference.userCharge());
        Point point = pointService.getPoint(user.getId());

        return UserPointInfo.from(user, point);
    }
}
