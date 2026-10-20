package com.example.pixelpetwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SizeF;
import android.view.View;
import android.widget.RemoteViews;

import com.example.pixelpetwidget.PlanFrames.Stage;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Home screen widget with an animated pixel pet. A tap wakes the pet; it then lives on its own
 * for a while (PetBehavior, kept going by PetLife) and falls asleep again. Frames are played by a
 * ViewFlipper inside the launcher, so no code runs per frame, only when the pet changes activity.
 */
public class PixelCatWidget extends AppWidgetProvider {

    private static final String TAG = "PixelCatWidget";

    public static final String PREFS_NAME = "PixelCatWidgetPrefs";
    public static final String ANIMATION_TYPE = "animation_type";
    public static final String PET_TYPE = "pet_type";
    public static final String ACTION_TOGGLE_ANIMATION = "com.example.pixelpetwidget.TOGGLE_ANIMATION";
    /** Alarm of older versions that put every pet to sleep; one may still come after the update. */
    public static final String ACTION_FORCE_SLEEP_ALL = "com.example.pixelpetwidget.FORCE_SLEEP_ALL";
    /** Where the pet stands on the ground (-1..1 of its walking range). */
    public static final String LAST_WALK_OFFSET = "last_walk_offset";
    public static final String BG_TYPE = "bg_type";

    // Frame timings and sizes per pet live in PetSpec
    static final int MAX_LOOP_FRAMES = 16;
    // PendingIntent request codes of the need bubbles: one per widget, apart from the tap intents
    private static final int BUBBLE_REQUEST_BASE = 100_000;

    // Layout constants
    private static final int DEFAULT_WIDGET_SIZE_DP = 250;

    // Animation types
    public static final int ANIMATION_SLEEP = 0;
    public static final int ANIMATION_LICK = 1;
    public static final int ANIMATION_WALK = 2;
    public static final int ANIMATION_EAT = 3;
    public static final int ANIMATION_SEAT = 4;
    public static final int ANIMATION_HAPPY = 5;
    public static final int ANIMATION_IDLE = 6;
    // Each pixel pet's own trick: the panda rolls, the hamster stuffs its cheeks, the capybara
    // takes a bath, the parrot chatters
    public static final int ANIMATION_SPECIAL = 7;
    // The parrot's flight: the take-off, the wingbeat loop and the landing (one activity)
    public static final int ANIMATION_FLY = 8;
    public static final int ANIMATION_TAKEOFF = 9;
    public static final int ANIMATION_LAND = 10;
    // 11 stands for a background event in the activity choice (BackgroundEvents.ANIMATION_EVENT).
    // The pixel pets' own poses for their interactions with the backgrounds: each pet has its
    // own (BackgroundEvents names them); they are never chosen as activities of their own
    public static final int ANIMATION_POSE_1 = 12;
    public static final int ANIMATION_POSE_2 = 13;
    public static final int ANIMATION_POSE_3 = 14;
    public static final int ANIMATION_POSE_4 = 15;
    // The pixel pets' transitions, played inside other animations, never on their own: turning
    // round (to the viewer and on to the other side), the first and last step of a walk, lying
    // down to sleep and getting up (PetBehavior's timelines)
    public static final int ANIMATION_TURN = 16;
    public static final int ANIMATION_WALK_START = 17;
    public static final int ANIMATION_WALK_STOP = 18;
    public static final int ANIMATION_LIE_DOWN = 19;
    public static final int ANIMATION_GET_UP = 20;

    // Pet types
    public static final int PET_CAT = 0;
    public static final int PET_DOG = 1;
    public static final int PET_RABBIT = 2;
    // 3 and 4 belonged to the old parrot and capybara clips; the redrawn pets took them over
    public static final int PET_PARROT = 3;
    public static final int PET_CAPYBARA = 4;
    public static final int PET_PANDA = 5;
    public static final int PET_HAMSTER = 6;
    public static final int PET_AQUARIUM = 7;
    public static final int PET_PUPPY = 8;
    public static final int PET_KITTEN = 9;
    // Sent by the launcher after a widget pinned from the app was placed
    public static final String ACTION_WIDGET_PINNED = "com.example.pixelpetwidget.WIDGET_PINNED";
    public static final String EXTRA_PET_TYPE = "com.example.pixelpetwidget.extra.PET_TYPE";
    public static final String EXTRA_BG_TYPE = "com.example.pixelpetwidget.extra.BG_TYPE";

    // Array of drawable resources for sleep animation frames (16 frames total)
    public static final int[] SLEEP_FRAMES = {
        R.drawable.cat_sleep_frame_001,
        R.drawable.cat_sleep_frame_002,
        R.drawable.cat_sleep_frame_003,
        R.drawable.cat_sleep_frame_004,
        R.drawable.cat_sleep_frame_005,
        R.drawable.cat_sleep_frame_006,
        R.drawable.cat_sleep_frame_007,
        R.drawable.cat_sleep_frame_008,
        R.drawable.cat_sleep_frame_009,
        R.drawable.cat_sleep_frame_010,
        R.drawable.cat_sleep_frame_011,
        R.drawable.cat_sleep_frame_012,
        R.drawable.cat_sleep_frame_013,
        R.drawable.cat_sleep_frame_014,
        R.drawable.cat_sleep_frame_015,
        R.drawable.cat_sleep_frame_016
    };

    // Array of drawable resources for lick animation frames
    public static final int[] LICK_FRAMES = {
        R.drawable.cat_lick_frame_001,
        R.drawable.cat_lick_frame_002,
        R.drawable.cat_lick_frame_003,
        R.drawable.cat_lick_frame_004,
        R.drawable.cat_lick_frame_005,
        R.drawable.cat_lick_frame_006,
        R.drawable.cat_lick_frame_007,
        R.drawable.cat_lick_frame_008,
        R.drawable.cat_lick_frame_009,
        R.drawable.cat_lick_frame_010,
        R.drawable.cat_lick_frame_011,
        R.drawable.cat_lick_frame_012,
        R.drawable.cat_lick_frame_013,
        R.drawable.cat_lick_frame_014
    };

    // Array of drawable resources for walk animation frames (16 frames)
    public static final int[] WALK_FRAMES = {
        R.drawable.cat_walk_frame_001,
        R.drawable.cat_walk_frame_002,
        R.drawable.cat_walk_frame_003,
        R.drawable.cat_walk_frame_004,
        R.drawable.cat_walk_frame_005,
        R.drawable.cat_walk_frame_006,
        R.drawable.cat_walk_frame_007,
        R.drawable.cat_walk_frame_008,
        R.drawable.cat_walk_frame_009,
        R.drawable.cat_walk_frame_010,
        R.drawable.cat_walk_frame_011,
        R.drawable.cat_walk_frame_012,
        R.drawable.cat_walk_frame_013,
        R.drawable.cat_walk_frame_014,
        R.drawable.cat_walk_frame_015,
        R.drawable.cat_walk_frame_016
    };

