package com.example.pixelpetwidget;

import static com.example.pixelpetwidget.PixelCatWidget.*;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PixelCatWidgetTest {

    @Test
    public void everythingAPetDoesHasFrames() {
        for (int pet : PetSpec.allPetTypes()) {
            PetSpec spec = PetSpec.forType(pet);
            assertTrue(framesFor(pet, ANIMATION_SLEEP).length > 0);
            assertTrue(framesFor(pet, spec.wakeAnimation).length > 0);
            assertTrue(framesFor(pet, spec.joyAnimation).length > 0);
            for (PetSpec.Activity activity : spec.activities) {
                assertTrue(framesFor(pet, activity.animation).length > 0);
            }
        }
        assertSame(PixelCatWidget.WALK_FRAMES, framesFor(PET_CAT, ANIMATION_WALK));
        assertSame(DOG_SIT_FRAMES, framesFor(PET_DOG, ANIMATION_LICK));
        assertSame(RABBIT_SIT_FRAMES, framesFor(PET_RABBIT, ANIMATION_SEAT));
    }

    @Test
    public void aWalkLegFitsOneLoop() {
        // A hand-drawn pet's walk loop goes out and back: at most 2 x 7 frames, within the 16 a loop may have
        int longestLeg = (int) Math.floor(2 * PetBehavior.GROUND_LIMIT / PetBehavior.WALK_STEP + 1e-4);
        assertTrue(2 * longestLeg <= MAX_LOOP_FRAMES);
    }
}
