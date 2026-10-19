package com.example.pixelpetwidget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Care state, kept per pet type: every widget with the same pet shows the same mood.
 * Also holds the daily gift (treats, streak) and what each pet is wearing.
 */
final class CareStore {

    private static final String PREFS = "PixelPetCare";
    private static final String FOOD = "food_full_at_";
    private static final String JOY = "joy_full_at_";
    private static final String PLAY_DAY = "play_day_";
    private static final String PLAY_COUNT = "play_count_";
    private static final String STROKE_DAY = "stroke_day_";
    private static final String STROKE_COUNT = "stroke_count_";
    private static final String WEAR = "wear_";
    private static final String TREATS = "treats";
    private static final String LAST_CLAIM_DAY = "last_claim_day";
    private static final String STREAK = "streak";
    private static final String FREEZES = "freezes";
    private static final String UNLOCKED = "unlocked";

    enum FeedResult { FED, ALREADY_FULL, NO_TREATS }

    private CareStore() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** A pet seen for the first time (also after updating the app) starts full and happy. */
    private static long fullAt(SharedPreferences prefs, String key, long now) {
        long value = prefs.getLong(key, Long.MIN_VALUE);
        if (value == Long.MIN_VALUE) {
            prefs.edit().putLong(key, now).apply();
            return now;
        }
        // A clock set back must not leave the pet "full" for days
        return Math.min(value, now);
    }

    static float food(Context context, int petType, long now) {
        return PetCare.level(fullAt(prefs(context), FOOD + petType, now), now, PetCare.HUNGER_PER_HOUR);
    }

    static float joy(Context context, int petType, long now) {
        return PetCare.level(fullAt(prefs(context), JOY + petType, now), now, PetCare.BOREDOM_PER_HOUR);
    }

    static PetCare.Need need(Context context, int petType, long now) {
        return PetCare.need(food(context, petType, now), joy(context, petType, now));
    }

    /** Spends a treat on the pet; refuses when it is already full or there are no treats. */
    static FeedResult feed(Context context, int petType, long now) {
        if (food(context, petType, now) > PetCare.FULL_ABOVE) {
            return FeedResult.ALREADY_FULL;
        }
        DailyRewards.State rewards = rewards(context);
        if (!DailyRewards.spendTreat(rewards)) {
            return FeedResult.NO_TREATS;
        }
        SharedPreferences prefs = prefs(context);
        long raised = PetCare.raise(fullAt(prefs, FOOD + petType, now), now, PetCare.FEED_AMOUNT,
            PetCare.HUNGER_PER_HOUR);
        prefs.edit().putLong(FOOD + petType, raised).putInt(TREATS, rewards.treats).apply();
        return FeedResult.FED;
    }

    /** A tap on the widget counts as playing, up to PLAYS_PER_DAY times a day. */
    static boolean play(Context context, int petType, long now) {
        return raiseJoy(context, petType, now, PLAY_DAY, PLAY_COUNT, PetCare.PLAYS_PER_DAY, PetCare.PLAY_AMOUNT);
    }

    /** Stroking the pet in the app, up to STROKES_PER_DAY times a day. */
    static boolean stroke(Context context, int petType, long now) {
        return raiseJoy(context, petType, now, STROKE_DAY, STROKE_COUNT, PetCare.STROKES_PER_DAY,
            PetCare.STROKE_AMOUNT);
    }

    private static boolean raiseJoy(Context context, int petType, long now, String dayKey, String countKey,
                                    int perDay, float amount) {
        SharedPreferences prefs = prefs(context);
        long today = DayClock.today();
        int count = prefs.getLong(dayKey + petType, Long.MIN_VALUE) == today ? prefs.getInt(countKey + petType, 0) : 0;
        if (count >= perDay) {
            return false;
        }
        long raised = PetCare.raise(fullAt(prefs, JOY + petType, now), now, amount, PetCare.BOREDOM_PER_HOUR);
        prefs.edit()
            .putLong(JOY + petType, raised)
            .putLong(dayKey + petType, today)
            .putInt(countKey + petType, count + 1)
            .apply();
        return true;
    }

    /** When the next active pet gets hungry or lonely, or Long.MAX_VALUE if none will. */
    static long nextNeedChange(Context context, long now) {
        SharedPreferences prefs = prefs(context);
        long next = Long.MAX_VALUE;
        for (int petType : activePetTypes(context)) {
            long hungry = PetCare.whenBelow(fullAt(prefs, FOOD + petType, now), PetCare.HUNGRY_BELOW,
                PetCare.HUNGER_PER_HOUR);
            long lonely = PetCare.whenBelow(fullAt(prefs, JOY + petType, now), PetCare.LONELY_BELOW,
                PetCare.BOREDOM_PER_HOUR);
            if (hungry > now) next = Math.min(next, hungry);
            if (lonely > now) next = Math.min(next, lonely);
        }
        return next;
    }

    /** Pet types that have at least one widget on the home screen, in widget order. */
    static Set<Integer> activePetTypes(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, PixelCatWidget.class));
        Set<Integer> types = new LinkedHashSet<>();
        for (int id : ids) {
            types.add(PixelCatWidget.getPetType(context, id));
        }
        return types;
    }

    // --- Wardrobe ------------------------------------------------------------------------------

    /** Accessory the pet wears, or null. */
    static String wearing(Context context, int petType) {
        String item = prefs(context).getString(WEAR + petType, null);
        if (item == null || !Wardrobe.supports(petType) || !rewards(context).unlocked.contains(item)) {
            return null;
        }
        return item;
    }

    static void wear(Context context, int petType, String item) {
        prefs(context).edit().putString(WEAR + petType, item).apply();
    }

    // --- Daily gift ----------------------------------------------------------------------------

    static DailyRewards.State rewards(Context context) {
        SharedPreferences prefs = prefs(context);
        DailyRewards.State state = new DailyRewards.State();
        state.treats = prefs.getInt(TREATS, state.treats);
        state.lastClaimDay = prefs.getLong(LAST_CLAIM_DAY, DailyRewards.NEVER);
        state.streak = prefs.getInt(STREAK, 0);
        state.freezes = prefs.getInt(FREEZES, 0);
        state.unlocked.addAll(prefs.getStringSet(UNLOCKED, new HashSet<>()));
        return state;
    }

    static void save(Context context, DailyRewards.State state) {
        prefs(context).edit()
            .putInt(TREATS, state.treats)
            .putLong(LAST_CLAIM_DAY, state.lastClaimDay)
            .putInt(STREAK, state.streak)
            .putInt(FREEZES, state.freezes)
            .putStringSet(UNLOCKED, new HashSet<>(state.unlocked))
            .apply();
    }

    /** Picks up a seasonal hat while its season lasts. */
    static boolean pickUp(Context context, String item, long now) {
        DailyRewards.State state = rewards(context);
        if (!Wardrobe.canPickUp(item, state.unlocked, PetSettings.season(context, now))) {
            return false;
        }
        state.unlocked.add(item);
        save(context, state);
        return true;
    }
}
