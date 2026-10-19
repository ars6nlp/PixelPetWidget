package com.example.pixelpetwidget.ui.pixel;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Фон всех пиксельных компонентов: плоская заливка, ровная обводка и срезанные углы (по одной
 * ступеньке). Толщина обводки — {@code stroke} пикселей экрана (обычно PixelTheme.stroke()).
 * Цвета меняются через {@link #setColors}.
 */
public final class PixelShape extends Drawable {

    private final Paint paint = new Paint();
    private final int stroke;
    private int fill;
    private int line;
    private int alpha = 255;
    private int width = -1;
    private int height = -1;

    public PixelShape(int fill, int line, int stroke) {
        this.fill = fill;
        this.line = line;
        this.stroke = stroke;
    }

    /** Собственный размер (нужен, например, ползунку слайдера). По умолчанию размера нет. */
    public PixelShape setSize(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    @Override
    public int getIntrinsicWidth() {
        return width;
    }

    @Override
    public int getIntrinsicHeight() {
        return height;
    }

    public void setColors(int fill, int line) {
        if (fill != this.fill || line != this.line) {
            this.fill = fill;
            this.line = line;
            invalidateSelf();
        }
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect b = getBounds();
        int s = stroke;
        // Заливка внутри обводки
        color(fill);
        canvas.drawRect(b.left + s, b.top + s, b.right - s, b.bottom - s, paint);
        // Обводка: четыре стороны, углы пустые — получается срез в одну ступеньку
        color(line);
        canvas.drawRect(b.left + s, b.top, b.right - s, b.top + s, paint);
        canvas.drawRect(b.left + s, b.bottom - s, b.right - s, b.bottom, paint);
        canvas.drawRect(b.left, b.top + s, b.left + s, b.bottom - s, paint);
        canvas.drawRect(b.right - s, b.top + s, b.right, b.bottom - s, paint);
    }

    private void color(int color) {
        paint.setColor(color);
        paint.setAlpha((color >>> 24) * alpha / 255);
    }

    @Override
    public void setAlpha(int alpha) {
        this.alpha = alpha;
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
    }

    @Override
    @SuppressWarnings("deprecation")   // still abstract in Drawable: every drawable must answer it
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
