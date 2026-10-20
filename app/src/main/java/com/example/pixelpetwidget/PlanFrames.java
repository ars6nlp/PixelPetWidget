package com.example.pixelpetwidget;

import static com.example.pixelpetwidget.PixelCatWidget.ANIMATION_FLY;
import static com.example.pixelpetwidget.PixelCatWidget.ANIMATION_SLEEP;
import static com.example.pixelpetwidget.PixelCatWidget.ANIMATION_WALK;

import java.util.ArrayList;
import java.util.List;

/**
 * What the launcher plays for a pet's plan (PetBehavior): a tree of ViewFlippers and frames,
 * worked out in plain Java so the tests see exactly what the widget shows; PixelCatWidget turns
 * it into RemoteViews.
 *
 * <p>A ViewFlipper shows each child for the same time and starts on its first child when the
 * update is applied; hidden flippers keep running, so copies of a loop run in step. The
 * hand-drawn cat, pug and rabbit get their plans built exactly the old way ({@link #legacyPlan}).
 * The pixel pets' plans never go back to an earlier moment: a timeline (a walk, a flight, getting
 * up, lying down) plays once, the second activity's loop starts in step with its own frames, and
 * once the plan is over the pet holds still until the next update, however late it comes
 * (flippers whose intervals double, so this costs a few views, see {@link #smoothPlan}).
 */
final class PlanFrames {

    /** A plan's first segment with less than this left is skipped in a redraw: the second starts at once. */
    static final long MIN_SWITCH_MS = 300;
    /** How long past the end of its plan a pixel pet holds still, waiting for a late update. */
    static final long TAIL_MS = 30_000;
    /**
     * Deepest a pixel pet's plan may nest (a frame counts one, every flipper around it one more).
     * The launcher refuses views nested more than ten deep ("Too many nested views": the whole
     * update is lost and the caller's process gets the exception); the widget's own layouts take
     * one, and one is kept spare.
     */
    static final int MAX_PLAN_DEPTH = 8;

    private PlanFrames() {
    }

    // --- The tree ----------------------------------------------------------------------------

    interface Node {
        /** Views the launcher creates for it: a frame is a layout with an image, a flipper one view. */
        int views();

        /** How deep it nests: a frame 1, a flipper one more than its deepest child. */
        int depth();

        /** Frames of the pet in it. */
        int frames();
    }

    /** One frame: a drawable placed by the padding of its box, mirrored when scaleX is -1. */
    static final class Frame implements Node {
        final int drawable;
        final int left;
        final int top;
        final int right;
        final int bottom;
        final float scaleX;

        Frame(int drawable, int left, int top, int right, int bottom, float scaleX) {
            this.drawable = drawable;
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.scaleX = scaleX;
        }

        @Override
        public int views() {
            return 2;
        }

        @Override
        public int depth() {
            return 1;
        }

        @Override
        public int frames() {
            return 1;
        }
    }

    /** A ViewFlipper showing each child for {@link #interval} ms in turn, from the first. */
    static final class Flipper implements Node {
        final int interval;
        final List<Node> children = new ArrayList<>();

        Flipper(long interval) {
            this.interval = (int) Math.max(1, Math.min(Integer.MAX_VALUE, interval));
        }

        Flipper add(Node child) {
            children.add(child);
            return this;
        }

        @Override
        public int views() {
            int views = 1;
            for (Node child : children) {
                views += child.views();
            }
            return views;
        }

        @Override
        public int depth() {
            int deepest = 0;
            for (Node child : children) {
                deepest = Math.max(deepest, child.depth());
            }
            return 1 + deepest;
        }

        @Override
        public int frames() {
            int frames = 0;
            for (Node child : children) {
                frames += child.frames();
            }
            return frames;
        }
    }

    // --- Where the pet goes at one widget size ----------------------------------------------

