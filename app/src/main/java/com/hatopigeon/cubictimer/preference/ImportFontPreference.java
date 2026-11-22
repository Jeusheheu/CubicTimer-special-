package com.hatopigeon.cubictimer.preference;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.hatopigeon.cubicify.R;
import com.google.android.material.button.MaterialButton;

public class ImportFontPreference extends Preference {
    public ImportFontPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_import_font);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        View importButton = holder.findViewById(R.id.import_font_button);
        if (importButton instanceof MaterialButton) {
            importButton.setOnClickListener(view -> onClick());
            importButton.setOnTouchListener((view, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    view.animate().scaleX(0.97f).scaleY(0.97f)
                            .setDuration(90L).start();
                } else if (event.getActionMasked() == MotionEvent.ACTION_UP
                        || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                    view.animate().scaleX(1f).scaleY(1f)
                            .setDuration(240L)
                            .setInterpolator(new OvershootInterpolator(0.55f))
                            .start();
                }
                return false;
            });
        }
    }
}
