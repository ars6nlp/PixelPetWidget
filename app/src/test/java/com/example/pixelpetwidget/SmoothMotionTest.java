package com.example.pixelpetwidget;

import static com.example.pixelpetwidget.PixelCatWidget.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The pixel pets move smoothly: plays their lives with many seeds on every background and widget
 * size, with alarms on time and late, and watches what the launcher shows every 10 ms (a model of
 * its ViewFlippers: all start when the update is applied, each child shows for the flipper's
 * interval in turn). Between two frames on screen the pet never moves more than
 * PetSpec.MAX_SHIFT_PX art pixels on the ground (MAX_FLIGHT_SHIFT_PX in the air), whether inside
 * a plan, from one plan to the next, after a late alarm or a redraw.
 *
 * <p>Writes build/smooth_motion_log.txt: the largest jumps, speeds and views per pet, background,
 * size and lateness, and a frame-by-frame log of one life per pet.
 */
public class SmoothMotionTest {

    private static final long T0 = 1_700_000_000_000L;
    private static final int[] PIXEL_PETS = {PET_PANDA, PET_HAMSTER, PET_CAPYBARA, PET_PARROT, PET_PUPPY, PET_KITTEN,
        PET_AQUARIUM};
    private static final int[] BACKGROUNDS = {1, Backdrop.MAPLE_PARK, Backdrop.RAINY_EVENING, Backdrop.PUMPKIN_FIELD};
    /** Widget sizes in dp: 4x4, 4x2, the smallest; on the emulator's density. */
    private static final float[][] SIZES = {{340, 370}, {340, 180}, {222, 148}};
    private static final float DENSITY = 3f;
    private static final long[] LATE = {0, 1_000, 3_000, 8_000, 20_000};

    // --- A model of the launcher -------------------------------------------------------------

    /** The frame a tree shows {@code elapsed} ms after the update was applied. */
    static PlanFrames.Frame shown(PlanFrames.Node node, long elapsed) {
        while (node instanceof PlanFrames.Flipper) {
            PlanFrames.Flipper flipper = (PlanFrames.Flipper) node;
            node = flipper.children.get((int) ((elapsed / flipper.interval) % flipper.children.size()));
        }
        return (PlanFrames.Frame) node;
    }

    static PlanFrames.Stage stage(PetSpec spec, float[] size, int bg) {
        if (spec.scene) {
            return PlanFrames.Stage.forScene(spec, size[0], size[1], DENSITY, 0);
        }
        return PlanFrames.Stage.forPet(spec, size[0], size[1], DENSITY, 0f, Backdrop.forType(bg));
    }

    /** What one life looked like on screen. */
    static final class Watch {
        final PetSpec spec;
        final PlanFrames.Stage stage;
        double maxGround;
        double maxAir;
        double maxUpdate;
        double maxRedraw;
        int maxViews;
        int maxFrames;
        int instantTurns;
        long walkPx;
        long walkMs;
        final List<String> log = new ArrayList<>();
        boolean logging;

        Watch(PetSpec spec, PlanFrames.Stage stage) {
            this.spec = spec;
            this.stage = stage;
        }

        boolean inAir(PlanFrames.Frame f) {
            return f.top != stage.top;
        }

        /** Art pixels between two frames on screen, sideways and up and down. */
        double[] jump(PlanFrames.Frame a, PlanFrames.Frame b) {
            int pixel = Math.max(1, stage.pixel);
            return new double[] {Math.abs(a.left - b.left) / (double) pixel, Math.abs(a.top - b.top) / (double) pixel};
        }
    }

