package com.example.pixelpetwidget;

import android.app.Activity;
import android.graphics.drawable.AnimationDrawable;
import android.view.LayoutInflater;
import android.widget.GridLayout;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.pixelpetwidget.ui.pixel.PixelCard;
import com.example.pixelpetwidget.ui.theme.PixelTheme;

import java.util.ArrayList;
import java.util.List;

/**
 * Карточки выбора питомца в две колонки: idle-кадр, имя и короткое описание.
 * Выбранная карточка залита акцентом. У сцены (аквариума) на карточке идёт её спокойный цикл,
 * пока экран открыт; с системной настройкой «Убрать анимации» он стоит.
 */
final class PetCards {

    interface Listener {
        /** Нажали карточку; {@code wasChosen} — если этот питомец уже был выбран. */
        void onPetTapped(String key, boolean wasChosen);
    }

    /** Питомец в списке. */
    static final class Option {
        final String key;
        final int descRes;

        Option(String key, int descRes) {
            this.key = key;
            this.descRes = descRes;
        }

        PetSpec spec() {
            return PetSpec.forType(PixelCatWidget.petTypeFromName(key));
        }
    }

    private final List<PixelCard> cards = new ArrayList<>();
    private final List<String> keys = new ArrayList<>();
    /** Циклы сцен на карточках: играют между start() и stop(). */
    private final List<AnimationDrawable> scenes = new ArrayList<>();

    PetCards(Activity activity, GridLayout grid, Option[] options, Listener listener) {
        LayoutInflater inflater = activity.getLayoutInflater();
        int gap = PixelTheme.px(activity, R.dimen.px_gap);
        int slot = PixelTheme.px(activity, R.dimen.px_pet_card_image);
        for (int i = 0; i < options.length; i++) {
            Option option = options[i];
            PetSpec spec = option.spec();
            PixelCard card = (PixelCard) inflater.inflate(R.layout.item_pet_card, grid, false);
            String name = activity.getString(spec.nameRes);
            ((TextView) card.findViewById(R.id.tile_name)).setText(name);
            ((TextView) card.findViewById(R.id.tile_desc)).setText(option.descRes);
            card.setContentDescription(name + ". " + activity.getString(option.descRes));

            // Первый кадр спокойной анимации. Пиксельные питомцы — в целом масштабе, без размытия.
            ImageView pet = card.findViewById(R.id.tile_pet);
            PetSpec.Anim calm = spec.animation(showcase(spec));
            if (spec.scene) {
                // Широкая сцена высотой со слот, в целом масштабе, со своим циклом
                int scale = Math.max(1, slot / spec.frameHeight);
                pet.getLayoutParams().width = spec.frameWidth * scale;
                // As tall as the other cards' pictures, the scene in the middle: the cards of a row line up
                pet.getLayoutParams().height = slot;
                int spare = slot - spec.frameHeight * scale;
                pet.setPadding(0, spare / 2, 0, spare - spare / 2);
                AnimationDrawable loop = new AnimationDrawable();
                for (int frame : calm.frames) {
                    loop.addFrame(ContextCompat.getDrawable(activity, frame), calm.intervalMs);
                }
                loop.setOneShot(false);
                pet.setImageDrawable(loop);
                scenes.add(loop);
            } else {
                if (spec.pixelGrid > 0) {
                    int size = Math.max(1, slot / spec.pixelGrid) * spec.pixelGrid;
                    pet.getLayoutParams().width = size;
                    pet.getLayoutParams().height = size;
                }
                pet.setImageResource(calm.frames[0]);
                if (spec.pixelGrid == 0) {
                    fitDrawing(pet, slot);
                }
            }
            card.setOnClickListener(v -> listener.onPetTapped(option.key, card.isSelected()));

            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f));
            params.width = 0;
            boolean leftColumn = i % 2 == 0;
            params.setMargins(leftColumn ? 0 : gap / 2, 0, leftColumn ? gap / 2 : 0, gap);
            grid.addView(card, params);
            cards.add(card);
            keys.add(option.key);
        }
    }

    /** Спокойная анимация питомца: idle, а у кого её нет — то, что он делает на тап (кот сидит). */
    /**
     * A hand-drawn pet's picture fills the slot by the drawing itself, not by its frame (the frames
     * have wide empty margins of different sizes, so the bunny looked far smaller than the cat).
     */
    private static void fitDrawing(ImageView view, int slot) {
        if (!(view.getDrawable() instanceof BitmapDrawable)) {
            return;
        }
        Bitmap bitmap = ((BitmapDrawable) view.getDrawable()).getBitmap();
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int[] pixels = new int[w * h];
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h);
        int left = w;
        int top = h;
        int right = -1;
        int bottom = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if ((pixels[y * w + x] >>> 24) != 0) {
                    left = Math.min(left, x);
                    right = Math.max(right, x);
                    top = Math.min(top, y);
                    bottom = Math.max(bottom, y);
                }
            }
        }
        if (right < left || bottom < top) {
            return;
        }
        // The drawable may be shown at another size than the bitmap's pixels
        float unit = view.getDrawable().getIntrinsicWidth() / (float) w;
        float boxWidth = (right - left + 1) * unit;
        float boxHeight = (bottom - top + 1) * unit;
        float scale = Math.min(slot / boxWidth, slot / boxHeight);
        Matrix matrix = new Matrix();
        matrix.setTranslate(-left * unit, -top * unit);
        matrix.postScale(scale, scale);
        matrix.postTranslate((slot - boxWidth * scale) / 2f, (slot - boxHeight * scale) / 2f);
        view.setScaleType(ImageView.ScaleType.MATRIX);
        view.setImageMatrix(matrix);
    }

    static int showcase(PetSpec spec) {
        if (spec.hasAnimation(PixelCatWidget.ANIMATION_IDLE)) {
            return PixelCatWidget.ANIMATION_IDLE;
        }
        return spec.joyAnimation;
    }

    /** Запускает циклы сцен (из onResume); без анимаций показывает первый кадр. */
    void start(boolean animate) {
        for (AnimationDrawable loop : scenes) {
            if (animate) {
                loop.start();
            } else {
                loop.stop();
                loop.selectDrawable(0);
            }
        }
    }

    /** Останавливает их (из onPause). */
    void stop() {
        for (AnimationDrawable loop : scenes) {
            loop.stop();
        }
    }

    void select(String key) {
        for (int i = 0; i < cards.size(); i++) {
            cards.get(i).setSelected(keys.get(i).equals(key));
        }
    }
}
