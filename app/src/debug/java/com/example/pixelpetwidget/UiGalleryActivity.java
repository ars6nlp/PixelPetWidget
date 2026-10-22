package com.example.pixelpetwidget;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.example.pixelpetwidget.ui.pixel.PixelButton;
import com.example.pixelpetwidget.ui.pixel.PixelCard;
import com.example.pixelpetwidget.ui.pixel.PixelDialog;
import com.example.pixelpetwidget.ui.pixel.PixelLabel;
import com.example.pixelpetwidget.ui.pixel.PixelSlider;
import com.example.pixelpetwidget.ui.pixel.PixelSwitch;
import com.example.pixelpetwidget.ui.theme.PixelTheme;

/**
 * Только в debug-сборке: все компоненты в светлой и тёмной теме рядом, чтобы сразу видеть правки
 * в PixelTheme, dimens.xml и ui/pixel. Открывается долгим нажатием на логотип на главном экране.
 */
@SuppressLint("SetTextI18n")
public class UiGalleryActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout columns = new LinearLayout(this);
        columns.addView(column(themed(false)), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        columns.addView(column(themed(true)), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        ScrollView scroll = new ScrollView(this);
        scroll.setFitsSystemWindows(true);
        scroll.addView(columns);
        setContentView(scroll);
    }

    /** Контекст со светлой или тёмной темой, независимо от системной. */
    private Context themed(boolean dark) {
        Configuration config = new Configuration(getResources().getConfiguration());
        config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
            | (dark ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO);
        return createConfigurationContext(config);
    }

    private LinearLayout column(Context c) {
        LinearLayout column = new LinearLayout(c);
        column.setOrientation(LinearLayout.VERTICAL);
        int pad = PixelTheme.px(c, R.dimen.px_screen_padding);
        column.setPadding(pad, pad, pad, pad);
        column.setBackgroundColor(PixelTheme.colors(c).background);

        add(column, label(c, PixelLabel.TITLE, PixelTheme.isDark(c) ? "Тёмная" : "Светлая"));
        add(column, label(c, PixelLabel.BODY, "Обычный текст"));
        add(column, label(c, PixelLabel.HINT, "Подсказка"));

        PixelButton primary = new PixelButton(c);
        primary.setText("Главная");
        add(column, primary);
        PixelButton secondary = new PixelButton(c);
        secondary.setVariant(PixelButton.SECONDARY);
        secondary.setText("Второстепенная");
        add(column, secondary);
        PixelButton disabled = new PixelButton(c);
        disabled.setText("Неактивная");
        disabled.setEnabled(false);
        add(column, disabled);
        PixelButton icon = new PixelButton(c);
        icon.setVariant(PixelButton.SECONDARY);
        icon.setIcon(R.drawable.ic_px_settings);
        icon.setContentDescription("Иконка");
        add(column, icon);

        PixelCard card = new PixelCard(c);
        card.addView(label(c, PixelLabel.BODY, "Карточка"));
        card.setOnClickListener(v -> v.setSelected(!v.isSelected()));
        add(column, card);
        PixelCard selected = new PixelCard(c);
        selected.addView(label(c, PixelLabel.BODY, "Выбранная"));
        selected.setSelected(true);
        add(column, selected);

        PixelSwitch off = new PixelSwitch(c);
        off.setContentDescription("Выключен");
        add(column, off);
        PixelSwitch on = new PixelSwitch(c);
        on.setChecked(true);
        on.setContentDescription("Включён");
        add(column, on);
        PixelSlider slider = new PixelSlider(c);
        slider.setMax(10);
        slider.setProgress(7);
        add(column, slider);

        PixelButton dialog = new PixelButton(c);
        dialog.setText("Диалог");
        dialog.setOnClickListener(v -> PixelDialog.with(c)
            .title("Заголовок")
            .message("Короткий тёплый текст диалога.")
            .negative(R.string.reminder_offer_no, null)
            .show());
        add(column, dialog);
        return column;
    }

    private static PixelLabel label(Context c, int kind, String text) {
        PixelLabel label = new PixelLabel(c);
        label.setKind(kind);
        label.setText(text);
        return label;
    }

    private static void add(LinearLayout column, android.view.View view) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = PixelTheme.px(column.getContext(), R.dimen.px_gap);
        column.addView(view, params);
    }
}
