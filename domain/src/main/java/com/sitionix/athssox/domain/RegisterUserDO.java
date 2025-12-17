package com.sitionix.athssox.domain;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterUserDO {

    @NotBlank
    @Email
    private String email;

    private UserRole role;

    private UserStatus status;

    @NotBlank
    private String password;

    private UUID siteId;

}
