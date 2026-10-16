package com.example.shareinfo.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.shareinfo.R;
import com.example.shareinfo.data.FollowStore;
import com.example.shareinfo.data.Topic;
import com.example.shareinfo.data.TopicReport;
import com.example.shareinfo.data.TopicRepository;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows a row of topics and the sentiment report for the selected one. Subclasses decide which
 * topics: trending, followed, or a single topic opened from search.
 */
public abstract class TopicFeedFragment extends Fragment implements ReportAdapter.FollowHandler {
    private static final String STATE_SELECTED = "selectedTopic";

    protected TopicRepository repository;
    protected FollowStore followStore;

    private ChipGroup chips;
    private HorizontalScrollView chipScroll;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView list;
    private View emptyState;
    private TextView emptyMessage;
    private Button emptyAction;
    private ReportAdapter adapter;

    private final List<Topic> topics = new ArrayList<>();
    private Topic selected;
    private boolean bindingChips;
    private String restoredSelectionId;

    /** Loads the topics to show. Called on first display and on pull to refresh. */
    protected abstract void loadTopics(boolean forceRefresh, TopicRepository.Callback<List<Topic>> callback);

    /** Shown when {@link #loadTopics} returns nothing. */
    protected abstract String emptyTopicsMessage();

    /** Label and action for the button under the empty message; null label hides it. */
    protected String emptyActionLabel() {
        return null;
    }

    protected void onEmptyAction() {
    }

    protected boolean showTopicChips() {
        return true;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_topic_feed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repository = TopicRepository.get(requireContext());
        followStore = new FollowStore(requireContext());

        chips = view.findViewById(R.id.topicChips);
        chipScroll = view.findViewById(R.id.topicScroll);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        list = view.findViewById(R.id.reportList);
        emptyState = view.findViewById(R.id.emptyState);
        emptyMessage = view.findViewById(R.id.emptyMessage);
        emptyAction = view.findViewById(R.id.emptyAction);

        adapter = new ReportAdapter(this);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);
        chipScroll.setVisibility(showTopicChips() ? View.VISIBLE : View.GONE);
        swipeRefresh.setOnRefreshListener(() -> refresh(true));
        chips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (bindingChips || checkedIds.isEmpty()) {
                return;
            }
            Chip chip = group.findViewById(checkedIds.get(0));
            Topic topic = chip != null ? (Topic) chip.getTag() : null;
            if (topic != null && !topic.equals(selected)) {
                reveal(chip);
                select(topic, false);
            }
        });

        if (savedInstanceState != null) {
            restoredSelectionId = savedInstanceState.getString(STATE_SELECTED);
        }
        refresh(false);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (selected != null) {
            outState.putString(STATE_SELECTED, selected.id());
        }
    }

    protected void refresh(boolean forceRefresh) {
        swipeRefresh.setRefreshing(true);
        loadTopics(forceRefresh, new TopicRepository.Callback<List<Topic>>() {
            @Override
            public void onResult(List<Topic> value) {
                if (isAdded()) {
                    showTopics(value, forceRefresh);
                }
            }

            @Override
            public void onError(Exception error) {
                if (isAdded()) {
                    showError(error);
                }
            }
        });
    }

    private void showTopics(List<Topic> loaded, boolean forceRefresh) {
        topics.clear();
        topics.addAll(loaded);
        if (topics.isEmpty()) {
            selected = null;
            adapter.setReport(null);
            swipeRefresh.setRefreshing(false);
            showEmpty(emptyTopicsMessage(), emptyActionLabel(), v -> onEmptyAction());
            return;
        }
        emptyState.setVisibility(View.GONE);

        Topic toSelect = topics.get(0);
        String wantedId = selected != null ? selected.id() : restoredSelectionId;
        for (Topic topic : topics) {
            if (topic.id().equals(wantedId)) {
                toSelect = topic;
            }
        }
        restoredSelectionId = null;

        bindingChips = true;
        chips.removeAllViews();
        for (Topic topic : topics) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_topic_chip, chips, false);
            chip.setId(View.generateViewId());
            chip.setText(topic.label());
            chip.setTag(topic);
            chips.addView(chip);
            if (topic.equals(toSelect)) {
                chip.setChecked(true);
                // A restored selection can be past the right edge of the row.
                chipScroll.post(() -> reveal(chip));
            }
        }
        bindingChips = false;
        select(toSelect, forceRefresh);
    }

    /** Scrolls the topic row just enough to show the whole chip. */
    private void reveal(Chip chip) {
        int margin = chips.getPaddingStart();
        int left = chip.getLeft() - margin;
        int right = chip.getRight() + margin - chipScroll.getWidth();
        int x = chipScroll.getScrollX();
        if (left < x) {
            chipScroll.smoothScrollTo(left, 0);
        } else if (right > x) {
            chipScroll.smoothScrollTo(right, 0);
        }
    }

    private void select(Topic topic, boolean forceRefresh) {
        selected = topic;
        TopicReport cached = forceRefresh ? null : repository.cachedReport(topic);
        if (cached != null) {
            showReport(cached);
            return;
        }
        swipeRefresh.setRefreshing(true);
        repository.loadReport(topic, forceRefresh, new TopicRepository.Callback<TopicReport>() {
            @Override
            public void onResult(TopicReport report) {
                // Ignore answers for a topic the user has already moved away from.
                if (isAdded() && topic.equals(selected)) {
                    showReport(report);
                }
            }

            @Override
            public void onError(Exception error) {
                if (isAdded() && topic.equals(selected)) {
                    showError(error);
                }
            }
        });
    }

    private void showReport(TopicReport report) {
        swipeRefresh.setRefreshing(false);
        adapter.setReport(report);
        list.scrollToPosition(0);
        emptyState.setVisibility(View.GONE);
    }

    private void showError(Exception error) {
        swipeRefresh.setRefreshing(false);
        adapter.setReport(null);
        showEmpty(getString(R.string.load_failed, Formatters.errorReason(error)), getString(R.string.retry), v -> refresh(true));
    }

    private void showEmpty(String message, String actionLabel, View.OnClickListener action) {
        emptyState.setVisibility(View.VISIBLE);
        emptyMessage.setText(message);
        emptyAction.setVisibility(actionLabel != null ? View.VISIBLE : View.GONE);
        emptyAction.setText(actionLabel);
        emptyAction.setOnClickListener(action);
    }

    @Override
    public boolean isFollowed(TopicReport report) {
        return followStore.isFollowed(report.topic);
    }

    @Override
    public boolean toggleFollow(TopicReport report) {
        boolean followed = !followStore.isFollowed(report.topic);
        if (followed) {
            followStore.follow(report.topic);
        } else {
            followStore.unfollow(report.topic);
        }
        Toast.makeText(requireContext(), getString(followed ? R.string.followed_toast : R.string.unfollowed_toast,
                report.topic.label()), Toast.LENGTH_SHORT).show();
        return followed;
    }
}