    /**
     * Where the pet's frames go at one widget size, in pixels inside the widget's padding: the
     * padding of a frame standing in the middle of the ground, and how far it may walk sideways.
     * A scene (the aquarium) fills the widget at the largest whole-number scale and stays put.
     */
    static final class Stage {
        int left;
        int top;
        int right;
        int bottom;
        /** Pixels the pet moves sideways at ground offset 1.0 (the hand-drawn pets). */
        int amplitude;
        /** The widget's content size in pixels, and the pixel scale of a pixel-art background (0: none). */
        int width;
        int height;
        int scale;
        /** Screen pixels per art pixel of a pixel pet (0: a hand-drawn pet). */
        int pixel;
        /** Art pixels per ground unit a pixel pet's positions are drawn with (0: a hand-drawn pet). */
        float ground;

        /** Rows under a pixel pet's feet in its frame (its shadow), so it stands on the ground line. */
        static final int FEET_ROWS = 3;
        private static final int PET_BOTTOM_MARGIN_DP = 5;
        /** Share of the side padding the pet may walk into, and a little more on a pixel-art background. */
        private static final float WALK_AMPLITUDE_RATIO = 0.65f;
        private static final float BACKDROP_AMPLITUDE_RATIO = 0.85f;

        static Stage forPet(PetSpec spec, float widthDp, float heightDp, float density, float framePaddingDp,
                            Backdrop backdrop) {
            // The pet takes spec.sizeRatio of the shorter side, centred horizontally and standing
            // on the bottom edge. Sizes are measured inside the widget's own padding.
            float contentWidthDp = Math.max(0f, widthDp - 2 * framePaddingDp);
            float contentHeightDp = Math.max(0f, heightDp - 2 * framePaddingDp);
            float petSizeDp = Math.min(contentWidthDp, contentHeightDp) * spec.sizeRatio;
            if (spec.pixelGrid > 0) {
                // Pixel art stays crisp only at whole-number scales of its native size
                petSizeDp = PixelCatWidget.pixelSnappedSize(petSizeDp * density, spec.pixelGrid) / density;
            }
            Stage stage = new Stage();
            stage.left = Math.max(0, (int) ((contentWidthDp - petSizeDp) / 2f * density));
            stage.right = stage.left;
            stage.bottom = (int) (PET_BOTTOM_MARGIN_DP * density);
            stage.width = Math.round(contentWidthDp * density);
            stage.height = Math.round(contentHeightDp * density);
            if (backdrop != null) {
                // On a pixel-art background the pet stands on its ground line, at the background's
                // pixel scale (the pet's own one; for the hand-drawn pets the nearest whole one)
                stage.scale = Math.max(1, Math.round(petSizeDp * density / PetSpec.PIXEL_GRID));
                stage.bottom = Math.max(0, (backdrop.groundMargin - FEET_ROWS) * stage.scale);
            }
            stage.top = Math.max(0, (int) ((contentHeightDp - petSizeDp) * density) - stage.bottom);
            stage.amplitude = (int) (stage.left * (backdrop != null ? BACKDROP_AMPLITUDE_RATIO : WALK_AMPLITUDE_RATIO));
            if (spec.smooth() && spec.pixelGrid > 0) {
                stage.pixel = Math.max(1, Math.round(petSizeDp * density / spec.pixelGrid));
                stage.ground = stage.roomPx();
            }
            return stage;
        }

        static Stage forScene(PetSpec spec, float widthDp, float heightDp, float density, int framePadding) {
            int width = Math.max(0, Math.round(widthDp * density) - 2 * framePadding);
            int height = Math.max(0, Math.round(heightDp * density) - 2 * framePadding);
            int scale = Math.max(1, Math.min(width / spec.frameWidth, height / spec.frameHeight));
            Stage stage = new Stage();
            stage.left = Math.max(0, (width - spec.frameWidth * scale) / 2);
            stage.top = Math.max(0, (height - spec.frameHeight * scale) / 2);
            stage.right = Math.max(0, width - spec.frameWidth * scale - stage.left);
            stage.bottom = Math.max(0, height - spec.frameHeight * scale - stage.top);
            return stage;
        }

