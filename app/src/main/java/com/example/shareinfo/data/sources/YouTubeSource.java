package com.example.shareinfo.data.sources;

import com.example.shareinfo.data.Http;
import com.example.shareinfo.data.HttpException;
import com.example.shareinfo.data.Json;
import com.example.shareinfo.data.Post;
import com.example.shareinfo.data.Source;
import com.example.shareinfo.data.SourceException;
import com.example.shareinfo.data.Topic;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * YouTube Data API v3: recent videos about a topic, and the top comments on the most relevant
 * ones. Video titles say what creators think; comments are where viewers argue about it, which
 * matters most for collectibles and other topics with no ticker.
 *
 * Quota: a search costs 100 of the free 10,000 daily units and every other call costs 1, so a
 * report costs about 106 units, roughly 90 fresh reports a day per key.
 */
public class YouTubeSource implements PostSource {
    private static final String API = "https://www.googleapis.com/youtube/v3/";
    private static final int VIDEOS = 10;
    private static final int VIDEOS_WITH_COMMENTS = 5;
    private static final int COMMENTS_PER_VIDEO = 20;
    private static final int MAX_DESCRIPTION_CHARS = 200;

    private final String apiKey;

    /** @param apiKey YouTube Data API key, or empty to leave YouTube out of reports. */
    public YouTubeSource(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    @Override
    public Source source() {
        return Source.YOUTUBE;
    }

    @Override
    public boolean supports(Topic topic) {
        return !apiKey.isEmpty();
    }

    @Override
    public List<Post> fetch(Topic topic) throws IOException {
        String publishedAfter = Instant.now().minus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();
        String searchUrl = API + "search?part=snippet&type=video&order=relevance&relevanceLanguage=en"
                + "&maxResults=" + VIDEOS + "&publishedAfter=" + Http.encode(publishedAfter)
                + "&q=" + Http.encode(topic.newsQuery()) + "&key=" + Http.encode(apiKey);
        try {
            List<Post> videos = parseSearch(call(searchUrl));
            if (videos.isEmpty()) {
                return videos;
            }
            List<Post> posts = new ArrayList<>(withStatistics(videos));
            for (int i = 0; i < Math.min(VIDEOS_WITH_COMMENTS, videos.size()); i++) {
                String videoId = videos.get(i).id;
                String commentsUrl = API + "commentThreads?part=snippet&order=relevance&textFormat=plainText"
                        + "&maxResults=" + COMMENTS_PER_VIDEO + "&videoId=" + Http.encode(videoId)
                        + "&key=" + Http.encode(apiKey);
                try {
                    posts.addAll(parseComments(call(commentsUrl), videoId));
                } catch (HttpException e) {
                    // Videos with comments turned off answer 403; skip them and keep the rest.
                    if (!e.body.contains("commentsDisabled")) {
                        throw e;
                    }
                }
            }
            return posts;
        } catch (JSONException e) {
            throw new IOException("Unexpected YouTube response", e);
        }
    }

    /** Adds like and comment counts to the videos; one call covers them all. */
    private List<Post> withStatistics(List<Post> videos) throws IOException, JSONException {
        StringBuilder ids = new StringBuilder();
        for (Post video : videos) {
            if (ids.length() > 0) {
                ids.append(',');
            }
            ids.append(video.id);
        }
        Map<String, int[]> stats = parseStatistics(call(API + "videos?part=statistics&id=" + Http.encode(ids.toString())
                + "&key=" + Http.encode(apiKey)));
        List<Post> counted = new ArrayList<>();
        for (Post video : videos) {
            int[] counts = stats.get(video.id);
            counted.add(counts == null ? video : new Post(video.source, video.id, video.author, video.text, video.url,
                    video.createdAtMillis, counts[0], counts[1], 0, 0));
        }
        return counted;
    }

    /** Turns YouTube's error reasons into messages a user can act on. */
    private static String call(String url) throws IOException {
        try {
            return Http.get(url);
        } catch (HttpException e) {
            if (e.body.contains("quotaExceeded") || e.body.contains("dailyLimitExceeded")) {
                throw new SourceException("daily quota reached, back tomorrow", e);
            }
            if (e.body.contains("API_KEY_INVALID") || e.body.contains("keyInvalid")) {
                throw new SourceException("API key was rejected", e);
            }
            if (e.body.contains("accessNotConfigured") || e.body.contains("SERVICE_DISABLED")) {
                throw new SourceException("YouTube Data API is not enabled for this key", e);
            }
            throw e;
        }
    }

    static List<Post> parseSearch(String body) throws JSONException {
        JSONArray items = new JSONObject(body).optJSONArray("items");
        List<Post> videos = new ArrayList<>();
        if (items == null) {
            return videos;
        }
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            JSONObject id = item.optJSONObject("id");
            JSONObject snippet = item.optJSONObject("snippet");
            String videoId = Json.string(id, "videoId", "");
            if (videoId.isEmpty() || snippet == null) {
                continue;
            }
            // Search snippets are HTML-escaped ("&#39;").
            String title = Text.decodeEntities(Json.string(snippet, "title", ""));
            String description = Text.decodeEntities(Json.string(snippet, "description", "")).trim();
            if (description.length() > MAX_DESCRIPTION_CHARS) {
                description = description.substring(0, MAX_DESCRIPTION_CHARS) + "…";
            }
            videos.add(new Post(Source.YOUTUBE, videoId, Json.string(snippet, "channelTitle", "YouTube"),
                    description.isEmpty() ? title : title + "\n" + description,
                    "https://www.youtube.com/watch?v=" + videoId,
                    Text.parseIso(Json.string(snippet, "publishedAt", "")),
                    0, 0, 0, 0));
        }
        return videos;
    }

