package com.gloud.auth.service;

import com.gloud.auth.dto.MemberDto;
import com.gloud.auth.dto.TokenDto;

public interface AuthService {
    void Register(MemberDto memberDto);

    TokenDto reissueToken(String authHeader);
}
