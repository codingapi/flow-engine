package com.codingapi.example.security;

import com.codingapi.example.entity.User;
import com.codingapi.example.repository.UserRepository;
import com.codingapi.springboot.framework.user.UserContext;
import com.codingapi.springboot.security.filter.AuthenticationTokenFilter;
import com.codingapi.springboot.security.gateway.TokenContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

@Component
@AllArgsConstructor
public class MyAuthenticationTokenFilter implements AuthenticationTokenFilter {

    private final UserRepository userRepository;

    @Override
    public void doFilter(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String userId = TokenContext.current().getExtra();
        if (StringUtils.hasText(userId)) {
            User user = userRepository.getUserById(Long.parseLong(userId));
            UserContext.getInstance().setCurrent(user);
        }
    }
}
