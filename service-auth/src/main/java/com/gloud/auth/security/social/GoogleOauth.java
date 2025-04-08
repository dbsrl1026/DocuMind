package com.gloud.auth.security.social;

import com.gloud.auth.enums.Provider;
import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
public class GoogleOauth implements Oauth {

    private Map<String, Object> attributes;
    public GoogleOauth() {
        this.attributes = new HashMap<>();
    }

    @Override
    public Provider getOauthProvider() {
        return Provider.GOOGLE;
    }

    @Override
    public String getOathEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getOauthName() {
        return (String) attributes.get("name");
    }


}
