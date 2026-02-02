package com.sitionix.athssox.application.security.internal;

import java.util.List;

public record ServiceIdentity(String serviceName,
                              String issuer,
                              String audience,
                              List<String> scopes) {
}
