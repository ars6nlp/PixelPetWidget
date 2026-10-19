package com.example.pixelpetwidget;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

/**
 * The daily reminder (only if the player turned it on). It says something only when there
 * is a reason to come back: an unclaimed daily gift or a hungry pet on the home screen.
 */
public class ReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL = "pet_reminders";
    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onReceive(Context context, Intent intent) {
        // Keep the daily rhythm going whatever happens below
        CareScheduler.scheduleReminder(context);
        if (!PetSettings.reminders(context) || !canNotify(context)) {
            return;
        }

        long now = System.currentTimeMillis();
        String title;
        String text;
        if (DailyRewards.canClaim(CareStore.rewards(context), DayClock.today())) {
            title = context.getString(R.string.reminder_gift_title);
            text = context.getString(R.string.reminder_gift_text);
        } else {
            PetSpec hungry = null;
            for (int petType : CareStore.activePetTypes(context)) {
                if (CareStore.need(context, petType, now) == PetCare.Need.HUNGRY) {
                    hungry = PetSpec.forType(petType);
                    break;
                }
            }
            if (hungry == null) {
                return;
            }
            title = context.getString(R.string.reminder_hungry_title, context.getString(hungry.emojiRes),
                context.getString(hungry.nameRes));
            text = context.getString(R.string.reminder_hungry_text);
        }

        createChannel(context);
        Intent open = new Intent(context, MainActivitySimple.class)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent content = PendingIntent.getActivity(context, 0, open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_paw)
            .setColor(ContextCompat.getColor(context, R.color.garden_accent))
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(content)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException e) {
            // Permission revoked between the check and the call
        }
    }

    static boolean canNotify(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    private static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(CHANNEL,
            context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(R.string.reminder_channel_description));
        context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }
}
