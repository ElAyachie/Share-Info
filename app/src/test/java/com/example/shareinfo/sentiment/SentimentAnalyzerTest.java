package com.example.shareinfo.sentiment;

import com.example.shareinfo.Fixtures;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SentimentAnalyzerTest {
    private static SentimentAnalyzer analyzer;

    @BeforeClass
    public static void setUp() throws Exception {
        analyzer = new SentimentAnalyzer(Fixtures.vaderLexicon());
    }

    /**
     * vader_reference.json holds scores from the reference Python implementation (vaderSentiment
     * 3.3.2, emoji translation disabled) for the README examples, edge cases and real posts
     * captured from every source the app reads.
     */
    @Test
    public void matchesReferenceImplementation() throws Exception {
        JSONArray cases = new JSONArray(Fixtures.read("vader_reference.json"));
        List<String> mismatches = new ArrayList<>();
        for (int i = 0; i < cases.length(); i++) {
            JSONObject expected = cases.getJSONObject(i);
            String text = expected.getString("text");
            SentimentScore actual = analyzer.polarityScores(text);
            // The reference rounds compound to 4 places and the proportions to 3.
            if (Math.abs(actual.compound - expected.getDouble("compound")) > 0.00051
                    || Math.abs(actual.positive - expected.getDouble("pos")) > 0.0051
                    || Math.abs(actual.negative - expected.getDouble("neg")) > 0.0051
                    || Math.abs(actual.neutral - expected.getDouble("neu")) > 0.0051) {
                mismatches.add(String.format("%s%n  expected %.4f, got %.4f", text, expected.getDouble("compound"), actual.compound));
            }
        }
        assertTrue(cases.length() + " cases, mismatches:\n" + String.join("\n", mismatches), mismatches.isEmpty());
    }

    @Test
    public void readmeExamples() {
        assertEquals(0.8316, analyzer.polarityScores("VADER is smart, handsome, and funny.").compound, 0.0001);
        assertEquals(-0.7424, analyzer.polarityScores("VADER is not smart, handsome, nor funny.").compound, 0.0001);
        assertEquals(0.431, analyzer.polarityScores("At least it isn't a horrible book.").compound, 0.0001);
    }

    @Test
    public void emptyTextIsNeutral() {
        assertEquals(0, analyzer.polarityScores("").compound, 0);
        assertEquals(0, analyzer.polarityScores("   ").compound, 0);
    }
}
