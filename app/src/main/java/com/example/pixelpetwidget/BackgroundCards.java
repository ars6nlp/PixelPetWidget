package com.example.pixelpetwidget;

import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * The background cards (layout part_bg_cards): the three painted pictures and the three pixel-art
 * backgrounds, card n standing for bg_type n. Shared by the main screen and the picker that
 * changes the background of a widget already on the home screen. Pixel art is shown at whole-
 * number scales only, so its pixels stay square and crisp.
 */
final class BackgroundCards {

    /** bg_type of every card, in order. */
    static final int[] TYPES = {1, 2, 3, Backdrop.MAPLE_PARK, Backdrop.RAINY_EVENING, Backdrop.PUMPKIN_FIELD};
    private static final int[] CARDS = {R.id.bg_button_1, R.id.bg_button_2, R.id.bg_button_3, R.id.bg_button_4,
        R.id.bg_button_5, R.id.bg_button_6};
    private static final int[] THUMBS = {R.id.bg_thumb_view_1, R.id.bg_thumb_view_2, R.id.bg_thumb_view_3,
        R.id.bg_thumb_view_4, R.id.bg_thumb_view_5, R.id.bg_thumb_view_6};

    interface Listener {
        void onPicked(int bgType);
    }

    private final View[] cards = new View[TYPES.length];
    private final TextView name;

    BackgroundCards(View root, Listener listener) {
        for (int i = 0; i < TYPES.length; i++) {
            int bgType = TYPES[i];
            cards[i] = root.findViewById(CARDS[i]);
            cards[i].setOnClickListener(v -> listener.onPicked(bgType));
            if (Backdrop.forType(bgType) != null) {
                coverWholePixels(root.findViewById(THUMBS[i]));
            }
        }
        name = root.findViewById(R.id.bg_name);
    }

    /** Marks the card of {@code bgType} and names a pixel-art background under the cards. */
    void select(int bgType) {
        for (int i = 0; i < TYPES.length; i++) {
            cards[i].setSelected(TYPES[i] == bgType);
        }
        Backdrop backdrop = Backdrop.forType(bgType);
        name.setVisibility(backdrop != null ? View.VISIBLE : View.GONE);
        if (backdrop != null) {
            name.setText(backdrop.nameRes);
        }
    }

    /**
     * Fills the view with its picture like centerCrop, but at the smallest whole-number scale that
     * covers it.
     */
    static void coverWholePixels(ImageView view) {
        view.setScaleType(ImageView.ScaleType.MATRIX);
        view.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            Drawable picture = view.getDrawable();
            int width = r - l - view.getPaddingLeft() - view.getPaddingRight();
            int height = b - t - view.getPaddingTop() - view.getPaddingBottom();
            if (picture == null || width <= 0 || height <= 0) {
                return;
            }
            int w = picture.getIntrinsicWidth();
            int h = picture.getIntrinsicHeight();
            int scale = (int) Math.max(1, Math.ceil(Math.max((double) width / w, (double) height / h)));
            Matrix matrix = new Matrix();
            matrix.setScale(scale, scale);
            matrix.postTranslate(Math.round((width - w * scale) / 2f), Math.round((height - h * scale) / 2f));
            view.setImageMatrix(matrix);
        });
    }

    /**
     * Shows a pixel-art background the way the widget does: at a whole-number {@code scale}, its
     * ground line {@code groundFromBottom} pixels above the view's bottom, centred, the sky colour
     * around it.
     */
    static void showOnGround(ImageView view, Backdrop backdrop, int scale, int groundFromBottom) {
        view.setImageResource(backdrop.previewRes);
        view.setBackgroundColor(backdrop.sky);
        view.setScaleType(ImageView.ScaleType.MATRIX);
        int width = view.getWidth();
        int height = view.getHeight();
        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);
        matrix.postTranslate(Math.round((width - backdrop.width * scale) / 2f),
            height - groundFromBottom - backdrop.ground * scale);
        view.setImageMatrix(matrix);
    }
}
