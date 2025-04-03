package com.gloud.auth.security;

import com.dmarket.domain.user.RefreshToken;
import com.dmarket.domain.user.User;
import com.dmarket.dto.common.UserCommonDto;
import com.dmarket.dto.response.CMResDto;
import com.dmarket.dto.response.UserResDto;
import com.dmarket.repository.user.RefreshTokenRepository;
import com.dmarket.repository.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${spring.jwt.expireT}")
    private Long jwtExpiration;

    @Value("${spring.jwt.ReFreshexpireT}")
    private Long RefreshJwtExpiration;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String[] excludePath = {"/", "/api/users/login", "/api/users/join", "api/users/email/**"};
        String path = request.getRequestURI();
        return Arrays.stream(excludePath).anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("JwtFilter");
//        //request에서 Authorization 헤더를 찾음
        String authHeader = jwtUtil.getAuthHeader(request);
        log.info("authHeader={}", authHeader);
        String token = null;

        try {
            token = jwtUtil.getToken(authHeader);
            log.info("token={}", token);
        } catch (IllegalArgumentException e) {
            log.warn(e.getMessage(), e.getCause());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            CMResDto<Void> cmRespDto = CMResDto.<Void>builder()
                    .code(HttpServletResponse.SC_UNAUTHORIZED)  // 401 Unauthorized
                    .msg("잘못된 토큰입니다.")
                    .build();

            writeResponse(response, cmRespDto);
            //조건이 해당되면 메소드 종료 (필수)
            return;
        }

//         헤더가 있기 때문에 헤더를 추출
//        String token = authHeader.split(" ")[1];

        // 헤더가 만료되었는 확인
        try {
            jwtUtil.isExpired(token);
        } catch (ExpiredJwtException e) {
            log.info("token expired");

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            CMResDto<Void> cmRespDto = CMResDto.<Void>builder()
                    .code(HttpServletResponse.SC_UNAUTHORIZED)  // 401 Unauthorized
                    .msg("토큰이 만료되었습니다.")
                    .build();

            writeResponse(response, cmRespDto);

            return;
        }

        //토큰에서 정보 추출
        String type = jwtUtil.getType(token);
        log.info("tokenType={}", type);
        String email = jwtUtil.getEmail(token);
        String role = jwtUtil.getRole(token);
        Long tokenUserId = jwtUtil.getUserId(token);


        // 타입이 refresh 인 경우 검증해서 재발급
        if (Objects.equals(type, "RTK")) {
            // refresh 토큰이 있으면
            if (refreshTokenRepository.existsById(token)) {
                refreshTokenRepository.deleteById(token);
                String newAccessToken = jwtUtil.createAccessJwt(tokenUserId, email, role);
                String newRefreshToken = jwtUtil.createRefreshJwt(tokenUserId);
                refreshTokenRepository.save(new RefreshToken(newRefreshToken, newAccessToken, email));

                UserCommonDto.TokenResponseDto tokenResponseDto = new UserCommonDto.TokenResponseDto(newAccessToken, newRefreshToken, tokenUserId, role);

                CMResDto<UserCommonDto.TokenResponseDto> cmRespDto = CMResDto.<UserCommonDto.TokenResponseDto>builder()
                        .code(200)
                        .msg("새로운 토큰 발급 Success")
                        .data(tokenResponseDto)
                        .build();

                writeResponse(response, cmRespDto);
                return;
            } else {
                // refresh 토큰이 없다면 다시 로그인 유도
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                CMResDto<Void> cmRespDto = CMResDto.<Void>builder()
                        .code(HttpServletResponse.SC_UNAUTHORIZED) // 401 Unauthorized
                        .msg("Refresh토큰이 없습니다. 다시 로그인 해주세요.")
                        .build();

                writeResponse(response, cmRespDto);
                return;
            }
        }

        User userEntity = userRepository.findByUserId(tokenUserId);
//        UserResDto.CustomUserDetails customUserDetails = new UserResDto.CustomUserDetails(userEntity);
        Authentication authToken = new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);
        log.info("userId={}", userEntity.getUserId());
        filterChain.doFilter(request, response);
    }


    // JSON 응답을 생성하는 메소드
    protected static void writeResponse(HttpServletResponse response, CMResDto<?> cmRespDto) {
        try {
            // cmRespDto 객체로 변환해서 타입 반환.
            ObjectMapper objectMapper = new ObjectMapper();

            // cmRespDto 내부에 LocalDatetime 형식 변환 설정.
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS"));

            // response body에 담기.
            String jsonResponse = objectMapper.writeValueAsString(cmRespDto);

            // response 타입지정.
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            // response 반환.
            response.getWriter().write(jsonResponse);

        } catch (IOException e) {
            // 에러 핸들링
            log.warn(e.getMessage(), e.getCause());
        }
    }
}