        /** Art pixels per ground unit there is room for at this size (a pixel pet). */
        float roomPx() {
            return pixel > 0 ? (float) Math.floor((float) amplitude / pixel) : 0f;
        }

        /**
         * Draws a pixel pet with the widget's ground (art pixels per ground unit, the same at all
         * of its sizes so a walk covers the same art pixels), or less where there is less room.
         */
        Stage withGround(float widgetGround) {
            if (pixel > 0 && widgetGround > 0) {
                ground = Math.min(widgetGround, roomPx());
            }
            return this;
        }

        /** Screen pixels from the middle at a ground position: whole art pixels for a pixel pet. */
        int offset(float groundPosition) {
            if (pixel > 0 && ground > 0) {
                return Math.round(groundPosition * ground) * pixel;
            }
            return (int) (groundPosition * amplitude);
        }

        /** Mirrors sprites drawn facing the other way. */
        float scaleX(PetSpec spec, boolean facingRight) {
            return spec.scene || spec.facesRight == facingRight ? 1.0f : -1.0f;
        }

        Frame at(int drawable, int offset, float scaleX) {
            return new Frame(drawable, left + offset, top, right - offset, bottom, scaleX);
        }

        /** Frame {@code frame} of an animation, drawn moved as the animation says (PetSpec.Anim.shifts). */
        Frame at(PetSpec.Anim anim, int frame, int offset, float scaleX) {
            int drawable = anim.frames[Math.min(frame, anim.frames.length - 1)];
            int dx = anim.dx(frame) * pixel * (scaleX < 0 ? -1 : 1);
            int dy = anim.dy(frame) * pixel;
            return new Frame(drawable, left + offset + dx, top + dy, right - offset - dx, bottom - dy,
                anim.flipped(frame) ? -scaleX : scaleX);
        }
    }

    // --- Plans -------------------------------------------------------------------------------

    /** The pet's frames for the plan at {@code now}, wearing {@code accessory} (or null). */
    static Node plan(PetSpec spec, PetBehavior.Session session, long now, Stage stage, String accessory) {
        return spec.smooth() ? smoothPlan(spec, session, now, stage, accessory, true)
            : legacyPlan(spec, session, now, stage, accessory);
    }

    /**
     * Views a pixel pet's plan of {@code first} and {@code second} (or null) needs at its start to
     * show all of it: with every copy of the second loop, even if that is over the budget (the plan
     * itself would then play fewer copies and keep still, so PetBehavior plans fewer loops).
     */
    static int views(PetSpec spec, PetBehavior.Segment first, PetBehavior.Segment second) {
        PetBehavior.Session session = new PetBehavior.Session();
        session.phase = PetBehavior.AWAKE;
        session.now = first;
        session.next = second;
        Stage stage = new Stage();
        stage.left = stage.right = stage.top = 400;
        stage.amplitude = 400;
        stage.pixel = 1;
        stage.ground = first.ground > 0 ? first.ground : PetBehavior.DEFAULT_GROUND_PX;
        if (!spec.smooth()) {
            return legacyPlan(spec, session, first.startedAt, stage, null).views();
        }
        Node plan = smoothPlan(spec, session, first.startedAt, stage, null, false);
        // Nested deeper than the launcher takes, it would have to play fewer loops than planned
        return plan.depth() > MAX_PLAN_DEPTH ? Math.max(plan.views(), PetBehavior.MAX_PLAN_VIEWS + 1) : plan.views();
    }

    // --- The hand-drawn pets: exactly as before -----------------------------------------------

