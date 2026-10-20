package com.example.pixelpetwidget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.pixelpetwidget.sound.SoundManager;
import com.example.pixelpetwidget.ui.theme.PixelTheme;

/**
 * Changes the background of a widget already on the home screen (Android 12+: long press on the
 * widget, then its settings): the same cards as in the app. A tap on a card sets it for this
 * widget only and closes the screen; its pet goes on living as it was.
 */
public class BackgroundPickerActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(MainActivitySimple.withAppLanguage(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int widgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID);
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }
        setContentView(R.layout.activity_background_picker);
        View root = findViewById(R.id.picker_root);
        root.setBackgroundColor(PixelTheme.colors(this).background);
        getWindow().getDecorView().setBackgroundColor(PixelTheme.colors(this).background);
        WindowInsetsControllerCompat systemBars = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        systemBars.setAppearanceLightStatusBars(!PixelTheme.isDark(this));
        systemBars.setAppearanceLightNavigationBars(!PixelTheme.isDark(this));
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            int pad = PixelTheme.px(this, R.dimen.px_screen_padding);
            v.setPadding(bars.left + pad, bars.top + pad, bars.right + pad, bars.bottom + pad);
            return WindowInsetsCompat.CONSUMED;
        });

        // The aquarium is a scene of its own: no background to choose
        boolean scene = PetSpec.forType(PixelCatWidget.getPetType(this, widgetId)).scene;
        findViewById(R.id.picker_scene_note).setVisibility(scene ? View.VISIBLE : View.GONE);
        View cards = findViewById(R.id.bg_cards);
        cards.setVisibility(scene ? View.GONE : View.VISIBLE);
        // The screen's own title says it already
        cards.findViewById(R.id.bg_title).setVisibility(View.GONE);
        BackgroundCards picker = new BackgroundCards(cards, bgType -> {
            PixelCatWidget.setBackground(this, widgetId, bgType);
            SoundManager.get(this).chosen(PixelCatWidget.getPetType(this, widgetId));
            finish();
        });
        picker.select(PixelCatWidget.getBackgroundType(this, widgetId));
        findViewById(R.id.picker_close).setOnClickListener(v -> finish());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // The picker opens on its own from the home screen: its sounds go with it (the app's
        // screen loads them again when it needs them)
        if (isFinishing()) {
            SoundManager.release();
        }
    }
}
