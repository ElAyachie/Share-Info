package com.example.shareinfo.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shareinfo.R;
import com.example.shareinfo.data.FollowStore;
import com.example.shareinfo.data.Topic;
import com.example.shareinfo.data.TopicRepository;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.List;

/**
 * Finds stocks, funds and coins by name or ticker, and lets the user track any free text topic
 * (for example "pokemon cards") that has no ticker at all.
 */
public class SearchFragment extends Fragment implements TopicListAdapter.Listener {
    private static final long TYPING_DELAY_MILLIS = 400;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TopicRepository repository;
    private FollowStore followStore;
    private TopicListAdapter adapter;
    private LinearProgressIndicator progress;
    private EditText input;
    private int requestId;

    private final Runnable searchRunnable = this::search;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repository = TopicRepository.get(requireContext());
        followStore = new FollowStore(requireContext());
        progress = view.findViewById(R.id.searchProgress);
        input = view.findViewById(R.id.searchInput);

        RecyclerView results = view.findViewById(R.id.searchResults);
        results.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TopicListAdapter(this);
        results.setAdapter(adapter);

        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                handler.removeCallbacks(searchRunnable);
                search();
                return true;
            }
            return false;
        });
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                handler.removeCallbacks(searchRunnable);
                handler.postDelayed(searchRunnable, TYPING_DELAY_MILLIS);
            }
        });
        search();
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacks(searchRunnable);
        super.onDestroyView();
    }

    private void search() {
        String query = input.getText() == null ? "" : input.getText().toString().trim();
        int id = ++requestId;
        progress.setVisibility(View.VISIBLE);

        if (query.isEmpty()) {
            repository.loadTrending(false, new TopicRepository.Callback<List<Topic>>() {
                @Override
                public void onResult(List<Topic> topics) {
                    if (isCurrent(id)) {
                        show(null, topics, null);
                    }
                }

                @Override
                public void onError(Exception error) {
                    if (isCurrent(id)) {
                        show(null, null, error);
                    }
                }
            });
            return;
        }
        repository.search(query, new TopicRepository.Callback<List<Topic>>() {
            @Override
            public void onResult(List<Topic> topics) {
                if (isCurrent(id)) {
                    show(query, topics, null);
                }
            }

            @Override
            public void onError(Exception error) {
                if (isCurrent(id)) {
                    show(query, null, error);
                }
            }
        });
    }

    private boolean isCurrent(int id) {
        return isAdded() && getView() != null && id == requestId;
    }

    private void show(String query, List<Topic> topics, Exception error) {
        progress.setVisibility(View.INVISIBLE);
        adapter.clear();
        if (query != null) {
            // Anything can be a topic, so free text tracking is always offered first.
            adapter.addTopic(Topic.custom(query), getString(R.string.search_custom_title, query),
                    getString(R.string.search_custom_subtitle));
        }
        if (error != null) {
            adapter.addHeader(getString(query != null ? R.string.search_failed : R.string.load_failed,
                    Formatters.errorReason(error)));
        } else if (topics != null && !topics.isEmpty()) {
            adapter.addHeader(getString(query != null ? R.string.search_results_header : R.string.search_trending_header));
            for (Topic topic : topics) {
                adapter.addTopic(requireContext(), topic);
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onOpen(Topic topic) {
        TopicDetailFragment.open(this, topic);
    }

    @Override
    public boolean isFollowed(Topic topic) {
        return followStore.isFollowed(topic);
    }

    @Override
    public void onToggleFollow(Topic topic) {
        if (followStore.isFollowed(topic)) {
            followStore.unfollow(topic);
        } else {
            followStore.follow(topic);
        }
    }
}