    static Watch live(int pet, int bg, float[] size, long seed, long late, boolean logging, boolean redraws) {
        PetSpec spec = PetSpec.forType(pet);
        PlanFrames.Stage stage = stage(spec, size, bg);
        BackgroundEvents.Interaction interaction = spec.scene ? null : BackgroundEvents.find(pet, bg);
        Watch w = new Watch(spec, stage);
        w.logging = logging;
        PetBehavior.Session s = new PetBehavior.Session();
        s.groundPx = stage.roomPx();
        PetBehavior.tap(spec, interaction, s, T0, seed);
        long applied = T0;
        PlanFrames.Node tree = build(w, s, applied);
        PlanFrames.Frame last = shown(tree, 0);
        long lastChange = T0;
        long redrawAt = T0 + 7_000 + seed * 131 % 9_000;
        for (int guard = 0; guard < 400 && s.phase != PetBehavior.ASLEEP; guard++) {
            long alarm = PetBehavior.nextTurn(s) + late;
            for (long t = applied + 10; t < alarm; t += 10) {
                if (redraws && t >= redrawAt && redrawAt > applied) {
                    // A redraw (resize, feeding in the app) carries on where the pet is
                    PlanFrames.Node again = build(w, s, t);
                    double[] d = w.jump(shown(tree, t - applied), shown(again, 0));
                    w.maxRedraw = Math.max(w.maxRedraw, Math.max(d[0], d[1]));
                    tree = again;
                    applied = t;
                    redrawAt = Long.MAX_VALUE;
                }
                PlanFrames.Frame now = shown(tree, t - applied);
                if (now != last) {
                    step(w, last, now, t, false, lastChange);
                    last = now;
                    lastChange = t;
                }
            }
            if (!PetBehavior.advance(spec, interaction, s, alarm)) {
                break;
            }
            applied = alarm;
            if (s.phase == PetBehavior.ASLEEP) {
                // The still picture where it fell asleep
                int offset = stage.offset(s.now.from);
                PlanFrames.Frame still = stage.at(spec.animation(ANIMATION_SLEEP).frames[0], offset,
                    stage.scaleX(spec, s.now.facingRight));
                step(w, last, still, alarm, true, lastChange);
                break;
            }
            tree = build(w, s, alarm);
            PlanFrames.Frame now = shown(tree, 0);
            step(w, last, now, alarm, true, lastChange);
            last = now;
            lastChange = alarm;
        }
        return w;
    }

    static PlanFrames.Node build(Watch w, PetBehavior.Session s, long now) {
        PlanFrames.Node tree = PlanFrames.plan(w.spec, s, now, w.stage.withGround(s.groundPx), null);
        w.maxViews = Math.max(w.maxViews, tree.views());
        w.maxFrames = Math.max(w.maxFrames, tree.frames());
        return tree;
    }

    static void step(Watch w, PlanFrames.Frame from, PlanFrames.Frame to, long t, boolean update, long since) {
        double[] d = w.jump(from, to);
        boolean air = w.inAir(from) || w.inAir(to);
        if (air) {
            // Diagonally
            w.maxAir = Math.max(w.maxAir, Math.hypot(d[0], d[1]));
        } else {
            w.maxGround = Math.max(w.maxGround, d[0]);
            if (update) {
                w.maxUpdate = Math.max(w.maxUpdate, d[0]);
            }
            if (d[0] > 0 && d[0] <= PetSpec.MAX_SHIFT_PX) {
                w.walkPx += Math.round(d[0]);
                w.walkMs += t - since;
            }
        }
        if (from.scaleX != to.scaleX && d[0] == 0 && !air) {
            w.instantTurns++;
        }
        assertTrue("left edge " + to.left, to.left >= 0 && to.right >= 0);
        // A pose drawn moved (a jump) stays inside the widget too
        assertTrue("top edge " + to.top, to.top >= 0 && to.bottom >= 0);
        if (w.logging) {
            w.log.add(String.format(Locale.ROOT, "%7d %s x=%6.1f y=%5.1f %s d=%4.1f%s", t - T0, update ? "UPDATE" : "      ",
                to.left / (double) Math.max(1, w.stage.pixel), to.top / (double) Math.max(1, w.stage.pixel),
                to.scaleX < 0 ? "M" : " ", Math.max(d[0], d[1]), d[0] > PetSpec.MAX_SHIFT_PX && !air ? "  JUMP" : ""));
        }
    }

    // --- The invariants ----------------------------------------------------------------------

