package com.example.shareinfo.data;

import com.example.shareinfo.data.market.MarketSnapshot;
import com.example.shareinfo.data.market.YahooFinance;
import com.example.shareinfo.data.sources.PostSource;
import com.example.shareinfo.sentiment.MarketSentiment;
import com.example.shareinfo.sentiment.SentimentAggregator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Fetches every source for a topic in parallel, scores the posts and aggregates them. A source
 * that fails is recorded in {@link TopicReport#failedSources} instead of failing the report.
 */
public class ReportBuilder {
    private static final long SOURCE_TIMEOUT_SECONDS = 25;
    private static final int MIN_TEXT_LENGTH = 12;

    private final List<PostSource> sources;
    private final YahooFinance yahoo;
    private final MarketSentiment sentiment;
    private final SentimentAggregator aggregator;
    private final ExecutorService executor;

    public ReportBuilder(List<PostSource> sources, YahooFinance yahoo, MarketSentiment sentiment,
                         SentimentAggregator aggregator, ExecutorService executor) {
        this.sources = sources;
        this.yahoo = yahoo;
        this.sentiment = sentiment;
        this.aggregator = aggregator;
        this.executor = executor;
    }

    /** Blocks until every source has answered or timed out. Call from a background thread. */
    public TopicReport build(Topic topic, long nowMillis) throws IOException {
        Future<MarketSnapshot> marketFuture = null;
        if (yahoo != null && topic.marketSymbol != null) {
            marketFuture = executor.submit(() -> yahoo.chart(topic.marketSymbol));
        }
        Map<PostSource, Future<List<Post>>> futures = new LinkedHashMap<>();
        for (PostSource source : sources) {
            if (source.supports(topic)) {
                futures.put(source, executor.submit(() -> source.fetch(topic)));
            }
        }

        List<Post> posts = new ArrayList<>();
        Map<Source, String> failed = new EnumMap<>(Source.class);
        Set<String> seenTexts = new HashSet<>();
        List<String> ignoreTerms = topic.ignoreTerms();
        for (Map.Entry<PostSource, Future<List<Post>>> entry : futures.entrySet()) {
            try {
                for (Post post : entry.getValue().get(SOURCE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    String text = post.text == null ? "" : post.text.trim();
                    // The same headline is syndicated by many outlets, and bots repost themselves.
                    if (text.length() < MIN_TEXT_LENGTH || !seenTexts.add(dedupeKey(text))) {
                        continue;
                    }
                    post.sentiment = sentiment.scorePost(text, post.authorLabel, ignoreTerms);
                    posts.add(post);
                }
            } catch (ExecutionException e) {
                failed.put(entry.getKey().source(), describe(e.getCause()));
            } catch (TimeoutException e) {
                entry.getValue().cancel(true);
                failed.put(entry.getKey().source(), "timed out");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted", e);
            }
        }

        MarketSnapshot market = null;
        String marketError = null;
        if (marketFuture != null) {
            // Prices are context; the sentiment report is still useful without them.
            try {
                market = marketFuture.get(SOURCE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                marketError = describe(e.getCause());
            } catch (TimeoutException e) {
                marketFuture.cancel(true);
                marketError = "timed out";
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted", e);
            }
        }

        if (posts.isEmpty() && !futures.isEmpty() && failed.size() == futures.size()) {
            throw new IOException("Could not reach any source. Check your connection and try again.");
        }
        posts.sort((a, b) -> Long.compare(b.createdAtMillis, a.createdAtMillis));
        return new TopicReport(topic, posts, aggregator.summarize(posts, nowMillis), market, marketError,
                failed, nowMillis);
    }

    static String dedupeKey(String text) {
        String normalized = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
        if (normalized.isEmpty()) {
            return text;
        }
        return normalized.length() > 80 ? normalized.substring(0, 80) : normalized;
    }

    static String describe(Throwable error) {
        if (error instanceof SourceException) {
            return error.getMessage();
        }
        if (error instanceof HttpException) {
            int code = ((HttpException) error).code;
            if (code == 429) {
                return "rate limited, try again later";
            }
            if (code == 401 || code == 403) {
                return "blocked the request";
            }
            return "server error " + code;
        }
        if (error instanceof java.net.UnknownHostException) {
            return "offline";
        }
        if (error instanceof java.net.SocketTimeoutException) {
            return "timed out";
        }
        return "unavailable";
    }
}
