package com.sitionix.athssox.domain.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResendEmailVerificationResponse {

    private String message;
}
