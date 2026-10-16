package com.example.shareinfo.sentiment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Reads the tab separated lexicon files shipped in assets/. */
public final class Lexicons {
    public static final String VADER_ASSET = "vader_lexicon.txt";
    public static final String MARKET_ASSET = "market_lexicon.txt";

    private Lexicons() {
    }

    /** Parses "token<TAB>valence[<TAB>...]" lines; lines starting with '#' are comments. */
    public static Map<String, Double> read(InputStream input) throws IOException {
        Map<String, Double> entries = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("\t");
                if (parts.length < 2) {
                    continue;
                }
                try {
                    // Keys are kept verbatim like the reference implementation, which looks words up in lower
                    // case; mixed case emoticons such as ":D" therefore never match.
                    entries.put(parts[0].trim(), Double.parseDouble(parts[1].trim()));
                } catch (NumberFormatException ignored) {
                    // Skip malformed rows rather than failing the whole lexicon.
                }
            }
        }
        return entries;
    }

    /**
     * Applies the market overlay on top of the VADER lexicon. Overlay entries win, and entries
     * with a valence of 0 are removed so the word no longer counts as sentiment.
     */
    public static Map<String, Double> merge(Map<String, Double> vader, Map<String, Double> overlay) {
        Map<String, Double> merged = new HashMap<>(vader);
        for (Map.Entry<String, Double> entry : overlay.entrySet()) {
            if (entry.getValue() == 0) {
                merged.remove(entry.getKey());
            } else {
                merged.put(entry.getKey(), entry.getValue());
            }
        }
        return merged;
    }

    /** Overlay keys written with underscores are multi-word phrases ("all_time_high"). */
    public static List<String> phrases(Map<String, Double> overlay) {
        List<String> phrases = new ArrayList<>();
        for (String key : overlay.keySet()) {
            if (key.indexOf('_') > 0) {
                phrases.add(key);
            }
        }
        return phrases;
    }
}
