package com.gloud.auth.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {
    private final Long userId;
    private final String email;
    private final String username;
    private final String password;
    private final List<GrantedAuthority> authorities;

    //private MemberDTO memberDTO;                                                  //---------------------- 추가

    public CustomUserDetails(Long userId, String email, String username, String password, List<GrantedAuthority> authorities) {
        this.userId = userId;
        this.email = email;
        this.username = username;
        this.password = password;
        this.authorities = authorities;
                                    //---------------------- 추가
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
