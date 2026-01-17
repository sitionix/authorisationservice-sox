package com.sitionix.athssox.domain.service;

import com.sitionix.athssox.domain.model.jwks.JwksResponse;

public interface JwksProvider {

    JwksResponse getJwks();
}
