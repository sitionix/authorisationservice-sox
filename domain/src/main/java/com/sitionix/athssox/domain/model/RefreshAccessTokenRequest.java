package com.sitionix.athssox.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefreshAccessTokenRequest {

    private String refreshToken;

    private String sessionSourceId;

    private String userAgent;
}
