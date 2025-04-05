package com.gloud.auth.exception;

import com.gloud.auth.dto.ErrorResponse;
import com.gloud.auth.util.ErrorResponseUtil;
import com.nimbusds.jose.jwk.JWKException;
import io.jsonwebtoken.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;
import java.util.Map;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ErrorResponseUtil errorResponseUtil;

    // 1. 인증 실패 (로그인 정보가 잘못됨)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return errorResponseUtil.createErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid Email or password.");
    }

    // 2. 인증 실패 (사용자 정보 없음)
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFound(UsernameNotFoundException ex) {
        return errorResponseUtil.createErrorResponse(HttpStatus.UNAUTHORIZED, "User not found.");
    }

    // 3. 인가 실패 (접근 권한 없음)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return errorResponseUtil.createErrorResponse(HttpStatus.FORBIDDEN, "Unauthorized.");
    }

    // 4. JWT 토큰 관련 - 만료
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ErrorResponse> handleExpiredJwt(ExpiredJwtException ex) {
        return errorResponseUtil.createErrorResponse(HttpStatus.UNAUTHORIZED, "Token is expired.");
    }

    // 5. JWT 토큰 관련 - 서명 오류, 유효하지 않음 등
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(SecurityException ex) {
        return errorResponseUtil.createErrorResponse(HttpStatus.UNAUTHORIZED, "Security issue in JWT");
    }

    @ExceptionHandler(MalformedJwtException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJwtException(MalformedJwtException ex) {
        return errorResponseUtil.createErrorResponse(HttpStatus.UNAUTHORIZED, "Malformed JWT token");
    }

    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<ErrorResponse> handleSignatureException(SignatureException ex) {
        return errorResponseUtil.createErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid JWT signature");
    }

    @ExceptionHandler(UnsupportedJwtException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedJwtException(UnsupportedJwtException ex) {
        return errorResponseUtil.createErrorResponse(HttpStatus.UNAUTHORIZED, "Unsupported JWT token");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        return errorResponseUtil.createErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid JWT argument");
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResponse> handleJwtException(JwtException ex) {
        return errorResponseUtil.createErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    // 6. 사용자 중복 (회원가입 시 중복된 이메일 등)
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        return errorResponseUtil.createErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    // 7. 모든 그 외 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        return errorResponseUtil.createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error.");
    }
}
