package com.example.pixelpetwidget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.util.Log;

/**
 * Keeps the pets' lives going on the home screen: stores each widget's {@link PetBehavior.Session}
 * and sets one alarm for the next activity of any awake pet. Frames are still played by the
 * launcher; the app only runs when an activity changes (every 5–20 s) and not at all while every
 * pet sleeps.
 *
 * <p>The alarm is an ordinary inexact one ({@link AlarmManager#set}): no exact-alarm permission,
 * no foreground service. Android delivers alarms due within 10 s on time while the phone is in use,
 * and a late one only makes the current activity last a little longer.
 */
final class PetLife {

    private static final String TAG = "PetLife";
    static final String ACTION_TICK = "com.example.pixelpetwidget.LIFE_TICK";

    // Session keys, one set per widget (suffix _<id>). The current activity's animation and
    // position keep the keys the tap cycle used, so a pet stays where it was after the update;
    // the activity that follows it in the same update has the same keys with "next_".
    private static final String PHASE = "life_phase";
    private static final String AWAKE_UNTIL = "life_awake_until";
    private static final String RANDOM = "life_random";
    private static final String FLIGHTS_LEFT = "life_flights_left";
    private static final String SEEN = "life_seen";
    private static final String EVENTS_LEFT = "life_events_left";
    private static final String LAST_EVENT_AT = "life_last_event_at";
    private static final String EVENT = "life_event";
    private static final String EVENT_NEXT = "life_event_next";
    private static final String STARTED_AT = "life_started_at";
    private static final String ENDS_AT = "life_ends_at";
    private static final String TO = "life_to";
    private static final String FACING_RIGHT = "life_facing_right";
    private static final String VARIANT = "life_variant";
    // The pixel pets' smooth walks (PetBehavior's timelines)
    private static final String GROUND = "life_ground";
    private static final String SEGMENT_GROUND = "life_segment_ground";
    private static final String TURNS = "life_turns";
    private static final String WAKES = "life_wakes";
    private static final String LIES = "life_lies";
    private static final String LOOK = "life_look";
    private static final String STEPS = "life_steps";
    private static final String FRAMED = "life_framed";
    private static final String NEXT = "life_next_";
    private static final String[] SEGMENT_KEYS = {"animation", "ends_at", "from", "to", "facing_right", "variant",
        "event", "ground", "turns", "wakes", "lies", "look", "steps", "framed"};
    private static final String[] KEYS = {PHASE, AWAKE_UNTIL, RANDOM, FLIGHTS_LEFT, SEEN, EVENTS_LEFT, LAST_EVENT_AT,
        EVENT, EVENT_NEXT, STARTED_AT, ENDS_AT, TO,
        FACING_RIGHT, VARIANT, PixelCatWidget.ANIMATION_TYPE, PixelCatWidget.LAST_WALK_OFFSET, GROUND, SEGMENT_GROUND,
        TURNS, WAKES, LIES, LOOK, STEPS, FRAMED};
    // Kept by older versions; removed on update
    private static final String[] OLD_KEYS = {"frame_counter", "force_sleep", "walk_position", "walk_direction",
        "walk_start_time", "walk_start_frame", "life_repeats"};

    private PetLife() {
    }

