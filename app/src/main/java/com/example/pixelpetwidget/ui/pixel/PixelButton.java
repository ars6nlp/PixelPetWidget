package com.example.pixelpetwidget.ui.pixel;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import com.example.pixelpetwidget.R;
import com.example.pixelpetwidget.sound.SoundManager;
import com.example.pixelpetwidget.ui.theme.PixelTheme;

/**
 * Кнопка. В XML:
 * {@code app:pixelVariant="primary"} — главная, акцентная (по умолчанию);
 * {@code app:pixelVariant="secondary"} — второстепенная, цвета карточки;
 * {@code app:pixelIcon="@drawable/ic_px_settings"} — иконка слева от текста, а без текста
 * кнопка становится квадратной кнопкой-иконкой (не забудьте contentDescription).
 * При нажатии кнопка чуть темнеет и щёлкает ({@link #setClickSound} выключает щелчок).
 */
public class PixelButton extends AppCompatTextView {

    public static final int PRIMARY = 0;
    public static final int SECONDARY = 1;

    private final PixelShape shape;
    private int variant;
    private int iconRes;
    private Drawable icon;
    private boolean clickSound = true;

    public PixelButton(Context context) {
        this(context, null);
    }

    public PixelButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PixelButton);
            variant = a.getInt(R.styleable.PixelButton_pixelVariant, PRIMARY);
            iconRes = a.getResourceId(R.styleable.PixelButton_pixelIcon, 0);
            a.recycle();
        }
        int touch = PixelTheme.px(context, R.dimen.px_touch);
        int pad = PixelTheme.px(context, R.dimen.px_card_padding);
        setMinHeight(touch);
        setMinWidth(touch);
        int gap = PixelTheme.px(context, R.dimen.px_gap);
        setPadding(pad, gap / 2, pad, gap / 2);
        setGravity(Gravity.CENTER);
        setClickable(true);
        setFocusable(true);
        shape = new PixelShape(0, 0, PixelTheme.stroke(context));
        setBackground(shape);
        PixelTheme.styleText(this, R.dimen.px_text_body, 0);
        applyColors();
    }

    public void setVariant(int variant) {
        this.variant = variant;
        applyColors();
    }

    /** Иконка из res/drawable-nodpi, 0 — без иконки. */
    public void setIcon(int resId) {
        iconRes = resId;
        applyColors();
    }

    /** false — без щелчка (например, когда кнопка сама играет голос питомца). */
    public void setClickSound(boolean on) {
        clickSound = on;
    }

    /** Цвет текста и иконка: зависят от варианта кнопки. */
    private void applyColors() {
        PixelTheme.Colors c = PixelTheme.colors(getContext());
        int text = variant == PRIMARY ? c.onAccent() : c.text;
        setTextColor(text);
        icon = iconRes != 0 ? PixelTheme.icon(getContext(), iconRes, text) : null;
        if (icon != null && !TextUtils.isEmpty(getText())) {
            setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null);
            setCompoundDrawablePadding(PixelTheme.px(getContext(), R.dimen.px_gap));
        } else {
            setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null);
        }
        paintShape();
    }

    /** Заливка: акцент или поверхность, темнее при нажатии; неактивная кнопка прозрачнее. */
    private void paintShape() {
        PixelTheme.Colors c = PixelTheme.colors(getContext());
        int fill = variant == PRIMARY ? c.accent : c.surface;
        boolean pressed = isPressed() || (isFocused() && !isInTouchMode());
        shape.setColors(pressed ? PixelTheme.pressed(fill) : fill, c.line);
        setAlpha(isEnabled() ? 1f : PixelTheme.DISABLED_ALPHA);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (shape != null) {
            paintShape();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Кнопка-иконка без текста: иконка по центру
        if (icon != null && TextUtils.isEmpty(getText())) {
            int w = icon.getIntrinsicWidth();
            int h = icon.getIntrinsicHeight();
            int left = (getWidth() - w) / 2;
            int top = (getHeight() - h) / 2;
            icon.setBounds(left, top, left + w, top + h);
            icon.draw(canvas);
        }
    }

    @Override
    public boolean performClick() {
        if (clickSound) {
            SoundManager.get(getContext()).click();
        }
        return super.performClick();
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return Button.class.getName();
    }
}
