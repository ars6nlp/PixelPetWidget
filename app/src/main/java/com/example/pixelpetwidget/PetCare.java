package com.example.pixelpetwidget;

/**
 * How full and how happy a pet is. Both values run from 0 to 100 and slowly drop over
 * time; nothing bad ever happens at 0, the pet just shows a thought bubble on the widget.
 *
 * A value is stored as the moment it was (or would have been) at 100, so the current
 * level follows from the clock alone and no background work is needed to "tick" it.
 */
final class PetCare {

    static final float MAX = 100f;

    // Food: full to hungry in a day; one treat a day keeps the pet content
    static final float HUNGER_PER_HOUR = 2.5f;
    static final float FEED_AMOUNT = 60f;
    static final float HUNGRY_BELOW = 40f;
    /** Feeding a pet this full wastes the treat, so the app says it isn't hungry. */
    static final float FULL_ABOVE = 95f;

    // Joy: playing with the widget and stroking the pet in the app
    static final float BOREDOM_PER_HOUR = 2f;
    static final float PLAY_AMOUNT = 5f;          // one tap on the widget
    static final int PLAYS_PER_DAY = 10;
    static final float STROKE_AMOUNT = 15f;       // stroking the pet in the app
    static final int STROKES_PER_DAY = 3;
    static final float LONELY_BELOW = 30f;

    /** What the pet would like right now; shown as a bubble on the widget. */
    enum Need { NONE, HUNGRY, LONELY }

    private PetCare() {}

    /** Current level of a value that was full at {@code fullAtMs} and drops by perHour. */
    static float level(long fullAtMs, long nowMs, float perHour) {
        float hours = Math.max(0, nowMs - fullAtMs) / (float) DayClock.HOUR_MS;
        return Math.max(0f, MAX - hours * perHour);
    }

    /** New "full at" moment after raising the current level by {@code amount}, capped at full. */
    static long raise(long fullAtMs, long nowMs, float amount, float perHour) {
        float target = Math.min(MAX, level(fullAtMs, nowMs, perHour) + amount);
        return nowMs - (long) ((MAX - target) / perHour * DayClock.HOUR_MS);
    }

    /** The moment a value full at {@code fullAtMs} drops below {@code threshold}. */
    static long whenBelow(long fullAtMs, float threshold, float perHour) {
        return fullAtMs + (long) ((MAX - threshold) / perHour * DayClock.HOUR_MS);
    }

    static Need need(float food, float joy) {
        if (food < HUNGRY_BELOW) return Need.HUNGRY;
        if (joy < LONELY_BELOW) return Need.LONELY;
        return Need.NONE;
    }
}
