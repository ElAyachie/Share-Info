package com.example.shareinfo.sentiment;

import com.example.shareinfo.data.Source;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Overall sentiment for a topic, aggregated from many scored posts. */
public class SentimentSummary {
    public enum Label {
        VERY_NEGATIVE, NEGATIVE, SLIGHTLY_NEGATIVE, NEUTRAL, SLIGHTLY_POSITIVE, POSITIVE, VERY_POSITIVE;

        public boolean isPositive() {
            return ordinal() > NEUTRAL.ordinal();
        }

        public boolean isNegative() {
            return ordinal() < NEUTRAL.ordinal();
        }
    }

    public enum Confidence { NONE, LOW, MEDIUM, HIGH }

    /** Weighted, sample-size adjusted mean from -1 to +1. */
    public final double score;
    public final Label label;
    public final Confidence confidence;
    public final int positiveCount;
    public final int neutralCount;
    public final int negativeCount;
    public final Map<Source, SourceBreakdown> bySource;
    /** One point per day that had posts, oldest first. */
    public final List<DailyPoint> trend;

    public SentimentSummary(double score, Label label, Confidence confidence, int positiveCount, int neutralCount,
                            int negativeCount, Map<Source, SourceBreakdown> bySource, List<DailyPoint> trend) {
        this.score = score;
        this.label = label;
        this.confidence = confidence;
        this.positiveCount = positiveCount;
        this.neutralCount = neutralCount;
        this.negativeCount = negativeCount;
        this.bySource = Collections.unmodifiableMap(bySource);
        this.trend = Collections.unmodifiableList(trend);
    }

    public int totalCount() {
        return positiveCount + neutralCount + negativeCount;
    }

    /** The score on a 0-100 scale where 50 is neutral; easier to read at a glance. */
    public int index() {
        return (int) Math.round((score + 1) * 50);
    }

    public static class SourceBreakdown {
        public final int count;
        public final double mean;

        public SourceBreakdown(int count, double mean) {
            this.count = count;
            this.mean = mean;
        }
    }

    public static class DailyPoint {
        public final long dayStartMillis;
        public final double mean;
        public final int count;

        public DailyPoint(long dayStartMillis, double mean, int count) {
            this.dayStartMillis = dayStartMillis;
            this.mean = mean;
            this.count = count;
        }
    }
}
