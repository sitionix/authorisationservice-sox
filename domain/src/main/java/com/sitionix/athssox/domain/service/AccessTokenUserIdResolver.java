package com.sitionix.athssox.domain.service;

public interface AccessTokenUserIdResolver {

    Long resolveUserId(String accessToken);
}
