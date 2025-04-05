package com.gloud.auth.util;


import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {


    private final Key SECRET_KEY;
    private final long ACCESS_EXPIRATION_TIME;
    private final long REFRESH_EXPIRATION_TIME;

    public JwtUtil(
            @Value("${spring.jwt.secret}") String secretKey,
            @Value("${spring.jwt.expiration_time.access}") long AccTokenExpTime,
            @Value("${spring.jwt.expiration_time.refresh}") long RefTokenExpTime
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.SECRET_KEY = Keys.hmacShaKeyFor(keyBytes);
        this.ACCESS_EXPIRATION_TIME = AccTokenExpTime * 60 * 1000L;
        this.REFRESH_EXPIRATION_TIME = RefTokenExpTime * 60 * 1000L;
    }

//    public boolean validateToken(String token) {
//        try {
//            Jwts.parserBuilder().setSigningKey(SECRET_KEY).build().parseClaimsJws(token);
//            return true;
//        } catch (SecurityException | MalformedJwtException e) {
//            System.out.println("Invalid JWT Token" + e.getMessage());
//        } catch (ExpiredJwtException e) {
//            System.out.println("Expired Token" + e.getMessage());
//        } catch (UnsupportedJwtException e) {
//            System.out.println("Unsupported Token" + e.getMessage());
//        } catch (IllegalArgumentException e) {
//            System.out.println("IllegalArgument claims Token" + e.getMessage());
//        } catch (SignatureException e) {
//            System.out.println("Signature Error" + e.getMessage());
//        } catch (JwtException e) {
//            System.out.println("JWT Exception" + e.getMessage());
//        }
//        return false;
//    }


    public void validateToken(String token) {
        Jwts.parserBuilder().setSigningKey(SECRET_KEY).build().parseClaimsJws(token);
    }


    public Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(SECRET_KEY).build().parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }


    public String getEmail(String token) {
        return parseClaims(token).get("email", String.class);
    }
    public Long getmemberId(String token) {
        return parseClaims(token).get("memberId", Long.class);
    }

    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public String getType(String token) {
        return parseClaims(token).get("type", String.class);
    }

    public String createAccessJwt(Long memberId, String email, String role) {
        Claims claims = Jwts.claims();
        claims.put("memberId", memberId);
        claims.put("role", role);
        claims.put("email", email);
        claims.put("type", "ATK");


        return Jwts.builder()
//                .setIssuer("https://example.com")
//                .setAudience("https://api.example.com")
                .setSubject(email)
                .setClaims(claims)
                .setIssuedAt(new Date())                                                //기본 클레임(토큰발급시간)
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

    public String createRefreshJwt(Long memberId, String email, String role) {
        Claims claims = Jwts.claims();
        claims.put("memberId", memberId);
        claims.put("role", role);
        claims.put("email", email);
        claims.put("type", "RTK");

        return Jwts.builder()
//                .setIssuer("https://example.com")
//                .setAudience("https://api.example.com")
                .setSubject(email)
                .setClaims(claims)
                .setIssuedAt(new Date())                                                //기본 클레임(토큰발급시간)
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .setId(UUID.randomUUID().toString()) // jti
                .compact();
    }



}
