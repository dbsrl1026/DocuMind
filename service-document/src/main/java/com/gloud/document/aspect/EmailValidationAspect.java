package com.gloud.document.aspect;

import com.gloud.document.annotation.ValidateEmailClaim;
import com.gloud.document.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Field;
import java.nio.file.AccessDeniedException;

@Aspect
@Component
@RequiredArgsConstructor
public class EmailValidationAspect {

    private final JwtUtil jwtUtil;

    @Around("@annotation(validateEmailClaim)")
    public Object validateEmail(ProceedingJoinPoint joinPoint, ValidateEmailClaim validateEmailClaim) throws Throwable {

        // 1. JWT에서 email 추출
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String token = request.getHeader("Authorization");
        String emailFromToken = jwtUtil.getEmail(token);

        // 2. 파라미터에서 email 찾기
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();

        boolean matchFound = false;

        for (int i = 0; i < args.length; i++) {
            String paramName = paramNames[i];
            Object value = args[i];

            // 2-1. email 파라미터 직접 일치
            if (paramName.equals(validateEmailClaim.paramName()) && value instanceof String) {
                if (!emailFromToken.equals(value)) {
                    throw new AccessDeniedException("email from token doesn't match with email token.");
                }
                matchFound = true;
                break;
            }

            // 2-2. DTO 등 내부에 email 필드가 있는 경우 (예: @RequestBody)
            if (value != null) {
                try {
                    Field emailField = value.getClass().getDeclaredField(validateEmailClaim.paramName());
                    emailField.setAccessible(true);
                    Object innerEmail = emailField.get(value);

                    if (innerEmail instanceof String && !emailFromToken.equals(innerEmail)) {
                        throw new AccessDeniedException("email from token doesn't match with email token.");
                    }
                    matchFound = true;
                    break;
                } catch (NoSuchFieldException ignored) {
                    // 해당 객체에 email 필드 없으면 무시
                }
            }
        }

        if (!matchFound) {
            throw new IllegalArgumentException("Cannot find '" + validateEmailClaim.paramName() + "' from request parameters.");
        }

        return joinPoint.proceed();
    }
}