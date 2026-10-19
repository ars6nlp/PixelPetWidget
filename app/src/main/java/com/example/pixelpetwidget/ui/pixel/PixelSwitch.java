package com.example.pixelpetwidget.ui.pixel;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.annotation.Nullable;

import com.example.pixelpetwidget.R;
import com.example.pixelpetwidget.sound.SoundManager;
import com.example.pixelpetwidget.ui.theme.PixelTheme;

/**
 * Переключатель вкл/выкл: дорожка (выключен — цвет фона, включён — акцент) и квадратный ползунок,
 * который сразу прыгает на другую сторону. Щёлкает при переключении.
 * Размер — в «пикселях» обводки: TRACK_WIDTH × TRACK_HEIGHT, ползунок KNOB_WIDTH.
 * Для TalkBack подпишите его: {@code android:labelFor} у текста рядом или contentDescription.
 */
public class PixelSwitch extends CompoundButton {

    private static final int TRACK_WIDTH = 18;
    private static final int TRACK_HEIGHT = 10;
    private static final int KNOB_WIDTH = 9;

    private final PixelShape track;
    private final PixelShape knob;
    private final int unit;

    public PixelSwitch(Context context) {
        this(context, null);
    }

    public PixelSwitch(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, 0);
        unit = PixelTheme.stroke(context);
        track = new PixelShape(0, 0, unit);
        knob = new PixelShape(0, 0, unit);
        setBackground(null);
        setButtonDrawable(null);
        setClickable(true);
        setFocusable(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int touch = PixelTheme.px(getContext(), R.dimen.px_touch);
        int width = Math.max(touch, TRACK_WIDTH * unit + getPaddingLeft() + getPaddingRight());
        int height = Math.max(touch, TRACK_HEIGHT * unit + getPaddingTop() + getPaddingBottom());
        setMeasuredDimension(resolveSize(width, widthMeasureSpec), resolveSize(height, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        PixelTheme.Colors c = PixelTheme.colors(getContext());
        int width = TRACK_WIDTH * unit;
        int height = TRACK_HEIGHT * unit;
        int left = getWidth() - getPaddingRight() - width;       // прижат к правому краю
        int top = (getHeight() - height) / 2;
        track.setColors(isChecked() ? c.accent : c.background, c.line);
        track.setBounds(left, top, left + width, top + height);
        track.draw(canvas);
        int knobLeft = isChecked() ? left + width - KNOB_WIDTH * unit : left;
        boolean pressed = isPressed() || (isFocused() && !isInTouchMode());
        knob.setColors(pressed ? PixelTheme.pressed(c.surface) : c.surface, c.line);
        knob.setBounds(knobLeft, top, knobLeft + KNOB_WIDTH * unit, top + height);
        knob.draw(canvas);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setAlpha(enabled ? 1f : PixelTheme.DISABLED_ALPHA);
    }

    @Override
    public boolean performClick() {
        boolean handled = super.performClick();      // переключает
        SoundManager.get(getContext()).toggle();
        return handled;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        invalidate();
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return Switch.class.getName();
    }
}
