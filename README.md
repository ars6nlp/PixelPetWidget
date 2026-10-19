# Pixel Pet Widget

<div align="center">

![Android API](https://img.shields.io/badge/API-24%2B-blue.svg)
![License](https://img.shields.io/badge/License-MIT-green.svg)
![Java](https://img.shields.io/badge/Java-11-orange.svg)
![Widget](https://img.shields.io/badge/Widget-Home%20Screen-important.svg)

**Adorable animated pet widgets for Android home screens**
Choose your companion: **Pixel Cat**  | **Pixel Dog** | **Pixel Rabbit** | **Pixel Panda** | **Pixel Hamster** | **Pixel Capybara** | **Pixel Parrot** | **Puppy** | **Kitten** | **Aquarium**
[Features](#features) • [Installation](#installation) • [Screenshots](#screenshots) • [How it works](#how-it-works) • [Contributing](#contributing)

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
