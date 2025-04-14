package com.gloud.auth.controller;


import com.gloud.auth.dto.MemberDto;
import com.gloud.auth.dto.TokenDto;
import com.gloud.auth.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<String> Register(@RequestBody MemberDto memberDto) {

        authService.Register(memberDto);

        return new ResponseEntity<>("Register Success", HttpStatus.OK);
    }

    @PostMapping("/validate")
    public ResponseEntity<String> validateToken() {
        // SecurityContextHolder에서 Authentication 객체 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
        }

        // 권한 정보 확인
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername();
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

        // 권한 정보를 사용하여 요청 처리
        return new ResponseEntity<>(email, HttpStatus.OK);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenDto> reissueToken(@RequestHeader("Authorization") String authHeader) {

        TokenDto tokenDto = authService.reissueToken(authHeader);
        return new ResponseEntity<>(tokenDto, HttpStatus.OK);
    }
}
