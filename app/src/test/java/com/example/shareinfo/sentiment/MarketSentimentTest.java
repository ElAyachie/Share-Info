package com.example.shareinfo.sentiment;

import com.example.shareinfo.Fixtures;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MarketSentimentTest {
    private static final List<String> NONE = Collections.emptyList();
    private static MarketSentiment sentiment;
    private static SentimentAnalyzer plainVader;

    @BeforeClass
    public static void setUp() throws Exception {
        sentiment = Fixtures.marketSentiment();
        plainVader = new SentimentAnalyzer(Fixtures.vaderLexicon());
    }

    private static double score(String text) {
        return sentiment.score(text, NONE).compound;
    }

    @Test
    public void understandsMarketSlang() {
        assertTrue(score("Super bullish here, $TSLA going to the moon") > 0.5);
        assertTrue(score("Bearish. This is going to crash hard") < -0.5);
        assertTrue(score("Got rekt, bagholding at the top") < -0.4);
        assertTrue(score("Earnings beat estimates and they raised guidance") > 0.4);
        assertTrue(score("Stock plunged after the downgrade") < -0.5);
    }

    @Test
    public void scoresEmojiEvenWhenAttachedToWords() {
        assertTrue(score("NVDA🚀🚀🚀") > 0.6);
        assertTrue(score("ugh 📉📉") < -0.4);
        // Variation selectors do not hide an emoji.
        assertTrue(score("🗑️") < 0);
    }

    @Test
    public void matchesPhrasesWrittenWithHyphensOrSpaces() {
        assertTrue(score("All-time high again") > 0.4);
        assertTrue(score("all time high again") > 0.4);
        assertTrue(score("Hit a new all-time low today") < -0.4);
        assertTrue(score("Classic dead cat bounce") < -0.3);
    }

    @Test
    public void neutralizesWordsThatAreNeutralInFinance() {
        // Plain VADER reads "shares", "interest" and "trust" as positive.
        String text = "Shares of the trust drew interest";
        assertTrue(plainVader.polarityScores(text).compound > 0.5);
        assertEquals(0, score(text), 0.0001);
    }

    @Test
    public void keepsNegationAndIntensifiers() {
        assertTrue(score("Not bullish at all") < 0);
        assertTrue(score("This isn’t a good entry") < 0);
        assertTrue(score("Extremely bullish") > score("Bullish"));
    }

    @Test
    public void ignoresTheTopicsOwnName() {
        List<String> bestBuy = Arrays.asList("Best Buy", "BBY");
        // Without the ignore list "best" (+3.2) would outweigh the actual news.
        assertTrue(sentiment.score("Best Buy stock falls after weak sales", NONE).compound > 0);
        assertTrue(sentiment.score("Best Buy stock falls after weak sales", bestBuy).compound < -0.3);
    }

    @Test
    public void ignoresCashtagsAndLinks() {
        assertEquals(0, score("$LOVE $WIN $HAPPY"), 0.0001);
        assertEquals(0, score("Read https://example.com/amazing-great-love-win"), 0.0001);
    }

    @Test
    public void fixesTheVersionOneScorersMistakes() {
        // Version 1 matched substrings, so "follow" counted as "low", and it listed "bullish" as bad.
        assertEquals(0, score("I follow this one"), 0.0001);
        assertTrue(score("bullish") > 0);
    }

    @Test
    public void blendsTheAuthorsOwnTag() {
        String neutralText = "Earnings report is on Thursday";
        assertEquals(0, sentiment.scorePost(neutralText, 0, NONE), 0.0001);
        assertEquals(0.4, sentiment.scorePost(neutralText, 1, NONE), 0.0001);
        assertEquals(-0.4, sentiment.scorePost(neutralText, -1, NONE), 0.0001);
        // A strongly negative post tagged bullish ends up near neutral rather than bullish.
        double contradictory = sentiment.scorePost("This company is a total disaster, awful", 1, NONE);
        assertTrue(contradictory < 0.1);
    }
}
