package com.example.shareinfo;

import com.example.shareinfo.sentiment.Lexicons;
import com.example.shareinfo.sentiment.MarketSentiment;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Loads test fixtures and the lexicons shipped in the app's assets. */
public final class Fixtures {
    // Gradle runs unit tests from the module directory.
    private static final String ASSETS = "src/main/assets/";
    private static Map<String, Double> vader;
    private static Map<String, Double> market;

    private Fixtures() {
    }

    public static String read(String name) throws IOException {
        try (InputStream input = Fixtures.class.getResourceAsStream("/" + name)) {
            if (input == null) {
                throw new IOException("Missing fixture " + name);
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    public static synchronized Map<String, Double> vaderLexicon() throws IOException {
        if (vader == null) {
            try (InputStream input = new FileInputStream(ASSETS + Lexicons.VADER_ASSET)) {
                vader = Lexicons.read(input);
            }
        }
        return vader;
    }

    public static synchronized Map<String, Double> marketLexicon() throws IOException {
        if (market == null) {
            try (InputStream input = new FileInputStream(ASSETS + Lexicons.MARKET_ASSET)) {
                market = Lexicons.read(input);
            }
        }
        return market;
    }

    public static MarketSentiment marketSentiment() throws IOException {
        return new MarketSentiment(vaderLexicon(), marketLexicon());
    }
}
