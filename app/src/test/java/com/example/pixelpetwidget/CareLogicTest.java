package com.example.pixelpetwidget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

public class CareLogicTest {

    private static final long HOUR = DayClock.HOUR_MS;
    private static final long T0 = 1_760_000_000_000L;

    // --- Food and mood ---------------------------------------------------------------------

    @Test
    public void levelsDropOverTime() {
        assertEquals(100f, PetCare.level(T0, T0, PetCare.HUNGER_PER_HOUR), 0.01f);
        assertEquals(75f, PetCare.level(T0, T0 + 10 * HOUR, PetCare.HUNGER_PER_HOUR), 0.01f);
        assertEquals(0f, PetCare.level(T0, T0 + 100 * HOUR, PetCare.HUNGER_PER_HOUR), 0.01f);
        // A clock set back never makes a pet fuller than full
        assertEquals(100f, PetCare.level(T0, T0 - 5 * HOUR, PetCare.HUNGER_PER_HOUR), 0.01f);
    }

    @Test
    public void feedingRaisesFoodAndCapsAtFull() {
        long hungry = T0 - 30 * HOUR;                     // 25 left
        long fed = PetCare.raise(hungry, T0, PetCare.FEED_AMOUNT, PetCare.HUNGER_PER_HOUR);
        assertEquals(85f, PetCare.level(fed, T0, PetCare.HUNGER_PER_HOUR), 0.01f);
        long overfed = PetCare.raise(fed, T0, PetCare.FEED_AMOUNT, PetCare.HUNGER_PER_HOUR);
        assertEquals(100f, PetCare.level(overfed, T0, PetCare.HUNGER_PER_HOUR), 0.01f);
    }

    @Test
    public void onePetGetsHungryAboutOnceADay() {
        long hungryAt = PetCare.whenBelow(T0, PetCare.HUNGRY_BELOW, PetCare.HUNGER_PER_HOUR);
        assertEquals(24 * HOUR, hungryAt - T0);
        // A treat a day keeps up with a day of hunger
        assertTrue(PetCare.FEED_AMOUNT >= 24 * PetCare.HUNGER_PER_HOUR);
    }

    @Test
    public void hungerComesBeforeLoneliness() {
        assertSame(PetCare.Need.NONE, PetCare.need(80, 80));
        assertSame(PetCare.Need.HUNGRY, PetCare.need(10, 10));
        assertSame(PetCare.Need.LONELY, PetCare.need(80, 10));
        assertSame(PetCare.Need.HUNGRY, PetCare.need(PetCare.HUNGRY_BELOW - 1, 100));
        assertSame(PetCare.Need.NONE, PetCare.need(PetCare.HUNGRY_BELOW, PetCare.LONELY_BELOW));
    }

    // --- Daily gift ------------------------------------------------------------------------

    @Test
    public void firstGiftStartsTheStreak() {
        DailyRewards.State state = new DailyRewards.State();
        assertTrue(DailyRewards.canClaim(state, 100));
        DailyRewards.Claim claim = DailyRewards.claim(state, 100);
        assertEquals(1, claim.streak);
        assertEquals(DailyRewards.DAILY_TREATS * 2, state.treats); // starter treats plus the gift
        assertFalse(DailyRewards.canClaim(state, 100));
        assertTrue(DailyRewards.canClaim(state, 101));
    }

    @Test
    public void aFullWeekGivesBonusesAFreezeAndTheBow() {
        DailyRewards.State state = new DailyRewards.State();
        state.treats = 0;
        int total = 0;
        DailyRewards.Claim claim = null;
        for (long day = 1; day <= 7; day++) {
            claim = DailyRewards.claim(state, day);
            total += claim.treats + claim.bonusTreats;
        }
        assertEquals(7, claim.streak);
        assertTrue(claim.freezeEarned);
        assertEquals(Wardrobe.BOW, claim.unlockedItem);
        assertEquals(1, state.freezes);
        assertEquals(7 * 3 + 2 + 2 + 3, total);
        assertEquals(Math.min(DailyRewards.MAX_TREATS, total), state.treats);
        // The bow comes only once
        for (long day = 8; day <= 14; day++) {
            claim = DailyRewards.claim(state, day);
        }
        assertNull(claim.unlockedItem);
        assertEquals(2, state.freezes);
    }