    @Test
    public void noJumpOnTheGroundOrInTheAirWhateverTheAlarmsDo() throws Exception {
        try (PrintWriter out = new PrintWriter("build/smooth_motion_log.txt", "UTF-8")) {
            out.println("Smooth motion of the pixel pets: 15 seeds each, a redraw in the middle of every life.");
            out.println("Limits: ground " + PetSpec.MAX_SHIFT_PX + " art px per frame, air "
                + PetSpec.MAX_FLIGHT_SHIFT_PX + ", views per update " + PetBehavior.MAX_PLAN_VIEWS + ".");
            out.println("Columns: largest move between two frames on screen in art px (on the ground, in the air, at an"
                + " update, at a redraw), most views and pet frames in one update, mirrorings without a turn (no"
                + " turn frames drawn yet), walking speed (PetSpec) and seconds to cross the ground at this size.");
            out.println("pet       bg size     late  | ground air update redraw | views frames | turns | walk px/s cross s");
            for (int pet : PIXEL_PETS) {
                for (int bg : BACKGROUNDS) {
                    for (float[] size : SIZES) {
                        for (long late : LATE) {
                            Watch all = null;
                            double ground = 0;
                            double air = 0;
                            double update = 0;
                            double redraw = 0;
                            int views = 0;
                            int frames = 0;
                            int turns = 0;
                            long px = 0;
                            long ms = 0;
                            for (long seed = 1; seed <= 15; seed++) {
                                Watch w = live(pet, bg, size, seed * 7919, late, false, true);
                                all = w;
                                ground = Math.max(ground, w.maxGround);
                                air = Math.max(air, w.maxAir);
                                update = Math.max(update, w.maxUpdate);
                                redraw = Math.max(redraw, w.maxRedraw);
                                views = Math.max(views, w.maxViews);
                                frames = Math.max(frames, w.maxFrames);
                                turns += w.instantTurns;
                                px += w.walkPx;
                                ms += w.walkMs;
                            }
                            double speed = PetSpec.forType(pet).gait != null ? PetSpec.forType(pet).walkSpeed() : 0;
                            double range = 2 * PetBehavior.GROUND_LIMIT * all.stage.ground;
                            out.println(String.format(Locale.ROOT,
                                "%-9s bg%d %3.0fx%3.0f %5d | %5.1f %5.1f %5.1f %5.1f | %4d %4d | %3d | %6.1f %6.1f",
                                name(pet), bg, size[0], size[1], late, ground, air, update, redraw, views, frames, turns,
                                speed, speed > 0 ? range / speed : 0));
                            String where = name(pet) + " bg" + bg + " " + size[0] + "x" + size[1] + " late " + late;
                            assertTrue(where + ": " + ground, ground <= PetSpec.MAX_SHIFT_PX);
                            // A jump at most MAX_FLIGHT_SHIFT_PX, diagonally; a flight leaves room
                            // for the wingbeat's bob inside its frames
                            assertTrue(where + ": " + air, air <= PetSpec.MAX_FLIGHT_SHIFT_PX
                                - (PetSpec.forType(pet).flies() ? PetSpec.FLIGHT_BOB_PX : 0));
                            assertTrue(where + ": " + redraw, redraw <= PetSpec.MAX_SHIFT_PX);
                            assertTrue(where + ": " + views, views <= PetBehavior.MAX_PLAN_VIEWS);
                            assertTrue(where + ": " + frames, frames <= PetBehavior.MAX_PLAN_FRAMES);
                        }
                    }
                }
            }
            out.println();
            for (int pet : PIXEL_PETS) {
                Watch w = live(pet, Backdrop.MAPLE_PARK, SIZES[0], 7919 * 3, 1_000, true, false);
                out.println("==== " + name(pet) + ", maple park, 4x4, alarms 1 s late (time ms, position in art px)");
                for (String line : w.log) {
                    out.println(line);
                }
            }
        }
    }

    /**
     * One life of each pixel pet on a 4x4 widget as the launcher shows it, for
     * tools/sprite_gen/life_gif.py: lines of "ms drawable left top mirrored" (art pixels from the
     * stage's corner) in build/lives/<pet>.txt, every time the picture changes.
     */
    @Test
    public void writesALifeOfEveryPetForThePreviews() throws Exception {
        java.util.Map<Integer, String> names = new java.util.HashMap<>();
        for (java.lang.reflect.Field field : R.drawable.class.getFields()) {
            names.put(field.getInt(null), field.getName());
        }
        new java.io.File("build/lives/more").mkdirs();
        for (int pet : new int[] {PET_PANDA, PET_HAMSTER, PET_CAPYBARA, PET_PARROT, PET_PUPPY, PET_KITTEN,
            PET_AQUARIUM}) {
            PetSpec spec = PetSpec.forType(pet);
            PlanFrames.Stage stage = stage(spec, SIZES[0], Backdrop.MAPLE_PARK);
            // The seed whose life has the most walks and turns among the first ones
            writeLife(names, "build/lives/" + name(pet) + ".txt", spec, stage, bestSeed(spec, stage));
            // More lives at every size for tools/sprite_gen/screen_check.py
            for (int size = 0; size < SIZES.length; size++) {
                PlanFrames.Stage at = stage(spec, SIZES[size], Backdrop.MAPLE_PARK);
                for (long seed = 1; seed <= 8; seed++) {
                    writeLife(names, "build/lives/more/" + name(pet) + "_" + size + "_" + seed + ".txt", spec, at,
                        seed * 7919);
                }
            }
        }
    }

