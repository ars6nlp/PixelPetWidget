package com.example.pixelpetwidget.ui.pixel;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.example.pixelpetwidget.R;
import com.example.pixelpetwidget.sound.SoundManager;
import com.example.pixelpetwidget.ui.theme.PixelTheme;

/**
 * Карточка: LinearLayout в пиксельной рамке (цвет поверхности и обводка).
 * Может быть кнопкой: дайте ей OnClickListener — при нажатии она темнеет и щёлкает.
 * {@code setSelected(true)} заливает её акцентом — так отмечен выбранный питомец, фон или вещь;
 * PixelLabel внутри сами меняют цвет текста.
 * Внутренний отступ по умолчанию — px_card_padding из dimens.xml (если в XML не задан свой).
 */
public class PixelCard extends LinearLayout {

    private final PixelShape shape;

    public PixelCard(Context context) {
        this(context, null);
    }

    public PixelCard(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        shape = new PixelShape(0, 0, PixelTheme.stroke(context));
        setBackground(shape);
        if (getPaddingLeft() == 0 && getPaddingTop() == 0 && getPaddingRight() == 0 && getPaddingBottom() == 0) {
            int pad = PixelTheme.px(context, R.dimen.px_card_padding);
            setPadding(pad, pad, pad, pad);
        }
        applyColors();
    }

    private void applyColors() {
        PixelTheme.Colors c = PixelTheme.colors(getContext());
        int fill = isSelected() ? c.accent : c.surface;
        boolean pressed = hasOnClickListeners() && (isPressed() || (isFocused() && !isInTouchMode()));
        shape.setColors(pressed ? PixelTheme.pressed(fill) : fill, c.line);
        setAlpha(isEnabled() ? 1f : PixelTheme.DISABLED_ALPHA);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (shape != null) {
            applyColors();
        }
    }

    @Override
    public boolean performClick() {
        if (hasOnClickListeners()) {
            SoundManager.get(getContext()).click();
        }
        return super.performClick();
    }
}
