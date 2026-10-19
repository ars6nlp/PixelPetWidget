package com.example.pixelpetwidget;

import java.util.Set;

/**
 * Accessories for the pixel-art pets and the seasons that bring them. The bow comes with
 * the first full week of the daily streak; the seasonal hats can be picked up for free
 * while their season lasts and are kept forever.
 */
final class Wardrobe {

    static final String BOW = "bow";
    static final String HALLOWEEN_HAT = "halloween_hat";
    static final String WINTER_HAT = "winter_hat";
    static final String[] ITEMS = {BOW, HALLOWEEN_HAT, WINTER_HAT};

    static final String SEASON_HALLOWEEN = "halloween";
    static final String SEASON_WINTER = "winter";

    private Wardrobe() {}

    /** Accessories are drawn into the pixel-art frames, so only those pets can wear them (a scene can't). */
    static boolean supports(int petType) {
        PetSpec spec = PetSpec.forType(petType);
        return spec.generatedName != null && !spec.scene;
    }

    /** Season on the given day, or null: Halloween 15 Oct - 10 Nov, winter 15 Dec - 10 Jan. */
    static String season(int month, int day) {
        if ((month == 10 && day >= 15) || (month == 11 && day <= 10)) return SEASON_HALLOWEEN;
        if ((month == 12 && day >= 15) || (month == 1 && day <= 10)) return SEASON_WINTER;
        return null;
    }

    /** The season whose hat this is, or null for items that aren't seasonal. */
    static String seasonOf(String item) {
        if (HALLOWEEN_HAT.equals(item)) return SEASON_HALLOWEEN;
        if (WINTER_HAT.equals(item)) return SEASON_WINTER;
        return null;
    }

    static boolean isUnlocked(String item, Set<String> unlocked) {
        return unlocked.contains(item);
    }

    /** A seasonal hat can be picked up while its season lasts. */
    static boolean canPickUp(String item, Set<String> unlocked, String currentSeason) {
        String season = seasonOf(item);
        return season != null && season.equals(currentSeason) && !unlocked.contains(item);
    }

    static int nameRes(String item) {
        if (BOW.equals(item)) return R.string.item_bow;
        if (HALLOWEEN_HAT.equals(item)) return R.string.item_halloween_hat;
        return R.string.item_winter_hat;
    }

    /** Picture of the accessory for the wardrobe in the app. */
    static int iconRes(int petType, String item) {
        String pet = PetSpec.forType(petType).generatedName;
        return pet != null ? GeneratedPetFrames.accessoryIcon(pet, item) : 0;
    }

    /** How the player gets a locked item. */
    static int hintRes(String item) {
        if (BOW.equals(item)) return R.string.item_bow_hint;
        if (HALLOWEEN_HAT.equals(item)) return R.string.item_halloween_hint;
        return R.string.item_winter_hint;
    }
}
