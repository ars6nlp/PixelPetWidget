package com.example.pixelpetwidget;

import static com.example.pixelpetwidget.PixelCatWidget.ANIMATION_FLY;
import static com.example.pixelpetwidget.PixelCatWidget.ANIMATION_GET_UP;
import static com.example.pixelpetwidget.PixelCatWidget.ANIMATION_LIE_DOWN;
import static com.example.pixelpetwidget.PixelCatWidget.ANIMATION_SLEEP;
import static com.example.pixelpetwidget.PixelCatWidget.ANIMATION_TURN;
import static com.example.pixelpetwidget.PixelCatWidget.ANIMATION_WALK;
import static com.example.pixelpetwidget.PixelCatWidget.ANIMATION_WALK_START;
import static com.example.pixelpetwidget.PixelCatWidget.ANIMATION_WALK_STOP;

import java.util.ArrayList;
import java.util.List;

/**
 * A pet's own life in the widget, the same for every pet: it sleeps until a tap wakes it, then
 * for {@link PetSpec#awakeMs} picks its activities from {@link PetSpec#activities} by weight, walks
 * about the ground, and finally dozes off and freezes until the next tap.
 *
 * <p>Every activity plays its animation {@link PetSpec#minRepeats}..{@link PetSpec#maxRepeats}
 * times in a row (a walk counts its step cycles), one-shots ({@link PetSpec.Activity#once}) once,
 * and the next activity is always a different one.
 *
 * <p>Android delivers an app's alarm no sooner than 5 s after it was set, and most loops are
 * shorter than that. So the widget gets a plan of one or two {@link Segment}s per update: when
 * the first one is too short, a second one follows it in the same update and the launcher
 * switches between them by itself (see PlanFrames). The alarm comes at the end of the plan.
 *
 * <p>The hand-drawn cat, pug and rabbit live exactly as they always did: every segment is a
 * seamless loop on the widget (a walk goes back and forth between two points), so if the next
 * alarm comes late the pet simply carries on for a moment. The pixel pets move smoothly (see the
 * section about them below): they walk whole art pixels per frame in step with their paws, one
 * way per walk, turn round, get up and lie down, and their plans never jump back.
 *
 * <p>Plain Java on purpose: the widget only stores a {@link Session} per widget. Random choices
 * come from the session's own generator, seeded on waking, so a seed always gives the same life.
 */
final class PetBehavior {

    static final int ASLEEP = 0;
    static final int AWAKE = 1;
    /** Falling asleep: the sleep animation plays once more, then the widget freezes. */
    static final int DOZING = 2;

    /** Shortest plan per update: Android delivers an app's alarm no sooner than 5 s after it was set. */
    static final int MIN_PLAN_MS = 5_000;
    /** How long the sleep animation plays before the widget stops on its first frame. */
    static final int DOZE_MS = 5_000;
    /** Plans due this soon are advanced together with the alarm that came for another widget. */
    static final long EARLY_MS = 500;
    /** A session this far past its turn missed its alarms (the phone was off, say): it is asleep. */
    static final long OVERDUE_MS = 60_000;

    // The ground: positions are offsets from the widget's centre, -1..1 of the travel range
    /** The pet keeps this far from the ends of its range. */
    static final float GROUND_LIMIT = 0.9f;
    /** Ground covered by one walk step: the whole ground in 7 steps, as the walk always went. */
    static final float WALK_STEP = 2 * GROUND_LIMIT / 7;
    /** Shortest walk leg, in steps, so a walk is never a shuffle on the spot. */
    static final int MIN_LEG_STEPS = 2;
    /** A flight lands at least this far from where it took off. */
    static final float MIN_FLIGHT = 0.6f;
    /**
     * Variety: once this many animations have been done in an awake spell, the ones not done yet
     * come first (by weight among themselves), so every pet shows all it can within a minute.
     */
    static final int VARIETY_AFTER = 2;
    /** The first background event may come this long after waking. */
    static final int FIRST_EVENT_AFTER_MS = 10_000;

    /** One activity in a plan: what the pet does, where, and from when to when. */
    static final class Segment {
        /** ANIMATION_*. */
        int animation = ANIMATION_SLEEP;
        long startedAt;
        long endsAt;
        /** Where the pet stands, or where its walk or flight starts. */
        float from;
        /** The other end of a walk leg, or where a flight lands; equal to {@link #from} otherwise. */
        float to;
        /** Which way it looks while standing (after a flight: the way it landed). */
        boolean facingRight;
        /** Shape of a flight (FlightPath.ARC, HIGH, WAVE); for an event, how its prop moves. */
        int variant;
        /**
         * Row of BackgroundEvents.TABLE when this segment is part of a background event, else -1:
         * the interaction itself (its effect plays with it), or the walk or flight over to the
         * prop, which ends there (see {@link #arrives}).
         */
        int event = -1;
        // The pixel pets' timelines (see timeline()); the hand-drawn pets never use these
        /** Art pixels per ground unit this segment was planned with (see {@link Session#groundPx}). */
        float ground;
        /** It starts by turning round: a walk or a flight the other way than the pet looked. */
        boolean turns;
        /** It starts by getting up: the reaction to the tap that woke the pet. */
        boolean wakes;
        /** It starts by lying down: dozing off. */
        boolean lies;
        /** A pose of an interaction played with its way in and out (PetSpec.ENTER, LEAVE). */
        boolean framed;
        /** Ticks the pet looks at a background event's prop before it walks over. */
        int look;
        /** Walk frames of a walk; frames in the air of a flight; loops after getting up or lying down. */
        int steps;

        long lengthMs() {
            return endsAt - startedAt;
        }
    }

    /** Everything the widget keeps about one pet's life; stored per widget. */
    static final class Session {
        int phase = ASLEEP;
        /** When the pet falls asleep unless tapped again. */
        long awakeUntil;
        /** What it does now; while asleep, where it sleeps. */
        Segment now = new Segment();
        /** What it does right after {@link #now} in the same update, or null. */
        Segment next;
        /** Flights left in this awake spell. */
        int flightsLeft;
        /** Animations done in this awake spell, one bit each (for variety). */
        int seen;
        /** Background events left in this awake spell, and when the last one began. */
        int eventsLeft;
        long lastEventAt;
        /** Row of BackgroundEvents.TABLE whose interaction comes with the next plan (the pet went over to its prop), else -1. */
        int eventNext = -1;
        /** State of the random generator. */
        long random;
        /**
         * Pixel pets: art pixels per ground unit on this widget (the narrowest of its sizes), so a
         * walk covers whole art pixels; 0 until the widget measured it ({@link #DEFAULT_GROUND_PX}).
         */
        float groundPx;
    }

    private PetBehavior() {
    }

    // --- Events ----------------------------------------------------------------------------

    /**
     * A tap: a sleeping pet wakes up with its wake reaction and a fresh generator from
     * {@code seed}; an awake one cheers up and stays awake {@link PetSpec#awakeMs} from now.
     */
    static void tap(PetSpec spec, Session s, long now, long seed) {
        tap(spec, null, s, now, seed);
    }

