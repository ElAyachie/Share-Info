package com.example.shareinfo.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

/** Minimal blocking HTTP client. Call it from a background thread. */
public final class Http {
    // Yahoo answers 429 and Reddit 403 to requests without a descriptive user agent.
    private static final String USER_AGENT = "android:com.example.shareinfo:2.0 (ShareInfo sentiment reader)";
    private static final int TIMEOUT_MILLIS = 15_000;
    // Error bodies are only kept to explain the failure.
    private static final int MAX_ERROR_BODY = 4096;

    private Http() {
    }

    public static String get(String url) throws IOException {
        return request("GET", url, Collections.emptyMap(), null);
    }

    public static String get(String url, Map<String, String> headers) throws IOException {
        return request("GET", url, headers, null);
    }

    /** Posts an application/x-www-form-urlencoded body. */
    public static String postForm(String url, Map<String, String> headers, String form) throws IOException {
        return request("POST", url, headers, form);
    }

    private static String request(String method, String url, Map<String, String> headers, String form) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            connection.setRequestMethod(method);
            connection.setConnectTimeout(TIMEOUT_MILLIS);
            connection.setReadTimeout(TIMEOUT_MILLIS);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Accept", "application/json, application/xml;q=0.9, */*;q=0.8");
            for (Map.Entry<String, String> header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
            if (form != null) {
                byte[] body = form.getBytes(StandardCharsets.UTF_8);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setFixedLengthStreamingMode(body.length);
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(body);
                }
            }
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new HttpException(code, connection.getURL().getHost(), readError(connection));
            }
            try (InputStream input = connection.getInputStream()) {
                return readFully(input, Integer.MAX_VALUE);
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

    private static String readError(HttpURLConnection connection) {
        try (InputStream error = connection.getErrorStream()) {
            return error == null ? "" : readFully(error, MAX_ERROR_BODY);
        } catch (IOException e) {
            return "";
        }
    }

    static String readFully(InputStream input, int limit) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[16 * 1024];
        int read;
        while (output.size() < limit && (read = input.read(buffer)) != -1) {
            output.write(buffer, 0, Math.min(read, limit - output.size()));
        }
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }
}
