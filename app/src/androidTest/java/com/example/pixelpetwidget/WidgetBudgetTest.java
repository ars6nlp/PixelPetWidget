package com.example.pixelpetwidget;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Parcel;
import android.util.Log;
import android.util.SizeF;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RemoteViews;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Every update the widget sends, as the launcher gets it: for every pet, background and widget
 * size, along awake lives on several seeds and in every outfit, the RemoteViews are inflated the
 * way the launcher does it (so a layout it cannot show fails here) and measured: views on screen
 * and the size of the parcel with all sizes together (TransactionTooLargeException).
 */
@RunWith(AndroidJUnit4.class)
public class WidgetBudgetTest {

    private static final String TAG = "WidgetBudgetTest";
    /** Sizes in dp: the smallest (4x2), 4x4, a wide landscape one, an unfolded foldable. */
    private static final float[][] SIZES = {{222, 148}, {340, 370}, {420, 150}, {520, 430}};
    private static final int[] PETS = {PixelCatWidget.PET_CAT, PixelCatWidget.PET_DOG, PixelCatWidget.PET_RABBIT,
        PixelCatWidget.PET_PANDA, PixelCatWidget.PET_HAMSTER, PixelCatWidget.PET_CAPYBARA, PixelCatWidget.PET_PARROT,
        PixelCatWidget.PET_PUPPY, PixelCatWidget.PET_KITTEN, PixelCatWidget.PET_AQUARIUM};
    private static final String[] OUTFITS = {null, "bow", "halloween_hat", "winter_hat"};
    /** What the binder takes in one go is about 1 MB, shared with everything else in flight. */
    private static final int MAX_PARCEL_BYTES = 500 * 1024;
    /** The project's budget for the pet (PetBehavior.MAX_PLAN_VIEWS) plus the widget around it. */
    private static final int MAX_VIEWS_ON_SCREEN = 400;

    @Test
    public void everyUpdateFitsTheLauncher() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Method build = PixelCatWidget.class.getDeclaredMethod("buildViews", Context.class, int.class, int.class,
            int.class, PetBehavior.Session.class, long.class, lookClass(), float.class, float.class, boolean.class);
        build.setAccessible(true);
        Method lookFor = PixelCatWidget.class.getDeclaredMethod("lookFor", Context.class, int.class, long.class);
        lookFor.setAccessible(true);
        Field accessory = lookClass().getDeclaredField("accessory");
        accessory.setAccessible(true);
        float density = context.getResources().getDisplayMetrics().density;
        float framePaddingDp = context.getResources().getDimension(R.dimen.widget_frame_padding) / density;

