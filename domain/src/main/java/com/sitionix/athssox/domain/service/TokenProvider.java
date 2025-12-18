package com.sitionix.athssox.domain.service;

import com.sitionix.athssox.domain.model.AccessToken;
import com.sitionix.athssox.domain.model.AuthUser;
import com.sitionix.athssox.domain.model.RefreshToken;

public interface TokenProvider {

    AccessToken generateAccessToken(final AuthUser user);

    RefreshToken generateRefreshToken(final AuthUser user);
}
