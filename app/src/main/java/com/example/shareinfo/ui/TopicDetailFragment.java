package com.example.shareinfo.ui;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.shareinfo.R;
import com.example.shareinfo.data.Topic;
import com.example.shareinfo.data.TopicRepository;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;

/** A single topic opened from search or the following list. */
public class TopicDetailFragment extends TopicFeedFragment {
    private static final String ARG_TITLE = "title";
    private static final String ARG_TOPIC = "topic";

    static void open(Fragment from, Topic topic) {
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, topic.label());
        args.putString(ARG_TOPIC, topic.toJson().toString());
        NavHostFragment.findNavController(from).navigate(R.id.topic, args);
    }

    @Override
    protected boolean showTopicChips() {
        return false;
    }

    @Override
    protected void loadTopics(boolean forceRefresh, TopicRepository.Callback<List<Topic>> callback) {
        try {
            Topic topic = Topic.fromJson(new JSONObject(requireArguments().getString(ARG_TOPIC, "{}")));
            callback.onResult(Collections.singletonList(topic));
        } catch (JSONException e) {
            callback.onError(e);
        }
    }

    @Override
    protected String emptyTopicsMessage() {
        return getString(R.string.no_posts);
    }
}
