package com.gloud.auth.service;

import com.gloud.auth.entity.Member;
import com.gloud.auth.entity.OauthToken;
import com.gloud.auth.enums.Provider;
import com.gloud.auth.enums.Role;
import com.gloud.auth.repository.MemberRepository;
import com.gloud.auth.repository.OauthTokenRepository;
import com.gloud.auth.security.social.GoogleOauth;
import com.gloud.auth.security.social.KakaoOauth;
import com.gloud.auth.security.social.NaverOauth;
import com.gloud.auth.security.social.Oauth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;


@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private OauthTokenRepository oauthTokenRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        //access-token
        String accessToken = userRequest.getAccessToken().getTokenValue();
        //provider이름
        String registrationId = userRequest.getClientRegistration().getRegistrationId(); //google
        //nameAttributeKey 이름
//        String nameAttributeKey = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName(); // "sub", "id" 등
        //refresh-token
        String refreshToken = userRequest.getAdditionalParameters().get("refresh-token") != null
                ? userRequest.getAdditionalParameters().get("refresh-token").toString() : "";

        Oauth oauth = switch (registrationId) {
            case "google" -> new GoogleOauth(oAuth2User.getAttributes());
            case "naver" -> new NaverOauth(oAuth2User.getAttributes());
            case "kakao" -> new KakaoOauth(oAuth2User.getAttributes());
            default -> throw new OAuth2AuthenticationException("Unknown provider");
        };

        String email = oauth.getOathEmail();
        String username = oauth.getOauthName();
        Provider provider = oauth.getOauthProvider();

        // DB에 유저 없으면 회원가입
        Member member = memberRepository.findByEmail(email)
                .orElseGet(() -> memberRepository.save(
                        Member.builder()
                                .email(email)
                                .username(username)
                                .provider(provider)
                                .role(Role.USER) // 권한 설정
                                .build()
                ));

        // 기존 토큰이 있으면 업데이트, 없으면 새로 저장
        OauthToken oauthToken = oauthTokenRepository.findByMember(member)
                .map(token -> {
                    token.setAccessToken(accessToken);
                    token.setRefreshToken(refreshToken);
                    return token;
                })
                .orElseGet(() -> OauthToken.builder()
                        .member(member)
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .build()
                );

        oauthTokenRepository.save(oauthToken);

        // Member를 UserDetails 로 감싸서 리턴하거나 Principal로 사용
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(member.getRole().name())),
                Map.of(
                        "email", member.getEmail(),
                        "memberId", member.getMemberId(),
                        "role", member.getRole().name()
                ),
                "email"
        );
    }
}