package com.example.pixelpetwidget;

import static com.example.pixelpetwidget.PetBehavior.*;
import static com.example.pixelpetwidget.PixelCatWidget.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class BackgroundEventsTest {

    private static final long T0 = 1_700_000_000_000L;
    private static final int[] PETS = {PET_CAT, PET_DOG, PET_RABBIT, PET_PANDA, PET_HAMSTER, PET_CAPYBARA, PET_PARROT,
        PET_PUPPY, PET_KITTEN};

    /** The plans of one awake spell on a background, with the alarms on time. */
    private static List<Segment[]> live(PetSpec spec, BackgroundEvents.Interaction interaction, long seed) {
        List<Segment[]> plans = new ArrayList<>();
        Session s = new Session();
        tap(spec, interaction, s, T0, seed);
        plans.add(new Segment[] {s.now, s.next});
        while (s.phase != ASLEEP) {
            advance(spec, interaction, s, planEnd(s));
            if (s.phase == AWAKE) {
                plans.add(new Segment[] {s.now, s.next});
            }
        }
        return plans;
    }

    /** The walk or flight over to an interaction's prop in a plan, or null. */
    private static Segment over(PetSpec spec, Segment[] plan) {
        for (Segment segment : plan) {
            if (segment != null && segment.event >= 0
                && (arrives(segment) || segment.animation == ANIMATION_FLY && spec.flies())) {
                return segment;
            }
        }
        return null;
    }

    /** Whether a plan is (a part of) the walk or flight over to an interaction's prop. */
    private static boolean goingOver(PetSpec spec, Segment[] plan) {
        return over(spec, plan) != null;
    }

    /** Whether a plan is an interaction itself. */
    private static boolean acting(PetSpec spec, Segment[] plan) {
        return plan[0].event >= 0 && !goingOver(spec, plan);
    }

    @Test
    public void everyPetHasOneInteractionWithEveryBackground() {
        assertEquals(PETS.length * Backdrop.allTypes().length, BackgroundEvents.TABLE.length);
        for (int pet : PETS) {
            for (int background : Backdrop.allTypes()) {
                BackgroundEvents.Interaction row = BackgroundEvents.find(pet, background);
                assertNotNull(pet + " on " + background, row);
            }
        }
        // No background, an old picture or the aquarium (a scene of its own): no events
        assertNull(BackgroundEvents.find(PET_CAT, 1));
        assertNull(BackgroundEvents.find(PET_CAT, 99));
        for (int background : Backdrop.allTypes()) {
            assertNull(BackgroundEvents.find(PET_AQUARIUM, background));
        }
        BackgroundEvents.Interaction cat = BackgroundEvents.find(PET_CAT, Backdrop.MAPLE_PARK);
        assertEquals(BackgroundEvents.LEAF, cat.effect);
        assertEquals(ANIMATION_SEAT, cat.act);
        assertEquals(ANIMATION_LICK, cat.then);
    }

    @Test
    public void everyRowOfTheTableIsSound() {
        for (BackgroundEvents.Interaction row : BackgroundEvents.TABLE) {
            String name = row.pet + "/" + row.background + ": " + row.what;
            PetSpec spec = PetSpec.forType(row.pet);
            assertEquals(row.pet, spec.petType);
            Backdrop backdrop = Backdrop.forType(row.background);
            assertNotNull(backdrop);
            // Only animations the pet has; the hand-drawn pets only their own frames
            assertTrue(name, spec.hasAnimation(row.act) && spec.hasAnimation(row.then));
            assertNotEquals(name, row.act, row.then);
            if (row.pet == PET_CAT || row.pet == PET_DOG || row.pet == PET_RABBIT) {
                assertTrue(name, row.act < ANIMATION_POSE_1 && row.then < ANIMATION_POSE_1);
            }
            if (row.prop != null) {
                Backdrop.Prop prop = backdrop.prop(row.prop);
                assertNotNull(name, prop);
                assertTrue(name, spec.hasAnimation(row.notice));
                assertNotEquals(name, ANIMATION_WALK, row.notice);
                assertNotEquals(name, ANIMATION_WALK, row.act);
                assertEquals(row.notice, row.first());
            } else {
                assertEquals(row.act, row.first());
            }
            if (row.effect == BackgroundEvents.STATE) {
                Backdrop.Prop prop = backdrop.prop(row.prop);
                assertEquals(name, row.state, prop.state(row.state).name);
                assertNotEquals(name, prop.ambient, row.state);
            }
            if (row.effect == BackgroundEvents.LEAF) {
                assertNotNull(name, backdrop.leafTurning);
                assertTrue(name, backdrop.canopyColumnNear(backdrop.width / 2) >= 0);
            }
            if (row.effect == BackgroundEvents.DROPS) {
                assertNotEquals(name, 0, backdrop.drop);
            }
            if (row.anchorX != BackgroundEvents.GROUND) {
                // A spot on a pixel pet's frame
                assertTrue(name, spec.pixelGrid > 0);
                assertTrue(name, row.anchorX >= 0 && row.anchorX < 64 && row.anchorY >= 0 && row.anchorY < 64);
            }
            assertTrue(row.weight > 0);
            assertTrue(row.what.length() > 10);
            // One row per pet and background
            assertTrue(BackgroundEvents.find(row.pet, row.background) == row);
        }
    }

    @Test
    public void eventsAreRareAndPlayOnceForEveryPetAndBackground() {
        for (BackgroundEvents.Interaction interaction : BackgroundEvents.TABLE) {
            PetSpec spec = PetSpec.forType(interaction.pet);
            String name = interaction.pet + "/" + interaction.background;
            int lives = 0;
            int withEvents = 0;
            for (long seed = 0; seed < 120; seed++) {
                List<Long> starts = new ArrayList<>();
                List<Segment[]> plans = live(spec, interaction, seed);
                for (int i = 0; i < plans.size(); i++) {
                    Segment[] plan = plans.get(i);
                    if (goingOver(spec, plan)) {
                        // A pixel pet's long walk over takes several plans, one after the other
                        if (i == 0 || !goingOver(spec, plans.get(i - 1))) {
                            starts.add(plan[0].startedAt);
                        }
                        Segment over = over(spec, plan);
                        float target = targetOf(interaction, 0f);
                        boolean there = Math.abs(positionAt(spec, over, over.endsAt) - target) < 1e-4f;
                        if (!there) {
                            // On its way: the next plan walks on (or it falls asleep)
                            assertTrue(name, spec.smooth() && over.animation == ANIMATION_WALK);
                            assertTrue(name, i + 1 == plans.size() || goingOver(spec, plans.get(i + 1)));
                            continue;
                        }
                        // It ends at its prop, and the interaction comes with the next plan (unless it fell asleep)
                        assertEquals(name, target, positionAt(spec, over, over.endsAt + 5_000), 1e-4f);
                        if (plan[1] != null && !spec.smooth()) {
                            // It looked at the prop, then walked over
                            assertEquals(name, interaction.notice, plan[0].animation);
                            assertEquals(-1, plan[0].event);
                        }
                        if (plan[1] != null && spec.smooth()) {
                            // It walked over (looking at the prop first), then looks at it there
                            assertEquals(name, interaction.notice, plan[1].animation);
                            assertEquals(-1, plan[1].event);
                        }
                        if (i + 1 < plans.size()) {
                            assertTrue(name, acting(spec, plans.get(i + 1)));
                        }
                    } else if (acting(spec, plan)) {
                        if (i == 0 || !goingOver(spec, plans.get(i - 1))) {
                            starts.add(plan[0].startedAt);
                        }
                        assertEquals(name, interaction.act, plan[0].animation);
                        assertEquals(BackgroundEvents.indexOf(interaction), plan[0].event);
                        long effect = BackgroundEvents.effectMs(interaction, Backdrop.forType(interaction.background));
                        assertTrue(name, plan[0].lengthMs() >= Math.min(effect, spec.maxRepeats * loopMs(spec, plan[0])));
                        assertNotNull(plan[1]);
                        assertEquals(name, interaction.then, plan[1].animation);
                        long repeats = repeatsOf(spec, plan[1]);
                        assertTrue(name + " " + repeats, repeats >= 2 && repeats <= 4);
                        assertTrue(name, plan[1].endsAt - plan[0].startedAt >= minPlanMs(spec));
                    }
                }
                lives++;
                assertTrue(name, starts.size() <= BackgroundEvents.MAX_PER_SESSION);
                if (!starts.isEmpty()) {
                    withEvents++;
                    assertTrue(starts.get(0) - T0 >= PetBehavior.FIRST_EVENT_AFTER_MS);
                }
                for (int i = 1; i < starts.size(); i++) {
                    assertTrue(starts.get(i) - starts.get(i - 1) >= BackgroundEvents.MIN_GAP_MS);
                }
            }
            assertTrue(name + " plays with its background now and then: " + withEvents + "/" + lives,
                withEvents > lives / 4);
        }
    }

    @Test
    public void neverTheSameAnimationTwiceInARowAndAlwaysOnTheGround() {
        for (BackgroundEvents.Interaction interaction : BackgroundEvents.TABLE) {
            PetSpec spec = PetSpec.forType(interaction.pet);
            for (long seed = 0; seed < 60; seed++) {
                int previous = -1;
                List<Segment[]> plans = live(spec, interaction, seed);
                for (Segment[] plan : plans) {
                    for (Segment segment : plan) {
                        if (segment == null) {
                            continue;
                        }
                        // (a pixel pet's long walk over a prop goes on in the next plan)
                        if (!(segment.event >= 0 && arrives(segment) && previous == ANIMATION_WALK)) {
                            assertNotEquals(interaction.what + " seed " + seed, previous, segment.animation);
                        }
                        previous = segment.animation;
                        for (long t = segment.startedAt; t < segment.endsAt; t += 97) {
                            float x = positionAt(spec, segment, t);
                            assertTrue(Math.abs(x) <= GROUND_LIMIT + 1e-5f);
                        }
                    }
                    // Every plan lasts long enough for the next alarm
                    Segment last = plan[1] != null ? plan[1] : plan[0];
                    assertTrue(last.endsAt - plan[0].startedAt >= minPlanMs(spec));
                }
            }
        }
    }

    @Test
    public void withoutABackgroundThereAreNoEvents() {
        for (int pet : PETS) {
            PetSpec spec = PetSpec.forType(pet);
            for (long seed = 0; seed < 50; seed++) {
                for (Segment[] plan : live(spec, null, seed)) {
                    for (Segment segment : plan) {
                        assertTrue(segment == null || segment.event == -1);
                    }
                }
            }
        }
    }

    @Test
    public void theSameSeedGivesTheSameEvents() {
        for (BackgroundEvents.Interaction interaction : BackgroundEvents.TABLE) {
            PetSpec spec = PetSpec.forType(interaction.pet);
            for (long seed = 0; seed < 10; seed++) {
                assertEquals(describe(live(spec, interaction, seed)), describe(live(spec, interaction, seed)));
            }
        }
    }

    @Test
    public void aTapOrSleepEndsAnEventHalfWay() {
        BackgroundEvents.Interaction dog = BackgroundEvents.find(PET_DOG, Backdrop.MAPLE_PARK);
        PetSpec spec = PetSpec.forType(PET_DOG);
        int seen = 0;
        for (long seed = 0; seed < 200 && seen < 5; seed++) {
            Session s = new Session();
            tap(spec, dog, s, T0, seed);
            while (s.phase == AWAKE && s.eventNext < 0) {
                advance(spec, dog, s, planEnd(s));
            }
            if (s.phase != AWAKE) {
                continue;
            }
            seen++;
            // On its way to the heap: a tap cheers it up instead
            long now = s.now.startedAt + 100;
            tap(spec, dog, s, now, seed);
            assertEquals(-1, s.eventNext);
            assertEquals(spec.joyAnimation, s.now.animation);
            // And another background (the widget's was changed): its interaction never comes
            Session t = new Session();
            tap(spec, dog, t, T0, seed);
            while (t.phase == AWAKE && t.eventNext < 0) {
                advance(spec, dog, t, planEnd(t));
            }
            BackgroundEvents.Interaction other = BackgroundEvents.find(PET_DOG, Backdrop.PUMPKIN_FIELD);
            advance(spec, other, t, planEnd(t));
            assertEquals(-1, t.now.event);
            assertEquals(-1, t.eventNext);
            // Asleep, nothing is left of it
            Session u = new Session();
            tap(spec, dog, u, T0, seed);
            while (u.phase == AWAKE && u.eventNext < 0) {
                advance(spec, dog, u, planEnd(u));
            }
            sleepNow(spec, u, planEnd(u) - 10);
            assertEquals(-1, u.eventNext);
        }
        assertTrue(seen > 0);
    }

    private static String describe(List<Segment[]> plans) {
        StringBuilder out = new StringBuilder();
        for (Segment[] plan : plans) {
            for (Segment segment : plan) {
                if (segment != null) {
                    out.append(segment.animation).append('/').append(segment.event).append('/')
                        .append(segment.variant).append('/').append(segment.to).append('@')
                        .append(segment.startedAt).append(';');
                }
            }
        }
        return out.toString();
    }

    @Test
    public void aLeafFallsFromTheCanopyToTheGroundInFrontOfThePet() {
        Backdrop maple = Backdrop.forType(Backdrop.MAPLE_PARK);
        for (int toX = 60; toX < 520; toX += 23) {
            for (boolean swayRight : new boolean[] {true, false}) {
                int column = maple.canopyColumnNear(toX - 8);
                assertTrue(column >= 0);
                int fromY = maple.canopy[column] - 2;
                int toY = maple.ground - maple.leafHeight + 1;
                BackgroundEvents.LeafPose[] poses = BackgroundEvents.fall(column, fromY, toX, toY, 10, swayRight,
                    BackgroundEvents.LEAF_FALL_FRAMES);
                assertEquals(BackgroundEvents.LEAF_FALL_FRAMES + 1, poses.length);
                assertEquals(column, poses[0].x);
                assertEquals(fromY, poses[0].y);
                BackgroundEvents.LeafPose lying = poses[poses.length - 1];
                assertEquals(toX, lying.x);
                assertEquals(toY, lying.y);
                assertEquals(-1, lying.sprite);
                for (int i = 1; i < poses.length - 1; i++) {
                    // Down all the way, a few pixels at a time, never off the picture
                    assertTrue(poses[i].y >= poses[i - 1].y);
                    assertTrue(poses[i].y - poses[i - 1].y <= 12);
                    assertTrue(Math.abs(poses[i].x - poses[i - 1].x) <= 8);
                    assertTrue(poses[i].x >= 0 && poses[i].x + maple.leafWidth <= maple.width);
                    assertTrue(poses[i].sprite >= 0 && poses[i].sprite < maple.leafTurning.length);
                }
            }
        }
    }

    @Test
    public void backgroundsAndTheirFallback() {
        assertNull("the painted pictures are not pixel-art backgrounds", Backdrop.forType(1));
        assertNull(Backdrop.forType(3));
        assertNull("an unknown background is the default picture", Backdrop.forType(42));
        assertEquals(3, Backdrop.allTypes().length);
        for (int type : Backdrop.allTypes()) {
            Backdrop backdrop = Backdrop.forType(type);
            assertEquals(type, backdrop.type);
            assertEquals(576, backdrop.width);
            assertEquals(288, backdrop.height);
            assertTrue(backdrop.ground > backdrop.height / 2 && backdrop.ground < backdrop.height);
            assertEquals(backdrop.width, backdrop.canopy.length);
            for (Backdrop.Element element : backdrop.elements) {
                assertEquals(element.frames.length, element.x.length);
                assertEquals(element.frames.length, element.y.length);
                for (int f = 0; f < element.frames.length; f++) {
                    assertTrue(element.x[f] >= 0 && element.x[f] + element.width <= backdrop.width);
                    assertTrue(element.y[f] >= 0 && element.y[f] + element.height <= backdrop.height);
                }
            }
            for (Backdrop.Prop prop : backdrop.props) {
                // Where a pet can walk to, standing on the ground line
                assertTrue(prop.name, Math.abs(prop.offset) <= GROUND_LIMIT);
                assertTrue(prop.name, prop.baseline > 0 && prop.baseline <= prop.height);
                assertEquals(prop.ambient, prop.state(prop.ambient).name);
                for (Backdrop.State state : prop.states) {
                    assertTrue(state.frames.length > 0 && state.intervalMs > 0);
                }
            }
            for (Backdrop.Tile tile : backdrop.tiles) {
                assertTrue(tile.frames.length > 0 && tile.intervalMs > 0);
            }
        }
        // The rain is only on the rainy evening; the props stay apart
        assertEquals(1, Backdrop.forType(Backdrop.RAINY_EVENING).tiles.length);
        assertEquals(0, Backdrop.forType(Backdrop.MAPLE_PARK).tiles.length);
        assertTrue(Backdrop.forType(Backdrop.PUMPKIN_FIELD).prop("pumpkin_big").front);
    }
}
