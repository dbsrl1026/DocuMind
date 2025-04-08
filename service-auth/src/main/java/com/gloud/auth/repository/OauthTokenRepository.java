package com.gloud.auth.repository;

import com.gloud.auth.entity.Member;
import com.gloud.auth.entity.OauthToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OauthTokenRepository extends JpaRepository<OauthToken, Long> {

    Optional<OauthToken> findByMember(Member member);
}