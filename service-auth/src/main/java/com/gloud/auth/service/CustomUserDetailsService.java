package com.gloud.auth.service;


import com.gloud.auth.entity.Member;
import com.gloud.auth.repository.MemberRepository;
import com.gloud.auth.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;


    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email:  " + email));


        return new CustomUserDetails(
                member.getMemberId(),
                member.getEmail(),
                member.getPassword(),    // ------------ 비밀번호는 전달만 하고, 검증은 Spring Security가 처리
                List.of(new SimpleGrantedAuthority("ROLE_" + member.getRole().toString())
                )
        );
    }
}


