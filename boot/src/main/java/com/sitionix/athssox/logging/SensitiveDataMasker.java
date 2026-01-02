package com.sitionix.athssox.logging;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SensitiveDataMasker {

    private static final Pattern TO_FIELD_PATTERN =
            Pattern.compile("\"to\"\\s*:\\s*\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);

    private SensitiveDataMasker() {
    }

    public static String mask(final String message) {
        if (message == null || message.isBlank()) {
            return message;
        }

        final Matcher matcher = TO_FIELD_PATTERN.matcher(message);
        if (!matcher.find()) {
            return message;
        }

        final StringBuffer masked = new StringBuffer();
        do {
            final String email = matcher.group(1);
            final String replacement = "\"to\":\"" + maskEmail(email) + "\"";
            matcher.appendReplacement(masked, Matcher.quoteReplacement(replacement));
        } while (matcher.find());
        matcher.appendTail(masked);

        return masked.toString();
    }

    private static String maskEmail(final String email) {
        if (email == null || email.isBlank()) {
            return "***";
        }
        final int atIndex = email.indexOf('@');
        if (atIndex <= 0 || atIndex == email.length() - 1) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}
