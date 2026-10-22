package com.example.pixelpetwidget;

import static com.example.pixelpetwidget.PixelCatWidget.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

/**
 * The launcher refuses views nested more than ten deep: the whole update is lost and the process
 * that sent it gets an exception ("Too many nested views"). A plan never nests deeper than
 * PlanFrames.MAX_PLAN_DEPTH, whenever it is drawn: at its start, in its middle (feeding in the app,
 * a resize), at its end and long after it (a reboot, a late alarm, a launcher restart).
 */
public class PlanDepthTest {

    private static final long T0 = 1_700_000_000_000L;
    private static final float[][] SIZES = {{222, 148}, {340, 370}, {420, 150}, {520, 430}};

    @Test
    public void noPlanNestsDeeperThanTheLauncherTakesWheneverItIsDrawn() {
        int deepest = 0;
        String where = "";
        int plans = 0;
        for (int pet : PetSpec.allPetTypes()) {
            PetSpec spec = PetSpec.forType(pet);
            for (int bg = 1; bg <= 6; bg++) {
                BackgroundEvents.Interaction interaction = spec.scene ? null : BackgroundEvents.find(pet, bg);
                for (float[] size : SIZES) {
                    PlanFrames.Stage stage = SmoothMotionTest.stage(spec, size, bg);
                    for (long seed = 1; seed <= 6; seed++) {
                        PetBehavior.Session s = new PetBehavior.Session();
                        s.groundPx = stage.roomPx();
                        long now = T0;
                        PetBehavior.tap(spec, interaction, s, now, seed * 7919 + pet * 31 + bg);
                        for (int turn = 0; turn < 60 && s.phase != PetBehavior.ASLEEP; turn++) {
                            long next = PetBehavior.nextTurn(s);
                            for (long at : new long[] {now, (now + next) / 2, next - 100, next + 2_000, next + 60_000,
                                next + 600_000}) {
                                PlanFrames.Node plan = PlanFrames.plan(spec, s, at, stage.withGround(s.groundPx), null);
                                plans++;
                                if (plan.depth() > deepest) {
                                    deepest = plan.depth();
                                    where = spec.generatedName + "/" + pet + " bg" + bg + " " + (int) size[0] + "x"
                                        + (int) size[1] + " seed " + seed + " turn " + turn + " at +" + (at - now);
                                }
                                if (spec.smooth()) {
                                    assertTrue(where, plan.views() <= PetBehavior.MAX_PLAN_VIEWS);
                                }
                            }
                            now = next;
                            PetBehavior.advance(spec, interaction, s, now);
                        }
                    }
                }
            }
        }
        System.out.println("PlanDepthTest: " + plans + " plans, deepest " + deepest + " (" + where + ")");
        assertTrue("deepest " + deepest + " at " + where, deepest <= PlanFrames.MAX_PLAN_DEPTH);
    }

    @Test
    public void aHoldDoublesWhileItHasLevelsAndTakesMoreCopiesWhenItHasNot() {
        // Plenty of levels: one copy a level, each doubles the time
        List<Integer> copies = PlanFrames.holdCopies(1_000, 8_000, 5);
        assertEquals(java.util.Arrays.asList(1, 1, 1), copies);
        // Few levels for a long hold: more copies, never more levels than allowed
        copies = PlanFrames.holdCopies(10, 60_000, 4);
        assertTrue(copies.toString(), copies.size() <= 4);
        long covered = 10;
        for (int c : copies) {
            covered *= c + 1;
        }
        assertTrue(covered >= 60_000);
        // Nothing to hold
        assertTrue(PlanFrames.holdCopies(5_000, 4_000, 3).isEmpty());
    }
}
