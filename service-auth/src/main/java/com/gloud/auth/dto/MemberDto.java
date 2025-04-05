package com.gloud.auth.dto;

import com.gloud.auth.enums.Provider;
import com.gloud.auth.enums.Role;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MemberDto implements Serializable {
    String email;
    String username;
    String password;

}