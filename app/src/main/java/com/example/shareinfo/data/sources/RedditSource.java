package com.example.shareinfo.data.sources;

import com.example.shareinfo.data.Http;
import com.example.shareinfo.data.HttpException;
import com.example.shareinfo.data.Post;
import com.example.shareinfo.data.Source;
import com.example.shareinfo.data.Topic;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Reddit search via its public Atom feed. Replaces SocialGrep, which shut down. Reddit's JSON
 * API now requires OAuth, and the feed allows one request per minute per IP address, so this
 * source is best effort: the repository reports a failure here without failing the whole report.
 */
public class RedditSource implements PostSource {
    private static final int MAX_BODY_CHARS = 400;
    // Reddit answers 429 to every further request within a minute of the last one.
    private static final long MIN_INTERVAL_MILLIS = 60_000;
    private static final Object LOCK = new Object();
    private static long nextRequestAtMillis;
    private static final Pattern SUBMITTED_BY = Pattern.compile("\\s*submitted by .*$", Pattern.DOTALL);

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
        String url = "https://www.reddit.com/search.rss?q=" + Http.encode(query) + "&sort=new&t=week&limit=50";
        synchronized (LOCK) {
            long now = System.currentTimeMillis();
            // Fail fast rather than send a request Reddit is certain to refuse.
            if (now < nextRequestAtMillis) {
                throw new HttpException(429, "www.reddit.com");
            }
            nextRequestAtMillis = now + MIN_INTERVAL_MILLIS;
        }
        return parse(Http.get(url));
    }

    static List<Post> parse(String xml) throws IOException {
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
