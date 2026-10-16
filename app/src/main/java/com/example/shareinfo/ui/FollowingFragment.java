package com.example.shareinfo.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shareinfo.R;
import com.example.shareinfo.data.FollowStore;
import com.example.shareinfo.data.Topic;

/** Appearance setting, followed topics, and an explanation of how scores are computed. */
public class FollowingFragment extends Fragment implements TopicListAdapter.Listener {
    private FollowStore followStore;
    private TopicListAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        followStore = new FollowStore(requireContext());
        adapter = new TopicListAdapter(this);
        RecyclerView list = view.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        bind();
    }

    private void bind() {
        adapter.clear();
        adapter.addAppearance();
        adapter.addHeader(getString(R.string.following_header));
        for (Topic topic : followStore.followed()) {
            adapter.addTopic(requireContext(), topic);
        }
        adapter.addAbout();
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
