package com.loopers.domain.user;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    boolean existsByAccountId(String accountId);

    Optional<User> findByAccountId(String accountId);

}
