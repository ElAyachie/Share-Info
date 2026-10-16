package com.example.shareinfo.data;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Handler;
import android.os.Looper;

import com.example.shareinfo.data.market.TrendingTopics;
import com.example.shareinfo.data.market.YahooFinance;
import com.example.shareinfo.data.sources.BlueskySource;
import com.example.shareinfo.data.sources.GoogleNewsSource;
import com.example.shareinfo.data.sources.RedditSource;
import com.example.shareinfo.data.sources.StockTwitsSource;
import com.example.shareinfo.sentiment.Lexicons;
import com.example.shareinfo.sentiment.MarketSentiment;
import com.example.shareinfo.sentiment.SentimentAggregator;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Single entry point the screens use to load data. Work runs on background threads and results
 * are delivered on the main thread. Reports are cached in memory so switching tabs is instant;
 * pull to refresh bypasses the cache.
 */
public final class TopicRepository {
    private static final long REPORT_TTL_MILLIS = 10 * 60 * 1000;
    private static final long TRENDING_TTL_MILLIS = 15 * 60 * 1000;

    public interface Callback<T> {
        void onResult(T value);

        void onError(Exception error);
    }

    private static TopicRepository instance;

    private final AssetManager assets;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Handler mainThread = new Handler(Looper.getMainLooper());
    private final YahooFinance yahoo = new YahooFinance();
    private final TrendingTopics trendingTopics = new TrendingTopics(yahoo);
    private final Map<String, TopicReport> reports = new ConcurrentHashMap<>();
    private volatile List<Topic> trending;
    private volatile long trendingFetchedAt;
    private ReportBuilder reportBuilder;

    public static synchronized TopicRepository get(Context context) {
        if (instance == null) {
            instance = new TopicRepository(context.getApplicationContext());
        }
        return instance;
    }

    private TopicRepository(Context context) {
        this.assets = context.getAssets();
    }

    /** A cached report that is still fresh, or null. */
    public TopicReport cachedReport(Topic topic) {
        TopicReport report = reports.get(topic.id());
        if (report == null || System.currentTimeMillis() - report.fetchedAtMillis > REPORT_TTL_MILLIS) {
            return null;
        }
        return report;
    }

    public void loadReport(Topic topic, boolean forceRefresh, Callback<TopicReport> callback) {
        TopicReport cached = forceRefresh ? null : cachedReport(topic);
        if (cached != null) {
            callback.onResult(cached);
            return;
        }
        run(() -> {
            TopicReport report = reportBuilder().build(topic, System.currentTimeMillis());
            reports.put(topic.id(), report);
            return report;
        }, callback);
    }

    public void loadTrending(boolean forceRefresh, Callback<List<Topic>> callback) {
        List<Topic> cached = trending;
        if (!forceRefresh && cached != null && System.currentTimeMillis() - trendingFetchedAt < TRENDING_TTL_MILLIS) {
            callback.onResult(cached);
            return;
        }
        run(() -> {
            List<Topic> topics = trendingTopics.load();
            trending = topics;
            trendingFetchedAt = System.currentTimeMillis();
            return topics;
        }, callback);
    }

    public void search(String query, Callback<List<Topic>> callback) {
        run(() -> yahoo.search(query), callback);
    }

    private synchronized ReportBuilder reportBuilder() throws IOException {
        if (reportBuilder == null) {
            // Loading ~7,500 lexicon entries takes a moment, so it happens lazily off the UI thread.
            MarketSentiment sentiment;
            try (InputStream vader = assets.open(Lexicons.VADER_ASSET);
                 InputStream market = assets.open(Lexicons.MARKET_ASSET)) {
                sentiment = new MarketSentiment(Lexicons.read(vader), Lexicons.read(market));
            }
            reportBuilder = new ReportBuilder(
                    Arrays.asList(new StockTwitsSource(), new BlueskySource(), new RedditSource(), new GoogleNewsSource()),
                    yahoo, sentiment, new SentimentAggregator(ZoneId.systemDefault()), executor);
        }
        return reportBuilder;
    }

    private interface Task<T> {
        T run() throws Exception;
    }

    private <T> void run(Task<T> task, Callback<T> callback) {
        executor.execute(() -> {
            try {
                T result = task.run();
                mainThread.post(() -> callback.onResult(result));
            } catch (Exception e) {
                mainThread.post(() -> callback.onError(e));
            }
        });
    }
}
