package com.example.pixelpetwidget;

import static com.example.pixelpetwidget.Backdrop.MAPLE_PARK;
import static com.example.pixelpetwidget.Backdrop.PUMPKIN_FIELD;
import static com.example.pixelpetwidget.Backdrop.RAINY_EVENING;
import static com.example.pixelpetwidget.PixelCatWidget.*;

/**
 * What each pet does with each pixel-art background: one interaction per pet and background, all
 * in {@link #TABLE}. While a pet is awake on such a background, now and then (see
 * {@link #MIN_GAP_MS}, {@link #MAX_PER_SESSION}) it notices something there and plays with it:
 *
 * <ol>
 * <li>it notices: a prop catches its eye (it looks at it for a moment, then walks or flies over),
 *     or something comes to it where it is (a leaf drifts down, rain falls);</li>
 * <li>the interaction itself ({@link Interaction#act}): the pet's animation while the background
 *     does its part ({@link Interaction#effect}: the falling leaf, the prop's other state, drops);</li>
 * <li>the punchline ({@link Interaction#then}), after which its life goes on as usual with
 *     something else.</li>
 * </ol>
 *
 * Every part plays 2–4 times like any activity (the walk over takes the steps it needs); the
 * event as a whole counts as one activity, played once. PetBehavior plans it, PixelCatWidget
 * draws the effects.
 *
 * <p><b>Adding a pet</b>: add its three rows to the table, one per background (a pet without rows
 * simply has no events). <b>Adding a background</b>: draw it in tools/sprite_gen (bg_*.py), give it
 * a bg_type in {@link Backdrop}, then add one row per pet here. Pixel pets may have poses of their
 * own for their interactions ({@link PixelCatWidget#ANIMATION_POSE_1}..4, see PetSpec).
 */
final class BackgroundEvents {

    /** At least this long between two events of one pet. */
    static final int MIN_GAP_MS = 25_000;
    /** At most this many events in one awake spell. */
    static final int MAX_PER_SESSION = 2;
    /** Pseudo animation id of an event in the activity choice (never drawn as such). */
    static final int ANIMATION_EVENT = 11;
    /** How long a pet looks at a prop before it goes there. */
    static final int NOTICE_MS = 3_500;

    // What the background does during the interaction
    static final int NONE = 0;
    /** A leaf comes loose from the canopy and drifts down to the pet ({@link Interaction#anchorX}). */
    static final int LEAF = 1;
    /** The prop shows another state ({@link Interaction#state}), for as long as {@link Interaction#timing} says. */
    static final int STATE = 2;
    /** Raindrops fall into the pet's open mouth, one after another. */
    static final int DROPS = 3;

    // How long a prop's state lasts
    /** One pass of its frames, then its ambient state again. */
    static final int ONCE = 0;
    /** While the pet's interaction lasts. */
    static final int ACT = 1;
    /** Until the end of the update (the punchline included). */
    static final int PLAN = 2;

    /** The leaf lands on the ground in front of the pet and stays there. */
    static final int GROUND = -1;

    /** Frame interval of the effects. */
    static final int PROP_INTERVAL_MS = 100;
    /** Frames an event's leaf takes to fall, whatever the widget's size (3 s). */
    static final int LEAF_FALL_FRAMES = 30;
    /** How far a leaf on the ground lands in front of the pet, as a share of the pet's size. */
    static final float LEAF_IN_FRONT = 0.42f;
    /** A moment the pet goes on watching after the effect is over. */
    static final int AFTER_EFFECT_MS = 800;
    /** Frames of one raindrop falling into the mouth, then frames until the next one (the panda's drink loop). */
    static final int DROP_FALL_FRAMES = 8;
    static final int DROP_GAP_FRAMES = 6;
    /** Rows above the mouth where a drop comes into view. */
    static final int DROP_FALL_ROWS = 56;

    // The pixel pets' own poses for their interactions
    static final int PANDA_FAN = ANIMATION_POSE_1;
    static final int PANDA_DRINK = ANIMATION_POSE_2;
    static final int PANDA_HUG = ANIMATION_POSE_3;
    static final int HAMSTER_NIBBLE = ANIMATION_POSE_1;
    static final int HAMSTER_UMBRELLA = ANIMATION_POSE_2;
    static final int HAMSTER_BALANCE = ANIMATION_POSE_3;
    static final int CAPYBARA_LEAF_HAT = ANIMATION_POSE_1;
    static final int CAPYBARA_LIE = ANIMATION_POSE_2;
    static final int PARROT_LEAF = ANIMATION_POSE_1;
    static final int PUPPY_FLOP = ANIMATION_POSE_1;
    static final int PUPPY_LOOK_UP = ANIMATION_POSE_2;
    static final int PUPPY_SHAKE = ANIMATION_POSE_3;
    static final int PUPPY_NOSE_PUSH = ANIMATION_POSE_4;
    static final int KITTEN_HIDE = ANIMATION_POSE_1;
    static final int KITTEN_POUNCE = ANIMATION_POSE_2;
    static final int KITTEN_BAT = ANIMATION_POSE_3;
    static final int KITTEN_THRONE = ANIMATION_POSE_4;