        int maxViews = 0;
        int maxParcel = 0;
        String worstViews = "";
        String worstParcel = "";
        int updates = 0;
        int checked = 0;
        int failures = 0;
        String firstFailure = "";
        java.util.Set<String> failedPets = new java.util.TreeSet<>();
        java.util.Map<Integer, int[]> byDepth = new java.util.TreeMap<>();
        int[][] bySlot = new int[6][2];
        for (int pet : PETS) {
            PetSpec spec = PetSpec.forType(pet);
            for (int bg = 1; bg <= 6; bg++) {
                Backdrop backdrop = spec.scene ? null : Backdrop.forType(bg);
                BackgroundEvents.Interaction interaction = spec.scene ? null : BackgroundEvents.find(pet, bg);
                float ground = Float.MAX_VALUE;
                for (float[] size : SIZES) {
                    if (!spec.scene) {
                        ground = Math.min(ground, PlanFrames.Stage.forPet(spec, size[0], size[1], density,
                            framePaddingDp, backdrop).roomPx());
                    }
                }
                for (long seed = 1; seed <= 3; seed++) {
                    PetBehavior.Session s = new PetBehavior.Session();
                    s.groundPx = spec.smooth() && !spec.scene ? Math.max(1f, ground) : 0f;
                    long now = 1_700_000_000_000L;
                    PetBehavior.tap(spec, interaction, s, now, seed * 7919 + pet * 31 + bg);
                    for (int turn = 0; turn < 60; turn++) {
                        Object look = lookFor.invoke(null, context, pet, now);
                        accessory.set(look, OUTFITS[(int) ((seed + turn) % OUTFITS.length)]);
                        Map<SizeF, RemoteViews> all = new LinkedHashMap<>();
                        for (float[] size : SIZES) {
                            RemoteViews views = (RemoteViews) build.invoke(null, context, 1000 + pet, pet, bg, s, now,
                                look, size[0], size[1], false);
                            all.put(new SizeF(size[0], size[1]), views);
                            int count = inflate(context, views, size, density);
                            String where = name(pet) + " bg" + bg + " " + (int) size[0] + "x" + (int) size[1]
                                + " seed " + seed + " turn " + turn;
                            if (count > maxViews) {
                                maxViews = count;
                                worstViews = where;
                            }
                        }
                        int parcel = parcelSize(new RemoteViews(all));
                        if (parcel > maxParcel) {
                            maxParcel = parcel;
                            worstParcel = name(pet) + " bg" + bg + " seed " + seed + " turn " + turn;
                        }
                        // The same plan redrawn later on (a late alarm, a reboot, a resize): the
                        // system reads the parcel back and refuses views nested too deep
                        long next = s.phase == PetBehavior.ASLEEP ? now : PetBehavior.nextTurn(s);
                        for (long at : new long[] {now, (now + next) / 2, next - 100, next + 2_000, next + 60_000,
                            next + 600_000}) {
                            Map<SizeF, RemoteViews> later = new LinkedHashMap<>();
                            for (float[] size : SIZES) {
                                later.put(new SizeF(size[0], size[1]), (RemoteViews) build.invoke(null, context,
                                    1000 + pet, pet, bg, s, at, look, size[0], size[1], false));
                            }
                            checked++;
                            String problem = readBack(new RemoteViews(later));
                            int depth = s.phase == PetBehavior.ASLEEP ? 0 : depth(PlanFrames.plan(spec, s, at,
                                PlanFrames.Stage.forPet(spec, SIZES[0][0], SIZES[0][1], density, framePaddingDp,
                                    backdrop == null ? null : backdrop).withGround(s.groundPx), null));
                            int slot = slotOf(at, now, next);
                            int[] row = byDepth.computeIfAbsent(depth, k -> new int[2]);
                            row[problem == null ? 0 : 1]++;
                            bySlot[slot][problem == null ? 0 : 1]++;
                            if (problem != null) {
                                failures++;
                                if (firstFailure.isEmpty()) {
                                    firstFailure = name(pet) + " bg" + bg + " seed " + seed + " turn " + turn + " at +"
                                        + (at - now) + " ms: " + problem;
                                }
                                failedPets.add(name(pet));
                            }
                        }
                        updates++;
                        if (s.phase == PetBehavior.ASLEEP) {
                            break;
                        }
                        now = PetBehavior.nextTurn(s);
                        PetBehavior.advance(spec, interaction, s, now);
                    }
                }
            }
        }
        String report = updates + " updates; most views on screen " + maxViews + " (" + worstViews
            + "); largest parcel of all sizes " + maxParcel + " bytes (" + worstParcel + "); read back by the system: "
            + failures + " of " + checked + " refused " + failedPets + " " + firstFailure;
        StringBuilder detail = new StringBuilder("plan depth: ok/refused");
        for (Map.Entry<Integer, int[]> e : byDepth.entrySet()) {
            detail.append(' ').append(e.getKey()).append('=').append(e.getValue()[0]).append('/').append(e.getValue()[1]);
        }
        detail.append("; by moment (start, middle, end-0.1s, +2s, +1min, +10min) ok/refused:");
        for (int[] slot : bySlot) {
            detail.append(' ').append(slot[0]).append('/').append(slot[1]);
        }
        Log.i(TAG, detail.toString());
        Log.i(TAG, report);
        System.out.println(TAG + ": " + report);
        assertTrue(report, maxViews <= MAX_VIEWS_ON_SCREEN);
        assertTrue(report, maxParcel <= MAX_PARCEL_BYTES);
        assertTrue(report, failures == 0);
    }

    private static int slotOf(long at, long now, long next) {
        if (at == now) return 0;
        if (at == (now + next) / 2) return 1;
        if (at == next - 100) return 2;
        if (at == next + 2_000) return 3;
        if (at == next + 60_000) return 4;
        return 5;
    }

    /** Flippers nested inside one another in the pet's plan (each is one more RemoteViews level). */
    private static int depth(PlanFrames.Node node) {
        if (!(node instanceof PlanFrames.Flipper)) {
            return 1;
        }
        int deepest = 0;
        for (PlanFrames.Node child : ((PlanFrames.Flipper) node).children) {
            deepest = Math.max(deepest, depth(child));
        }
        return 1 + deepest;
    }

    private static String name(int pet) {
        String name = PetSpec.forType(pet).generatedName;
        return name != null ? name : new String[] {"cat", "dog", "rabbit"}[pet];
    }

    private static Class<?> lookClass() throws ClassNotFoundException {
        return Class.forName("com.example.pixelpetwidget.PixelCatWidget$Look");
    }

    /** Inflates the views the way a launcher does and counts them. */
    private static int inflate(Context context, RemoteViews views, float[] size, float density) {
        final int[] count = new int[1];
        final RuntimeException[] failure = new RuntimeException[1];
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            try {
                FrameLayout parent = new FrameLayout(context);
                parent.setLayoutParams(new ViewGroup.LayoutParams((int) (size[0] * density), (int) (size[1] * density)));
                View view = views.apply(context, parent);
                count[0] = countViews(view);
            } catch (RuntimeException e) {
                failure[0] = e;
            }
        });
        if (failure[0] != null) {
            throw failure[0];
        }
        return count[0];
    }

    private static int countViews(View view) {
        int count = 1;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                count += countViews(group.getChildAt(i));
            }
        }
        return count;
    }

    /** Writes the views and reads them back as the system does; the reason it refuses them, or null. */
    private static String readBack(RemoteViews views) {
        Parcel parcel = Parcel.obtain();
        try {
            views.writeToParcel(parcel, 0);
            parcel.setDataPosition(0);
            new RemoteViews(parcel);
            return null;
        } catch (RuntimeException e) {
            return e.getMessage();
        } finally {
            parcel.recycle();
        }
    }

    private static int parcelSize(RemoteViews views) {
        Parcel parcel = Parcel.obtain();
        try {
            views.writeToParcel(parcel, 0);
            return parcel.dataSize();
        } finally {
            parcel.recycle();
        }
    }
}
