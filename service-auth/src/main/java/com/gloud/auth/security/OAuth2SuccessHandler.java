package com.gloud.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gloud.auth.dto.TokenDto;
import com.gloud.auth.entity.Member;
import com.gloud.auth.repository.MemberRepository;
import com.gloud.auth.repository.RedisTokenRepository;
import com.gloud.auth.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final RedisTokenRepository redisTokenRepository;
    private final MemberRepository memberRepository;
    private final ObjectMapper objectMapper;


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = (String) oAuth2User.getAttribute("email");
        Long memberId = Long.parseLong(oAuth2User.getAttribute("memberId").toString());
        String role = (String) oAuth2User.getAttribute("role");


        // JWT 생성
        String accessToken = jwtUtil.createAccessJwt(memberId, email, role);
        String refreshToken = jwtUtil.createRefreshJwt(memberId, email, role);

        // Redis 저장
        redisTokenRepository.deleteTokens(email);
        redisTokenRepository.save(email, accessToken, refreshToken);

        // 응답 반환 : 리다이렉션 아닌 JSON 반환 : 프론트에서 팝업 방식으로 OAuth 로그인 처리하고 부모창에 메시지로 전달하는 과정 필요
        TokenDto tokenDto = new TokenDto(email, accessToken, refreshToken);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(objectMapper.writeValueAsString(tokenDto));
    }
}