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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Reddit search. With a client ID from reddit.com/prefs/apps it uses Reddit's OAuth API, which
 * allows far more requests, ranks results by relevance and reports scores and comment counts.
 * Both app types work: an "installed app" needs only its ID; a "personal use script" also needs
 * its secret. Without an ID it falls back to the public search feed, which allows one request per
 * minute per IP address, sorts by date and has no engagement numbers.
 */
public class RedditSource implements PostSource {
    private static final int MAX_BODY_CHARS = 400;
    private static final Pattern SUBMITTED_BY = Pattern.compile("\\s*submitted by .*$", Pattern.DOTALL);

    // Installed apps have no secret; Reddit documents this device ID for apps that don't track devices.
    private static final String INSTALLED_TOKEN_FORM = "grant_type="
            + Http.encode("https://oauth.reddit.com/grants/installed_client") + "&device_id=DO_NOT_TRACK_THIS_DEVICE";
    // Script and web apps authenticate as themselves with their secret.
    private static final String SCRIPT_TOKEN_FORM = "grant_type=client_credentials";
    // Renew tokens a little before Reddit expires them.
    private static final long TOKEN_MARGIN_MILLIS = 60_000;

    // The public feed answers 429 to every further request within a minute of the last one.
    private static final long FEED_INTERVAL_MILLIS = 60_000;
    private static final Object FEED_LOCK = new Object();
    private static long nextFeedRequestAtMillis;

    private final String clientId;
    private final String clientSecret;
    private String accessToken;
    private long accessTokenExpiresAtMillis;

    /**
     * @param clientId     Reddit app client ID, or empty to use the public feed.
     * @param clientSecret the secret of a "personal use script" app; empty for an installed app.
     */
    public RedditSource(String clientId, String clientSecret) {
        this.clientId = clientId == null ? "" : clientId.trim();
        this.clientSecret = clientSecret == null ? "" : clientSecret.trim();
    }

    @Override
    public Source source() {
        return Source.REDDIT;
    }

    @Override
    public boolean supports(Topic topic) {
        return true;
    }

    @Override
    public List<Post> fetch(Topic topic) throws IOException {
        // A bare ticker is ambiguous on Reddit ("SLS" is also a NASA rocket), so ask for the stock.
        String query = topic.type == Topic.Type.STOCK || topic.type == Topic.Type.FUND
                ? topic.symbol + " stock" : topic.name;
        return clientId.isEmpty() ? fetchFeed(query) : fetchApi(query);
    }

    private List<Post> fetchApi(String query) throws IOException {
        // Relevance within the week: sorting by new mostly returns fresh posts nobody has read yet.
        String url = "https://oauth.reddit.com/search?q=" + Http.encode(query)
                + "&sort=relevance&t=week&type=link&limit=50&raw_json=1";
        String body;
        try {
            body = Http.get(url, Collections.singletonMap("Authorization", "bearer " + accessToken(false)));
        } catch (HttpException e) {
            if (e.code != 401) {
                throw e;
            }
            // The token was revoked or expired early; get a new one and try once more.
            body = Http.get(url, Collections.singletonMap("Authorization", "bearer " + accessToken(true)));
        }
        try {
            return parseListing(body);
        } catch (JSONException e) {
            throw new IOException("Unexpected Reddit response", e);
        }
    }

    private synchronized String accessToken(boolean renew) throws IOException {
        long now = System.currentTimeMillis();
        if (!renew && accessToken != null && now < accessTokenExpiresAtMillis) {
            return accessToken;
        }
        String credentials = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
        String body;
        try {
            body = Http.postForm("https://www.reddit.com/api/v1/access_token",
                    Collections.singletonMap("Authorization", "Basic " + credentials),
                    clientSecret.isEmpty() ? INSTALLED_TOKEN_FORM : SCRIPT_TOKEN_FORM);
        } catch (HttpException e) {
            if (e.code == 401 || e.code == 403) {
                throw new SourceException("client ID or secret was rejected", e);
            }
            throw e;
        }
        try {
            JSONObject json = new JSONObject(body);
            String token = Json.string(json, "access_token", "");
            if (token.isEmpty()) {
                // Reddit reports bad credentials as 200 with {"error": ...}.
                throw new SourceException("client ID or secret was rejected");
            }
            accessToken = token;
            accessTokenExpiresAtMillis = now + json.optLong("expires_in", 3600) * 1000 - TOKEN_MARGIN_MILLIS;
            return accessToken;
        } catch (JSONException e) {
            throw new IOException("Unexpected Reddit token response", e);
        }
    }

