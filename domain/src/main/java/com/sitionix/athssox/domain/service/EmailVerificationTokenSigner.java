package com.sitionix.athssox.domain.service;

import java.util.UUID;

public interface EmailVerificationTokenSigner {

    String sign(UUID tokenId, UUID pepperId);

    String buildToken(UUID tokenId, UUID pepperId);
}
