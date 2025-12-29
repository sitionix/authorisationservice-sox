package com.sitionix.athssox.domain.service;

import java.util.UUID;

public interface VerificationLinkFactory {
    String buildEmailVerifyUrl(String rawToken, UUID siteId);

}
