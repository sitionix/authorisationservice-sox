package com.sitionix.athssox.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResponseRegisterUser {

    private Long userId;
    private String message;
    private UserStatus status;

}
