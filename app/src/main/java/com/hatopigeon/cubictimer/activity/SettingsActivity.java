package com.hatopigeon.cubictimer.activity;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.IntegerRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.view.WindowCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.Preference;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;
import androidx.appcompat.widget.AppCompatSeekBar;

import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.hatopigeon.cubicify.R;
import com.hatopigeon.cubictimer.fragment.dialog.CrossHintFaceSelectDialog;
import com.hatopigeon.cubictimer.fragment.dialog.LocaleSelectDialog;
import com.hatopigeon.cubictimer.preference.ImportFontPreference;
import com.hatopigeon.cubictimer.utils.InsetsUtils;
import com.hatopigeon.cubictimer.utils.LocaleUtils;
import com.hatopigeon.cubictimer.utils.Prefs;
import com.hatopigeon.cubictimer.utils.ThemeUtils;
import com.hatopigeon.cubictimer.utils.TimerFontUtils;
import com.takisoft.preferencex.PreferenceFragmentCompat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SettingsActivity extends AppCompatActivity {
    /**
     * Flag to enable debug logging for this class.
     */
    private static final boolean DEBUG_ME = false;

    /**
     * A "tag" to identify this class in log messages.
     */
    private static final String TAG = SettingsActivity.class.getSimpleName();


    @BindView(R.id.back) View backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (DEBUG_ME) Log.d(TAG, "onCreate(savedInstanceState=" + savedInstanceState + ")");
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setTheme(R.style.SettingsTheme);

        LocaleUtils.updateLocale(getApplicationContext());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);

        backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        if (savedInstanceState == null) {
            // Add the main "parent" settings fragment. It is not added to be back stack, so that
            // when "Back" is pressed, the "SettingsActivity" will exit, which is appropriate.
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main_activity_container, new SettingsFragment(), "fragment_settings")
                    .commit();
        }

        InsetsUtils.applySafeInsetsPadding(findViewById(R.id.activity_settings), false);
    }

    public void onRecreateRequired() {
        if (DEBUG_ME) Log.d(TAG, "onRecreationRequired(): " + this);
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                if (DEBUG_ME) Log.d(TAG, "  Activity.recreate() NOW!: " + this);
                recreate();
            }
        });
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleUtils.updateLocale(newBase));
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        // Variables used to handle back button behavior
        // Stores last PreferenceScreen opened
        private PreferenceScreen lastPreferenceScreen;
        // Stores the main PreferenceScreen
        private PreferenceScreen mainScreen;

        Context mContext;

        private int inspectionDuration;

        private String averageText;
        private final int maximumPhaseNum = 5;
        private final int defaultPhaseNum = 4;

        private ActivityResultLauncher<Intent> openDocLauncher;
        private ActivityResultLauncher<Intent> importFontLauncher;
        private boolean isBgImageSelectPortrait = true;

        private final androidx.preference.Preference.OnPreferenceClickListener clickListener
                = new androidx.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(androidx.preference.Preference preference) {
                switch (Prefs.keyToResourceID(preference.getKey(),
                        R.string.pk_inspection_time,
                        R.string.pk_multi_phase_num,
                        R.string.pk_show_scramble_x_cross_hints,
                        R.string.pk_locale,
                        R.string.pk_reset_locale,
                        R.string.pk_options_show_scramble_hints,
                        R.string.pk_timer_text_size,
                        R.string.pk_timer_text_offset,
                        R.string.pk_scramble_image_size,
                        R.string.pk_scramble_text_size,
                        R.string.pk_advanced_timer_settings_enabled,
                        R.string.pk_stat_trim_size,
                        R.string.pk_timer_animation_duration,
                        R.string.pk_bg_image_portrait,
                        R.string.pk_bg_image_landscape,
                        R.string.pk_bg_image_opacity,
                        R.string.pk_timer_font_import,
                        R.string.pk_appearance_timer,
                        R.string.pk_appearance_scramble,
                        R.string.pk_appearance_ao5,
                        R.string.pk_appearance_ao12,
                        R.string.pk_appearance_ao100,
                        R.string.pk_appearance_status,
                        R.string.pk_appearance_laps)) {

                    case R.string.pk_inspection_time:
                        createNumberDialog(R.string.inspection_time, R.string.pk_inspection_time);
                        break;

                    case R.string.pk_multi_phase_num:
                        createPhaseNumDialog(R.string.phase_num, R.string.pk_multi_phase_num);
                        break;

                    case R.string.pk_show_scramble_x_cross_hints:
                        if (Prefs.getBoolean(R.string.pk_show_scramble_x_cross_hints, false)) {
                            ThemeUtils.roundAndShowDialog(mContext, new MaterialDialog.Builder(mContext)
                                    .title(R.string.warning)
                                    .content(R.string.showHintsXCrossSummary)
                                    .positiveText(R.string.action_ok)
                                    .build());
                        }
                        break;

                    case R.string.pk_options_show_scramble_hints:
                        CrossHintFaceSelectDialog.newInstance()
                                .show(((AppCompatActivity) getActivity()).getSupportFragmentManager(), "cross_hint_face_dialog");
                        break;

                    case R.string.pk_locale:
                        if (getActivity() instanceof AppCompatActivity) {
                            LocaleSelectDialog.newInstance()
                                    .show(((AppCompatActivity) getActivity()).getSupportFragmentManager(), "locale_dialog");
                        } else {
                            Log.e(TAG, "Could not find correct activity to launch dialog!");
                        }
                        break;

                    case R.string.pk_reset_locale:
                        LocaleUtils.resetLocale();
                        ((SettingsActivity)getActivity()).onRecreateRequired();
                        break;

                    case R.string.pk_timer_text_size:
                        createSeekTextSizeDialog(R.string.pk_timer_text_size, 60, "12.34", true);
                        break;

                    case R.string.pk_scramble_image_size:
                        createImageSeekDialog(
                                R.string.pk_scramble_image_size, R.string.scrambleImageSize_text);
                        break;

                    case R.string.pk_scramble_text_size:
                        createSeekTextSizeDialog(R.string.pk_scramble_text_size,
                                14, "R U R' U' R' F R2 U' R' U' R U R' F'", false);
                        break;

                    case R.string.pk_advanced_timer_settings_enabled:
                        if (Prefs.getBoolean(R.string.pk_advanced_timer_settings_enabled, false)) {
                            ThemeUtils.roundAndShowDialog(mContext, new MaterialDialog.Builder(mContext)
                                    .title(R.string.warning)
                                    .content(R.string.advanced_pref_summary)
                                    .positiveText(R.string.action_ok)
                                    .build());
                        }
                        break;
                    case R.string.pk_timer_animation_duration:
                        createSeekDialog(R.string.pk_timer_animation_duration,
                                         0, 1000,
                                         R.integer.defaultAnimationDuration,
                                         "%d ms");
                        break;
                    case R.string.pk_stat_trim_size:
                        // This would be a lot cleaner with high-order functions, but I couldn't find a way to get it working for API < 24
                        MaterialDialog trimDialogView = createAverageSeekDialog(R.string.pk_stat_trim_size,
                                                0, 30,
                                                R.integer.defaultTrimSize);

                        TextView trimText = trimDialogView.getView().findViewById(R.id.text);
                        AppCompatSeekBar trimSeekBar = trimDialogView.getView().findViewById(R.id.seekbar);

                        trimDialogView.getBuilder().onPositive((dialog, which) -> {
                            Prefs.edit()
                                    .putInt(R.string.pk_stat_trim_size, trimSeekBar.getProgress() > 0 ? trimSeekBar.getProgress() : 0)
                                    .apply();
                            Prefs.edit()
                                    .putInt(R.string.pk_stat_acceptable_dnf_size, trimSeekBar.getProgress() > 0 ? trimSeekBar.getProgress() : 0)
                                    .apply();
                        }).onNeutral((dialog, which) -> {
                            Prefs.edit()
                                    .putInt(R.string.pk_stat_trim_size, getResources().getInteger(R.integer.defaultTrimSize))
                                    .apply();
                            Prefs.edit()
                                    .putInt(R.string.pk_stat_acceptable_dnf_size, getResources().getInteger(R.integer.defaultTrimSize))
                                    .apply();
                        });


                        SeekBar.OnSeekBarChangeListener trimChangeListener = new SeekBar.OnSeekBarChangeListener() {
                            int ao50, ao100, ao1000;
                            @Override
                            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                ao50 = getTrim(50, progress);
                                ao100 = getTrim(100, progress);
                                ao1000 = getTrim(1000, progress);
                                trimText.setText(String.format(
                                        getString(R.string.pref_dialog_trim_size, progress) + "%%\n\n" +
                                        getString(R.string.pref_dialog_included_solves) +
                                                               "\n\n%s: %d\n%s: %d\n%s: %d\n%s: %d\n%s: %d\n%s: %d",
                                                               averageText + 3, 0,
                                                               averageText + 5, 2,
                                                               averageText + 12, 2,
                                                               averageText + 50, ao50,
                                                               averageText + 100, ao100,
                                                               averageText + 1000, ao1000));
                            }

                            @Override
                            public void onStartTrackingTouch(SeekBar seekBar) {

                            }

                            @Override
                            public void onStopTrackingTouch(SeekBar seekBar) {

                            }
                        };

                        trimSeekBar.setOnSeekBarChangeListener(trimChangeListener);
                        trimChangeListener.onProgressChanged(trimSeekBar, trimSeekBar.getProgress(), false);
                        trimDialogView.show();
                        break;
                    case R.string.pk_bg_image_portrait:
                        createImagePickerDialog(true);
                        break;
                    case R.string.pk_bg_image_landscape:
                        createImagePickerDialog(false);
                        break;
                    case R.string.pk_bg_image_opacity:
                        createSeekDialog(R.string.pk_bg_image_opacity,
                                0, 100,
                                R.integer.defaultColorOverlayOpacity,
                                "%d %%");
                        break;
                    case R.string.pk_timer_font_import:
                        launchFontPicker();
                        break;
                    case R.string.pk_appearance_timer:
                        showComponentAppearanceDialog("timer", R.string.timer_appearance_timer);
                        break;
                    case R.string.pk_appearance_scramble:
                        showComponentAppearanceDialog("scramble", R.string.timer_appearance_scramble);
                        break;
                    case R.string.pk_appearance_ao5:
                        showComponentAppearanceDialog("ao5", R.string.timer_appearance_ao5);
                        break;
                    case R.string.pk_appearance_ao12:
                        showComponentAppearanceDialog("ao12", R.string.timer_appearance_ao12);
                        break;
                    case R.string.pk_appearance_ao100:
                        showComponentAppearanceDialog("ao100", R.string.timer_appearance_ao100);
                        break;
                    case R.string.pk_appearance_status:
                        showComponentAppearanceDialog("status", R.string.timer_appearance_status);
                        break;
                    case R.string.pk_appearance_laps:
                        showComponentAppearanceDialog("laps", R.string.timer_appearance_laps);
                        break;
                }
                return false;
            }
        };


        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
            super.onCreate(savedInstanceState);

            mContext = getContext();

            averageText = getString(R.string.graph_legend_avg_prefix);

            openDocLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            Uri uri = result.getData().getData();
                            if (uri != null) {
                                onBackgroundMediaPicked(uri, result.getData().getFlags());
                            }
                        }
                    }
            );
            importFontLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null
                                && result.getData().getData() != null) {
                            importCustomFont(result.getData().getData());
                        }
                    }
            );
        }

        @Override
        public void onCreatePreferencesFix(Bundle bundle, String rootKey) {
            setPreferencesFromResource(R.xml.prefs, rootKey);

            int listenerPrefIds[] = {R.string.pk_inspection_time,
                    R.string.pk_multi_phase_num,
                    R.string.pk_show_scramble_x_cross_hints,
                    R.string.pk_locale,
                    R.string.pk_reset_locale,
                    R.string.pk_options_show_scramble_hints,
                    R.string.pk_timer_text_size,
                    R.string.pk_scramble_text_size,
                    R.string.pk_scramble_image_size,
                    R.string.pk_advanced_timer_settings_enabled,
                    R.string.pk_stat_trim_size,
                    R.string.pk_timer_animation_duration,
                    R.string.pk_bg_image_portrait,
                    R.string.pk_bg_image_landscape,
                    R.string.pk_bg_image_opacity,
                    R.string.pk_timer_font_import,
                    R.string.pk_appearance_timer,
                    R.string.pk_appearance_scramble,
                    R.string.pk_appearance_ao5,
                    R.string.pk_appearance_ao12,
                    R.string.pk_appearance_ao100,
                    R.string.pk_appearance_status,
                    R.string.pk_appearance_laps};

            for (int prefId : listenerPrefIds) {
                Preference p = findPreference(getString(prefId));
                if (p != null) {
                    p.setOnPreferenceClickListener(clickListener);
                } else {
                    Log.w("SettingsActivity", "Preference not found at this root: " + getString(prefId));
                }
            }

            Preference fontImportPreference = findPreference(getString(R.string.pk_timer_font_import));
            if (fontImportPreference instanceof ImportFontPreference) {
                ((ImportFontPreference) fontImportPreference)
                        .setOnImportFontClickListener(this::launchFontPicker);
            } else if (fontImportPreference != null) {
                Log.e(TAG, "Font import preference has an unexpected type: "
                        + fontImportPreference.getClass().getName());
            }

            updateTimerFontPreferenceEntries();
            mainScreen = getPreferenceScreen();
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            if (savedInstanceState == null) {
                view.setAlpha(0f);
                view.setTranslationY(ThemeUtils.dpToPix(requireContext(), 12));
                view.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(360L)
                        .setInterpolator(new OvershootInterpolator(0.35f))
                        .start();
            }

            // Return to the previous page on back press
            requireActivity().getOnBackPressedDispatcher()
                    .addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
                        @Override
                        public void handleOnBackPressed() {
                            if (lastPreferenceScreen != null && getPreferenceScreen() != lastPreferenceScreen) {
                                setPreferenceScreen(lastPreferenceScreen);
                                // consume and return
                                return;
                            }

                            // Not consumed -> temporarily disable this callback, delegate to the default back behavior,
                            setEnabled(false);
                            requireActivity().getOnBackPressedDispatcher().onBackPressed();
                            // then re-enable it afterward so it can handle future back presses again
                            setEnabled(true);
                        }
                    });
        }

        @Override
        public void onResume() {
            super.onResume();
            // Set the Inspection Alert preference summary to display the correct information
            // about time elapsed depending on user's current inspection duration
            updateInspectionAlertText();
            updatePhaseNumText();
        }

        private void updateInspectionAlertText() {
            inspectionDuration = Prefs.getInt(R.string.pk_inspection_time, 15);

            Preference inspectionPreference = findPreference(getString(R.string.pk_inspection_alert_enabled));
            if (inspectionPreference != null)
                    inspectionPreference.setSummary(getString(R.string.pref_inspection_alert_summary,
                            inspectionDuration == 15 ? 8 : (int) (inspectionDuration * 0.5f),
                            inspectionDuration == 15 ? 12 : (int) (inspectionDuration * 0.8f)));
        }

        private void updatePhaseNumText() {
            int phaseNum = Prefs.getInt(R.string.pk_multi_phase_num, defaultPhaseNum);

            Preference pahseNumPreference = findPreference(getString(R.string.pk_multi_phase_num));
            if (pahseNumPreference != null)
                pahseNumPreference.setSummary(getString(R.string.multiPhaseNumSummary, phaseNum, maximumPhaseNum));
        }

        @Override
        public void onNavigateToScreen(PreferenceScreen preferenceScreen) {
            lastPreferenceScreen = getPreferenceScreen();
            setPreferenceScreen(preferenceScreen);
        }

        private void createNumberDialog(@StringRes int title, final int prefKeyResID) {
            ThemeUtils.roundAndShowDialog(mContext, new MaterialDialog.Builder(mContext)
                    .title(title)
                    .input("", String.valueOf(Prefs.getInt(prefKeyResID, 15)),
                           (dialog, input) -> {
                               try {
                                   final int time = Integer.parseInt(input.toString());

                                   Prefs.edit().putInt(prefKeyResID, time).apply();


                               } catch (NumberFormatException e) {
                                   Toast.makeText(getActivity(),
                                           R.string.invalid_time, Toast.LENGTH_SHORT).show();
                               }
                           })
                    .inputType(InputType.TYPE_CLASS_NUMBER)
                    .positiveText(R.string.action_done)
                    .negativeText(R.string.action_cancel)
                    .neutralText(R.string.action_default)
                    .onNeutral((dialog, which) -> Prefs.edit().putInt(prefKeyResID, 15).apply())
                    .onAny((dialog, which) -> updateInspectionAlertText())
                    .build());
        }

        private void createPhaseNumDialog(@StringRes int title, final int prefKeyResID) {
            MaterialDialog dialog = ThemeUtils.roundDialog(mContext, new MaterialDialog.Builder(mContext)
                    .title(title)
                    .input("", String.valueOf(Prefs.getInt(prefKeyResID, defaultPhaseNum)),
                            (dialog1, input) -> {
                                try {
                                    int num = Integer.parseInt(input.toString());
                                    num = Math.max(Math.min(num, maximumPhaseNum),2);
                                    Prefs.edit().putInt(prefKeyResID, num).apply();
                                } catch (NumberFormatException e) {
                                    Toast.makeText(getActivity(),
                                            R.string.invalid_time, Toast.LENGTH_SHORT).show();
                                }
                            })
                    .inputType(InputType.TYPE_CLASS_NUMBER)
                    .positiveText(R.string.action_done)
                    .negativeText(R.string.action_cancel)
                    .onAny((dialog1, which) -> updatePhaseNumText())
                    .build());
            EditText editText = dialog.getInputEditText();
            if (editText != null) {
                editText.setFilters(new InputFilter[] {new InputFilter.LengthFilter(1)});
            }
            dialog.show();
        }

        private void createSeekDialog(@StringRes int prefKeyResID,
                                      int minValue, int maxValue, @IntegerRes int defaultValueRes,
                                      String formatText) {

            final View dialogView = LayoutInflater.from(
                    getActivity()).inflate(R.layout.dialog_settings_progress, null);
            final AppCompatSeekBar seekBar = dialogView.findViewById(R.id.seekbar);
            final TextView text = dialogView.findViewById(R.id.text);

            int defaultValue = getContext().getResources().getInteger(defaultValueRes);

            seekBar.setMax(maxValue);
            seekBar.setProgress(Prefs.getInt(prefKeyResID, defaultValue));

            text.setText(String.format(formatText, seekBar.getProgress()));

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    text.setText(String.format(formatText, seekBar.getProgress()));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            ThemeUtils.roundAndShowDialog(mContext, new MaterialDialog.Builder(mContext)
                    .customView(dialogView, true)
                    .positiveText(R.string.action_done)
                    .negativeText(R.string.action_cancel)
                    .onPositive((dialog, which) -> {
                        final int seekProgress = seekBar.getProgress();

                        Prefs.edit()
                                .putInt(prefKeyResID, seekProgress > minValue ? seekProgress : minValue)
                                .apply();
                    })
                    .neutralText(R.string.action_default)
                    .onNeutral((dialog, which) -> Prefs.edit().putInt(prefKeyResID, defaultValue).apply())
                    .build());
        }

        private MaterialDialog createAverageSeekDialog(@StringRes int prefKeyResID,
                                             int minValue, int maxValue, @IntegerRes int defaultValueRes) {

            final View dialogView = LayoutInflater.from(
                    getActivity()).inflate(R.layout.dialog_settings_progress, null);
            final AppCompatSeekBar seekBar = dialogView.findViewById(R.id.seekbar);

            int defaultValue = getContext().getResources().getInteger(defaultValueRes);

            seekBar.setMax(maxValue);
            seekBar.setProgress(Prefs.getInt(prefKeyResID, defaultValue));

            return ThemeUtils.roundDialog(mContext, new MaterialDialog.Builder(mContext)
                    .customView(dialogView, true)
                    .positiveText(R.string.action_done)
                    .negativeText(R.string.action_cancel)
                    .onPositive((dialog, which) -> {
                        final int seekProgress = seekBar.getProgress();

                        Prefs.edit()
                                .putInt(prefKeyResID, seekProgress > minValue ? seekProgress : minValue)
                                .apply();
                    })
                    .neutralText(R.string.action_default)
                    .onNeutral((dialog, which) -> Prefs.edit().putInt(prefKeyResID, defaultValue).apply())
                    .build());
        }

        private int getTrim(int avg, int trim) {
            return ((int) Math.ceil(avg * (trim / 100f)) * 2);
        }

        private int getAcceptableDNFCount(int avg, int trim) {
            return (int) Math.ceil(avg * (trim / 100f));
        }

        private void createSeekTextSizeDialog(
                final int prefKeyResID, int defaultTextSize, String showText, boolean bold) {
            final View dialogView = LayoutInflater.from(
                    getActivity()).inflate(R.layout.dialog_settings_progress, null);
            final AppCompatSeekBar seekBar
                    = (AppCompatSeekBar) dialogView.findViewById(R.id.seekbar);
            final TextView text = (TextView) dialogView.findViewById(R.id.text);
            seekBar.setMax(300);
            seekBar.setProgress(Prefs.getInt(prefKeyResID, 100));

            text.setTextSize(TypedValue.COMPLEX_UNIT_SP, defaultTextSize);
            final float defaultTextSizePx = text.getTextSize();
            text.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    defaultTextSizePx * (seekBar.getProgress() / 100f));
            if (bold)
                text.setTypeface(Typeface.DEFAULT_BOLD);
            text.setText(showText);

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    text.setTextSize(TypedValue.COMPLEX_UNIT_PX, defaultTextSizePx * (i / 100f));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });

            ThemeUtils.roundAndShowDialog(mContext, new MaterialDialog.Builder(mContext)
                    .customView(dialogView, true)
                    .positiveText(R.string.action_done)
                    .negativeText(R.string.action_cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(
                                @NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            final int seekProgress = seekBar.getProgress();

                            Prefs.edit()
                                    .putInt(prefKeyResID, seekProgress > 10 ? seekProgress : 10)
                                    .apply();
                        }
                    })
                    .neutralText(R.string.action_default)
                    .onNeutral(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(
                                @NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            Prefs.edit().putInt(prefKeyResID, 100).apply();
                        }
                    })
                    .build());
        }

        private void createImageSeekDialog(final int prefKeyResID, @StringRes int title) {
            final View dialogView = LayoutInflater.from(
                    getActivity()).inflate(R.layout.dialog_settings_progress_image, null);
            final AppCompatSeekBar seekBar
                    = (AppCompatSeekBar) dialogView.findViewById(R.id.seekbar);
            final View image = dialogView.findViewById(R.id.image);
            seekBar.setMax(300);
            seekBar.setProgress(Prefs.getInt(prefKeyResID, 100));

            final int defaultWidth = image.getLayoutParams().width;
            final int defaultHeight = image.getLayoutParams().height;

            image.getLayoutParams().width *= (seekBar.getProgress() / 100f);
            image.getLayoutParams().height *= (seekBar.getProgress() / 100f);


            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    LinearLayout.LayoutParams params
                            = (LinearLayout.LayoutParams) image.getLayoutParams();
                    params.width = (int) (defaultWidth * (i / 100f));
                    params.height = (int) (defaultHeight * (i / 100f));
                    image.setLayoutParams(params);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });

            ThemeUtils.roundAndShowDialog(mContext, new MaterialDialog.Builder(mContext)
                    .customView(dialogView, true)
                    .positiveText(R.string.action_done)
                    .negativeText(R.string.action_cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(
                                @NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            final int seekProgress = seekBar.getProgress();

                            Prefs.edit()
                                    .putInt(prefKeyResID, seekProgress > 10 ? seekProgress : 10)
                                    .apply();
                        }
                    })
                    .neutralText(R.string.action_default)
                    .onNeutral(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(
                                @NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            Prefs.edit().putInt(prefKeyResID, 100).apply();
                        }
                    })
                    .build());
        }

        private void launchFontPicker() {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES,
                    new String[]{"font/*", "application/font-sfnt", "font/ttf", "font/otf",
                            "application/x-font-ttf", "application/x-font-otf",
                            "application/x-font-opentype", "application/vnd.ms-opentype",
                            "application/octet-stream"});
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                importFontLauncher.launch(intent);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "No document picker is available for importing fonts", e);
                Toast.makeText(requireContext(), R.string.timer_font_import_failed,
                        Toast.LENGTH_LONG).show();
            }
        }

        private void importCustomFont(@NonNull Uri uri) {
            String displayName = getDocumentName(uri);
            String lowerName = displayName.toLowerCase(Locale.ROOT);
            String extension = lowerName.endsWith(".ttf") ? ".ttf"
                    : lowerName.endsWith(".otf") ? ".otf" : null;
            if (extension == null) {
                Toast.makeText(requireContext(), R.string.timer_font_import_failed,
                        Toast.LENGTH_LONG).show();
                return;
            }

            File fontDirectory = TimerFontUtils.getCustomFontDirectory(requireContext());
            if (!fontDirectory.isDirectory() && !fontDirectory.mkdirs()) {
                Log.e(TAG, "Could not create custom font directory: " + fontDirectory);
                Toast.makeText(requireContext(), R.string.timer_font_import_failed,
                        Toast.LENGTH_LONG).show();
                return;
            }

            String fileName = UUID.randomUUID().toString() + extension;
            File temporaryFile = new File(fontDirectory, fileName + ".tmp");
            File fontFile = new File(fontDirectory, fileName);
            try {
                try (InputStream input = requireContext().getContentResolver().openInputStream(uri)) {
                    if (input == null) {
                        throw new IOException("No data available for selected font");
                    }
                    try (OutputStream output = new java.io.FileOutputStream(temporaryFile)) {
                        byte[] buffer = new byte[32 * 1024];
                        int count;
                        while ((count = input.read(buffer)) != -1) {
                            output.write(buffer, 0, count);
                        }
                        output.flush();
                    }
                }
                Typeface.createFromFile(temporaryFile.getAbsolutePath());
                if (!temporaryFile.renameTo(fontFile)) {
                    throw new IOException("Could not save imported font");
                }
                Prefs.getPrefs().edit()
                        .putString(TimerFontUtils.getDisplayNamePreferenceKey(fileName), displayName)
                        .apply();
                updateTimerFontPreferenceEntries();
                Toast.makeText(requireContext(), R.string.timer_font_import_success,
                        Toast.LENGTH_SHORT).show();
            } catch (IOException | RuntimeException e) {
                Log.e(TAG, "Could not import custom font " + displayName, e);
                if (temporaryFile.exists() && !temporaryFile.delete()) {
                    Log.w(TAG, "Could not remove invalid temporary font file " + temporaryFile);
                }
                if (fontFile.exists() && !fontFile.delete()) {
                    Log.w(TAG, "Could not remove unregistered font file " + fontFile);
                }
                Prefs.getPrefs().edit()
                        .remove(TimerFontUtils.getDisplayNamePreferenceKey(fileName))
                        .apply();
                Toast.makeText(requireContext(), R.string.timer_font_import_failed,
                        Toast.LENGTH_LONG).show();
            }
        }

        @NonNull
        private String getDocumentName(@NonNull Uri uri) {
            try (Cursor cursor = requireContext().getContentResolver().query(
                    uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (column >= 0) {
                        String name = cursor.getString(column);
                        if (name != null && !name.trim().isEmpty()) {
                            return new File(name).getName();
                        }
                    }
                }
            } catch (RuntimeException e) {
                Log.w(TAG, "Could not read the selected font's display name", e);
            }
            String lastSegment = uri.getLastPathSegment();
            return lastSegment == null ? "" : lastSegment;
        }

        private void showComponentAppearanceDialog(@NonNull String component, int titleRes) {
            Context context = requireContext();
            String prefsPrefix = "timerAppearance_" + component + "_";
            List<String> fontValues = new ArrayList<>();
            List<String> fontLabels = new ArrayList<>();
            addFontChoice(fontValues, fontLabels, "default", R.string.timer_appearance_default_font);
            addFontChoice(fontValues, fontLabels, "lato", R.string.timer_font_lato);
            addFontChoice(fontValues, fontLabels, "quicksand", R.string.timer_font_quicksand);
            addFontChoice(fontValues, fontLabels, "sans_serif", R.string.timer_font_sans_serif);
            addFontChoice(fontValues, fontLabels, "monospace", R.string.timer_font_monospace);

            for (File font : getImportedFonts(context)) {
                fontValues.add(TimerFontUtils.getCustomFontId(font.getName()));
                fontLabels.add(Prefs.getPrefs().getString(
                        TimerFontUtils.getDisplayNamePreferenceKey(font.getName()), font.getName()));
            }

            LinearLayout panel = new LinearLayout(context);
            panel.setOrientation(LinearLayout.VERTICAL);
            int horizontalPadding = ThemeUtils.dpToPix(context, 24);
            panel.setPadding(horizontalPadding, 0, horizontalPadding, 0);

            TextView fontLabel = new TextView(context);
            fontLabel.setText(R.string.timer_appearance_font);
            panel.addView(fontLabel);

            Spinner fontSpinner = new Spinner(context);
            ArrayAdapter<String> fontAdapter = new ArrayAdapter<>(context,
                    android.R.layout.simple_spinner_item, fontLabels);
            fontAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            fontSpinner.setAdapter(fontAdapter);
            String selectedFont = Prefs.getPrefs().getString(prefsPrefix + "font", "default");
            int selectedFontIndex = fontValues.indexOf(selectedFont);
            fontSpinner.setSelection(Math.max(0, selectedFontIndex));
            panel.addView(fontSpinner);

            TextView sizeLabel = new TextView(context);
            int size = Prefs.getPrefs().getInt(prefsPrefix + "size", 100);
            sizeLabel.setText(getString(R.string.timer_appearance_size, size));
            panel.addView(sizeLabel);

            SeekBar sizeSeekBar = new SeekBar(context);
            sizeSeekBar.setMax(150);
            sizeSeekBar.setProgress(Math.max(0, Math.min(150, size - 50)));
            sizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    sizeLabel.setText(getString(R.string.timer_appearance_size, progress + 50));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
            panel.addView(sizeSeekBar);

            CheckBox boldCheckBox = new CheckBox(context);
            boldCheckBox.setText(R.string.timer_appearance_bold);
            boolean defaultBold = "timer".equals(component) || "laps".equals(component)
                    || "status".equals(component);
            boldCheckBox.setChecked(Prefs.getPrefs().getBoolean(prefsPrefix + "bold", defaultBold));
            panel.addView(boldCheckBox);

            CheckBox italicCheckBox = new CheckBox(context);
            italicCheckBox.setText(R.string.timer_appearance_italic);
            italicCheckBox.setChecked(Prefs.getPrefs().getBoolean(prefsPrefix + "italic", false));
            panel.addView(italicCheckBox);

            TextView colorLabel = new TextView(context);
            colorLabel.setText(R.string.timer_appearance_color);
            panel.addView(colorLabel);
            String[] colorValues = {"theme", "accent", "white", "black"};
            String[] colorLabels = {
                    getString(R.string.timer_appearance_color_theme),
                    getString(R.string.timer_appearance_color_accent),
                    getString(R.string.timer_appearance_color_white),
                    getString(R.string.timer_appearance_color_black)
            };
            Spinner colorSpinner = new Spinner(context);
            ArrayAdapter<String> colorAdapter = new ArrayAdapter<>(context,
                    android.R.layout.simple_spinner_item, colorLabels);
            colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            colorSpinner.setAdapter(colorAdapter);
            String color = Prefs.getPrefs().getString(prefsPrefix + "color", "theme");
            int selectedColorIndex = java.util.Arrays.asList(colorValues).indexOf(color);
            colorSpinner.setSelection(Math.max(0, selectedColorIndex));
            panel.addView(colorSpinner);

            MaterialDialog dialog = ThemeUtils.roundDialog(context, new MaterialDialog.Builder(context)
                    .title(getString(R.string.timer_appearance_dialog_title, getString(titleRes)))
                    .customView(panel, true)
                    .positiveText(R.string.action_done)
                    .negativeText(R.string.action_cancel)
                    .onPositive((materialDialog, which) -> {
                        int fontIndex = fontSpinner.getSelectedItemPosition();
                        Prefs.getPrefs().edit()
                                .putString(prefsPrefix + "font", fontValues.get(fontIndex))
                                .putInt(prefsPrefix + "size", sizeSeekBar.getProgress() + 50)
                                .putBoolean(prefsPrefix + "bold", boldCheckBox.isChecked())
                                .putBoolean(prefsPrefix + "italic", italicCheckBox.isChecked())
                                .putString(prefsPrefix + "color",
                                        colorValues[colorSpinner.getSelectedItemPosition()])
                                .apply();
                    })
                    .build());
            dialog.show();
        }

        private void addFontChoice(List<String> values, List<String> labels, String value, int labelRes) {
            values.add(value);
            labels.add(getString(labelRes));
        }

        private void updateTimerFontPreferenceEntries() {
            Preference preference = findPreference(getString(R.string.pk_timer_font));
            if (!(preference instanceof ListPreference)) {
                if (preference != null) {
                    Log.e(TAG, "Timer font preference has an unexpected type: "
                            + preference.getClass().getName());
                }
                return;
            }

            List<CharSequence> entries = new ArrayList<>();
            List<CharSequence> entryValues = new ArrayList<>();
            CharSequence[] builtInEntries = getResources().getTextArray(R.array.timer_font_entries);
            CharSequence[] builtInValues = getResources().getTextArray(R.array.timer_font_values);
            if (builtInEntries.length != builtInValues.length) {
                Log.e(TAG, "Built-in timer font entries and values have different lengths");
                return;
            }
            for (int i = 0; i < builtInEntries.length; i++) {
                entries.add(builtInEntries[i]);
                entryValues.add(builtInValues[i]);
            }
            for (File font : getImportedFonts(requireContext())) {
                entries.add(Prefs.getPrefs().getString(
                        TimerFontUtils.getDisplayNamePreferenceKey(font.getName()), font.getName()));
                entryValues.add(TimerFontUtils.getCustomFontId(font.getName()));
            }
            ((ListPreference) preference).setEntries(entries.toArray(new CharSequence[0]));
            ((ListPreference) preference).setEntryValues(entryValues.toArray(new CharSequence[0]));
        }

        @NonNull
        private File[] getImportedFonts(@NonNull Context context) {
            File fontDirectory = TimerFontUtils.getCustomFontDirectory(context);
            File[] fonts = fontDirectory.listFiles((directory, name) ->
                    name.toLowerCase(Locale.ROOT).endsWith(".ttf")
                            || name.toLowerCase(Locale.ROOT).endsWith(".otf"));
            if (fonts == null) {
                return new File[0];
            }
            java.util.Arrays.sort(fonts, (left, right) ->
                    left.getName().compareToIgnoreCase(right.getName()));
            return fonts;
        }

        private void createImagePickerDialog(boolean isPortrait) {
            isBgImageSelectPortrait = isPortrait;

            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

            openDocLauncher.launch(intent);
        }

        private void onBackgroundMediaPicked(@NonNull Uri uri, int grantedFlags) {
            final String fileName;
            final int key;

            String mimeType = requireContext().getContentResolver().getType(uri);
            if (mimeType == null
                    || (!mimeType.startsWith("image/") && !mimeType.startsWith("video/"))) {
                Toast.makeText(requireContext(), R.string.background_media_type_not_supported,
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (isBgImageSelectPortrait) {
                key = R.string.pk_bg_image_portrait;
            } else {
                key = R.string.pk_bg_image_landscape;
            }

            if (mimeType.startsWith("video/")) {
                int readPermission = grantedFlags & Intent.FLAG_GRANT_READ_URI_PERMISSION;
                if (readPermission == 0) {
                    Log.e(TAG, "Selected background video did not grant read access");
                    Toast.makeText(requireContext(), R.string.background_video_access_fail,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    requireContext().getContentResolver()
                            .takePersistableUriPermission(uri, readPermission);
                } catch (SecurityException e) {
                    Log.e(TAG, "Could not retain access to selected background video", e);
                    Toast.makeText(requireContext(), R.string.background_video_access_fail,
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                Prefs.edit().putString(key, uri.toString()).apply();
                return;
            }

            String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            if (extension == null) {
                Toast.makeText(requireContext(), R.string.background_media_type_not_supported,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            fileName = isBgImageSelectPortrait
                    ? "background_portrait." + extension : "background_landscape." + extension;
            File dst = new File(requireContext().getFilesDir(), fileName);

            Log.d(TAG, "onImagePicked copy start : " + fileName);
            try (InputStream source = requireContext().getContentResolver().openInputStream(uri)) {
                if (source == null) {
                    throw new IOException("No data available for selected background image");
                }
                if (Build.VERSION.SDK_INT >= 26) {
                    java.nio.file.Files.copy(source, dst.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } else {
                    try (OutputStream out = new java.io.FileOutputStream(dst)) {
                        byte[] buf = new byte[64 * 1024]; // 64KB
                        int n;
                        while ((n = source.read(buf)) != -1) out.write(buf, 0, n);
                        out.flush();
                    }
                }

                // set internal filename into preference
                Prefs.edit()
                        .putString(key, dst.getAbsolutePath())
                        .apply();
            } catch (IOException e) {
                Log.e(TAG, "Failed to copy selected background image", e);
                Toast.makeText(requireContext(), R.string.image_load_fail, Toast.LENGTH_SHORT).show();
            }
            Log.d(TAG, "Background image copy finished : " + fileName);
        }
    }
}
