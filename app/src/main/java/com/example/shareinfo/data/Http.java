package com.example.shareinfo.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Minimal blocking HTTP client. Call it from a background thread. */
public final class Http {
    // Yahoo answers 429 and Reddit 403 to requests without a descriptive user agent.
    private static final String USER_AGENT = "android:com.example.shareinfo:2.0 (ShareInfo sentiment reader)";
    private static final int TIMEOUT_MILLIS = 15_000;

    private Http() {
    }

    public static String get(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            connection.setConnectTimeout(TIMEOUT_MILLIS);
            connection.setReadTimeout(TIMEOUT_MILLIS);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Accept", "application/json, application/xml;q=0.9, */*;q=0.8");
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new HttpException(code, connection.getURL().getHost());
            }
            try (InputStream input = connection.getInputStream()) {
                return readFully(input);
            }
        } finally {
            connection.disconnect();
        }
    }

    public static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }

    static String readFully(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[16 * 1024];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }
}
