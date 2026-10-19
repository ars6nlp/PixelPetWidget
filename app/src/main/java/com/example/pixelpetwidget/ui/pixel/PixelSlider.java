package com.example.pixelpetwidget.ui.pixel;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatSeekBar;

import com.example.pixelpetwidget.R;
import com.example.pixelpetwidget.sound.SoundManager;
import com.example.pixelpetwidget.ui.theme.PixelTheme;

/**
 * Слайдер (например, громкость): дорожка цвета фона, заполненная часть — акцент, ползунок —
 * квадрат цвета карточки. Когда палец отпускает ползунок, слышен щелчок — проба новой громкости.
 * Значение 0..getMax(), слушатель — {@link #setListener}.
 */
public class PixelSlider extends AppCompatSeekBar {

    public interface Listener {
        void onValueChanged(int value, boolean fromUser);
    }

    private static final int TRACK_HEIGHT = 6;     // в «пикселях» обводки
    private static final int KNOB_WIDTH = 8;
    private static final int KNOB_HEIGHT = 12;

    private Listener listener;

    public PixelSlider(Context context) {
        this(context, null);
    }

    public PixelSlider(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        int unit = PixelTheme.stroke(context);
        PixelTheme.Colors c = PixelTheme.colors(context);
        setProgressDrawable(new Track(c, unit));
        PixelShape knob = new PixelShape(c.surface, c.line, unit).setSize(KNOB_WIDTH * unit, KNOB_HEIGHT * unit);
        setThumb(knob);
        setThumbOffset(knob.getIntrinsicWidth() / 2);
        setSplitTrack(false);
        setBackground(null);
        setMinimumHeight(PixelTheme.px(context, R.dimen.px_touch));
        setPadding(knob.getIntrinsicWidth() / 2, 0, knob.getIntrinsicWidth() / 2, 0);
        super.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (listener != null) {
                    listener.onValueChanged(progress, fromUser);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                SoundManager.get(getContext()).click();
            }
        });
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setAlpha(enabled ? 1f : PixelTheme.DISABLED_ALPHA);
    }

    /** Дорожка: рамка во всю ширину и заливка акцентом до ползунка. */
    private static final class Track extends Drawable {
        private final PixelShape frame;
        private final Paint paint = new Paint();
        private final int unit;

        Track(PixelTheme.Colors c, int unit) {
            this.unit = unit;
            frame = new PixelShape(c.background, c.line, unit);
            paint.setColor(c.accent);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            Rect b = getBounds();
            int height = TRACK_HEIGHT * unit;
            int top = b.centerY() - height / 2;
            frame.setBounds(b.left, top, b.right, top + height);
            frame.draw(canvas);
            int filled = Math.round((b.width() - 2 * unit) * getLevel() / 10000f);
            canvas.drawRect(b.left + unit, top + unit, b.left + unit + filled, top + height - unit, paint);
        }

        @Override
        protected boolean onLevelChange(int level) {
            invalidateSelf();
            return true;
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
            frame.setAlpha(alpha);
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
}