    /** {@link #tap} on a background with an interaction for this pet (null: none). */
    static void tap(PetSpec spec, BackgroundEvents.Interaction interaction, Session s, long now, long seed) {
        Segment current = segmentAt(s, now);
        float here = positionAt(spec, current, now);
        boolean facing = facingRightAt(spec, current, now);
        int reaction;
        boolean waking = !isAwake(s, now);
        if (!waking) {
            if (flying(spec, s, current) && now < current.endsAt) {
                // Busy flying: it lands first, but stays awake longer
                s.awakeUntil = now + spec.awakeMs;
                return;
            }
            reaction = spec.joyAnimation;
        } else {
            s.random = seed;
            s.flightsLeft = spec.flightsPerSession;
            s.seen = 0;
            s.eventsLeft = BackgroundEvents.MAX_PER_SESSION;
            // The first event may come a while after waking, the next ones MIN_GAP_MS apart
            s.lastEventAt = now - BackgroundEvents.MIN_GAP_MS + FIRST_EVENT_AFTER_MS;
            reaction = spec.wakeAnimation;
        }
        s.phase = AWAKE;
        s.awakeUntil = now + spec.awakeMs;
        s.eventNext = -1;
        if (spec.smooth()) {
            // A pixel pet woken up gets up first (if it can), then reacts
            boolean getsUp = waking && spec.hasAnimation(ANIMATION_GET_UP);
            Pick first = new Pick(reaction, false, getsUp ? spec.minRepeats : reactionRepeats(spec, s, reaction));
            planSmooth(spec, s, first, null, -1, here, facing, now, getsUp);
            fillToAlarm(spec, s);
            return;
        }
        Pick first = new Pick(reaction, false, reactionRepeats(spec, s, reaction));
        plan(spec, s, first, null, -1, here, facing, now);
    }

    /**
     * Moves the life on to {@code now} when its plan has run out: the next plan, dozing off or
     * the final stop. Returns whether anything changed, i.e. whether the widget needs a redraw.
     */
    static boolean advance(PetSpec spec, Session s, long now) {
        return advance(spec, null, s, now);
    }

    /** {@link #advance} on a background with an interaction for this pet (null: none). */
    static boolean advance(PetSpec spec, BackgroundEvents.Interaction interaction, Session s, long now) {
        if (s.phase == ASLEEP || now + EARLY_MS < planEnd(s)) {
            return false;
        }
        Segment last = s.next != null ? s.next : s.now;
        float here = positionAt(spec, last, now);
        boolean facing = facingRightAt(spec, last, now);
        if (s.phase == DOZING || now >= planEnd(s) + OVERDUE_MS) {
            stopAsleep(s, here, facing);
        } else if (now >= s.awakeUntil) {
            // Lies down where it is; the sleep animation plays for a moment, then everything stops
            s.phase = DOZING;
            Segment doze = new Segment();
            doze.from = here;
            doze.to = here;
            doze.facingRight = facing;
            doze.startedAt = now;
            doze.endsAt = now + wholeLoops(spec.animation(ANIMATION_SLEEP).loopMs(), DOZE_MS);
            if (spec.smooth() && spec.hasAnimation(ANIMATION_LIE_DOWN)) {
                // A pixel pet lies down first, then sleeps the loops a doze takes
                doze.steps = (int) repeatsOf(spec, doze);
                doze.lies = true;
                doze.endsAt = now + timeline(spec, doze).size() * (long) tickMs(spec, doze);
            }
            s.now = doze;
            s.next = null;
            s.eventNext = -1;
        } else if (s.eventNext >= 0 && interaction != null && BackgroundEvents.TABLE[s.eventNext] == interaction) {
            if (spec.smooth() && spec.walks() && !atProp(interaction, s, here)) {
                // Still on its way over (a long walk takes more than one update): it walks on
                walkOver(spec, interaction, s, here, facing, false, now);
                fillToAlarm(spec, s);
                return true;
            }
            // It went over to the prop: now it plays with it
            s.eventNext = -1;
            planAct(spec, interaction, s, here, facing, now);
        } else {
            s.eventNext = -1;
            PetSpec.Activity activity = choose(spec, s, last.animation,
                eventOption(interaction, s, now, last.animation));
            if (activity.animation == BackgroundEvents.ANIMATION_EVENT) {
                startEvent(spec, interaction, s, here, facing, last.animation, now);
            } else if (spec.smooth()) {
                Pick first = new Pick(activity.animation, activity.once, repeats(spec, s, activity.once));
                planSmooth(spec, s, first, activity, last.animation, here, facing, now, false);
            } else {
                Pick first = new Pick(activity.animation, activity.once, repeats(spec, s, activity.once));
                plan(spec, s, first, activity, last.animation, here, facing, now);
            }
        }
        if (spec.smooth() && s.phase == AWAKE) {
            fillToAlarm(spec, s);
        }
        return true;
    }

    /** Asleep at once, where it is now: at night, when the screen goes off, after a reboot. */
    static void sleepNow(PetSpec spec, Session s, long now) {
        if (s.phase != ASLEEP) {
            Segment current = segmentAt(s, now);
            stopAsleep(s, positionAt(spec, current, now), facingRightAt(spec, current, now));
        }
    }

    /** Whether a tap now would find the pet awake (dozing counts as asleep). */
    static boolean isAwake(Session s, long now) {
        return s.phase == AWAKE && now < s.awakeUntil && now < planEnd(s) + OVERDUE_MS;
    }

    /** When this session needs {@link #advance} next; Long.MAX_VALUE while it sleeps. */
    static long nextTurn(Session s) {
        return s.phase == ASLEEP ? Long.MAX_VALUE : planEnd(s);
    }

    /** End of the current plan. */
    static long planEnd(Session s) {
        return s.next != null ? s.next.endsAt : s.now.endsAt;
    }

    /** The segment on screen at {@code now}. */
    static Segment segmentAt(Session s, long now) {
        return s.next != null && now >= s.now.endsAt ? s.next : s.now;
    }

    private static void stopAsleep(Session s, float here, boolean facing) {
        s.phase = ASLEEP;
        s.eventNext = -1;
        Segment rest = new Segment();
        rest.from = here;
        rest.to = here;
        rest.facingRight = facing;
        s.now = rest;
        s.next = null;
    }

    // --- Planning --------------------------------------------------------------------------

    /** An activity picked for a plan, before it is placed: its animation and how often it plays. */
    private static final class Pick {
        final int animation;
        final boolean once;
        int repeats;

        Pick(int animation, boolean once, int repeats) {
            this.animation = animation;
            this.once = once;
            this.repeats = repeats;
        }

        long lengthMs(PetSpec spec) {
            return repeats * loopMs(spec, animation);
        }
    }

    /**
     * The plan of one update. A first activity long enough for an update (or one with its own
     * path, the flight) plays alone; a shorter one gets a second, different activity that makes
     * the plan reach {@link #MIN_PLAN_MS}. The longer of the two goes first when the rules allow
     * it (the reaction to a tap always comes first): the second one is then never longer than
     * the first, which keeps the widget's copies of it few.
     *
     * @param activity the first one's config, null for a reaction
     * @param previous what the pet did before the plan, -1 after a tap
     */
    private static void plan(PetSpec spec, Session s, Pick first, PetSpec.Activity activity, int previous,
                             float here, boolean facing, long now) {
        s.next = null;
        markSeen(s, first.animation);
        if (scripted(spec, first.animation)) {
            s.now = ownFlight(spec, s, here, facing, now);
            return;
        }
        long length = first.lengthMs(spec);
        if (length >= minPlanMs(spec)) {
            s.now = build(spec, s, first, here, facing, now);
            return;
        }
        long missing = minPlanMs(spec) - length;
        PetSpec.Activity filler = chooseFiller(spec, s, first, activity, previous, missing);
        if (filler == null) {
            // Nothing fits after it: the first activity takes its longest run instead
            first.repeats = first.once ? 1 : spec.maxRepeats;
            s.now = build(spec, s, first, here, facing, now);
            return;
        }
        boolean swappable = swappable(filler, activity, previous);
        long cover = swappable ? Long.MAX_VALUE : cover(spec, length, filler);
        Pick second = new Pick(filler.animation, filler.once, fillingRepeats(spec, s, filler, missing, cover));
        boolean swap = swappable && second.lengthMs(spec) > length;
        Pick a = swap ? second : first;
        Pick b = swap ? first : second;
        markSeen(s, a.animation);
        markSeen(s, b.animation);
        s.now = build(spec, s, a, here, facing, now);
        s.next = build(spec, s, b, positionAt(spec, s.now, s.now.endsAt), facingRightAt(spec, s.now, s.now.endsAt),
            s.now.endsAt);
    }

