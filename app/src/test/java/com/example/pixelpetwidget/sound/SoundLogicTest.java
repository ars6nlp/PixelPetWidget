package com.example.pixelpetwidget.sound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.pixelpetwidget.PixelCatWidget;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class SoundLogicTest {

    @Test
    public void theSameSoundDoesNotRepeatTooSoon() {
        assertFalse("first time always plays", SoundManager.tooSoon(null, 1000));
        assertTrue(SoundManager.tooSoon(1000L, 1000 + SoundManager.MIN_GAP_MS - 1));
        assertFalse(SoundManager.tooSoon(1000L, 1000 + SoundManager.MIN_GAP_MS));
    }

    @Test
    public void volumeRisesGentlyFromSilenceToFull() {
        assertEquals(0f, SoundManager.gain(0), 0f);
        assertEquals(1f, SoundManager.gain(SoundManager.MAX_VOLUME), 0f);
        float previous = 0f;
        for (int v = 1; v <= SoundManager.MAX_VOLUME; v++) {
            float gain = SoundManager.gain(v);
            assertTrue(gain > previous);
            previous = gain;
        }
    }

    @Test
    public void everyPetHasItsOwnVoiceAndUnknownPetsSoundLikeTheCat() {
        int[] pets = {PixelCatWidget.PET_CAT, PixelCatWidget.PET_DOG, PixelCatWidget.PET_RABBIT,
            PixelCatWidget.PET_PARROT, PixelCatWidget.PET_CAPYBARA, PixelCatWidget.PET_PANDA,
            PixelCatWidget.PET_HAMSTER, PixelCatWidget.PET_AQUARIUM, PixelCatWidget.PET_PUPPY,
            PixelCatWidget.PET_KITTEN};
        Set<Integer> voices = new HashSet<>();
        for (int petType : pets) {
            voices.add(SoundManager.voiceOf(petType, 0));
        }
        assertEquals(pets.length, voices.size());
        assertEquals(SoundManager.voiceOf(PixelCatWidget.PET_CAT, 0), SoundManager.voiceOf(99, 0));
        // The aquarium answers with two sounds in turn; the others always with the same one
        assertTrue(SoundManager.voiceOf(PixelCatWidget.PET_AQUARIUM, 0) != SoundManager.voiceOf(PixelCatWidget.PET_AQUARIUM, 1));
        voices.add(SoundManager.voiceOf(PixelCatWidget.PET_AQUARIUM, 1));
        // So do the puppy (two yips) and the kitten (a mew, then a purr)
        for (int petType : new int[] {PixelCatWidget.PET_PUPPY, PixelCatWidget.PET_KITTEN}) {
            assertTrue(SoundManager.voiceOf(petType, 0) != SoundManager.voiceOf(petType, 1));
            assertEquals(SoundManager.voiceOf(petType, 0), SoundManager.voiceOf(petType, 2));
            voices.add(SoundManager.voiceOf(petType, 1));
        }
        assertEquals(SoundManager.voiceOf(PixelCatWidget.PET_PANDA, 0), SoundManager.voiceOf(PixelCatWidget.PET_PANDA, 1));
        for (int voice : voices) {
            boolean listed = false;
            for (int res : SoundManager.ALL) {
                listed |= res == voice;
            }
            assertTrue("every voice is loaded", listed);
        }
    }
}
