package com.sitionix.athssox.api.mapper;

import com.app_afesox.athssox.api_first.dto.IssueEmailVerificationLinkResponse;
import com.sitionix.athssox.domain.config.MapstructComponent;
import com.sitionix.athssox.domain.model.emailverify.EmailVerificationLinkIssue;
import org.mapstruct.Mapper;

import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

@Mapper(componentModel = MapstructComponent.SPRING_COMPONENT)
public interface EmailVerificationLinkApiMapper {

    IssueEmailVerificationLinkResponse asResponse(EmailVerificationLinkIssue issue);

    default URI map(final String value) {
        if (Objects.isNull(value)) {
            return null;
        }
        return URI.create(value);
    }

    default OffsetDateTime map(final Instant value) {
        if (Objects.isNull(value)) {
            return null;
        }
        return OffsetDateTime.ofInstant(value, ZoneOffset.UTC);
    }
}
