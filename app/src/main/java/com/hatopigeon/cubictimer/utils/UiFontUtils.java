package com.hatopigeon.cubictimer.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.hatopigeon.cubicify.R;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class UiFontUtils {
    private static final String TAG = UiFontUtils.class.getSimpleName();
    private static final Map<String, CachedTypeface> TYPEFACE_CACHE = new HashMap<>();

    private UiFontUtils() {
    }

    @Nullable
    public static Typeface getSelectedTypeface(@NonNull Context context) {
        String fontId = Prefs.getPrefs().getString(
                context.getString(R.string.pk_app_ui_font), "default");
        return getTypeface(context, fontId);
    }

    public static void preloadSelectedTypeface(@NonNull Context context) {
        getSelectedTypeface(context);
    }

    @Nullable
    public static Typeface getTypeface(@NonNull Context context, @Nullable String fontId) {
        if (fontId == null || !fontId.startsWith(TimerFontUtils.CUSTOM_FONT_ID_PREFIX)) {
            return null;
        }

        String fileName = fontId.substring(TimerFontUtils.CUSTOM_FONT_ID_PREFIX.length());
        File fontFile = new File(TimerFontUtils.getCustomFontDirectory(context), fileName);
        if (!fontFile.isFile() || !fontFile.getName().equals(fileName)
                || !(fileName.toLowerCase(java.util.Locale.ROOT).endsWith(".ttf")
                || fileName.toLowerCase(java.util.Locale.ROOT).endsWith(".otf"))) {
            Log.e(TAG, "Selected app UI font is missing or invalid: " + fileName);
            return null;
        }

        String path = fontFile.getAbsolutePath();
        long lastModified = fontFile.lastModified();
        synchronized (TYPEFACE_CACHE) {
            CachedTypeface cached = TYPEFACE_CACHE.get(path);
            if (cached != null && cached.lastModified == lastModified) {
                return cached.typeface;
            }
            try {
                Typeface typeface = Typeface.createFromFile(fontFile.getAbsolutePath());
                TYPEFACE_CACHE.put(path, new CachedTypeface(lastModified, typeface));
                return typeface;
            } catch (RuntimeException e) {
                Log.e(TAG, "Could not load app UI font " + fileName, e);
                return null;
            }
        }
    }

    public static void applyToViewTree(@NonNull View view) {
        applyToViewTree(view, getSelectedTypeface(view.getContext()));
    }

    public static void applyToViewTree(@NonNull View view, @Nullable Typeface selectedTypeface) {
        if (isTimerComponentFontControlled(view)) {
            return;
        }

        if (view instanceof RecyclerView
                && view.getTag(R.id.ui_font_recycler_listener) == null) {
            RecyclerView recyclerView = (RecyclerView) view;
            recyclerView.setTag(R.id.ui_font_recycler_listener, Boolean.TRUE);
            recyclerView.addOnChildAttachStateChangeListener(
                    new RecyclerView.OnChildAttachStateChangeListener() {
                        @Override
                        public void onChildViewAttachedToWindow(@NonNull View child) {
                            UiFontUtils.applyToViewTree(child,
                                    getSelectedTypeface(recyclerView.getContext()));
                        }

                        @Override
                        public void onChildViewDetachedFromWindow(@NonNull View child) {
                        }
                    });
        }

        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            Object originalTypeface = textView.getTag(R.id.ui_font_original_typeface);
            if (originalTypeface == null) {
                if (selectedTypeface == null) {
                    return;
                }
                Typeface currentTypeface = textView.getTypeface();
                textView.setTag(R.id.ui_font_original_typeface,
                        currentTypeface == null ? Typeface.DEFAULT : currentTypeface);
                originalTypeface = textView.getTag(R.id.ui_font_original_typeface);
            }

            if (selectedTypeface == null) {
                textView.setTypeface((Typeface) originalTypeface);
            } else {
                int style = selectedTypeface.getStyle()
                        | ((Typeface) originalTypeface).getStyle();
                textView.setTypeface(Typeface.create(selectedTypeface, style));
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyToViewTree(group.getChildAt(i), selectedTypeface);
            }
        }
    }

    public static void applyToMenu(@NonNull Menu menu, @NonNull Context context) {
        Typeface typeface = getSelectedTypeface(context);
        if (typeface == null) {
            return;
        }
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            SpannableString title = new SpannableString(item.getTitle());
            if (title.length() > 0) {
                title.setSpan(new MenuTypefaceSpan(typeface), 0, title.length(), 0);
                item.setTitle(title);
            }
            if (item.hasSubMenu()) {
                applyToMenu((SubMenu) item.getSubMenu(), context);
            }
        }
    }

    public static void applyWhenDialogShown(@NonNull Context context, @NonNull Dialog dialog) {
        if (dialog.getWindow() == null) {
            return;
        }
        View decorView = dialog.getWindow().getDecorView();
        if (dialog.isShowing()) {
            decorView.post(() -> applyToViewTree(decorView, getSelectedTypeface(context)));
            return;
        }

        ViewTreeObserver observer = decorView.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ViewTreeObserver currentObserver = decorView.getViewTreeObserver();
                if (currentObserver.isAlive()) {
                    currentObserver.removeOnGlobalLayoutListener(this);
                }
                applyToViewTree(decorView, getSelectedTypeface(context));
            }
        });
    }

    private static boolean isTimerComponentFontControlled(@NonNull View view) {
        int id = view.getId();
        return id == R.id.chronometer
                || id == R.id.scramble_text
                || id == R.id.lapTimeText
                || id == R.id.sessionDetailTextAverage
                || id == R.id.sessionDetailTextOther
                || id == R.id.sessionRecentResultText
                || id == R.id.detail_average_record_message
                || id == R.id.congratsText
                || id == R.id.inspection_text
                || id == R.id.multi_phase_status_message
                || id == R.id.ble_status_message
                || id == R.id.serial_status_message;
    }

    private static final class CachedTypeface {
        final long lastModified;
        final Typeface typeface;

        CachedTypeface(long lastModified, @NonNull Typeface typeface) {
            this.lastModified = lastModified;
            this.typeface = typeface;
        }
    }

    private static final class MenuTypefaceSpan extends MetricAffectingSpan {
        private final Typeface typeface;

        MenuTypefaceSpan(@NonNull Typeface typeface) {
            this.typeface = typeface;
        }

        @Override
        public void updateMeasureState(@NonNull TextPaint paint) {
            applyTypeface(paint);
        }

        @Override
        public void updateDrawState(@NonNull TextPaint paint) {
            applyTypeface(paint);
        }

        private void applyTypeface(@NonNull TextPaint paint) {
            int style = typeface.getStyle()
                    | (paint.getTypeface() == null
                    ? Typeface.NORMAL : paint.getTypeface().getStyle());
            paint.setTypeface(Typeface.create(typeface, style));
        }
    }
}
