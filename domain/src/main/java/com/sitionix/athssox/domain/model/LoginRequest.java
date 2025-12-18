package com.sitionix.athssox.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {

    private String email;

    private String password;

    private UUID siteId;

    private String sessionSourceId;

    private String userAgent;
}
