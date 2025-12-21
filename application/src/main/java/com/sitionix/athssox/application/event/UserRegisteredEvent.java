package com.sitionix.athssox.application.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class UserRegisteredEvent {

    private final Long userId;
    private final String email;
    private final UUID siteId;
}
