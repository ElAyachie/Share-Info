package com.example.shareinfo.data.sources;

import com.example.shareinfo.data.Http;
import com.example.shareinfo.data.HttpException;
import com.example.shareinfo.data.Json;
import com.example.shareinfo.data.Post;
import com.example.shareinfo.data.Source;
import com.example.shareinfo.data.Topic;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bluesky post search. Replaces the Twitter integration: the X API no longer has a free tier,
 * while Bluesky's AppView search is public and needs no key.
 */
public class BlueskySource implements PostSource {
    // The documented public host sometimes rejects requests that api.bsky.app serves.
    private static final String[] HOSTS = {"https://api.bsky.app", "https://public.api.bsky.app"};
    private static final int LIMIT = 50;
    // Price bots post every few minutes; keep a few of their posts so they cannot dominate.
    private static final int MAX_POSTS_PER_AUTHOR = 2;
    private static final int MAX_TAGS_PER_POST = 5;

    @Override
    public Source source() {
        return Source.BLUESKY;
    }

    @Override
    public boolean supports(Topic topic) {
        return true;
    }

    @Override
    public List<Post> fetch(Topic topic) throws IOException {
        String path = "/xrpc/app.bsky.feed.searchPosts?q=" + Http.encode(topic.socialQuery())
                + "&sort=latest&lang=en&limit=" + LIMIT;
        IOException lastError = null;
        for (String host : HOSTS) {
            try {
                return parse(Http.get(host + path));
            } catch (HttpException e) {
                lastError = e;
            } catch (JSONException e) {
                lastError = new IOException("Unexpected Bluesky response", e);
            }
        }
        throw lastError;
    }

    static List<Post> parse(String body) throws JSONException {
        JSONArray results = new JSONObject(body).getJSONArray("posts");
        List<Post> posts = new ArrayList<>();
        Map<String, Integer> postsPerAuthor = new HashMap<>();
        for (int i = 0; i < results.length(); i++) {
            JSONObject post = results.getJSONObject(i);
            JSONObject record = post.optJSONObject("record");
            JSONObject author = post.optJSONObject("author");
            if (record == null || author == null) {
                continue;
            }
            if (!isEnglish(record) || countTags(record) > MAX_TAGS_PER_POST) {
                continue;
            }
            String handle = Json.string(author, "handle", "");
            int authorCount = postsPerAuthor.getOrDefault(handle, 0);
            if (authorCount >= MAX_POSTS_PER_AUTHOR) {
                continue;
            }
            postsPerAuthor.put(handle, authorCount + 1);

            String uri = Json.string(post, "uri", "");
            String recordKey = uri.substring(uri.lastIndexOf('/') + 1);
            posts.add(new Post(Source.BLUESKY, uri, "@" + handle,
                    Json.string(record, "text", ""),
                    "https://bsky.app/profile/" + handle + "/post/" + recordKey,
                    Text.parseIso(Json.string(record, "createdAt", Json.string(post, "indexedAt", ""))),
                    post.optInt("likeCount"),
                    post.optInt("replyCount"),
                    post.optInt("repostCount") + post.optInt("quoteCount"),
                    0));
        }
        return posts;
    }

    // The lang=en query parameter is a hint; posts tagged with other languages still come back.
    private static boolean isEnglish(JSONObject record) {
        JSONArray langs = record.optJSONArray("langs");
        if (langs == null || langs.length() == 0) {
            return true;
        }
        for (int i = 0; i < langs.length(); i++) {
            if (langs.optString(i).toLowerCase().startsWith("en")) {
                return true;
            }
        }
        return false;
    }

    private static int countTags(JSONObject record) {
        JSONArray facets = record.optJSONArray("facets");
        if (facets == null) {
            return 0;
        }
        int tags = 0;
        for (int i = 0; i < facets.length(); i++) {
            JSONArray features = facets.optJSONObject(i) != null
                    ? facets.optJSONObject(i).optJSONArray("features") : null;
            if (features == null) {
                continue;
            }
            for (int j = 0; j < features.length(); j++) {
                JSONObject feature = features.optJSONObject(j);
                if (feature != null && feature.optString("$type").endsWith("#tag")) {
                    tags++;
                }
            }
        }
        return tags;
    }
}
