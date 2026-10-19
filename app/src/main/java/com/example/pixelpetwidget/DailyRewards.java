package com.example.pixelpetwidget;

import java.util.HashSet;
import java.util.Set;

/**
 * Daily gift and streak. Opening the app once a day gives treats; coming back on
 * consecutive days builds a streak with bonus treats, a streak freeze each full week
 * and the bow on the first full week. Missed days are covered by saved freezes.
 */
final class DailyRewards {

    static final long NEVER = Long.MIN_VALUE;
    static final int DAILY_TREATS = 3;
    static final int MAX_TREATS = 15;
    static final int MAX_FREEZES = 2;
    static final int WEEK = 7;
    /** Days away after which the pet welcomes the player back with extra treats. */
    static final int WELCOME_BACK_AFTER_DAYS = 3;
    static final int WELCOME_BACK_TREATS = 3;
    static final String STREAK_ITEM = Wardrobe.BOW;

    /** Everything the daily gift remembers. */
    static final class State {
        int treats = DAILY_TREATS;      // a first handful so the player can feed right away
        long lastClaimDay = NEVER;
        int streak;
        int freezes;
        final Set<String> unlocked = new HashSet<>();
    }

    /** What one claim gave, for the message in the app. */
    static final class Claim {
        int treats;
        int bonusTreats;
        int streak;
        int freezesUsed;
        boolean freezeEarned;
        boolean streakReset;
        boolean welcomeBack;
        String unlockedItem;
    }

    private DailyRewards() {}

    static boolean canClaim(State state, long today) {
        return state.lastClaimDay != today;
    }

    /** The streak as it stands today: what it will continue from, or 0 if it is lost. */
    static int currentStreak(State state, long today) {
        if (state.lastClaimDay == NEVER) return 0;
        long missed = today - state.lastClaimDay - 1;
        if (missed <= 0 || missed <= state.freezes) return state.streak;
        return 0;
    }

    /** Day of the weekly calendar (1..7) that today's claim fills. */
    static int dayInWeekToClaim(State state, long today) {
        return (currentStreak(state, today) % WEEK) + 1;
    }

    static Claim claim(State state, long today) {
        if (!canClaim(state, today)) {
            throw new IllegalStateException("already claimed today");
        }
        Claim claim = new Claim();
        long gap = state.lastClaimDay == NEVER ? Long.MAX_VALUE : today - state.lastClaimDay;
        long missed = gap - 1;
        if (state.lastClaimDay == NEVER) {
            state.streak = 1;
        } else if (missed <= 0) {
            state.streak += 1;
        } else if (missed <= state.freezes) {
            state.freezes -= (int) missed;
            claim.freezesUsed = (int) missed;
            state.streak += 1;
        } else {
            claim.streakReset = state.streak > 0;
            state.streak = 1;
        }
        claim.welcomeBack = state.lastClaimDay != NEVER && gap >= WELCOME_BACK_AFTER_DAYS;

        int dayInWeek = ((state.streak - 1) % WEEK) + 1;
        claim.treats = DAILY_TREATS;
        if (dayInWeek == 3 || dayInWeek == 5) {
            claim.bonusTreats += 2;
        }
        if (dayInWeek == WEEK) {
            claim.bonusTreats += 3;
            if (state.freezes < MAX_FREEZES) {
                state.freezes += 1;
                claim.freezeEarned = true;
            }
            if (!state.unlocked.contains(STREAK_ITEM)) {
                state.unlocked.add(STREAK_ITEM);
                claim.unlockedItem = STREAK_ITEM;
            }
        }
        if (claim.welcomeBack) {
            claim.bonusTreats += WELCOME_BACK_TREATS;
        }
        state.treats = Math.min(MAX_TREATS, state.treats + claim.treats + claim.bonusTreats);
        state.lastClaimDay = today;
        claim.streak = state.streak;
        return claim;
    }

    /** Spends one treat; false if there are none left. */
    static boolean spendTreat(State state) {
        if (state.treats <= 0) return false;
        state.treats -= 1;
        return true;
    }
}