    /** Whether the filler may play first instead: allowed after what came before, and the first after it. */
    private static boolean swappable(PetSpec.Activity filler, PetSpec.Activity first, int previous) {
        return first != null && filler.animation != previous && !filler.notAfter(previous)
            && !first.notAfter(filler.animation);
    }

    /** How long a filler that has to stay second may last: what the widget's copies of it cover. */
    private static long cover(PetSpec spec, long firstMs, PetSpec.Activity filler) {
        return (long) nextCopies(firstMs, Long.MAX_VALUE / 4, framesBound(spec, filler.animation)) * firstMs
            - LATE_MARGIN_MS;
    }

    /**
     * The second activity of a plan, by weight: different from the first, able to reach
     * {@code missingMs}, and either short enough for the widget's copies of it or free to go first.
     */
    private static PetSpec.Activity chooseFiller(PetSpec spec, Session s, Pick first, PetSpec.Activity activity,
                                                 int previous, long missingMs) {
        long length = first.lengthMs(spec);
        List<PetSpec.Activity> fitting = new ArrayList<>();
        for (PetSpec.Activity filler : spec.activities) {
            if (!allowed(spec, filler, s, first.animation, true, missingMs)) {
                continue;
            }
            long loop = loopMs(spec, filler.animation);
            long shortest = filler.once ? loop : Math.max(spec.minRepeats, (missingMs + loop - 1) / loop) * loop;
            if (swappable(filler, activity, previous) || shortest <= cover(spec, length, filler)) {
                fitting.add(filler);
            }
        }
        return pick(s, fitting);
    }

    /** By weight; while some of them have not been done in this awake spell, among those first. */
    private static PetSpec.Activity pick(Session s, List<PetSpec.Activity> options) {
        if (options.isEmpty()) {
            return null;
        }
        List<PetSpec.Activity> fresh = new ArrayList<>();
        if (Integer.bitCount(s.seen) >= VARIETY_AFTER) {
            for (PetSpec.Activity option : options) {
                if ((s.seen & (1 << option.animation)) == 0) {
                    fresh.add(option);
                }
            }
        }
        List<PetSpec.Activity> from = fresh.isEmpty() ? options : fresh;
        int total = 0;
        for (PetSpec.Activity option : from) {
            total += option.weight;
        }
        int choice = nextInt(s, total);
        for (PetSpec.Activity option : from) {
            choice -= option.weight;
            if (choice < 0) {
                return option;
            }
        }
        return from.get(from.size() - 1);
    }

    private static void markSeen(Session s, int animation) {
        s.seen |= 1 << animation;
    }

    /**
     * The background event as an option for the next activity, weighted as the interaction says;
     * null when there is none or it is too soon (see BackgroundEvents).
     */
    static PetSpec.Activity eventOption(BackgroundEvents.Interaction interaction, Session s, long now) {
        if (interaction == null || s.eventsLeft <= 0 || now - s.lastEventAt < BackgroundEvents.MIN_GAP_MS) {
            return null;
        }
        return new PetSpec.Activity(BackgroundEvents.ANIMATION_EVENT, interaction.weight, true, new int[0]);
    }

    /**
     * {@link #eventOption}, and not right after the animation it begins with (the interaction
     * itself when the pet is at its prop already).
     */
    static PetSpec.Activity eventOption(BackgroundEvents.Interaction interaction, Session s, long now, int previous) {
        if (interaction != null && (interaction.first() == previous || interaction.act == previous)) {
            return null;
        }
        return eventOption(interaction, s, now);
    }

    /**
     * A background event begins. With a prop to go to, the pet first looks at it and walks over
     * (a plan of its own: the interaction comes with the next one) or, if it flies and the prop is
     * far (and did not just fly), flies there; with nothing to go to, or already there, the
     * interaction comes at once.
     */
    private static void startEvent(PetSpec spec, BackgroundEvents.Interaction interaction, Session s, float here,
                                   boolean facing, int previous, long now) {
        s.eventsLeft--;
        s.lastEventAt = now;
        markSeen(s, BackgroundEvents.ANIMATION_EVENT);
        int index = BackgroundEvents.indexOf(interaction);
        float target = targetOf(interaction, here);
        if (spec.smooth()) {
            float ground = ground(s);
            if (spec.flies() && previous != ANIMATION_FLY && Math.abs(target - here) >= MIN_FLIGHT
                && Math.abs(target - here) * ground <= maxFlightPx(spec)) {
                s.now = untilAlarm(spec, smoothFlight(spec, s, here, facing, target, now));
                s.now.event = index;
                s.next = null;
                s.eventNext = index;
                return;
            }
            if (spec.walks() && !atProp(interaction, s, here)) {
                markSeen(s, interaction.notice);
                walkOver(spec, interaction, s, here, facing, true, now);
                return;
            }
            planAct(spec, interaction, s, here, facing, now);
            return;
        }
        int steps = approachSteps(here, target);
        if (steps > 0 && spec.walks()) {
            boolean right = target > here;
            markSeen(s, interaction.notice);
            Segment look = stayFor(spec, interaction.notice, here, right, now, noticeRepeats(spec, interaction.notice));
            Segment walk = new Segment();
            walk.animation = ANIMATION_WALK;
            walk.from = here;
            walk.to = target;
            walk.facingRight = right;
            walk.event = index;
            // Once there it stands in its noticing pose until the interaction begins
            walk.variant = interaction.notice;
            walk.startedAt = look.endsAt;
            walk.endsAt = look.endsAt + Math.max((long) steps * spec.walkStepMs(), minPlanMs(spec) - look.lengthMs());
            s.now = look;
            s.next = walk;
            s.eventNext = index;
            return;
        }
        planAct(spec, interaction, s, here, facing, now);
    }

    /** Steps of a walk over to a prop: at least one unless the pet is there already. */
    static int approachSteps(float from, float to) {
        float distance = Math.abs(to - from);
        return distance < ARRIVED ? 0 : Math.max(1, Math.round(distance / WALK_STEP));
    }

    /** A pet this close to a prop (in ground units) is at it. */
    static final float ARRIVED = 0.01f;

    /**
     * The interaction begins right now, whatever the limits say: for tests and the debug build's
     * receiver that plays any of them on the emulator. The pet must be awake.
     */
    static void startEventNow(PetSpec spec, BackgroundEvents.Interaction interaction, Session s, long now) {
        Segment current = segmentAt(s, now);
        s.eventsLeft = Math.max(1, s.eventsLeft);
        startEvent(spec, interaction, s, positionAt(spec, current, now), facingRightAt(spec, current, now), -1, now);
        if (spec.smooth()) {
            fillToAlarm(spec, s);
        }
    }

    /** Where the pet goes for an interaction: its prop's place; here when it has none. */
    static float targetOf(BackgroundEvents.Interaction interaction, float here) {
        Backdrop backdrop = interaction.prop != null ? Backdrop.forType(interaction.background) : null;
        Backdrop.Prop prop = backdrop != null ? backdrop.prop(interaction.prop) : null;
        return prop != null ? onGround(prop.offset) : here;
    }

    /** Repeats of the look before going over: about {@link BackgroundEvents#NOTICE_MS}, 2–4. */
    private static int noticeRepeats(PetSpec spec, int animation) {
        long loop = loopMs(spec, animation);
        int repeats = (int) ((BackgroundEvents.NOTICE_MS + loop - 1) / loop);
        return Math.max(spec.minRepeats, Math.min(spec.maxRepeats, repeats));
    }