    /**
     * The plan of an update: one segment, or two that the launcher switches between by itself.
     * An outer flipper shows the first segment's loop until it ends, then the second one's,
     * repeated in identical copies that run in step, so the switch never goes back to the first
     * one while the next alarm is on its way.
     */
    private static Node legacyPlan(PetSpec spec, PetBehavior.Session session, long now, Stage stage,
                                   String accessory) {
        PetBehavior.Segment first = PetBehavior.segmentAt(session, now);
        PetBehavior.Segment second = first == session.now ? session.next : null;
        long left = first.endsAt - now;
        if (second != null && left < MIN_SWITCH_MS) {
            // Almost over: start with the second one straight away
            first = second;
            second = null;
        }
        if (second == null) {
            return legacyLoop(spec, first, now - first.startedAt, stage, accessory);
        }
        Flipper outer = new Flipper(left);
        outer.add(legacyLoop(spec, first, now - first.startedAt, stage, accessory));
        // The second one starts `left` from now: its loop is rotated to begin right then
        int copies = PetBehavior.nextCopies(left, second.lengthMs(), legacyFrameCount(spec, second));
        for (int copy = 0; copy < copies; copy++) {
            outer.add(legacyLoop(spec, second, -left, stage, accessory));
        }
        return outer;
    }

    /** Frames one segment puts in its flipper (a walk: there and back). */
    static int legacyFrameCount(PetSpec spec, PetBehavior.Segment segment) {
        int legSteps = PetBehavior.legSteps(spec, segment);
        if (legSteps > 0 && PetBehavior.arrives(segment)) {
            return legSteps + arrivalHold(spec, segment, legSteps);
        }
        if (legSteps > 0) {
            return 2 * legSteps;
        }
        return Math.min(spec.animation(segment.animation).frames.length, spec.maxLoopFrames());
    }

    /**
     * A segment's frames in a flipper of its own, the first of them being what the segment shows
     * {@code offsetMs} after its start (negative: before it starts, for a segment that comes second).
     */
    private static Node legacyLoop(PetSpec spec, PetBehavior.Segment segment, long offsetMs, Stage stage,
                                   String accessory) {
        int legSteps = PetBehavior.legSteps(spec, segment);
        if (legSteps > 0 && PetBehavior.arrives(segment)) {
            // Over to a prop: the steps there, then it stands at it until the next update
            int[] walk = spec.animation(ANIMATION_WALK, accessory).frames;
            int pose = spec.animation(segment.variant, accessory).frames[0];
            int total = legSteps + arrivalHold(spec, segment, legSteps);
            long base = Math.floorDiv(offsetMs, spec.walkStepMs());
            float scaleX = stage.scaleX(spec, segment.facingRight);
            Flipper loop = new Flipper(spec.walkStepMs());
            for (int j = 0; j < total; j++) {
                int step = (int) Math.floorMod(base + j, (long) total);
                int offset = stage.offset(PetBehavior.positionAtStep(spec, segment, step));
                int drawable = step < legSteps ? walk[step % walk.length] : pose;
                loop.add(stage.at(drawable, offset, scaleX));
            }
            return loop;
        }
        if (legSteps > 0) {
            // Back and forth along the leg, starting with the step the pet is on now
            int[] frames = spec.animation(ANIMATION_WALK, accessory).frames;
            long base = Math.floorDiv(offsetMs, spec.walkStepMs());
            Flipper loop = new Flipper(spec.walkStepMs());
            for (int j = 0; j < 2 * legSteps; j++) {
                long step = base + j;
                int offset = stage.offset(PetBehavior.positionAtStep(spec, segment, step));
                loop.add(stage.at(frames[(int) Math.floorMod(step, (long) frames.length)], offset,
                    stage.scaleX(spec, PetBehavior.facingRightAtStep(spec, segment, step))));
            }
            return loop;
        }
        return loop(spec, segment, offsetMs, stage, accessory);
    }

    /** Frames a pet stands at a prop after walking over: the rest of the segment and a late alarm. */
    private static int arrivalHold(PetSpec spec, PetBehavior.Segment segment, int legSteps) {
        int step = spec.walkStepMs();
        long rest = Math.max(0, segment.lengthMs() - (long) legSteps * step) + PetBehavior.LATE_MARGIN_MS;
        return (int) ((rest + step - 1) / step);
    }

