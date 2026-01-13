package com.sitionix.athssox.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeviceSession {

    private UUID id;

    private AuthUser user;

    private String sessionSourceId;

    private SessionStatus status;

    private Instant createdAt;

    private Instant lastUsedAt;

    private String initialUserAgent;

    private String lastUserAgent;
}
