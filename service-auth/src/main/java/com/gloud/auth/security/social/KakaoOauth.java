package com.gloud.auth.security.social;

import com.gloud.auth.enums.Provider;
import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
public class KakaoOauth implements Oauth {

    private Map<String, Object> attributes;
    public KakaoOauth() {
        this.attributes = new HashMap<>();
    }

    @Override
    public Provider getOauthProvider() {
        return Provider.KAKAO;
    }

    @Override
    public String getOathEmail() {
        return (String) ((Map) attributes.get("kakao_account")).get("email");
        //return (String) attributes.get("email");
    }

    @Override
    public String getOauthName() {
        return (String) ((Map) attributes.get("properties")).get("nickname");
        //return (String) attributes.get("name");
    }

}
