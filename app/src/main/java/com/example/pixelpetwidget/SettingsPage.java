package com.example.pixelpetwidget;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.pixelpetwidget.sound.SoundManager;
import com.example.pixelpetwidget.ui.pixel.PixelDialog;
import com.example.pixelpetwidget.ui.pixel.PixelSlider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Страница настроек: звук и громкость, напоминание, ночной режим, сезонное оформление, язык, о приложении. */
final class SettingsPage {

    interface Host {
        void requestNotificationPermission();

        void switchLanguage(String lang);

        String currentLanguage();

        /** Whether the user must be able to change their consent to ads (GDPR and the like). */
        boolean adPrivacyOptionsRequired();

        void showAdPrivacyOptions();
    }

    private final AppCompatActivity activity;
    private final Host host;
    private final SoundManager sound;
    private final CompoundButton soundSwitch;
    private final PixelSlider volume;
    private final TextView volumeTitle;
    private final CompoundButton reminders;
    private final CompoundButton night;
    private final CompoundButton seasonal;
    private final View languageRu;
    private final View languageEn;

    SettingsPage(AppCompatActivity activity, Host host) {
        this.activity = activity;
        this.host = host;
        this.sound = SoundManager.get(activity);
        soundSwitch = activity.findViewById(R.id.switch_sound);
        volume = activity.findViewById(R.id.volume_slider);
        volumeTitle = activity.findViewById(R.id.volume_title);
        reminders = activity.findViewById(R.id.switch_reminders);
        night = activity.findViewById(R.id.switch_night);
        seasonal = activity.findViewById(R.id.switch_seasonal);
        languageRu = activity.findViewById(R.id.language_ru);
        languageEn = activity.findViewById(R.id.language_en);

        volume.setMax(SoundManager.MAX_VOLUME);
        volume.setListener((value, fromUser) -> {
            if (fromUser) {
                sound.setVolume(value);
            }
            showVolume(value);
        });
        languageRu.setOnClickListener(v -> host.switchLanguage("ru"));
        languageEn.setOnClickListener(v -> host.switchLanguage("en"));
        activity.findViewById(R.id.about_button).setOnClickListener(v -> showAbout());
        activity.findViewById(R.id.ad_privacy_button).setOnClickListener(v -> host.showAdPrivacyOptions());
    }

    /** Показывает сохранённые значения (при каждом открытии страницы). */
    void bind() {
        activity.findViewById(R.id.ad_privacy_button)
            .setVisibility(host.adPrivacyOptionsRequired() ? View.VISIBLE : View.GONE);
        set(soundSwitch, sound.soundOn(), (b, on) -> {
            sound.setSoundOn(on);
            volume.setEnabled(on);
        });
        volume.setEnabled(sound.soundOn());
        volume.setProgress(sound.volume());
        showVolume(sound.volume());
        set(reminders, PetSettings.reminders(activity), (b, on) -> {
            if (on) {
                turnOnReminders();
            } else {
                setReminders(false);
            }
        });
        set(night, PetSettings.nightMode(activity), (b, on) -> {
            PetSettings.setNightMode(activity, on);
            widgetsChanged();
        });
        set(seasonal, PetSettings.seasonalDecor(activity), (b, on) -> {
            PetSettings.setSeasonalDecor(activity, on);
            widgetsChanged();
        });
        boolean russian = "ru".equals(host.currentLanguage());
        languageRu.setSelected(russian);
        languageEn.setSelected(!russian);
    }

    private void showVolume(int value) {
        volumeTitle.setText(activity.getString(R.string.settings_volume_level, value, SoundManager.MAX_VOLUME));
    }

    /** Включает напоминания; на Android 13+ сначала спрашивает разрешение на уведомления. */
    void turnOnReminders() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
            host.requestNotificationPermission();
            return;
        }
        setReminders(true);
    }

    /** Ответ на системный запрос разрешения. */
    void onNotificationPermission(boolean granted) {
        setReminders(granted);
        if (!granted) {
            PixelDialog.with(activity).message(R.string.reminders_denied).error().show();
        }
    }

    private void setReminders(boolean on) {
        PetSettings.setReminders(activity, on);
        CareScheduler.scheduleReminder(activity);
        bind();
    }

    private void widgetsChanged() {
        PixelCatWidget.updateAllWidgets(activity);
        CareScheduler.scheduleCareTick(activity);
    }

    private void showAbout() {
        String version;
        try {
            version = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            version = "";
        }
        PixelDialog about = PixelDialog.with(activity)
            .title(R.string.app_name)
            .message(activity.getString(R.string.about_text, version))
            .positive(R.string.close, null)
            .negative(R.string.about_licence, this::showLicence);
        // The privacy policy's address lives in one place: R.string.privacy_policy_url (empty: no button)
        String policy = activity.getString(R.string.privacy_policy_url).trim();
        if (!policy.isEmpty()) {
            about.extra(R.string.privacy_policy, () -> openLink(policy));
        }
        about.show();
    }

    /** Opens a web address in the browser; says so when there is none to open it. */
    private void openLink(String url) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            PixelDialog.with(activity)
                .message(R.string.no_browser)
                .error()
                .show();
        }
    }

    private void showLicence() {
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                activity.getAssets().open("licenses/Tiny5-OFL.txt"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line).append('\n');
            }
        } catch (IOException e) {
            return;
        }
        PixelDialog.with(activity)
            .title(R.string.licence_title)
            .message(paragraphs(text.toString()))
            .positive(R.string.close, null)
            .show();
    }

    /**
     * В файле лицензии строки переносятся на 70 символах — на телефоне это рваные строки.
     * Склеивает строки каждого абзаца и убирает линейки из дефисов.
     */
    static String paragraphs(String text) {
        StringBuilder out = new StringBuilder();
        for (String paragraph : text.replace("\r", "").split("\n\\s*\n")) {
            String joined = paragraph.replaceAll("(?m)^-+\\s*$", "").trim()
                .replaceAll("\\s*\n\\s*", " ");
            if (!joined.isEmpty()) {
                out.append(out.length() > 0 ? "\n\n" : "").append(joined);
            }
        }
        return out.toString();
    }

    /** Ставит переключатель, не вызывая его слушателя. */
    private static void set(CompoundButton view, boolean checked, CompoundButton.OnCheckedChangeListener listener) {
        view.setOnCheckedChangeListener(null);
        view.setChecked(checked);
        view.setOnCheckedChangeListener(listener);
    }
}