    /**
     * The frames of a loop that are shown from {@code fromMs} to {@code toMs} after the update: all
     * of the loop if that is a whole loop or more, otherwise only those, placed so that each one
     * comes up at its time (a flipper of n frames shows frame k mod n in the k-th interval).
     */
    private static Flipper loop(PetSpec spec, PetBehavior.Segment segment, long offsetMs, long fromMs, long toMs,
                                Stage stage, String accessory) {
        PetSpec.Anim anim = spec.animation(segment.animation, accessory);
        int count = Math.min(anim.frames.length, spec.maxLoopFrames());
        long first = Math.floorDiv(fromMs, anim.intervalMs);
        long last = Math.floorDiv(Math.max(fromMs, toMs - 1), anim.intervalMs);
        int shown = (int) (last - first + 1);
        if (shown >= count) {
            return loop(spec, segment, offsetMs, stage, accessory);
        }
        long base = Math.floorDiv(offsetMs, anim.intervalMs);
        int offset = stage.offset(segment.from);
        Frame[] frames = new Frame[shown];
        for (long k = first; k <= last; k++) {
            frames[(int) Math.floorMod(k, (long) shown)] = stage.at(anim, (int) Math.floorMod(base + k, (long) count),
                offset, stage.scaleX(spec, segment.facingRight));
        }
        Flipper loop = new Flipper(anim.intervalMs);
        for (Frame frame : frames) {
            loop.add(frame);
        }
        return loop;
    }

    /** On the spot (eat, sit, sleep, the aquarium's loops), looking the way it last went. */
    private static Flipper loop(PetSpec spec, PetBehavior.Segment segment, long offsetMs, Stage stage,
                                String accessory) {
        PetSpec.Anim anim = spec.animation(segment.animation, accessory);
        int count = Math.min(anim.frames.length, spec.maxLoopFrames());
        long base = Math.floorDiv(offsetMs, anim.intervalMs);
        int offset = stage.offset(segment.from);
        Flipper loop = new Flipper(anim.intervalMs);
        for (int j = 0; j < count; j++) {
            loop.add(stage.at(anim, (int) Math.floorMod(base + j, (long) count), offset,
                stage.scaleX(spec, segment.facingRight)));
        }
        return loop;
    }

    // --- The pixel pets ----------------------------------------------------------------------

    /**
     * A pixel pet's plan. The first segment (a loop, or a timeline played once) until it ends;
     * then the second one, always a loop, which the launcher switches to exactly when one of its
     * frames begins, in copies that run in step until its end; then the pet keeps still in its
     * last pose. Each further level of flippers lasts twice as long as the one inside it, so a
     * level shows the copy or the still frame for as long as everything before it took (a copy
     * shown for less than a loop has only the frames it shows). {@code fit}: within the widget's
     * budget (otherwise the whole plan, to count the views it needs).
     */
    private static Node smoothPlan(PetSpec spec, PetBehavior.Session session, long now, Stage stage,
                                   String accessory, boolean fit) {
        PetBehavior.Segment first = PetBehavior.segmentAt(session, now);
        PetBehavior.Segment second = first == session.now ? session.next : null;
        long left = Math.max(1, first.endsAt - now);
        if (second != null) {
            // The second loop's frames start in step with the switch to it: a little before the
            // first loop's end, or after a timeline's end (it keeps its last pose a moment, for
            // its steps must all be shown)
            int tick = PetBehavior.tickMs(spec, second);
            long aligned = left - Math.floorMod(left, (long) tick);
            if (PetBehavior.linear(spec, first)) {
                left = aligned < left ? aligned + tick : left;
            } else if (aligned < MIN_SWITCH_MS) {
                first = second;
                second = null;
                left = Math.max(1, first.endsAt - now);
            } else {
                left = aligned;
            }
        }
        if (second == null) {
            // Its whole loops or its timeline, then still in its last pose until the next update:
            // every plan ends in the pose the next one begins with, however late the alarm
            long framesLeft = Math.max(1, Math.min(left, PetBehavior.framesEnd(spec, first) - now));
            Node shown = segment(spec, session, first, now - first.startedAt, framesLeft, stage, accessory);
            return holdStill(shown, framesLeft, left + TAIL_MS, rest(spec, first, stage, accessory));
        }
        Node shown = segment(spec, session, first, now - first.startedAt, left, stage, accessory);
        Node pair = pair(spec, shown, left, second, Integer.MAX_VALUE, stage, accessory);
        if (!fit || fits(pair)) {
            return pair;
        }
        // A redraw in the middle of a plan may need more copies of the second loop than there is
        // room for (views, or levels the launcher takes): it plays as many as fit, then keeps
        // still on its first frame
        for (int copies = 8; copies >= 1; copies--) {
            pair = pair(spec, shown, left, second, copies, stage, accessory);
            if (fits(pair)) {
                return pair;
            }
        }
        // Not even one fits: a loop that is under way gives way to the second one at once (a
        // timeline goes on to its end)
        return PetBehavior.linear(spec, first) ? pair : smoothPlan(spec, onlySecond(session), now, stage, accessory, true);
    }

