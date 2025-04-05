package com.gloud.auth.entity;

import com.gloud.auth.enums.Provider;
import com.gloud.auth.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.ZoneId;


@Entity
@Table(name = "tb_member")
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "memberId")
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "email", unique = true, length = 64, nullable = false)
    private String email;

    @Column(name= "username", length = 32, nullable = false)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 16, nullable = false)
    private Role role;

    @Column(name = "regdate")
    //@DateTimeFormat은 컨트롤러에서 날짜 형식을 지정할 때 사용되는 어노테이션입니다. 엔티티에서는 불필요합니다.
    private LocalDate regdate;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", length = 16)
    private Provider provider;

    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Oauth oauth;

    @PrePersist
    protected void onCreate() {
        if (this.role == null) {
            this.role = Role.USER;
        }
        if (this.regdate == null) {
            this.regdate = LocalDate.now(ZoneId.of("Asia/Seoul"));
        }
        if (this.provider == null) {
            this.provider = Provider.LOCAL;
        }
    }


}
