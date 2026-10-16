package com.example.shareinfo.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shareinfo.R;
import com.example.shareinfo.data.Topic;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.ArrayList;
import java.util.List;

/** Section headers, topic rows with a follow star, and optional settings and about cards. */
class TopicListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_TOPIC = 1;
    private static final int TYPE_ABOUT = 2;
    private static final int TYPE_APPEARANCE = 3;

    interface Listener {
        void onOpen(Topic topic);

        boolean isFollowed(Topic topic);

        void onToggleFollow(Topic topic);
    }

    private static final class Item {
        final int type;
        final String title;
        final String subtitle;
        final Topic topic;

        Item(int type, String title, String subtitle, Topic topic) {
            this.type = type;
            this.title = title;
            this.subtitle = subtitle;
            this.topic = topic;
        }
    }

    private final Listener listener;
    private final List<Item> items = new ArrayList<>();

    TopicListAdapter(Listener listener) {
        this.listener = listener;
    }

    void clear() {
        items.clear();
    }

    void addHeader(String title) {
        items.add(new Item(TYPE_HEADER, title, null, null));
    }

    void addTopic(Context context, Topic topic) {
        items.add(new Item(TYPE_TOPIC, topic.label(),
                topic.symbol != null ? topic.name + " · " + Formatters.typeName(context, topic)
                        : Formatters.typeName(context, topic), topic));
    }

    void addTopic(Topic topic, String title, String subtitle) {
        items.add(new Item(TYPE_TOPIC, title, subtitle, topic));
    }

    void addAppearance() {
        items.add(new Item(TYPE_APPEARANCE, null, null, null));
    }

    void addAbout() {
        items.add(new Item(TYPE_ABOUT, null, null, null));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        int layout;
        switch (viewType) {
            case TYPE_HEADER:
                layout = R.layout.item_section_header;
                break;
            case TYPE_ABOUT:
                layout = R.layout.item_about;
                break;
            case TYPE_APPEARANCE:
                layout = R.layout.item_appearance;
                break;
            default:
                layout = R.layout.item_topic;
                break;
        }
        return new RecyclerView.ViewHolder(inflater.inflate(layout, parent, false)) {
        };
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Item item = items.get(position);
        View view = holder.itemView;
        if (item.type == TYPE_HEADER) {
            ((TextView) view.findViewById(R.id.sectionTitle)).setText(item.title);
        } else if (item.type == TYPE_TOPIC) {
            ((TextView) view.findViewById(R.id.topicTitle)).setText(item.title);
            ((TextView) view.findViewById(R.id.topicSubtitle)).setText(item.subtitle);
            MaterialButton follow = view.findViewById(R.id.followToggle);
            bindStar(follow, item.topic, listener.isFollowed(item.topic));
            follow.setOnClickListener(v -> {
                listener.onToggleFollow(item.topic);
                bindStar(follow, item.topic, listener.isFollowed(item.topic));
            });
            view.setOnClickListener(v -> listener.onOpen(item.topic));
        } else if (item.type == TYPE_APPEARANCE) {
            bindAppearance(view);
        }
    }

    private static void bindAppearance(View view) {
        Context context = view.getContext();
        MaterialButtonToggleGroup toggle = view.findViewById(R.id.themeToggle);
        toggle.clearOnButtonCheckedListeners();
        switch (ThemePrefs.nightMode(context)) {
            case AppCompatDelegate.MODE_NIGHT_NO:
                toggle.check(R.id.themeLight);
                break;
            case AppCompatDelegate.MODE_NIGHT_YES:
                toggle.check(R.id.themeDark);
                break;
            default:
                toggle.check(R.id.themeSystem);
                break;
        }
        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            int mode = checkedId == R.id.themeLight ? AppCompatDelegate.MODE_NIGHT_NO
                    : checkedId == R.id.themeDark ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            if (mode != ThemePrefs.nightMode(context)) {
                ThemePrefs.set(context, mode);
            }
        });
    }

    private static void bindStar(MaterialButton button, Topic topic, boolean followed) {
        Context context = button.getContext();
        button.setIcon(ContextCompat.getDrawable(context, followed ? R.drawable.ic_baseline_star : R.drawable.ic_star_border));
        // Name the action and the topic; a list of buttons all read as "Following" is meaningless.
        button.setContentDescription(context.getString(followed ? R.string.unfollow_topic : R.string.follow_topic, topic.label()));
    }
}
