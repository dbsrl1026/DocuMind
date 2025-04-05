package com.gloud.auth.security;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.gloud.auth.dto.TokenDto;
import com.gloud.auth.repository.RedisTokenRepository;
import com.gloud.auth.util.ErrorResponseUtil;
import com.gloud.auth.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.filter.OncePerRequestFilter;


import java.io.IOException;
import java.util.Arrays;
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService customUserDetailsService;
    private final RedisTokenRepository redisTokenRepository; // Redis에서 토큰 확인할 Repository

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String[] excludePath = { "/", "/auth/register", "/auth/login", "/auth/refresh"};
        String path = request.getRequestURI();
        return Arrays.stream(excludePath).anyMatch(path::equals);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new JwtException("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);

        String tokenType = jwtUtil.getType(token);
        if (tokenType == null || !"ATK".equals(tokenType)) {
            throw new JwtException("Invalid or missing token type");
        }

        //  Redis 화이트리스트 확인 (존재하지 않으면 비정상 토큰)
        String email = jwtUtil.getEmail(token);
        String storedAccessToken = redisTokenRepository.getAccessToken(email);

        if (storedAccessToken == null || !storedAccessToken.equals(token)) {
            throw new JwtException("Access token not found in whitelist (Redis)");
        }

        //  JWT 유효성 검증
        jwtUtil.validateToken(token);


        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        if (userDetails == null) {
            throw new UsernameNotFoundException("User not found");
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            Authentication authenticationToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }

        filterChain.doFilter(request, response);
    }
}