    /**
     * The interaction itself, a plan of its own: the pet's animation while the background does its
     * part (a leaf drifts down to it, the prop changes), at least as long as that takes, then the
     * punchline. Both play 2–4 times.
     */
    private static void planAct(PetSpec spec, BackgroundEvents.Interaction interaction, Session s, float here,
                                boolean facing, long now) {
        markSeen(s, interaction.act);
        markSeen(s, interaction.then);
        long effect = BackgroundEvents.effectMs(interaction, Backdrop.forType(interaction.background));
        long loop = loopMs(spec, interaction.act);
        int repeats;
        if (effect > 0) {
            repeats = (int) Math.max(spec.minRepeats, Math.min(spec.maxRepeats, (effect + loop - 1) / loop));
        } else {
            repeats = repeats(spec, s, false);
        }
        Segment act = stayFor(spec, interaction.act, here, facing, now, repeats);
        if (spec.smooth() && spec.framed(interaction.act)) {
            // Into the pose (onto the pumpkin, into the heap), its loops, and out again
            act.framed = true;
            act.steps = repeats;
            act.endsAt = now + timeline(spec, act).size() * (long) tickMs(spec, act);
        }
        act.event = BackgroundEvents.indexOf(interaction);
        // Which way the leaf sways first, and where above the pet it comes loose
        act.variant = nextInt(s, 4);
        long missing = Math.max(0, minPlanMs(spec) - act.lengthMs());
        long thenLoop = loopMs(spec, interaction.then);
        long cover = (long) nextCopies(act.lengthMs(), Long.MAX_VALUE / 4, framesBound(spec, interaction.then))
            * act.lengthMs() - LATE_MARGIN_MS;
        int fewest = (int) Math.min(spec.maxRepeats, Math.max(spec.minRepeats, (missing + thenLoop - 1) / thenLoop));
        int most = (int) Math.max(fewest, Math.min(spec.maxRepeats, cover / thenLoop));
        Pick then = new Pick(interaction.then, false, fewest + nextInt(s, most - fewest + 1));
        s.now = act;
        s.next = build(spec, s, then, here, facing, act.endsAt);
        if (spec.smooth()) {
            // A pixel pet's plan stays within the widget's budget: a shorter punchline (2 times at
            // least), a longer act (its own loop covers more of the plan, so fewer copies of the
            // punchline), fewer loops of a framed pose while it still lasts as long as the effect,
            // a punchline played once, then fewer loops still if need be
            while (repeatsOf(spec, s.next) > spec.minRepeats && PlanFrames.views(spec, s.now, s.next) > MAX_PLAN_VIEWS) {
                s.next.endsAt -= loopMs(spec, s.next);
            }
            while (!linear(spec, act) && repeatsOf(spec, act) < spec.maxRepeats
                && PlanFrames.views(spec, s.now, s.next) > MAX_PLAN_VIEWS) {
                act.endsAt += loop;
                s.next.startedAt += loop;
                s.next.endsAt += loop;
            }
            shortenFramed(spec, s, act, effect);
            while (repeatsOf(spec, s.next) > 1 && PlanFrames.views(spec, s.now, s.next) > MAX_PLAN_VIEWS) {
                s.next.endsAt -= loopMs(spec, s.next);
            }
            shortenFramed(spec, s, act, 0);
        }
    }

    /**
     * Fewer loops of a framed pose (an interaction's act) while the plan is over the widget's budget,
     * as long as it still lasts {@code atLeastMs}; the punchline follows it.
     */
    private static void shortenFramed(PetSpec spec, Session s, Segment act, long atLeastMs) {
        while (act.framed && act.steps > 1 && PlanFrames.views(spec, s.now, s.next) > MAX_PLAN_VIEWS) {
            long shorter = act.startedAt + (timeline(spec, act).size() - loopFrames(spec, act)) * (long) tickMs(spec, act);
            if (shorter - act.startedAt < atLeastMs) {
                return;
            }
            long punchline = s.next.lengthMs();
            act.steps--;
            act.endsAt = shorter;
            s.next.startedAt = act.endsAt;
            s.next.endsAt = act.endsAt + punchline;
        }
    }

    /** Frames of one loop of a segment's animation as the widget plays it. */
    private static int loopFrames(PetSpec spec, Segment segment) {
        return Math.min(spec.animation(segment.animation).frames.length, spec.maxLoopFrames());
    }

    /** Shortest plan: an update at most this often (PetSpec.minPlanMs; the hand-drawn pets: MIN_PLAN_MS). */
    static long minPlanMs(PetSpec spec) {
        return spec.smooth() ? spec.minPlanMs : MIN_PLAN_MS;
    }

    /** Places a picked activity: a walk gets its leg, anything else stays on the spot. */
    private static Segment build(PetSpec spec, Session s, Pick pick, float here, boolean facing, long start) {
        if (pick.animation == ANIMATION_WALK && spec.walks()) {
            return walk(spec, s, here, start, pick.repeats);
        }
        return stayFor(spec, pick.animation, here, facing, start, pick.repeats);
    }

    /**
     * Repeats of a reaction: 2–4, and as many as a whole plan takes when its loop allows, since it
     * always comes first and a short first one needs many copies of what follows.
     */
    private static int reactionRepeats(PetSpec spec, Session s, int animation) {
        long loop = loopMs(spec, animation);
        int fewest = (int) Math.min(spec.maxRepeats, Math.max(spec.minRepeats, (minPlanMs(spec) + loop - 1) / loop));
        return fewest + nextInt(s, spec.maxRepeats - fewest + 1);
    }

    /**
     * Repeats of a plan's filler: 2–4, enough to reach {@code missingMs} (one for a one-shot), and
     * if possible no longer than {@code coverMs}.
     */
    private static int fillingRepeats(PetSpec spec, Session s, PetSpec.Activity activity, long missingMs,
                                      long coverMs) {
        if (activity.once) {
            return 1;
        }
        long loop = loopMs(spec, activity.animation);
        int fewest = (int) Math.max(spec.minRepeats, (missingMs + loop - 1) / loop);
        int most = (int) Math.max(fewest, Math.min(spec.maxRepeats, coverMs / loop));
        return fewest + nextInt(s, most - fewest + 1);
    }

    /** Most frames an activity puts in one flipper: a walk at most 2 x 7 steps there and back. */
    static int framesBound(PetSpec spec, int animation) {
        if (animation == ANIMATION_WALK && spec.walks()) {
            return 2 * (int) Math.floor(2 * GROUND_LIMIT / WALK_STEP + 1e-4);
        }
        return Math.min(spec.animation(animation).frames.length, spec.maxLoopFrames());
    }

    /** Activities with their own path, drawn as a whole: they always start a plan of their own. */
    static boolean scripted(PetSpec spec, int animation) {
        return animation == ANIMATION_FLY && spec.flies();
    }

    /** The next activity by weight (see {@link #pick}), never the one before. */
    static PetSpec.Activity choose(PetSpec spec, Session s, int previous) {
        return choose(spec, s, previous, null);
    }

    /** Like {@link #choose(PetSpec, Session, int)}, with a background event as one more option (or null). */
    static PetSpec.Activity choose(PetSpec spec, Session s, int previous, PetSpec.Activity event) {
        List<PetSpec.Activity> options = new ArrayList<>();
        for (PetSpec.Activity activity : spec.activities) {
            if (allowed(spec, activity, s, previous, false, 0)) {
                options.add(activity);
            }
        }
        if (event != null) {
            options.add(event);
        }
        PetSpec.Activity chosen = pick(s, options);
        if (chosen != null) {
            return chosen;
        }
        // Nothing allowed (a config with one activity, say): anything but the one before
        for (PetSpec.Activity activity : spec.activities) {
            if (activity.animation != previous) {
                return activity;
            }
        }
        return spec.activities[0];
    }

