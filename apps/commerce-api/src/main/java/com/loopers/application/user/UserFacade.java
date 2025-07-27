package com.loopers.application.user;

import com.loopers.domain.user.User;
import com.loopers.domain.user.UserCommand;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@RequiredArgsConstructor
@Component
public class UserFacade {
    private final UserService userService;

    public UserInfo signUp(UserCommand.Create command) {
        User user = userService.signUp(command);
        return UserInfo.from(user);
    }

    public UserInfo getMyInfo(String userId) {
        User user = userService.findByUserId(userId)
                .orElseThrow(() -> new CoreException(ErrorType.USER_NOT_FOUND, userId));
        return UserInfo.from(user);
    }

    public UserPointInfo getMyPoint(String userId) {
        return userService.findByUserId(userId)
                .map(user -> UserPointInfo.from(user))
                .orElseThrow(() -> new CoreException(ErrorType.USER_NOT_FOUND));
    }

    public UserPointInfo chargePoint(String userId, int amount) {
        User user = userService.chargePoint(userId, amount);

        return UserPointInfo.from(user);
    }
}
