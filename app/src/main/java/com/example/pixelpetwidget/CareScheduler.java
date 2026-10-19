package com.example.pixelpetwidget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.TimeZone;

/**
 * Two inexact alarms. The care tick redraws the widgets when something visible changes
 * (a pet gets hungry or lonely, night falls or ends, a new day or season starts); it does
 * not wake the device, the widgets are only seen when it is on anyway. The reminder fires
 * once a day at REMINDER_HOUR if the player turned reminders on.
 */
final class CareScheduler {

    private static final String TAG = "CareScheduler";
    static final String ACTION_CARE_TICK = "com.example.pixelpetwidget.CARE_TICK";
    static final int NIGHT_STARTS = 22;
    static final int NIGHT_ENDS = 7;
    static final int REMINDER_HOUR = 19;
    private static final long REMINDER_WINDOW_MS = 30 * 60 * 1000;
    private static final int CARE_TICK_REQUEST = 2;
    private static final int REMINDER_REQUEST = 3;

    private CareScheduler() {}

    static boolean isNight(long now) {
        int hour = DayClock.hourOfDay(now, TimeZone.getDefault());
        return hour >= NIGHT_STARTS || hour < NIGHT_ENDS;
    }

    /** The next moment the widgets need redrawing without anyone touching them. */
    static long nextCareTick(Context context, long now) {
        TimeZone zone = TimeZone.getDefault();
        long next = CareStore.nextNeedChange(context, now);
        next = Math.min(next, DayClock.nextTimeAt(now, 0, zone));            // new day, maybe a season
        if (PetSettings.nightMode(context)) {
            next = Math.min(next, DayClock.nextTimeAt(now, NIGHT_STARTS, zone));
            next = Math.min(next, DayClock.nextTimeAt(now, NIGHT_ENDS, zone));
        }
        // Inexact alarms may be late anyway; a minute past the change is fine
        return next + 60 * 1000;
    }

    static void scheduleCareTick(Context context) {
        if (CareStore.activePetTypes(context).isEmpty()) {
            cancelCareTick(context);
            return;
        }
        try {
            AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarms.set(AlarmManager.RTC, nextCareTick(context, System.currentTimeMillis()), careTickIntent(context));
        } catch (RuntimeException e) {
            Log.e(TAG, "Error scheduling care tick", e);
        }
    }

    static void cancelCareTick(Context context) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarms.cancel(careTickIntent(context));
    }

    private static PendingIntent careTickIntent(Context context) {
        Intent intent = new Intent(context, PixelCatWidget.class);
        intent.setAction(ACTION_CARE_TICK);
        return PendingIntent.getBroadcast(context, CARE_TICK_REQUEST, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** Schedules tomorrow's (or today's) reminder, or cancels it if reminders are off. */
    static void scheduleReminder(Context context) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent intent = reminderIntent(context);
        if (!PetSettings.reminders(context)) {
            alarms.cancel(intent);
            return;
        }
        long at = DayClock.nextTimeAt(System.currentTimeMillis(), REMINDER_HOUR, TimeZone.getDefault());
        try {
            alarms.setWindow(AlarmManager.RTC_WAKEUP, at, REMINDER_WINDOW_MS, intent);
        } catch (RuntimeException e) {
            Log.e(TAG, "Error scheduling reminder", e);
        }
    }

    private static PendingIntent reminderIntent(Context context) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        return PendingIntent.getBroadcast(context, REMINDER_REQUEST, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
