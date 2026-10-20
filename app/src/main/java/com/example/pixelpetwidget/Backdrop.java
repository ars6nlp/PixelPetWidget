package com.example.pixelpetwidget;

/**
 * A pixel-art background behind and under the pet (the autumn scenes): a picture drawn by
 * tools/sprite_gen/scenery.py, the row the pets stand on, small animated elements, layers tiled
 * over the whole widget (the rain) and props the pets play with. The widget scales it by whole
 * numbers at the pet's pixel scale, its ground under the pet's feet. Widgets keep their background
 * as bg_type: 1–3 are the painted pictures of old, these come after them.
 */
final class Backdrop {

    /** bg_type of the autumn backgrounds (1–3 are the old pictures). */
    static final int MAPLE_PARK = 4;
    static final int RAINY_EVENING = 5;
    static final int PUMPKIN_FIELD = 6;

    /** An animated element: a sprite with a position per frame, in canvas pixels. */
    static final class Element {
        final int[] frames;
        final int[] x;
        final int[] y;
        final int intervalMs;
        final int width;
        final int height;

        Element(int[] frames, int[] x, int[] y, int intervalMs, int width, int height) {
            this.frames = frames;
            this.x = x;
            this.y = y;
            this.intervalMs = intervalMs;
            this.width = width;
            this.height = height;
        }
    }

    /** A layer tiled over the whole widget, in canvas pixels (the rain): its frames in a loop. */
    static final class Tile {
        final int[] frames;
        final int intervalMs;

        Tile(int[] frames, int intervalMs) {
            this.frames = frames;
            this.intervalMs = intervalMs;
        }
    }

    /** One state of a prop: its frames in a loop (one frame: it keeps still). */
    static final class State {
        final String name;
        final int[] frames;
        final int intervalMs;

        State(String name, int[] frames, int intervalMs) {
            this.name = name;
            this.frames = frames;
            this.intervalMs = intervalMs;
        }

        long loopMs() {
            return (long) frames.length * intervalMs;
        }
    }

    /**
     * A prop pets play with (a heap of leaves, a puddle, a pumpkin). It stands on the ground line at
     * a ground position like the pets' own (-1..1 of their range, so a pet that walks to
     * {@link #offset} stands at it on any widget size), behind the pet or in front of it. It shows
     * its {@link #ambient} state, and others in the pets' interactions with it.
     */
    static final class Prop {
        final String name;
        final float offset;
        final boolean front;
        final int width;
        final int height;
        /** Rows from its top down to the ground line. */
        final int baseline;
        /** Rows above the ground line where a bird could perch on it (0: nowhere). */
        final int perch;
        final String ambient;
        final State[] states;

        Prop(String name, float offset, boolean front, int width, int height, int baseline, int perch, String ambient,
             State... states) {
            this.name = name;
            this.offset = offset;
            this.front = front;
            this.width = width;
            this.height = height;
            this.baseline = baseline;
            this.perch = perch;
            this.ambient = ambient;
            this.states = states;
        }

        /** The state called {@code name}; the ambient one for an unknown name. */
        State state(String name) {
            State ambientState = states[0];
            for (State state : states) {
                if (state.name.equals(name)) {
                    return state;
                }
                if (state.name.equals(ambient)) {
                    ambientState = state;
                }
            }
            return ambientState;
        }
    }

    final int type;
    final int image;
    final int width;
    final int height;
    /** Row the pets stand on, and rows of ground shown under their feet. */
    final int ground;
    final int groundMargin;
    /** Colours that continue the picture above and below it. */
    final int sky;
    final int earth;
    /** Lowest leafy pixel above each column, -1 where there is none (where event leaves come loose). */
    final short[] canopy;
    final Element[] elements;
    final Tile[] tiles;
    final Prop[] props;
    /** A leaf turning as it falls (four frames) and lying on the ground, all {@link #leafWidth} wide; or none. */
    final int[] leafTurning;
    final int leafLying;
    final int leafWidth;
    final int leafHeight;
    /** A raindrop, or 0 for none. */
    final int drop;
    final int dropWidth;
    final int dropHeight;
    final int nameRes;
    /** The picture with its props in their usual places, for the preview in the app. */
    final int previewRes;

    private Backdrop(Builder b) {
        this.type = b.type;
        this.image = b.image;
        this.width = b.width;
        this.height = b.height;
        this.ground = b.ground;
        this.groundMargin = b.groundMargin;
        this.sky = b.sky;
        this.earth = b.earth;
        this.canopy = b.canopy;
        this.elements = b.elements;
        this.tiles = b.tiles;
        this.props = b.props;
        this.leafTurning = b.leafTurning;
        this.leafLying = b.leafLying;
        this.leafWidth = b.leafWidth;
        this.leafHeight = b.leafHeight;
        this.drop = b.drop;
        this.dropWidth = b.dropWidth;
        this.dropHeight = b.dropHeight;
        this.nameRes = b.nameRes;
        this.previewRes = b.previewRes;
    }

    private static final class Builder {
        int type;
        int image;
        int width;
        int height;
        int ground;
        int groundMargin;
        int sky;
        int earth;
        short[] canopy;
        Element[] elements;
        Tile[] tiles;
        Prop[] props;
        int[] leafTurning;
        int leafLying;
        int leafWidth;
        int leafHeight;
        int drop;
        int dropWidth;
        int dropHeight;
        int nameRes;
        int previewRes;
    }

    private static final Backdrop MAPLE;
    private static final Backdrop RAINY;
    private static final Backdrop PUMPKIN;

