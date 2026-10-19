package com.example.pixelpetwidget.sound;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.SystemClock;

import com.example.pixelpetwidget.PixelCatWidget;
import com.example.pixelpetwidget.R;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Все звуки приложения. Виджет звуков не издаёт и этот класс не использует.
 *
 * <pre>
 * SoundManager.get(context).click();      // кнопка, карточка
 * SoundManager.get(context).toggle();     // переключатель
 * SoundManager.get(context).success();    // подарок получен, виджет добавлен
 * SoundManager.get(context).error();      // нельзя (нет лакомств, вещь закрыта)
 * SoundManager.get(context).pet(petType); // голос питомца
 * SoundManager.get(context).chosen(petType);   // питомца выбрали (у аквариума — бульканье)
 * SoundManager.get(context).success(petType);  // успех рядом с питомцем (у аквариума — капелька)
 * </pre>
 * Файлы лежат в res/raw. Чтобы заменить звук, положите WAV с тем же именем; чтобы добавить —
 * положите файл, добавьте его в {@link #ALL} и метод, который его играет.
 *
 * Звуки идут на громкость медиа и не играют, когда звук выключен в настройках, громкость 0 или
 * телефон в беззвучном режиме. Один и тот же звук не повторяется чаще раза в {@link #MIN_GAP_MS}.
 */
public final class SoundManager {

    // --- Список звуков ----------------------------------------------------------------------
    private static final int CLICK = R.raw.sfx_click;
    private static final int TOGGLE = R.raw.sfx_toggle;
    private static final int SUCCESS = R.raw.sfx_success;
    private static final int ERROR = R.raw.sfx_error;

    /**
     * Голос питомца: по одному файлу на питомца; у аквариума, щенка и котёнка два звука по очереди
     * (аквариум — «блюп» и «плюх», щенок — два «тяв», котёнок — «мяу» и мурлыканье).
     */
    static int voiceOf(int petType, int turn) {
        switch (petType) {
            case PixelCatWidget.PET_AQUARIUM: return turn % 2 == 0 ? R.raw.voice_aquarium_1 : R.raw.voice_aquarium_2;
            case PixelCatWidget.PET_PUPPY: return turn % 2 == 0 ? R.raw.voice_puppy_1 : R.raw.voice_puppy_2;
            case PixelCatWidget.PET_KITTEN: return turn % 2 == 0 ? R.raw.voice_kitten_1 : R.raw.voice_kitten_2;
            case PixelCatWidget.PET_DOG: return R.raw.voice_dog;
            case PixelCatWidget.PET_RABBIT: return R.raw.voice_rabbit;
            case PixelCatWidget.PET_PARROT: return R.raw.voice_parrot;
            case PixelCatWidget.PET_CAPYBARA: return R.raw.voice_capybara;
            case PixelCatWidget.PET_PANDA: return R.raw.voice_panda;
            case PixelCatWidget.PET_HAMSTER: return R.raw.voice_hamster;
            default: return R.raw.voice_cat;
        }
    }

    // Аквариум: бульканье при выборе и мягкая «капелька» вместо обычного успеха
    private static final int AQUARIUM_CHOSEN = R.raw.sfx_aquarium_pick;
    private static final int AQUARIUM_SUCCESS = R.raw.sfx_aquarium_drop;

    static final int[] ALL = {CLICK, TOGGLE, SUCCESS, ERROR,
        R.raw.voice_cat, R.raw.voice_dog, R.raw.voice_rabbit, R.raw.voice_parrot,
        R.raw.voice_capybara, R.raw.voice_panda, R.raw.voice_hamster,
        R.raw.voice_aquarium_1, R.raw.voice_aquarium_2, AQUARIUM_CHOSEN, AQUARIUM_SUCCESS,
        R.raw.voice_puppy_1, R.raw.voice_puppy_2, R.raw.voice_kitten_1, R.raw.voice_kitten_2};

    // --- Настройки (экран «Настройки») ------------------------------------------------------
    private static final String PREFS = "PixelPetSettings";
    private static final String KEY_SOUND = "sound_enabled";
    private static final String KEY_VOLUME = "sound_volume";
    public static final int MAX_VOLUME = 10;
    private static final int DEFAULT_VOLUME = 7;

    /** Защита от спама: один и тот же звук не чаще раза в столько миллисекунд. */
    static final long MIN_GAP_MS = 100;

    private static SoundManager instance;

    private final SharedPreferences prefs;
    private final AudioManager audio;
    private final SoundPool pool;
    private final Map<Integer, Integer> ids = new HashMap<>();
    private final Set<Integer> loaded = new HashSet<>();
    private final Map<Integer, Long> lastPlayed = new HashMap<>();
    private int voiceTurn;

    public static synchronized SoundManager get(Context context) {
        if (instance == null) {
            instance = new SoundManager(context.getApplicationContext());
        }
        return instance;
    }

    /** Освобождает звуки, когда приложение закрывается. */
    public static synchronized void release() {
        if (instance != null) {
            instance.pool.release();
            instance = null;
        }
    }

    private SoundManager(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        pool = new SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build())
            .build();
        pool.setOnLoadCompleteListener((p, id, status) -> {
            if (status == 0) {
                loaded.add(id);
            }
        });
        for (int res : ALL) {
            ids.put(res, pool.load(context, res, 1));     // загрузка идёт в фоне
        }
    }

    public void click() {
        play(CLICK);
    }

    public void toggle() {
        play(TOGGLE);
    }

    public void success() {
        play(SUCCESS);
    }

    public void error() {
        play(ERROR);
    }

    public void pet(int petType) {
        play(voiceOf(petType, voiceTurn++));
    }

    /** Питомца выбрали в списке: аквариум отвечает бульканьем, остальным хватает щелчка карточки. */
    public void chosen(int petType) {
        if (petType == PixelCatWidget.PET_AQUARIUM) {
            play(AQUARIUM_CHOSEN);
        }
    }

    /** Успех, когда на экране этот питомец: у аквариума своя «капелька». */
    public void success(int petType) {
        play(petType == PixelCatWidget.PET_AQUARIUM ? AQUARIUM_SUCCESS : SUCCESS);
    }

    // --- Настройки звука --------------------------------------------------------------------

    public boolean soundOn() {
        return prefs.getBoolean(KEY_SOUND, true);
    }

    public void setSoundOn(boolean on) {
        prefs.edit().putBoolean(KEY_SOUND, on).apply();
    }

    /** Громкость от 0 до {@link #MAX_VOLUME}. */
    public int volume() {
        return Math.max(0, Math.min(MAX_VOLUME, prefs.getInt(KEY_VOLUME, DEFAULT_VOLUME)));
    }

    public void setVolume(int volume) {
        prefs.edit().putInt(KEY_VOLUME, Math.max(0, Math.min(MAX_VOLUME, volume))).apply();
    }

    // --- Проигрывание -----------------------------------------------------------------------

    private void play(int res) {
        boolean ringerNormal = audio == null || audio.getRingerMode() == AudioManager.RINGER_MODE_NORMAL;
        Integer id = ids.get(res);
        if (!soundOn() || volume() == 0 || !ringerNormal || id == null || !loaded.contains(id)) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        if (tooSoon(lastPlayed.get(res), now)) {
            return;
        }
        lastPlayed.put(res, now);
        float gain = gain(volume());
        pool.play(id, gain, gain, 1, 0, 1f);
    }

    /** Тот же звук только что играл. */
    static boolean tooSoon(Long last, long now) {
        return last != null && now - last < MIN_GAP_MS;
    }

    /** Громкость 0..1: мягкая кривая, чтобы тихие деления тоже были слышны. */
    static float gain(int volume) {
        return (float) Math.pow(volume / (float) MAX_VOLUME, 1.5);
    }
}