    private static void writeLife(java.util.Map<Integer, String> names, String path, PetSpec spec,
                                  PlanFrames.Stage stage, long seed) throws Exception {
        try (PrintWriter out = new PrintWriter(path, "UTF-8")) {
            out.println("# pixel " + stage.pixel + " ground " + stage.roomPx() + " left " + stage.left + " top "
                + stage.top);
            PetBehavior.Session s = new PetBehavior.Session();
            s.groundPx = stage.roomPx();
            PetBehavior.tap(spec, s, T0, seed);
            long applied = T0;
            PlanFrames.Node tree = PlanFrames.plan(spec, s, T0, stage.withGround(s.groundPx), null);
            PlanFrames.Frame last = null;
            for (int guard = 0; guard < 400; guard++) {
                long next = s.phase == PetBehavior.ASLEEP ? applied + 2_000 : PetBehavior.nextTurn(s);
                for (long t = applied; t < next; t += 10) {
                    PlanFrames.Frame f = shown(tree, t - applied);
                    if (f != last) {
                        out.println((t - T0) + " " + names.get(f.drawable) + " " + (f.left / (double) stage.pixel)
                            + " " + (f.top / (double) stage.pixel) + " " + (f.scaleX < 0 ? 1 : 0));
                        last = f;
                    }
                }
                if (s.phase == PetBehavior.ASLEEP) {
                    out.println((next - T0) + " end");
                    break;
                }
                PetBehavior.advance(spec, s, next);
                applied = next;
                tree = s.phase == PetBehavior.ASLEEP
                    ? stage.at(spec.animation(ANIMATION_SLEEP).frames[0], stage.offset(s.now.from),
                        stage.scaleX(spec, s.now.facingRight))
                    : PlanFrames.plan(spec, s, next, stage, null);
            }
        }
    }

    private static long bestSeed(PetSpec spec, PlanFrames.Stage stage) {
        long best = 1;
        int most = -1;
        for (long seed = 1; seed <= 40; seed++) {
            PetBehavior.Session s = new PetBehavior.Session();
            s.groundPx = stage.roomPx();
            PetBehavior.tap(spec, s, T0, seed);
            int score = 0;
            while (s.phase == PetBehavior.AWAKE) {
                for (PetBehavior.Segment g : new PetBehavior.Segment[] {s.now, s.next}) {
                    if (g != null && (g.animation == ANIMATION_WALK || g.animation == ANIMATION_FLY)) {
                        score += g.turns ? 3 : 1;
                    }
                }
                PetBehavior.advance(spec, s, PetBehavior.planEnd(s));
            }
            if (score > most) {
                most = score;
                best = seed;
            }
        }
        return best;
    }

    private static String name(int pet) {
        return PetSpec.forType(pet).generatedName;
    }

