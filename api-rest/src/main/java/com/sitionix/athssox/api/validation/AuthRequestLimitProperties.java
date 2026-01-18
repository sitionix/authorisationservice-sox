package com.sitionix.athssox.api.validation;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

@Component
@ConfigurationProperties(prefix = "auth.request-limits")
public class AuthRequestLimitProperties {

    private DataSize maxBodySize = DataSize.ofKilobytes(16);

    private int emailMaxLength = 254;

    private int passwordMaxLength = 128;

    private int sessionSourceIdMaxLength = 64;

    public DataSize getMaxBodySize() {
        return this.maxBodySize;
    }

    public void setMaxBodySize(final DataSize maxBodySize) {
        this.maxBodySize = maxBodySize;
    }

    public int getEmailMaxLength() {
        return this.emailMaxLength;
    }

    public void setEmailMaxLength(final int emailMaxLength) {
        this.emailMaxLength = emailMaxLength;
    }

    public int getPasswordMaxLength() {
        return this.passwordMaxLength;
    }

    public void setPasswordMaxLength(final int passwordMaxLength) {
        this.passwordMaxLength = passwordMaxLength;
    }

    public int getSessionSourceIdMaxLength() {
        return this.sessionSourceIdMaxLength;
    }

    public void setSessionSourceIdMaxLength(final int sessionSourceIdMaxLength) {
        this.sessionSourceIdMaxLength = sessionSourceIdMaxLength;
    }
}
