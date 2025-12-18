package com.sitionix.athssox.domain.repository;

import com.sitionix.athssox.domain.model.RefreshTokenRecord;

public interface RefreshTokenRepository {

    void save(final RefreshTokenRecord refreshTokenRecord);
}