    // Array of drawable resources for seat animation frames (14 frames)
    public static final int[] CAT_SEAT_FRAMES = {
        R.drawable.cat_seat_frame_001,
        R.drawable.cat_seat_frame_002,
        R.drawable.cat_seat_frame_003,
        R.drawable.cat_seat_frame_004,
        R.drawable.cat_seat_frame_005,
        R.drawable.cat_seat_frame_006,
        R.drawable.cat_seat_frame_007,
        R.drawable.cat_seat_frame_008,
        R.drawable.cat_seat_frame_009,
        R.drawable.cat_seat_frame_010,
        R.drawable.cat_seat_frame_011,
        R.drawable.cat_seat_frame_012,
        R.drawable.cat_seat_frame_013,
        R.drawable.cat_seat_frame_014
    };

    // Dog sleep animation frames (16 frames)
    public static final int[] DOG_SLEEP_FRAMES = {
        R.drawable.dog_sleep_frame_001,
        R.drawable.dog_sleep_frame_002,
        R.drawable.dog_sleep_frame_003,
        R.drawable.dog_sleep_frame_004,
        R.drawable.dog_sleep_frame_005,
        R.drawable.dog_sleep_frame_006,
        R.drawable.dog_sleep_frame_007,
        R.drawable.dog_sleep_frame_008,
        R.drawable.dog_sleep_frame_009,
        R.drawable.dog_sleep_frame_010,
        R.drawable.dog_sleep_frame_011,
        R.drawable.dog_sleep_frame_012,
        R.drawable.dog_sleep_frame_013,
        R.drawable.dog_sleep_frame_014,
        R.drawable.dog_sleep_frame_015,
        R.drawable.dog_sleep_frame_016
    };

    // Dog sit animation frames (16 frames)
    public static final int[] DOG_SIT_FRAMES = {
        R.drawable.dog_sit_frame_001,
        R.drawable.dog_sit_frame_002,
        R.drawable.dog_sit_frame_003,
        R.drawable.dog_sit_frame_004,
        R.drawable.dog_sit_frame_005,
        R.drawable.dog_sit_frame_006,
        R.drawable.dog_sit_frame_007,
        R.drawable.dog_sit_frame_008,
        R.drawable.dog_sit_frame_009,
        R.drawable.dog_sit_frame_010,
        R.drawable.dog_sit_frame_011,
        R.drawable.dog_sit_frame_012,
        R.drawable.dog_sit_frame_013,
        R.drawable.dog_sit_frame_014,
        R.drawable.dog_sit_frame_015,
        R.drawable.dog_sit_frame_016
    };

    // Dog walk animation frames (16 frames)
    public static final int[] DOG_WALK_FRAMES = {
        R.drawable.dog_walk_frame_001,
        R.drawable.dog_walk_frame_002,
        R.drawable.dog_walk_frame_003,
        R.drawable.dog_walk_frame_004,
        R.drawable.dog_walk_frame_005,
        R.drawable.dog_walk_frame_006,
        R.drawable.dog_walk_frame_007,
        R.drawable.dog_walk_frame_008,
        R.drawable.dog_walk_frame_009,
        R.drawable.dog_walk_frame_010,
        R.drawable.dog_walk_frame_011,
        R.drawable.dog_walk_frame_012,
        R.drawable.dog_walk_frame_013,
        R.drawable.dog_walk_frame_014,
        R.drawable.dog_walk_frame_015,
        R.drawable.dog_walk_frame_016
    };

    // Dog eat animation frames (16 frames)
    public static final int[] DOG_EAT_FRAMES = {
        R.drawable.dog_eat_frame_010,
        R.drawable.dog_eat_frame_011,
        R.drawable.dog_eat_frame_012,
        R.drawable.dog_eat_frame_013,
        R.drawable.dog_eat_frame_015,
        R.drawable.dog_eat_frame_016,
        R.drawable.dog_eat_frame_017,
        R.drawable.dog_eat_frame_018,
        R.drawable.dog_eat_frame_019,
        R.drawable.dog_eat_frame_020,
        R.drawable.dog_eat_frame_021,
        R.drawable.dog_eat_frame_022,
        R.drawable.dog_eat_frame_024,
        R.drawable.dog_eat_frame_029,
        R.drawable.dog_eat_frame_030,
        R.drawable.dog_eat_frame_032
    };

    // Rabbit sleep animation frames (16 frames)
    public static final int[] RABBIT_SLEEP_FRAMES = {
        R.drawable.rabbit_sleep_frame_001,
        R.drawable.rabbit_sleep_frame_002,
        R.drawable.rabbit_sleep_frame_003,
        R.drawable.rabbit_sleep_frame_004,
        R.drawable.rabbit_sleep_frame_005,
        R.drawable.rabbit_sleep_frame_006,
        R.drawable.rabbit_sleep_frame_007,
        R.drawable.rabbit_sleep_frame_008,
        R.drawable.rabbit_sleep_frame_009,
        R.drawable.rabbit_sleep_frame_010,
        R.drawable.rabbit_sleep_frame_011,
        R.drawable.rabbit_sleep_frame_012,
        R.drawable.rabbit_sleep_frame_013,
        R.drawable.rabbit_sleep_frame_014,
        R.drawable.rabbit_sleep_frame_015,
        R.drawable.rabbit_sleep_frame_016
    };

    // Rabbit walk animation frames (16 frames)
    public static final int[] RABBIT_WALK_FRAMES = {
        R.drawable.rabbit_walk_frame_001,
        R.drawable.rabbit_walk_frame_002,
        R.drawable.rabbit_walk_frame_003,
        R.drawable.rabbit_walk_frame_004,
        R.drawable.rabbit_walk_frame_005,
        R.drawable.rabbit_walk_frame_006,
        R.drawable.rabbit_walk_frame_007,
        R.drawable.rabbit_walk_frame_008,
        R.drawable.rabbit_walk_frame_009,
        R.drawable.rabbit_walk_frame_010,
        R.drawable.rabbit_walk_frame_011,
        R.drawable.rabbit_walk_frame_012,
        R.drawable.rabbit_walk_frame_013,
        R.drawable.rabbit_walk_frame_014,
        R.drawable.rabbit_walk_frame_015,
        R.drawable.rabbit_walk_frame_016
    };

    // Rabbit eat animation frames (16 frames)
    public static final int[] RABBIT_EAT_FRAMES = {
        R.drawable.rabbit_eat_frame_016,
        R.drawable.rabbit_eat_frame_017,
        R.drawable.rabbit_eat_frame_018,
        R.drawable.rabbit_eat_frame_019,
        R.drawable.rabbit_eat_frame_020,
        R.drawable.rabbit_eat_frame_021,
        R.drawable.rabbit_eat_frame_022,
        R.drawable.rabbit_eat_frame_023,
        R.drawable.rabbit_eat_frame_024,
        R.drawable.rabbit_eat_frame_025,
        R.drawable.rabbit_eat_frame_026,
        R.drawable.rabbit_eat_frame_027,
        R.drawable.rabbit_eat_frame_028,
        R.drawable.rabbit_eat_frame_029,
        R.drawable.rabbit_eat_frame_030,
        R.drawable.rabbit_eat_frame_031
    };

