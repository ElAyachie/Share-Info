package com.example.shareinfo.sentiment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scores posts about a market topic. Wraps {@link SentimentAnalyzer} with clean-up that matters
 * for financial chatter: links, cashtags, emoji, multi-word phrases ("all-time high") and the
 * topic's own name are handled before the text is scored.
 */
public class MarketSentiment {
    /** Valence given to an explicit "Bullish"/"Bearish" tag before blending with the text. */
    private static final double LABEL_STRENGTH = 0.8;
    /** Share of the final score that comes from the author's own tag when one is present. */
    private static final double LABEL_WEIGHT = 0.5;

    private static final Pattern URL = Pattern.compile("(?i)\\b(?:https?://|www\\.)\\S+");
    private static final Pattern CASHTAG = Pattern.compile("\\$[A-Za-z][A-Za-z0-9.\\-]{0,11}\\b");
    // Variation selectors and skin tone modifiers change how an emoji looks, not what it means.
    private static final Pattern EMOJI_MODIFIERS = Pattern.compile("[\\x{FE0E}\\x{FE0F}\\x{1F3FB}-\\x{1F3FF}\\x{200D}]");

    private final SentimentAnalyzer analyzer;
    private final List<PhraseRule> phraseRules = new ArrayList<>();
    private final Set<Integer> emoji = new HashSet<>();

    public MarketSentiment(Map<String, Double> vaderLexicon, Map<String, Double> marketOverlay) {
        this.analyzer = new SentimentAnalyzer(Lexicons.merge(vaderLexicon, marketOverlay));

        List<String> phrases = Lexicons.phrases(marketOverlay);
        // Longest phrases first so "price target cut" wins over "cut".
        phrases.sort(Comparator.comparingInt(String::length).reversed());
        for (String phrase : phrases) {
            phraseRules.add(new PhraseRule(phrase));
        }
        for (String key : marketOverlay.keySet()) {
            int codePoint = key.codePointAt(0);
            if (Character.charCount(codePoint) == key.length() && !Character.isLetterOrDigit(codePoint)) {
                emoji.add(codePoint);
            }
        }
    }

    /** Scores free text. {@code ignoreTerms} are names that should not count as sentiment. */
    public SentimentScore score(String text, Collection<String> ignoreTerms) {
        return analyzer.polarityScores(prepare(text, ignoreTerms));
    }

    /**
     * Scores a post, blending in the author's own Bullish/Bearish tag when the source provides
     * one (StockTwits does). The tag is a strong signal, but sarcastic or contradictory text can
     * still pull the score back toward neutral.
     *
     * @param authorLabel +1 for bullish, -1 for bearish, 0 when untagged.
     */
    public double scorePost(String text, int authorLabel, Collection<String> ignoreTerms) {
        double textScore = score(text, ignoreTerms).compound;
        if (authorLabel == 0) {
            return textScore;
        }
        return LABEL_WEIGHT * (authorLabel * LABEL_STRENGTH) + (1 - LABEL_WEIGHT) * textScore;
    }

    String prepare(String text, Collection<String> ignoreTerms) {
        String prepared = text.replace('’', '\'').replace('‘', '\'');
        prepared = URL.matcher(prepared).replaceAll(" ");
        // Tickers can collide with lexicon words ($FUN, $LOVE, $SAFE).
        prepared = CASHTAG.matcher(prepared).replaceAll(" ");
        for (String term : ignoreTerms) {
            if (term != null && term.trim().length() > 1) {
                prepared = Pattern.compile("(?i)(?<![\\w])" + Pattern.quote(term.trim()) + "(?![\\w])")
                        .matcher(prepared).replaceAll(" ");
            }
        }
        for (PhraseRule rule : phraseRules) {
            prepared = rule.apply(prepared);
        }
        return separateEmoji(EMOJI_MODIFIERS.matcher(prepared).replaceAll(""));
    }

    /** Surrounds known emoji with spaces so "moon🚀🚀" scores the word and each rocket. */
    private String separateEmoji(String text) {
        StringBuilder builder = new StringBuilder(text.length() + 16);
        int i = 0;
        while (i < text.length()) {
            int codePoint = text.codePointAt(i);
            if (emoji.contains(codePoint)) {
                builder.append(' ').appendCodePoint(codePoint).append(' ');
            } else {
                builder.appendCodePoint(codePoint);
            }
            i += Character.charCount(codePoint);
        }
        return builder.toString();
    }

    /** Rewrites a phrase such as "all time high" / "All-Time High" into the token "all_time_high". */
    private static final class PhraseRule {
        private final Pattern pattern;
        private final String token;

        PhraseRule(String phrase) {
            this.token = phrase;
            String[] words = phrase.split("_");
            StringBuilder regex = new StringBuilder("(?i)(?<![\\w])");
            for (int i = 0; i < words.length; i++) {
                if (i > 0) {
                    regex.append("[\\s\\-]+");
                }
                regex.append(Pattern.quote(words[i]));
            }
            regex.append("(?![\\w])");
            this.pattern = Pattern.compile(regex.toString());
        }

        String apply(String text) {
            return pattern.matcher(text).replaceAll(Matcher.quoteReplacement(" " + token + " "));
        }
    }
}
