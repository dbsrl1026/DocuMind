package com.gloud.auth.security.social;

import com.gloud.auth.enums.Provider;

public interface Oauth {
    public Provider getOauthProvider();
    public String getOathEmail();
    public String getOauthName();
}
