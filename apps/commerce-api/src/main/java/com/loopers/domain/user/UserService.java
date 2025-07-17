package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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
        
        User user = new User(userId, email, birthDate, gender, 0);

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUserId(String userId) {
        return userRepository.findByUserId(userId);
    }
}
