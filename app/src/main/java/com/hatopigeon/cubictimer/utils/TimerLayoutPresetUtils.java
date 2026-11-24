package com.hatopigeon.cubictimer.utils;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimerLayoutPresetUtils {
    private static final String STORE_KEY = "savedTimerLayoutPresets";
    private static final int FORMAT_VERSION = 1;
    private static final Pattern LAYOUT_KEY = Pattern.compile(
            "timerLayout\\.(portrait|landscape)\\.[A-Za-z0-9_]+\\.(x|y|scale|visibility)");
    private static final Pattern APPEARANCE_KEY = Pattern.compile(
            "timerAppearance_[A-Za-z0-9]+_(font|size|bold|italic|color)");

    private TimerLayoutPresetUtils() {
    }

    @NonNull
    public static String createCurrentPresetJson(@NonNull SharedPreferences preferences,
                                                  @NonNull String name) throws JSONException {
        String normalizedName = name.trim();
        if (!isValidName(normalizedName)) {
            throw new IllegalArgumentException("Preset name must be 1 to 50 characters");
        }

        JSONObject values = new JSONObject();
        for (Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
            if (isManagedPreference(entry.getKey())) {
                validatePreference(entry.getKey(), entry.getValue());
                values.put(entry.getKey(), entry.getValue());
            }
        }
        return new JSONObject()
                .put("version", FORMAT_VERSION)
                .put("name", normalizedName)
                .put("preferences", values)
                .toString(2);
    }

    @NonNull
    public static JSONObject parsePreset(@NonNull String json) throws JSONException {
        JSONObject preset = new JSONObject(json);
        Object version = preset.opt("version");
        if (!(version instanceof Number)
                || ((Number) version).intValue() != FORMAT_VERSION
                || ((Number) version).doubleValue() != FORMAT_VERSION) {
            throw new JSONException("Unsupported timer layout preset version");
        }
        Object rawName = preset.opt("name");
        if (!(rawName instanceof String)) {
            throw new JSONException("Invalid timer layout preset name");
        }
        String name = ((String) rawName).trim();
        if (!isValidName(name)) {
            throw new JSONException("Invalid timer layout preset name");
        }
        preset.put("name", name);
        JSONObject values = preset.optJSONObject("preferences");
        if (values == null) {
            throw new JSONException("Timer layout preset preferences are missing");
        }

        Iterator<String> keys = values.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            validatePreference(key, values.get(key));
        }
        return preset;
    }

    public static boolean hasSavedPreset(@NonNull SharedPreferences preferences,
                                         @NonNull String name) throws JSONException {
        return getSavedPresets(preferences).has(name);
    }

    public static void saveNamedPreset(@NonNull SharedPreferences preferences,
                                       @NonNull String json) throws JSONException {
        JSONObject preset = parsePreset(json);
        JSONObject savedPresets = getSavedPresets(preferences);
        savedPresets.put(preset.getString("name"), preset);
        preferences.edit().putString(STORE_KEY, savedPresets.toString()).apply();
    }

    @NonNull
    public static List<String> getSavedPresetNames(@NonNull SharedPreferences preferences)
            throws JSONException {
        JSONObject savedPresets = getSavedPresets(preferences);
        List<String> names = new ArrayList<>();
        Iterator<String> keys = savedPresets.keys();
        while (keys.hasNext()) {
            String name = keys.next();
            parsePreset(savedPresets.getJSONObject(name).toString());
            names.add(name);
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    @NonNull
    public static String getSavedPresetJson(@NonNull SharedPreferences preferences,
                                            @NonNull String name) throws JSONException {
        JSONObject preset = getSavedPresets(preferences).optJSONObject(name);
        if (preset == null) {
            throw new JSONException("Timer layout preset was not found");
        }
        return parsePreset(preset.toString()).toString(2);
    }

    public static void applyPreset(@NonNull SharedPreferences preferences,
                                   @NonNull String json) throws JSONException {
        JSONObject preset = parsePreset(json);
        JSONObject values = preset.getJSONObject("preferences");
        SharedPreferences.Editor editor = preferences.edit();
        for (String key : preferences.getAll().keySet()) {
            if (isManagedPreference(key)) {
                editor.remove(key);
            }
        }

        Iterator<String> keys = values.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            putPreference(editor, key, values.get(key));
        }
        editor.apply();
    }

    public static boolean isValidName(@NonNull String name) {
        if (name.length() < 1 || name.length() > 50) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            if (Character.isISOControl(name.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static JSONObject getSavedPresets(SharedPreferences preferences) throws JSONException {
        String json = preferences.getString(STORE_KEY, null);
        return json == null ? new JSONObject() : new JSONObject(json);
    }

    private static boolean isManagedPreference(String key) {
        return LAYOUT_KEY.matcher(key).matches()
                || APPEARANCE_KEY.matcher(key).matches()
                || "timerFont".equals(key)
                || "timerTextSize".equals(key)
                || "scrambleTextSize".equals(key);
    }

    private static void validatePreference(String key, Object value) throws JSONException {
        Matcher layoutMatcher = LAYOUT_KEY.matcher(key);
        Matcher appearanceMatcher = APPEARANCE_KEY.matcher(key);
        if (layoutMatcher.matches()) {
            String property = layoutMatcher.group(2);
            if (!(value instanceof Number)) {
                throw new JSONException("Invalid value for " + key);
            }
            double number = ((Number) value).doubleValue();
            if (!Double.isFinite(number)) {
                throw new JSONException("Non-finite value for " + key);
            }
            if ("visibility".equals(property)) {
                if (number != Math.rint(number) || number < -1 || number > 1) {
                    throw new JSONException("Invalid visibility value for " + key);
                }
            } else if ("scale".equals(property)) {
                if (number < 0.5 || number > 2.0) {
                    throw new JSONException("Invalid scale value for " + key);
                }
            } else if (number < -100.0 || number > 100.0) {
                throw new JSONException("Invalid position value for " + key);
            }
            return;
        }

        if (appearanceMatcher.matches()) {
            validateAppearanceValue(key, appearanceMatcher.group(1), value);
            return;
        }

        switch (key) {
            case "timerFont":
                validateFont(value, key);
                return;
            case "timerTextSize":
            case "scrambleTextSize":
                validateInteger(value, key, 1, 300);
                return;
            default:
                throw new JSONException("Unsupported timer layout preference: " + key);
        }
    }

    private static void validateAppearanceValue(String key, String property, Object value)
            throws JSONException {
        switch (property) {
            case "font":
                validateFont(value, key);
                break;
            case "color":
                if (!(value instanceof String)
                        || !("theme".equals(value) || "accent".equals(value)
                        || "white".equals(value) || "black".equals(value))) {
                    throw new JSONException("Invalid color value for " + key);
                }
                break;
            case "size":
                validateInteger(value, key, 50, 200);
                break;
            case "bold":
            case "italic":
                if (!(value instanceof Boolean)) {
                    throw new JSONException("Invalid boolean value for " + key);
                }
                break;
            default:
                throw new JSONException("Unsupported appearance property: " + key);
        }
    }

    private static void validateFont(Object value, String key) throws JSONException {
        if (!(value instanceof String)) {
            throw new JSONException("Invalid font value for " + key);
        }
        String font = (String) value;
        if ("default".equals(font) || "lato".equals(font) || "quicksand".equals(font)
                || "sans_serif".equals(font) || "monospace".equals(font)) {
            return;
        }
        if (font.startsWith(TimerFontUtils.CUSTOM_FONT_ID_PREFIX)) {
            String fileName = font.substring(TimerFontUtils.CUSTOM_FONT_ID_PREFIX.length());
            if (fileName.matches("[A-Za-z0-9._-]+\\.(?i:ttf|otf)")
                    && !fileName.contains("..")) {
                return;
            }
        }
        throw new JSONException("Invalid font value for " + key);
    }

    private static void validateInteger(Object value, String key, int minimum, int maximum)
            throws JSONException {
        if (!(value instanceof Number)) {
            throw new JSONException("Invalid number value for " + key);
        }
        double number = ((Number) value).doubleValue();
        if (!Double.isFinite(number) || number != Math.rint(number)
                || number < minimum || number > maximum) {
            throw new JSONException("Out-of-range number value for " + key);
        }
    }

    private static void putPreference(SharedPreferences.Editor editor, String key, Object value)
            throws JSONException {
        Matcher layoutMatcher = LAYOUT_KEY.matcher(key);
        Matcher appearanceMatcher = APPEARANCE_KEY.matcher(key);
        if (layoutMatcher.matches()) {
            String property = layoutMatcher.group(2);
            if ("visibility".equals(property)) {
                editor.putInt(key, ((Number) value).intValue());
            } else {
                editor.putFloat(key, ((Number) value).floatValue());
            }
        } else if (appearanceMatcher.matches()) {
            switch (appearanceMatcher.group(1)) {
                case "font":
                case "color":
                    editor.putString(key, (String) value);
                    break;
                case "size":
                    editor.putInt(key, ((Number) value).intValue());
                    break;
                case "bold":
                case "italic":
                    editor.putBoolean(key, (Boolean) value);
                    break;
                default:
                    throw new JSONException("Unsupported appearance property: " + key);
            }
        } else if ("timerFont".equals(key)) {
            editor.putString(key, (String) value);
        } else if ("timerTextSize".equals(key) || "scrambleTextSize".equals(key)) {
            editor.putInt(key, ((Number) value).intValue());
        } else {
            throw new JSONException("Unsupported timer layout preference: " + key);
        }
    }
}
