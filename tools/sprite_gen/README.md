# Sprite generator

Draws the pixel-art pets (panda, hamster, capybara, parrot, puppy, kitten) and the aquarium scene from text pixel
matrices and exports them in the format the widget uses.

```
python tools/sprite_gen/sprite_gen.py            # frames, Java arrays, widget preview, previews
python tools/sprite_gen/sprite_gen.py preview    # only output/ previews and GIFs
python tools/sprite_gen/sprite_gen.py dump panda idle 3   # copy a frame to overrides/ for hand editing
```

Requires Python 3 and Pillow.

## Files
- `sprite_gen.py` - engine: parses parts, composes layers, outlines, shading, export.
- `panda.py`, `hamster.py`, `capybara.py`, `parrot.py`, `puppy.py`, `kitten.py` - palette (16 colours; up to 20 with
  `MAX_COLOURS`, for the leaves of the background poses), parts as text
  matrices, poses and animations (`ANIMATIONS`: frame interval and frame builder per animation),
  `ACCESSORIES` and `NO_ACCESSORY` (animations where the pet wears nothing: the panda's roll,
  the capybara's bath).
- `aquarium.py` - a whole scene instead of a pet: `CANVAS = (128, 96)`, 23 colours (`MAX_COLOURS`),
  the tank, decor and three fish. Fish swim on fixed routes (`ROUTES`: polyline laps and keyframes,
  no randomness); `OBSTACLES` lists the decor boxes, and its `check()` keeps every fish inside the
  water and out of the decor in every frame. Written directly, not by a design script.
- `overrides/` - optional hand-edited frames that replace composed ones.
- `output/` - `preview_sheet*.png` (x8, light and dark background), one GIF per animation,
  `widget_preview.png` (all four pets, for the widget picker).

## What gets written
- `app/src/main/res/drawable-nodpi/raw_<pet>_<anim>_frame_NNN.png` - 64x64 frames,
  alpha 0 or 255 only, duplicates removed
- `app/src/main/res/drawable/<pet>_<anim>_frame_NNN.xml` - `<bitmap android:filter="false">`
  wrappers so the launcher scales them nearest-neighbour
- `app/src/main/res/drawable*/pixel_<pet>_icon.*` - icon for the pet list
- `app/src/main/res/drawable-nodpi/widget_preview.webp` - widget picker preview
- `app/src/main/res/drawable*/pixel_<pet>_acc_<item>.*` - accessory pictures for the wardrobe
- `app/src/main/java/.../GeneratedPetFrames.java` - frame arrays and intervals, plus lookups by name
  (`frames`, `interval`, `icon`, `accessoryIcon`, `withAccessory`) used by `PetSpec` and `Wardrobe`

## Conventions
- Canvas 64x64, pets face left, feet on row 63, same scale in every animation.
- One outline colour for every pet (`#1f2133`, deep blue-grey, not black); alpha 0 or 255 only.
- Nothing may touch the left, right or top edge of the canvas (it would look cut off on the widget).
- Adding a pet: a module here, its name in `PETS`, then one `pixelPet(...)` line in `PetSpec`.
- Walk cycles have 8 frames. A pet with smooth motion sets `WALK_SHIFTS`: how many art pixels its
  planted paws move back in each walk frame; the widget moves the pet exactly so far
  (`PetSpec` gait, checked by `PetSpecTest`), so the paws never slide.
- Loops have at most 24 entries (`PetSpec.PIXEL_LOOP_FRAMES`); a scene may have 32
  (`PetSpec.SCENE_LOOP_FRAMES`), and its frames are whole pictures, so it is never moved or padded.
- Smooth motion: every loop starts and ends in the pet's neutral pose (the first idle frame); a
  pet may add transitions the widget plays inside other animations: `turn` (towards the viewer,
  played there and back), `walk_start`, `walk_stop`, `lie_down`, `get_up`, and for a pose of an
  interaction `<pose>_in` / `<pose>_out` (onto the pumpkin, into the heap). List them in `ONE_SHOT`;
  poses off the ground (on a prop) in `OFF_GROUND`. `L(..., cut=row)` hides a layer from that row
  down (sunk into a heap drawn behind it); `CLEAN_ORPHANS = True` repaints stray pixels where layers
  meet.
- `motion_check.py PET`: feet on row 60, pivot, jumps, joins with the neutral pose and the paws
  against `WALK_SHIFTS`; writes `output/<pet>_walk_on_ground.png`. `life_gif.py PET` turns a life
  from the app's engine (`SmoothMotionTest` writes `app/build/lives/`) into `output/<pet>_life.gif`.
- Light comes from the top left; contours are added by the engine, don't draw them.

## Backgrounds
`scenery.py` draws the pixel-art backgrounds (`bg_maple.py`, `bg_rainy.py`, `bg_pumpkin.py`; see its
docstring) and writes `raw_bg_*` / `bg_*` drawables and `GeneratedBackgrounds.java`:

```
python tools/sprite_gen/scenery.py          # checks, resources, previews (bg_<name>_x2.png, _sprites_x4.png, _elements.gif)
python tools/sprite_gen/scenery.py check    # palette, alpha and stray-pixel checks only
```

- Canvas 576x288, the ground line at row `GROUND`; the widget scales the whole background by the
  pet's pixel scale, so a background pixel is a pet pixel.
- Props stand at a ground position (`offset`, the pets' -0.9..0.9), on the back or front layer
  (`layer`: the pet goes behind a front prop), with named states; `ambient` is the one they show
  when nothing happens.
- Adding a background: a `bg_<name>.py` module, its name in `BACKGROUNDS`, a bg_type and a
  `Backdrop` in `Backdrop.java`, a card in `layout/part_bg_cards.xml`, and one row per pet in
  `BackgroundEvents.TABLE`.
- The pixel pets' poses for their interactions with the backgrounds (`fan`, `drink`, `hug`,
  `nibble`, `umbrella`, `balance`, `leaf_hat`, `lie`, `leaf`; the puppy's `flop`, `look_up`, `shake`,
  `nose_push`; the kitten's `hide`, `pounce`, `bat`, `throne`) are ordinary animations of the pet
  modules (written in `design/<pet>_template.py`); `PetSpec` registers them with `.poses(...)` (up to
  four). Their maple-leaf colours are the maple park's own. A pose that meets a prop (ears poking out
  of the heap, lying on the big pumpkin, the nose on the little one) is drawn against the prop's
  outline, the prop standing at the middle of the pet's frame.
- `interactions.py` plays every row of `BackgroundEvents.TABLE` into `output/interaction_<bg>_<pet>.gif`
  and a key-frame sheet per background; `pet_on_backgrounds.py` puts a pet's frames on the three
  scenes (`output/<pet>_on_backgrounds.png`) to check contrast and how the poses meet the props.
