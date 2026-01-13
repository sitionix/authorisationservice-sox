package com.sitionix.athssox.domain.usecase;

import com.sitionix.athssox.domain.model.RefreshAccessTokenRequest;
import com.sitionix.athssox.domain.model.RefreshAccessTokenResponse;

public interface RefreshAccessToken {

    RefreshAccessTokenResponse execute(final RefreshAccessTokenRequest refreshAccessTokenRequest);
}
