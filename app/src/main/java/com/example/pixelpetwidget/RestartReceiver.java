package com.example.pixelpetwidget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * After a reboot, an app update or a change of the clock every pet is put to sleep (their
 * awake time no longer means anything), and the care and reminder alarms are set again.
 */
public class RestartReceiver extends BroadcastReceiver {

    private static final String TAG = "RestartReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received broadcast: " + action);

        if (action == null) return;

        if (action.equals(Intent.ACTION_BOOT_COMPLETED) ||
            action.equals("android.intent.action.QUICKBOOT_POWERON") ||
            action.equals(Intent.ACTION_MY_PACKAGE_REPLACED) ||
            action.equals(Intent.ACTION_TIME_CHANGED) ||
            action.equals(Intent.ACTION_TIMEZONE_CHANGED)) {

            if (PetLife.widgetIds(context).length > 0) {
                // Asleep, and redrawn: moods, night and seasons depend on the clock
                PetLife.restart(context);
                CareScheduler.scheduleCareTick(context);
            }
            CareScheduler.scheduleReminder(context);
        }
    }
}
