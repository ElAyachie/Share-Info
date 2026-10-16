package com.example.shareinfo.data;

/** Where a post came from. */
public enum Source {
    STOCKTWITS("StockTwits"),
    BLUESKY("Bluesky"),
    REDDIT("Reddit"),
    YOUTUBE("YouTube"),
    NEWS("News");

    public final String displayName;

    Source(String displayName) {
        this.displayName = displayName;
    }
}