    /** Video id to {likes, comments}. */
    static Map<String, int[]> parseStatistics(String body) throws JSONException {
        Map<String, int[]> stats = new HashMap<>();
        JSONArray items = new JSONObject(body).optJSONArray("items");
        if (items == null) {
            return stats;
        }
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            JSONObject statistics = item.optJSONObject("statistics");
            if (statistics != null) {
                // Counts arrive as strings and are absent when a channel hides them.
                stats.put(Json.string(item, "id", ""), new int[]{
                        parseCount(Json.string(statistics, "likeCount", "0")),
                        parseCount(Json.string(statistics, "commentCount", "0"))});
            }
        }
        return stats;
    }

    static List<Post> parseComments(String body, String videoId) throws JSONException {
        JSONArray items = new JSONObject(body).optJSONArray("items");
        List<Post> comments = new ArrayList<>();
        if (items == null) {
            return comments;
        }
        for (int i = 0; i < items.length(); i++) {
            JSONObject thread = items.getJSONObject(i);
            JSONObject threadSnippet = thread.optJSONObject("snippet");
            JSONObject top = threadSnippet != null ? threadSnippet.optJSONObject("topLevelComment") : null;
            JSONObject snippet = top != null ? top.optJSONObject("snippet") : null;
            if (snippet == null) {
                continue;
            }
            String commentId = Json.string(top, "id", Json.string(thread, "id", ""));
            comments.add(new Post(Source.YOUTUBE, commentId,
                    Json.string(snippet, "authorDisplayName", "YouTube viewer"),
                    Json.string(snippet, "textOriginal", Json.string(snippet, "textDisplay", "")),
                    "https://www.youtube.com/watch?v=" + videoId + "&lc=" + commentId,
                    Text.parseIso(Json.string(snippet, "publishedAt", "")),
                    snippet.optInt("likeCount"),
                    threadSnippet.optInt("totalReplyCount"),
                    0, 0));
        }
        return comments;
    }

    private static int parseCount(String value) {
        try {
            return (int) Math.min(Integer.MAX_VALUE, Long.parseLong(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
