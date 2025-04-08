package com.gloud.auth.security.social;

import com.gloud.auth.enums.Provider;
import lombok.AllArgsConstructor;
import org.springframework.security.core.parameters.P;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
public class NaverOauth implements Oauth {

    private Map<String, Object> attributes;
    public NaverOauth() {
        this.attributes = new HashMap<>();
    }

    @Override
    public Provider getOauthProvider() {
        return Provider.NAVER;
    }

    @Override
    public String getOathEmail() {
        return (String) ((Map) attributes.get("response")).get("email");
    }

    @Override
    public String getOauthName() {
        return (String) ((Map) attributes.get("response")).get("name");
    }

}