    // Rabbit sit animation frames (16 frames)
    public static final int[] RABBIT_SIT_FRAMES = {
        R.drawable.rabbit_sit_frame_002,
        R.drawable.rabbit_sit_frame_003,
        R.drawable.rabbit_sit_frame_004,
        R.drawable.rabbit_sit_frame_005,
        R.drawable.rabbit_sit_frame_006,
        R.drawable.rabbit_sit_frame_009,
        R.drawable.rabbit_sit_frame_013,
        R.drawable.rabbit_sit_frame_014,
        R.drawable.rabbit_sit_frame_015,
        R.drawable.rabbit_sit_frame_017,
        R.drawable.rabbit_sit_frame_018,
        R.drawable.rabbit_sit_frame_019,
        R.drawable.rabbit_sit_frame_020,
        R.drawable.rabbit_sit_frame_023,
        R.drawable.rabbit_sit_frame_024,
        R.drawable.rabbit_sit_frame_028
    };


    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();

        if (ACTION_TOGGLE_ANIMATION.equals(action)) {
            int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                onTap(context, widgetId);
            }
        } else if (PetLife.ACTION_TICK.equals(action)) {
            PetLife.tick(context, System.currentTimeMillis());
        } else if (ACTION_FORCE_SLEEP_ALL.equals(action)) {
            PetLife.sleepAll(context, System.currentTimeMillis());
        } else if (CareScheduler.ACTION_CARE_TICK.equals(action)) {
            long now = System.currentTimeMillis();
            if (PetSettings.nightMode(context) && PetSettings.isNight(context, now)) {
                // Bedtime: everyone goes to sleep; a tap still wakes a pet up
                PetLife.sleepAll(context, now);
            } else {
                // Moods and seasons change; a pet whose alarms went missing falls asleep here
                PetLife.tick(context, now);
                updateAllWidgets(context);
            }
            CareScheduler.scheduleCareTick(context);
        } else if (ACTION_WIDGET_PINNED.equals(action)) {
            int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                configureNewWidget(context, widgetId,
                    intent.getIntExtra(EXTRA_PET_TYPE, PET_CAT), intent.getIntExtra(EXTRA_BG_TYPE, 1));
            }
        }
    }

    /** Sets up a freshly added widget with the pet and background chosen in the app; the pet starts asleep. */
    public static void configureNewWidget(Context context, int widgetId, int petType, int bgType) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        PetLife.remove(editor, widgetId);
        editor.putInt(PET_TYPE + "_" + widgetId, petType)
            .putInt(BG_TYPE + "_" + widgetId, bgType)
            .apply();
        updateWidget(context, AppWidgetManager.getInstance(context), widgetId);
        PetLife.schedule(context);
        CareScheduler.scheduleCareTick(context);
    }

    /**
     * A new background for a widget already on the home screen (the picker): its pet goes on as it
     * was; an interaction with the old background is simply over.
     */
    static void setBackground(Context context, int widgetId, int bgType) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putInt(BG_TYPE + "_" + widgetId, bgType)
            .apply();
        updateWidget(context, AppWidgetManager.getInstance(context), widgetId);
    }

    /** Redraws every widget, e.g. after feeding a pet or changing what it wears in the app. */
    public static void updateAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        for (int widgetId : appWidgetManager.getAppWidgetIds(new ComponentName(context, PixelCatWidget.class))) {
            updateWidget(context, appWidgetManager, widgetId);
        }
    }

    /** Redraws the widgets showing one kind of pet, e.g. after it was fed. */
    public static void updateWidgetsOf(Context context, int petType) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        for (int widgetId : appWidgetManager.getAppWidgetIds(new ComponentName(context, PixelCatWidget.class))) {
            if (getPetType(context, widgetId) == petType) {
                updateWidget(context, appWidgetManager, widgetId);
            }
        }
    }

    /** Pet type for a pet name stored by the main screen ("cat", "dog", ...). */
    public static int petTypeFromName(String name) {
        if ("dog".equals(name)) return PET_DOG;
        if ("rabbit".equals(name)) return PET_RABBIT;
        if ("panda".equals(name)) return PET_PANDA;
        if ("hamster".equals(name)) return PET_HAMSTER;
        if ("capybara".equals(name)) return PET_CAPYBARA;
        if ("parrot".equals(name)) return PET_PARROT;
        if ("aquarium".equals(name)) return PET_AQUARIUM;
        if ("puppy".equals(name)) return PET_PUPPY;
        if ("kitten".equals(name)) return PET_KITTEN;
        return PET_CAT;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        for (int appWidgetId : appWidgetIds) {
            // Widgets that skipped the configuration screen default to the rabbit
            if (!prefs.contains(PET_TYPE + "_" + appWidgetId)) {
                prefs.edit().putInt(PET_TYPE + "_" + appWidgetId, PET_RABBIT).apply();
            }
            updateWidget(context, appWidgetManager, appWidgetId);
        }
        PetLife.schedule(context);
        CareScheduler.scheduleCareTick(context);
    }

    @Override
    public void onEnabled(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] widgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, PixelCatWidget.class));
        if (widgetIds.length > 0) {
            onUpdate(context, appWidgetManager, widgetIds);
        }
    }

    @Override
    public void onDisabled(Context context) {
        PetLife.cancel(context);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {
        // Re-render the widget with correct padding for the new size
        updateWidget(context, appWidgetManager, appWidgetId);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        for (int appWidgetId : appWidgetIds) {
            PetLife.remove(editor, appWidgetId);
            editor.remove(PET_TYPE + "_" + appWidgetId);
            editor.remove(BG_TYPE + "_" + appWidgetId);
        }
        editor.apply();

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] remainingWidgets = appWidgetManager.getAppWidgetIds(new ComponentName(context, PixelCatWidget.class));
        // The removed pet may have been the only one awake
        PetLife.schedule(context);
        if (remainingWidgets.length == 0) {
            CareScheduler.cancelCareTick(context);
        } else {
            // The pets left on the home screen may have changed
            CareScheduler.scheduleCareTick(context);
        }

        super.onDeleted(context, appWidgetIds);
    }

    private static void onTap(Context context, int widgetId) {
        long now = System.currentTimeMillis();
        // Playing with the pet on the home screen cheers it up (a few times a day)
        if (CareStore.play(context, getPetType(context, widgetId), now)) {
            CareScheduler.scheduleCareTick(context);
        }
        PetLife.tap(context, widgetId, now);
    }

    /** Frames for the given pet and animation. */
    static int[] framesFor(int petType, int animationType) {
        return PetSpec.forType(petType).animation(animationType).frames;
    }

    // Public static method for updating widget from other classes
    public static void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int petType = prefs.getInt(PET_TYPE + "_" + appWidgetId, PET_CAT);
        int bgType = prefs.getInt(BG_TYPE + "_" + appWidgetId, 1);
        long now = System.currentTimeMillis();
        PetBehavior.Session session = PetLife.current(context, appWidgetId, now);

        Look look = lookFor(context, petType, now);
        Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
        try {
            appWidgetManager.updateAppWidget(appWidgetId, viewsFor(context, appWidgetId, petType, bgType, session, now,
                look, options, false));
        } catch (RuntimeException e) {
            // The launcher refused the update (it should not happen, see PlanFrames.MAX_PLAN_DEPTH):
            // the pet is shown still where it is rather than the app going down with it
            Log.e(TAG, "Widget update refused, showing the pet still", e);
            try {
                appWidgetManager.updateAppWidget(appWidgetId, viewsFor(context, appWidgetId, petType, bgType, session,
                    now, look, options, true));
            } catch (RuntimeException again) {
                Log.e(TAG, "Still widget refused too", again);
            }
        }
    }

    /** The widget's views at every size the launcher may show; {@code still}: the pet keeps still. */
    private static RemoteViews viewsFor(Context context, int appWidgetId, int petType, int bgType,
                                        PetBehavior.Session session, long now, Look look, Bundle options,
                                        boolean still) {
        List<SizeF> sizes = getWidgetSizes(options);
        RemoteViews views;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !sizes.isEmpty()) {
            // Android 12+: one layout per size the launcher may show (portrait, landscape, foldables)
            Map<SizeF, RemoteViews> viewsBySize = new ArrayMap<>();
            for (SizeF size : sizes) {
                viewsBySize.put(size, buildViews(context, appWidgetId, petType, bgType, session, now, look,
                    size.getWidth(), size.getHeight(), still));
            }
            views = new RemoteViews(viewsBySize);
        } else {
            // Launchers report portrait as min width x max height, landscape as max width x min height
            int minWidth = sizeOrDefault(options, AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            int maxWidth = sizeOrDefault(options, AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
            int minHeight = sizeOrDefault(options, AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
            int maxHeight = sizeOrDefault(options, AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
            RemoteViews portrait = buildViews(context, appWidgetId, petType, bgType, session, now, look,
                minWidth, maxHeight, still);
            RemoteViews landscape = buildViews(context, appWidgetId, petType, bgType, session, now, look,
                maxWidth, minHeight, still);
            views = new RemoteViews(landscape, portrait);
        }
        return views;
    }

    @SuppressWarnings("deprecation")
    private static List<SizeF> getWidgetSizes(Bundle options) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || options == null) {
            return Collections.emptyList();
        }
        List<SizeF> sizes = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            ? options.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, SizeF.class)
            : options.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES);
        if (sizes == null) {
            return Collections.emptyList();
        }
        // RemoteViews accepts at most 16 sizes
        return sizes.size() > 16 ? sizes.subList(0, 16) : sizes;
    }

    private static int sizeOrDefault(Bundle options, String key) {
        int size = options != null ? options.getInt(key) : 0;
        // Some launchers report 0 before the widget is laid out
        return size > 0 ? size : DEFAULT_WIDGET_SIZE_DP;
    }

    /** What the widget shows around the pet, worked out once per update. */
    private static final class Look {
        String accessory;
        PetCare.Need need;
        boolean night;
        String season;
    }

    private static Look lookFor(Context context, int petType, long now) {
        Look look = new Look();
        look.accessory = CareStore.wearing(context, petType);
        look.need = CareStore.need(context, petType, now);
        look.night = PetSettings.nightMode(context) && PetSettings.isNight(context, now);
        if (PetSettings.seasonalDecor(context)) {
            look.season = PetSettings.season(context, now);
        }
        return look;
    }

    /** Pixel art is scaled by whole numbers only: the size closest to desiredPx, at least 1x. */
    static int pixelSnappedSize(float desiredPx, int grid) {
        return Math.max(1, Math.round(desiredPx / grid)) * grid;
    }

    private static RemoteViews buildViews(Context context, int appWidgetId, int petType, int bgType,
                                          PetBehavior.Session session, long now, Look look,
                                          float widthDp, float heightDp, boolean still) {
        PetSpec spec = PetSpec.forType(petType);
        boolean asleep = session.phase == PetBehavior.ASLEEP;
        // Asleep, or kept still: one picture, nothing runs
        boolean frozen = asleep || still;
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.pixel_cat_widget);

        // Background image
        int bgResId = R.drawable.widget_background;
        if (bgType == 2) {
            bgResId = R.drawable.widget_background_2;
        } else if (bgType == 3) {
            bgResId = R.drawable.widget_background_3;
        }
        views.setImageViewResource(R.id.background_image, bgResId);

        // The frames' flipper comes new with every update (a reused one would keep its old timer)
        views.removeAllViews(R.id.pet_frames);

        Backdrop backdrop = spec.scene ? null : Backdrop.forType(bgType);
        Stage stage = stageFor(context, spec, session, widthDp, heightDp, backdrop);
        views.removeAllViews(R.id.bg_props);
        views.removeAllViews(R.id.bg_front);
        views.setViewVisibility(R.id.bg_stage, backdrop != null ? View.VISIBLE : View.GONE);
        views.setViewVisibility(R.id.bg_front, backdrop != null ? View.VISIBLE : View.GONE);
        // The launcher may reuse the views of the last update: every visibility is set each time.
        // A scene is its own background; a pixel-art background replaces the picture
        views.setViewVisibility(R.id.background_image, spec.scene || backdrop != null ? View.GONE : View.VISIBLE);
        // The interaction with the background playing in this plan, if any
        PetBehavior.Segment act = frozen || backdrop == null ? null : actOf(spec, session);
        BackgroundEvents.Interaction interaction = act != null ? BackgroundEvents.at(act.event, petType, bgType) : null;
        if (backdrop != null) {
            // It freezes with the pet
            showBackdrop(context, views, backdrop, stage, frozen,
                interaction != null && interaction.effect == BackgroundEvents.STATE ? interaction.prop : null);
        }
        if (frozen) {
            // Asleep: one still picture where the pet lay down, nothing runs. Kept still: its
            // standing pose where it is now
            PetBehavior.Segment rest = asleep ? session.now : PetBehavior.segmentAt(session, now);
            float from = asleep ? rest.from : PetBehavior.positionAt(spec, rest, now);
            boolean facingRight = asleep ? rest.facingRight : PetBehavior.facingRightAt(spec, rest, now);
            views.setViewVisibility(R.id.pet_frames, View.GONE);
            views.setViewVisibility(R.id.pet_static, View.VISIBLE);
            int offset = stage.offset(from);
            views.setViewPadding(R.id.pet_container, stage.left + offset, stage.top, stage.right - offset, stage.bottom);
            views.setImageViewResource(R.id.pet_static,
                spec.animation(asleep ? ANIMATION_SLEEP : ANIMATION_IDLE, look.accessory).frames[0]);
            views.setFloat(R.id.pet_static, "setScaleX", stage.scaleX(spec, facingRight));
        } else {
            // Awake or dozing off: the launcher's ViewFlipper plays the plan
            views.setViewVisibility(R.id.pet_frames, View.VISIBLE);
            views.setViewVisibility(R.id.pet_static, View.GONE);
            // Each frame places itself with its own padding
            views.setViewPadding(R.id.pet_container, 0, 0, 0, 0);
            addPlan(context, views, spec, session, now, look, stage);
            if (interaction != null) {
                addEffect(context, views, spec, session, act, interaction, now, backdrop, stage);
            }
        }
        if (backdrop != null) {
            addTiles(context, views, backdrop, frozen);
        }

        renderSurroundings(context, views, appWidgetId, spec, look, backdrop != null);

        // A tap wakes the pet or cheers it up (handled in onReceive)
        Intent intent = new Intent(context, PixelCatWidget.class);
        intent.setAction(ACTION_TOGGLE_ANIMATION);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent);

        return views;
    }

    /** Where the pet goes at one widget size (see PlanFrames.Stage); a pixel pet with the widget's ground. */
    private static Stage stageFor(Context context, PetSpec spec, PetBehavior.Session session, float widthDp,
                                  float heightDp, Backdrop backdrop) {
        float density = context.getResources().getDisplayMetrics().density;
        if (spec.scene) {
            return Stage.forScene(spec, widthDp, heightDp, density,
                context.getResources().getDimensionPixelSize(R.dimen.widget_frame_padding));
        }
        float framePaddingDp = context.getResources().getDimension(R.dimen.widget_frame_padding) / density;
        return Stage.forPet(spec, widthDp, heightDp, density, framePaddingDp, backdrop)
            .withGround(PetBehavior.ground(session));
    }

    /**
     * Art pixels per ground unit a pixel pet walks with on this widget: what its narrowest size
     * has room for, so a walk covers the same art pixels at every size (0 for a hand-drawn pet).
     */
    static float groundPixels(Context context, int widgetId) {
        PetSpec spec = PetSpec.forType(getPetType(context, widgetId));
        if (!spec.smooth() || spec.scene) {
            return 0f;
        }
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        Bundle options = manager.getAppWidgetOptions(widgetId);
        Backdrop backdrop = Backdrop.forType(getBackgroundType(context, widgetId));
        float density = context.getResources().getDisplayMetrics().density;
        float framePaddingDp = context.getResources().getDimension(R.dimen.widget_frame_padding) / density;
        List<SizeF> sizes = getWidgetSizes(options);
        float ground = Float.MAX_VALUE;
        if (sizes.isEmpty()) {
            float[][] old = {{sizeOrDefault(options, AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH),
                sizeOrDefault(options, AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)},
                {sizeOrDefault(options, AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH),
                    sizeOrDefault(options, AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)}};
            for (float[] size : old) {
                ground = Math.min(ground, Stage.forPet(spec, size[0], size[1], density, framePaddingDp, backdrop).roomPx());
            }
        } else {
            for (SizeF size : sizes) {
                ground = Math.min(ground, Stage.forPet(spec, size.getWidth(), size.getHeight(), density,
                    framePaddingDp, backdrop).roomPx());
            }
        }
        return ground == Float.MAX_VALUE ? 0f : Math.max(1f, ground);
    }

    /** Rows under a pixel pet's feet in its frame (its shadow), so it stands on the ground line. */
    static final int FEET_ROWS = Stage.FEET_ROWS;

    /**
     * A pixel-art background: the stage is scaled by the pet's pixel scale from its centre, so a
     * point of the picture lands on screen where the pet expects it; its ground line is
     * {@link Backdrop#groundMargin} pixels above the bottom. Its front layer (bg_front) is scaled
     * the same way. Awake, its elements and props play; asleep it is a still picture.
     *
     * @param busyProp a prop an interaction is playing with: the interaction draws it (or null)
     */
    private static void showBackdrop(Context context, RemoteViews views, Backdrop backdrop, Stage stage,
                                     boolean asleep, String busyProp) {
        int k = stage.scale;
        for (int layer : new int[] {R.id.bg_stage, R.id.bg_front}) {
            views.setFloat(layer, "setScaleX", k);
            views.setFloat(layer, "setScaleY", k);
        }
        views.setInt(R.id.bg_stage, "setBackgroundColor", backdrop.sky);
        views.setInt(R.id.bg_earth, "setBackgroundColor", backdrop.earth);
        views.setImageViewResource(R.id.bg_picture, backdrop.image);
        int left = canvasLeft(backdrop, stage);
        int top = canvasTop(backdrop, stage);
        // scaleType center puts the picture in the middle of its padded box: pad so that its
        // corner lands on (left, top) before the stage's scaling
        int padX = 2 * left + backdrop.width - stage.width;
        int padY = 2 * top + backdrop.height - stage.height;
        views.setViewPadding(R.id.bg_picture, Math.max(0, padX), Math.max(0, padY), Math.max(0, -padX),
            Math.max(0, -padY));
        // Earth from just under the ground line down, for a widget taller or wider than the picture
        views.setViewPadding(R.id.bg_earth_holder, 0, Math.max(0, top + backdrop.ground + 3), 0, 0);
        if (!asleep) {
            for (Backdrop.Element element : backdrop.elements) {
                RemoteViews loop = new RemoteViews(context.getPackageName(), R.layout.item_pet_loop);
                loop.setInt(R.id.loop_flipper, "setFlipInterval", element.intervalMs);
                int added = 0;
                for (int f = 0; f < element.frames.length; f++) {
                    RemoteViews frame = prop(context, stage, element.frames[f], left + element.x[f],
                        top + element.y[f], element.width, element.height);
                    if (frame != null) {
                        loop.addView(R.id.loop_flipper, frame);
                        added++;
                    }
                }
                if (added == element.frames.length) {
                    views.addView(R.id.bg_props, loop);
                }
            }
        }
        for (Backdrop.Prop prop : backdrop.props) {
            if (prop.name.equals(busyProp)) {
                continue;
            }
            RemoteViews view = propState(context, backdrop, stage, prop, prop.state(prop.ambient), 0, asleep);
            if (view != null) {
                views.addView(prop.front ? R.id.bg_front : R.id.bg_props, view);
            }
        }
    }

    /**
     * A prop in one of its states at its place: a loop starting {@code elapsedMs} into it, or its
     * first frame when the background is still. Null where it does not fit on the stage.
     */
    private static RemoteViews propState(Context context, Backdrop backdrop, Stage stage, Backdrop.Prop prop,
                                         Backdrop.State state, long elapsedMs, boolean still) {
        int x = propX(backdrop, stage, prop);
        int y = canvasTop(backdrop, stage) + backdrop.ground - prop.baseline;
        if (still || state.frames.length == 1) {
            return prop(context, stage, state.frames[0], x, y, prop.width, prop.height);
        }
        RemoteViews loop = new RemoteViews(context.getPackageName(), R.layout.item_pet_loop);
        loop.setInt(R.id.loop_flipper, "setFlipInterval", state.intervalMs);
        long base = elapsedMs / state.intervalMs;
        for (int j = 0; j < state.frames.length; j++) {
            RemoteViews frame = prop(context, stage, state.frames[(int) ((base + j) % state.frames.length)], x, y,
                prop.width, prop.height);
            if (frame == null) {
                return null;
            }
            loop.addView(R.id.loop_flipper, frame);
        }
        return loop;
    }

    /** Left edge of a prop on the unscaled stage: centred where a pet at its ground position stands. */
    private static int propX(Backdrop backdrop, Stage stage, Backdrop.Prop prop) {
        float centre = stage.left + stage.offset(prop.offset) + (stage.width - stage.left - stage.right) / 2f;
        return canvasLeft(backdrop, stage) + Math.round(canvasX(backdrop, stage, centre) - prop.width / 2f);
    }

    /** Layers tiled over everything (the rain), in the front layer; still while the pet sleeps. */
    private static void addTiles(Context context, RemoteViews views, Backdrop backdrop, boolean asleep) {
        for (Backdrop.Tile tile : backdrop.tiles) {
            if (asleep) {
                views.addView(R.id.bg_front, tiled(context, tile.frames[0]));
                continue;
            }
            RemoteViews loop = new RemoteViews(context.getPackageName(), R.layout.item_pet_loop);
            loop.setInt(R.id.loop_flipper, "setFlipInterval", tile.intervalMs);
            for (int frame : tile.frames) {
                loop.addView(R.id.loop_flipper, tiled(context, frame));
            }
            views.addView(R.id.bg_front, loop);
        }
    }

    /** A view covered with a repeating bitmap (its drawable has tileMode repeat). */
    private static RemoteViews tiled(Context context, int drawable) {
        RemoteViews view = new RemoteViews(context.getPackageName(), R.layout.item_pet_frame);
        view.setInt(R.id.frame_container, "setBackgroundResource", drawable);
        return view;
    }

    /** Where the picture's left edge goes on the stage before its scaling: centred. */
    static int canvasLeft(Backdrop backdrop, Stage stage) {
        return Math.round(stage.width / 2f - backdrop.width / 2f);
    }

    /** Where its top goes: its ground line lands groundMargin pixels above the bottom once scaled. */
    static int canvasTop(Backdrop backdrop, Stage stage) {
        float groundOnScreen = stage.height - backdrop.groundMargin * stage.scale;
        float groundOnStage = stage.height / 2f + (groundOnScreen - stage.height / 2f) / stage.scale;
        return Math.round(groundOnStage) - backdrop.ground;
    }

    /** A canvas x of the background for a pixel x of the widget (the inverse of the stage's scaling). */
    static float canvasX(Backdrop backdrop, Stage stage, float screenX) {
        return stage.width / 2f + (screenX - stage.width / 2f) / stage.scale - canvasLeft(backdrop, stage);
    }

    /** A canvas y of the background for a pixel y of the widget. */
    static float canvasY(Backdrop backdrop, Stage stage, float screenY) {
        return stage.height / 2f + (screenY - stage.height / 2f) / stage.scale - canvasTop(backdrop, stage);
    }

    /** A prop's frame at (x, y) on the unscaled stage, or null where it would not fit on it. */
    private static RemoteViews prop(Context context, Stage stage, int drawable, int x, int y, int width, int height) {
        int right = stage.width - x - width;
        int bottom = stage.height - y - height;
        if (x < 0 || y < 0 || right < 0 || bottom < 0) {
            return null;
        }
        RemoteViews frame = new RemoteViews(context.getPackageName(), R.layout.item_pet_frame);
        frame.setImageViewResource(R.id.frame_image, drawable);
        frame.setViewPadding(R.id.frame_container, x, y, right, bottom);
        return frame;
    }

    /** The plan's interaction with the background (not the walk or flight over to it), or null. */
    private static PetBehavior.Segment actOf(PetSpec spec, PetBehavior.Session session) {
        PetBehavior.Segment act = session.now;
        if (act.event < 0 || PetBehavior.arrives(act) || PetBehavior.flying(spec, session, act)) {
            return null;
        }
        return act;
    }

    /** A view that shows nothing: fills a flipper's turn once an effect is over. */
    private static RemoteViews empty(Context context) {
        return new RemoteViews(context.getPackageName(), R.layout.item_pet_frame);
    }

    /**
     * An outer flipper that shows {@code first} for {@code firstMs}, then {@code rest} views in
     * turn (identical ones, so the switch never brings {@code first} back before the next update).
     */
    private static RemoteViews thenHold(Context context, RemoteViews first, long firstMs, long planLeftMs,
                                        RestFactory rest) {
        if (planLeftMs > PlanFrames.TAIL_MS) {
            // A pixel pet's plan: levels of flippers hold the rest well past the plan, no deeper
            // than the launcher takes (PlanFrames.MAX_PLAN_DEPTH); an effect about to end is over
            if (firstMs < EFFECT_OVER_MS) {
                return rest.make();
            }
            RemoteViews levels = new RemoteViews(context.getPackageName(), R.layout.item_pet_loop);
            levels.setInt(R.id.loop_flipper, "setFlipInterval", (int) firstMs);
            levels.addView(R.id.loop_flipper, first);
            levels.addView(R.id.loop_flipper, rest.make());
            long covered = 2 * firstMs;
            for (int copies : PlanFrames.holdCopies(covered, planLeftMs, EFFECT_HOLD_LEVELS)) {
                RemoteViews up = new RemoteViews(context.getPackageName(), R.layout.item_pet_loop);
                up.setInt(R.id.loop_flipper, "setFlipInterval", (int) Math.min(Integer.MAX_VALUE, covered));
                up.addView(R.id.loop_flipper, levels);
                for (int c = 0; c < copies; c++) {
                    up.addView(R.id.loop_flipper, rest.make());
                }
                levels = up;
                covered *= copies + 1;
            }
            return levels;
        }
        RemoteViews outer = new RemoteViews(context.getPackageName(), R.layout.item_pet_loop);
        outer.setInt(R.id.loop_flipper, "setFlipInterval", (int) Math.max(1, firstMs));
        outer.addView(R.id.loop_flipper, first);
        long copies = Math.max(1, (planLeftMs - firstMs + firstMs - 1) / Math.max(1, firstMs));
        for (int c = 0; c < Math.min(copies, MAX_HOLD_COPIES); c++) {
            outer.addView(R.id.loop_flipper, rest.make());
        }
        return outer;
    }

    private interface RestFactory {
        RemoteViews make();
    }

    /** Most copies an effect's hold puts in its flipper (each a single view, or a short loop). */
    private static final int MAX_HOLD_COPIES = 24;
    /**
     * Levels a pixel pet's effect may add around its first part (a loop of frames, two deep, in
     * a flipper of its own): the whole stays within PlanFrames.MAX_PLAN_DEPTH.
     */
    private static final int EFFECT_HOLD_LEVELS = PlanFrames.MAX_PLAN_DEPTH - 3;
    /** An effect with less than this left is shown as over (a redraw at its very end). */
    private static final long EFFECT_OVER_MS = 100;

    /**
     * What the background does in the pet's interaction with it (BackgroundEvents): a leaf drifts
     * down to the pet, a prop shows another state, raindrops fall into its mouth. All of it starts
     * with the plan (a redraw carries on where it is) and ends in time for the next one.
     */
    private static void addEffect(Context context, RemoteViews views, PetSpec spec, PetBehavior.Session session,
                                  PetBehavior.Segment act, BackgroundEvents.Interaction interaction, long now,
                                  Backdrop backdrop, Stage stage) {
        long elapsed = Math.max(0, now - act.startedAt);
        // A pixel pet's effect holds its end until well after the plan (see PlanFrames), the
        // hand-drawn pets' for a moment
        long planLeft = PetBehavior.planEnd(session) - now
            + (spec.smooth() ? PlanFrames.TAIL_MS + 1 : PetBehavior.LATE_MARGIN_MS);
        long actLeft = act.endsAt - now;
        switch (interaction.effect) {
            case BackgroundEvents.LEAF:
                addLeaf(context, views, spec, act, interaction, elapsed, actLeft, planLeft, backdrop, stage);
                break;
            case BackgroundEvents.STATE:
                addPropState(context, views, act, interaction, elapsed, actLeft, planLeft, backdrop, stage);
                break;
            case BackgroundEvents.DROPS:
                addDrops(context, views, spec, act, interaction, elapsed, actLeft, planLeft, backdrop, stage);
                break;
            default:
                break;
        }
    }

    /** A point of the pet's 64 x 64 frame (as drawn, facing its own way) in canvas pixels. */
    private static float[] onPet(PetSpec spec, Backdrop backdrop, Stage stage, PetBehavior.Segment segment, int x,
                                 int y) {
        int box = stage.width - stage.left - stage.right;
        float pixel = box / (float) PetSpec.PIXEL_GRID;
        float frameX = x + 0.5f;
        if (stage.scaleX(spec, segment.facingRight) < 0) {
            frameX = PetSpec.PIXEL_GRID - frameX;
        }
        float screenX = stage.left + stage.offset(segment.from) + frameX * pixel;
        float screenY = stage.top + (y + 0.5f) * pixel;
        return new float[] {canvasX(backdrop, stage, screenX), canvasY(backdrop, stage, screenY)};
    }

    /**
     * A leaf comes loose from the canopy and drifts down: to the ground in front of the pet, where
     * it stays, or to a spot on the pet (its paws, its head), where it stays until the interaction
     * is over and the pet's next pose takes it.
     */
    private static void addLeaf(Context context, RemoteViews views, PetSpec spec, PetBehavior.Segment act,
                                BackgroundEvents.Interaction interaction, long elapsed, long actLeft, long planLeft,
                                Backdrop backdrop, Stage stage) {
        if (backdrop.leafTurning == null) {
            return;
        }
        int left = canvasLeft(backdrop, stage);
        int top = canvasTop(backdrop, stage);
        int side = act.facingRight ? 1 : -1;
        boolean ground = interaction.anchorX == BackgroundEvents.GROUND;
        int toX;
        int toY;
        if (ground) {
            // In front of the pet: on the side it looks to, on the ground line
            int box = stage.width - stage.left - stage.right;
            float centre = stage.left + stage.offset(act.from) + box / 2f;
            toX = Math.round(canvasX(backdrop, stage, centre + side * BackgroundEvents.LEAF_IN_FRONT * box)
                - backdrop.leafWidth / 2f);
            toY = backdrop.ground - backdrop.leafHeight + 1;
        } else {
            float[] spot = onPet(spec, backdrop, stage, act, interaction.anchorX, interaction.anchorY);
            toX = Math.round(spot[0] - backdrop.leafWidth / 2f);
            toY = Math.round(spot[1] - backdrop.leafHeight / 2f);
        }
        // It comes loose from the canopy a little behind where it lands, and sways on the way
        boolean swayRight = (act.variant & 1) == 0;
        int column = backdrop.canopyColumnNear(toX - side * (8 + 4 * (act.variant >> 1)));
        int fromX = column >= 0 ? column : toX;
        int fromY = column >= 0 ? backdrop.canopy[column] - 2 : 0;
        BackgroundEvents.LeafPose[] poses = BackgroundEvents.fall(fromX, fromY, toX, toY, 10, swayRight,
            BackgroundEvents.LEAF_FALL_FRAMES);
        int interval = BackgroundEvents.PROP_INTERVAL_MS;
        int first = (int) (elapsed / interval);
        long fallLeft = (long) (poses.length - 1 - first) * interval;
        BackgroundEvents.LeafPose end = poses[poses.length - 1];
        int restSprite = ground ? backdrop.leafLying : backdrop.leafTurning[0];
        RemoteViews rest = prop(context, stage, restSprite, left + end.x, top + end.y, backdrop.leafWidth,
            backdrop.leafHeight);
        if (rest == null) {
            return;
        }
        if (fallLeft <= 0) {
            if (ground || actLeft > 0) {
                views.addView(R.id.bg_front, ground ? rest
                    : thenHold(context, rest, actLeft, planLeft, () -> empty(context)));
            }
            return;
        }
        RemoteViews falling = new RemoteViews(context.getPackageName(), R.layout.item_pet_loop);
        falling.setInt(R.id.loop_flipper, "setFlipInterval", interval);
        for (int f = first; f < poses.length - 1; f++) {
            RemoteViews frame = prop(context, stage, backdrop.leafTurning[poses[f].sprite], left + poses[f].x,
                top + poses[f].y, backdrop.leafWidth, backdrop.leafHeight);
            falling.addView(R.id.loop_flipper, frame != null ? frame : empty(context));
        }
        if (ground) {
            views.addView(R.id.bg_front, thenHold(context, falling, fallLeft, planLeft,
                () -> prop(context, stage, backdrop.leafLying, left + end.x, top + end.y, backdrop.leafWidth,
                    backdrop.leafHeight)));
            return;
        }
        // On the pet: it stays where it landed until the interaction is over, then goes into the pose
        long holdMs = Math.max(0, actLeft - fallLeft);
        for (long h = 0; h < holdMs / interval; h++) {
            falling.addView(R.id.loop_flipper, prop(context, stage, restSprite, left + end.x, top + end.y,
                backdrop.leafWidth, backdrop.leafHeight));
        }
        views.addView(R.id.bg_front, thenHold(context, falling, fallLeft + holdMs / interval * interval, planLeft,
            () -> empty(context)));
    }

    /**
     * The prop of an interaction in its other state: for one pass of its frames (then it keeps
     * still in its last one: the heap lies scattered, the puddle calms down), while the
     * interaction lasts (then its ambient state again), or until the plan is over.
     */
    private static void addPropState(Context context, RemoteViews views, PetBehavior.Segment act,
                                     BackgroundEvents.Interaction interaction, long elapsed, long actLeft,
                                     long planLeft, Backdrop backdrop, Stage stage) {
        Backdrop.Prop prop = backdrop.prop(interaction.prop);
        if (prop == null) {
            return;
        }
        Backdrop.State state = prop.state(interaction.state);
        Backdrop.State ambient = prop.state(prop.ambient);
        int layer = prop.front ? R.id.bg_front : R.id.bg_props;
        long stateLeft;
        if (interaction.timing == BackgroundEvents.PLAN) {
            stateLeft = Long.MAX_VALUE;
        } else if (interaction.timing == BackgroundEvents.ACT) {
            stateLeft = actLeft;
        } else {
            stateLeft = state.loopMs() - elapsed;
        }
        RemoteViews ambientView = propState(context, backdrop, stage, prop, ambient, 0, false);
        if (stateLeft <= 0 && interaction.timing == BackgroundEvents.ONCE) {
            // A redraw after its one pass: still in its last frame
            ambientView = prop(context, stage, state.frames[state.frames.length - 1], propX(backdrop, stage, prop),
                canvasTop(backdrop, stage) + backdrop.ground - prop.baseline, prop.width, prop.height);
        }
        if (stateLeft <= 0 || stateLeft >= planLeft) {
            RemoteViews view = stateLeft <= 0 ? ambientView : propState(context, backdrop, stage, prop, state, elapsed,
                false);
            if (view != null) {
                views.addView(layer, view);
            }
            return;
        }
        RemoteViews now = propState(context, backdrop, stage, prop, state, elapsed, false);
        if (now == null || ambientView == null) {
            return;
        }
        if (interaction.timing == BackgroundEvents.ONCE) {
            int last = state.frames[state.frames.length - 1];
            int x = propX(backdrop, stage, prop);
            int y = canvasTop(backdrop, stage) + backdrop.ground - prop.baseline;
            views.addView(layer, thenHold(context, now, stateLeft, planLeft,
                () -> prop(context, stage, last, x, y, prop.width, prop.height)));
            return;
        }
        views.addView(layer, thenHold(context, now, stateLeft, planLeft,
            () -> propState(context, backdrop, stage, prop, ambient, 0, false)));
    }

    /**
     * Raindrops falling one after another into the pet's open mouth while the interaction lasts
     * (the pet's pose gulps each one down in step).
     */
    private static void addDrops(Context context, RemoteViews views, PetSpec spec, PetBehavior.Segment act,
                                 BackgroundEvents.Interaction interaction, long elapsed, long actLeft, long planLeft,
                                 Backdrop backdrop, Stage stage) {
        if (backdrop.drop == 0 || actLeft <= 0) {
            return;
        }
        int left = canvasLeft(backdrop, stage);
        int top = canvasTop(backdrop, stage);
        float[] mouth = onPet(spec, backdrop, stage, act, interaction.anchorX, interaction.anchorY);
        int dropX = left + Math.round(mouth[0] - backdrop.dropWidth / 2f);
        int endY = top + Math.round(mouth[1]) - backdrop.dropHeight;
        int frames = BackgroundEvents.DROP_FALL_FRAMES + BackgroundEvents.DROP_GAP_FRAMES;
        int interval = BackgroundEvents.PROP_INTERVAL_MS;
        RemoteViews loop = new RemoteViews(context.getPackageName(), R.layout.item_pet_loop);
        loop.setInt(R.id.loop_flipper, "setFlipInterval", interval);
        long base = elapsed / interval;
        for (int j = 0; j < frames; j++) {
            int f = (int) ((base + j) % frames);
            RemoteViews frame;
            if (f < BackgroundEvents.DROP_FALL_FRAMES) {
                // Faster and faster
                float t = (f + 1f) / BackgroundEvents.DROP_FALL_FRAMES;
                int y = endY - Math.round(BackgroundEvents.DROP_FALL_ROWS * (1 - t * t));
                frame = prop(context, stage, backdrop.drop, dropX, y, backdrop.dropWidth, backdrop.dropHeight);
            } else {
                frame = null;
            }
            loop.addView(R.id.loop_flipper, frame != null ? frame : empty(context));
        }
        views.addView(R.id.bg_front, thenHold(context, loop, actLeft, planLeft, () -> empty(context)));
    }

    /** The plan of an update (PlanFrames) as the launcher's views. */
    private static void addPlan(Context context, RemoteViews views, PetSpec spec, PetBehavior.Session session,
                                long now, Look look, Stage stage) {
        views.addView(R.id.pet_frames, toViews(context, PlanFrames.plan(spec, session, now, stage, look.accessory)));
    }

    private static RemoteViews toViews(Context context, PlanFrames.Node node) {
        if (node instanceof PlanFrames.Frame) {
            PlanFrames.Frame f = (PlanFrames.Frame) node;
            return frame(context, f.drawable, f.left, f.top, f.right, f.bottom, f.scaleX);
        }
        PlanFrames.Flipper flipper = (PlanFrames.Flipper) node;
        RemoteViews loop = new RemoteViews(context.getPackageName(), R.layout.item_pet_loop);
        for (PlanFrames.Node child : flipper.children) {
            loop.addView(R.id.loop_flipper, toViews(context, child));
        }
        loop.setInt(R.id.loop_flipper, "setFlipInterval", flipper.interval);
        return loop;
    }

    private static RemoteViews frame(Context context, int drawable, int left, int top, int right, int bottom,
                                     float scaleX) {
        RemoteViews frameView = new RemoteViews(context.getPackageName(), R.layout.item_pet_frame);
        frameView.setImageViewResource(R.id.frame_image, drawable);
        frameView.setViewPadding(R.id.frame_container, left, top, right, bottom);
        frameView.setFloat(R.id.frame_image, "setScaleX", scaleX);
        return frameView;
    }

    /**
     * Seasonal decorations, the night veil and the need bubble. A scene has wallpaper around it,
     * so it gets neither decorations nor the veil (it sleeps in its own night colours).
     */
    private static void renderSurroundings(Context context, RemoteViews views, int appWidgetId, PetSpec spec,
                                           Look look, boolean ownScene) {
        // An autumn background is a season of its own: no pumpkins or snow over it
        if (spec.scene || ownScene) {
            views.setViewVisibility(R.id.season_strip, View.GONE);
            views.setViewVisibility(R.id.season_corner_start, View.GONE);
            views.setViewVisibility(R.id.season_corner_end, View.GONE);
        } else if (Wardrobe.SEASON_HALLOWEEN.equals(look.season)) {
            showDecor(views, R.drawable.prop_decor_halloween_strip, R.drawable.prop_decor_pumpkin,
                R.drawable.prop_decor_pumpkin_small);
        } else if (Wardrobe.SEASON_WINTER.equals(look.season)) {
            showDecor(views, R.drawable.prop_decor_winter_strip, R.drawable.prop_decor_snowman,
                R.drawable.prop_decor_snow_pile);
        } else {
            views.setViewVisibility(R.id.season_strip, View.GONE);
            views.setViewVisibility(R.id.season_corner_start, View.GONE);
            views.setViewVisibility(R.id.season_corner_end, View.GONE);
        }

        views.setViewVisibility(R.id.night_overlay, look.night && !spec.scene ? View.VISIBLE : View.GONE);

        if (look.need == PetCare.Need.NONE) {
            views.setViewVisibility(R.id.need_bubble, View.GONE);
            return;
        }
        boolean hungry = look.need == PetCare.Need.HUNGRY;
        String name = context.getString(spec.nameRes);
        views.setViewVisibility(R.id.need_bubble, View.VISIBLE);
        views.setImageViewResource(R.id.need_bubble, hungry ? spec.foodBubbleRes : R.drawable.prop_bubble_lonely);
        views.setContentDescription(R.id.need_bubble, context.getString(
            hungry ? R.string.need_hungry_description : R.string.need_lonely_description, name));
        // The bubble opens the app on this pet, ready to feed or stroke it
        Intent open = new Intent(context, MainActivitySimple.class)
            .putExtra(MainActivitySimple.EXTRA_OPEN_PET, spec.petType)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        views.setOnClickPendingIntent(R.id.need_bubble, PendingIntent.getActivity(context,
            BUBBLE_REQUEST_BASE + appWidgetId, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
    }

    private static void showDecor(RemoteViews views, int strip, int cornerStart, int cornerEnd) {
        views.setViewVisibility(R.id.season_strip, View.VISIBLE);
        views.setImageViewResource(R.id.season_strip, strip);
        views.setViewVisibility(R.id.season_corner_start, View.VISIBLE);
        views.setImageViewResource(R.id.season_corner_start, cornerStart);
        views.setViewVisibility(R.id.season_corner_end, View.VISIBLE);
        views.setImageViewResource(R.id.season_corner_end, cornerEnd);
    }

    // Method to get current animation type for a widget
    public static int getAnimationType(Context context, int widgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(ANIMATION_TYPE + "_" + widgetId, ANIMATION_SLEEP);
    }

    /** The widget's background (bg_type): 1–3 the old pictures, Backdrop types the pixel-art ones. */
    public static int getBackgroundType(Context context, int widgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(BG_TYPE + "_" + widgetId, 1);
    }

    // Method to get pet type for a widget
    public static int getPetType(Context context, int widgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(PET_TYPE + "_" + widgetId, PET_CAT);
    }
}
