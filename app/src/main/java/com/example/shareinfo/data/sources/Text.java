package com.example.shareinfo.data.sources;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Small text helpers shared by the parsers. Plain Java so they work in JVM unit tests. */
final class Text {
    private static final Pattern TAG = Pattern.compile("<[^>]+>");
    private static final Pattern NUMERIC_ENTITY = Pattern.compile("&#(x?)([0-9a-fA-F]+);");

    private Text() {
    }

    static String decodeEntities(String text) {
        if (text == null || text.indexOf('&') < 0) {
            return text;
        }
        Matcher matcher = NUMERIC_ENTITY.matcher(text);
        StringBuffer decoded = new StringBuffer();
        while (matcher.find()) {
            int codePoint;
            try {
                codePoint = Integer.parseInt(matcher.group(2), matcher.group(1).isEmpty() ? 10 : 16);
            } catch (NumberFormatException e) {
                continue;
            }
            matcher.appendReplacement(decoded, Matcher.quoteReplacement(new String(Character.toChars(codePoint))));
        }
        matcher.appendTail(decoded);
        return decoded.toString()
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&#39;", "'")
                .replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&");
    }

    static String stripHtml(String html) {
        if (html == null) {
            return "";
        }
        return decodeEntities(TAG.matcher(html).replaceAll(" ")).replaceAll("\\s+", " ").trim();
    }

    /** ISO-8601 instant such as "2026-10-16T15:18:58Z"; 0 when unparseable. */
    static long parseIso(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        try {
            return Instant.parse(value).toEpochMilli();
        } catch (DateTimeParseException e) {
            try {
                return ZonedDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant().toEpochMilli();
            } catch (DateTimeParseException ignored) {
                return 0;
            }
        }
    }

    /** RSS date such as "Thu, 15 Oct 2026 19:31:20 GMT"; 0 when unparseable. */
    static long parseRfc1123(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        try {
            return ZonedDateTime.parse(value.trim(), DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.US))
                    .toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            return 0;
        }
    }
}
