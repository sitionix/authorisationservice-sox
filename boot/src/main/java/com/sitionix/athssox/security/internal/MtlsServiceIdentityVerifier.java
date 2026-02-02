package com.sitionix.athssox.security.internal;

import com.sitionix.athssox.application.security.internal.ServiceIdentity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.security.auth.x500.X500Principal;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MtlsServiceIdentityVerifier {

    private static final String CERT_ATTRIBUTE = "jakarta.servlet.request.X509Certificate";
    private static final String LEGACY_CERT_ATTRIBUTE = "javax.servlet.request.X509Certificate";
    private static final Pattern CN_PATTERN = Pattern.compile("CN=([^,]+)", Pattern.CASE_INSENSITIVE);

    public ServiceIdentity verify(final Object certificateAttribute) {
        final X509Certificate[] certificates = this.extractCertificates(certificateAttribute);
        if (certificates.length == 0) {
            throw new BadCredentialsException("mTLS client certificate required");
        }
        final X509Certificate certificate = certificates[0];
        final String subject = this.resolveSubject(certificate);
        final String serviceName = this.extractCommonName(subject);
        return new ServiceIdentity(serviceName, certificate.getIssuerX500Principal().getName(),
                null, List.of());
    }

    public ServiceIdentity verify(final jakarta.servlet.http.HttpServletRequest request) {
        final Object attr = request.getAttribute(CERT_ATTRIBUTE);
        if (attr == null) {
            return this.verify(request.getAttribute(LEGACY_CERT_ATTRIBUTE));
        }
        return this.verify(attr);
    }

    private X509Certificate[] extractCertificates(final Object attribute) {
        if (attribute instanceof X509Certificate[] certificates) {
            return certificates;
        }
        return new X509Certificate[0];
    }

    private String resolveSubject(final X509Certificate certificate) {
        final X500Principal principal = certificate.getSubjectX500Principal();
        return principal == null ? "" : principal.getName();
    }

    private String extractCommonName(final String subject) {
        if (!StringUtils.hasText(subject)) {
            return "unknown";
        }
        final Matcher matcher = CN_PATTERN.matcher(subject);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return subject;
    }
}
