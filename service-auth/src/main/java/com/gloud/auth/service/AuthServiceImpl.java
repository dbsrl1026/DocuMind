package com.gloud.auth.service;


import com.gloud.auth.dto.MemberDto;
import com.gloud.auth.dto.TokenDto;
import com.gloud.auth.entity.Member;
import com.gloud.auth.exception.UserAlreadyExistsException;
import com.gloud.auth.repository.MemberRepository;
import com.gloud.auth.repository.RedisTokenRepository;
import com.gloud.auth.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisTokenRepository redisTokenRepository;

    @Override
    public void Register(MemberDto memberDto) {

        String encodePassword = passwordEncoder.encode(memberDto.getPassword());

        if(memberRepository.existsByEmail(memberDto.getEmail())) {
            throw new UserAlreadyExistsException("User already exists");
        }

        Member member = Member.builder()
                .email(memberDto.getEmail())
                .password(encodePassword)
                .username(memberDto.getUsername())
                .build();

        memberRepository.save(member);
    }

    @Override
    public TokenDto reissueToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new JwtException("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);

        String tokenType = jwtUtil.getType(token);
        if (tokenType == null || !"RTK".equals(tokenType)) {
            throw new JwtException("Invalid or missing token type");
        }

        //  Redis 화이트리스트 확인 (존재하지 않으면 비정상 토큰)
        String email = jwtUtil.getEmail(token);
        String storedAccessToken = redisTokenRepository.getRefreshToken(email);

        if (storedAccessToken == null || !storedAccessToken.equals(token)) {
            throw new JwtException("Access token not found in whitelist (Redis)");
        }

        //  JWT 유효성 검증
        jwtUtil.validateToken(token);

        Long memberId = jwtUtil.getmemberId(token);
        String role = jwtUtil.getRole(token);

        String newAccessToken = jwtUtil.createAccessJwt(memberId, email, role);
        String newRefreshToken = jwtUtil.createRefreshJwt(memberId, email, role);


        redisTokenRepository.deleteTokens(email);
        redisTokenRepository.save(email, newAccessToken, newRefreshToken);
        TokenDto tokenDto = TokenDto.builder()
                .email(email)
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();

        return tokenDto;
    }
}
