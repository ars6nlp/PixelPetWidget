package com.example.pixelpetwidget;

import static com.example.pixelpetwidget.PixelCatWidget.*;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PetSpecTest {

    // --- The original pets must behave exactly as before PetSpec existed --------------------

    @Test
    public void catKeepsItsBehaviour() {
        PetSpec cat = PetSpec.forType(PET_CAT);
        assertArrayEquals(new int[] {ANIMATION_SLEEP, ANIMATION_LICK, ANIMATION_SEAT, ANIMATION_WALK}, cat.animationTypes);
        assertReactions(cat, ANIMATION_LICK, ANIMATION_SEAT);
        assertAnim(cat, ANIMATION_SLEEP, SLEEP_FRAMES, 100);
        assertAnim(cat, ANIMATION_LICK, LICK_FRAMES, 100);
        assertAnim(cat, ANIMATION_SEAT, CAT_SEAT_FRAMES, 100);
        assertAnim(cat, ANIMATION_WALK, PixelCatWidget.WALK_FRAMES, 130);
        assertAnim(cat, ANIMATION_EAT, SLEEP_FRAMES, 100);       // not a cat animation: sleep frames
        assertCounts(cat, 16, 14, 14, 16);
        assertSmoothPet(cat, false);
    }

    @Test
    public void dogKeepsItsBehaviour() {
        PetSpec dog = PetSpec.forType(PET_DOG);
        assertArrayEquals(new int[] {ANIMATION_SLEEP, ANIMATION_EAT, ANIMATION_LICK, ANIMATION_WALK}, dog.animationTypes);
        assertReactions(dog, ANIMATION_EAT, ANIMATION_LICK);
        assertAnim(dog, ANIMATION_SLEEP, DOG_SLEEP_FRAMES, 100);
        assertAnim(dog, ANIMATION_EAT, DOG_EAT_FRAMES, 100);
        assertAnim(dog, ANIMATION_LICK, DOG_SIT_FRAMES, 100);
        assertAnim(dog, ANIMATION_WALK, DOG_WALK_FRAMES, 130);
        assertAnim(dog, ANIMATION_SEAT, DOG_SLEEP_FRAMES, 100);
        assertCounts(dog, 16, 16, 16, 16);
        assertSmoothPet(dog, true);
    }

    @Test
    public void rabbitKeepsItsBehaviour() {
        PetSpec rabbit = PetSpec.forType(PET_RABBIT);
        assertArrayEquals(new int[] {ANIMATION_SLEEP, ANIMATION_EAT, ANIMATION_SEAT, ANIMATION_WALK}, rabbit.animationTypes);
        assertReactions(rabbit, ANIMATION_SEAT, ANIMATION_SEAT);
        assertAnim(rabbit, ANIMATION_SLEEP, RABBIT_SLEEP_FRAMES, 100);
        assertAnim(rabbit, ANIMATION_EAT, RABBIT_EAT_FRAMES, 100);
        assertAnim(rabbit, ANIMATION_SEAT, RABBIT_SIT_FRAMES, 100);
        assertAnim(rabbit, ANIMATION_WALK, RABBIT_WALK_FRAMES, 130);
        assertAnim(rabbit, ANIMATION_LICK, RABBIT_SLEEP_FRAMES, 100);
        assertCounts(rabbit, 16, 16, 16, 16);
        assertSmoothPet(rabbit, false);
    }

    @Test
    public void unknownPetTypesFallBackToTheCat() {
        for (int unknown : new int[] {10, 99, -1}) {
            PetSpec spec = PetSpec.forType(unknown);
            assertEquals(PET_CAT, spec.petType);
            assertSame(SLEEP_FRAMES, framesFor(unknown, ANIMATION_SLEEP));
            // ...and lives like the cat
            assertEquals(ANIMATION_LICK, spec.wakeAnimation);
            assertSame(PetSpec.forType(PET_CAT).activities, spec.activities);
        }
        assertEquals(PET_CAT, petTypeFromName(null));
    }

    // --- What the pets do on their own --------------------------------------------------------

    @Test
    public void repeatsAreTwoToFourEverywhere() {
        assertEquals(2, PetSpec.MIN_REPEATS);
        assertEquals(4, PetSpec.MAX_REPEATS);
        for (int petType : PetSpec.allPetTypes()) {
            assertEquals(PetSpec.MIN_REPEATS, PetSpec.forType(petType).minRepeats);
            assertEquals(PetSpec.MAX_REPEATS, PetSpec.forType(petType).maxRepeats);
        }
    }

    @Test
    public void oneShotsAreTheRollTheChestAndTheFlight() {
        for (int petType : PetSpec.allPetTypes()) {
            for (PetSpec.Activity activity : PetSpec.forType(petType).activities) {
                boolean oneShot = petType == PET_PANDA && activity.animation == ANIMATION_SPECIAL
                    || petType == PET_AQUARIUM && activity.animation == ANIMATION_SPECIAL
                    || petType == PET_PARROT && activity.animation == ANIMATION_FLY;
                assertEquals("pet " + petType + " " + activity.animation, oneShot, activity.once);
            }
        }
    }

    @Test
    public void everyPetStaysAwakeAMinute() {
        assertEquals(60_000, PetSpec.AWAKE_MS);
        for (int petType : PetSpec.allPetTypes()) {
            assertEquals(PetSpec.AWAKE_MS, PetSpec.forType(petType).awakeMs);
        }
    }

    @Test
    public void everyActivityIsSound() {
        for (int petType : PetSpec.allPetTypes()) {
            PetSpec spec = PetSpec.forType(petType);
            assertTrue(spec.hasAnimation(spec.wakeAnimation));
            assertTrue(spec.hasAnimation(spec.joyAnimation));
            assertTrue("pet " + petType, spec.activities.length >= 3);
            for (PetSpec.Activity activity : spec.activities) {
                assertTrue("pet " + petType + " has no animation " + activity.animation,
                    spec.hasAnimation(activity.animation));
                assertTrue(activity.animation != ANIMATION_SLEEP);
                assertTrue(activity.weight > 0);
                for (int previous : activity.notAfter) {
                    assertTrue(spec.hasAnimation(previous));
                }
            }
        }
    }

    @Test
    public void everyPetHasItsActivities() {
        assertActivities(PET_CAT, ANIMATION_SEAT, ANIMATION_LICK, ANIMATION_WALK);
        assertActivities(PET_DOG, ANIMATION_WALK, ANIMATION_LICK, ANIMATION_EAT);
        assertActivities(PET_RABBIT, ANIMATION_EAT, ANIMATION_WALK, ANIMATION_SEAT);
        assertActivities(PET_PANDA, ANIMATION_IDLE, ANIMATION_EAT, ANIMATION_SPECIAL, ANIMATION_HAPPY, ANIMATION_WALK);
        assertActivities(PET_HAMSTER, ANIMATION_WALK, ANIMATION_SPECIAL, ANIMATION_IDLE, ANIMATION_EAT, ANIMATION_HAPPY);
        assertActivities(PET_CAPYBARA, ANIMATION_IDLE, ANIMATION_SPECIAL, ANIMATION_EAT, ANIMATION_HAPPY, ANIMATION_WALK);
        assertActivities(PET_PARROT, ANIMATION_SPECIAL, ANIMATION_IDLE, ANIMATION_EAT, ANIMATION_WALK, ANIMATION_HAPPY,
            ANIMATION_FLY);
        assertTrue(activity(PET_PARROT, ANIMATION_WALK).notAfter(ANIMATION_FLY));
        assertActivities(PET_AQUARIUM, ANIMATION_WALK, ANIMATION_IDLE, ANIMATION_EAT, ANIMATION_SPECIAL, ANIMATION_HAPPY);
        // The panda does not walk off right after a roll, nor the capybara after its bath
        assertTrue(activity(PET_PANDA, ANIMATION_WALK).notAfter(ANIMATION_SPECIAL));
        assertTrue(activity(PET_CAPYBARA, ANIMATION_WALK).notAfter(ANIMATION_SPECIAL));
    }

    @Test
    public void walkingSpeeds() {
        // The hand-drawn pets as they were: the whole ground (-0.9..0.9) in 7 steps, one per walk frame
        assertEquals(1.8f / 7, PetBehavior.WALK_STEP, 1e-6f);
        assertEquals(PetBehavior.WALK_STEP * 1000f / 130, PetSpec.forType(PET_CAT).walkSpeed(), 1e-5f);
        // The pixel pets by their gaits (PetSpec), in art pixels per second
        assertEquals(160, PetSpec.forType(PET_PANDA).walkStepMs());
        assertEquals(90, PetSpec.forType(PET_HAMSTER).walkStepMs());
        assertEquals(180, PetSpec.forType(PET_CAPYBARA).walkStepMs());
        assertEquals(120, PetSpec.forType(PET_PARROT).walkStepMs());
        assertEquals(90, PetSpec.forType(PET_PUPPY).walkStepMs());
        assertEquals(110, PetSpec.forType(PET_KITTEN).walkStepMs());
        assertEquals(12 * 1000f / (8 * 110), PetSpec.forType(PET_KITTEN).walkSpeed(), 1e-4f);
        assertTrue(PetSpec.forType(PET_HAMSTER).walkSpeed() > PetSpec.forType(PET_PANDA).walkSpeed());
        for (int petType : PetSpec.allPetTypes()) {
            PetSpec spec = PetSpec.forType(petType);
            if (spec.gait == null) {
                continue;
            }
            // Comfortable: a 4x4 widget (about 146 art pixels of ground) takes 7–22 s to cross
            float crossing = 146 / spec.walkSpeed();
            assertTrue(petType + ": " + crossing, crossing >= 7f && crossing <= 22f);
            assertEquals(spec.animation(ANIMATION_WALK).frames.length, spec.gait.shifts.length);
            // The gait follows the drawing: its paws move exactly so far in each walk frame
            int[] drawn = GeneratedPetFrames.walkShifts(spec.generatedName);
            if (drawn != null) {
                org.junit.Assert.assertArrayEquals(spec.generatedName, drawn, spec.gait.shifts);
            }
            for (int shift : spec.gait.shifts) {
                assertTrue(shift >= 0 && shift <= PetSpec.MAX_SHIFT_PX);
            }
        }
        for (int petType : PetSpec.allPetTypes()) {
            assertEquals(petType != PET_AQUARIUM, PetSpec.forType(petType).walks());
        }
    }

    // --- Registry ------------------------------------------------------------------------------

    @Test
    public void everyPetIsRegistered() {
        for (int petType : PetSpec.allPetTypes()) {
            assertEquals(petType, PetSpec.forType(petType).petType);
        }
        assertEquals(PET_CAT, PetSpec.forType(99).petType); // unknown stored value
    }

    @Test
    public void petNamesFromTheMainScreen() {
        assertEquals(PET_PANDA, petTypeFromName("panda"));
        assertEquals(PET_HAMSTER, petTypeFromName("hamster"));
        assertEquals(PET_CAPYBARA, petTypeFromName("capybara"));
        assertEquals(PET_PARROT, petTypeFromName("parrot"));
        assertEquals(PET_AQUARIUM, petTypeFromName("aquarium"));
        assertEquals(PET_CAT, petTypeFromName("dragon"));
    }

    @Test
    public void storedIdsNeverChange() {
        // Saved in SharedPreferences of existing users
        assertEquals(0, PET_CAT);
        assertEquals(1, PET_DOG);
        assertEquals(2, PET_RABBIT);
        // The old parrot and capybara were saved as 3 and 4: those widgets now show the new ones
        assertEquals(3, PET_PARROT);
        assertEquals(4, PET_CAPYBARA);
        assertEquals(5, PET_PANDA);
        assertEquals(6, PET_HAMSTER);
        assertEquals(7, PET_AQUARIUM);
        assertEquals(0, ANIMATION_SLEEP);
        assertEquals(1, ANIMATION_LICK);
        assertEquals(2, ANIMATION_WALK);
        assertEquals(3, ANIMATION_EAT);
        assertEquals(4, ANIMATION_SEAT);
    }

    // --- New pets ------------------------------------------------------------------------------

    private static final int[] PIXEL_PETS = {PET_PANDA, PET_HAMSTER, PET_CAPYBARA, PET_PARROT, PET_PUPPY, PET_KITTEN};

    // --- Aquarium -------------------------------------------------------------------------------

    @Test
    public void aquariumIsASceneWithSixAnimations() {
        PetSpec aquarium = PetSpec.forType(PET_AQUARIUM);
        assertArrayEquals(new int[] {ANIMATION_SLEEP, ANIMATION_HAPPY, ANIMATION_IDLE, ANIMATION_EAT, ANIMATION_SPECIAL,
            ANIMATION_WALK}, aquarium.animationTypes);
        assertReactions(aquarium, ANIMATION_HAPPY, ANIMATION_HAPPY);
        assertTrue(aquarium.scene);
        assertEquals(128, aquarium.frameWidth);
        assertEquals(96, aquarium.frameHeight);
        assertEquals("aquarium", aquarium.generatedName);
        assertFalse("a scene wears nothing", Wardrobe.supports(PET_AQUARIUM));
        assertTrue(aquarium.nameRes != 0 && aquarium.emojiRes != 0 && aquarium.foodBubbleRes != 0 && aquarium.iconRes != 0);
    }

    @Test
    public void aquariumFramesMatchItsConfig() {
        PetSpec aquarium = PetSpec.forType(PET_AQUARIUM);
        assertFrames(aquarium, ANIMATION_SLEEP, 12, 200);
        assertFrames(aquarium, ANIMATION_HAPPY, 16, 100);
        assertFrames(aquarium, ANIMATION_IDLE, 32, 140);
        assertFrames(aquarium, ANIMATION_EAT, 24, 120);
        assertFrames(aquarium, ANIMATION_SPECIAL, 24, 120);
        assertFrames(aquarium, ANIMATION_WALK, 32, 150);
        assertSame(GeneratedPetFrames.frames("aquarium", "treasure"), aquarium.animation(ANIMATION_SPECIAL).frames);
        assertSame(GeneratedPetFrames.frames("aquarium", "swim"), aquarium.animation(ANIMATION_WALK).frames);
        assertEquals(PetSpec.SCENE_LOOP_FRAMES, aquarium.maxLoopFrames());
    }

    @Test
    public void onlyTheSceneLoopsLonger() {
        for (int petType : PetSpec.allPetTypes()) {
            PetSpec spec = PetSpec.forType(petType);
            assertEquals(spec.petType == PET_AQUARIUM, spec.scene);
            if (!spec.scene) {
                assertEquals(spec.smooth() ? PetSpec.PIXEL_LOOP_FRAMES : MAX_LOOP_FRAMES, spec.maxLoopFrames());
                assertEquals(PetSpec.PIXEL_GRID, spec.frameWidth);
            }
        }
    }

    @Test
    public void newPetsHaveSixAnimations() {
        int[] animations = {ANIMATION_SLEEP, ANIMATION_HAPPY, ANIMATION_IDLE, ANIMATION_EAT, ANIMATION_SPECIAL,
            ANIMATION_WALK};
        for (int petType : PIXEL_PETS) {
            PetSpec spec = PetSpec.forType(petType);
            // Then their own poses for the backgrounds' interactions (the parrot's after its flight)
            int[] own;
            if (petType == PET_PANDA) {
                // and its transitions (it turns round sitting, sleeps sitting) and the ways into and
                // out of drinking the rain and hugging the pumpkin
                own = new int[] {ANIMATION_SLEEP, ANIMATION_HAPPY, ANIMATION_IDLE, ANIMATION_EAT, ANIMATION_SPECIAL,
                    ANIMATION_WALK, ANIMATION_TURN, ANIMATION_WALK_START, ANIMATION_WALK_STOP, ANIMATION_POSE_1,
                    ANIMATION_POSE_2, PetSpec.ENTER + ANIMATION_POSE_2, PetSpec.LEAVE + ANIMATION_POSE_2,
                    ANIMATION_POSE_3, PetSpec.ENTER + ANIMATION_POSE_3, PetSpec.LEAVE + ANIMATION_POSE_3};
            } else if (petType == PET_PARROT) {
                // and the way to hold a seed up, its transitions and its flight
                own = new int[] {ANIMATION_SLEEP, ANIMATION_HAPPY, ANIMATION_IDLE, ANIMATION_EAT,
                    PetSpec.ENTER + ANIMATION_EAT, PetSpec.LEAVE + ANIMATION_EAT, ANIMATION_SPECIAL, ANIMATION_WALK,
                    ANIMATION_TURN, ANIMATION_WALK_START, ANIMATION_WALK_STOP, ANIMATION_LIE_DOWN, ANIMATION_GET_UP,
                    ANIMATION_TAKEOFF, ANIMATION_FLY, ANIMATION_LAND, ANIMATION_POSE_1};
            } else if (petType == PET_CAPYBARA) {
                // and the way into and out of its bath, its turn, lying down and getting up, the way
                // to lie on the field or in the puddle
                own = new int[] {ANIMATION_SLEEP, ANIMATION_HAPPY, ANIMATION_IDLE, ANIMATION_EAT, ANIMATION_SPECIAL,
                    PetSpec.ENTER + ANIMATION_SPECIAL, PetSpec.LEAVE + ANIMATION_SPECIAL, ANIMATION_WALK,
                    ANIMATION_TURN, ANIMATION_LIE_DOWN, ANIMATION_GET_UP, ANIMATION_POSE_1, ANIMATION_POSE_2,
                    PetSpec.ENTER + ANIMATION_POSE_2, PetSpec.LEAVE + ANIMATION_POSE_2};
            } else if (petType == PET_KITTEN) {
                // and its transitions, and the ways into and out of the heap and onto the pumpkin
                own = new int[] {ANIMATION_SLEEP, ANIMATION_HAPPY, ANIMATION_IDLE, ANIMATION_EAT, ANIMATION_SPECIAL,
                    ANIMATION_WALK, ANIMATION_TURN, ANIMATION_WALK_START, ANIMATION_WALK_STOP, ANIMATION_LIE_DOWN,
                    ANIMATION_GET_UP, ANIMATION_POSE_1, PetSpec.ENTER + ANIMATION_POSE_1,
                    PetSpec.LEAVE + ANIMATION_POSE_1, ANIMATION_POSE_2, ANIMATION_POSE_3, ANIMATION_POSE_4,
                    PetSpec.ENTER + ANIMATION_POSE_4, PetSpec.LEAVE + ANIMATION_POSE_4};
            } else if (petType == PET_PUPPY) {
                // and the ways into and out of gnawing its bone and spinning, its transitions, and the
                // way to and from the little pumpkin
                own = new int[] {ANIMATION_SLEEP, ANIMATION_HAPPY, ANIMATION_IDLE, ANIMATION_EAT,
                    PetSpec.ENTER + ANIMATION_EAT, PetSpec.LEAVE + ANIMATION_EAT, ANIMATION_SPECIAL,
                    PetSpec.ENTER + ANIMATION_SPECIAL, PetSpec.LEAVE + ANIMATION_SPECIAL, ANIMATION_WALK,
                    ANIMATION_TURN, ANIMATION_WALK_START, ANIMATION_WALK_STOP, ANIMATION_LIE_DOWN, ANIMATION_GET_UP,
                    ANIMATION_POSE_1, ANIMATION_POSE_2, ANIMATION_POSE_3, ANIMATION_POSE_4,
                    PetSpec.ENTER + ANIMATION_POSE_4, PetSpec.LEAVE + ANIMATION_POSE_4};
            } else {
                // The hamster: its transitions and the ways to and from the leaf umbrella and up and
                // down the little pumpkin
                own = new int[] {ANIMATION_SLEEP, ANIMATION_HAPPY, ANIMATION_IDLE, ANIMATION_EAT, ANIMATION_SPECIAL,
                    ANIMATION_WALK, ANIMATION_TURN, ANIMATION_WALK_START, ANIMATION_WALK_STOP, ANIMATION_LIE_DOWN,
                    ANIMATION_GET_UP, ANIMATION_POSE_1, ANIMATION_POSE_2, PetSpec.ENTER + ANIMATION_POSE_2,
                    PetSpec.LEAVE + ANIMATION_POSE_2, ANIMATION_POSE_3, PetSpec.ENTER + ANIMATION_POSE_3,
                    PetSpec.LEAVE + ANIMATION_POSE_3};
            }
            assertArrayEquals(own, spec.animationTypes);
            assertReactions(spec, ANIMATION_HAPPY, ANIMATION_HAPPY);
            assertEquals(PetSpec.PIXEL_GRID, spec.pixelGrid);
            assertFalse(spec.facesRight);
            for (int animation : animations) {
                assertTrue("missing " + animation, spec.hasAnimation(animation));
            }
            // The poses are only for the interactions, never activities of their own; a worn hat
            // stays on in them
            for (PetSpec.Activity activity : spec.activities) {
                assertTrue(activity.animation < ANIMATION_POSE_1);
            }
            for (int pose : new int[] {ANIMATION_POSE_1, ANIMATION_POSE_2, ANIMATION_POSE_3, ANIMATION_POSE_4}) {
                if (spec.hasAnimation(pose)) {
                    assertNotSame(spec.animation(pose).frames, spec.animation(pose, "winter_hat").frames);
                    assertEquals(spec.animation(pose).frames.length, spec.animation(pose, "bow").frames.length);
                }
            }
        }
    }

    @Test
    public void frameCountsMatchTheGeneratedSprites() {
        PetSpec panda = PetSpec.forType(PET_PANDA);
        assertFrames(panda, ANIMATION_SLEEP, 16, 140);
        assertFrames(panda, ANIMATION_HAPPY, 14, 80);
        assertFrames(panda, ANIMATION_IDLE, 19, 120);
        assertFrames(panda, ANIMATION_EAT, 16, 110);
        assertFrames(panda, ANIMATION_SPECIAL, 16, 80);
        assertFrames(panda, ANIMATION_WALK, 8, 160);   // walk and flight tempos: PetSpec
        assertFrames(panda, BackgroundEvents.PANDA_FAN, 12, 110);
        assertFrames(panda, BackgroundEvents.PANDA_DRINK, 14, 100);
        assertFrames(panda, BackgroundEvents.PANDA_HUG, 12, 110);
        assertFrames(panda, ANIMATION_TURN, 1, 160);
        assertFrames(panda, ANIMATION_WALK_START, 2, 160);
        assertFrames(panda, PetSpec.ENTER + BackgroundEvents.PANDA_DRINK, 1, 100);
        assertFrames(panda, PetSpec.ENTER + BackgroundEvents.PANDA_HUG, 2, 110);
        // It turns round sitting facing you: its turn is the sitting pose itself
        assertEquals(panda.animation(ANIMATION_IDLE).frames[0], panda.animation(ANIMATION_TURN).frames[0]);
        // The drops of the rain fall into its mouth in step with the drink loop
        assertEquals((BackgroundEvents.DROP_FALL_FRAMES + BackgroundEvents.DROP_GAP_FRAMES)
            * BackgroundEvents.PROP_INTERVAL_MS, panda.animation(BackgroundEvents.PANDA_DRINK).loopMs());
        PetSpec hamster = PetSpec.forType(PET_HAMSTER);
        assertFrames(hamster, ANIMATION_SLEEP, 12, 160);
        assertFrames(hamster, ANIMATION_HAPPY, 12, 80);
        assertFrames(hamster, ANIMATION_IDLE, 16, 100);
        assertFrames(hamster, ANIMATION_EAT, 16, 90);
        assertFrames(hamster, ANIMATION_SPECIAL, 16, 90);
        assertFrames(hamster, ANIMATION_WALK, 8, 90);
        assertFrames(hamster, BackgroundEvents.HAMSTER_NIBBLE, 16, 90);
        assertFrames(hamster, BackgroundEvents.HAMSTER_UMBRELLA, 12, 100);
        assertFrames(hamster, BackgroundEvents.HAMSTER_BALANCE, 16, 120);
        assertFrames(hamster, PetSpec.ENTER + BackgroundEvents.HAMSTER_BALANCE, 4, 120);
        assertFrames(hamster, PetSpec.LEAVE + BackgroundEvents.HAMSTER_BALANCE, 5, 120);
        // Eight walk drawings now, and its balance on the pumpkin is its sitting pose moved up
        assertEquals(8, new java.util.HashSet<>(java.util.Arrays.asList(
            java.util.Arrays.stream(hamster.animation(ANIMATION_WALK).frames).boxed().toArray(Integer[]::new))).size());
        assertEquals(-12, hamster.animation(BackgroundEvents.HAMSTER_BALANCE).dy(0));
        PetSpec capybara = PetSpec.forType(PET_CAPYBARA);
        assertFrames(capybara, ANIMATION_SLEEP, 12, 170);
        assertFrames(capybara, ANIMATION_HAPPY, 10, 110);
        assertFrames(capybara, ANIMATION_IDLE, 16, 150);
        assertFrames(capybara, ANIMATION_EAT, 16, 120);
        assertFrames(capybara, ANIMATION_SPECIAL, 16, 150);
        assertFrames(capybara, ANIMATION_WALK, 8, 180);
        assertFrames(capybara, BackgroundEvents.CAPYBARA_LEAF_HAT, 16, 150);
        assertFrames(capybara, BackgroundEvents.CAPYBARA_LIE, 12, 170);
        assertFrames(capybara, ANIMATION_TURN, 2, 180);
        assertFrames(capybara, PetSpec.ENTER + ANIMATION_SPECIAL, 1, 150);
        assertFrames(capybara, PetSpec.ENTER + BackgroundEvents.CAPYBARA_LIE, 1, 170);
        // Lying down, getting up and the way to lie in the puddle are one lowered pose
        assertEquals(capybara.animation(ANIMATION_LIE_DOWN).frames[0], capybara.animation(ANIMATION_GET_UP).frames[0]);
        assertEquals(capybara.animation(ANIMATION_LIE_DOWN).frames[0],
            capybara.animation(PetSpec.ENTER + BackgroundEvents.CAPYBARA_LIE).frames[0]);
        PetSpec parrot = PetSpec.forType(PET_PARROT);
        assertFrames(parrot, ANIMATION_SLEEP, 12, 160);
        assertFrames(parrot, ANIMATION_HAPPY, 10, 90);
        assertFrames(parrot, ANIMATION_IDLE, 16, 130);
        assertFrames(parrot, ANIMATION_EAT, 16, 110);
        assertFrames(parrot, ANIMATION_SPECIAL, 12, 110);
        assertFrames(parrot, ANIMATION_WALK, 8, 120);
        assertFrames(parrot, ANIMATION_TAKEOFF, 6, 80);
        assertFrames(parrot, ANIMATION_FLY, 8, 80);
        assertFrames(parrot, ANIMATION_LAND, 6, 80);
        assertFrames(parrot, BackgroundEvents.PARROT_LEAF, 12, 130);
        assertFrames(parrot, ANIMATION_TURN, 2, 120);
        assertFrames(parrot, ANIMATION_WALK_START, 1, 120);
        assertFrames(parrot, ANIMATION_LIE_DOWN, 1, 160);
        assertFrames(parrot, PetSpec.ENTER + ANIMATION_EAT, 1, 110);
        // Fluffing up to sleep and waking are one pose, so are the first and the last step
        assertEquals(parrot.animation(ANIMATION_LIE_DOWN).frames[0], parrot.animation(ANIMATION_GET_UP).frames[0]);
        assertEquals(parrot.animation(ANIMATION_WALK_START).frames[0],
            parrot.animation(ANIMATION_WALK_STOP).frames[0]);
        PetSpec puppy = PetSpec.forType(PET_PUPPY);
        assertFrames(puppy, ANIMATION_SLEEP, 12, 160);
        assertFrames(puppy, ANIMATION_HAPPY, 16, 80);
        assertFrames(puppy, ANIMATION_IDLE, 24, 110);
        assertFrames(puppy, ANIMATION_EAT, 16, 90);
        assertFrames(puppy, ANIMATION_SPECIAL, 10, 100);
        assertFrames(puppy, ANIMATION_WALK, 8, 90);
        assertFrames(puppy, BackgroundEvents.PUPPY_FLOP, 14, 100);
        assertFrames(puppy, BackgroundEvents.PUPPY_LOOK_UP, 14, 100);
        assertFrames(puppy, BackgroundEvents.PUPPY_SHAKE, 16, 80);
        assertFrames(puppy, BackgroundEvents.PUPPY_NOSE_PUSH, 12, 120);
        // Gnawing the bone and spinning have their ways in and out; nosing the pumpkin too
        assertFrames(puppy, PetSpec.ENTER + ANIMATION_EAT, 3, 90);
        assertFrames(puppy, PetSpec.LEAVE + ANIMATION_EAT, 3, 90);
        assertFrames(puppy, PetSpec.ENTER + ANIMATION_SPECIAL, 2, 100);
        assertFrames(puppy, PetSpec.LEAVE + ANIMATION_SPECIAL, 2, 100);
        assertFrames(puppy, PetSpec.ENTER + BackgroundEvents.PUPPY_NOSE_PUSH, 6, 120);
        assertFrames(puppy, PetSpec.LEAVE + BackgroundEvents.PUPPY_NOSE_PUSH, 6, 120);
        assertFrames(puppy, ANIMATION_TURN, 2, 90);
        assertTrue(puppy.framed(ANIMATION_EAT) && puppy.framed(ANIMATION_SPECIAL));
        assertFalse(puppy.framed(ANIMATION_IDLE));
        // Spinning shows the far side by mirroring the near one
        PetSpec.Anim spin = puppy.animation(ANIMATION_SPECIAL);
        assertTrue(spin.flipped(5) && !spin.flipped(0));
        assertEquals(spin.frames[2], spin.frames[4]);
        // The drops fall on its nose in step with the look up
        assertEquals((BackgroundEvents.DROP_FALL_FRAMES + BackgroundEvents.DROP_GAP_FRAMES)
            * BackgroundEvents.PROP_INTERVAL_MS, puppy.animation(BackgroundEvents.PUPPY_LOOK_UP).loopMs());
        PetSpec kitten = PetSpec.forType(PET_KITTEN);
        assertFrames(kitten, ANIMATION_SLEEP, 12, 170);
        assertFrames(kitten, ANIMATION_HAPPY, 16, 80);
        assertFrames(kitten, ANIMATION_IDLE, 24, 120);
        assertFrames(kitten, ANIMATION_EAT, 20, 100);
        assertFrames(kitten, ANIMATION_SPECIAL, 20, 90);
        assertFrames(kitten, ANIMATION_WALK, 8, 110);
        assertFrames(kitten, BackgroundEvents.KITTEN_HIDE, 12, 130);
        assertFrames(kitten, BackgroundEvents.KITTEN_POUNCE, 20, 80);
        assertFrames(kitten, BackgroundEvents.KITTEN_BAT, 15, 100);
        assertFrames(kitten, BackgroundEvents.KITTEN_THRONE, 16, 140);
        // Its transitions: turning round, the first and last step, lying down and getting up, and
        // the ways into and out of the heap and onto and off the pumpkin
        assertFrames(kitten, ANIMATION_TURN, 2, 110);
        assertFrames(kitten, ANIMATION_WALK_START, 2, 110);
        assertFrames(kitten, ANIMATION_WALK_STOP, 2, 110);
        assertFrames(kitten, ANIMATION_LIE_DOWN, 2, 170);
        assertFrames(kitten, ANIMATION_GET_UP, 2, 160);
        assertFrames(kitten, PetSpec.ENTER + BackgroundEvents.KITTEN_HIDE, 8, 130);
        assertFrames(kitten, PetSpec.LEAVE + BackgroundEvents.KITTEN_HIDE, 8, 130);
        assertFrames(kitten, PetSpec.ENTER + BackgroundEvents.KITTEN_THRONE, 9, 140);
        assertFrames(kitten, PetSpec.LEAVE + BackgroundEvents.KITTEN_THRONE, 9, 140);
        // A jump is one crouch drawn at several heights: onto the pumpkin, five pixels a frame
        PetSpec.Anim up = kitten.animation(PetSpec.ENTER + BackgroundEvents.KITTEN_THRONE);
        assertEquals(up.frames[3], up.frames[7]);
        assertEquals(-25, up.dy(8));
        assertTrue(kitten.framed(BackgroundEvents.KITTEN_HIDE) && kitten.framed(BackgroundEvents.KITTEN_THRONE));
        assertFalse(kitten.framed(BackgroundEvents.KITTEN_BAT));
        assertEquals(1, parrot.flightsPerSession);
    }

    @Test
    public void eachPixelPetHasItsOwnTrick() {
        assertEquals("roll", specialName(PET_PANDA));
        assertEquals("cheeks", specialName(PET_HAMSTER));
        assertEquals("bath", specialName(PET_CAPYBARA));
        assertEquals("talk", specialName(PET_PARROT));
        assertEquals("chase", specialName(PET_PUPPY));
        assertEquals("yarn", specialName(PET_KITTEN));
        for (int petType : PIXEL_PETS) {
            PetSpec spec = PetSpec.forType(petType);
            assertTrue(spec.iconRes != 0);
            assertTrue(spec.nameRes != 0 && spec.emojiRes != 0 && spec.foodBubbleRes != 0);
        }
    }

    @Test
    public void sizesFollowTheCharacters() {
        assertEquals(0.32f, PetSpec.forType(PET_PANDA).sizeRatio, 0f);
        assertEquals(0.30f, PetSpec.forType(PET_CAPYBARA).sizeRatio, 0f);
        assertEquals(0.24f, PetSpec.forType(PET_PARROT).sizeRatio, 0f);
        assertEquals(0.25f, PetSpec.forType(PET_HAMSTER).sizeRatio, 0f);
        assertEquals(0.26f, PetSpec.forType(PET_PUPPY).sizeRatio, 0f);
        assertEquals(0.22f, PetSpec.forType(PET_KITTEN).sizeRatio, 0f);
    }

    @Test
    public void thePuppyRunsAboutAndTheKittenSitsAndPlays() {
        PetSpec puppy = PetSpec.forType(PET_PUPPY);
        PetSpec kitten = PetSpec.forType(PET_KITTEN);
        // The puppy is the quickest of all (a step every 85 ms), the kitten goes at a middling pace
        assertTrue(puppy.walkSpeed() > kitten.walkSpeed());
        assertTrue(kitten.walkSpeed() > PetSpec.forType(PET_PANDA).walkSpeed());
        // The puppy walks and cheers up the most; the kitten sits, washes and plays
        assertEquals(ANIMATION_WALK, heaviest(puppy, -1));
        assertEquals(ANIMATION_HAPPY, heaviest(puppy, ANIMATION_WALK));
        assertEquals(ANIMATION_IDLE, heaviest(kitten, -1));
        assertEquals(ANIMATION_SPECIAL, heaviest(kitten, ANIMATION_IDLE));
        assertEquals(PetSpec.MIN_REPEATS, puppy.minRepeats);
        assertEquals(PetSpec.MAX_REPEATS, kitten.maxRepeats);
        assertEquals(R.drawable.prop_bubble_food_dog, puppy.foodBubbleRes);
        assertEquals(R.drawable.prop_bubble_food_cat, kitten.foodBubbleRes);
        assertEquals(PET_PUPPY, petTypeFromName("puppy"));
        assertEquals(PET_KITTEN, petTypeFromName("kitten"));
    }

    /** The activity with the biggest weight, leaving one out (-1: none). */
    private static int heaviest(PetSpec spec, int except) {
        PetSpec.Activity best = null;
        for (PetSpec.Activity activity : spec.activities) {
            if (activity.animation != except && (best == null || activity.weight > best.weight)) {
                best = activity;
            }
        }
        return best.animation;
    }

    @Test
    public void everyAnimationFitsTheWidget() {
        for (int petType : PetSpec.allPetTypes()) {
            PetSpec spec = PetSpec.forType(petType);
            for (int animation : spec.animationTypes) {
                PetSpec.Anim anim = spec.animation(animation);
                assertTrue(anim.intervalMs > 0);
                for (int frame : anim.frames) {
                    assertTrue(frame != 0);
                }
                // The widget shows at most MAX_LOOP_FRAMES frames per loop (a scene a few more)
                assertTrue(anim.frames.length <= spec.maxLoopFrames());
            }
        }
    }

    @Test
    public void characterShowsInTheTiming() {
        int pandaStep = PetSpec.forType(PET_PANDA).walkStepMs();
        int catStep = PetSpec.forType(PET_CAT).walkStepMs();
        int hamsterStep = PetSpec.forType(PET_HAMSTER).walkStepMs();
        assertTrue("the panda waddles slower than the cat", pandaStep > catStep);
        assertTrue("the hamster scurries faster than the cat", hamsterStep < catStep);
        assertTrue(PetSpec.forType(PET_PANDA).animation(ANIMATION_IDLE).intervalMs
            > PetSpec.forType(PET_HAMSTER).animation(ANIMATION_IDLE).intervalMs);
        int capybaraStep = PetSpec.forType(PET_CAPYBARA).walkStepMs();
        int parrotStep = PetSpec.forType(PET_PARROT).walkStepMs();
        assertTrue("the capybara strolls slower than the cat", capybaraStep > catStep);
        assertTrue("the parrot hops slower than the hamster", parrotStep > hamsterStep);
        assertTrue("the parrot hops faster than the slow pets", parrotStep < pandaStep && parrotStep < capybaraStep);
    }

    // --- Integer scaling ------------------------------------------------------------------------

    @Test
    public void pixelArtSnapsToWholeScales() {
        assertEquals(64, pixelSnappedSize(10f, 64));     // never below 1x
        assertEquals(64, pixelSnappedSize(95f, 64));
        assertEquals(128, pixelSnappedSize(124f, 64));
        assertEquals(192, pixelSnappedSize(166f, 64));
        assertEquals(256, pixelSnappedSize(252f, 64));
        for (int desired = 160; desired <= 1200; desired++) {
            int snapped = pixelSnappedSize(desired, 64);
            assertEquals(0, snapped % 64);
            // From 2.5x upwards the nearest whole scale is always within 20 %
            assertTrue("desired " + desired, Math.abs(snapped - desired) <= 0.2f * desired);
        }
    }

    private static void assertAnim(PetSpec spec, int animation, int[] frames, int intervalMs) {
        assertSame(frames, spec.animation(animation).frames);
        assertEquals(intervalMs, spec.animation(animation).intervalMs);
    }

    private static void assertSmoothPet(PetSpec spec, boolean facesRight) {
        assertEquals(0.30f, spec.sizeRatio, 0f);
        assertEquals(130, spec.walkStepMs());
        assertEquals(facesRight, spec.facesRight);
        assertEquals(0, spec.pixelGrid);
    }

    private static void assertCounts(PetSpec spec, int... counts) {
        for (int i = 0; i < counts.length; i++) {
            int animation = spec.animationTypes[i];
            assertEquals("animation " + animation, counts[i], spec.animation(animation).frames.length);
        }
    }

    private static void assertReactions(PetSpec spec, int wake, int joy) {
        assertEquals(wake, spec.wakeAnimation);
        assertEquals(joy, spec.joyAnimation);
    }

    private static void assertActivities(int petType, int... animations) {
        PetSpec.Activity[] activities = PetSpec.forType(petType).activities;
        assertEquals(animations.length, activities.length);
        for (int i = 0; i < animations.length; i++) {
            assertEquals(animations[i], activities[i].animation);
        }
    }

    private static PetSpec.Activity activity(int petType, int animation) {
        for (PetSpec.Activity activity : PetSpec.forType(petType).activities) {
            if (activity.animation == animation) {
                return activity;
            }
        }
        throw new AssertionError("no activity " + animation);
    }

    /** Name of the special animation in the sprite generator, read back through the accessory lookup. */
    private static String specialName(int petType) {
        PetSpec spec = PetSpec.forType(petType);
        for (String name : new String[] {"roll", "cheeks", "bath", "talk", "chase", "yarn"}) {
            if (GeneratedPetFrames.frames(spec.generatedName, name) == spec.animation(ANIMATION_SPECIAL).frames) {
                return name;
            }
        }
        return null;
    }

    private static void assertFrames(PetSpec spec, int animation, int count, int intervalMs) {
        assertEquals(count, spec.animation(animation).frames.length);
        assertEquals(intervalMs, spec.animation(animation).intervalMs);
    }
}
