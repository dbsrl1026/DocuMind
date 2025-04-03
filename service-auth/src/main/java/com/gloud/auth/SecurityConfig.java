package com.gloud.auth;


import com.gloud.auth.security.CustomLogoutHandler;
import com.gloud.auth.security.JwtFilter;
import com.gloud.auth.security.JwtUtil;
import com.gloud.auth.security.LoginFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final JwtFilter jwtFilter;
    private final CustomLogoutHandler customLogoutHandler;
    private final LoginFilter loginFilter;

    @Value("${spring.cors.path}")
    private List<String> corsPath;


    // PasswordEncoder 설정
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // 비밀번호 암호화
    }

    private static final String[] AUTH_WHITELIST = {
            "/auth/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // cors
        http
                .cors(corsConfigurer -> corsConfigurer.configurationSource(corsConfiguration()));

        // csrf disable
        http
                .csrf(AbstractHttpConfigurer::disable);

        // From 로그인 방식 disable
        http
                .formLogin(AbstractHttpConfigurer::disable);

        // http basic 인증 방식 disable
        http
                .httpBasic(AbstractHttpConfigurer::disable);

        // Session 설정
        http
                .sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // 로그아웃 접근 경로 설정, 접근 시 동작할 핸들러 지정
        http
                .logout((logout) -> logout.logoutUrl("/auth/logout")
                        .addLogoutHandler(customLogoutHandler)
                        .logoutSuccessHandler((request, response, authentication)
                                -> SecurityContextHolder.clearContext())
                );

        // 접근 권한 설정
        http
                .authorizeHttpRequests((auth) -> auth
                        .requestMatchers(AUTH_WHITELIST).permitAll()
                        .anyRequest().authenticated());


        // Error Handling : 기본 security 코드 이용 ( 주석 처리)
//        http
//                .exceptionHandling((exceptionHandling) -> exceptionHandling
//                        .authenticationEntryPoint(customAuthenticationEntryPoint())
//                        .accessDeniedHandler(customAccessDeniedHandler()));

        // UsernamePasswordAuthenticationFilter 자리에 LoginFilter 삽입 (실제로 override 되지는 않음)
        http.addFilterAt(loginFilter, UsernamePasswordAuthenticationFilter.class);

        // LoginFilter 앞에 JwtFilter 삽입
        http.addFilterBefore(new JwtFilter(jwtUtil, userRepository, refreshTokenRepository), LoginFilter.class);

        // JwtFilter 앞에 ExceptionHandlerFilter 삽입
//        http.addFilterBefore(new ExceptionHandlerFilter(jwtUtil), JwtFilter.class);

        return http.build();
    }

    private CorsConfigurationSource corsConfiguration() {
        CorsConfiguration configuration = new CorsConfiguration();


        configuration.setAllowedOriginPatterns(Arrays.asList("http://localhost*"));
//        configuration.setAllowedOriginPatterns(corsPath);                 //마지막으로 설정된 setAllowedOriginPatterns 만 적용 TODO : 완성하고 주석풀기
        configuration.setAllowedMethods(Collections.singletonList("*"));    // POST, GET, PUT ,DELETE 등 모든 메서드 허용
        configuration.setAllowCredentials(true);                            // 인증 정보 (쿠키, 인증 헤더 등)를 포함한 요청을 허용
        configuration.setAllowedHeaders(Collections.singletonList("*"));    // 모든 HTTP 요청 헤더 허용
        configuration.setExposedHeaders(Collections.singletonList("Authorization")); //  클라이언트가 접근할 수 있는 응답 헤더를 설정
        configuration.setMaxAge(3600L);                                     //  preflight 요청에 대한 응답을 캐시할 시간 : 1시간

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);      // 모든 (/**) 경로에 대해 위에서 정의한 configuration 객체의 CORS 설정을 적용

        return source;
    }

}
