package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    
    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User signUp(UserCommand.Create command) {
        if (userRepository.existsByAccountId(command.accountId())) {
            throw new CoreException(ErrorType.ALREADY_REGISTERED_USER, command.accountId());
        }
        
        User user = User.of(command);

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User findByAccountId(String accountId) {
        return userRepository.findByAccountId(accountId).orElseThrow(
                () -> new CoreException(ErrorType.USER_NOT_FOUND, accountId)
        );
    }

    public User findById(Long userId) {
        return userRepository.findById(userId).orElseThrow(
                () -> new CoreException(ErrorType.USER_NOT_FOUND, "존재하지 않는 유저입니다.")
        );
    }
}
