package com.loopers.domain.user;

public interface UserRepository {

    User save(User user);

    boolean existsByUserId(String userId);

}
