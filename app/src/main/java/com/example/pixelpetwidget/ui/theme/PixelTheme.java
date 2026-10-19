package com.example.pixelpetwidget.ui.theme;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.util.TypedValue;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.example.pixelpetwidget.R;

/**
 * Тема приложения: цвета светлой и тёмной темы и шрифт. Поменяйте значение здесь — поменяется на
 * всех экранах. Размеры (обводка, отступы, текст) — рядом, в res/values/dimens.xml.
 *
 * Тёмная тема включается вместе с тёмной темой телефона. Виджет эту тему не использует.
 */
public final class PixelTheme {

    // --- Цвета -----------------------------------------------------------------------------
    // Светлая тема
    private static final Colors LIGHT = new Colors(
        0xFFF7EEDD,   // фон экрана
        0xFFFFF8EA,   // поверхность: карточки, кнопки
        0xFF3D2A20,   // текст
        0xFF6B5243,   // второстепенный текст, подсказки
        0xFFE9875B,   // акцент: главная кнопка, выбранная карточка, включённый переключатель
        0xFF5A4032,   // обводка
        0xFFA9503A);  // ошибка

    // Тёмная тема
    private static final Colors DARK = new Colors(
        0xFF2B201C,
        0xFF3B2C26,
        0xFFF7EADB,
        0xFFD2BCA6,
        0xFFF0A070,
        0xFFA88A72,
        0xFFE88A72);

    /** Неактивные элементы рисуются теми же цветами, только прозрачнее. */
    public static final float DISABLED_ALPHA = 0.45f;
    /** Затемнение под диалогом (чёрный, 60 %). */
    public static final int SCRIM = 0x99000000;

    // --- Размеры ----------------------------------------------------------------------------
    // Все размеры (обводка, отступы, текст) — в res/values/dimens.xml (px_*): их используют и
    // разметка экранов, и компоненты. Здесь только чтение.

    // --- Шрифт ------------------------------------------------------------------------------
    public static final int FONT = R.font.tiny5_regular;
    /** Tiny5 нарисован на сетке 8 px: размер текста округляется до кратного 8, чтобы буквы были чёткими. */
    private static final int FONT_GRID_PX = 8;

    private PixelTheme() {}

    /** Набор цветов одной темы. */
    public static final class Colors {
        public final int background;
        public final int surface;
        public final int text;
        public final int textSecondary;
        public final int accent;
        public final int line;
        public final int error;

        Colors(int background, int surface, int text, int textSecondary, int accent, int line, int error) {
            this.background = background;
            this.surface = surface;
            this.text = text;
            this.textSecondary = textSecondary;
            this.accent = accent;
            this.line = line;
            this.error = error;
        }

        /** Текст на акцентной заливке: тёмный в светлой теме, цвет фона в тёмной. */
        public int onAccent() {
            return this == DARK ? background : text;
        }
    }

    /** Цвета текущей темы (светлой или тёмной, как в системе). */
    public static Colors colors(Context context) {
        return isDark(context) ? DARK : LIGHT;
    }

    public static boolean isDark(Context context) {
        int mode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }

    /** Размер из dimens.xml в пикселях экрана, например {@code px(context, R.dimen.px_gap)}. */
    public static int px(Context context, int dimenRes) {
        return context.getResources().getDimensionPixelSize(dimenRes);
    }

    /** Толщина обводки в пикселях экрана, всегда целая. */
    public static int stroke(Context context) {
        return Math.max(1, px(context, R.dimen.px_stroke));
    }

    /** Чуть темнее: так выглядит нажатая кнопка или карточка. */
    public static int pressed(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return (color & 0xFF000000) | ((r * 88 / 100) << 16) | ((g * 88 / 100) << 8) | (b * 88 / 100);
    }

    /**
     * Пиксельный шрифт с чётким размером. Цвет обычный, а на выбранной карточке (акцентная заливка)
     * — {@link Colors#onAccent()}.
     */
    public static void styleText(TextView view, int sizeRes, int color) {
        Context context = view.getContext();
        view.setTypeface(font(context));
        float px = context.getResources().getDimension(sizeRes);
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, Math.max(FONT_GRID_PX, Math.round(px / FONT_GRID_PX) * FONT_GRID_PX));
        Paint paint = view.getPaint();
        paint.setAntiAlias(false);
        paint.setSubpixelText(false);
        paint.setFilterBitmap(true);      // эмодзи в именах питомцев остаются гладкими
        int onAccent = colors(context).onAccent();
        view.setTextColor(new ColorStateList(
            new int[][] {{android.R.attr.state_selected}, {}},
            new int[] {onAccent, color}));
    }

    public static Typeface font(Context context) {
        Typeface typeface = ResourcesCompat.getFont(context, FONT);
        return typeface != null ? typeface : Typeface.DEFAULT;
    }

    /**
     * Пиксельная иконка из res/drawable-nodpi: каждый её пиксель становится квадратом в толщину
     * обводки, без размытия. {@code tint} перекрашивает одноцветные иконки (0 — оставить как есть).
     */
    public static BitmapDrawable icon(Context context, int resId, int tint) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resId, options);
        int unit = stroke(context);
        BitmapDrawable drawable = new BitmapDrawable(context.getResources(),
            Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * unit, bitmap.getHeight() * unit, false));
        drawable.setFilterBitmap(false);
        if (tint != 0) {
            drawable.setColorFilter(new PorterDuffColorFilter(tint, PorterDuff.Mode.SRC_IN));
        }
        return drawable;
    }
}
