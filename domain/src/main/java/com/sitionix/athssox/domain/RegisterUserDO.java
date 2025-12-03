package com.sitionix.athssox.domain;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterUserDO {

    private String email;

    private UserRole role;

    private UserStatus status;

    private String password;

    private UUID siteId;

}
