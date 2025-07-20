package com.loopers.application.user;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class UserFacade {
    private final UserService userService;

    public UserInfo signUp(String userId, String email, String birthDate, Gender gender) {
        User user = userService.signUp(userId, email, birthDate, gender);
        return UserInfo.from(user);
    }

    public UserInfo getMyInfo(String userId) {
        User user = userService.findByUserId(userId)
                .orElseThrow(() -> new CoreException(ErrorType.USER_NOT_FOUND, userId));
        return UserInfo.from(user);
    }

    public UserInfo chargePoint(String userId, int amount) {
        User user = userService.chargePoint(userId, amount);
        return UserInfo.from(user);
    }
}
