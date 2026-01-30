package com.sitionix.athssox.api.mapper;

import com.app_afesox.athssox.api_first.dto.IssueEmailVerificationLinkResponseDTO;
import com.sitionix.athssox.domain.config.MapstructComponent;
import com.sitionix.athssox.domain.model.emailverify.EmailVerificationLinkIssue;
import org.mapstruct.Mapper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

@Mapper(componentModel = MapstructComponent.SPRING_COMPONENT)
public interface EmailVerificationLinkApiMapper {

    IssueEmailVerificationLinkResponseDTO asResponse(EmailVerificationLinkIssue issue);

    default OffsetDateTime mapInstantToOffsetDateTime(final Instant instant) {
        if (Objects.isNull(instant)) {
            return null;
        }
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