    /**
     * Whether an activity may come after {@code previous}; a filler (the second one of a plan,
     * {@code filling}) must also be able to last {@code fillMs} and is never a flight.
     */
    static boolean allowed(PetSpec spec, PetSpec.Activity activity, Session s, int previous, boolean filling,
                           long fillMs) {
        if (activity.weight <= 0 || activity.animation == previous || activity.notAfter(previous)) {
            return false;
        }
        if (activity.animation == ANIMATION_FLY && (s.flightsLeft <= 0 || filling)) {
            return false;
        }
        return !filling || longestMs(spec, activity) >= fillMs;
    }

    /** The longest run an activity can have. */
    static long longestMs(PetSpec spec, PetSpec.Activity activity) {
        return (activity.once ? 1L : spec.maxRepeats) * loopMs(spec, activity.animation);
    }

    /** One loop of an animation; for a walk, one cycle of its steps. */
    static long loopMs(PetSpec spec, int animation) {
        return spec.animation(animation).loopMs();
    }

    static long loopMs(PetSpec spec, Segment segment) {
        return loopMs(spec, segment.animation);
    }

    /** How many times an activity plays: once, or minRepeats..maxRepeats. */
    private static int repeats(PetSpec spec, Session s, boolean once) {
        return once ? 1 : spec.minRepeats + nextInt(s, spec.maxRepeats - spec.minRepeats + 1);
    }

    private static Segment stayFor(PetSpec spec, int animation, float here, boolean facing, long start,
                                   int repeats) {
        Segment segment = new Segment();
        segment.animation = animation;
        segment.from = here;
        segment.to = here;
        segment.facingRight = facing;
        segment.startedAt = start;
        segment.endsAt = start + repeats * loopMs(spec, animation);
        return segment;
    }

    /**
     * A walk of {@code cycles} step cycles, back and forth between here and a point at least
     * two steps away; it turns at both ends, so it never runs into the edge.
     */
    private static Segment walk(PetSpec spec, Session s, float here, long start, int cycles) {
        int roomRight = (int) Math.floor((GROUND_LIMIT - here) / WALK_STEP + 1e-4);
        int roomLeft = (int) Math.floor((here + GROUND_LIMIT) / WALK_STEP + 1e-4);
        boolean right;
        if (roomRight < MIN_LEG_STEPS) {
            right = false;
        } else if (roomLeft < MIN_LEG_STEPS) {
            right = true;
        } else {
            right = nextInt(s, 2) == 0;
        }
        int room = right ? roomRight : roomLeft;
        int steps = MIN_LEG_STEPS + nextInt(s, room - MIN_LEG_STEPS + 1);
        Segment segment = new Segment();
        segment.animation = ANIMATION_WALK;
        segment.from = here;
        segment.to = onGround(here + (right ? steps : -steps) * WALK_STEP);
        segment.facingRight = right;
        segment.startedAt = start;
        segment.endsAt = start + cycles * loopMs(spec, ANIMATION_WALK);
        return segment;
    }

    /** Whether the segment is a flight (the widget draws its path). */
    static boolean flying(PetSpec spec, Session s, Segment segment) {
        return s.phase == AWAKE && segment.animation == ANIMATION_FLY && spec.flies();
    }

    /** How long after its planned end the second segment of a plan stays on screen, for a late alarm. */
    static final long LATE_MARGIN_MS = 1_500;
    /** Most frames the copies of a plan's second segment may add to one update. */
    static final int MAX_PAIR_FRAMES = 48;

    /**
     * Copies of a plan's second segment in the widget's outer flipper: it flips every
     * {@code firstMs}, so the copies (running in step) cover the second segment and a late
     * alarm, within {@link #MAX_PAIR_FRAMES}.
     */
    static int nextCopies(long firstMs, long secondMs, int framesPerCopy) {
        long copies = (secondMs + LATE_MARGIN_MS + firstMs - 1) / Math.max(1, firstMs);
        long cap = Math.max(1, MAX_PAIR_FRAMES / Math.max(1, framesPerCopy));
        return (int) Math.max(1, Math.min(copies, cap));
    }

    /** {@code wantedMs} rounded up to whole loops (the doze before sleep). */
    static long wholeLoops(long loopMs, long wantedMs) {
        return Math.max(1, (wantedMs + loopMs - 1) / loopMs) * loopMs;
    }

    /** How many times the animation of a finished segment played (a walk: step cycles). */
    static long repeatsOf(PetSpec spec, Segment segment) {
        if (linear(spec, segment) && segment.animation != ANIMATION_FLY) {
            // A pixel pet's timeline counts its own cycles, not the time it stands still after them
            int frames = segment.animation == ANIMATION_WALK ? spec.animation(ANIMATION_WALK).frames.length : 1;
            return Math.round((float) segment.steps / frames);
        }
        return segment.lengthMs() / loopMs(spec, segment);
    }

    // --- The pixel pets: smooth walks, transitions, plans within the widget's budget --------
    //
    // The hand-drawn cat, pug and rabbit keep everything above exactly as it was. The pixel pets
    // (PetSpec#smooth) walk whole art pixels per frame in step with their paws (PetSpec.Gait), go
    // one way per walk, turn round, start and stop their walks, lie down and get up, and their
    // walks, flights and these transitions are timelines: played once from start to end (never
    // looped, so a late alarm can't bring back their start). A timeline always comes first in a
    // plan; the second activity is always a loop.

    /** Art pixels per ground unit until the widget measured its own (see {@link Session#groundPx}). */
    static final float DEFAULT_GROUND_PX = 80f;
    /** Most frames of one timeline (a walk over to a prop takes more updates when it is longer). */
    static final int MAX_TIMELINE_FRAMES = 56;
    /** Most frames of the pet in one update, and views they make up (as many as the widget had before). */
    static final int MAX_PLAN_FRAMES = 64;
    static final int MAX_PLAN_VIEWS = 133;
    /** How long a pixel pet looks at a prop before it walks over. */
    static final int NOTICE_LOOK_MS = 1_200;
    /** Average art pixels a flight covers sideways per frame in the air, and most frames of a flight. */
    static final int FLIGHT_PACE_PX = 3;
    static final int MAX_FLIGHT_FRAMES = 60;

    /** One frame of a timeline: what it shows, how far along the pet is (art pixels), which way it looks. */
    static final class Entry {
        final int animation;
        final int frame;
        final int px;
        final boolean facingRight;

        Entry(int animation, int frame, int px, boolean facingRight) {
            this.animation = animation;
            this.frame = frame;
            this.px = px;
            this.facingRight = facingRight;
        }
    }

    static float ground(Session s) {
        return s.groundPx > 0 ? s.groundPx : DEFAULT_GROUND_PX;
    }