    @Test
    public void aWalkMovesWithItsPawsAtItsGaitsSpeed() {
        for (int pet : new int[] {PET_PANDA, PET_HAMSTER, PET_CAPYBARA, PET_PARROT, PET_PUPPY, PET_KITTEN}) {
            PetSpec spec = PetSpec.forType(pet);
            int walks = 0;
            for (long seed = 0; seed < 200; seed++) {
                PetBehavior.Session s = new PetBehavior.Session();
                s.groundPx = 100;
                PetBehavior.tap(spec, s, T0, seed);
                for (int i = 0; i < 30 && s.phase == PetBehavior.AWAKE; i++) {
                    for (PetBehavior.Segment segment : new PetBehavior.Segment[] {s.now, s.next}) {
                        if (segment == null || segment.animation != ANIMATION_WALK) {
                            continue;
                        }
                        walks++;
                        // One way, whole art pixels, each frame as far as the paw on the ground moves
                        assertEquals(segment.to > segment.from, segment.facingRight);
                        List<PetBehavior.Entry> timeline = PetBehavior.timeline(spec, segment);
                        int px = 0;
                        int k = 0;
                        for (PetBehavior.Entry entry : timeline) {
                            if (entry.animation == ANIMATION_WALK) {
                                assertEquals(Math.min(px + spec.gait.shifts[k % 8], PetBehavior.distancePx(segment)),
                                    entry.px);
                                assertEquals(k % 8, entry.frame);
                                px = entry.px;
                                k++;
                            }
                        }
                        assertEquals(PetBehavior.distancePx(segment), px);
                        if (segment.event < 0) {
                            long cycles = PetBehavior.repeatsOf(spec, segment);
                            assertTrue(cycles <= spec.maxRepeats);
                        }
                        assertTrue(Math.abs(segment.to) <= PetBehavior.GROUND_LIMIT + 1e-5f);
                    }
                    PetBehavior.advance(spec, s, PetBehavior.planEnd(s));
                }
            }
            assertTrue(walks > 50);
        }
    }

    @Test
    public void theSecondLoopStartsInStepWithItsFrames() {
        PetSpec spec = PetSpec.forType(PET_KITTEN);
        PlanFrames.Stage stage = stage(spec, SIZES[0], 1);
        int pairs = 0;
        for (long seed = 0; seed < 200; seed++) {
            PetBehavior.Session s = new PetBehavior.Session();
            s.groundPx = stage.roomPx();
            PetBehavior.tap(spec, s, T0, seed);
            for (int i = 0; i < 20 && s.phase == PetBehavior.AWAKE; i++) {
                long now = s.now.startedAt + 37;
                if (s.next != null && s.now.endsAt - now > 400) {
                    pairs++;
                    PlanFrames.Node tree = PlanFrames.plan(spec, s, now, stage.withGround(s.groundPx), null);
                    int interval = PetBehavior.tickMs(spec, s.next);
                    long left = s.now.endsAt - now;
                    // Before a loop's end, after a timeline's (it shows all its steps)
                    long switchAt = left - Math.floorMod(left, (long) interval);
                    if (PetBehavior.linear(spec, s.now) && switchAt < left) {
                        switchAt += interval;
                    }
                    int[] frames = spec.animation(s.next.animation).frames;
                    // The switch shows the loop's first frame for a whole interval, then the second
                    assertEquals(frames[0], shown(tree, switchAt).drawable);
                    assertEquals(frames[0], shown(tree, switchAt + interval - 1).drawable);
                    assertEquals(frames[1 % frames.length], shown(tree, switchAt + interval).drawable);
                }
                PetBehavior.advance(spec, s, PetBehavior.planEnd(s));
            }
        }
        assertTrue(pairs > 50);
    }

    @Test
    public void lateAlarmsNeverBringBackTheStartOfAPlan() {
        // Long after its plan, the launcher still shows the pet where the plan left it
        for (int pet : new int[] {PET_HAMSTER, PET_PUPPY, PET_PARROT, PET_KITTEN}) {
            PetSpec spec = PetSpec.forType(pet);
            PlanFrames.Stage stage = stage(spec, SIZES[0], Backdrop.PUMPKIN_FIELD);
            BackgroundEvents.Interaction interaction = BackgroundEvents.find(pet, Backdrop.PUMPKIN_FIELD);
            for (long seed = 0; seed < 60; seed++) {
                PetBehavior.Session s = new PetBehavior.Session();
                s.groundPx = stage.roomPx();
                PetBehavior.tap(spec, interaction, s, T0, seed);
                for (int i = 0; i < 25 && s.phase == PetBehavior.AWAKE; i++) {
                    long start = s.now.startedAt;
                    PlanFrames.Node tree = PlanFrames.plan(spec, s, start, stage.withGround(s.groundPx), null);
                    long end = PetBehavior.planEnd(s) - start;
                    PlanFrames.Frame atEnd = shown(tree, end + 5);
                    for (long late = end; late < end + PlanFrames.TAIL_MS; late += 250) {
                        assertEquals("seed " + seed, atEnd.left, shown(tree, late).left);
                    }
                    PetBehavior.advance(spec, interaction, s, PetBehavior.planEnd(s));
                }
            }
        }
    }