    /** Within the widget's budget: views, and how deep the launcher nests them. */
    private static boolean fits(Node plan) {
        return plan.views() <= PetBehavior.MAX_PLAN_VIEWS && plan.depth() <= MAX_PLAN_DEPTH;
    }

    /**
     * {@code shown} for {@code left} ms, then the loop of {@code second} in up to {@code copies}
     * copies until its end, then still on its first frame.
     */
    private static Node pair(PetSpec spec, Node shown, long left, PetBehavior.Segment second, int copies, Stage stage,
                             String accessory) {
        // The second loop's whole loops, then its first frame still (the plan may last a little longer)
        long secondEnds = left + PetBehavior.framesEnd(spec, second) - second.startedAt;
        Frame still = rest(spec, second, stage, accessory);
        Flipper levels = new Flipper(left).add(shown).add(secondPart(spec, second, left, left, secondEnds, stage,
            accessory, still));
        long covered = 2 * left;
        for (int copy = 1; covered < secondEnds && copy < copies; copy++) {
            levels = new Flipper(covered).add(levels).add(secondPart(spec, second, left, covered, secondEnds, stage,
                accessory, still));
            covered *= 2;
        }
        return hold(levels, covered, secondEnds + TAIL_MS, still);
    }

    /**
     * {@code inner} (a flipper that wraps after {@code coveredMs}) wrapped in levels that show
     * {@code still} after it until at least {@code untilMs}, no deeper than a plan may go: each
     * level shows everything before it once, then the still frame as many times as it must (one
     * copy doubles the time; a level that has to cover more takes more copies).
     */
    private static Node hold(Node inner, long coveredMs, long untilMs, Frame still) {
        Node levels = inner;
        long covered = Math.max(1, coveredMs);
        for (int copies : holdCopies(covered, untilMs, MAX_PLAN_DEPTH - inner.depth())) {
            Flipper level = new Flipper(covered).add(levels);
            for (int c = 0; c < copies; c++) {
                level.add(still);
            }
            levels = level;
            covered *= copies + 1;
        }
        return levels;
    }

    /**
     * Copies of the still frame in each level of a hold from {@code coveredMs} to at least
     * {@code untilMs} in at most {@code levels} levels (one if none is left): one per level (each
     * doubles the time) while the levels last, more where they would not.
     */
    static List<Integer> holdCopies(long coveredMs, long untilMs, int levels) {
        List<Integer> copies = new ArrayList<>();
        long covered = Math.max(1, coveredMs);
        while (covered < untilMs) {
            int left = Math.max(1, levels - copies.size());
            double needed = Math.pow((double) untilMs / covered, 1.0 / left);
            int factor = (int) Math.max(2, Math.ceil(needed - 1e-9));
            copies.add(factor - 1);
            covered *= factor;
        }
        return copies;
    }

