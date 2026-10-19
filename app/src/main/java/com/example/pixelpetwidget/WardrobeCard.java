package com.example.pixelpetwidget;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.pixelpetwidget.sound.SoundManager;
import com.example.pixelpetwidget.ui.pixel.PixelCard;
import com.example.pixelpetwidget.ui.theme.PixelTheme;

/** Гардероб: что надето на выбранного питомца и что ещё можно получить. */
final class WardrobeCard {

    interface Listener {
        void onWardrobeChanged();
    }

    private final Activity activity;
    private final Listener listener;
    private final View card;
    private final TextView season;
    private final LinearLayout items;
    private final TextView message;

    WardrobeCard(Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
        card = activity.findViewById(R.id.wardrobe_card);
        season = activity.findViewById(R.id.wardrobe_season);
        items = activity.findViewById(R.id.wardrobe_items);
        message = activity.findViewById(R.id.wardrobe_message);
    }

    void bind(int petType) {
        if (!Wardrobe.supports(petType)) {
            card.setVisibility(View.GONE);
            return;
        }
        card.setVisibility(View.VISIBLE);
        message.setVisibility(View.GONE);

        String currentSeason = PetSettings.season(activity, System.currentTimeMillis());
        if (Wardrobe.SEASON_HALLOWEEN.equals(currentSeason)) {
            season.setText(R.string.season_halloween);
            season.setVisibility(View.VISIBLE);
        } else if (Wardrobe.SEASON_WINTER.equals(currentSeason)) {
            season.setText(R.string.season_winter);
            season.setVisibility(View.VISIBLE);
        } else {
            season.setVisibility(View.GONE);
        }

        DailyRewards.State state = CareStore.rewards(activity);
        String worn = CareStore.wearing(activity, petType);
        items.removeAllViews();
        LayoutInflater inflater = activity.getLayoutInflater();
        int gap = PixelTheme.px(activity, R.dimen.px_gap);
        int slot = PixelTheme.px(activity, R.dimen.px_wardrobe_image);
        int lockColor = PixelTheme.colors(activity).text;

        // «Ничего» и все вещи по порядку
        String[] options = new String[Wardrobe.ITEMS.length + 1];
        System.arraycopy(Wardrobe.ITEMS, 0, options, 1, Wardrobe.ITEMS.length);
        for (int i = 0; i < options.length; i++) {
            String item = options[i];
            PixelCard chip = (PixelCard) inflater.inflate(R.layout.item_wardrobe, items, false);
            boolean unlocked = item == null || state.unlocked.contains(item);
            boolean canPickUp = item != null && Wardrobe.canPickUp(item, state.unlocked, currentSeason);

            setWholeScale(chip.findViewById(R.id.item_icon),
                item == null ? PetSpec.forType(petType).iconRes : Wardrobe.iconRes(petType, item), slot);
            TextView name = chip.findViewById(R.id.item_name);
            name.setText(item == null ? R.string.wardrobe_none : Wardrobe.nameRes(item));
            String description = name.getText().toString();
            if (canPickUp) {
                name.setText(R.string.wardrobe_pick_up);
                description += ", " + activity.getString(R.string.wardrobe_pick_up);
            } else if (!unlocked) {
                ImageView lock = chip.findViewById(R.id.item_lock);
                lock.setImageDrawable(PixelTheme.icon(activity, R.drawable.ic_px_lock, lockColor));
                lock.setVisibility(View.VISIBLE);
                chip.findViewById(R.id.item_icon).setAlpha(PixelTheme.DISABLED_ALPHA);
                description += ", " + activity.getString(R.string.wardrobe_locked_badge);
            }
            chip.setSelected(item == null ? worn == null : item.equals(worn));
            chip.setContentDescription(description);
            chip.setOnClickListener(v -> choose(petType, item));

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) chip.getLayoutParams();
            params.setMarginStart(i == 0 ? 0 : gap);
            items.addView(chip, params);
        }
    }

    /** Пиксельная картинка в самом крупном целом масштабе, который помещается. */
    private void setWholeScale(ImageView view, int drawableRes, int slotPx) {
        Drawable drawable = ContextCompat.getDrawable(activity, drawableRes);
        view.setImageDrawable(drawable);
        if (drawable == null || drawable.getIntrinsicWidth() <= 0) {
            return;
        }
        int scale = Math.max(1, Math.min(slotPx / drawable.getIntrinsicWidth(), slotPx / drawable.getIntrinsicHeight()));
        view.getLayoutParams().width = drawable.getIntrinsicWidth() * scale;
        view.getLayoutParams().height = drawable.getIntrinsicHeight() * scale;
    }

    private void choose(int petType, String item) {
        DailyRewards.State state = CareStore.rewards(activity);
        String note = null;
        if (item != null && !state.unlocked.contains(item)) {
            String name = activity.getString(Wardrobe.nameRes(item));
            if (!CareStore.pickUp(activity, item, System.currentTimeMillis())) {
                SoundManager.get(activity).error();
                showMessage(activity.getString(R.string.wardrobe_locked, name, activity.getString(Wardrobe.hintRes(item))));
                return;
            }
            SoundManager.get(activity).success();
            note = activity.getString(R.string.wardrobe_new_item, name);
        }
        CareStore.wear(activity, petType, item);
        bind(petType);
        if (note != null) {
            showMessage(note);
        }
        listener.onWardrobeChanged();
    }

    private void showMessage(String text) {
        message.setText(text);
        message.setVisibility(View.VISIBLE);
    }
}
