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
        List<Post> posts = RedditSource.parseFeed(Fixtures.read("fixtures/reddit_nvda.xml"));
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
    public void parsesRedditApiListingWithEngagement() throws Exception {
        List<Post> posts = RedditSource.parseListing(Fixtures.read("fixtures/reddit_api_search.json"));
        // The NSFW post and the subreddit result are skipped.
        assertEquals(2, posts.size());
        Post first = posts.get(0);
        assertEquals("t3_1x9abc", first.id);
        assertEquals("/u/tcg_fan in r/PokemonTCG", first.author);
        assertTrue(first.text.startsWith("Pulled a Charizard ex"));
        assertTrue(first.text.contains("\nThird box this week"));
        assertEquals("https://www.reddit.com/r/PokemonTCG/comments/1x9abc/pulled_a_charizard/", first.url);
        assertEquals(1792140000000L, first.createdAtMillis);
        assertEquals(412, first.likes);
        assertEquals(57, first.replies);
        // A downvoted post still counts as engagement of zero, not negative.
        assertEquals(0, posts.get(1).likes);
    }

    @Test
    public void parsesYouTubeVideosStatisticsAndComments() throws Exception {
        List<Post> videos = YouTubeSource.parseSearch(Fixtures.read("fixtures/youtube_search.json"));
        // The channel result is skipped.
        assertEquals(2, videos.size());
        Post video = videos.get(0);
        assertEquals("vid00000001", video.id);
        assertEquals("Card Market Weekly", video.author);
        assertTrue(video.text.startsWith("Is the Pokémon card market about to crash? Here's what I think"));
        assertTrue(video.text.contains("Prices are way up this year & collectors"));
        assertEquals("https://www.youtube.com/watch?v=vid00000001", video.url);
        assertTrue(video.createdAtMillis > 0);

        java.util.Map<String, int[]> stats = YouTubeSource.parseStatistics(Fixtures.read("fixtures/youtube_videos.json"));
        assertEquals(3400, stats.get("vid00000001")[0]);
        assertEquals(512, stats.get("vid00000001")[1]);
        // Hidden like counts read as zero.
        assertEquals(0, stats.get("vid00000002")[0]);

        List<Post> comments = YouTubeSource.parseComments(Fixtures.read("fixtures/youtube_comments.json"), "vid00000001");
        assertEquals(2, comments.size());
        Post comment = comments.get(0);
        assertEquals(Source.YOUTUBE, comment.source);
        assertEquals("@collector99", comment.author);
        assertEquals(88, comment.likes);
        assertEquals(4, comment.replies);
        assertEquals("https://www.youtube.com/watch?v=vid00000001&lc=Ugx1", comment.url);
    }

    @Test
    public void youTubeIsLeftOutWithoutAKey() {
        assertFalse(new YouTubeSource("").supports(Topic.custom("pokemon cards")));
        assertFalse(new YouTubeSource(null).supports(Topic.custom("pokemon cards")));
        assertTrue(new YouTubeSource("key").supports(Topic.stock("AAPL", "Apple Inc.")));
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