    static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PixelCatWidget.PREFS_NAME, Context.MODE_PRIVATE);
    }

    static PetBehavior.Session load(SharedPreferences prefs, int widgetId) {
        String id = "_" + widgetId;
        PetBehavior.Session s = new PetBehavior.Session();
        s.phase = prefs.getInt(PHASE + id, PetBehavior.ASLEEP);
        s.awakeUntil = prefs.getLong(AWAKE_UNTIL + id, 0);
        s.random = prefs.getLong(RANDOM + id, 0);
        s.flightsLeft = prefs.getInt(FLIGHTS_LEFT + id, 0);
        s.seen = prefs.getInt(SEEN + id, 0);
        s.eventsLeft = prefs.getInt(EVENTS_LEFT + id, 0);
        s.lastEventAt = prefs.getLong(LAST_EVENT_AT + id, 0);
        s.eventNext = prefs.getInt(EVENT_NEXT + id, -1);
        s.groundPx = prefs.getFloat(GROUND + id, 0f);
        PetBehavior.Segment now = s.now;
        now.animation = prefs.getInt(PixelCatWidget.ANIMATION_TYPE + id, PixelCatWidget.ANIMATION_SLEEP);
        now.startedAt = prefs.getLong(STARTED_AT + id, 0);
        now.endsAt = prefs.getLong(ENDS_AT + id, 0);
        now.from = PetBehavior.onGround(prefs.getFloat(PixelCatWidget.LAST_WALK_OFFSET + id, 0f));
        now.to = prefs.getFloat(TO + id, now.from);
        now.facingRight = prefs.getBoolean(FACING_RIGHT + id, false);
        now.variant = prefs.getInt(VARIANT + id, 0);
        now.event = prefs.getInt(EVENT + id, -1);
        now.ground = prefs.getFloat(SEGMENT_GROUND + id, 0f);
        now.turns = prefs.getBoolean(TURNS + id, false);
        now.wakes = prefs.getBoolean(WAKES + id, false);
        now.lies = prefs.getBoolean(LIES + id, false);
        now.look = prefs.getInt(LOOK + id, 0);
        now.steps = prefs.getInt(STEPS + id, 0);
        now.framed = prefs.getBoolean(FRAMED + id, false);
        if (s.phase == PetBehavior.ASLEEP) {
            now.animation = PixelCatWidget.ANIMATION_SLEEP;
            now.to = now.from;
            return s;
        }
        int nextAnimation = prefs.getInt(NEXT + "animation" + id, -1);
        if (nextAnimation >= 0) {
            PetBehavior.Segment next = new PetBehavior.Segment();
            next.animation = nextAnimation;
            next.startedAt = now.endsAt;
            next.endsAt = prefs.getLong(NEXT + "ends_at" + id, now.endsAt);
            next.from = prefs.getFloat(NEXT + "from" + id, now.from);
            next.to = prefs.getFloat(NEXT + "to" + id, next.from);
            next.facingRight = prefs.getBoolean(NEXT + "facing_right" + id, false);
            next.variant = prefs.getInt(NEXT + "variant" + id, 0);
            next.event = prefs.getInt(NEXT + "event" + id, -1);
            next.ground = prefs.getFloat(NEXT + "ground" + id, 0f);
            next.turns = prefs.getBoolean(NEXT + "turns" + id, false);
            next.wakes = prefs.getBoolean(NEXT + "wakes" + id, false);
            next.lies = prefs.getBoolean(NEXT + "lies" + id, false);
            next.look = prefs.getInt(NEXT + "look" + id, 0);
            next.steps = prefs.getInt(NEXT + "steps" + id, 0);
            next.framed = prefs.getBoolean(NEXT + "framed" + id, false);
            s.next = next;
        }
        return s;
    }

    static void save(SharedPreferences.Editor editor, int widgetId, PetBehavior.Session s) {
        String id = "_" + widgetId;
        PetBehavior.Segment now = s.now;
        editor.putInt(PHASE + id, s.phase)
            .putLong(AWAKE_UNTIL + id, s.awakeUntil)
            .putLong(RANDOM + id, s.random)
            .putInt(FLIGHTS_LEFT + id, s.flightsLeft)
            .putInt(SEEN + id, s.seen)
            .putInt(EVENTS_LEFT + id, s.eventsLeft)
            .putLong(LAST_EVENT_AT + id, s.lastEventAt)
            .putInt(EVENT_NEXT + id, s.eventNext)
            .putInt(EVENT + id, now.event)
            .putInt(PixelCatWidget.ANIMATION_TYPE + id, now.animation)
            .putLong(STARTED_AT + id, now.startedAt)
            .putLong(ENDS_AT + id, now.endsAt)
            .putFloat(PixelCatWidget.LAST_WALK_OFFSET + id, now.from)
            .putFloat(TO + id, now.to)
            .putBoolean(FACING_RIGHT + id, now.facingRight)
            .putInt(VARIANT + id, now.variant)
            .putFloat(GROUND + id, s.groundPx)
            .putFloat(SEGMENT_GROUND + id, now.ground)
            .putBoolean(TURNS + id, now.turns)
            .putBoolean(WAKES + id, now.wakes)
            .putBoolean(LIES + id, now.lies)
            .putInt(LOOK + id, now.look)
            .putInt(STEPS + id, now.steps)
            .putBoolean(FRAMED + id, now.framed);
        PetBehavior.Segment next = s.next;
        if (next == null) {
            editor.putInt(NEXT + "animation" + id, -1);
        } else {
            editor.putInt(NEXT + "animation" + id, next.animation)
                .putLong(NEXT + "ends_at" + id, next.endsAt)
                .putFloat(NEXT + "from" + id, next.from)
                .putFloat(NEXT + "to" + id, next.to)
                .putBoolean(NEXT + "facing_right" + id, next.facingRight)
                .putInt(NEXT + "variant" + id, next.variant)
                .putInt(NEXT + "event" + id, next.event)
                .putFloat(NEXT + "ground" + id, next.ground)
                .putBoolean(NEXT + "turns" + id, next.turns)
                .putBoolean(NEXT + "wakes" + id, next.wakes)
                .putBoolean(NEXT + "lies" + id, next.lies)
                .putInt(NEXT + "look" + id, next.look)
                .putInt(NEXT + "steps" + id, next.steps)
                .putBoolean(NEXT + "framed" + id, next.framed);
        }
    }

    /** Forgets a widget's life: it was removed, or a new pet starts there asleep in the middle. */
    static void remove(SharedPreferences.Editor editor, int widgetId) {
        for (String key : KEYS) {
            editor.remove(key + "_" + widgetId);
        }
        for (String key : SEGMENT_KEYS) {
            editor.remove(NEXT + key + "_" + widgetId);
        }
        for (String key : OLD_KEYS) {
            editor.remove(key + "_" + widgetId);
        }
    }

    /**
     * The life to draw now. A pixel pet whose plan ran out with nobody to move it on (a reboot, a
     * resize or a redraw from the app before a late alarm) is moved on first, as the alarm would
     * do: the widget never redraws a plan that is over. The hand-drawn pets are drawn as stored.
     */
    static PetBehavior.Session current(Context context, int widgetId, long now) {
        SharedPreferences prefs = prefs(context);
        PetBehavior.Session s = load(prefs, widgetId);
        PetSpec spec = PetSpec.forType(PixelCatWidget.getPetType(context, widgetId));
        if (spec.smooth() && s.phase != PetBehavior.ASLEEP && now + PetBehavior.EARLY_MS >= PetBehavior.planEnd(s)) {
            measureGround(context, widgetId, s);
            if (PetBehavior.advance(spec, interaction(context, widgetId), s, now)) {
                SharedPreferences.Editor editor = prefs.edit();
                save(editor, widgetId, s);
                editor.apply();
                schedule(context);
            }
        }
        return s;
    }

    /** A pixel pet's walks are planned in whole art pixels of the widget's present sizes. */
    private static void measureGround(Context context, int widgetId, PetBehavior.Session s) {
        float ground = PixelCatWidget.groundPixels(context, widgetId);
        if (ground > 0) {
            s.groundPx = ground;
        }
    }

    /** The widget's pet's interaction with its background, or null. */
    static BackgroundEvents.Interaction interaction(Context context, int widgetId) {
        return BackgroundEvents.find(PixelCatWidget.getPetType(context, widgetId),
            PixelCatWidget.getBackgroundType(context, widgetId));
    }

    static int[] widgetIds(Context context) {
        return AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, PixelCatWidget.class));
    }

    /** A tap on a widget: the pet wakes or cheers up, and the widget is redrawn. */
    static void tap(Context context, int widgetId, long now) {
        SharedPreferences prefs = prefs(context);
        PetSpec spec = PetSpec.forType(PixelCatWidget.getPetType(context, widgetId));
        PetBehavior.Session s = load(prefs, widgetId);
        measureGround(context, widgetId, s);
        // Each waking gives a new life: the seed mixes the time and the widget
        PetBehavior.tap(spec, interaction(context, widgetId), s, now, now * 31 + widgetId);
        SharedPreferences.Editor editor = prefs.edit();
        save(editor, widgetId, s);
        editor.apply();
        PixelCatWidget.updateWidget(context, AppWidgetManager.getInstance(context), widgetId);
        schedule(context);
    }

    /**
     * The alarm: every pet whose turn has come moves on. With the screen off nobody watches, so
     * every pet goes to sleep and the alarms stop.
     */
    static void tick(Context context, long now) {
        PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (power != null && !power.isInteractive()) {
            sleepAll(context, now);
            return;
        }
        SharedPreferences prefs = prefs(context);
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        for (int widgetId : widgetIds(context)) {
            PetSpec spec = PetSpec.forType(PixelCatWidget.getPetType(context, widgetId));
            PetBehavior.Session s = load(prefs, widgetId);
            if (s.phase != PetBehavior.ASLEEP && now + PetBehavior.EARLY_MS >= PetBehavior.planEnd(s)) {
                measureGround(context, widgetId, s);
            }
            if (PetBehavior.advance(spec, interaction(context, widgetId), s, now)) {
                SharedPreferences.Editor editor = prefs.edit();
                save(editor, widgetId, s);
                editor.apply();
                PixelCatWidget.updateWidget(context, manager, widgetId);
            }
        }
        schedule(context);
    }

    /** Every pet asleep where it stands and redrawn, no alarm left. */
    static void sleepAll(Context context, long now) {
        SharedPreferences prefs = prefs(context);
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        for (int widgetId : widgetIds(context)) {
            PetSpec spec = PetSpec.forType(PixelCatWidget.getPetType(context, widgetId));
            PetBehavior.Session s = load(prefs, widgetId);
            PetBehavior.sleepNow(spec, s, now);
            SharedPreferences.Editor editor = prefs.edit();
            save(editor, widgetId, s);
            editor.apply();
            PixelCatWidget.updateWidget(context, manager, widgetId);
        }
        cancel(context);
    }

    /**
     * After a reboot, an app update or a clock change: drops what the old tap cycle kept and puts
     * every pet to sleep.
     */
    static void restart(Context context) {
        SharedPreferences prefs = prefs(context);
        SharedPreferences.Editor editor = prefs.edit();
        for (int widgetId : widgetIds(context)) {
            for (String key : OLD_KEYS) {
                editor.remove(key + "_" + widgetId);
            }
        }
        editor.apply();
        sleepAll(context, System.currentTimeMillis());
    }

    /** Sets the alarm for the next turn of any awake pet, or cancels it when all of them sleep. */
    static void schedule(Context context) {
        SharedPreferences prefs = prefs(context);
        long next = Long.MAX_VALUE;
        for (int widgetId : widgetIds(context)) {
            next = Math.min(next, PetBehavior.nextTurn(load(prefs, widgetId)));
        }
        if (next == Long.MAX_VALUE) {
            cancel(context);
            return;
        }
        try {
            AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            // Not a wakeup alarm: with the phone asleep there is nobody to watch the pet
            alarms.set(AlarmManager.RTC, next, tickIntent(context));
        } catch (RuntimeException e) {
            Log.e(TAG, "Could not set the alarm", e);
        }
    }

    static void cancel(Context context) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms != null) {
            alarms.cancel(tickIntent(context));
        }
    }

    private static PendingIntent tickIntent(Context context) {
        Intent intent = new Intent(context, PixelCatWidget.class).setAction(ACTION_TICK);
        return PendingIntent.getBroadcast(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
