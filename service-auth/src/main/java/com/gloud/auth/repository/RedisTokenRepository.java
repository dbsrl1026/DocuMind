package com.gloud.auth.repository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class RedisTokenRepository {

    private final StringRedisTemplate redisTemplate;

    @Value("${spring.jwt.expiration_time.access}")
    private long AccTokenExpTime;

    @Value("${spring.jwt.expiration_time.refresh}")
    private long RefreshTokenExpTime;

    private Duration ACCESS_TOKEN_TTL;
    private Duration REFRESH_TOKEN_TTL;

    @PostConstruct
    public void init() {
        ACCESS_TOKEN_TTL = Duration.ofMinutes(AccTokenExpTime);
        REFRESH_TOKEN_TTL = Duration.ofMinutes(RefreshTokenExpTime);
    }

    public void save(String email, String accessToken, String refreshToken) {
        redisTemplate.opsForValue().set(getAccessKey(email), accessToken, ACCESS_TOKEN_TTL);
        redisTemplate.opsForValue().set(getRefreshKey(email), refreshToken, REFRESH_TOKEN_TTL);
    }

    public String getAccessToken(String email) {
        return redisTemplate.opsForValue().get(getAccessKey(email));
    }

    public String getRefreshToken(String email) {
        return redisTemplate.opsForValue().get(getRefreshKey(email));
    }

    public void deleteTokens(String email) {
        redisTemplate.delete(getAccessKey(email));
        redisTemplate.delete(getRefreshKey(email));
    }

    private String getAccessKey(String email) {
        return "ACCESS:" + email;
    }

    private String getRefreshKey(String email) {
        return "REFRESH:" + email;
    }
}