    @Test
    public void theTransitionsPlayWhereTheyBelong() {
        // A made-up pixel pet with every transition (drawn with some of the kitten's frames)
        int[] three = GeneratedPetFrames.frames("kitten", "idle");
        PetSpec spec = PetSpec.pixelPet(99, "kitten", "yarn", 0.22f, 0, 0, 0)
            .gait(PetSpec.KITTEN_GAIT)
            .anim(ANIMATION_TURN, new int[] {three[0], three[1]}, 110)
            .anim(ANIMATION_WALK_START, new int[] {three[2]}, 110)
            .anim(ANIMATION_WALK_STOP, new int[] {three[3]}, 110)
            .anim(ANIMATION_LIE_DOWN, new int[] {three[4], three[5]}, 170)
            .anim(ANIMATION_GET_UP, new int[] {three[5], three[4]}, 160)
            .does(ANIMATION_WALK, 50)
            .does(ANIMATION_IDLE, 50)
            .build();
        int turned = 0;
        for (long seed = 0; seed < 150; seed++) {
            PetBehavior.Session s = new PetBehavior.Session();
            s.groundPx = 80;
            PetBehavior.tap(spec, s, T0, seed);
            // Woken up: it gets up first, then reacts
            assertTrue(s.now.wakes);
            List<PetBehavior.Entry> wake = PetBehavior.timeline(spec, s.now);
            assertEquals(ANIMATION_GET_UP, wake.get(0).animation);
            assertEquals(spec.joyAnimation, wake.get(wake.size() - 1).animation);
            boolean facing = s.now.facingRight;
            while (s.phase != PetBehavior.ASLEEP) {
                for (PetBehavior.Segment segment : new PetBehavior.Segment[] {s.now, s.next}) {
                    if (segment == null) {
                        continue;
                    }
                    if (segment.animation == ANIMATION_WALK) {
                        List<PetBehavior.Entry> walk = PetBehavior.timeline(spec, segment);
                        assertEquals(segment.facingRight != facing, segment.turns);
                        int i = 0;
                        if (segment.turns) {
                            turned++;
                            // Turns to the viewer looking the old way, back looking the new way
                            assertEquals(ANIMATION_TURN, walk.get(0).animation);
                            assertEquals(facing, walk.get(0).facingRight);
                            assertEquals(ANIMATION_TURN, walk.get(2).animation);
                            assertEquals(!facing, walk.get(2).facingRight);
                            i = 3;
                        }
                        assertEquals(ANIMATION_WALK_START, walk.get(i).animation);
                        assertEquals(ANIMATION_WALK_STOP, walk.get(walk.size() - 1).animation);
                        for (PetBehavior.Entry entry : walk) {
                            if (entry.animation != ANIMATION_TURN) {
                                assertEquals(segment.facingRight, entry.facingRight);
                            }
                        }
                    }
                    facing = PetBehavior.facingRightAt(spec, segment, segment.endsAt);
                }
                PetBehavior.advance(spec, s, PetBehavior.planEnd(s));
                if (s.phase == PetBehavior.DOZING) {
                    // Lies down first, then sleeps
                    assertTrue(s.now.lies);
                    assertEquals(ANIMATION_LIE_DOWN, PetBehavior.timeline(spec, s.now).get(0).animation);
                }
            }
        }
        assertTrue(turned > 20);
    }

    @Test
    public void aWidgetOfSeveralSizesWalksTheSameArtPixelsEverywhere() {
        PetSpec spec = PetSpec.forType(PET_PUPPY);
        PlanFrames.Stage narrow = stage(spec, new float[] {222, 300}, Backdrop.MAPLE_PARK);
        PlanFrames.Stage wide = stage(spec, new float[] {420, 160}, Backdrop.MAPLE_PARK);
        float ground = Math.min(narrow.roomPx(), wide.roomPx());
        assertTrue(ground > 0);
        assertEquals(ground, narrow.withGround(ground).ground, 0f);
        assertEquals(ground, wide.withGround(ground).ground, 0f);
        // The same art pixels from the middle at either size
        for (float g = -0.9f; g <= 0.9f; g += 0.05f) {
            assertEquals(narrow.offset(g) / narrow.pixel, wide.offset(g) / wide.pixel);
        }
        assertFalse(PetSpec.forType(PET_CAT).smooth());
        assertNotNull(spec.gait);
    }
}
