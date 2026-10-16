package com.example.shareinfo.sentiment;

import com.example.shareinfo.data.Post;
import com.example.shareinfo.data.Source;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Combines per-post scores into one reading for a topic.
 *
 * Each post is weighted by how recent it is (half-life of {@value #HALF_LIFE_HOURS} hours) and how
 * much engagement it drew (logarithmic, so one viral post cannot drown out everyone else). The
 * weighted mean is then shrunk toward neutral by a small prior, so a handful of posts cannot
 * produce a "very bullish" reading on their own. Confidence comes from the effective sample size
 * of the weights.
 */
public class SentimentAggregator {
    /** Posts scoring inside +/- this band count as neutral (VADER's recommended threshold). */
    public static final double NEUTRAL_BAND = 0.05;
    static final double HALF_LIFE_HOURS = 48;
    /** Weight of the imaginary neutral posts every topic starts with. */
    static final double PRIOR_WEIGHT = 3;
    private static final int TREND_DAYS = 7;

    private final ZoneId zone;

    public SentimentAggregator(ZoneId zone) {
        this.zone = zone;
    }

    public SentimentSummary summarize(List<Post> posts, long nowMillis) {
        double weightSum = 0;
        double weightSquaredSum = 0;
        double weightedScoreSum = 0;
        int positive = 0;
        int neutral = 0;
        int negative = 0;
        Map<Source, double[]> sourceTotals = new EnumMap<>(Source.class);
        // Day -> {engagement weighted score sum, weight sum, count}.
        TreeMap<LocalDate, double[]> days = new TreeMap<>();
        LocalDate today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate();

        for (Post post : posts) {
            double weight = weight(post, nowMillis);
            weightSum += weight;
            weightSquaredSum += weight * weight;
            weightedScoreSum += weight * post.sentiment;

            if (post.sentiment > NEUTRAL_BAND) {
                positive++;
            } else if (post.sentiment < -NEUTRAL_BAND) {
                negative++;
            } else {
                neutral++;
            }

            double[] totals = sourceTotals.computeIfAbsent(post.source, s -> new double[2]);
            totals[0] += 1;
            totals[1] += post.sentiment;

            if (post.createdAtMillis > 0) {
                LocalDate day = Instant.ofEpochMilli(post.createdAtMillis).atZone(zone).toLocalDate();
                if (!day.isBefore(today.minusDays(TREND_DAYS - 1)) && !day.isAfter(today)) {
                    double[] bucket = days.computeIfAbsent(day, d -> new double[3]);
                    double engagementWeight = engagementWeight(post);
                    bucket[0] += engagementWeight * post.sentiment;
                    bucket[1] += engagementWeight;
                    bucket[2] += 1;
                }
            }
        }

        double score = weightSum > 0 ? weightedScoreSum / (weightSum + PRIOR_WEIGHT) : 0;
        double effectiveSampleSize = weightSquaredSum > 0 ? weightSum * weightSum / weightSquaredSum : 0;

        Map<Source, SentimentSummary.SourceBreakdown> bySource = new EnumMap<>(Source.class);
        for (Map.Entry<Source, double[]> entry : sourceTotals.entrySet()) {
            double[] totals = entry.getValue();
            bySource.put(entry.getKey(), new SentimentSummary.SourceBreakdown((int) totals[0], totals[1] / totals[0]));
        }
        List<SentimentSummary.DailyPoint> trend = new ArrayList<>();
        for (Map.Entry<LocalDate, double[]> entry : days.entrySet()) {
            double[] bucket = entry.getValue();
            trend.add(new SentimentSummary.DailyPoint(entry.getKey().atStartOfDay(zone).toInstant().toEpochMilli(),
                    bucket[0] / bucket[1], (int) bucket[2]));
        }

        return new SentimentSummary(score, label(score), confidence(posts.size(), effectiveSampleSize),
                positive, neutral, negative, bySource, trend);
    }

    static double weight(Post post, long nowMillis) {
        double ageHours = post.createdAtMillis > 0
                ? Math.max(0, (nowMillis - post.createdAtMillis) / 3_600_000.0)
                : HALF_LIFE_HOURS; // Undated posts count as moderately old.
        double recency = Math.pow(0.5, ageHours / HALF_LIFE_HOURS);
        return recency * engagementWeight(post);
    }

    /** 1 for no engagement, 2 for ~10 interactions, 3 for ~100. */
    static double engagementWeight(Post post) {
        return 1 + Math.log10(1 + Math.max(0, post.engagement()));
    }

    static SentimentSummary.Label label(double score) {
        double magnitude = Math.abs(score);
        if (magnitude < NEUTRAL_BAND) {
            return SentimentSummary.Label.NEUTRAL;
        }
        boolean positive = score > 0;
        if (magnitude < 0.15) {
            return positive ? SentimentSummary.Label.SLIGHTLY_POSITIVE : SentimentSummary.Label.SLIGHTLY_NEGATIVE;
        }
        if (magnitude < 0.35) {
            return positive ? SentimentSummary.Label.POSITIVE : SentimentSummary.Label.NEGATIVE;
        }
        return positive ? SentimentSummary.Label.VERY_POSITIVE : SentimentSummary.Label.VERY_NEGATIVE;
    }

    static SentimentSummary.Confidence confidence(int postCount, double effectiveSampleSize) {
        if (postCount == 0) {
            return SentimentSummary.Confidence.NONE;
        }
        if (effectiveSampleSize < 8) {
            return SentimentSummary.Confidence.LOW;
        }
        if (effectiveSampleSize < 25) {
            return SentimentSummary.Confidence.MEDIUM;
        }
        return SentimentSummary.Confidence.HIGH;
    }
}
