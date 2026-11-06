package com.hatopigeon.cubictimer.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsManager {
    private static final String PREF_NAME = "CubicTimerCustomization";
    private static final String KEY_FONT = "selected_font";
    private static final String KEY_BG_URI = "background_uri";
    private static final String KEY_ANIMATION_ENABLED = "animation_enabled";

    private SharedPreferences sharedPreferences;

    public SettingsManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Save and get Font choice
    public void setFont(String fontName) {
        sharedPreferences.edit().putString(KEY_FONT, fontName).apply();
    }

    public String getFont() {
        return sharedPreferences.getString(KEY_FONT, "default");
    }

    // Save and get Background Image URI
    public void setBackgroundUri(String uriString) {
        sharedPreferences.edit().putString(KEY_BG_URI, uriString).apply();
    }

    public String getBackgroundUri() {
        return sharedPreferences.getString(KEY_BG_URI, null);
    }

    // Save and get Animation Toggle
    public void setAnimationEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_ANIMATION_ENABLED, enabled).apply();
    }

    boolean isAnimationEnabled() {
        return sharedPreferences.getBoolean(KEY_ANIMATION_ENABLED, false);
    }
}