    /**
     * {@link #plan} for a pixel pet: a first activity (a timeline when it walks or gets up) and,
     * if it is shorter than an update, a second one, always a loop, chosen so that the plan stays
     * within the widget's budget of frames and views ({@link PlanFrames#views}).
     */
    private static void planSmooth(PetSpec spec, Session s, Pick first, PetSpec.Activity activity, int previous,
                                   float here, boolean facing, long now, boolean getsUp) {
        s.next = null;
        markSeen(s, first.animation);
        if (scripted(spec, first.animation)) {
            s.now = untilAlarm(spec, ownFlight(spec, s, here, facing, now));
            return;
        }
        Segment a = buildSmooth(spec, s, first, here, facing, now, getsUp);
        if (a.lengthMs() >= minPlanMs(spec)) {
            s.now = a;
            return;
        }
        long missing = minPlanMs(spec) - a.lengthMs();
        float there = positionAt(spec, a, a.endsAt);
        boolean facingThere = facingRightAt(spec, a, a.endsAt);
        List<PetSpec.Activity> options = new ArrayList<>();
        for (PetSpec.Activity filler : spec.activities) {
            // A walk or a framed activity only where it may go first: a timeline always does
            boolean leads = filler.animation == ANIMATION_WALK && spec.walks() || spec.framed(filler.animation);
            if (leads && (linear(spec, a) || !swappable(filler, activity, previous))) {
                continue;
            }
            if (allowed(spec, filler, s, first.animation, true, missing)) {
                options.add(filler);
            }
        }
        while (!options.isEmpty()) {
            PetSpec.Activity filler = pick(s, options);
            options.remove(filler);
            long loop = loopMs(spec, filler.animation);
            int fewest = filler.once ? 1 : (int) Math.max(spec.minRepeats, (missing + loop - 1) / loop);
            int most = filler.once ? 1 : spec.maxRepeats;
            if (fewest > most) {
                continue;
            }
            int repeats = fewest + nextInt(s, most - fewest + 1);
            // The longer of two loops goes first when the rules allow it
            boolean leads = filler.animation == ANIMATION_WALK && spec.walks() || spec.framed(filler.animation);
            boolean swap = !linear(spec, a) && swappable(filler, activity, previous) && repeats * loop > a.lengthMs();
            for (int r = repeats; r >= fewest; r--) {
                Segment x;
                Segment y;
                if (leads) {
                    x = buildSmooth(spec, s, new Pick(filler.animation, filler.once, r), here, facing, now, false);
                    y = stayFor(spec, first.animation, positionAt(spec, x, x.endsAt), facingRightAt(spec, x, x.endsAt),
                        x.endsAt, first.repeats);
                } else if (swap) {
                    x = stayFor(spec, filler.animation, here, facing, now, r);
                    y = stayFor(spec, first.animation, here, facing, x.endsAt, first.repeats);
                } else {
                    x = a;
                    y = stayFor(spec, filler.animation, there, facingThere, a.endsAt, r);
                }
                if (PlanFrames.views(spec, x, y) <= MAX_PLAN_VIEWS) {
                    markSeen(s, filler.animation);
                    s.now = x;
                    s.next = y;
                    return;
                }
            }
        }
        // Nothing fits after it: a loop takes its longest run; a timeline stands still at its end
        if (!linear(spec, a)) {
            first.repeats = first.once ? 1 : spec.maxRepeats;
            a = buildSmooth(spec, s, first, here, facing, now, false);
        }
        s.now = untilAlarm(spec, a);
    }

    /**
     * A pixel pet's plan lasts at least until the soonest alarm: its last loop plays more times
     * while the rules and the budget allow, then the plan simply lasts longer (the loop plays on,
     * a timeline keeps still at its end).
     */
    private static void fillToAlarm(PetSpec spec, Session s) {
        Segment last = s.next != null ? s.next : s.now;
        long until = s.now.startedAt + minPlanMs(spec);
        if (last.endsAt >= until) {
            return;
        }
        if (!linear(spec, last)) {
            long loop = loopMs(spec, last);
            while (last.endsAt < until && repeatsOf(spec, last) < spec.maxRepeats) {
                last.endsAt += loop;
                if (s.next != null && PlanFrames.views(spec, s.now, s.next) > MAX_PLAN_VIEWS) {
                    last.endsAt -= loop;
                    break;
                }
            }
        }
        last.endsAt = Math.max(last.endsAt, until);
    }

    /**
     * A timeline alone in its plan lasts at least until the soonest alarm: the pet keeps still at
     * its end in the meantime (PlanFrames).
     */
    private static Segment untilAlarm(PetSpec spec, Segment segment) {
        if (linear(spec, segment)) {
            segment.endsAt = Math.max(segment.endsAt, segment.startedAt + minPlanMs(spec));
        }
        return segment;
    }

    /**
     * When the frames of a pixel pet's segment are over: a timeline's (or flight's) last frame, a
     * loop's last whole loop. The segment may go on still after it, in its last pose (a loop's
     * first frame, the pose every loop begins and ends with).
     */
    static long framesEnd(PetSpec spec, Segment segment) {
        if (!linear(spec, segment)) {
            return segment.startedAt + repeatsOf(spec, segment) * loopMs(spec, segment);
        }
        if (segment.animation == ANIMATION_FLY && spec.flies()) {
            return segment.startedAt + (long) smoothFlightFrames(spec, segment) * spec.animation(ANIMATION_FLY).intervalMs;
        }
        return segment.startedAt + timeline(spec, segment).size() * (long) tickMs(spec, segment);
    }

    /** A picked activity of a pixel pet: a walk, the reaction after getting up, or a loop. */
    private static Segment buildSmooth(PetSpec spec, Session s, Pick pick, float here, boolean facing, long start,
                                       boolean getsUp) {
        if (pick.animation == ANIMATION_WALK && spec.walks()) {
            return smoothWalk(spec, s, here, facing, start, pick.repeats);
        }
        Segment segment = stayFor(spec, pick.animation, here, facing, start, pick.repeats);
        if (getsUp) {
            segment.wakes = true;
            segment.steps = pick.repeats;
            segment.endsAt = start + timeline(spec, segment).size() * (long) tickMs(spec, segment);
        } else if (spec.framed(pick.animation)) {
            // Into it, its loops, out again (lies down to gnaw the bone, gets up after); as many
            // loops as one timeline holds
            segment.framed = true;
            segment.steps = pick.repeats;
            while (segment.steps > 1 && timeline(spec, segment).size() > MAX_TIMELINE_FRAMES) {
                segment.steps--;
            }
            segment.endsAt = start + timeline(spec, segment).size() * (long) tickMs(spec, segment);
        }
        return segment;
    }

    /**
     * A walk of {@code cycles} step cycles one way: on the way the pet looks if there is room,
     * now and then (or at the edge) the other way after turning round; with no room for all the
     * cycles it walks as far as there is.
     */
    private static Segment smoothWalk(PetSpec spec, Session s, float here, boolean facing, long start, int cycles) {
        float ground = ground(s);
        int walkFrames = spec.animation(ANIMATION_WALK).frames.length;
        int want = spec.gait.pixelsAfter(cycles * walkFrames);
        int roomRight = (int) Math.floor((GROUND_LIMIT - here) * ground + 1e-3);
        int roomLeft = (int) Math.floor((here + GROUND_LIMIT) * ground + 1e-3);
        int ahead = facing ? roomRight : roomLeft;
        int behind = facing ? roomLeft : roomRight;
        boolean right;
        if (ahead >= want && (behind < want || nextInt(s, 3) != 0)) {
            right = facing;
        } else if (behind >= want) {
            right = !facing;
        } else {
            right = roomRight >= roomLeft;
        }
        int room = right ? roomRight : roomLeft;
        int steps = cycles * walkFrames;
        while (steps > 1 && spec.gait.pixelsAfter(steps) > room) {
            steps--;
        }
        Segment walk = new Segment();
        walk.animation = ANIMATION_WALK;
        walk.from = here;
        walk.facingRight = right;
        walk.turns = right != facing;
        walk.ground = ground;
        walk.steps = steps;
        int distance = Math.min(room, spec.gait.pixelsAfter(steps));
        walk.to = onGround(here + (right ? distance : -distance) / ground);
        walk.startedAt = start;
        walk.endsAt = start + timeline(spec, walk).size() * (long) spec.walkStepMs();
        return walk;
    }

    /** Whether the pet stands at the prop of an interaction (or it has none). */
    static boolean atProp(BackgroundEvents.Interaction interaction, Session s, float here) {
        return Math.abs(targetOf(interaction, here) - here) * ground(s) < 0.5f;
    }

