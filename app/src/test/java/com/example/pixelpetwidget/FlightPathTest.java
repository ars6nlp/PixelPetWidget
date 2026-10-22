package com.example.pixelpetwidget;

import static com.example.pixelpetwidget.PixelCatWidget.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FlightPathTest {

    /** Free areas of real widget sizes: tiny, 4x2, 4x3, 4x4, wide and tall, in pixels. */
    private static final int[][] AREAS = {{40, 30}, {520, 200}, {640, 420}, {700, 760}, {1100, 260}, {300, 900}};

    @Test
    public void theFlightNeverLeavesTheWidget() {
        for (int[] area : AREAS) {
            for (int style = 0; style < FlightPath.STYLES; style++) {
                for (int frames = FlightPath.minFrames(); frames <= 60; frames += 7) {
                    for (boolean right : new boolean[] {true, false}) {
                        int from = right ? area[0] / 5 : area[0] * 4 / 5;
                        int to = right ? area[0] * 3 / 4 : area[0] / 4;
                        for (FlightPath.Pose pose : FlightPath.plan(style, frames, from, to, area[0], area[1], 24)) {
                            assertTrue(pose.x >= 0 && pose.x <= area[0]);
                            assertTrue(pose.y >= 0 && pose.y <= area[1]);
                        }
                    }
                }
            }
        }
    }

    @Test
    public void itTakesOffFromItsSpotAndLandsOnTheOtherLookingAhead() {
        int width = 640;
        int height = 420;
        for (int style = 0; style < FlightPath.STYLES; style++) {
            for (boolean right : new boolean[] {true, false}) {
                int from = right ? 150 : 470;
                int to = right ? 470 : 150;
                FlightPath.Pose[] poses = FlightPath.plan(style, 54, from, to, width, height, 24);
                assertEquals(54, poses.length);
                // Crouches on the ground, then rises
                assertEquals(from, poses[0].x);
                assertEquals(height, poses[0].y);
                assertEquals(height, poses[2].y);
                assertTrue("style " + style, poses[20].y < height - 50);
                // Touches down four frames before the end and stays there
                for (int f = poses.length - 4; f < poses.length; f++) {
                    assertEquals(to, poses[f].x);
                    assertEquals(height, poses[f].y);
                }
                // Never turns round in the air: it always looks the way it flies
                for (FlightPath.Pose pose : poses) {
                    assertEquals(right, pose.facingRight);
                }
            }
        }
    }

    @Test
    public void itFliesForwardsWithoutLoopsOrZigzags() {
        for (int[] area : AREAS) {
            for (int style = 0; style < FlightPath.STYLES; style++) {
                FlightPath.Pose[] poses = FlightPath.plan(style, 50, area[0] / 5, area[0] * 4 / 5, area[0], area[1], 24);
                // Forwards all the way, up once and down once (y grows towards the ground)
                int turns = 0;
                for (int f = 1; f < poses.length; f++) {
                    assertTrue(poses[f].x >= poses[f - 1].x);
                    if (f > 1 && poses[f].y < poses[f - 1].y && poses[f - 1].y > poses[f - 2].y) {
                        turns++;
                    }
                }
                assertEquals("style " + style + " at " + area[0] + "x" + area[1], 0, turns);
            }
        }
    }

    @Test
    public void noFrameMovesFartherThanTheLimit() {
        for (int[] area : AREAS) {
            for (int style = 0; style < FlightPath.STYLES; style++) {
                for (int maxStep : new int[] {6, 12, 24}) {
                    // A way the planner allows: at most FLIGHT_PACE_PX per frame in the air on average
                    int frames = 50;
                    int air = frames - FlightPath.TAKEOFF_FRAMES - FlightPath.LAND_FRAMES;
                    int way = Math.min(area[0] - 2, air * PetBehavior.FLIGHT_PACE_PX * maxStep / 5);
                    FlightPath.Pose[] poses = FlightPath.plan(style, frames, 1, 1 + way, area[0], area[1], maxStep);
                    for (int f = 1; f < poses.length; f++) {
                        // Diagonally too
                        assertTrue("style " + style + " at " + area[0] + "x" + area[1], Math.hypot(
                            poses[f].x - poses[f - 1].x, poses[f].y - poses[f - 1].y) <= maxStep);
                    }
                }
            }
        }
    }

    @Test
    public void itRisesAsHighAsItsStyleWhenThereIsTime() {
        FlightPath.Pose[] high = FlightPath.plan(FlightPath.HIGH, 60, 100, 300, 640, 420, 100);
        FlightPath.Pose[] lower = FlightPath.plan(FlightPath.LOW, 60, 100, 300, 640, 420, 100);
        assertTrue(top(high) < 420 - 0.75 * 420);
        assertTrue(top(lower) > top(high));
        // With little time for the way up, the arc is lower but still an arc (a way the planner
        // gives 30 frames: FLIGHT_PACE_PX in each frame in the air)
        FlightPath.Pose[] low = FlightPath.plan(FlightPath.HIGH, 30, 100, 100 + 18 * PetBehavior.FLIGHT_PACE_PX,
            640, 420, 6);
        assertTrue(top(low) > top(high));
        assertTrue(top(low) < 420 - 20);
    }

    @Test
    public void framesGoTakeoffFlyLand() {
        FlightPath.Pose[] poses = FlightPath.plan(FlightPath.ARC, 50, 100, 400, 640, 420, 24);
        for (int f = 0; f < poses.length; f++) {
            if (f < FlightPath.TAKEOFF_FRAMES) {
                assertEquals(ANIMATION_TAKEOFF, poses[f].animation);
                assertEquals(f, poses[f].frame);
            } else if (f >= poses.length - FlightPath.LAND_FRAMES) {
                assertEquals(ANIMATION_LAND, poses[f].animation);
                assertEquals(f - (poses.length - FlightPath.LAND_FRAMES), poses[f].frame);
            } else {
                assertEquals(ANIMATION_FLY, poses[f].animation);
                assertEquals((f - FlightPath.TAKEOFF_FRAMES) % FlightPath.FLY_FRAMES, poses[f].frame);
            }
        }
        // The parrot has exactly these frames, all at the flight's tempo
        PetSpec parrot = PetSpec.forType(PET_PARROT);
        assertEquals(FlightPath.TAKEOFF_FRAMES, parrot.animation(ANIMATION_TAKEOFF).frames.length);
        assertEquals(FlightPath.FLY_FRAMES, parrot.animation(ANIMATION_FLY).frames.length);
        assertEquals(FlightPath.LAND_FRAMES, parrot.animation(ANIMATION_LAND).frames.length);
        for (int animation : new int[] {ANIMATION_TAKEOFF, ANIMATION_FLY, ANIMATION_LAND}) {
            assertEquals(PetSpec.FLIGHT_INTERVAL_MS, parrot.animation(animation).intervalMs);
        }
    }

    @Test
    public void aWidgetWithNoRoomStillWorks() {
        for (FlightPath.Pose pose : FlightPath.plan(FlightPath.ARC, 20, 0, 0, 0, 0, 6)) {
            assertEquals(0, pose.x);
            assertEquals(0, pose.y);
        }
    }

    private static int top(FlightPath.Pose[] poses) {
        int top = Integer.MAX_VALUE;
        for (FlightPath.Pose pose : poses) {
            top = Math.min(top, pose.y);
        }
        return top;
    }
}
