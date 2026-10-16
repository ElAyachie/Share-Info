package com.example.shareinfo.sentiment;

import com.example.shareinfo.data.Post;
import com.example.shareinfo.data.Source;

import org.junit.Test;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SentimentAggregatorTest {
    private static final long HOUR = 3_600_000L;
    // 2026-10-16T12:00:00Z
    private static final long NOW = 1792152000000L;

    private final SentimentAggregator aggregator = new SentimentAggregator(ZoneOffset.UTC);

    private static Post post(Source source, double sentiment, long ageHours, int likes) {
        Post post = new Post(source, "id", "author", "text", null, NOW - ageHours * HOUR, likes, 0, 0, 0);
        post.sentiment = sentiment;
        return post;
    }

    @Test
    public void noPostsMeansNoReading() {
        SentimentSummary summary = aggregator.summarize(Collections.emptyList(), NOW);
        assertEquals(0, summary.score, 0);
        assertEquals(SentimentSummary.Label.NEUTRAL, summary.label);
        assertEquals(SentimentSummary.Confidence.NONE, summary.confidence);
        assertEquals(50, summary.index());
    }

    @Test
    public void manyAgreeingPostsGiveAConfidentReading() {
        List<Post> posts = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            posts.add(post(Source.STOCKTWITS, 0.6, 1, 0));
        }
        SentimentSummary summary = aggregator.summarize(posts, NOW);
        assertEquals(SentimentSummary.Label.VERY_POSITIVE, summary.label);
        assertEquals(SentimentSummary.Confidence.HIGH, summary.confidence);
        assertEquals(40, summary.positiveCount);
    }

    @Test
    public void aSinglePostIsShrunkTowardNeutral() {
        SentimentSummary summary = aggregator.summarize(Collections.singletonList(post(Source.BLUESKY, 0.9, 0, 0)), NOW);
        // 0.9 * 1 / (1 + prior of 3)
        assertEquals(0.225, summary.score, 0.0001);
        assertEquals(SentimentSummary.Label.POSITIVE, summary.label);
        assertEquals(SentimentSummary.Confidence.LOW, summary.confidence);
    }

    @Test
    public void engagementCountsButCannotDominate() {
        List<Post> posts = new ArrayList<>();
        posts.add(post(Source.STOCKTWITS, -0.8, 0, 100_000));
        for (int i = 0; i < 10; i++) {
            posts.add(post(Source.STOCKTWITS, 0.5, 0, 0));
        }
        SentimentSummary summary = aggregator.summarize(posts, NOW);
        // The viral post weighs ~6x a normal one, not 100,000x.
        assertTrue(summary.score > 0);
        assertTrue(SentimentAggregator.engagementWeight(posts.get(0)) < 7);
    }

    @Test
    public void recentPostsOutweighOldOnes() {
        List<Post> posts = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            posts.add(post(Source.NEWS, -0.5, 24 * 6, 0));
            posts.add(post(Source.NEWS, 0.5, 1, 0));
        }
        assertTrue(aggregator.summarize(posts, NOW).score > 0.2);
        assertEquals(0.5, SentimentAggregator.weight(post(Source.NEWS, 0, 48, 0), NOW), 0.0001);
    }

    @Test
    public void breaksDownBySourceAndDay() {
        List<Post> posts = new ArrayList<>();
        posts.add(post(Source.NEWS, 0.4, 1, 0));
        posts.add(post(Source.NEWS, 0.2, 2, 0));
        posts.add(post(Source.REDDIT, -0.6, 30, 0));
        posts.add(post(Source.REDDIT, 0.0, 24 * 30, 0)); // Outside the 7 day trend window.
        SentimentSummary summary = aggregator.summarize(posts, NOW);

        assertEquals(2, summary.bySource.get(Source.NEWS).count);
        assertEquals(0.3, summary.bySource.get(Source.NEWS).mean, 0.0001);
        assertEquals(-0.3, summary.bySource.get(Source.REDDIT).mean, 0.0001);
        assertEquals(2, summary.positiveCount);
        assertEquals(1, summary.neutralCount);
        assertEquals(1, summary.negativeCount);

        assertEquals(2, summary.trend.size());
        assertEquals(-0.6, summary.trend.get(0).mean, 0.0001);
        assertEquals(0.3, summary.trend.get(1).mean, 0.0001);
        assertEquals(2, summary.trend.get(1).count);
    }

    @Test
    public void labelsUseSymmetricBands() {
        assertEquals(SentimentSummary.Label.NEUTRAL, SentimentAggregator.label(0.049));
        assertEquals(SentimentSummary.Label.SLIGHTLY_NEGATIVE, SentimentAggregator.label(-0.05));
        assertEquals(SentimentSummary.Label.POSITIVE, SentimentAggregator.label(0.2));
        assertEquals(SentimentSummary.Label.VERY_NEGATIVE, SentimentAggregator.label(-0.5));
    }
}
