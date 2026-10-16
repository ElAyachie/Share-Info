package com.example.shareinfo.ui;

import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;

/** Remembers whether the app follows the system theme or is forced light or dark. */
public final class ThemePrefs {
    private static final String KEY_NIGHT_MODE = "nightMode";

    private ThemePrefs() {
    }

    /** One of AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, MODE_NIGHT_NO or MODE_NIGHT_YES. */
    public static int nightMode(Context context) {
        return preferences(context).getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    /** Applies the saved mode; call before the first activity inflates its views. */
    public static void apply(Context context) {
        AppCompatDelegate.setDefaultNightMode(nightMode(context));
    }

    /** Saves and applies a mode. AppCompat recreates visible activities to switch themes. */
    public static void set(Context context, int mode) {
        preferences(context).edit().putInt(KEY_NIGHT_MODE, mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    private static android.content.SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
    }
}
