package com.sitionix.athssox.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefreshAccessTokenResponse {

    private String accessToken;

    private String refreshToken;

    private long expiresIn;

    private TokenType tokenType;
}
