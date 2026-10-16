package com.example.shareinfo.ui;

import com.example.shareinfo.R;
import com.example.shareinfo.data.Topic;
import com.example.shareinfo.data.TopicRepository;

import java.util.List;

/** Topics with the most social activity right now. */
public class TrendingFragment extends TopicFeedFragment {
    @Override
    protected void loadTopics(boolean forceRefresh, TopicRepository.Callback<List<Topic>> callback) {
        repository.loadTrending(forceRefresh, callback);
    }

    @Override
    protected String emptyTopicsMessage() {
        return getString(R.string.no_trending);
    }
}
