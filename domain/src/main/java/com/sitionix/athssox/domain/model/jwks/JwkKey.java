package com.sitionix.athssox.domain.model.jwks;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@Builder
@EqualsAndHashCode
public class JwkKey {

    private String kty;

    private String kid;

    private String use;

    private String alg;

    private String n;

    private String e;
}
