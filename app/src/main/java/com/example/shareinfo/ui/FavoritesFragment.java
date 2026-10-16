package com.example.shareinfo.ui;

import com.example.shareinfo.MainActivity;
import com.example.shareinfo.R;
import com.example.shareinfo.data.Topic;
import com.example.shareinfo.data.TopicRepository;

import java.util.List;

/** The topics the user follows. */
public class FavoritesFragment extends TopicFeedFragment {
    private List<Topic> shown;

    @Override
    protected void loadTopics(boolean forceRefresh, TopicRepository.Callback<List<Topic>> callback) {
        shown = followStore.followed();
        callback.onResult(shown);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Follows may have changed on another tab.
        if (shown != null && !shown.equals(followStore.followed())) {
            refresh(false);
        }
    }

    @Override
    protected String emptyTopicsMessage() {
        return getString(R.string.no_favorites);
    }

    @Override
    protected String emptyActionLabel() {
        return getString(R.string.open_search);
    }

    @Override
    protected void onEmptyAction() {
        MainActivity.selectTab(requireActivity(), R.id.search);
    }
}
