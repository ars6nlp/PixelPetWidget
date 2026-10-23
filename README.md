# Pixel Pet Widget

<div align="center">

![Android API](https://img.shields.io/badge/API-24%2B-blue.svg)
![License](https://img.shields.io/badge/License-MIT-green.svg)
![Java](https://img.shields.io/badge/Java-11-orange.svg)
![Widget](https://img.shields.io/badge/Widget-Home%20Screen-important.svg)

**Adorable animated pet widgets for Android home screens**

Choose your companion: **Pixel Cat**  | **Pixel Dog** | **Pixel Rabbit** | **Pixel Panda** | **Pixel Hamster** | **Pixel Capybara** | **Pixel Parrot** | **Puppy** | **Kitten** | **Aquarium**

[Features](#features) • [Installation](#installation) • [How it works](#how-it-works) • [Contributing](#contributing)

</div>

---
## Features

- **Multiple Pets**: Choose from Cat, Dog, Rabbit, Panda, Hamster, Capybara, Parrot, a corgi Puppy, a grey Kitten or a whole Aquarium with three goldfish - each with unique character animations!
- **Multilingual Support**: Full localization for **English** and **Russian** languages, switched in the settings
- **Cozy pixel interface**: one pixel-art look across the app (frames, icons, the Tiny5 font), light and warm dark
  themes, soft chiptune sounds for buttons and pet voices. The widget itself stays silent.
- **WebP Optimized Animations**: Upgraded from PNG to lightweight WebP sequences for drastically reduced APK size and faster rendering
- **Battery-Friendly (Auto-Sleep)**: Active animations (walking, playing) intelligently pause
- **Rich Animation States**:
  - Cat: Sleep → Lick → Sit → Walk →
  - Dog: Sleep → mouth open → Sit → Walk →
  - Rabbit: Sleep → Eat → Walk →
  - Panda: Sleep → Happy → Idle → Eat bamboo → Roll → Walk →
  - Hamster: Sleep → Happy → Idle → Eat a seed → Stuff cheeks → Scurry →
  - Capybara: Sleep → Happy → Idle → Chew grass → Warm bath with an orange → Stroll →
  - Parrot: Sleep → Flap wings → Idle → Eat a seed → Chatter → Hop →
  - Puppy: Sleep → Jump for joy → Idle → Chew a bone → Chase its tail → Trot →
  - Kitten: Sleep → Hop and purr → Idle → Lap milk → Play with yarn → Walk →
  - Aquarium: Sleep (lamp off) → Joy → Idle → Feeding flakes → Treasure chest → Swim →

- **Precision Scaling & Grounding**: Advanced programmatic padding logic ensures pets are perfectly sized and "grounded" (sitting on the path) across all widget sizes (2x2, 3x3, 4x4) and animation frames.
- **Premium Aesthetics**: Dynamic, beautiful widget backgrounds (day/sunset scenes) using optimal `centerCrop` presentation without stretching.
- **Interactive**: Tap the widget to interact with your pet and cycle through its actions.
- **Widget Limiter**: Hardcoded safety limit (max 5 widgets) to ensure your launcher remains butter-smooth.
- **Reliable**: Auto-restarts after device reboot with `RestartReceiver`

---

### Available Pets

| Pixel Cat | Pixel Dog | Pixel Rabbit | Pixel Panda | Pixel Hamster | Pixel Capybara | Pixel Parrot | Puppy | Kitten | Aquarium |
|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| Sweet & cuddly | Loyal & playful | Fluffy & cute | Sleepy & chill | Quick & thrifty | Calm & friendly | Bright & chatty | Playful & kind | Curious & playful | Calm & cozy |
| 4 actions | 4 actions | 4 actions | 6 actions | 6 actions | 6 actions | 6 actions | 6 actions | 6 actions | 6 actions |

The panda, the hamster, the capybara, the parrot, the puppy (a corgi) and the kitten (a grey tabby)
are true 64×64 pixel art drawn in code by [`tools/sprite_gen`](tools/sprite_gen/README.md) and scaled
by whole numbers only. Their character shows in size and speed: the panda and the capybara are big
and slow, the parrot is medium, the hamster is tiny and quick, the puppy is quick and runs about, the
kitten is the smallest and mostly sits, washes and plays. On the three autumn backgrounds every pet
but the aquarium has its own little scene with each of them.

The aquarium is a whole 128×96 pixel scene from the same generator: a tank with a lamp, plants, a
castle, a treasure chest, an aerator and three goldfish (a slow classic, a slow veiltail and a quick
comet) on fixed routes. It is its own background, so the widget shows it without one, at the largest
whole-number scale that fits.

### The App
One screen: the live preview of the widget with "Add to home screen" (and "Feed" for a pet that is
already there), the pets, the background, the wardrobe and the daily gift. The gear opens the
settings: sound, volume, reminder, night mode, decorations, language and About. The look follows the
phone's light or dark theme.

### Widget Sizes

| Small (2x2) | Medium (3x3) | Large (4x4) |
|:-------------:|:-------------:|:-----------:|
| Close-up crop | Balanced crop | Wide landscape |

---

## Installation

### Method 1: From Source

1. Clone the repository:
   ```bash
   git clone https://github.com/ars6nlp/PixelPetWidget.git
   cd PixelPetWidget
   ```

2. Open in Android Studio and build the project
3. Install on your device/emulator

### Method 2: Download APK

1. Download the latest APK from [Releases](https://github.com/ars6nlp/PixelPetWidget/releases)
2. Enable "Install from unknown sources" in your device settings
3. Open the APK file to install

---

##  Usage

### Selecting Your Pet

1. **Open the Pixel Pet app** from your app drawer.
2. **Pick a friend**: tap a pet card (tap the chosen one again and it is happy to see you).
3. **Choose a background**.
4. **Press "Add to home screen"** and confirm in the launcher's dialog.
5. Language, theme and sounds are in the settings (the gear in the top-right corner).

### Interacting with the Widget

- **Tap to play**: Tap your pet to wake it up and cycle through different actions.
- **Auto-sleep**: Let the widget be, and your pet will naturally fall asleep after 60 seconds to conserve battery.

### Daily Care

- **Food and mood** slowly drop over a day. A hungry pet shows a thought bubble with its favourite food
  on the widget, a lonely one shows a heart; tap the bubble to open the app on that pet.
- **Feed** costs one treat. **Stroke** the pet in the app's preview, and taps on the widget count as play.
- **Daily gift**: treats once a day, bonuses on a streak, a streak freeze for every full week and a bow
  after the first one.
- **Wardrobe** (pixel-art pets): bow, Halloween hat (Oct 15 – Nov 10) and Santa hat (Dec 15 – Jan 10).
  Seasonal decorations appear on every widget during those dates.
- **Night mode**: from 22:00 to 7:00 the widget dims and pets go to sleep.
- **Reminder** (optional, 19:00): only when a gift or a hungry pet is waiting.

Pets never get sick or run away: the care loop is meant to be cozy, not stressful.

#### Testing seasons and night on a debug build

Debug builds read two overrides (release builds ignore them):

```
adb shell am force-stop com.aixdev.pixelpet
adb shell run-as com.aixdev.pixelpet sh -c "cat > shared_prefs/PixelPetSettings.xml" <<'EOF'
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <string name="debug_season">halloween</string>  <!-- halloween, winter or empty -->
    <string name="debug_night">true</string>
</map>
EOF
adb shell am broadcast -n com.aixdev.pixelpet/com.example.pixelpetwidget.PixelCatWidget -a com.example.pixelpetwidget.CARE_TICK
```

### Resizing the Widget

1. **Long press** on the widget
2. **Drag the edges** to resize (from 2x2 to 4x4)
3. The background and pet will **automatically adapt** to the new bounds beautifully.

---

## How It Works

### Architecture

The widget consists of smart components working together symmetrically:

1. **MainActivitySimple.java** - The aesthetic Pet selection UI.
2. **PixelCatWidget.java** - Central AppWidgetProvider containing all the scaling, padding math, and `RemoteViews` layouts.
3. **AnimationService.java** - Foreground service executing ping-pong `ViewFlipper` logic with precise memory management, including the 60-second auto-sleep alarm.
4. **RestartReceiver.java** - BroadcastReceiver that gracefully restores states post-reboot.

### Animation System

```text
┌─────────────────────────────────────────────────────────────┐
│  User taps widget → Toggle action (Walk/Eat/Sit)            │
│                       ↓                                     │
│  AnimationService updates RemoteViews via ViewFlipper       │
│                       ↓                                     │
│  Foreground service + WakeLock acquired                     │
│                       ↓                                     │
│  60 Second AlarmManager intent scheduled                    │
│                       ↓                                     │
│   ... time passes with beautiful 10FPS animation ...        │
│                       ↓                                     │
│  Alarm triggers → ACTION_FORCE_SLEEP_ALL sent               │
│                       ↓                                     │
│  WidgetProvider gracefully resets pets to SLEEP frames      │
│  and releases WakeLocks to conserve battery.                │
└─────────────────────────────────────────────────────────────┘
```

### Technical Details

- **Asset Pipeline**: Migrated fully to `<pet>_<action>_frame_XXX.webp` files to drop app size significantly without visual loss.
- **Frame Rate**: ~10 FPS (relaxed speed, 100ms per frame) for cozy pixel art vibes.
- **Power Management**: `PARTIAL_WAKE_LOCK` combined with `AlarmManager` auto-sleep rules prevent infinite battery drain!
- **State Management**: Robust `SharedPreferences` handling tracking the exact frame, layout size, and sleep state of up to 5 individual widgets.
- **Rendering Tricks**: Custom padding multipliers and ping-pong translation offsets simulating physical space across raw `RemoteViews`.

---

## Interface and Sounds

How the app screens are built and how to change them: [`DESIGN.md`](DESIGN.md).

- **Components** (`app/src/main/java/.../ui/pixel`): PixelButton, PixelCard, PixelSwitch, PixelSlider,
  PixelDialog and PixelLabel: flat colours, a 2dp outline, cut corners.
- **Theme**: colours and font in `ui/theme/PixelTheme.java`, sizes in `res/values/dimens.xml`.
- **Font**: Tiny5 (SIL OFL 1.1, [`licenses/Tiny5-OFL.txt`](licenses/Tiny5-OFL.txt)), prepared by
  `tools/font/prepare_font.py`. Text sizes snap to multiples of 8 px, where the font is crisp.
- **Sounds**: WAV files in `res/raw`, played by `sound/SoundManager.java` on the media volume, never on
  silent or vibrate, and never from the widget.
- **Debug gallery**: in debug builds a long press on the logo shows every component in the light and
  the dark theme.

---

## Requirements

- **Minimum SDK**: API 24 (Android 7.0)
- **Target SDK**: API 36 (Android 16)
- **Language**: Java 11

---

## Contributing

Contributions are welcome! We'd love to see:

- New pet types (fox, owl, fish, etc.)
- New animations within WebP efficiency
- Translations for other languages

To contribute:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

The MIT License covers the source code. The sounds and the graphics (pet sprites, backgrounds,
icons and other artwork under `app/src/main/res`, `art-source` and `tools/sprite_gen/output`) may be
protected separately: all rights reserved unless a file says otherwise. The font Tiny5 keeps its own
licence (SIL OFL 1.1, see [`licenses/Tiny5-OFL.txt`](licenses/Tiny5-OFL.txt)).

---

## Author

**ars6nlp** - [GitHub](https://github.com/ars6nlp)

<div align="center">

[⭐ Star](https://github.com/ars6nlp/PixelPetWidget/stargazers) this repo if you like it!

</div>
