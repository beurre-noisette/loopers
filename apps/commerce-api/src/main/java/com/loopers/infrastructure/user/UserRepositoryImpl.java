package com.loopers.infrastructure.user;

import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    @Autowired
    public UserRepositoryImpl(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public User save(User user) {
        return userJpaRepository.save(user);
    }

    @Override
    public boolean existsByUserId(String userId) {
        return userJpaRepository.existsByUserId(userId);
    }
}
