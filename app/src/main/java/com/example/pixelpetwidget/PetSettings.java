package com.example.pixelpetwidget;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;

import java.util.TimeZone;

/** Player options from the settings card on the main screen. */
final class PetSettings {

    private static final String PREFS = "PixelPetSettings";
    private static final String REMINDERS = "reminders";
    private static final String REMINDERS_OFFERED = "reminders_offered";
    private static final String NIGHT_MODE = "night_mode";
    private static final String SEASONAL_DECOR = "seasonal_decor";
    private static final String REVIEW_ASKED = "review_asked";
    // Debug builds only: force a season ("halloween", "winter", "" for none) or night ("true"/"false")
    private static final String DEBUG_SEASON = "debug_season";
    private static final String DEBUG_NIGHT = "debug_night";

    private PetSettings() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Daily reminder; off until the player turns it on. */
    static boolean reminders(Context context) {
        return prefs(context).getBoolean(REMINDERS, false);
    }

    static void setReminders(Context context, boolean on) {
        prefs(context).edit().putBoolean(REMINDERS, on).putBoolean(REMINDERS_OFFERED, true).apply();
    }

    /** Whether the app already suggested reminders once. */
    static boolean remindersOffered(Context context) {
        return prefs(context).getBoolean(REMINDERS_OFFERED, false);
    }

    static void setRemindersOffered(Context context) {
        prefs(context).edit().putBoolean(REMINDERS_OFFERED, true).apply();
    }

    /** Darker widget and sleepy pets at night. */
    static boolean nightMode(Context context) {
        return prefs(context).getBoolean(NIGHT_MODE, true);
    }

    static void setNightMode(Context context, boolean on) {
        prefs(context).edit().putBoolean(NIGHT_MODE, on).apply();
    }

    /** Pumpkins and snow on the widget during the seasons. */
    static boolean seasonalDecor(Context context) {
        return prefs(context).getBoolean(SEASONAL_DECOR, true);
    }

    static void setSeasonalDecor(Context context, boolean on) {
        prefs(context).edit().putBoolean(SEASONAL_DECOR, on).apply();
    }

    /** The season right now, or null. */
    static String season(Context context, long now) {
        String forced = debugOverride(context, DEBUG_SEASON);
        if (forced != null) {
            return forced.isEmpty() ? null : forced;
        }
        int[] date = DayClock.monthAndDay(now, TimeZone.getDefault());
        return Wardrobe.season(date[0], date[1]);
    }

    /** Whether it is night on the widget's clock. */
    static boolean isNight(Context context, long now) {
        String forced = debugOverride(context, DEBUG_NIGHT);
        return forced != null ? Boolean.parseBoolean(forced) : CareScheduler.isNight(now);
    }

    /** Test hooks set with adb on debug builds; release builds ignore them. */
    private static String debugOverride(Context context, String key) {
        if ((context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0) {
            return null;
        }
        return prefs(context).getString(key, null);
    }

    static boolean reviewAsked(Context context) {
        return prefs(context).getBoolean(REVIEW_ASKED, false);
    }

    static void setReviewAsked(Context context) {
        prefs(context).edit().putBoolean(REVIEW_ASKED, true).apply();
    }
}