    /** One pet's interaction with one background; built row by row in {@link #TABLE}. */
    static final class Interaction {
        final int pet;
        final int background;
        /** How often it is chosen, against the pet's own activities' weights. */
        final int weight;
        /** What happens, in a few words (for the reader of this table). */
        final String what;
        /** The prop the pet goes to (and whose state may change), or null: it stays where it is. */
        String prop;
        /** Its animation while it looks at the prop before going there. */
        int notice = -1;
        /** Its animation during the interaction, while the effect plays. */
        int act = ANIMATION_SLEEP;
        int effect = NONE;
        /** For STATE: the prop's state, and how long it lasts (ONCE, ACT, PLAN). */
        String state;
        int timing = ONCE;
        /**
         * For LEAF and DROPS: where on the pet they end, in pixels of its 64 x 64 frame as drawn
         * (facing its own way); a leaf with {@link #GROUND} lies on the ground in front of it.
         * There the leaf goes into the pet's next pose, which shows it in its paws, on its head...
         */
        int anchorX = GROUND;
        int anchorY = GROUND;
        /** Its animation afterwards, the punchline. */
        int then = ANIMATION_SLEEP;

        private Interaction(int pet, int background, int weight, String what) {
            this.pet = pet;
            this.background = background;
            this.weight = weight;
            this.what = what;
        }

        /** It goes to {@code prop}, after looking at it with {@code notice}. */
        Interaction goesTo(String propName, int noticeAnimation) {
            prop = propName;
            notice = noticeAnimation;
            return this;
        }

        Interaction act(int animation) {
            act = animation;
            return this;
        }

        /** A leaf falls to (x, y) of the pet's frame, or to the ground in front of it (GROUND, GROUND). */
        Interaction leaf(int x, int y) {
            effect = LEAF;
            anchorX = x;
            anchorY = y;
            return this;
        }

        Interaction state(String name, int howLong) {
            effect = STATE;
            state = name;
            timing = howLong;
            return this;
        }

        /** Raindrops fall into the mouth at (x, y) of the pet's frame. */
        Interaction drops(int x, int y) {
            effect = DROPS;
            anchorX = x;
            anchorY = y;
            return this;
        }

        Interaction then(int animation) {
            then = animation;
            return this;
        }

        /** Its first animation: what the pet must not have done just before. */
        int first() {
            return prop != null ? notice : act;
        }
    }

    private static Interaction row(int pet, int background, int weight, String what) {
        return new Interaction(pet, background, weight, what);
    }

