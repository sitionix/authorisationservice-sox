package com.sitionix.athssox.api.mapper;

import com.app_afesox.athssox.api_first.dto.IssueEmailVerificationLinkResponseDTO;
import com.sitionix.athssox.domain.model.emailverify.EmailVerificationLinkIssue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EmailVerificationLinkApiMapperTest {

    private EmailVerificationLinkApiMapper emailVerificationLinkApiMapper;

    @BeforeEach
    void setUp() {
        this.emailVerificationLinkApiMapper = new EmailVerificationLinkApiMapperImpl();
    }

    @Test
    void givenEmailVerificationLinkIssue_whenAsResponse_thenReturnResponseDto() {
        //given
        final UUID tokenId = this.getTokenId();
        final UUID siteId = this.getSiteId();
        final Instant expiresAt = this.getExpiresAt();

        final EmailVerificationLinkIssue given = this.getEmailVerificationLinkIssue(tokenId, siteId, expiresAt);
        final IssueEmailVerificationLinkResponseDTO expected = this.getIssueEmailVerificationLinkResponseDTO(
                tokenId,
                siteId,
                this.getExpiresAtOffsetDateTime(expiresAt)
        );

        //when
        final IssueEmailVerificationLinkResponseDTO actual = this.emailVerificationLinkApiMapper.asResponse(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    private EmailVerificationLinkIssue getEmailVerificationLinkIssue(final UUID tokenId,
                                                                     final UUID siteId,
                                                                     final Instant expiresAt) {
        return new EmailVerificationLinkIssue(tokenId,
                siteId,
                "token",
                expiresAt);
    }

    private IssueEmailVerificationLinkResponseDTO getIssueEmailVerificationLinkResponseDTO(final UUID tokenId,
                                                                                           final UUID siteId,
                                                                                           final OffsetDateTime expiresAt) {
        return IssueEmailVerificationLinkResponseDTO.builder()
                .tokenId(tokenId)
                .siteId(siteId)
                .token("token")
                .expiresAt(expiresAt)
                .build();
    }

    private UUID getTokenId() {
        return UUID.fromString("91a8a86b-21ad-4b8d-8c62-5d6f5d0a89da");
    }

    private UUID getSiteId() {
        return UUID.fromString("b8aabef0-47a7-4c18-8d8b-cc72d1d00b8a");
    }

    private Instant getExpiresAt() {
        return Instant.parse("2024-01-10T12:30:00Z");
    }

    private OffsetDateTime getExpiresAtOffsetDateTime(final Instant expiresAt) {
        return OffsetDateTime.ofInstant(expiresAt, ZoneOffset.UTC);
    }
}
