package com.gloud.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gloud.auth.dto.LoginRequest;
import com.gloud.auth.dto.TokenDto;
import com.gloud.auth.repository.RedisTokenRepository;
import com.gloud.auth.util.JwtUtil;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

import java.io.IOException;


//@Component
public class LoginFilter extends UsernamePasswordAuthenticationFilter {

    private final JwtUtil jwtUtil;
    private final RedisTokenRepository redisTokenRepository;
    private final AuthenticationManager authenticationManager;
    private final ObjectMapper objectMapper;

    public LoginFilter(JwtUtil jwtUtil, RedisTokenRepository redisTokenRepository, AuthenticationManager authenticationManager, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.redisTokenRepository = redisTokenRepository;
        this.authenticationManager = authenticationManager;
        this.objectMapper = objectMapper;
        setFilterProcessesUrl("/auth/login");
        super.setAuthenticationManager(authenticationManager);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
            // 요청 본문에서 JSON 데이터 읽기
            LoginRequest loginRequest = objectMapper.readValue(request.getInputStream(), LoginRequest.class);

            // LoginRequest 객체에서 email과 password 추출
            String useremail = loginRequest.getEmail();
            String password = loginRequest.getPassword();

            // 유저 정보 검증을 위해 이메일, 패스워드 값 전달
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(useremail, password, null);

            return authenticationManager.authenticate(authToken);

        } catch (IOException e) {
            throw new AuthenticationServiceException("Failed to read authentication request", e);
        }
    }

    // 요청받은 정보가 DB에 있는 사용자인 경우
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) throws IOException {

        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();

        String email = customUserDetails.getUsername();
        Long memberId = customUserDetails.getMemberId();

        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse(null); // 권한이 없는 경우 null 반환


        String accesstoken = jwtUtil.createAccessJwt(memberId, email, role);

        String refreshtoken = jwtUtil.createRefreshJwt(memberId, email, role);

        redisTokenRepository.deleteTokens(email);
        redisTokenRepository.save(email, accesstoken, refreshtoken);

        TokenDto tokenDto = new TokenDto(email, accesstoken, refreshtoken);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // TokenDto를 JSON으로 변환
        ObjectMapper objectMapper = new ObjectMapper();
        String tokenJson = objectMapper.writeValueAsString(tokenDto);

        // 응답에 JSON 데이터 쓰기
        response.setStatus(HttpServletResponse.SC_OK); // 200 OK 상태 코드 설정 (선택적)
        response.getWriter().write(tokenJson);

    }

    // 요청받은 정보가 DB에 없는 사용자인 경우
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException {
        throw new UsernameNotFoundException("User not found");
    }


}
