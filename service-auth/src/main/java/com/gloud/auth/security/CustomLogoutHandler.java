package com.gloud.auth.security;


import com.gloud.auth.repository.RedisTokenRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class CustomLogoutHandler implements LogoutHandler {
    private final RedisTokenRepository redisTokenRepository;


    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        // 요청에서 이메일 정보를 추출 (예: 헤더, 파라미터, 쿠키 등)
        String email = request.getParameter("email");

        if (email != null && !email.isEmpty()) {
            redisTokenRepository.deleteTokens(email);
        } else {
            throw new IllegalArgumentException("Email not found in request");
        }
    }
}
