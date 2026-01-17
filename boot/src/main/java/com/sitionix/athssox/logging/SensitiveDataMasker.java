package com.sitionix.athssox.logging;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SensitiveDataMasker {

    private static final Pattern TO_FIELD_PATTERN =
            Pattern.compile("\"to\"\\s*:\\s*\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern TOKEN_FIELD_PATTERN =
            Pattern.compile("\"token\"\\s*:\\s*\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern PASSWORD_FIELD_PATTERN =
            Pattern.compile("\"password\"\\s*:\\s*\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern REFRESH_TOKEN_FIELD_PATTERN =
            Pattern.compile("\"refreshToken\"\\s*:\\s*\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern VERIFY_URL_FIELD_PATTERN =
            Pattern.compile("\"verifyUrl\"\\s*:\\s*\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern TOKEN_QUERY_PARAM_PATTERN =
            Pattern.compile("(?i)(\\btoken=)([^&\\s\\\"]+)");
    private static final Pattern PASSWORD_KEY_VALUE_PATTERN =
            Pattern.compile("(?i)(password\\s*[=:]\\s*)([^,\\s\\)]+)([,\\)])?");
    private static final Pattern REFRESH_TOKEN_KEY_VALUE_PATTERN =
            Pattern.compile("(?i)(refreshToken\\s*[=:]\\s*)([^,\\s\\)]+)([,\\)])?");

    private SensitiveDataMasker() {
    }

    public static String mask(final String message) {
        if (message == null || message.isBlank()) {
            return message;
        }

        String masked = message;
        masked = maskJsonField(masked, TO_FIELD_PATTERN, SensitiveDataMasker::maskEmail, "to");
        masked = maskJsonField(masked, TOKEN_FIELD_PATTERN, value -> "***", "token");
        masked = maskJsonField(masked, PASSWORD_FIELD_PATTERN, value -> "***", "password");
        masked = maskJsonField(masked, REFRESH_TOKEN_FIELD_PATTERN, value -> "***", "refreshToken");
        masked = maskJsonField(masked, VERIFY_URL_FIELD_PATTERN, SensitiveDataMasker::maskVerifyUrl, "verifyUrl");
        masked = maskTokenQueryParam(masked);
        masked = maskKeyValue(masked, PASSWORD_KEY_VALUE_PATTERN);
        masked = maskKeyValue(masked, REFRESH_TOKEN_KEY_VALUE_PATTERN);

        return masked;
    }

    private static String maskJsonField(final String message,
                                        final Pattern pattern,
                                        final Function<String, String> valueMasker,
                                        final String fieldName) {
        final Matcher matcher = pattern.matcher(message);
        if (!matcher.find()) {
            return message;
        }

        final StringBuffer masked = new StringBuffer();
        do {
            final String raw = matcher.group(1);
            final String replacementValue = valueMasker.apply(raw);
            final String replacement = "\"" + fieldName + "\":\"" + replacementValue + "\"";
            matcher.appendReplacement(masked, Matcher.quoteReplacement(replacement));
        } while (matcher.find());
        matcher.appendTail(masked);

        return masked.toString();
    }

    private static String maskTokenQueryParam(final String message) {
        final Matcher matcher = TOKEN_QUERY_PARAM_PATTERN.matcher(message);
        if (!matcher.find()) {
            return message;
        }

        final StringBuffer masked = new StringBuffer();
        do {
            final String replacement = matcher.group(1) + "***";
            matcher.appendReplacement(masked, Matcher.quoteReplacement(replacement));
        } while (matcher.find());
        matcher.appendTail(masked);

        return masked.toString();
    }

    private static String maskKeyValue(final String message, final Pattern pattern) {
        final Matcher matcher = pattern.matcher(message);
        if (!matcher.find()) {
            return message;
        }

        final StringBuffer masked = new StringBuffer();
        do {
            final String delimiter = matcher.groupCount() >= 3 && matcher.group(3) != null
                    ? matcher.group(3)
                    : "";
            final String replacement = matcher.group(1) + "***" + delimiter;
            matcher.appendReplacement(masked, Matcher.quoteReplacement(replacement));
        } while (matcher.find());
        matcher.appendTail(masked);

        return masked.toString();
    }

    private static String maskVerifyUrl(final String url) {
        if (url == null || url.isBlank()) {
            return "***";
        }
        return TOKEN_QUERY_PARAM_PATTERN.matcher(url).replaceAll("$1***");
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
