package com.example.pixelpetwidget;

import static com.example.pixelpetwidget.PetBehavior.*;
import static com.example.pixelpetwidget.PixelCatWidget.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PetBehaviorTest {

    private static final long T0 = 1_700_000_000_000L;

    /** One awake spell from a tap until the widget freezes, with every alarm exactly on time. */
    private static final class Life {
        /** Awake segments in the order they play (the reaction first). */
        final List<Segment> segments = new ArrayList<>();
        /** The plans: when each update came, and its segments. */
        final List<Long> updates = new ArrayList<>();
        final List<Segment[]> plans = new ArrayList<>();
        Segment doze;
        Session end;

        void record(Session s, long at) {
            updates.add(at);
            plans.add(new Segment[] {s.now, s.next});
            if (s.phase == AWAKE) {
                segments.add(s.now);
                if (s.next != null) {
                    segments.add(s.next);
                }
            } else if (s.phase == DOZING) {
                doze = s.now;
            }
        }
    }

    private static Life live(PetSpec spec, long seed) {
        Life life = new Life();
        Session s = new Session();
        tap(spec, s, T0, seed);
        life.record(s, T0);
        for (int guard = 0; s.phase != ASLEEP; guard++) {
            assertTrue("never falls asleep", guard < 200);
            long at = planEnd(s);
            assertTrue(advance(spec, s, at));
            if (s.phase != ASLEEP) {
                life.record(s, at);
            }
        }
        life.end = s;
        return life;
    }

    private static String describe(Life life) {
        StringBuilder out = new StringBuilder();
        for (Segment segment : life.segments) {
            out.append(segment.animation).append('@').append(segment.startedAt).append('-').append(segment.endsAt)
                .append(' ').append(segment.from).append('>').append(segment.to).append('/').append(segment.variant)
                .append(';');
        }
        return out.toString();
    }

    private static boolean once(PetSpec spec, int animation) {
        for (PetSpec.Activity activity : spec.activities) {
            if (activity.animation == animation) {
                return activity.once;
            }
        }
        return false;
    }

    // --- The repeats rule -------------------------------------------------------------------

    @Test
    public void everyActivityPlaysTwoToFourTimesAndOneShotsOnce() {
        for (int petType : PetSpec.allPetTypes()) {
            PetSpec spec = PetSpec.forType(petType);
            for (long seed = 0; seed < 300; seed++) {
                for (Segment segment : live(spec, seed).segments) {
                    if (segment.animation == ANIMATION_FLY) {
                        continue;     // a flight is checked with the flights
                    }
                    long loop = loopMs(spec, segment);
                    if (!linear(spec, segment)) {
                        // (a pixel pet's timelines count their own loops: walks, getting up, framed poses)
                        assertEquals("pet " + petType + " plays whole loops", 0, segment.lengthMs() % loop);
                    }
                    long repeats = repeatsOf(spec, segment);
                    if (once(spec, segment.animation)) {
                        assertEquals("pet " + petType + " one-shot " + segment.animation, 1, repeats);
                    } else {
                        assertTrue("pet " + petType + " animation " + segment.animation + " played " + repeats,
                            repeats >= PetSpec.MIN_REPEATS && repeats <= PetSpec.MAX_REPEATS);
                    }
                }
            }
        }
    }

    @Test
    public void neverTheSameActivityTwiceInARow() {
        for (int petType : PetSpec.allPetTypes()) {
            PetSpec spec = PetSpec.forType(petType);
            for (long seed = 0; seed < 300; seed++) {
                List<Segment> segments = live(spec, seed).segments;
                for (int i = 1; i < segments.size(); i++) {
                    assertNotEquals("pet " + petType + " seed " + seed + " at " + i,
                        segments.get(i - 1).animation, segments.get(i).animation);
                }
            }
        }
    }

    @Test
    public void segmentsFollowEachOtherWithoutGaps() {
        for (int petType : PetSpec.allPetTypes()) {
            PetSpec spec = PetSpec.forType(petType);
            for (long seed = 0; seed < 100; seed++) {
                Life life = live(spec, seed);
                for (int i = 1; i < life.segments.size(); i++) {
                    assertEquals(life.segments.get(i - 1).endsAt, life.segments.get(i).startedAt);
                }
            }
        }
    }

    @Test
    public void everyUpdateLastsAtLeastTheShortestAlarm() {
        for (int petType : PetSpec.allPetTypes()) {
            PetSpec spec = PetSpec.forType(petType);
            for (long seed = 0; seed < 300; seed++) {
                Life life = live(spec, seed);
                for (int i = 0; i < life.plans.size(); i++) {
                    Segment[] plan = life.plans.get(i);
                    if (plan[0] == life.doze) {
                        continue;
                    }
                    long end = plan[1] != null ? plan[1].endsAt : plan[0].endsAt;
                    assertTrue("pet " + petType + " seed " + seed + ": an update of " + (end - life.updates.get(i)),
                        end - life.updates.get(i) >= minPlanMs(spec));
                }
            }
        }
    }

    @Test
    public void theCopiesOfASecondActivityCoverIt() {
        // The hand-drawn pets (the pixel pets' plans: SmoothMotionTest)
        for (int petType : new int[] {PET_CAT, PET_DOG, PET_RABBIT}) {
            PetSpec spec = PetSpec.forType(petType);
            for (long seed = 0; seed < 300; seed++) {
                for (Segment[] plan : live(spec, seed).plans) {
                    Segment first = plan[0];
                    Segment second = plan[1];
                    if (second == null) {
                        continue;
                    }
                    int frames = PlanFrames.legacyFrameCount(spec, second);
                    int copies = nextCopies(first.lengthMs(), second.lengthMs(), frames);
                    assertTrue("pet " + petType + " seed " + seed + ": " + copies + " copies of " + frames,
                        copies * frames <= MAX_PAIR_FRAMES);
                    // The outer flipper never comes back to the first one while the alarm is on time
                    assertTrue("pet " + petType + " seed " + seed + ": " + first.lengthMs() + " x " + copies
                        + " < " + second.lengthMs(), first.lengthMs() * copies >= second.lengthMs());
                }
            }
        }
    }

    @Test
    public void everyPetShowsVariety() {
        for (int petType : PetSpec.allPetTypes()) {
            PetSpec spec = PetSpec.forType(petType);
            Set<Integer> available = new HashSet<>();
            available.add(spec.wakeAnimation);
            for (PetSpec.Activity activity : spec.activities) {
                available.add(activity.animation);
            }
            int wanted = Math.min(4, available.size());
            for (long seed = 0; seed < 300; seed++) {
                Set<Integer> seen = new HashSet<>();
                for (Segment segment : live(spec, seed).segments) {
                    if (segment.startedAt < T0 + PetSpec.AWAKE_MS) {
                        seen.add(segment.animation);
                    }
                }
                assertTrue("pet " + petType + " seed " + seed + " did only " + seen, seen.size() >= wanted);
            }
        }
    }

    @Test
    public void forbiddenTransitionsNeverHappen() {
        for (int petType : PetSpec.allPetTypes()) {
            PetSpec spec = PetSpec.forType(petType);
            for (long seed = 0; seed < 300; seed++) {
                List<Segment> segments = live(spec, seed).segments;
                for (int i = 1; i < segments.size(); i++) {
                    PetSpec.Activity activity = find(spec, segments.get(i).animation);
                    if (activity != null) {
                        assertFalse("pet " + petType, activity.notAfter(segments.get(i - 1).animation));
                    }
                }
            }
        }
    }

    // --- Determinism ------------------------------------------------------------------------

    @Test
    public void theSameSeedGivesTheSameLife() {
        for (int petType : PetSpec.allPetTypes()) {
            PetSpec spec = PetSpec.forType(petType);
            assertEquals(describe(live(spec, 42)), describe(live(spec, 42)));
        }
    }

    @Test
    public void otherSeedsGiveOtherLives() {
        PetSpec panda = PetSpec.forType(PET_PANDA);
        assertNotEquals(describe(live(panda, 1)), describe(live(panda, 2)));
    }

    // --- Life cycle -------------------------------------------------------------------------

    @Test
    public void aTapWakesASleepingPetWithItsReaction() {
        for (int petType : PetSpec.allPetTypes()) {
            PetSpec spec = PetSpec.forType(petType);
            Session s = new Session();
            assertFalse(isAwake(s, T0));
            assertFalse("a sleeping pet has nothing to do", advance(spec, s, T0 + 999_999));
            assertEquals(Long.MAX_VALUE, nextTurn(s));
            tap(spec, s, T0, 7);
            assertEquals(AWAKE, s.phase);
            assertTrue(isAwake(s, T0));
            assertEquals(spec.wakeAnimation, s.now.animation);
            assertEquals(T0, s.now.startedAt);
            assertEquals(T0 + spec.awakeMs, s.awakeUntil);
            assertTrue(planEnd(s) - T0 >= minPlanMs(spec));
        }
    }

    @Test
    public void aTapOnAnAwakePetCheersItUpAndKeepsItAwake() {
        PetSpec panda = PetSpec.forType(PET_PANDA);
        Session s = new Session();
        tap(panda, s, T0, 7);
        advance(panda, s, planEnd(s));
        long later = planEnd(s) - 1_000;
        long random = s.random;
        tap(panda, s, later, 99);
        assertEquals(ANIMATION_HAPPY, s.now.animation);
        assertEquals(later, s.now.startedAt);
        assertEquals(later + PetSpec.AWAKE_MS, s.awakeUntil);
        assertTrue("the life goes on with the same generator", random != s.random);
        // The cat washes when it wakes and sits and looks at you on a tap
        PetSpec cat = PetSpec.forType(PET_CAT);
        Session c = new Session();
        tap(cat, c, T0, 1);
        assertEquals(ANIMATION_LICK, c.now.animation);
        tap(cat, c, T0 + 1_000, 1);
        assertEquals(ANIMATION_SEAT, c.now.animation);
    }

    @Test
    public void everyPetFallsAsleepOnTime() {
        for (int petType : PetSpec.allPetTypes()) {
            PetSpec spec = PetSpec.forType(petType);
            for (long seed = 0; seed < 50; seed++) {
                Life life = live(spec, seed);
                Segment doze = life.doze;
                assertNotNull(doze);
                assertEquals(ANIMATION_SLEEP, doze.animation);
                assertTrue(doze.startedAt >= T0 + spec.awakeMs);
                // The last plan that started awake runs out at most ~20 s later
                assertTrue("pet " + petType, doze.startedAt <= T0 + spec.awakeMs + 20_000);
                assertTrue(doze.lengthMs() >= DOZE_MS);
                assertEquals(ASLEEP, life.end.phase);
                assertNull(life.end.next);
                assertEquals(doze.from, life.end.now.from, 0f);
                assertEquals(Long.MAX_VALUE, nextTurn(life.end));
            }
        }
    }

    @Test
    public void nothingHappensBeforeItsTurn() {
        PetSpec hamster = PetSpec.forType(PET_HAMSTER);
        Session s = new Session();
        tap(hamster, s, T0, 3);
        long end = planEnd(s);
        Segment now = s.now;
        assertFalse(advance(hamster, s, end - EARLY_MS - 1));
        assertTrue(now == s.now);
        // Just before the turn it moves on together with another widget's alarm
        assertTrue(advance(hamster, s, end - EARLY_MS));
    }

    @Test
    public void lateAlarmsOnlyMakeTheLastActivityLonger() {
        PetSpec dog = PetSpec.forType(PET_DOG);
        Session s = new Session();
        tap(dog, s, T0, 5);
        for (int i = 0; i < 60 && s.phase != ASLEEP; i++) {
            long late = planEnd(s) + 3_000;
            Segment last = s.next != null ? s.next : s.now;
            float before = positionAt(dog, last, late);
            advance(dog, s, late);
            // The next plan starts where the pet is at that moment, however late
            assertEquals(before, s.now.from, 1e-5f);
            if (s.phase == AWAKE) {
                assertEquals(late, s.now.startedAt);
            }
        }
        assertEquals(ASLEEP, s.phase);
    }

    @Test
    public void aSessionThatMissedItsAlarmsIsAsleep() {
        PetSpec parrot = PetSpec.forType(PET_PARROT);
        Session s = new Session();
        tap(parrot, s, T0, 5);
        long hourLater = T0 + 3_600_000;
        assertFalse(isAwake(s, hourLater));
        assertTrue(advance(parrot, s, hourLater));
        assertEquals(ASLEEP, s.phase);
        tap(parrot, s, hourLater, 6);
        assertEquals(parrot.wakeAnimation, s.now.animation);
    }

    @Test
    public void sleepNowStopsWhereThePetIs() {
        PetSpec cat = PetSpec.forType(PET_CAT);
        for (long seed = 0; seed < 50; seed++) {
            Session s = new Session();
            tap(cat, s, T0, seed);
            for (int i = 0; i < 30 && s.phase == AWAKE; i++) {
                Segment walk = s.next != null && s.next.animation == ANIMATION_WALK ? s.next
                    : s.now.animation == ANIMATION_WALK ? s.now : null;
                if (walk != null && legSteps(cat, walk) > 0) {
                    long mid = walk.startedAt + 3 * cat.walkStepMs() + 10;
                    float there = positionAt(cat, s, mid);
                    sleepNow(cat, s, mid);
                    assertEquals(ASLEEP, s.phase);
                    assertEquals(there, s.now.from, 0f);
                    assertEquals(there, positionAt(cat, s, mid + 99_999), 0f);
                    return;
                }
                advance(cat, s, planEnd(s));
            }
        }
        throw new AssertionError("the cat never walked");
    }

    // --- Character --------------------------------------------------------------------------

    @Test
    public void weightsShowTheCharacters() {
        Map<Integer, Integer> panda = shares(PET_PANDA);
        assertTrue("the panda stands and eats", panda.get(ANIMATION_IDLE) + panda.get(ANIMATION_EAT)
            > 3 * panda.get(ANIMATION_WALK));
        Map<Integer, Integer> hamster = shares(PET_HAMSTER);
        for (int animation : hamster.keySet()) {
            assertTrue("the hamster runs the most", hamster.get(ANIMATION_WALK) >= hamster.get(animation));
        }
        assertTrue("the capybara takes its time", averageLength(PET_CAPYBARA) > 1.5 * averageLength(PET_HAMSTER));
    }

    private static Map<Integer, Integer> shares(int petType) {
        PetSpec spec = PetSpec.forType(petType);
        Map<Integer, Integer> counts = new HashMap<>();
        for (PetSpec.Activity activity : spec.activities) {
            counts.put(activity.animation, 0);
        }
        for (long seed = 0; seed < 500; seed++) {
            List<Segment> segments = live(spec, seed).segments;
            for (int i = 1; i < segments.size(); i++) {
                counts.merge(segments.get(i).animation, 1, Integer::sum);
            }
        }
        return counts;
    }

    private static double averageLength(int petType) {
        PetSpec spec = PetSpec.forType(petType);
        long total = 0;
        int count = 0;
        for (long seed = 0; seed < 300; seed++) {
            for (Segment segment : live(spec, seed).segments) {
                total += segment.lengthMs();
                count++;
            }
        }
        return (double) total / count;
    }

    private static PetSpec.Activity find(PetSpec spec, int animation) {
        for (PetSpec.Activity activity : spec.activities) {
            if (activity.animation == animation) {
                return activity;
            }
        }
        return null;
    }

    // --- Walking ----------------------------------------------------------------------------

    @Test
    public void thePetNeverLeavesTheGroundNorJumps() {
        for (int petType : PetSpec.allPetTypes()) {
            PetSpec spec = PetSpec.forType(petType);
            for (long seed = 0; seed < 200; seed++) {
                Session s = new Session();
                s.now.from = s.now.to = (seed % 7 - 3) * 0.3f; // somewhere on the ground, or just past it
                float last = onGround(s.now.from);
                tap(spec, s, T0, seed);
                Life life = new Life();
                life.record(s, T0);
                while (s.phase != ASLEEP) {
                    advance(spec, s, planEnd(s));
                    life.record(s, 0);
                }
                for (Segment segment : life.segments) {
                    assertEquals("pet " + petType + " jumped", last, segment.from, 1e-5f);
                    if (segment.animation == ANIMATION_FLY) {
                        assertTrue(Math.abs(segment.to) <= GROUND_LIMIT + 1e-5f);
                        last = segment.to;
                        continue;
                    }
                    int n = legSteps(spec, segment);
                    if (n > 0) {
                        assertTrue(n >= MIN_LEG_STEPS);
                    }
                    long steps = segment.lengthMs() / spec.walkStepMs();
                    float previous = positionAtStep(spec, segment, 0);
                    for (long step = 0; step <= steps; step++) {
                        float here = positionAtStep(spec, segment, step);
                        assertTrue("pet " + petType + " at " + here, Math.abs(here) <= GROUND_LIMIT + 1e-5f);
                        assertTrue(Math.abs(here - previous) <= WALK_STEP + 1e-4f);
                        previous = here;
                    }
                    last = positionAt(spec, segment, segment.endsAt);
                }
            }
        }
    }

    @Test
    public void aWalkCountsItsStepCyclesAndTurnsAtTheEnds() {
        // The hand-drawn pets' walk (the pixel pets': SmoothMotionTest)
        PetSpec dog = PetSpec.forType(PET_DOG);
        int walks = 0;
        for (long seed = 0; seed < 200; seed++) {
            for (Segment segment : live(dog, seed).segments) {
                if (segment.animation != ANIMATION_WALK) {
                    continue;
                }
                walks++;
                long cycles = repeatsOf(dog, segment);
                assertTrue(cycles >= 2 && cycles <= 4);
                assertEquals(cycles * dog.animation(ANIMATION_WALK).frames.length * dog.walkStepMs(),
                    segment.lengthMs());
            }
        }
        assertTrue(walks > 100);
    }

    @Test
    public void aWalkGoesThereAndBackFacingTheWay() {
        PetSpec cat = PetSpec.forType(PET_CAT);
        Segment s = new Segment();
        s.animation = ANIMATION_WALK;
        s.from = -0.5f;
        s.to = -0.5f + 3 * WALK_STEP;
        assertEquals(3, legSteps(cat, s));
        assertEquals(-0.5f, positionAtStep(cat, s, 0), 1e-5f);
        assertEquals(s.to, positionAtStep(cat, s, 3), 1e-5f);
        assertEquals(-0.5f, positionAtStep(cat, s, 6), 1e-5f);
        assertEquals("steps before the start fold back too", positionAtStep(cat, s, 5), positionAtStep(cat, s, -1),
            1e-5f);
        assertTrue(facingRightAtStep(cat, s, 0));
        assertTrue("arrives facing the way it went", facingRightAtStep(cat, s, 3));
        assertFalse(facingRightAtStep(cat, s, 4));
        assertFalse("arrives back facing the way it went", facingRightAtStep(cat, s, 6));
        assertTrue(facingRightAtStep(cat, s, 7));
        s.startedAt = T0;
        assertEquals(0, stepAt(cat, s, T0 - 5_000)); // clock went backwards
        assertEquals(2, stepAt(cat, s, T0 + 2 * cat.walkStepMs() + 1));
    }

    @Test
    public void scenesNeverWalk() {
        PetSpec aquarium = PetSpec.forType(PET_AQUARIUM);
        assertFalse(aquarium.walks());
        for (long seed = 0; seed < 50; seed++) {
            for (Segment segment : live(aquarium, seed).segments) {
                assertEquals(0, legSteps(aquarium, segment));
                assertEquals(0f, segment.from, 0f);
            }
        }
    }

    // --- Flying -----------------------------------------------------------------------------

    @Test
    public void onlyTheParrotFliesAndAtMostOnceAWake() {
        int flights = 0;
        for (int petType : PetSpec.allPetTypes()) {
            PetSpec spec = PetSpec.forType(petType);
            assertEquals(petType == PET_PARROT, spec.flies());
            for (long seed = 0; seed < 300; seed++) {
                int inThisLife = 0;
                for (Segment segment : live(spec, seed).segments) {
                    if (segment.animation == ANIMATION_FLY) {
                        inThisLife++;
                    }
                }
                assertTrue(inThisLife <= 1);
                flights += inThisLife;
            }
        }
        assertTrue("the parrot flies now and then", flights > 30);
    }

    @Test
    public void aFlightIsAPlanOfItsOwnAndLandsElsewhere() {
        PetSpec parrot = PetSpec.forType(PET_PARROT);
        int seen = 0;
        for (long seed = 0; seed < 300; seed++) {
            Life life = live(parrot, seed);
            for (int i = 0; i < life.plans.size(); i++) {
                Segment[] plan = life.plans.get(i);
                for (int k = 0; k < 2; k++) {
                    if (plan[k] != null && plan[k].animation == ANIMATION_FLY) {
                        assertEquals("a flight starts its plan", 0, k);
                        assertNull("nothing follows a flight in the same update", plan[1]);
                    }
                }
            }
            List<Segment> segments = life.segments;
            for (int i = 0; i + 1 < segments.size(); i++) {
                Segment flight = segments.get(i);
                if (flight.animation != ANIMATION_FLY) {
                    continue;
                }
                seen++;
                assertTrue(Math.abs(flight.to - flight.from) >= MIN_FLIGHT - 1e-5f);
                assertTrue(Math.abs(flight.to - flight.from) * DEFAULT_GROUND_PX <= maxFlightPx(parrot) + 1);
                int frames = smoothFlightFrames(parrot, flight);
                assertTrue(frames >= FlightPath.minFrames() && frames <= MAX_FLIGHT_FRAMES);
                assertTrue(flight.lengthMs() >= minPlanMs(parrot));
                Segment next = segments.get(i + 1);
                assertEquals("lands where it was going", flight.to, next.from, 1e-5f);
                assertNotEquals(ANIMATION_WALK, next.animation);
                assertEquals("lands looking the way it flew", flight.to > flight.from, next.facingRight);
            }
        }
        assertTrue(seen > 30);
    }

    @Test
    public void aTapDuringAFlightLetsItLand() {
        PetSpec parrot = PetSpec.forType(PET_PARROT);
        for (long seed = 0; seed < 300; seed++) {
            Session s = new Session();
            tap(parrot, s, T0, seed);
            for (int i = 0; i < 40 && s.phase == AWAKE; i++) {
                if (s.now.animation == ANIMATION_FLY) {
                    long mid = s.now.startedAt + 2_000;
                    long endsAt = s.now.endsAt;
                    tap(parrot, s, mid, 1);
                    assertEquals(ANIMATION_FLY, s.now.animation);
                    assertEquals(endsAt, s.now.endsAt);
                    assertEquals(mid + PetSpec.AWAKE_MS, s.awakeUntil);
                    return;
                }
                advance(parrot, s, planEnd(s));
            }
        }
        throw new AssertionError("the parrot never flew");
    }

    // --- Several widgets --------------------------------------------------------------------

    @Test
    public void widgetsLiveIndependently() {
        PetSpec panda = PetSpec.forType(PET_PANDA);
        PetSpec parrot = PetSpec.forType(PET_PARROT);
        String pandaAlone = describe(live(panda, 11));
        String parrotAlone = describe(live(parrot, 12));
        // Both awake at once, advanced by one shared alarm at whichever turn comes first
        Session a = new Session();
        Session b = new Session();
        tap(panda, a, T0, 11);
        tap(parrot, b, T0, 12);
        Life lifeA = new Life();
        Life lifeB = new Life();
        lifeA.record(a, T0);
        lifeB.record(b, T0);
        while (a.phase != ASLEEP || b.phase != ASLEEP) {
            long alarm = Math.min(nextTurn(a), nextTurn(b));
            if (alarm == nextTurn(a) && advance(panda, a, alarm) && a.phase != ASLEEP) {
                lifeA.record(a, alarm);
            }
            if (alarm == nextTurn(b) && advance(parrot, b, alarm) && b.phase != ASLEEP) {
                lifeB.record(b, alarm);
            }
        }
        assertEquals(pandaAlone, describe(lifeA));
        assertEquals(parrotAlone, describe(lifeB));
    }

    // --- Generator --------------------------------------------------------------------------

    @Test
    public void theGeneratorStaysInRangeAndSpreads() {
        Session s = new Session();
        s.random = 123;
        int[] counts = new int[6];
        for (int i = 0; i < 6000; i++) {
            int value = nextInt(s, 6);
            assertTrue(value >= 0 && value < 6);
            counts[value]++;
        }
        for (int count : counts) {
            assertTrue(count > 800 && count < 1200);
        }
    }

    @Test
    public void copiesStayWithinTheirBudget() {
        assertEquals(1, nextCopies(6_000, 3_000, 16));
        assertEquals(2, nextCopies(3_000, 4_000, 16));
        assertEquals(3, nextCopies(1_000, 9_000, 16));    // capped by MAX_PAIR_FRAMES
        assertEquals(1, nextCopies(1_000, 9_000, 32));
    }

    @Test
    public void theNewPetsShowAtLeastFourActivitiesInAMinute() {
        for (int petType : new int[] {PixelCatWidget.PET_PUPPY, PixelCatWidget.PET_KITTEN}) {
            PetSpec spec = PetSpec.forType(petType);
            for (long seed = 0; seed < 300; seed++) {
                Session s = new Session();
                long t0 = 1_700_000_000_000L;
                tap(spec, s, t0, seed);
                java.util.Set<Integer> done = new java.util.HashSet<>();
                int previous = -1;
                while (s.phase == AWAKE) {
                    for (Segment segment : new Segment[] {s.now, s.next}) {
                        if (segment != null) {
                            assertTrue(segment.animation != previous);
                            previous = segment.animation;
                            long repeats = repeatsOf(spec, segment);
                            assertTrue(segment.animation == PixelCatWidget.ANIMATION_HAPPY
                                || repeats >= spec.minRepeats && repeats <= spec.maxRepeats);
                            done.add(segment.animation);
                        }
                    }
                    advance(spec, s, planEnd(s));
                }
                assertTrue(petType + " seed " + seed + ": " + done, done.size() >= 4);
            }
        }
    }
}
