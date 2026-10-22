package com.example.pixelpetwidget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Sounds belong to every pet, the widget stays silent and outside the new interface, and every
 * text exists in English and Russian. Reads the sources from disk (the module directory is the
 * working directory of unit tests).
 */
public class SoundAndUiCatalogTest {

    private static final File MAIN = new File("src/main");
    private static final File JAVA = new File(MAIN, "java/com/example/pixelpetwidget");

    // --- Sounds -----------------------------------------------------------------------------

    @Test
    public void appSoundsAreFewAndSmall() throws Exception {
        String[] expected = {"sfx_click.wav", "sfx_toggle.wav", "sfx_success.wav", "sfx_error.wav",
            "voice_cat.wav", "voice_dog.wav", "voice_rabbit.wav", "voice_parrot.wav", "voice_capybara.wav",
            "voice_panda.wav", "voice_hamster.wav",
            // the aquarium: two reactions, choosing it and its own success
            "voice_aquarium_1.wav", "voice_aquarium_2.wav", "sfx_aquarium_pick.wav", "sfx_aquarium_drop.wav",
            // the puppy's two yips, the kitten's mew and purr
            "voice_puppy_1.wav", "voice_puppy_2.wav", "voice_kitten_1.wav", "voice_kitten_2.wav"};
        File[] raw = new File(MAIN, "res/raw").listFiles();
        assertNotNull(raw);
        Set<String> names = new HashSet<>();
        long total = 0;
        for (File file : raw) {
            names.add(file.getName());
            assertTrue(file.getName() + " is small", file.length() <= 30 * 1024);
            total += file.length();
        }
        assertEquals(new HashSet<>(Arrays.asList(expected)), names);
        assertTrue("all sounds together stay under 200 KB", total < 200 * 1024);
    }

    // --- The widget stays as it was ---------------------------------------------------------

    @Test
    public void widgetCodeNeverPlaysSoundsOrUsesThePixelInterface() throws Exception {
        String[] widgetSide = {"PixelCatWidget.java", "PetLife.java", "PetBehavior.java", "RestartReceiver.java",
            "ReminderReceiver.java", "CareScheduler.java", "WidgetConfigActivity.java", "PetSpec.java",
            "GeneratedPetFrames.java", "CareStore.java", "PetSettings.java"};
        for (String name : widgetSide) {
            String source = read(new File(JAVA, name));
            for (String forbidden : new String[] {"SoundManager", "SoundPool", "MediaPlayer", ".sound.",
                    ".ui.pixel.", ".ui.theme."}) {
                assertFalse(name + " mentions " + forbidden, source.contains(forbidden));
            }
        }
        for (String layout : new String[] {"pixel_cat_widget.xml", "widget_preview.xml", "item_pet_frame.xml"}) {
            String xml = read(new File(MAIN, "res/layout/" + layout));
            assertFalse(layout + " uses pixel components", xml.contains("ui.pixel"));
        }
    }

    @Test
    public void appColoursNeverReachTheWidget() throws Exception {
        // App colours live in PixelTheme; no night resources, so the widget looks the same at night
        assertFalse(new File(MAIN, "res/values-night").exists());
        String colors = read(new File(MAIN, "res/values/colors.xml"));
        assertFalse(colors.contains("px_"));
    }

    // --- Texts ------------------------------------------------------------------------------

    @Test
    public void everyTextExistsInEnglishAndRussian() throws Exception {
        Map<String, String> en = strings(new File(MAIN, "res/values/strings.xml"), true);
        Map<String, String> ru = strings(new File(MAIN, "res/values-ru/strings.xml"), false);
        for (String key : en.keySet()) {
            assertTrue("no Russian text for " + key, ru.containsKey(key));
        }
        for (String key : ru.keySet()) {
            assertTrue("Russian text without English: " + key, en.containsKey(key));
        }
        for (String key : new String[] {"settings_sound", "settings_volume_level", "settings_language",
                "about_button", "about_text", "about_licence", "widget_added", "back", "close", "section_pets",
                "add_friend_button", "gift_come_back", "care_add_hint", "feed_button_count", "pet_name_puppy",
                "pet_name_kitten", "pet_puppy_desc", "pet_kitten_desc"}) {
            assertTrue(key, en.containsKey(key) && !en.get(key).isEmpty() && !ru.get(key).isEmpty());
        }
    }

    @Test
    public void theFontLicenceShipsWithTheApp() throws Exception {
        String licence = read(new File(MAIN, "assets/licenses/Tiny5-OFL.txt"));
        assertTrue(licence.contains("SIL OPEN FONT LICENSE"));
        assertTrue(new File(MAIN, "res/font/tiny5_regular.ttf").isFile());
        assertTrue(new File("../licenses/Tiny5-OFL.txt").isFile());
    }

    @Test
    public void theLicenceReadsAsParagraphsOnAPhone() throws Exception {
        String text = SettingsPage.paragraphs(read(new File(MAIN, "assets/licenses/Tiny5-OFL.txt")));
        assertTrue(text.startsWith("Copyright"));
        assertTrue(text.contains("PREAMBLE"));
        for (String paragraph : text.split("\n\n")) {
            assertFalse("no hard line breaks inside: " + paragraph, paragraph.contains("\n"));
            assertFalse("no rulers of dashes", paragraph.matches("-+"));
        }
    }

    /** Translatable strings of a file (all of them for the default locale, which marks the others). */
    private static Map<String, String> strings(File file, boolean skipUntranslatable) throws Exception {
        Map<String, String> result = new HashMap<>();
        NodeList nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            .getElementsByTagName("string");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element e = (Element) nodes.item(i);
            if (skipUntranslatable && "false".equals(e.getAttribute("translatable"))) {
                continue;
            }
            result.put(e.getAttribute("name"), e.getTextContent());
        }
        return result;
    }

    private static String read(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
}
