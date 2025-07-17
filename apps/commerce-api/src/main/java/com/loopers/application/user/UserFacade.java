package com.loopers.application.user;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class UserFacade {
    private final UserService userService;

    public UserInfo register(String userId, String email, String birthDate, Gender gender) {
        User user = userService.register(userId, email, birthDate, gender);
        return UserInfo.from(user);
    }

    public Optional<UserInfo> findByUserId(String userId) {
        Optional<User> user = userService.findByUserId(userId);
        return user.map(UserInfo::from);
    }

    public UserInfo chargePoint(String userId, int amount) {
        User user = userService.chargePoint(userId, amount);
        return UserInfo.from(user);
    }
}