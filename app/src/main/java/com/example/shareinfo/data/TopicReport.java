package com.example.shareinfo.data;

import com.example.shareinfo.data.market.MarketSnapshot;
import com.example.shareinfo.sentiment.SentimentSummary;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Everything shown for one topic: scored posts, the overall sentiment and the price. */
public class TopicReport {
    public final Topic topic;
    /** Scored posts, newest first. */
    public final List<Post> posts;
    public final SentimentSummary summary;
    /** Null for topics without a ticker or when price data could not be loaded. */
    public final MarketSnapshot market;
    /** Why price data is missing for a ticker, or null. */
    public final String marketError;
    /** Sources that failed, with a short reason. A report can be useful with some missing. */
    public final Map<Source, String> failedSources;
    public final long fetchedAtMillis;

    public TopicReport(Topic topic, List<Post> posts, SentimentSummary summary, MarketSnapshot market,
                       String marketError, Map<Source, String> failedSources, long fetchedAtMillis) {
        this.topic = topic;
        this.posts = Collections.unmodifiableList(posts);
        this.summary = summary;
        this.market = market;
        this.marketError = marketError;
        this.failedSources = Collections.unmodifiableMap(failedSources);
        this.fetchedAtMillis = fetchedAtMillis;
    }
}
