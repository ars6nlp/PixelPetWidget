package com.example.pixelpetwidget;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * Debug builds only: plays a widget's interaction with its background at once, to check all of
 * them on the emulator:
 * adb shell am broadcast -n com.aixdev.pixelpet/com.example.pixelpetwidget.DebugEventReceiver --ei widget ID
 */
public class DebugEventReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int widgetId = intent.getIntExtra("widget", AppWidgetManager.INVALID_APPWIDGET_ID);
        BackgroundEvents.Interaction interaction = PetLife.interaction(context, widgetId);
        if (interaction == null) {
            return;
        }
        long now = System.currentTimeMillis();
        SharedPreferences prefs = PetLife.prefs(context);
        PetSpec spec = PetSpec.forType(PixelCatWidget.getPetType(context, widgetId));
        PetBehavior.Session s = PetLife.load(prefs, widgetId);
        if (!PetBehavior.isAwake(s, now)) {
            PetBehavior.tap(spec, interaction, s, now, now);
        }
        PetBehavior.startEventNow(spec, interaction, s, now);
        SharedPreferences.Editor editor = prefs.edit();
        PetLife.save(editor, widgetId, s);
        editor.apply();
        PixelCatWidget.updateWidget(context, AppWidgetManager.getInstance(context), widgetId);
        PetLife.schedule(context);
    }
}
