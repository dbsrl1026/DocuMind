package com.gloud.auth.security;


import com.nimbusds.oauth2.sdk.token.RefreshToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomLogoutHandler implements LogoutHandler {


    private final JwtUtil jwtUtil;

    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        String authorization = request.getHeader("Authorization");
        String token = authorization.split(" ")[1];

        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByAccessToken(token);
        if (refreshToken.isPresent()) {
            refreshTokenRepository.deleteById(refreshToken.get().getRefreshToken());
        } else {
            System.out.println("사용자 정보 오류");
        }
    }
}
