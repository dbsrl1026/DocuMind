package com.gloud.auth.entity;


import jakarta.persistence.*;
import lombok.*;

import java.util.Optional;
import java.util.function.Function;

@Entity
@Table(name = "tb_oauth")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "oauthId")
public class OauthToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oauth_id", nullable = false)
    private Long oauthId;

    @Column(name="access_token", length=500)
    private String accessToken;

    @Column(name="refresh_token", length=500)
    private String refreshToken;

    @ToString.Exclude
    @OneToOne(optional = false)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

}