    @Test
    public void freezesCoverMissedDays() {
        DailyRewards.State state = new DailyRewards.State();
        DailyRewards.claim(state, 10);
        DailyRewards.claim(state, 11);
        state.freezes = 1;
        assertEquals(2, DailyRewards.currentStreak(state, 13));      // one day missed, one freeze
        DailyRewards.Claim claim = DailyRewards.claim(state, 13);
        assertEquals(3, claim.streak);
        assertEquals(1, claim.freezesUsed);
        assertEquals(0, state.freezes);
        assertFalse(claim.streakReset);
    }

    @Test
    public void missingTooManyDaysStartsANewStreakWithAWelcome() {
        DailyRewards.State state = new DailyRewards.State();
        for (long day = 1; day <= 4; day++) {
            DailyRewards.claim(state, day);
        }
        assertEquals(0, DailyRewards.currentStreak(state, 10));
        DailyRewards.Claim claim = DailyRewards.claim(state, 10);
        assertEquals(1, claim.streak);
        assertTrue(claim.streakReset);
        assertTrue(claim.welcomeBack);
        assertEquals(DailyRewards.WELCOME_BACK_TREATS, claim.bonusTreats);
    }

    @Test
    public void treatsAreCappedAndSpentOneByOne() {
        DailyRewards.State state = new DailyRewards.State();
        state.treats = DailyRewards.MAX_TREATS;
        DailyRewards.claim(state, 1);
        assertEquals(DailyRewards.MAX_TREATS, state.treats);
        state.treats = 1;
        assertTrue(DailyRewards.spendTreat(state));
        assertFalse(DailyRewards.spendTreat(state));
        assertEquals(0, state.treats);
    }

    @Test
    public void calendarShowsTheDayToClaim() {
        DailyRewards.State state = new DailyRewards.State();
        assertEquals(1, DailyRewards.dayInWeekToClaim(state, 1));
        for (long day = 1; day <= 6; day++) {
            DailyRewards.claim(state, day);
        }
        assertEquals(7, DailyRewards.dayInWeekToClaim(state, 7));
        DailyRewards.claim(state, 7);
        assertEquals(1, DailyRewards.dayInWeekToClaim(state, 8));
    }

    // --- Seasons and wardrobe --------------------------------------------------------------

    @Test
    public void seasonsHaveClearEdges() {
        assertNull(Wardrobe.season(10, 14));
        assertEquals(Wardrobe.SEASON_HALLOWEEN, Wardrobe.season(10, 15));
        assertEquals(Wardrobe.SEASON_HALLOWEEN, Wardrobe.season(11, 10));
        assertNull(Wardrobe.season(11, 11));
        assertEquals(Wardrobe.SEASON_WINTER, Wardrobe.season(12, 15));
        assertEquals(Wardrobe.SEASON_WINTER, Wardrobe.season(1, 10));
        assertNull(Wardrobe.season(1, 11));
        assertNull(Wardrobe.season(7, 1));
    }

    @Test
    public void seasonalHatsOnlyInTheirSeasonAndOnce() {
        Set<String> unlocked = new HashSet<>();
        String october = Wardrobe.season(10, 20);
        assertTrue(Wardrobe.canPickUp(Wardrobe.HALLOWEEN_HAT, unlocked, october));
        assertFalse(Wardrobe.canPickUp(Wardrobe.HALLOWEEN_HAT, unlocked, Wardrobe.season(12, 20)));
        assertFalse(Wardrobe.canPickUp(Wardrobe.HALLOWEEN_HAT, unlocked, null));
        assertFalse(Wardrobe.canPickUp(Wardrobe.WINTER_HAT, unlocked, october));
        assertFalse(Wardrobe.canPickUp(Wardrobe.BOW, unlocked, october));   // the bow comes from the streak
        unlocked.add(Wardrobe.HALLOWEEN_HAT);
        assertFalse(Wardrobe.canPickUp(Wardrobe.HALLOWEEN_HAT, unlocked, october));
    }

