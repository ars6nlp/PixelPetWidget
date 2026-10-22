package com.example.pixelpetwidget;

import static com.example.pixelpetwidget.PixelCatWidget.*;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The cat, the pug and the rabbit must live and look exactly as they did before the pixel pets got
 * their smooth motion: the same frames, intervals, speed and walks, the same plans and the same
 * views for the launcher. legacy_motion_golden.txt holds a hash per life (25 seeds, 4 backgrounds,
 * 3 widget sizes, alarms on time and 3 s late, a tap in the middle), taken from the code as it was.
 * Frames are named by their drawables (resource ids change whenever drawables are added).
 */
public class LegacyPetsRegressionTest {

    private static final long T0 = 1_700_000_000_000L;
    private static final int[] LEGACY = {PET_CAT, PET_DOG, PET_RABBIT};
    private static final Map<Integer, String> DRAWABLES = new HashMap<>();

    static {
        for (Field field : R.drawable.class.getFields()) {
            try {
                DRAWABLES.put(field.getInt(null), field.getName());
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }
    }

    private static String drawable(int id) {
        String name = DRAWABLES.get(id);
        return name != null ? name : Integer.toString(id);
    }

    @Test
    public void theirFramesIntervalsAndSpeedAreAsBefore() {
        PetSpec cat = PetSpec.forType(PET_CAT);
        assertArrayEquals(SLEEP_FRAMES, cat.animation(ANIMATION_SLEEP).frames);
        assertArrayEquals(LICK_FRAMES, cat.animation(ANIMATION_LICK).frames);
        assertArrayEquals(CAT_SEAT_FRAMES, cat.animation(ANIMATION_SEAT).frames);
        assertArrayEquals(WALK_FRAMES, cat.animation(ANIMATION_WALK).frames);
        PetSpec dog = PetSpec.forType(PET_DOG);
        assertArrayEquals(DOG_SLEEP_FRAMES, dog.animation(ANIMATION_SLEEP).frames);
        assertArrayEquals(DOG_EAT_FRAMES, dog.animation(ANIMATION_EAT).frames);
        assertArrayEquals(DOG_SIT_FRAMES, dog.animation(ANIMATION_LICK).frames);
        assertArrayEquals(DOG_WALK_FRAMES, dog.animation(ANIMATION_WALK).frames);
        PetSpec rabbit = PetSpec.forType(PET_RABBIT);
        assertArrayEquals(RABBIT_SLEEP_FRAMES, rabbit.animation(ANIMATION_SLEEP).frames);
        assertArrayEquals(RABBIT_EAT_FRAMES, rabbit.animation(ANIMATION_EAT).frames);
        assertArrayEquals(RABBIT_SIT_FRAMES, rabbit.animation(ANIMATION_SEAT).frames);
        assertArrayEquals(RABBIT_WALK_FRAMES, rabbit.animation(ANIMATION_WALK).frames);
        int[] counts = {16, 14, 14, 16, 16, 16, 16, 16, 16, 16, 16, 16};
        int i = 0;
        for (PetSpec spec : new PetSpec[] {cat, dog, rabbit}) {
            assertFalse(spec.smooth());
            assertNull(spec.gait);
            assertEquals(16, spec.maxLoopFrames());
            assertEquals(130, spec.walkStepMs());
            assertEquals(2 * 0.9f / 7, PetBehavior.WALK_STEP, 0f);
            assertEquals(PetBehavior.WALK_STEP * 1000f / 130, spec.walkSpeed(), 0f);
            for (int animation : spec.animationTypes) {
                PetSpec.Anim anim = spec.animation(animation);
                assertEquals(animation == ANIMATION_WALK ? 130 : 100, anim.intervalMs);
                assertEquals(counts[i++], anim.frames.length);
            }
        }
        assertEquals(1_500, PetBehavior.LATE_MARGIN_MS);
        assertEquals(48, PetBehavior.MAX_PAIR_FRAMES);
        assertEquals(16, MAX_LOOP_FRAMES);
    }

    @Test
    public void theirLivesAndFramesAreAsBefore() throws Exception {
        List<String> golden = new ArrayList<>();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("legacy_motion_golden.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                if (!line.startsWith("#")) {
                    golden.add(line);
                }
            }
        }
        assertEquals(1800, golden.size());
        for (String line : golden) {
            String[] f = line.split(" ");
            int pet = Integer.parseInt(f[0]);
            int bg = Integer.parseInt(f[1]);
            float[] size = {Float.parseFloat(f[2]), Float.parseFloat(f[3])};
            long seed = Long.parseLong(f[4]);
            long late = Long.parseLong(f[5]);
            assertEquals(line, f[6], life(pet, bg, size, seed * 7919, late));
        }
        for (int pet : LEGACY) {
            assertFalse(PetSpec.forType(pet).smooth());
        }
    }

    // The same lives as when the golden file was taken, drawn by PlanFrames

    private static void tree(StringBuilder out, PlanFrames.Node node) {
        if (node instanceof PlanFrames.Frame) {
            PlanFrames.Frame frame = (PlanFrames.Frame) node;
            out.append("f").append(drawable(frame.drawable)).append(',').append(frame.left).append(',').append(frame.top)
                .append(',').append(frame.right).append(',').append(frame.bottom).append(',').append(frame.scaleX)
                .append(';');
            return;
        }
        PlanFrames.Flipper flipper = (PlanFrames.Flipper) node;
        out.append("F").append(flipper.interval).append('[');
        for (PlanFrames.Node child : flipper.children) {
            tree(out, child);
        }
        out.append(']');
    }

    private static void describe(StringBuilder out, PetBehavior.Session s) {
        out.append("p").append(s.phase).append(" u").append(s.awakeUntil - T0).append(' ');
        for (PetBehavior.Segment g : new PetBehavior.Segment[] {s.now, s.next}) {
            if (g == null) {
                continue;
            }
            out.append(g.animation).append('@').append(g.startedAt - T0).append('-').append(g.endsAt - T0).append(' ')
                .append(g.from).append('>').append(g.to).append(' ').append(g.facingRight).append(' ')
                .append(g.variant).append(' ').append(g.event).append('|');
        }
    }

    private static void render(StringBuilder out, PetSpec spec, PetBehavior.Session s, long now,
                               PlanFrames.Stage stage) {
        if (s.phase == PetBehavior.ASLEEP) {
            int offset = stage.offset(s.now.from);
            out.append("asleep ").append(stage.left + offset).append(',').append(stage.right - offset).append(',')
                .append(drawable(spec.animation(ANIMATION_SLEEP).frames[0])).append(',')
                .append(stage.scaleX(spec, s.now.facingRight));
        } else {
            tree(out, PlanFrames.plan(spec, s, now, stage, null));
        }
        out.append('\n');
    }

    private static String life(int pet, int bg, float[] size, long seed, long late) throws Exception {
        PetSpec spec = PetSpec.forType(pet);
        PlanFrames.Stage stage = PlanFrames.Stage.forPet(spec, size[0], size[1], 3f, 0f, Backdrop.forType(bg));
        BackgroundEvents.Interaction interaction = BackgroundEvents.find(pet, bg);
        StringBuilder out = new StringBuilder();
        PetBehavior.Session s = new PetBehavior.Session();
        PetBehavior.tap(spec, interaction, s, T0, seed);
        describe(out, s);
        render(out, spec, s, T0, stage);
        long tapAt = T0 + 20_000 + (seed * 37) % 5000;
        boolean tapped = false;
        for (int guard = 0; guard < 300 && s.phase != PetBehavior.ASLEEP; guard++) {
            long at = PetBehavior.nextTurn(s) + late;
            if (!tapped && tapAt < at) {
                tapped = true;
                PetBehavior.tap(spec, interaction, s, tapAt, seed + 1);
                describe(out, s);
                render(out, spec, s, tapAt, stage);
                continue;
            }
            PetBehavior.advance(spec, interaction, s, at);
            describe(out, s);
            render(out, spec, s, at, stage);
        }
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(out.toString().getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            hex.append(String.format("%02x", hash[i]));
        }
        return hex.toString();
    }
}
