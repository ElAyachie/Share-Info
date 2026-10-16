package com.example.shareinfo.sentiment;

/** VADER output for one piece of text. */
public class SentimentScore {
    public static final SentimentScore NEUTRAL = new SentimentScore(0, 0, 0, 0);

    /** Overall polarity from -1 (most negative) to +1 (most positive). */
    public final double compound;
    /** Proportions of the text that read positive, negative and neutral; they sum to ~1. */
    public final double positive;
    public final double negative;
    public final double neutral;

    public SentimentScore(double compound, double positive, double negative, double neutral) {
        this.compound = compound;
        this.positive = positive;
        this.negative = negative;
        this.neutral = neutral;
    }
}
