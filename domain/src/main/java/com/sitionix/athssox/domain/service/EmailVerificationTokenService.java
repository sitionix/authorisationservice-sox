package com.sitionix.athssox.domain.service;

import java.util.UUID;

public interface EmailVerificationTokenService {
    String issue(Long userId, UUID siteId);
}
