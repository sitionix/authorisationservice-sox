package com.sitionix.athssox.domain.model.emailverify;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class EmailVerification {

    private UUID siteId;

    private String token;
}
