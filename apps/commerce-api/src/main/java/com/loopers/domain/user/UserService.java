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
    public User register(String userId, String email, String birthDate, Gender gender) {
        if (userRepository.existsByUserId(userId)) {
            throw new CoreException(ErrorType.ALREADY_REGISTERED_USER, userId);
        }
        
        User user = new User(userId, email, birthDate, gender);

        return userRepository.save(user);
    }
}
