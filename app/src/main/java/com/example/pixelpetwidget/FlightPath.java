package com.example.pixelpetwidget;

/**
 * Where a flying pet (the parrot) is in every frame of its flight, worked out for one widget
 * size: it takes off from its spot on the ground, flies an arc over the widget and lands on
 * another spot, always forwards, so it never turns round in the air and lands looking the way it
 * flew. The shape is the same at every size, only stretched to the free area.
 *
 * <p>Sideways it goes at an even pace that eases in after the take-off and out before the
 * landing; up and down it follows its style's curve, which starts and ends level: one arc, never
 * a wave or a loop. The arc is as high as the style wants unless that would move the bird more
 * than {@code maxStep} pixels (diagonally) between two frames; then it is lower.
 */
final class FlightPath {

    /** One arc over the middle of the way. */
    static final int ARC = 0;
    /** Rises high early, then glides down. */
    static final int HIGH = 1;
    /** A lower arc. */
    static final int LOW = 2;
    static final int STYLES = 3;

    /** Frames of the take-off and the landing; the frame is in the air from the third to the fourth last. */
    static final int TAKEOFF_FRAMES = 6;
    static final int LAND_FRAMES = 6;
    private static final int LIFT_FRAME = 2;
    private static final int TOUCH_FROM_END = 4;
    /** Frames of the wingbeat loop. */
    static final int FLY_FRAMES = 8;
    /** Highest point of each style, as a share of the free height. */
    private static final float[] PEAKS = {0.6f, 0.8f, 0.4f};

    /** One frame of the flight: where the frame's top-left corner is, which way it looks, what it shows. */
    static final class Pose {
        final int x;
        final int y;
        final boolean facingRight;
        /** ANIMATION_TAKEOFF, ANIMATION_FLY or ANIMATION_LAND. */
        final int animation;
        final int frame;

        Pose(int x, int y, boolean facingRight, int animation, int frame) {
            this.x = x;
            this.y = y;
            this.facingRight = facingRight;
            this.animation = animation;
            this.frame = frame;
        }
    }

    private FlightPath() {
    }

    /** Fewest frames a flight can have: the take-off, one wingbeat and the landing. */
    static int minFrames() {
        return TAKEOFF_FRAMES + FLY_FRAMES + LAND_FRAMES;
    }

    /**
     * The flight in {@code frames} frames from the ground spot fromX to the ground spot toX: x is
     * where the frame's left edge is (0..width), y where its top is (0 at the top, height on the
     * ground). Neighbouring frames are at most {@code maxStep} pixels apart (diagonally) when the
     * way and the frames allow it sideways; the arc gets lower until they are.
     */
    static Pose[] plan(int style, int frames, int fromX, int toX, int width, int height, int maxStep) {
        frames = Math.max(frames, minFrames());
        boolean right = toX >= fromX;
        int lastAir = frames - TOUCH_FROM_END;
        float[] along = new float[frames];
        for (int f = 0; f < frames; f++) {
            float t = f <= LIFT_FRAME ? 0f : f >= lastAir ? 1f : (float) (f - LIFT_FRAME) / (lastAir - LIFT_FRAME);
            // Mostly an even pace, a little slower off the ground and coming down
            along[f] = 0.7f * t + 0.3f * t * t * (3 - 2 * t);
        }
        float peak = PEAKS[Math.floorMod(style, STYLES)] * Math.max(0, height);
        int[] xs = new int[frames];
        int[] ys = new int[frames];
        for (int attempt = 0; attempt < 60; attempt++) {
            double biggest = 0;
            for (int f = 0; f < frames; f++) {
                xs[f] = Math.round(fromX + (toX - fromX) * along[f]);
                ys[f] = Math.round(height - peak * shape(style, along[f]));
                if (f > 0) {
                    biggest = Math.max(biggest, Math.hypot(xs[f] - xs[f - 1], ys[f] - ys[f - 1]));
                }
            }
            if (biggest <= Math.max(1, maxStep) || peak < 1f) {
                break;
            }
            peak *= 0.9f;
        }
        Pose[] poses = new Pose[frames];
        for (int f = 0; f < frames; f++) {
            int animation;
            int frame;
            if (f < TAKEOFF_FRAMES) {
                animation = PixelCatWidget.ANIMATION_TAKEOFF;
                frame = f;
            } else if (f >= frames - LAND_FRAMES) {
                animation = PixelCatWidget.ANIMATION_LAND;
                frame = f - (frames - LAND_FRAMES);
            } else {
                animation = PixelCatWidget.ANIMATION_FLY;
                frame = (f - TAKEOFF_FRAMES) % FLY_FRAMES;
            }
            poses[f] = new Pose(Math.max(0, Math.min(width, xs[f])), Math.max(0, ys[f]), right, animation, frame);
        }
        return poses;
    }

    /** Height of a style's curve at {@code u} (0..1 along the way), 0 at both ends, level there. */
    static float shape(int style, float u) {
        if (Math.floorMod(style, STYLES) == HIGH) {
            // Up quickly, a long glide down
            double v = Math.sin(Math.PI * Math.pow(u, 0.75));
            return (float) (v * v);
        }
        double s = Math.sin(Math.PI * u);
        return (float) (s * s);
    }
}