    /**
     * The walk over to an interaction's prop, a plan of its own: it turns to it, looks at it a
     * moment ({@code look}), walks over and, once there, looks at it until the interaction comes
     * with the next plan. A walk longer than a timeline ends on the way: the next plan goes on.
     */
    private static void walkOver(PetSpec spec, BackgroundEvents.Interaction interaction, Session s, float here,
                                 boolean facing, boolean look, long now) {
        int index = BackgroundEvents.indexOf(interaction);
        float target = targetOf(interaction, here);
        float ground = ground(s);
        boolean right = target > here;
        Segment walk = new Segment();
        walk.animation = ANIMATION_WALK;
        walk.event = index;
        walk.variant = interaction.notice;
        walk.from = here;
        walk.facingRight = right;
        walk.turns = right != facing;
        walk.ground = ground;
        walk.look = look ? Math.max(1, Math.round((float) NOTICE_LOOK_MS / spec.walkStepMs())) : 0;
        walk.startedAt = now;
        int distance = Math.round(Math.abs(target - here) * ground);
        int steps = stepsFor(spec.gait, distance);
        int room = Math.max(1, MAX_TIMELINE_FRAMES - timeline(spec, walk).size());
        boolean arrives = steps <= room;
        walk.steps = Math.min(steps, room);
        walk.to = arrives ? target : onGround(here + (right ? 1 : -1) * spec.gait.pixelsAfter(walk.steps) / ground);
        walk.endsAt = now + timeline(spec, walk).size() * (long) spec.walkStepMs();
        s.now = walk;
        s.next = null;
        s.eventNext = index;
        if (arrives && walk.lengthMs() < minPlanMs(spec)) {
            // There it looks at the prop until the interaction begins
            long loop = loopMs(spec, interaction.notice);
            int repeats = (int) Math.max(1, Math.min(spec.maxRepeats, (minPlanMs(spec) - walk.lengthMs() + loop - 1) / loop));
            for (; repeats >= 1; repeats--) {
                Segment wait = stayFor(spec, interaction.notice, target, right, walk.endsAt, repeats);
                if (PlanFrames.views(spec, walk, wait) <= MAX_PLAN_VIEWS) {
                    s.next = wait;
                    return;
                }
            }
        }
        untilAlarm(spec, walk);
    }

    /** Fewest walk frames that cover {@code distance} art pixels. */
    static int stepsFor(PetSpec.Gait gait, int distance) {
        int steps = 0;
        while (gait.pixelsAfter(steps) < distance && steps < 10_000) {
            steps++;
        }
        return Math.max(1, steps);
    }

    /** The parrot's own flight: to a spot at least {@link #MIN_FLIGHT} away that one flight reaches. */
    private static Segment ownFlight(PetSpec spec, Session s, float here, boolean facing, long now) {
        float reach = maxFlightPx(spec) / ground(s);
        float roomRight = Math.min(GROUND_LIMIT - here, reach);
        float roomLeft = Math.min(here + GROUND_LIMIT, reach);
        boolean right;
        if (roomRight < MIN_FLIGHT) {
            right = roomLeft < roomRight;
        } else if (roomLeft < MIN_FLIGHT) {
            right = true;
        } else {
            right = nextInt(s, 2) == 0;
        }
        float room = right ? roomRight : roomLeft;
        float distance = room <= MIN_FLIGHT ? room : MIN_FLIGHT + (room - MIN_FLIGHT) * nextInt(s, 101) / 100f;
        s.flightsLeft--;
        return smoothFlight(spec, s, here, facing, here + (right ? distance : -distance), now);
    }

    /**
     * A flight to {@code to}: turning round first if it flies the other way, the take-off, as many
     * frames in the air as the way takes at {@link #FLIGHT_PACE_PX}, the landing (FlightPath draws
     * the path for the widget's size). It lands looking the way it flew.
     */
    private static Segment smoothFlight(PetSpec spec, Session s, float here, boolean facing, float to, long now) {
        Segment flight = new Segment();
        flight.animation = ANIMATION_FLY;
        flight.from = here;
        flight.to = onGround(to);
        flight.facingRight = flight.to > here;
        flight.turns = flight.facingRight != facing;
        flight.ground = ground(s);
        flight.variant = nextInt(s, FlightPath.STYLES);
        int distance = Math.round(Math.abs(flight.to - here) * flight.ground);
        flight.steps = Math.max(FlightPath.FLY_FRAMES,
            Math.min(maxAirFrames(spec), (distance + FLIGHT_PACE_PX - 1) / FLIGHT_PACE_PX));
        flight.startedAt = now;
        flight.endsAt = now + (long) smoothFlightFrames(spec, flight) * spec.animation(ANIMATION_FLY).intervalMs;
        return flight;
    }

    /** All frames of a pixel pet's flight: turning round, take-off, the air, landing. */
    static int smoothFlightFrames(PetSpec spec, Segment flight) {
        return timeline(spec, flight).size() + FlightPath.TAKEOFF_FRAMES + flight.steps + FlightPath.LAND_FRAMES;
    }

    private static int maxAirFrames(PetSpec spec) {
        return MAX_FLIGHT_FRAMES - FlightPath.TAKEOFF_FRAMES - FlightPath.LAND_FRAMES
            - turnFrames(spec, spec.animation(ANIMATION_FLY).intervalMs);
    }

    /** The farthest one flight goes, in art pixels. */
    static int maxFlightPx(PetSpec spec) {
        return maxAirFrames(spec) * FLIGHT_PACE_PX;
    }

    /** The time one entry of a segment's timeline (or one frame of its loop) stays on screen. */
    static int tickMs(PetSpec spec, Segment segment) {
        if (segment.animation == ANIMATION_WALK && spec.walks()) {
            return spec.walkStepMs();
        }
        return spec.animation(segment.animation).intervalMs;
    }

    /** Whether a pixel pet's segment is a timeline, played once, rather than a loop. */
    static boolean linear(PetSpec spec, Segment segment) {
        return spec.smooth() && (segment.animation == ANIMATION_WALK && spec.walks()
            || segment.animation == ANIMATION_FLY && spec.flies() || segment.wakes || segment.lies || segment.framed);
    }

    /**
     * The frames of a pixel pet's timeline, one per tick: a walk (turning round, looking at a
     * prop, the first step, the steps, the last one), getting up and reacting, lying down and
     * sleeping. A flight: only its turn; FlightPath adds the rest.
     */
    static List<Entry> timeline(PetSpec spec, Segment segment) {
        List<Entry> out = new ArrayList<>();
        int tick = tickMs(spec, segment);
        boolean right = segment.facingRight;
        if (segment.animation == ANIMATION_FLY && spec.flies()) {
            if (segment.turns) {
                addTurn(out, spec, tick, !right);
            }
            return out;
        }
        if (segment.animation == ANIMATION_WALK && spec.walks()) {
            if (segment.turns) {
                addTurn(out, spec, tick, !right);
            }
            if (segment.look > 0) {
                int[] look = spec.animation(segment.variant).frames;
                for (int j = 0; j < segment.look; j++) {
                    out.add(new Entry(segment.variant, j % look.length, 0, right));
                }
            }
            addTransition(out, spec, ANIMATION_WALK_START, tick, 0, right);
            int distance = distancePx(segment);
            int walkFrames = spec.animation(ANIMATION_WALK).frames.length;
            for (int k = 0; k < segment.steps; k++) {
                out.add(new Entry(ANIMATION_WALK, k % walkFrames, Math.min(spec.gait.pixelsAfter(k + 1), distance),
                    right));
            }
            addTransition(out, spec, ANIMATION_WALK_STOP, tick, distance, right);
            return out;
        }
        if (segment.wakes) {
            addTransition(out, spec, ANIMATION_GET_UP, tick, 0, right);
        } else if (segment.lies) {
            addTransition(out, spec, ANIMATION_LIE_DOWN, tick, 0, right);
        } else if (segment.framed) {
            addTransition(out, spec, PetSpec.ENTER + segment.animation, tick, 0, right);
        }
        int count = Math.min(spec.animation(segment.animation).frames.length, spec.maxLoopFrames());
        for (int r = 0; r < segment.steps; r++) {
            for (int j = 0; j < count; j++) {
                out.add(new Entry(segment.animation, j, 0, right));
            }
        }
        if (segment.framed) {
            addTransition(out, spec, PetSpec.LEAVE + segment.animation, tick, 0, right);
        }
        return out;
    }

