package com.gloud.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.SimpleDateFormat;


@Component
public class LoginFilter extends UsernamePasswordAuthenticationFilter {



    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @PostConstruct
    public void init() {
        setFilterProcessesUrl("/auth/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        // 로그인 요청 API 에서 password 값을 추출
        String password = obtainPassword(request);

        // 로그인 요청 API 에서 email 값을 추출
        String useremail = request.getParameter("email");

        // 유저 정보 검증을 위해 이메일, 패스워드 값 전달
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(useremail, password, null);

        return this.getAuthenticationManager().authenticate(authToken);
    }

    // 요청받은 정보가 DB에 있는 사용자인 경우
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) {

        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();

        String email = customUserDetails.getEmail();
        Long userId = customUserDetails.getUserId();

        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse(null); // 권한이 없는 경우 null 반환


        String accesstoken = jwtUtil.createAccessJwt(userId, email, role);

        String refreshtoken = jwtUtil.createRefreshJwt(userId);

        refreshTokenRepository.save(new RefreshToken(refreshtoken, accesstoken, email));

        UserCommonDto.TokenResponseDto tokenResponseDto = new UserCommonDto.TokenResponseDto(accesstoken, refreshtoken, userId, role);

        CMResDto<UserCommonDto.TokenResponseDto> cmRespDto = CMResDto.<UserCommonDto.TokenResponseDto>builder()
                .code(200)
                .msg("Success")
                .data(tokenResponseDto)
                .build();

        // HttpServletRequest 에 body에 정보를 담기.
        writeResponse(response, cmRespDto);
    }

    // 요청받은 정보가 DB에 없는 사용자인 경우
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) {
        CMResDto<Void> cmRespDto = CMResDto.<Void>builder()
                .code(HttpServletResponse.SC_UNAUTHORIZED) // 401 Unauthorized
                .msg("아이디 또는 비밀번호가 틀렸습니다.")
                .build();

        writeResponse(response, cmRespDto);
        response.setStatus(401);
    }


    private void writeResponse(HttpServletResponse response, CMResDto<?> cmRespDto) {
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
