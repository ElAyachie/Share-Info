package com.example.shareinfo.data.sources;

import com.example.shareinfo.data.Post;
import com.example.shareinfo.data.Source;
import com.example.shareinfo.data.Topic;

import java.io.IOException;
import java.util.List;

/** A place posts about a topic can be fetched from. Implementations block; call off the UI thread. */
public interface PostSource {
    Source source();

    boolean supports(Topic topic);

    List<Post> fetch(Topic topic) throws IOException;
}