    @Test
    public void onlyPixelPetsWearAccessories() {
        assertTrue(Wardrobe.supports(PixelCatWidget.PET_PANDA));
        assertTrue(Wardrobe.supports(PixelCatWidget.PET_HAMSTER));
        assertTrue(Wardrobe.supports(PixelCatWidget.PET_CAPYBARA));
        assertTrue(Wardrobe.supports(PixelCatWidget.PET_PARROT));
        assertFalse(Wardrobe.supports(PixelCatWidget.PET_CAT));
        assertFalse(Wardrobe.supports(PixelCatWidget.PET_DOG));
        assertFalse(Wardrobe.supports(PixelCatWidget.PET_RABBIT));
    }

    @Test
    public void everyAccessoryHasFramesForEveryAnimationButTheRollAndTheBath() {
        int[] cycle = {PixelCatWidget.ANIMATION_SLEEP, PixelCatWidget.ANIMATION_HAPPY, PixelCatWidget.ANIMATION_IDLE,
            PixelCatWidget.ANIMATION_EAT, PixelCatWidget.ANIMATION_SPECIAL, PixelCatWidget.ANIMATION_WALK};
        int[] pets = {PixelCatWidget.PET_PANDA, PixelCatWidget.PET_HAMSTER, PixelCatWidget.PET_CAPYBARA,
            PixelCatWidget.PET_PARROT};
        for (int petType : pets) {
            PetSpec spec = PetSpec.forType(petType);
            for (String item : Wardrobe.ITEMS) {
                assertTrue(Wardrobe.iconRes(petType, item) != 0);
                for (int animation : cycle) {
                    PetSpec.Anim plain = spec.animation(animation);
                    PetSpec.Anim worn = spec.animation(animation, item);
                    assertEquals(plain.frames.length, worn.frames.length);
                    assertEquals(plain.intervalMs, worn.intervalMs);
                    // Upside down in a roll or in the bath with an orange on top, the pet wears nothing
                    boolean bare = animation == PixelCatWidget.ANIMATION_SPECIAL
                        && (petType == PixelCatWidget.PET_PANDA || petType == PixelCatWidget.PET_CAPYBARA);
                    if (bare) {
                        assertSame(plain.frames, worn.frames);
                    } else {
                        assertNotSame(plain.frames, worn.frames);
                    }
                }
            }
        }
        // Old pets ignore accessories
        PetSpec cat = PetSpec.forType(PixelCatWidget.PET_CAT);
        assertSame(cat.animation(PixelCatWidget.ANIMATION_SLEEP).frames,
            cat.animation(PixelCatWidget.ANIMATION_SLEEP, Wardrobe.BOW).frames);
    }

    @Test
    public void everyPetHasAFoodBubbleAndAName() {
        for (int petType : PetSpec.allPetTypes()) {
            PetSpec spec = PetSpec.forType(petType);
            assertTrue(spec.foodBubbleRes != 0);
            assertTrue(spec.nameRes != 0);
            assertTrue(spec.emojiRes != 0);
            assertTrue(spec.iconRes != 0);
        }
    }

    // --- Local days ------------------------------------------------------------------------

    @Test
    public void localDaysFollowTheTimeZone() {
        TimeZone moscow = TimeZone.getTimeZone("GMT+03:00");
        long midnightMoscow = 1_760_043_600_000L;      // 2025-10-10 00:00 in GMT+3
        assertEquals(DayClock.dayNumber(midnightMoscow - 1, moscow) + 1, DayClock.dayNumber(midnightMoscow, moscow));
        assertEquals(0, DayClock.hourOfDay(midnightMoscow, moscow));
        assertEquals(midnightMoscow + 19 * HOUR, DayClock.nextTimeAt(midnightMoscow, 19, moscow));
        assertEquals(midnightMoscow + 24 * HOUR, DayClock.nextTimeAt(midnightMoscow, 0, moscow));
        int[] date = DayClock.monthAndDay(midnightMoscow, moscow);
        assertEquals(10, date[0]);
        assertEquals(10, date[1]);
        assertNotNull(date);
    }
}