    /**
     * Turning round on the spot: the turn's frames towards the viewer looking the old way, then
     * back the other way; each held as long as its own frame time takes at this tick.
     */
    private static void addTurn(List<Entry> out, PetSpec spec, int tick, boolean fromRight) {
        if (!spec.hasAnimation(ANIMATION_TURN)) {
            return;
        }
        int n = spec.animation(ANIMATION_TURN).frames.length;
        int hold = holdTicks(spec, ANIMATION_TURN, tick);
        for (int i = 0; i < n; i++) {
            for (int h = 0; h < hold; h++) {
                out.add(new Entry(ANIMATION_TURN, i, 0, fromRight));
            }
        }
        for (int i = n - 2; i >= 0; i--) {
            for (int h = 0; h < hold; h++) {
                out.add(new Entry(ANIMATION_TURN, i, 0, !fromRight));
            }
        }
    }

    /** Frames of turning round at this tick (0 without turn frames). */
    static int turnFrames(PetSpec spec, int tick) {
        if (!spec.hasAnimation(ANIMATION_TURN)) {
            return 0;
        }
        return (2 * spec.animation(ANIMATION_TURN).frames.length - 1) * holdTicks(spec, ANIMATION_TURN, tick);
    }

    private static void addTransition(List<Entry> out, PetSpec spec, int animation, int tick, int px, boolean right) {
        if (!spec.hasAnimation(animation)) {
            return;
        }
        int[] frames = spec.animation(animation).frames;
        int hold = holdTicks(spec, animation, tick);
        for (int i = 0; i < frames.length; i++) {
            for (int h = 0; h < hold; h++) {
                out.add(new Entry(animation, i, px, right));
            }
        }
    }

    /** Ticks one frame of a transition stays on screen: its own frame time, at least one tick. */
    private static int holdTicks(PetSpec spec, int animation, int tick) {
        return Math.max(1, Math.round((float) spec.animation(animation).intervalMs / tick));
    }

    /** Art pixels between the two ends of a walk. */
    static int distancePx(Segment segment) {
        return Math.round(Math.abs(segment.to - segment.from) * segment.ground);
    }

    /** The timeline's entry on screen at {@code now} (the last one once it is over). */
    static Entry entryAt(PetSpec spec, Segment segment, long now) {
        List<Entry> timeline = timeline(spec, segment);
        if (timeline.isEmpty()) {
            return null;
        }
        long index = Math.max(0, now - segment.startedAt) / tickMs(spec, segment);
        return timeline.get((int) Math.min(index, timeline.size() - 1));
    }

    /** Where a pixel pet is: along its walk at the entry on screen, where a flight lands, else where it stands. */
    private static float smoothPositionAt(PetSpec spec, Segment segment, long now) {
        if (segment.animation == ANIMATION_FLY && spec.flies()) {
            return onGround(segment.to);
        }
        if (segment.animation == ANIMATION_WALK && spec.walks()) {
            Entry entry = entryAt(spec, segment, now);
            int distance = distancePx(segment);
            if (entry == null || entry.px >= distance || segment.ground <= 0) {
                return onGround(entry == null || entry.px == 0 ? segment.from : segment.to);
            }
            return onGround(segment.from + (segment.to > segment.from ? 1 : -1) * entry.px / segment.ground);
        }
        return onGround(segment.from);
    }

    private static boolean smoothFacingAt(PetSpec spec, Segment segment, long now) {
        if (segment.animation == ANIMATION_WALK && spec.walks()) {
            Entry entry = entryAt(spec, segment, now);
            return entry != null ? entry.facingRight : segment.facingRight;
        }
        return segment.facingRight;
    }

    // --- Where the pet is ------------------------------------------------------------------

    /**
     * Whether a walk goes over to a background event's prop: out along its leg once, then the pet
     * stands there (in the pose {@link Segment#variant}) instead of walking back.
     */
    static boolean arrives(Segment segment) {
        return segment.animation == ANIMATION_WALK && segment.event >= 0;
    }

    /** Steps in the walk leg of a hand-drawn pet; 0 when the segment is not such a walk. */
    static int legSteps(PetSpec spec, Segment segment) {
        if (segment.animation != ANIMATION_WALK || !spec.walks() || spec.smooth()) {
            return 0;
        }
        if (arrives(segment)) {
            return approachSteps(segment.from, segment.to);
        }
        return Math.round(Math.abs(segment.to - segment.from) / WALK_STEP);
    }

    /** Index of the walk step shown at {@code now}, counted from the start of the segment. */
    static long stepAt(PetSpec spec, Segment segment, long now) {
        return Math.max(0, now - segment.startedAt) / spec.walkStepMs();
    }

    /**
     * Ground position at walk step {@code step}: out along the leg, then back, and again. A flying
     * pet counts as being where it will land.
     */
    static float positionAtStep(PetSpec spec, Segment segment, long step) {
        if (segment.animation == ANIMATION_FLY && spec.flies()) {
            return onGround(segment.to);
        }
        int n = legSteps(spec, segment);
        if (n == 0) {
            return onGround(segment.from);
        }
        int k = (int) Math.floorMod(step, 2L * n);
        int along = arrives(segment) ? (int) Math.max(0, Math.min(step, n)) : k <= n ? k : 2 * n - k;
        return onGround(segment.from + (segment.to - segment.from) * along / n);
    }

    /** Keeps a position on the ground (rounding, or an offset saved by an older version). */
    static float onGround(float position) {
        return Math.max(-GROUND_LIMIT, Math.min(GROUND_LIMIT, position));
    }

    /**
     * Which way the pet looks at walk step {@code step}: the way it is going, and on arriving at
     * either end the way it came, until it turns with the next step.
     */
    static boolean facingRightAtStep(PetSpec spec, Segment segment, long step) {
        int n = legSteps(spec, segment);
        if (n == 0 || arrives(segment)) {
            return segment.facingRight;
        }
        long k = Math.floorMod(step, 2L * n);
        boolean out = k == 0 ? step == 0 : k <= n;
        return out == (segment.to > segment.from);
    }

    static float positionAt(PetSpec spec, Segment segment, long now) {
        if (spec.smooth()) {
            return smoothPositionAt(spec, segment, now);
        }
        return positionAtStep(spec, segment, stepAt(spec, segment, now));
    }

    static boolean facingRightAt(PetSpec spec, Segment segment, long now) {
        if (spec.smooth()) {
            return smoothFacingAt(spec, segment, now);
        }
        return facingRightAtStep(spec, segment, stepAt(spec, segment, now));
    }

    /** Where the pet is at {@code now}, in whichever segment is on screen. */
    static float positionAt(PetSpec spec, Session s, long now) {
        return positionAt(spec, segmentAt(s, now), now);
    }

    // --- Random numbers --------------------------------------------------------------------

    /** 0..bound-1 from the session's generator (SplitMix64). */
    static int nextInt(Session s, int bound) {
        s.random += 0x9E3779B97F4A7C15L;
        long z = s.random;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z ^= z >>> 31;
        return (int) Long.remainderUnsigned(z, bound);
    }
}
