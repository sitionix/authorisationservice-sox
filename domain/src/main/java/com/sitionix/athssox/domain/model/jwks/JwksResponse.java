package com.sitionix.athssox.domain.model.jwks;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@EqualsAndHashCode
public class JwksResponse {

    private List<JwkKey> keys;
}