    /**
     * What a level shows of the second segment from {@code from} to twice that (ms after the
     * update): its loop, or its loop until it ends at {@code ends} and then the still frame.
     */
    private static Node secondPart(PetSpec spec, PetBehavior.Segment second, long left, long from, long ends,
                                   Stage stage, String accessory, Frame still) {
        Node loop = loop(spec, second, -left, from, Math.min(2 * from, ends), stage, accessory);
        if (2 * from <= ends) {
            return loop;
        }
        // The loop until `ends`, then still (this flipper wraps no sooner than 2 x ends, past the level)
        return new Flipper(ends).add(loop).add(still);
    }

    /** The session as if its second segment were on now. */
    private static PetBehavior.Session onlySecond(PetBehavior.Session session) {
        PetBehavior.Session copy = new PetBehavior.Session();
        copy.phase = session.phase;
        copy.groundPx = session.groundPx;
        copy.now = session.next;
        return copy;
    }

    /**
     * {@code shown} for {@code shownMs}, then {@code still} until at least {@code untilMs}. With
     * nothing left to show (a redraw after the plan's frames are over) just the still frame, the
     * pose the plan ends in.
     */
    private static Node holdStill(Node shown, long shownMs, long untilMs, Frame still) {
        if (shownMs <= 1) {
            return still;
        }
        return hold(new Flipper(shownMs).add(shown).add(still), 2 * shownMs, untilMs, still);
    }

    /** A pixel pet's segment from {@code offsetMs} after its start: a flight, a timeline or a loop. */
    private static Node segment(PetSpec spec, PetBehavior.Session session, PetBehavior.Segment segment,
                                long offsetMs, long shownMs, Stage stage, String accessory) {
        if (PetBehavior.flying(spec, session, segment)) {
            return flight(spec, segment, offsetMs, stage, accessory);
        }
        if (PetBehavior.linear(spec, segment)) {
            List<PetBehavior.Entry> entries = PetBehavior.timeline(spec, segment);
            int tick = PetBehavior.tickMs(spec, segment);
            int first = (int) Math.min(entries.size() - 1, Math.max(0, offsetMs) / tick);
            Flipper timeline = new Flipper(tick);
            for (int j = Math.max(0, first); j < entries.size(); j++) {
                timeline.add(entryFrame(spec, segment, entries.get(j), stage, accessory));
            }
            // Its last pose as long as it may stay on screen past its frames (never its start again)
            Frame last = rest(spec, segment, stage, accessory);
            long sinceEntry = Math.floorMod(Math.max(0, offsetMs), (long) tick);
            while (timeline.children.isEmpty() || (long) timeline.children.size() * tick - sinceEntry < shownMs) {
                timeline.add(last);
            }
            return timeline;
        }
        return loop(spec, segment, offsetMs, stage, accessory);
    }

    /** One entry of a timeline where the pet is at it: whole art pixels along the walk from its start. */
    private static Frame entryFrame(PetSpec spec, PetBehavior.Segment segment, PetBehavior.Entry entry, Stage stage,
                                    String accessory) {
        return stage.at(spec.animation(entry.animation, accessory), entry.frame, walkOffset(segment, entry.px, stage),
            stage.scaleX(spec, entry.facingRight));
    }

    /** Screen offset {@code px} art pixels along a walk (drawn at the stage's ground). */
    static int walkOffset(PetBehavior.Segment segment, int px, Stage stage) {
        if (stage.pixel <= 0 || stage.ground <= 0 || segment.ground <= 0) {
            return stage.offset(segment.from);
        }
        int start = Math.round(segment.from * stage.ground);
        int along = stage.ground == segment.ground ? px : Math.round(px * stage.ground / segment.ground);
        int way = segment.to > segment.from ? 1 : segment.to < segment.from ? -1 : 0;
        return (start + way * along) * stage.pixel;
    }

