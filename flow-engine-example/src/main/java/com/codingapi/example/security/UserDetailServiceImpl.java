package com.codingapi.example.security;

import com.codingapi.example.entity.User;
import com.codingapi.example.repository.UserRepository;
import com.codingapi.springboot.security.gateway.TokenContext;
import lombok.AllArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserDetailServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.getUserByAccount(username);
        if (user == null) {
            throw new UsernameNotFoundException("can't fond username " + username + " account");
        }

        TokenContext.pushExtra(String.valueOf(user.getUserId()));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getAccount())
                .password(user.getPassword())
                .authorities(user.getRoles().stream().map(SimpleGrantedAuthority::new).toList())
                .build();
    }
}
