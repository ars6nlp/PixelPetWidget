package com.example.pixelpetwidget;

import static com.example.pixelpetwidget.PixelCatWidget.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * How one pet looks and behaves in the widget: its animations with their frames and speed,
 * what it does on its own while awake (see {@link PetBehavior}), how fast it walks, how big it
 * is and which way it faces. Adding a pet means adding one entry to {@link #forType}.
 */
final class PetSpec {

    /**
     * Frames of one animation and the time each frame stays on screen; a pixel pet's frame may be
     * a pose drawn moved and mirrored (shifts: dx, dy in art pixels and 1 for mirrored, per frame;
     * null for none), so a jump is one picture at several heights and a spin shows both sides.
     */
    static final class Anim {
        final int[] frames;
        final int intervalMs;
        final int[] shifts;

        Anim(int[] frames, int intervalMs) {
            this(frames, intervalMs, null);
        }

        Anim(int[] frames, int intervalMs, int[] shifts) {
            this.frames = frames;
            this.intervalMs = intervalMs;
            this.shifts = shifts;
        }

        /** How far frame {@code frame} is drawn moved to the right (as drawn, facing its own way). */
        int dx(int frame) {
            return shifts != null ? shifts[3 * frame] : 0;
        }

        /** How far it is drawn moved down (negative: up). */
        int dy(int frame) {
            return shifts != null ? shifts[3 * frame + 1] : 0;
        }

        /** Whether it is drawn mirrored (the other side of a pose). */
        boolean flipped(int frame) {
            return shifts != null && shifts[3 * frame + 2] != 0;
        }

        /** Time one pass through the frames takes. */
        int loopMs() {
            return frames.length * intervalMs;
        }
    }

    /**
     * Something the pet does on its own while awake. The weight is how often it is chosen
     * compared with the pet's other activities. It plays its animation minRepeats..maxRepeats
     * times (a walk: its step cycles), a one-shot once.
     */
    static final class Activity {
        final int animation;
        final int weight;
        /** A one-shot (a roll, a flight, the chest): played once, then something else. */
        final boolean once;
        /** Animations right after which this activity is never chosen. */
        final int[] notAfter;

        Activity(int animation, int weight, boolean once, int[] notAfter) {
            this.animation = animation;
            this.weight = weight;
            this.once = once;
            this.notAfter = notAfter;
        }

        boolean notAfter(int previous) {
            for (int animation : notAfter) {
                if (animation == previous) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * How a pixel pet walks: the time each of its 8 walk frames stays on screen and how many art
     * pixels the pet moves forward in each of them. The pixels follow the paw that stands on the
     * ground in the drawing, so the paws never slide; their sum is the stride of one cycle.
     */
    static final class Gait {
        final int intervalMs;
        final int[] shifts;

        Gait(int intervalMs, int... shifts) {
            this.intervalMs = intervalMs;
            this.shifts = shifts;
        }

        /** Art pixels covered by one cycle of the 8 walk frames. */
        int stridePx() {
            int sum = 0;
            for (int shift : shifts) {
                sum += shift;
            }
            return sum;
        }

        /** Art pixels per second. */
        float speed() {
            return stridePx() * 1000f / (shifts.length * intervalMs);
        }

        /** Art pixels covered by the first {@code steps} walk frames. */
        int pixelsAfter(int steps) {
            int sum = 0;
            for (int i = 0; i < steps; i++) {
                sum += shifts[i % shifts.length];
            }
            return sum;
        }
    }

    // --- How fast the pixel pets move: every speed is here ------------------------------------
    // Gait(ms per walk frame, art pixels moved in each of the 8 walk frames). To make a pet walk
    // faster or slower change its ms: speed = stride / (8 x ms). The pixels belong to the drawing
    // (they follow the paw on the ground); change them only together with the walk frames.
    /** Panda: big and slow; 12 px a cycle, 9.4 px/s (a 4x4 widget crossed in ~11 s). */
    static final Gait PANDA_GAIT = new Gait(160, 2, 1, 2, 1, 2, 1, 2, 1);
    /** Hamster: quick little steps; 12 px a cycle, 16.7 px/s (~9 s). */
    static final Gait HAMSTER_GAIT = new Gait(90, 2, 1, 2, 1, 2, 1, 2, 1);
    /** Capybara: unhurried; 10 px a cycle, 6.9 px/s (~15 s). */
    static final Gait CAPYBARA_GAIT = new Gait(180, 1, 1, 2, 1, 1, 1, 2, 1);
    /** Parrot: hops; 8 px a cycle, 8.3 px/s (~17 s). */
    static final Gait PARROT_GAIT = new Gait(120, 0, 2, 2, 0, 0, 2, 2, 0);
    /** Puppy: trots; 14 px a cycle, 19.4 px/s (~7.5 s). */
    static final Gait PUPPY_GAIT = new Gait(90, 2, 2, 2, 1, 2, 2, 2, 1);
    /** Kitten: a medium pace; 12 px a cycle, 13.6 px/s (~11 s). */
    static final Gait KITTEN_GAIT = new Gait(110, 2, 1, 2, 1, 2, 1, 2, 1);
    /**
     * Shortest plans of the pets whose updates would otherwise come more often than before smooth
     * motion (updates per awake minute before it: capybara 6.4, parrot 8.4, hamster 9.3; now 6.3,
     * 8.4, 9.25; the new puppy 9.2, no more often than the busiest pet before it). Longer ones
     * would leave the pet standing still at the end of its plans. The others plan at least
     * PetBehavior.MIN_PLAN_MS.
     */
    static final int CAPYBARA_MIN_PLAN_MS = 6_500;
    static final int PARROT_MIN_PLAN_MS = 5_800;
    static final int HAMSTER_MIN_PLAN_MS = 5_200;
    static final int PUPPY_MIN_PLAN_MS = 5_300;
    /** Most art pixels a pixel pet may move between two frames on the ground, and in the air. */
    static final int MAX_SHIFT_PX = 3;
    static final int MAX_FLIGHT_SHIFT_PX = 6;
    /**
     * How far the wingbeat lifts or drops the parrot's body inside its frame between two frames:
     * the flight path keeps its frames this much closer, so the body itself never moves more than
     * MAX_FLIGHT_SHIFT_PX (any way, diagonally too).
     */
    static final int FLIGHT_BOB_PX = 1;
    /** Frame time of the parrot's take-off, wingbeat and landing. */
    static final int FLIGHT_INTERVAL_MS = 80;
    /**
     * Ids of the way into and out of an animation (a jump onto the pumpkin, a dive into the heap,
     * lying down to gnaw a bone): its own id plus these. They play once around its loops
     * (PetBehavior: an activity or an interaction's pose framed this way is a timeline).
     */
    static final int ENTER = 100;
    static final int LEAVE = 200;
    /** Most frames one loop of a pixel pet may have (the hand-drawn pets: MAX_LOOP_FRAMES). */
    static final int PIXEL_LOOP_FRAMES = 24;

    /** How long a pet stays awake after a tap before it falls asleep by itself. */
    static final int AWAKE_MS = 60_000;
    /**
     * How many times in a row an activity plays its animation before the pet does something else
     * (a walk counts its step cycles); a pet may set its own with {@link Builder#repeats}.
     */
    static final int MIN_REPEATS = 2;
    static final int MAX_REPEATS = 4;

    // Timings shared by the hand-drawn pets
    private static final int LOOP_INTERVAL_MS = 100;
    private static final int WALK_STEP_MS = 130;
    static final float DEFAULT_SIZE_RATIO = 0.30f;
    static final int PIXEL_GRID = 64;
    /** Longest loop of a scene (its fish need more frames to cross the tank smoothly). */
    static final int SCENE_LOOP_FRAMES = 32;
    /** Animations of the pixel-art pets; the special animation is each pet's own trick. */
    private static final int[] PIXEL_PET_ANIMATIONS = {ANIMATION_SLEEP, ANIMATION_HAPPY, ANIMATION_IDLE,
        ANIMATION_EAT, ANIMATION_SPECIAL, ANIMATION_WALK};

    final int petType;
    /** Animations the pet has, in the order they were registered; the first is sleep. */
    final int[] animationTypes;
    /** Time awake after a tap ({@link #AWAKE_MS} unless a pet sets its own). */
    final int awakeMs;
    /** Repeats of an activity ({@link #MIN_REPEATS}..{@link #MAX_REPEATS} unless a pet sets its own). */
    final int minRepeats;
    final int maxRepeats;
    /** What the pet does when a tap wakes it up. */
    final int wakeAnimation;
    /** What it does when tapped while awake. */
    final int joyAnimation;
    /** What it does on its own while awake. */
    final Activity[] activities;
    /** How many times it may fly in one awake spell (only pets with flight frames fly). */
    final int flightsPerSession;
    /** How a pixel pet walks (null: a hand-drawn pet, which walks the old way, see PetBehavior). */
    final Gait gait;
    /**
     * Shortest plan, so the shortest time between two updates (PetBehavior.MIN_PLAN_MS unless set):
     * a pet with short loops and walks in true steps plans a little longer, so its updates come no
     * more often than before smooth motion; longer still would leave it standing at the ends.
     */
    final int minPlanMs;
    /** Pet size relative to the widget's shorter side. */
    final float sizeRatio;
    /** Direction the sprites are drawn in; walking the other way mirrors them. */
    final boolean facesRight;
    /** Native frame size of pixel art scaled by whole numbers only; 0 for smooth artwork. */
    final int pixelGrid;
    /**
     * A whole scene instead of a pet on the widget's background (the aquarium): it has no
     * background of its own, fills the widget at the largest whole-number scale, never walks
     * sideways and may loop up to {@link #SCENE_LOOP_FRAMES} frames.
     */
    final boolean scene;
    /** Native frame size in pixels (pixel art): 64x64 for the pets, wider for a scene. */
    final int frameWidth;
    final int frameHeight;
    /** Name in tools/sprite_gen for generated pets, which can wear accessories; null otherwise. */
    final String generatedName;
    final int nameRes;
    final int emojiRes;
    /** Thought bubble with the pet's favourite food, shown on the widget when it is hungry. */
    final int foodBubbleRes;
    /** Picture of the pet for lists in the app. */
    final int iconRes;
    private final Map<Integer, Anim> animations;
    private final Map<Integer, String> generatedAnimations;

    private PetSpec(Builder b) {
        this.petType = b.petType;
        this.animationTypes = new int[b.order.size()];
        for (int i = 0; i < animationTypes.length; i++) {
            animationTypes[i] = b.order.get(i);
        }
        this.awakeMs = b.awakeMs;
        this.minRepeats = b.minRepeats;
        this.maxRepeats = b.maxRepeats;
        this.wakeAnimation = b.wakeAnimation;
        this.joyAnimation = b.joyAnimation;
        this.activities = b.activities.toArray(new Activity[0]);
        this.flightsPerSession = b.flightsPerSession;
        this.gait = b.gait;
        this.minPlanMs = b.minPlanMs;
        this.animations = b.animations;
        this.sizeRatio = b.sizeRatio;
        this.facesRight = b.facesRight;
        this.pixelGrid = b.pixelGrid;
        this.scene = b.scene;
        this.frameWidth = b.frameWidth;
        this.frameHeight = b.frameHeight;
        this.generatedName = b.generatedName;
        this.generatedAnimations = b.generatedAnimations;
        this.nameRes = b.nameRes;
        this.emojiRes = b.emojiRes;
        this.foodBubbleRes = b.foodBubbleRes;
        this.iconRes = b.iconRes;
    }

    /** The animation, or the sleep animation if this pet doesn't have it. */
    Anim animation(int animationType) {
        Anim anim = animations.get(animationType);
        return anim != null ? anim : animations.get(ANIMATION_SLEEP);
    }

    /** The animation with the pet wearing {@code accessory}; plain frames if there is no such variant. */
    Anim animation(int animationType, String accessory) {
        Anim plain = animation(animationType);
        if (accessory == null || generatedName == null) {
            return plain;
        }
        String name = generatedAnimations.get(animations.containsKey(animationType) ? animationType : ANIMATION_SLEEP);
        int[] frames = name != null ? GeneratedPetFrames.withAccessory(generatedName, name, accessory) : null;
        // A worn accessory moves with the pose
        return frames != null ? new Anim(frames, plain.intervalMs, plain.shifts) : plain;
    }

    /** Most frames one loop may have in the widget. */
    int maxLoopFrames() {
        return scene ? SCENE_LOOP_FRAMES : smooth() ? PIXEL_LOOP_FRAMES : PixelCatWidget.MAX_LOOP_FRAMES;
    }

    /**
     * Whether the pet moves the smooth way (the pixel-art pets and the aquarium): walks of whole
     * art pixels per frame, transitions, plans that never jump back. The hand-drawn cat, pug and
     * rabbit keep the old way exactly (PetBehavior, PlanFrames).
     */
    boolean smooth() {
        return pixelGrid > 0;
    }

    boolean hasAnimation(int animationType) {
        return animations.containsKey(animationType);
    }

    /** Whether a pose has its own way in or out (see {@link #ENTER}). */
    boolean framed(int animationType) {
        return hasAnimation(ENTER + animationType) || hasAnimation(LEAVE + animationType);
    }

    /** Time per step along the walk path. */
    int walkStepMs() {
        return animation(ANIMATION_WALK).intervalMs;
    }

    /**
     * Walking speed: ground offsets per second for the hand-drawn pets (see
     * {@link PetBehavior#WALK_STEP}), art pixels per second for the pixel pets ({@link #gait}).
     */
    float walkSpeed() {
        return gait != null ? gait.speed() : PetBehavior.WALK_STEP * 1000f / walkStepMs();
    }

    /** Whether the pet can fly about the widget (the parrot). */
    boolean flies() {
        return flightsPerSession > 0 && hasAnimation(ANIMATION_TAKEOFF) && hasAnimation(ANIMATION_FLY)
            && hasAnimation(ANIMATION_LAND);
    }

    /** Whether the pet walks along the ground; a scene swims in place. */
    boolean walks() {
        return !scene && hasAnimation(ANIMATION_WALK);
    }

    private static final Map<Integer, PetSpec> REGISTRY = new HashMap<>();

    // What every pet does on its own: .does(animation, weight, never right after ...) plays it
    // MIN_REPEATS..MAX_REPEATS times, .once(...) a single time; then the pet does something else.
    // Weights are relative. AWAKE_MS, MIN_REPEATS and MAX_REPEATS above are the common settings.
    static {
        // Cat: mostly sits and washes, now and then strolls; washes when it wakes, sits and looks at you on a tap
        register(new Builder(PET_CAT)
            .anim(ANIMATION_SLEEP, SLEEP_FRAMES, LOOP_INTERVAL_MS)
            .anim(ANIMATION_LICK, LICK_FRAMES, LOOP_INTERVAL_MS)
            .anim(ANIMATION_SEAT, CAT_SEAT_FRAMES, LOOP_INTERVAL_MS)
            .anim(ANIMATION_WALK, WALK_FRAMES, WALK_STEP_MS)
            .reactions(ANIMATION_LICK, ANIMATION_SEAT)
            .does(ANIMATION_SEAT, 40)
            .does(ANIMATION_LICK, 30)
            .does(ANIMATION_WALK, 30)
            .ui(R.string.pet_name_cat, R.string.pet_cat_emoji, R.drawable.prop_bubble_food_cat,
                R.drawable.pixel_kitty_icon));
        // Dog: loves walks; opens its mouth (EAT frames) when it wakes, sits with its tongue out
        // (stored in the LICK slot) on a tap; sprites face right
        register(new Builder(PET_DOG)
            .anim(ANIMATION_SLEEP, DOG_SLEEP_FRAMES, LOOP_INTERVAL_MS)
            .anim(ANIMATION_EAT, DOG_EAT_FRAMES, LOOP_INTERVAL_MS)
            .anim(ANIMATION_LICK, DOG_SIT_FRAMES, LOOP_INTERVAL_MS)
            .anim(ANIMATION_WALK, DOG_WALK_FRAMES, WALK_STEP_MS)
            .facesRight()
            .reactions(ANIMATION_EAT, ANIMATION_LICK)
            .does(ANIMATION_WALK, 45)
            .does(ANIMATION_LICK, 30)
            .does(ANIMATION_EAT, 25)
            .ui(R.string.pet_name_dog, R.string.pet_dog_emoji, R.drawable.prop_bubble_food_dog,
                R.drawable.pixel_doggy_icon));
        // Rabbit: mostly munches its carrot, hops about and sits
        register(new Builder(PET_RABBIT)
            .anim(ANIMATION_SLEEP, RABBIT_SLEEP_FRAMES, LOOP_INTERVAL_MS)
            .anim(ANIMATION_EAT, RABBIT_EAT_FRAMES, LOOP_INTERVAL_MS)
            .anim(ANIMATION_SEAT, RABBIT_SIT_FRAMES, LOOP_INTERVAL_MS)
            .anim(ANIMATION_WALK, RABBIT_WALK_FRAMES, WALK_STEP_MS)
            .reactions(ANIMATION_SEAT, ANIMATION_SEAT)
            .does(ANIMATION_EAT, 40)
            .does(ANIMATION_WALK, 30)
            .does(ANIMATION_SEAT, 30)
            .ui(R.string.pet_name_rabbit, R.string.pet_rabbit_emoji, R.drawable.prop_bubble_food_rabbit,
                R.drawable.pixel_bunny_icon));
        // Pixel-art pets: the character comes from size, speed, the special trick and what they like
        // to do. Frames and timings come from tools/sprite_gen. Parrot and capybara keep the ids of
        // the old clips, so widgets saved with them get the new pets.
        // Panda: big and slow, rolls over; mostly stands about and eats, rarely walks
        register(pixelPet(PET_PANDA, "panda", "roll", 0.32f,
            R.string.pet_name_panda, R.string.pet_panda_emoji, R.drawable.prop_bubble_food_panda)
            .gait(PANDA_GAIT)
            .poses("fan", "drink", "hug")
            .does(ANIMATION_IDLE, 35)
            .does(ANIMATION_EAT, 30)
            .once(ANIMATION_SPECIAL, 15)
            .does(ANIMATION_HAPPY, 10)
            .does(ANIMATION_WALK, 10, ANIMATION_SPECIAL));
        // Hamster: tiny and quick, stuffs its cheeks; runs about the most, in short bursts.
        // It sits upright and fills less of its frame than the others, hence the slightly larger ratio
        register(pixelPet(PET_HAMSTER, "hamster", "cheeks", 0.25f,
            R.string.pet_name_hamster, R.string.pet_hamster_emoji, R.drawable.prop_bubble_food_seeds)
            .gait(HAMSTER_GAIT)
            .minPlan(HAMSTER_MIN_PLAN_MS)
            .poses("nibble", "umbrella", "balance")
            .does(ANIMATION_WALK, 35)
            .does(ANIMATION_SPECIAL, 20)
            .does(ANIMATION_IDLE, 15)
            .does(ANIMATION_EAT, 15)
            .does(ANIMATION_HAPPY, 15));
        // Capybara: big and unhurried, takes a warm bath with an orange on its head; long calm
        // spells, and after the bath it sits for a while before walking off
        register(pixelPet(PET_CAPYBARA, "capybara", "bath", 0.30f,
            R.string.pet_name_capybara, R.string.pet_capybara_emoji, R.drawable.prop_bubble_food_capybara)
            .gait(CAPYBARA_GAIT)
            .minPlan(CAPYBARA_MIN_PLAN_MS)
            .poses("leaf_hat", "lie")
            .does(ANIMATION_IDLE, 35)
            .does(ANIMATION_SPECIAL, 25)
            .does(ANIMATION_EAT, 20)
            .does(ANIMATION_HAPPY, 10)
            .does(ANIMATION_WALK, 10, ANIMATION_SPECIAL));
        // Parrot: medium and lively, flaps and chatters; chatters the most, and once in an awake
        // spell may fly about the whole widget (after landing it doesn't hop off at once)
        register(pixelPet(PET_PARROT, "parrot", "talk", 0.24f,
            R.string.pet_name_parrot, R.string.pet_parrot_emoji, R.drawable.prop_bubble_food_seeds)
            .gait(PARROT_GAIT)
            .minPlan(PARROT_MIN_PLAN_MS)
            .flight("parrot", 1)
            .poses("leaf")
            .does(ANIMATION_SPECIAL, 25)
            .does(ANIMATION_IDLE, 20)
            .does(ANIMATION_EAT, 15)
            .does(ANIMATION_WALK, 15, ANIMATION_FLY)
            .does(ANIMATION_HAPPY, 15)
            .once(ANIMATION_FLY, 10));
        // Puppy: a corgi pup, cheerful and full of beans; small and quick. It runs about and jumps
        // for joy the most, and chases its own tail now and then
        register(pixelPet(PET_PUPPY, "puppy", "chase", 0.26f,
            R.string.pet_name_puppy, R.string.pet_puppy_emoji, R.drawable.prop_bubble_food_dog)
            .gait(PUPPY_GAIT)
            .minPlan(PUPPY_MIN_PLAN_MS)
            .poses("flop", "look_up", "shake", "nose_push")
            .does(ANIMATION_WALK, 35)
            .does(ANIMATION_HAPPY, 20)
            .does(ANIMATION_SPECIAL, 15)
            .does(ANIMATION_IDLE, 15)
            .does(ANIMATION_EAT, 15));
        // Kitten: a little grey tabby, curious and a bit lazy; the smallest, at a medium pace. It
        // mostly sits and washes, plays with its ball of yarn, and walks the least of the two
        register(pixelPet(PET_KITTEN, "kitten", "yarn", 0.22f,
            R.string.pet_name_kitten, R.string.pet_kitten_emoji, R.drawable.prop_bubble_food_cat)
            .gait(KITTEN_GAIT)
            .poses("hide", "pounce", "bat", "throne")
            .does(ANIMATION_IDLE, 35)
            .does(ANIMATION_SPECIAL, 25)
            .does(ANIMATION_EAT, 15)
            .does(ANIMATION_WALK, 15)
            .does(ANIMATION_HAPPY, 10));
        // Aquarium: a whole tank with three goldfish; its own trick is the treasure chest. It is
        // calm: the fish mostly swim about, now and then they are fed or open the chest
        register(scene(pixelPet(PET_AQUARIUM, "aquarium", new String[] {"sleep", "happy", "idle", "eat",
            "treasure", "swim"}, 1f, R.string.pet_name_aquarium, R.string.pet_aquarium_emoji,
            R.drawable.prop_bubble_food_aquarium), 128, 96)
            .does(ANIMATION_WALK, 30)
            .does(ANIMATION_IDLE, 30)
            .does(ANIMATION_EAT, 20)
            .once(ANIMATION_SPECIAL, 15)
            .does(ANIMATION_HAPPY, 5));
    }

    /** Spec for a stored pet type; unknown types fall back to the cat. */
    static PetSpec forType(int petType) {
        PetSpec spec = REGISTRY.get(petType);
        return spec != null ? spec : REGISTRY.get(PET_CAT);
    }

    static int[] allPetTypes() {
        return new int[] {PET_CAT, PET_DOG, PET_RABBIT, PET_PANDA, PET_HAMSTER, PET_CAPYBARA, PET_PARROT,
            PET_AQUARIUM, PET_PUPPY, PET_KITTEN};
    }

    private static void register(Builder builder) {
        REGISTRY.put(builder.petType, builder.build());
    }

    /** A pet drawn by tools/sprite_gen with the six animations; {@code special} is its own trick. */
    static Builder pixelPet(int petType, String name, String special, float sizeRatio, int nameRes,
                                    int emojiRes, int foodBubbleRes) {
        return pixelPet(petType, name, new String[] {"sleep", "happy", "idle", "eat", special, "walk"}, sizeRatio,
            nameRes, emojiRes, foodBubbleRes);
    }

    /** names: the generated animations for PIXEL_PET_ANIMATIONS, in that order. It wakes and cheers up with joy. */
    static Builder pixelPet(int petType, String name, String[] names, float sizeRatio, int nameRes,
                                    int emojiRes, int foodBubbleRes) {
        Builder builder = new Builder(petType);
        builder.generatedName = name;
        for (int i = 0; i < PIXEL_PET_ANIMATIONS.length; i++) {
            builder.generated(PIXEL_PET_ANIMATIONS[i], names[i], true);
            // An activity's way in and out, if drawn (lying down to gnaw a bone, getting up again)
            builder.generated(ENTER + PIXEL_PET_ANIMATIONS[i], names[i] + "_in", false);
            builder.generated(LEAVE + PIXEL_PET_ANIMATIONS[i], names[i] + "_out", false);
        }
        builder.pixelGrid = PIXEL_GRID;
        builder.transitions();
        builder.sizeRatio = sizeRatio;
        builder.reactions(ANIMATION_HAPPY, ANIMATION_HAPPY);
        return builder.ui(nameRes, emojiRes, foodBubbleRes, GeneratedPetFrames.icon(name));
    }

    /** Makes a pixel pet a scene of frameWidth x frameHeight pixels (see {@link #scene}). */
    private static Builder scene(Builder builder, int frameWidth, int frameHeight) {
        builder.scene = true;
        builder.frameWidth = frameWidth;
        builder.frameHeight = frameHeight;
        return builder;
    }

    /** Builds pets: the registry above, and pets made up by the tests. */
    static final class Builder {
        final int petType;
        final List<Integer> order = new ArrayList<>();
        int awakeMs = AWAKE_MS;
        int minRepeats = MIN_REPEATS;
        int maxRepeats = MAX_REPEATS;
        int wakeAnimation = ANIMATION_SLEEP;
        int joyAnimation = ANIMATION_SLEEP;
        final List<Activity> activities = new ArrayList<>();
        int flightsPerSession;
        Gait gait;
        int minPlanMs = PetBehavior.MIN_PLAN_MS;
        final Map<Integer, Anim> animations = new HashMap<>();
        float sizeRatio = DEFAULT_SIZE_RATIO;
        boolean facesRight;
        int pixelGrid;
        boolean scene;
        int frameWidth = PIXEL_GRID;
        int frameHeight = PIXEL_GRID;
        String generatedName;
        final Map<Integer, String> generatedAnimations = new HashMap<>();
        int nameRes;
        int emojiRes;
        int foodBubbleRes;
        int iconRes;

        Builder(int petType) {
            this.petType = petType;
        }

        Builder anim(int animationType, int[] frames, int intervalMs) {
            return anim(animationType, new Anim(frames, intervalMs));
        }

        private Builder anim(int animationType, Anim anim) {
            if (animations.put(animationType, anim) == null) {
                order.add(animationType);
            }
            return this;
        }

        /**
         * The generated animation {@code name} of this pet (frames, interval, moved frames) as
         * {@code animationType}; when it is not {@code required} and was not drawn, nothing.
         */
        Builder generated(int animationType, String name, boolean required) {
            int[] frames = GeneratedPetFrames.frames(generatedName, name);
            if (frames == null) {
                if (required) {
                    throw new IllegalStateException("No generated frames for " + generatedName + "/" + name);
                }
                return this;
            }
            anim(animationType, new Anim(frames, GeneratedPetFrames.interval(generatedName, name),
                GeneratedPetFrames.shifts(generatedName, name)));
            generatedAnimations.put(animationType, name);
            return this;
        }

        /** What the pet does when a tap wakes it, and when it is tapped while awake. */
        Builder reactions(int wake, int joy) {
            wakeAnimation = wake;
            joyAnimation = joy;
            return this;
        }

        /** The generated take-off, wingbeat and landing; at most {@code perSession} flights an awake spell. */
        Builder flight(String name, int perSession) {
            String[] names = {"takeoff", "fly", "land"};
            int[] types = {ANIMATION_TAKEOFF, ANIMATION_FLY, ANIMATION_LAND};
            for (int i = 0; i < names.length; i++) {
                int[] frames = GeneratedPetFrames.frames(name, names[i]);
                if (frames == null) {
                    throw new IllegalStateException("No generated frames for " + name + "/" + names[i]);
                }
                // Every frame of the flight shares one tempo (PetSpec.FLIGHT_INTERVAL_MS)
                anim(types[i], new Anim(frames, FLIGHT_INTERVAL_MS, GeneratedPetFrames.shifts(name, names[i])));
                generatedAnimations.put(types[i], names[i]);
            }
            flightsPerSession = perSession;
            return this;
        }

        /**
         * The pet's own poses for its interactions with the backgrounds (BackgroundEvents), from
         * the generated frames, as ANIMATION_POSE_1, 2, 3, 4 in this order. They are no activities:
         * only the interactions play them.
         */
        Builder poses(String... names) {
            int[] types = {ANIMATION_POSE_1, ANIMATION_POSE_2, ANIMATION_POSE_3, ANIMATION_POSE_4};
            for (int i = 0; i < names.length; i++) {
                generated(types[i], names[i], true);
                // Its way in and out, if drawn
                generated(ENTER + types[i], names[i] + "_in", false);
                generated(LEAVE + types[i], names[i] + "_out", false);
            }
            return this;
        }

        /** Shortest plan (see {@link PetSpec#minPlanMs}). */
        Builder minPlan(int ms) {
            minPlanMs = ms;
            return this;
        }

        /** How the pet walks; its walk frames take the gait's tempo. */
        Builder gait(Gait walking) {
            gait = walking;
            Anim walk = animations.get(ANIMATION_WALK);
            if (walk != null) {
                animations.put(ANIMATION_WALK, new Anim(walk.frames, walking.intervalMs, walk.shifts));
            }
            return this;
        }

        /**
         * The generated transitions the pet has, if any: turning round, the first and last step of
         * a walk, lying down and getting up. They play inside other animations (PetBehavior).
         */
        Builder transitions() {
            String[] names = {"turn", "walk_start", "walk_stop", "lie_down", "get_up"};
            int[] types = {ANIMATION_TURN, ANIMATION_WALK_START, ANIMATION_WALK_STOP, ANIMATION_LIE_DOWN,
                ANIMATION_GET_UP};
            for (int i = 0; i < names.length; i++) {
                generated(types[i], names[i], false);
            }
            return this;
        }

        /** An activity played 2–4 times, never right after any of {@code notAfter}. */
        Builder does(int animationType, int weight, int... notAfter) {
            activities.add(new Activity(animationType, weight, false, notAfter));
            return this;
        }

        /** A one-shot activity, played once. */
        Builder once(int animationType, int weight, int... notAfter) {
            activities.add(new Activity(animationType, weight, true, notAfter));
            return this;
        }

        /** Repeats of every activity of this pet, instead of MIN_REPEATS..MAX_REPEATS. */
        @SuppressWarnings("unused")
        Builder repeats(int min, int max) {
            minRepeats = min;
            maxRepeats = max;
            return this;
        }

        Builder facesRight() {
            facesRight = true;
            return this;
        }

        Builder ui(int nameRes, int emojiRes, int foodBubbleRes, int iconRes) {
            this.nameRes = nameRes;
            this.emojiRes = emojiRes;
            this.foodBubbleRes = foodBubbleRes;
            this.iconRes = iconRes;
            return this;
        }

        PetSpec build() {
            return new PetSpec(this);
        }
    }
}
