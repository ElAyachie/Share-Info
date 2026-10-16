package com.example.shareinfo.data;

import com.example.shareinfo.Fixtures;
import com.example.shareinfo.data.sources.PostSource;
import com.example.shareinfo.sentiment.SentimentAggregator;

import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ReportBuilderTest {
    private static final long NOW = 1792152000000L;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @After
    public void tearDown() {
        executor.shutdownNow();
    }

    private ReportBuilder builder(PostSource... sources) throws IOException {
        return new ReportBuilder(Arrays.asList(sources), null, Fixtures.marketSentiment(),
                new SentimentAggregator(ZoneOffset.UTC), executor);
    }

    private static Post post(Source source, String text, long createdAt) {
        return new Post(source, text, "author", text, null, createdAt, 0, 0, 0, 0);
    }

    @Test
    public void scoresDedupesAndSortsPosts() throws Exception {
        PostSource news = new FakeSource(Source.NEWS, Arrays.asList(
                post(Source.NEWS, "Shares soar to a record high after blowout quarter", NOW - 5000),
                post(Source.NEWS, "Shares soar to a record high after blowout quarter!", NOW - 4000),
                post(Source.NEWS, "short", NOW)));
        PostSource social = new FakeSource(Source.BLUESKY, Collections.singletonList(
                post(Source.BLUESKY, "Terrible guidance, I'm selling everything", NOW - 1000)));

        TopicReport report = builder(news, social).build(Topic.stock("ACME", "Acme Corp"), NOW);

        // The syndicated duplicate and the too-short post are dropped.
        assertEquals(2, report.posts.size());
        assertEquals(Source.BLUESKY, report.posts.get(0).source);
        assertTrue(report.posts.get(0).sentiment < 0);
        assertTrue(report.posts.get(1).sentiment > 0);
        assertTrue(report.failedSources.isEmpty());
        assertNull(report.market);
        assertEquals(2, report.summary.totalCount());
    }

    @Test
    public void reportsFailedSourcesWithoutFailingTheReport() throws Exception {
        PostSource news = new FakeSource(Source.NEWS, Collections.singletonList(
                post(Source.NEWS, "Company posts strong growth this year", NOW)));
        PostSource reddit = new FailingSource(Source.REDDIT, new HttpException(429, "www.reddit.com"));

        TopicReport report = builder(news, reddit).build(Topic.custom("acme"), NOW);

        assertEquals(1, report.posts.size());
        assertEquals("rate limited, try again later", report.failedSources.get(Source.REDDIT));
    }

    @Test
    public void showsSourceMessagesAsWritten() throws Exception {
        PostSource news = new FakeSource(Source.NEWS, Collections.singletonList(
                post(Source.NEWS, "Company posts strong growth this year", NOW)));
        PostSource youtube = new FailingSource(Source.YOUTUBE, new SourceException("daily quota reached, back tomorrow"));

        TopicReport report = builder(news, youtube).build(Topic.custom("acme"), NOW);

        assertEquals("daily quota reached, back tomorrow", report.failedSources.get(Source.YOUTUBE));
    }

    @Test
    public void failsWhenEverySourceFails() throws Exception {
        try {
            builder(new FailingSource(Source.NEWS, new java.net.UnknownHostException("news.google.com")),
                    new FailingSource(Source.REDDIT, new HttpException(403, "www.reddit.com")))
                    .build(Topic.custom("acme"), NOW);
            fail("Expected an IOException");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("Check your connection"));
        }
    }

    @Test
    public void skipsSourcesThatDoNotSupportTheTopic() throws Exception {
        FakeSource marketOnly = new FakeSource(Source.STOCKTWITS, Collections.emptyList()) {
            @Override
            public boolean supports(Topic topic) {
                return topic.isMarket();
            }
        };
        FakeSource news = new FakeSource(Source.NEWS, Collections.singletonList(
                post(Source.NEWS, "Collectors say prices are climbing", NOW)));
        builder(marketOnly, news).build(Topic.custom("pokemon cards"), NOW);
        assertEquals(0, marketOnly.calls);
        assertEquals(1, news.calls);
    }

    private static class FakeSource implements PostSource {
        private final Source source;
        private final List<Post> posts;
        int calls;

        FakeSource(Source source, List<Post> posts) {
            this.source = source;
            this.posts = posts;
        }

        @Override
        public Source source() {
            return source;
        }

        @Override
        public boolean supports(Topic topic) {
            return true;
        }

        @Override
        public List<Post> fetch(Topic topic) {
            calls++;
            return posts;
        }
    }

    private static class FailingSource implements PostSource {
        private final Source source;
        private final IOException error;

        FailingSource(Source source, IOException error) {
            this.source = source;
            this.error = error;
        }

        @Override
        public Source source() {
            return source;
        }

        @Override
        public boolean supports(Topic topic) {
            return true;
        }

        @Override
        public List<Post> fetch(Topic topic) throws IOException {
            throw error;
        }
    }
}
