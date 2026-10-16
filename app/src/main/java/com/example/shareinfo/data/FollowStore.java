package com.example.shareinfo.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** The topics the user follows, persisted in shared preferences as a JSON array. */
public final class FollowStore {
    private static final String KEY_FOLLOWED = "followedTopics";
    // Keys and folders written by version 1 of the app.
    private static final String LEGACY_SYMBOLS = "favoriteStocksSymbols";
    private static final String LEGACY_NAMES = "favoriteStocksNames";
    private static final String[] LEGACY_KEYS = {LEGACY_SYMBOLS, LEGACY_NAMES, "stockTrendingNames",
            "stockTrendingSymbols", "trendingExists", "personalizedExists", "searchExists", "profileExists"};
    private static final String[] LEGACY_FOLDERS = {"stock_information", "favorites_stock_information"};

    private final SharedPreferences preferences;

    public FollowStore(Context context) {
        // The same file PreferenceManager.getDefaultSharedPreferences used in version 1.
        preferences = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        migrateLegacyFavorites(context);
    }

    public List<Topic> followed() {
        List<Topic> topics = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(preferences.getString(KEY_FOLLOWED, "[]"));
            for (int i = 0; i < array.length(); i++) {
                topics.add(Topic.fromJson(array.getJSONObject(i)));
            }
        } catch (JSONException | IllegalArgumentException e) {
            // Corrupt data should not crash the app; start over with an empty list.
            preferences.edit().remove(KEY_FOLLOWED).apply();
        }
        return topics;
    }

    public boolean isFollowed(Topic topic) {
        return followed().contains(topic);
    }

    public void follow(Topic topic) {
        List<Topic> topics = followed();
        if (!topics.contains(topic)) {
            topics.add(topic);
            save(topics);
        }
    }

    public void unfollow(Topic topic) {
        List<Topic> topics = followed();
        if (topics.remove(topic)) {
            save(topics);
        }
    }

    private void save(List<Topic> topics) {
        JSONArray array = new JSONArray();
        for (Topic topic : topics) {
            array.put(topic.toJson());
        }
        preferences.edit().putString(KEY_FOLLOWED, array.toString()).apply();
    }

    /** Version 1 kept favorites as two comma separated strings and cached API responses on disk. */
    private void migrateLegacyFavorites(Context context) {
        if (!preferences.contains(LEGACY_SYMBOLS)) {
            return;
        }
        String[] symbols = preferences.getString(LEGACY_SYMBOLS, "").split(",");
        String[] names = preferences.getString(LEGACY_NAMES, "").split(",");
        List<Topic> topics = followed();
        for (int i = 0; i < symbols.length; i++) {
            String symbol = symbols[i].trim();
            if (symbol.isEmpty()) {
                continue;
            }
            Topic topic = Topic.stock(symbol, i < names.length ? names[i] : symbol);
            if (!topics.contains(topic)) {
                topics.add(topic);
            }
        }
        save(topics);
        SharedPreferences.Editor editor = preferences.edit();
        for (String key : LEGACY_KEYS) {
            editor.remove(key);
        }
        editor.apply();
        for (String folder : LEGACY_FOLDERS) {
            deleteRecursively(new File(context.getFilesDir(), folder));
        }
    }

    private static void deleteRecursively(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }
}