    // One row per pet and background. Weights are against the pet's own activities (PetSpec).
    static final Interaction[] TABLE = {
        // Maple park: leaves drift down from the two maples; a heap of leaves on the left
        row(PET_CAT, MAPLE_PARK, 30, "follows a falling leaf until it lands by its nose, then washes as if it never cared")
            .act(ANIMATION_SEAT).leaf(GROUND, GROUND).then(ANIMATION_LICK),
        row(PET_DOG, MAPLE_PARK, 30, "spots the heap of leaves, runs into it and sends it flying, then sits in the mess")
            .goesTo("heap", ANIMATION_LICK).act(ANIMATION_EAT).state("burst", ONCE).then(ANIMATION_LICK),
        row(PET_RABBIT, MAPLE_PARK, 30, "hears the heap rustle, hops over and finds a carrot in it")
            .goesTo("heap", ANIMATION_SEAT).act(ANIMATION_EAT).state("rustle", ACT).then(ANIMATION_SEAT),
        row(PET_PANDA, MAPLE_PARK, 25, "catches a falling leaf in its paws and fans itself with it")
            .act(ANIMATION_IDLE).leaf(27, 43).then(PANDA_FAN),
        row(PET_HAMSTER, MAPLE_PARK, 25, "catches a falling leaf in its paws and nibbles at it like a seed")
            .act(ANIMATION_IDLE).leaf(21, 36).then(HAMSTER_NIBBLE),
        row(PET_CAPYBARA, MAPLE_PARK, 25, "lets a falling leaf settle on its head and wears it, calm as ever")
            .act(ANIMATION_IDLE).leaf(13, 20).then(CAPYBARA_LEAF_HAT),
        row(PET_PARROT, MAPLE_PARK, 25, "catches a falling leaf in its beak and shows it off")
            .act(ANIMATION_IDLE).leaf(9, 28).then(PARROT_LEAF),

        // Rainy evening: rain over everything; a puddle in the middle, a leaf in a small one, an umbrella
        row(PET_CAT, RAINY_EVENING, 30, "steps into the puddle, splash! washes its wet paw and walks off")
            .goesTo("puddle", ANIMATION_SEAT).act(ANIMATION_LICK).state("splash", ONCE).then(ANIMATION_WALK),
        row(PET_DOG, RAINY_EVENING, 30, "barks at the puddle, jumps in with a splash, sits in it happily, then trots off")
            .goesTo("puddle", ANIMATION_EAT).act(ANIMATION_LICK).state("splash", ONCE).then(ANIMATION_WALK),
        row(PET_RABBIT, RAINY_EVENING, 30, "hides from the rain under the umbrella and has a snack there")
            .goesTo("umbrella", ANIMATION_EAT).act(ANIMATION_SEAT).then(ANIMATION_EAT),
        row(PET_PANDA, RAINY_EVENING, 25, "opens its mouth to the rain and catches drops, then cheers")
            .act(PANDA_DRINK).drops(25, 30).then(ANIMATION_HAPPY),
        row(PET_HAMSTER, RAINY_EVENING, 25, "fishes the leaf out of the little puddle and holds it up as an umbrella")
            .goesTo("puddle_leaf", ANIMATION_IDLE).act(HAMSTER_UMBRELLA).state("empty", PLAN).then(ANIMATION_HAPPY),
        row(PET_CAPYBARA, RAINY_EVENING, 25, "lies down in the big puddle as if it were a warm spring")
            .goesTo("puddle", ANIMATION_IDLE).act(CAPYBARA_LIE).state("splash", ONCE).then(ANIMATION_IDLE),
        row(PET_PARROT, RAINY_EVENING, 25, "flies under the umbrella, shakes the rain off and chatters")
            .goesTo("umbrella", ANIMATION_IDLE).act(ANIMATION_HAPPY).then(ANIMATION_SPECIAL),

        // Pumpkin field: a big pumpkin in front, a carrot bed, a small pumpkin, a scarecrow
        row(PET_CAT, PUMPKIN_FIELD, 30, "hides behind the big pumpkin, only its ears showing, then strolls out")
            .goesTo("pumpkin_big", ANIMATION_LICK).act(ANIMATION_SEAT).then(ANIMATION_WALK),
        row(PET_DOG, PUMPKIN_FIELD, 30, "sniffs at the scarecrow and barks; the scarecrow shakes and the dog runs off")
            .goesTo("scarecrow", ANIMATION_LICK).act(ANIMATION_EAT).state("shake", ACT).then(ANIMATION_WALK),
        row(PET_RABBIT, PUMPKIN_FIELD, 30, "pulls a carrot out of the bed and munches it")
            .goesTo("bed", ANIMATION_SEAT).act(ANIMATION_EAT).state("eaten", PLAN).then(ANIMATION_SEAT),
        row(PET_PANDA, PUMPKIN_FIELD, 25, "goes to the big pumpkin and hugs it, then cheers")
            .goesTo("pumpkin_big", ANIMATION_IDLE).act(PANDA_HUG).then(ANIMATION_HAPPY),
        row(PET_HAMSTER, PUMPKIN_FIELD, 25, "climbs onto the little pumpkin and keeps its balance as it wobbles, then stuffs its cheeks")
            .goesTo("pumpkin_small", ANIMATION_IDLE).act(HAMSTER_BALANCE).state("wobble", ACT).then(ANIMATION_SPECIAL),
        row(PET_CAPYBARA, PUMPKIN_FIELD, 25, "lies down on the warm field in the low sun, then nibbles grass")
            .act(CAPYBARA_LIE).then(ANIMATION_EAT),
        row(PET_PARROT, PUMPKIN_FIELD, 25, "flies over to the scarecrow and chatters at it; the scarecrow shakes")
            .goesTo("scarecrow", ANIMATION_IDLE).act(ANIMATION_SPECIAL).state("shake", ACT).then(ANIMATION_HAPPY),

        // Puppy and kitten (added later): their rows for each background
        row(PET_PUPPY, MAPLE_PARK, 30, "spins round and round after a falling leaf, then flops over on its back, paws up")
            .act(ANIMATION_SPECIAL).leaf(GROUND, GROUND).then(PUPPY_FLOP),
        row(PET_PUPPY, RAINY_EVENING, 30, "looks up into the rain, drops land on its nose, it sneezes and shakes itself dry")
            .act(PUPPY_LOOK_UP).drops(22, 29).then(PUPPY_SHAKE),
        row(PET_PUPPY, PUMPKIN_FIELD, 30, "noses the little pumpkin along, it wobbles, then the pup jumps for joy")
            .goesTo("pumpkin_small", ANIMATION_IDLE).act(PUPPY_NOSE_PUSH).state("wobble", ACT).then(ANIMATION_HAPPY),
        row(PET_KITTEN, MAPLE_PARK, 30, "hides in the heap of leaves, only its ears showing, then pounces on a falling leaf")
            .goesTo("heap", ANIMATION_IDLE).act(KITTEN_HIDE).leaf(GROUND, GROUND).then(KITTEN_POUNCE),
        row(PET_KITTEN, RAINY_EVENING, 30, "sits under the umbrella and swipes at the drips running off its edge, then washes")
            .goesTo("umbrella", ANIMATION_IDLE).act(KITTEN_BAT).then(ANIMATION_IDLE),
        row(PET_KITTEN, PUMPKIN_FIELD, 30, "climbs onto the big pumpkin and lies on it like a queen on her throne, washing")
            .goesTo("pumpkin_big", ANIMATION_IDLE).act(KITTEN_THRONE).then(ANIMATION_HAPPY),
    };