    static {
        Builder b = new Builder();
        b.type = MAPLE_PARK;
        b.image = GeneratedBackgrounds.MAPLE_IMAGE;
        b.width = GeneratedBackgrounds.MAPLE_WIDTH;
        b.height = GeneratedBackgrounds.MAPLE_HEIGHT;
        b.ground = GeneratedBackgrounds.MAPLE_GROUND;
        b.groundMargin = GeneratedBackgrounds.MAPLE_GROUND_MARGIN;
        b.sky = GeneratedBackgrounds.MAPLE_SKY;
        b.earth = GeneratedBackgrounds.MAPLE_EARTH;
        b.canopy = GeneratedBackgrounds.MAPLE_CANOPY;
        b.elements = GeneratedBackgrounds.MAPLE_ELEMENTS;
        b.tiles = GeneratedBackgrounds.MAPLE_TILES;
        b.props = GeneratedBackgrounds.MAPLE_PROPS;
        b.leafTurning = new int[] {GeneratedBackgrounds.MAPLE_LEAF_0, GeneratedBackgrounds.MAPLE_LEAF_1,
            GeneratedBackgrounds.MAPLE_LEAF_2, GeneratedBackgrounds.MAPLE_LEAF_3};
        b.leafLying = GeneratedBackgrounds.MAPLE_LEAF_LYING;
        b.leafWidth = GeneratedBackgrounds.MAPLE_LEAF_0_WIDTH;
        b.leafHeight = GeneratedBackgrounds.MAPLE_LEAF_0_HEIGHT;
        b.nameRes = R.string.bg_maple_park;
        b.previewRes = GeneratedBackgrounds.MAPLE_PREVIEW;
        MAPLE = new Backdrop(b);

        b = new Builder();
        b.type = RAINY_EVENING;
        b.image = GeneratedBackgrounds.RAINY_IMAGE;
        b.width = GeneratedBackgrounds.RAINY_WIDTH;
        b.height = GeneratedBackgrounds.RAINY_HEIGHT;
        b.ground = GeneratedBackgrounds.RAINY_GROUND;
        b.groundMargin = GeneratedBackgrounds.RAINY_GROUND_MARGIN;
        b.sky = GeneratedBackgrounds.RAINY_SKY;
        b.earth = GeneratedBackgrounds.RAINY_EARTH;
        b.canopy = GeneratedBackgrounds.RAINY_CANOPY;
        b.elements = GeneratedBackgrounds.RAINY_ELEMENTS;
        b.tiles = GeneratedBackgrounds.RAINY_TILES;
        b.props = GeneratedBackgrounds.RAINY_PROPS;
        b.drop = GeneratedBackgrounds.RAINY_DROP;
        b.dropWidth = GeneratedBackgrounds.RAINY_DROP_WIDTH;
        b.dropHeight = GeneratedBackgrounds.RAINY_DROP_HEIGHT;
        b.nameRes = R.string.bg_rainy_evening;
        b.previewRes = GeneratedBackgrounds.RAINY_PREVIEW;
        RAINY = new Backdrop(b);

        b = new Builder();
        b.type = PUMPKIN_FIELD;
        b.image = GeneratedBackgrounds.PUMPKIN_IMAGE;
        b.width = GeneratedBackgrounds.PUMPKIN_WIDTH;
        b.height = GeneratedBackgrounds.PUMPKIN_HEIGHT;
        b.ground = GeneratedBackgrounds.PUMPKIN_GROUND;
        b.groundMargin = GeneratedBackgrounds.PUMPKIN_GROUND_MARGIN;
        b.sky = GeneratedBackgrounds.PUMPKIN_SKY;
        b.earth = GeneratedBackgrounds.PUMPKIN_EARTH;
        b.canopy = GeneratedBackgrounds.PUMPKIN_CANOPY;
        b.elements = GeneratedBackgrounds.PUMPKIN_ELEMENTS;
        b.tiles = GeneratedBackgrounds.PUMPKIN_TILES;
        b.props = GeneratedBackgrounds.PUMPKIN_PROPS;
        b.nameRes = R.string.bg_pumpkin_field;
        b.previewRes = GeneratedBackgrounds.PUMPKIN_PREVIEW;
        PUMPKIN = new Backdrop(b);
    }

    /** The background of a widget's bg_type, or null for the old pictures and unknown values. */
    static Backdrop forType(int bgType) {
        switch (bgType) {
            case MAPLE_PARK:
                return MAPLE;
            case RAINY_EVENING:
                return RAINY;
            case PUMPKIN_FIELD:
                return PUMPKIN;
            default:
                return null;
        }
    }

    /** bg_type values of the pixel-art backgrounds, in the order of the cards in the app. */
    static int[] allTypes() {
        return new int[] {MAPLE_PARK, RAINY_EVENING, PUMPKIN_FIELD};
    }

    /** The prop called {@code name}, or null. */
    Prop prop(String name) {
        for (Prop prop : props) {
            if (prop.name.equals(name)) {
                return prop;
            }
        }
        return null;
    }

    /**
     * Column of the canopy nearest to {@code x} (searching outwards), where a leaf can come
     * loose; -1 if the picture has no canopy at all.
     */
    int canopyColumnNear(int x) {
        for (int d = 0; d < width; d++) {
            for (int c : new int[] {x - d, x + d}) {
                if (c >= 0 && c < canopy.length && canopy[c] >= 0) {
                    return c;
                }
            }
        }
        return -1;
    }
}
