package com.sitionix.athssox.postgresql.repository;

import com.sitionix.athssox.domain.model.emailverify.EmailVerificationTokenRecord;
import com.sitionix.athssox.postgresql.entity.token.EmailVerificationTokenEntity;
import com.sitionix.athssox.postgresql.jpa.token.EmailVerificationTokenJpaRepository;
import com.sitionix.athssox.postgresql.mapper.token.EmailVerificationTokenInfraMapper;
import com.sitionix.athssox.postgresql.repository.token.EmailVerificationTokenRepositoryImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationTokenRepositoryImplTest {

    private EmailVerificationTokenRepositoryImpl emailVerificationTokenRepository;

    @Mock
    private EmailVerificationTokenJpaRepository emailVerificationTokenJpaRepository;

    @Mock
    private EmailVerificationTokenInfraMapper emailVerificationTokenInfraMapper;

    @BeforeEach
    void setUp() {
        this.emailVerificationTokenRepository = new EmailVerificationTokenRepositoryImpl(this.emailVerificationTokenJpaRepository,
                this.emailVerificationTokenInfraMapper);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.emailVerificationTokenJpaRepository,
                this.emailVerificationTokenInfraMapper);
    }

    @Test
    void givenTokenRecord_whenSave_thenPersistEntity() {
        //given
        final EmailVerificationTokenRecord given = mock(EmailVerificationTokenRecord.class);
        final EmailVerificationTokenEntity entity = mock(EmailVerificationTokenEntity.class);

        when(this.emailVerificationTokenInfraMapper.asEntity(given))
                .thenReturn(entity);

        //when
        this.emailVerificationTokenRepository.save(given);

        //then
        verify(this.emailVerificationTokenInfraMapper)
                .asEntity(given);
        verify(this.emailVerificationTokenJpaRepository)
                .save(entity);
    }

    @Test
    void givenHashedToken_whenFindByHashedToken_thenReturnRecord() {
        //given
        final String hashedToken = this.getHashedToken();
        final EmailVerificationTokenEntity entity = mock(EmailVerificationTokenEntity.class);
        final EmailVerificationTokenRecord expected = mock(EmailVerificationTokenRecord.class);

        when(this.emailVerificationTokenJpaRepository.findByTokenHash(hashedToken))
                .thenReturn(Optional.of(entity));
        when(this.emailVerificationTokenInfraMapper.asRecord(entity))
                .thenReturn(expected);

        //when
        final Optional<EmailVerificationTokenRecord> actual =
                this.emailVerificationTokenRepository.findByHashedToken(hashedToken);

        //then
        assertThat(actual).isEqualTo(Optional.of(expected));
        verify(this.emailVerificationTokenJpaRepository)
                .findByTokenHash(hashedToken);
        verify(this.emailVerificationTokenInfraMapper)
                .asRecord(entity);
    }

    @Test
    void givenHashedToken_whenFindByHashedTokenEmpty_thenReturnEmpty() {
        //given
        final String hashedToken = this.getHashedToken();

        when(this.emailVerificationTokenJpaRepository.findByTokenHash(hashedToken))
                .thenReturn(Optional.empty());

        //when
        final Optional<EmailVerificationTokenRecord> actual =
                this.emailVerificationTokenRepository.findByHashedToken(hashedToken);

        //then
        assertThat(actual).isEmpty();
        verify(this.emailVerificationTokenJpaRepository)
                .findByTokenHash(hashedToken);
    }

    private String getHashedToken() {
        return "hashed-token";
    }
}
