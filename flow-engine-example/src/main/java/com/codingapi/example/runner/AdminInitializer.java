package com.codingapi.example.runner;

import com.codingapi.example.entity.User;
import com.codingapi.example.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        User admin = userRepository.getUserByAccount(User.ADMIN_ACCOUNT);
        if (admin == null) {
            admin = User.admin(passwordEncoder);
            userRepository.save(admin);
        }
    }
}
