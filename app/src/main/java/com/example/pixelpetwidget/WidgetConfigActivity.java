package com.example.pixelpetwidget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

/**
 * Configuration Activity for Pixel Cat Widget.
 * Launched when the widget is added to the home screen; it has no UI and copies the pet
 * and background chosen in the app to the new widget. Launched again for a widget already there
 * (Android 12+, its settings on a long press), it opens the background picker instead.
 */
public class WidgetConfigActivity extends Activity {

    static final int MAX_WIDGETS = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Cancel by default so backing out doesn't leave an empty widget
        setResult(RESULT_CANCELED);

        Bundle extras = getIntent().getExtras();
        int appWidgetId = extras != null
            ? extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            : AppWidgetManager.INVALID_APPWIDGET_ID;
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        if (getSharedPreferences(PixelCatWidget.PREFS_NAME, MODE_PRIVATE)
            .contains(PixelCatWidget.PET_TYPE + "_" + appWidgetId)) {
            // A widget already on the home screen: change its background
            startActivity(new Intent(this, BackgroundPickerActivity.class)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId));
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            setResult(RESULT_OK, resultValue);
            finish();
            return;
        }

        // The new widget is already counted here, so more than MAX_WIDGETS means over the limit
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] activeWidgets = appWidgetManager.getAppWidgetIds(new ComponentName(this, PixelCatWidget.class));
        if (activeWidgets.length > MAX_WIDGETS) {
            Toast.makeText(this, R.string.max_widgets_reached, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Pet and background last chosen on the main screen
        SharedPreferences mainPrefs = getSharedPreferences(MainActivitySimple.PET_PREFS, MODE_PRIVATE);
        int petType = PixelCatWidget.petTypeFromName(
            mainPrefs.getString(MainActivitySimple.KEY_SELECTED_PET, "cat"));
        int bgType = mainPrefs.getInt(MainActivitySimple.KEY_SELECTED_BG, 1);
        PixelCatWidget.configureNewWidget(this, appWidgetId, petType, bgType);

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }
}