    private BackgroundEvents() {
    }

    /** The interaction of a pet with a background, or null (no background, or none for this pet). */
    static Interaction find(int petType, int bgType) {
        for (Interaction interaction : TABLE) {
            if (interaction.pet == petType && interaction.background == bgType) {
                return interaction;
            }
        }
        return null;
    }

    /** Row of an interaction in {@link #TABLE}, -1 for none. */
    static int indexOf(Interaction interaction) {
        for (int i = 0; i < TABLE.length; i++) {
            if (TABLE[i] == interaction) {
                return i;
            }
        }
        return -1;
    }

    /** The row {@code index} if it is the interaction of this pet on this background, else null. */
    static Interaction at(int index, int petType, int bgType) {
        if (index < 0 || index >= TABLE.length) {
            return null;
        }
        Interaction interaction = TABLE[index];
        return interaction.pet == petType && interaction.background == bgType ? interaction : null;
    }

    /** How long the effect needs before the punchline may come (0: none). */
    static long effectMs(Interaction interaction, Backdrop backdrop) {
        switch (interaction.effect) {
            case LEAF:
                return (long) LEAF_FALL_FRAMES * PROP_INTERVAL_MS + AFTER_EFFECT_MS;
            case DROPS:
                return 2L * (DROP_FALL_FRAMES + DROP_GAP_FRAMES) * PROP_INTERVAL_MS;
            case STATE:
                if (interaction.timing == ONCE && backdrop != null) {
                    Backdrop.Prop prop = backdrop.prop(interaction.prop);
                    if (prop != null) {
                        return prop.state(interaction.state).loopMs() + AFTER_EFFECT_MS;
                    }
                }
                return 0;
            default:
                return 0;
        }
    }

    // --- The falling leaf ------------------------------------------------------------------

    /** One frame of a falling leaf: its top-left corner in canvas pixels and which sprite it shows. */
    static final class LeafPose {
        final int x;
        final int y;
        /** 0–3: a frame of the leaf turning; -1: lying on the ground. */
        final int sprite;

        LeafPose(int x, int y, int sprite) {
            this.x = x;
            this.y = y;
            this.sprite = sprite;
        }
    }

    /**
     * A leaf's way down from the canopy at (fromX, fromY) to (toX, toY) in {@code frames} frames,
     * swaying from side to side and turning as it sways, a little slower at first; one more pose
     * at the end lies on the ground.
     */
    static LeafPose[] fall(int fromX, int fromY, int toX, int toY, int sway, boolean swayRight, int frames) {
        LeafPose[] poses = new LeafPose[frames + 1];
        double turns = 1.5;
        for (int i = 0; i < frames; i++) {
            double t = (double) i / (frames - 1);
            double swing = Math.sin(t * turns * 2 * Math.PI) * (1 - 0.3 * t);
            double x = fromX + (toX - fromX) * t + (swayRight ? sway : -sway) * swing;
            double y = fromY + (toY - fromY) * (0.25 * t + 0.75 * t * t * (3 - 2 * t));
            double turning = Math.cos(t * turns * 2 * Math.PI);
            int sprite = Math.abs(turning) > 0.7 ? 0 : Math.abs(turning) > 0.25 ? (turning > 0 ? 1 : 3) : 2;
            poses[i] = new LeafPose((int) Math.round(x), (int) Math.round(y), sprite);
        }
        poses[frames] = new LeafPose(toX, toY, -1);
        return poses;
    }
}
