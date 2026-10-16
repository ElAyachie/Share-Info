package com.example.shareinfo.data.sources;

import com.example.shareinfo.data.Http;
import com.example.shareinfo.data.Post;
import com.example.shareinfo.data.Source;
import com.example.shareinfo.data.Topic;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Google News search over the past week, via its public RSS feed. Replaces Mediastack, whose
 * free plan only searched a single day and required an API key.
 */
public class GoogleNewsSource implements PostSource {
    private static final int MAX_ARTICLES = 40;

    @Override
    public Source source() {
        return Source.NEWS;
    }

    @Override
    public boolean supports(Topic topic) {
        return true;
    }

    @Override
    public List<Post> fetch(Topic topic) throws IOException {
        String url = "https://news.google.com/rss/search?q=" + Http.encode(topic.newsQuery() + " when:7d")
                + "&hl=en-US&gl=US&ceid=US:en";
        return parse(Http.get(url));
    }

    static List<Post> parse(String xml) throws IOException {
        Document document = Xml.parse(xml);
        NodeList items = document.getElementsByTagName("item");
        List<Post> posts = new ArrayList<>();
        for (int i = 0; i < items.getLength() && posts.size() < MAX_ARTICLES; i++) {
            Element item = (Element) items.item(i);
            String publisher = Xml.childText(item, "source");
            String title = Xml.childText(item, "title");
            // Titles come back as "Headline - Publisher"; the publisher is shown separately.
            String suffix = " - " + publisher;
            if (!publisher.isEmpty() && title.endsWith(suffix)) {
                title = title.substring(0, title.length() - suffix.length());
            }
            if (title.isEmpty()) {
                continue;
            }
            String link = Xml.childText(item, "link");
            posts.add(new Post(Source.NEWS, Xml.childText(item, "guid"),
                    publisher.isEmpty() ? "News" : publisher,
                    Text.decodeEntities(title),
                    link.isEmpty() ? null : link,
                    Text.parseRfc1123(Xml.childText(item, "pubDate")),
                    0, 0, 0, 0));
        }
        return posts;
    }
}
