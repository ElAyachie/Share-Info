package com.example.shareinfo.data;

import org.json.JSONObject;

/** JSON helpers that behave the same on Android and on the JVM. */
public final class Json {
    private Json() {
    }

    /**
     * Like {@link JSONObject#optString(String, String)}, except that an explicit JSON null also
     * returns the fallback. Android's implementation returns the text "null" in that case.
     */
    public static String string(JSONObject object, String key, String fallback) {
        if (object == null || object.isNull(key)) {
            return fallback;
        }
        String value = object.optString(key, fallback);
        return value.isEmpty() ? fallback : value;
    }
}
