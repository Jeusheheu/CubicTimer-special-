package com.hatopigeon.cubictimer.utils;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.File;

public final class TimerFontUtils {
    public static final String CUSTOM_FONT_ID_PREFIX = "custom:";

    private TimerFontUtils() {
    }

    @NonNull
    public static File getCustomFontDirectory(@NonNull Context context) {
        return new File(context.getFilesDir(), "custom_fonts");
    }

    @NonNull
    public static String getCustomFontId(@NonNull String fileName) {
        return CUSTOM_FONT_ID_PREFIX + fileName;
    }

    @NonNull
    public static String getDisplayNamePreferenceKey(@NonNull String fileName) {
        return "timerCustomFontName_" + fileName;
    }
}