    private List<Post> fetchFeed(String query) throws IOException {
        String url = "https://www.reddit.com/search.rss?q=" + Http.encode(query) + "&sort=new&t=week&limit=50";
        synchronized (FEED_LOCK) {
            long now = System.currentTimeMillis();
            // Fail fast rather than send a request Reddit is certain to refuse.
            if (now < nextFeedRequestAtMillis) {
                throw new HttpException(429, "www.reddit.com");
            }
            nextFeedRequestAtMillis = now + FEED_INTERVAL_MILLIS;
        }
        return parseFeed(Http.get(url));
    }

    /** Parses an OAuth API search listing. */
    static List<Post> parseListing(String body) throws JSONException {
        JSONArray children = new JSONObject(body).getJSONObject("data").getJSONArray("children");
        List<Post> posts = new ArrayList<>();
        for (int i = 0; i < children.length(); i++) {
            JSONObject child = children.getJSONObject(i);
            JSONObject data = child.optJSONObject("data");
            if (data == null || !"t3".equals(Json.string(child, "kind", "")) || data.optBoolean("over_18")) {
                continue;
            }
            String title = Json.string(data, "title", "");
            String text = Json.string(data, "selftext", "").trim();
            if (text.length() > MAX_BODY_CHARS) {
                text = text.substring(0, MAX_BODY_CHARS) + "…";
            }
            String subreddit = Json.string(data, "subreddit_name_prefixed", "");
            String user = "/u/" + Json.string(data, "author", "[deleted]");
            String permalink = Json.string(data, "permalink", "");
            posts.add(new Post(Source.REDDIT, "t3_" + Json.string(data, "id", ""),
                    subreddit.startsWith("r/") ? user + " in " + subreddit : user,
                    text.isEmpty() ? title : title + "\n" + text,
                    permalink.isEmpty() ? null : "https://www.reddit.com" + permalink,
                    (long) (data.optDouble("created_utc", 0) * 1000),
                    // Scores can be negative; engagement counts interactions, not approval.
                    Math.max(0, data.optInt("score")),
                    data.optInt("num_comments"),
                    0, 0));
        }
        return posts;
    }

    /** Parses the public Atom search feed. */
    static List<Post> parseFeed(String xml) throws IOException {
        Document document = Xml.parse(xml);
        NodeList entries = document.getElementsByTagName("entry");
        List<Post> posts = new ArrayList<>();
        for (int i = 0; i < entries.getLength(); i++) {
            Element entry = (Element) entries.item(i);
            String id = Xml.childText(entry, "id");
            // Search also matches whole subreddits (t5_); only posts (t3_) are opinions.
            if (!id.startsWith("t3_")) {
                continue;
            }
            String title = Text.decodeEntities(Xml.childText(entry, "title"));
            String body = SUBMITTED_BY.matcher(Text.stripHtml(Xml.childText(entry, "content"))).replaceAll("");
            if (body.length() > MAX_BODY_CHARS) {
                body = body.substring(0, MAX_BODY_CHARS) + "…";
            }
            Element author = Xml.child(entry, "author");
            Element link = Xml.child(entry, "link");
            Element category = Xml.child(entry, "category");
            String subreddit = category != null ? category.getAttribute("label") : "";
            String user = author != null ? Xml.childText(author, "name") : "";

            String published = Xml.childText(entry, "published");
            posts.add(new Post(Source.REDDIT, id,
                    subreddit.startsWith("r/") ? user + " in " + subreddit : user,
                    body.isEmpty() ? title : title + "\n" + body,
                    link != null ? link.getAttribute("href") : null,
                    Text.parseIso(published.isEmpty() ? Xml.childText(entry, "updated") : published),
                    0, 0, 0, 0));
        }
        return posts;
    }
}
