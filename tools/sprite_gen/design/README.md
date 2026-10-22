# Pet design sources

The six pixel pets (panda, hamster, capybara, parrot, puppy, kitten) are written into `tools/sprite_gen/<pet>.py`
by the scripts in this folder. The pet modules are plain text pixel matrices and a palette, so
they can be edited by hand. These scripts show where the matrices come from and rebuild them.

- `shapes.py` holds helpers for big shaded shapes: ellipse masks, three tones with light from the
  top left and an even rim, and a cleanup pass for stray pixels.
- `<pet>_template.py` is the pet module with `{PLACEHOLDERS}` for the big shapes. Small parts
  (eyes, paws, props, accessories), poses and animations are written here directly.
- `<pet>_design.py` generates the big shapes (head, body, wings, tail feathers, water and so on),
  fills the placeholders and writes `tools/sprite_gen/<pet>.py`.

To change a pet, from the repo root:

```
python tools/sprite_gen/design/hamster_design.py   # rewrite tools/sprite_gen/hamster.py
python tools/sprite_gen/check.py hamster            # palette, alpha, clipping, pivot, stray pixels
python tools/sprite_gen/sprite_gen.py preview       # sheets and GIFs in tools/sprite_gen/output/
python tools/sprite_gen/compare.py hamster          # before/after against HEAD
python tools/sprite_gen/sprite_gen.py               # app resources and GeneratedPetFrames.java
```

If frame counts or intervals change, update `PetSpecTest.frameCountsMatchTheGeneratedSprites`.
A stationary loop has at most 24 frames, a walk cycle has 8, and all frames of a loop play at one
interval, so a held pose is a repeated frame. After a change also run
`python tools/sprite_gen/motion_check.py <pet>` (feet, joins, paws against WALK_SHIFTS).
