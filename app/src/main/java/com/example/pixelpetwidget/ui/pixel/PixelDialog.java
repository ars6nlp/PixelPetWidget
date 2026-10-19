package com.example.pixelpetwidget.ui.pixel;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.pixelpetwidget.R;
import com.example.pixelpetwidget.sound.SoundManager;
import com.example.pixelpetwidget.ui.theme.PixelTheme;

/**
 * Диалог: заголовок, текст и одна-две кнопки в пиксельной карточке на затемнённом фоне.
 * <pre>
 * PixelDialog.with(context)
 *     .title(R.string.x)
 *     .message(R.string.y)
 *     .positive(R.string.ok, () -> ...)      // главная кнопка (по умолчанию «Понятно»)
 *     .negative(R.string.no, null)           // вторая кнопка, необязательна
 *     .show();
 * </pre>
 * Длинный текст прокручивается внутри. «Назад» или нажатие мимо карточки закрывают диалог.
 */
public final class PixelDialog {

    private static final float MAX_WIDTH_DP = 440;
    /** Текст занимает не больше этой доли высоты экрана, остальное прокручивается. */
    private static final float MESSAGE_MAX_SCREEN_SHARE = 0.55f;

    private final Context context;
    private CharSequence title;
    private CharSequence message;
    private CharSequence positive;
    private Runnable onPositive;
    private CharSequence negative;
    private Runnable onNegative;
    private CharSequence extra;
    private Runnable onExtra;
    private boolean error;

    private PixelDialog(Context context) {
        this.context = context;
    }

    public static PixelDialog with(Context context) {
        return new PixelDialog(context);
    }

    public PixelDialog title(int resId) {
        return title(context.getString(resId));
    }

    public PixelDialog title(CharSequence title) {
        this.title = title;
        return this;
    }

    public PixelDialog message(int resId) {
        return message(context.getString(resId));
    }

    public PixelDialog message(CharSequence message) {
        this.message = message;
        return this;
    }

    public PixelDialog positive(int resId, @Nullable Runnable action) {
        this.positive = context.getString(resId);
        this.onPositive = action;
        return this;
    }

    public PixelDialog negative(int resId, @Nullable Runnable action) {
        this.negative = context.getString(resId);
        this.onNegative = action;
        return this;
    }

    /** Ещё одна кнопка во всю ширину над двумя основными (например, ссылка). */
    public PixelDialog extra(int resId, @Nullable Runnable action) {
        this.extra = context.getString(resId);
        this.onExtra = action;
        return this;
    }

    /** Сообщение об ошибке или отказе: при открытии звучит звук ошибки. */
    public PixelDialog error() {
        this.error = true;
        return this;
    }

    public AppCompatDialog show() {
        AppCompatDialog dialog = new AppCompatDialog(context, R.style.Theme_PixelPetWidget_Dialog);
        int gap = PixelTheme.px(context, R.dimen.px_gap);

        // Карточка: заголовок, текст, кнопки
        PixelCard card = new PixelCard(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setClickable(true);                     // нажатие по карточке не закрывает диалог
        card.setSoundEffectsEnabled(false);
        if (title != null) {
            PixelLabel titleView = new PixelLabel(context);
            titleView.setKind(PixelLabel.TITLE);
            titleView.setText(title);
            card.addView(titleView);
            dialog.setTitle(title);                  // TalkBack читает его при открытии
        }
        if (message != null) {
            int maxHeight = Math.round(context.getResources().getDisplayMetrics().heightPixels
                * MESSAGE_MAX_SCREEN_SHARE);
            ScrollView scroll = new ScrollView(context) {
                @Override
                protected void onMeasure(int widthSpec, int heightSpec) {
                    super.onMeasure(widthSpec, MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST));
                }
            };
            PixelLabel messageView = new PixelLabel(context);
            messageView.setText(message);
            scroll.addView(messageView);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.topMargin = title != null ? gap : 0;
            card.addView(scroll, params);
        }

        if (extra != null) {
            PixelButton extraButton = new PixelButton(context);
            extraButton.setVariant(PixelButton.SECONDARY);
            extraButton.setText(extra);
            extraButton.setOnClickListener(v -> {
                dialog.dismiss();
                if (onExtra != null) onExtra.run();
            });
            LinearLayout.LayoutParams extraParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            extraParams.topMargin = 2 * gap;
            card.addView(extraButton, extraParams);
        }

        LinearLayout buttons = new LinearLayout(context);
        buttons.setGravity(Gravity.END);
        PixelButton positiveButton = new PixelButton(context);
        positiveButton.setText(positive != null ? positive : context.getString(R.string.dialog_ok));
        positiveButton.setOnClickListener(v -> {
            dialog.dismiss();
            if (onPositive != null) onPositive.run();
        });
        if (negative != null) {
            PixelButton negativeButton = new PixelButton(context);
            negativeButton.setVariant(PixelButton.SECONDARY);
            negativeButton.setText(negative);
            negativeButton.setOnClickListener(v -> {
                dialog.dismiss();
                if (onNegative != null) onNegative.run();
            });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            p.setMarginEnd(gap);
            buttons.addView(negativeButton, p);
            buttons.addView(positiveButton, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        } else {
            buttons.addView(positiveButton);
        }
        LinearLayout.LayoutParams buttonsParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonsParams.topMargin = extra != null ? gap : 2 * gap;
        card.addView(buttons, buttonsParams);

        // Затемнённый фон на весь экран, карточка по центру
        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(PixelTheme.SCRIM);
        root.setOnClickListener(v -> dialog.cancel());
        root.setSoundEffectsEnabled(false);
        int width = Math.min(Math.round(MAX_WIDTH_DP * context.getResources().getDisplayMetrics().density),
            context.getResources().getDisplayMetrics().widthPixels - 2 * PixelTheme.px(context, R.dimen.px_section_gap));
        root.addView(card, new FrameLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        dialog.setContentView(root);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            WindowCompat.setDecorFitsSystemWindows(window, false);
            ViewCompat.setOnApplyWindowInsetsListener(root, (View v, WindowInsetsCompat insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return WindowInsetsCompat.CONSUMED;
            });
        }
        dialog.show();
        if (error) {
            SoundManager.get(context).error();
        }
        positiveButton.requestFocus();
        return dialog;
    }
}