    /** The pose a pixel pet keeps after a segment: lying asleep, standing where its walk or flight ended. */
    private static Frame rest(PetSpec spec, PetBehavior.Segment segment, Stage stage, String accessory) {
        if (segment.lies) {
            return stage.at(spec.animation(ANIMATION_SLEEP, accessory), 0, stage.offset(segment.to),
                stage.scaleX(spec, segment.facingRight));
        }
        if (PetBehavior.flying(spec, awake(), segment)) {
            PetSpec.Anim land = spec.animation(PixelCatWidget.ANIMATION_LAND, accessory);
            return stage.at(land, land.frames.length - 1, stage.offset(segment.to),
                stage.scaleX(spec, segment.facingRight));
        }
        if (PetBehavior.linear(spec, segment)) {
            List<PetBehavior.Entry> entries = PetBehavior.timeline(spec, segment);
            if (!entries.isEmpty()) {
                return entryFrame(spec, segment, entries.get(entries.size() - 1), stage, accessory);
            }
        }
        // A loop's still frame: its first, the pose its loop begins and ends with
        return stage.at(spec.animation(segment.animation, accessory), 0, stage.offset(segment.from),
            stage.scaleX(spec, segment.facingRight));
    }

    private static PetBehavior.Session awake() {
        PetBehavior.Session session = new PetBehavior.Session();
        session.phase = PetBehavior.AWAKE;
        return session;
    }

    /**
     * A flight over the widget: turning round on the spot if it flies the other way, the take-off,
     * the path worked out for this size (FlightPath, no farther per frame than
     * PetSpec.MAX_FLIGHT_SHIFT_PX with the wingbeat's bob), the landing. A redraw in mid-flight carries on from the frame
     * on screen.
     */
    private static Node flight(PetSpec spec, PetBehavior.Segment segment, long offsetMs, Stage stage,
                               String accessory) {
        int interval = spec.animation(ANIMATION_FLY).intervalMs;
        List<Frame> frames = new ArrayList<>();
        for (PetBehavior.Entry entry : PetBehavior.timeline(spec, segment)) {
            frames.add(entryFrame(spec, segment, entry, stage, accessory));
        }
        int freeWidth = stage.left + stage.right;
        int fromX = stage.left + stage.offset(segment.from);
        int toX = stage.left + stage.offset(segment.to);
        int air = FlightPath.TAKEOFF_FRAMES + segment.steps + FlightPath.LAND_FRAMES;
        FlightPath.Pose[] poses = FlightPath.plan(segment.variant, air, fromX, toX, freeWidth, stage.top,
            (PetSpec.MAX_FLIGHT_SHIFT_PX - PetSpec.FLIGHT_BOB_PX) * Math.max(1, stage.pixel));
        for (FlightPath.Pose pose : poses) {
            PetSpec.Anim anim = spec.animation(pose.animation, accessory);
            int frame = Math.min(pose.frame, anim.frames.length - 1);
            float scaleX = stage.scaleX(spec, pose.facingRight);
            int dx = anim.dx(frame) * Math.max(1, stage.pixel) * (scaleX < 0 ? -1 : 1);
            int dy = anim.dy(frame) * Math.max(1, stage.pixel);
            // Higher up, the gap below grows by as much as the gap above shrinks
            frames.add(new Frame(anim.frames[frame], pose.x + dx, pose.y + dy, freeWidth - pose.x - dx,
                stage.bottom + stage.top - pose.y - dy, anim.flipped(frame) ? -scaleX : scaleX));
        }
        Flipper flipper = new Flipper(interval);
        int first = (int) Math.min(frames.size() - 1, Math.max(0, offsetMs) / interval);
        for (int f = first; f < frames.size(); f++) {
            flipper.add(frames.get(f));
        }
        return flipper;
    }
}
