package com.example.shareinfo.data.sources;

import com.example.shareinfo.Fixtures;
import com.example.shareinfo.data.Post;
import com.example.shareinfo.data.Source;
import com.example.shareinfo.data.Topic;

import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Parses responses captured from each live API on 2026-10-16. */
public class SourceParserTest {

    @Test
    public void parsesStockTwitsWithAuthorTags() throws Exception {
        List<Post> posts = StockTwitsSource.parse(Fixtures.read("fixtures/stocktwits_aapl.json"));
        assertFalse(posts.isEmpty());
        int bullish = 0;
        int bearish = 0;
        for (Post post : posts) {
            assertEquals(Source.STOCKTWITS, post.source);
            assertTrue(post.url.startsWith("https://stocktwits.com/" + post.author + "/message/"));
            assertTrue(post.createdAtMillis > 0);
            assertFalse(post.text.contains("&amp;"));
            bullish += post.authorLabel > 0 ? 1 : 0;
            bearish += post.authorLabel < 0 ? 1 : 0;
        }
        // The fixture has 4 Bullish and 3 Bearish tags.
        assertEquals(4, bullish);
        assertEquals(3, bearish);
    }

    @Test
    public void usesCryptoSuffixForStockTwitsStreams() {
        assertEquals("BTC.X", StockTwitsSource.streamSymbol(Topic.crypto("BTC", "Bitcoin USD")));
        assertEquals("AAPL", StockTwitsSource.streamSymbol(Topic.stock("AAPL", "Apple Inc.")));
    }

    @Test
    public void parsesBlueskyAndLimitsEachAuthor() throws Exception {
        List<Post> posts = BlueskySource.parse(Fixtures.read("fixtures/bluesky_nvda.json"));
        assertFalse(posts.isEmpty());
        Map<String, Integer> perAuthor = new HashMap<>();
        for (Post post : posts) {
            assertTrue(post.author.startsWith("@"));
            assertTrue(post.url.matches("https://bsky\\.app/profile/[^/]+/post/[a-z0-9]+"));
            assertTrue(post.createdAtMillis > 0);
            perAuthor.merge(post.author, 1, Integer::sum);
        }
        for (int count : perAuthor.values()) {
            assertTrue(count <= 2);
        }
    }

    @Test
    public void parsesGoogleNewsAndSplitsOffThePublisher() throws Exception {
        List<Post> posts = GoogleNewsSource.parse(Fixtures.read("fixtures/google_news_nvda.xml"));
        assertEquals(10, posts.size());
        Post first = posts.get(0);
        assertEquals("Nvidia leads chip stocks lower as AI trade takes", first.text);
        assertEquals("Yahoo Finance", first.author);
        assertTrue(first.url.startsWith("https://news.google.com/"));
        assertTrue(first.createdAtMillis > 0);
    }

    @Test
    public void parsesRedditPostsAndSkipsSubreddits() throws Exception {
        List<Post> posts = RedditSource.parse(Fixtures.read("fixtures/reddit_nvda.xml"));
        // The feed has 10 entries; 3 are subreddits rather than posts.
        assertEquals(7, posts.size());
        for (Post post : posts) {
            assertTrue(post.id.startsWith("t3_"));
            assertTrue(post.author.startsWith("/u/"));
            assertNotNull(post.url);
            assertFalse(post.text.contains("submitted by"));
            assertTrue(post.createdAtMillis > 0);
        }
    }

    @Test
    public void decodesEntitiesAndDates() {
        assertEquals("AT&T's \"deal\" <now> é", Text.decodeEntities("AT&amp;T&#39;s &quot;deal&quot; &lt;now&gt; &#xe9;"));
        assertEquals("bold text", Text.stripHtml("<b>bold</b>&nbsp;<i>text</i>"));
        assertEquals(1792152000000L, Text.parseIso("2026-10-16T12:00:00Z"));
        assertEquals(1792152000000L, Text.parseIso("2026-10-16T12:00:00+00:00"));
        assertEquals(1792152000000L, Text.parseRfc1123("Fri, 16 Oct 2026 12:00:00 GMT"));
        assertEquals(0, Text.parseIso("not a date"));
    }
}
