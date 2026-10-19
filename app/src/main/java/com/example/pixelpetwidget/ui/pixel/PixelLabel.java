package com.example.pixelpetwidget.ui.pixel;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.view.ViewCompat;

import com.example.pixelpetwidget.R;
import com.example.pixelpetwidget.ui.theme.PixelTheme;

/**
 * Текст пиксельным шрифтом. В XML: {@code app:pixelText="title|body|hint"}.
 * title — заголовок (крупнее, для TalkBack это заголовок), body — обычный текст,
 * hint — мелкая подсказка вторым цветом. Размеры и цвета берутся из PixelTheme.
 * На выбранной карточке текст сам перекрашивается в цвет «на акценте».
 */
public class PixelLabel extends AppCompatTextView {

    public static final int TITLE = 0;
    public static final int BODY = 1;
    public static final int HINT = 2;

    public PixelLabel(Context context) {
        this(context, null);
    }

    public PixelLabel(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        int kind = BODY;
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PixelLabel);
            kind = a.getInt(R.styleable.PixelLabel_pixelText, BODY);
            a.recycle();
        }
        setKind(kind);
    }

    public void setKind(int kind) {
        PixelTheme.Colors c = PixelTheme.colors(getContext());
        if (kind == TITLE) {
            PixelTheme.styleText(this, R.dimen.px_text_title, c.text);
            ViewCompat.setAccessibilityHeading(this, true);
        } else if (kind == HINT) {
            PixelTheme.styleText(this, R.dimen.px_text_hint, c.textSecondary);
        } else {
            PixelTheme.styleText(this, R.dimen.px_text_body, c.text);
        }
    }
}
