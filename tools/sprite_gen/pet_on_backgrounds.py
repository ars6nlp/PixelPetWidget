"""A pet's frames on the pixel-art backgrounds, stacked as the widget does it (props behind it or in
front, the rain on top), x5: to check how it reads on each scene and how its poses meet the props.

    python tools/sprite_gen/pet_on_backgrounds.py              output/<pet>_on_backgrounds.png for the puppy and the kitten
    python tools/sprite_gen/pet_on_backgrounds.py out.png pet anim:bg:prop:frames ...   (prop '-' for none)
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import sprite_gen as sg  # noqa: E402
import scenery  # noqa: E402
from PIL import Image, ImageDraw  # noqa: E402

DEFAULT = {
    'puppy': ['idle:maple:-:0', 'chase:maple:-:2', 'chase:maple:-:8', 'flop:maple:-:6', 'walk:maple:-:2',
              'turn:maple:-:1', 'look_up:rainy:-:4', 'shake:rainy:-:2', 'eat:rainy:-:1', 'sleep:rainy:-:3',
              'walk:rainy:-:5', 'lie_down:rainy:-:1', 'idle:pumpkin:-:13', 'nose_push_in:pumpkin:pumpkin_small:3',
              'nose_push:pumpkin:pumpkin_small:2', 'happy:pumpkin:-:7'],
    'kitten': ['idle:maple:-:0', 'hide_in:maple:heap:3', 'hide:maple:heap:0', 'pounce:maple:-:9', 'walk:maple:-:2',
               'turn:maple:-:1', 'idle:rainy:-:0', 'bat:rainy:umbrella:4', 'eat:rainy:-:5', 'sleep:rainy:-:3',
               'walk:rainy:-:5', 'lie_down:rainy:-:0', 'idle:pumpkin:-:0', 'throne_in:pumpkin:pumpkin_big:5',
               'throne:pumpkin:pumpkin_big:4', 'throne_out:pumpkin:pumpkin_big:3', 'yarn:pumpkin:-:3',
               'happy:pumpkin:-:7'],
}


def scene(pet, anims, anim, bgname, propname, index, scale=5):
    bg = scenery.load(bgname)
    grid = anims[anim][1][index]
    W, H = 96, 80
    cx = 288 if propname == '-' else int(288 + bg.PROPS[propname]['offset'] * bg.PREVIEW_AMPLITUDE)
    img = scenery.image(bg.SCENE, bg.PALETTE)
    front = []
    for name, prop in bg.PROPS.items():
        sp = scenery.image(prop['states'][prop['ambient']][1][0], bg.PALETTE)
        x = cx - sp.width // 2 if name == propname else int(288 + prop['offset'] * bg.PREVIEW_AMPLITUDE) - sp.width // 2
        at = (x, bg.GROUND - prop['baseline'])
        (front.append((sp, at)) if prop['layer'] == 'front' else img.alpha_composite(sp, at))
    img.alpha_composite(sg.to_image(grid, pet), (cx - 32, bg.GROUND - 61))
    for sp, at in front:
        img.alpha_composite(sp, at)
    for tile in getattr(bg, 'TILES', {}).values():
        frame = scenery.image(tile['frames'][0], bg.PALETTE)
        for ty in range(0, img.height, frame.height):
            for tx in range(0, img.width, frame.width):
                img.alpha_composite(frame, (tx, ty))
    return img.crop((cx - W // 2, bg.GROUND - 70, cx + W // 2, bg.GROUND + 10)).resize((W * scale, H * scale), Image.NEAREST)


def main(out, petname, specs):
    pet, anims = sg.build_pet(petname)
    cells = []
    for spec in specs:
        anim, bgname, propname, idx = spec.split(':')
        for i in [int(v) for v in idx.split(',')]:
            c = scene(pet, anims, anim, bgname, propname, i)
            ImageDraw.Draw(c).text((4, 4), f'{anim} {i} {bgname}', fill=(0, 0, 0))
            cells.append(c)
    cols = 4
    sheet = Image.new('RGB', (cols * 484, ((len(cells) + cols - 1) // cols) * 404), (255, 255, 255))
    for i, c in enumerate(cells):
        sheet.paste(c.convert('RGB'), ((i % cols) * 484, (i // cols) * 404))
    sheet.save(out)


if __name__ == '__main__':
    if len(sys.argv) > 2:
        main(sys.argv[1], sys.argv[2], sys.argv[3:])
    else:
        for name, specs in DEFAULT.items():
            main(os.path.join(sg.OUT, f'{name}_on_backgrounds.png'), name, specs)
