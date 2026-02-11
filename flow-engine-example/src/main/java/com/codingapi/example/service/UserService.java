package com.codingapi.example.service;

import com.codingapi.example.entity.User;
import com.codingapi.example.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void save(User user) {
        user.encodePassword(passwordEncoder);
        userRepository.save(user);
    }

    public void remove(long id) {
        userRepository.deleteById(id);
    }
}
