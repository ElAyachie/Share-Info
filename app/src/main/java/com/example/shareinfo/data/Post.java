package com.example.shareinfo.data;

/** A single social media post or news headline about a topic. */
public class Post {
    public final Source source;
    public final String id;
    /** User handle for social posts, publisher for news. */
    public final String author;
    public final String text;
    /** Link to the original post or article; may be null. */
    public final String url;
    public final long createdAtMillis;
    public final int likes;
    public final int replies;
    public final int reposts;
    /** The author's own tag where the source supports one: +1 bullish, -1 bearish, 0 none. */
    public final int authorLabel;

    /** Filled in by the repository once the post has been scored (-1..1). */
    public double sentiment;

    public Post(Source source, String id, String author, String text, String url, long createdAtMillis,
                int likes, int replies, int reposts, int authorLabel) {
        this.source = source;
        this.id = id;
        this.author = author;
        this.text = text;
        this.url = url;
        this.createdAtMillis = createdAtMillis;
        this.likes = likes;
        this.replies = replies;
        this.reposts = reposts;
        this.authorLabel = authorLabel;
    }

    public int engagement() {
        return likes + replies + reposts;
    }
}
