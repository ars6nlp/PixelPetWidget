"""Before / after sheets of a pet: the module from a git revision next to the current one.

    python tools/sprite_gen/compare.py panda [REV]      (REV defaults to HEAD)

Writes tools/sprite_gen/output/compare_<pet>.png: for every animation a few frames spread over
the loop, "before" on top and "after" below, on the light and the dark background, plus the
first idle frame at 1x and 2x on the widget's scenes.
"""
import importlib.util
import os
import subprocess
import sys
import tempfile

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import sprite_gen as sg  # noqa: E402

N = sg.SIZE
SCALE = 4
PICK = 6
LIGHT, DARK = sg.LIGHT, sg.DARK


def old_module(name, rev):
    source = subprocess.run(['git', 'show', f'{rev}:tools/sprite_gen/{name}.py'], cwd=sg.ROOT,
                            capture_output=True, check=True).stdout
    path = os.path.join(tempfile.mkdtemp(), f'{name}_before.py')
    with open(path, 'wb') as f:
        f.write(source)
    spec = importlib.util.spec_from_file_location(f'{name}_before', path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def frames_of(pet):
    pet.wearing = None

    def grid(layers):
        # A moved frame (sg.Moved) shows its pose where the widget draws it
        if hasattr(layers, 'layers') and hasattr(layers, 'dy'):
            return sg.shift_grid(sg.compose(layers.layers, pet), layers.dx, layers.dy, getattr(layers, 'flip', False))
        return sg.compose(layers, pet)
    return {anim: (spec['interval'], [grid(layers) for layers in spec['frames']()])
            for anim, spec in pet.ANIMATIONS.items()}


def spread(grids, count=PICK):
    if len(grids) <= count:
        return grids
    return [grids[round(i * (len(grids) - 1) / (count - 1))] for i in range(count)]


def cell(pet, grid, bg, scale=SCALE):
    img = Image.new('RGBA', (N, N), bg)
    img.alpha_composite(sg.to_image(grid, pet))
    return img.resize((N * scale, N * scale), Image.NEAREST)


def scenes(pets_frames, label_w):
    backgrounds = [Image.open(os.path.join(sg.RES, 'drawable-nodpi', f)).convert('RGBA')
                   for f in ('widget_background.webp', 'widget_background_2.webp', 'widget_background_3.webp')]
    tiles = []
    for pet, anims in pets_frames:
        sprite = sg.to_image(anims['idle'][1][0], pet)
        for bg in backgrounds:
            for scale in (1, 2):
                crop = bg.crop((bg.width // 2 - 100, int(bg.height * 0.6), bg.width // 2 + 100,
                                int(bg.height * 0.6) + 140)).resize((200, 140))
                s = sprite.resize((N * scale, N * scale), Image.NEAREST)
                crop.alpha_composite(s, ((200 - N * scale) // 2, 140 - N * scale))
                tiles.append(crop)
    per_row = len(backgrounds) * 2
    img = Image.new('RGBA', (label_w + per_row * 204, 2 * 144), (255, 255, 255, 255))
    for i, tile in enumerate(tiles):
        img.paste(tile, (label_w + (i % per_row) * 204, (i // per_row) * 144))
    return img


def main(name, rev='HEAD'):
    before_pet = old_module(name, rev)
    before = frames_of(before_pet)
    after_pet = sg.load_pet(name)
    after = frames_of(after_pet)
    label_w = 90
    width = label_w + PICK * N * SCALE
    rows = []
    for anim in after:
        for label, pet, anims in (('before', before_pet, before), ('after', after_pet, after)):
            if anim not in anims:
                continue
            for bg in (LIGHT, DARK):
                rows.append((f'{anim}\n{label}', pet, spread(anims[anim][1]), bg))
    sheet = Image.new('RGBA', (width, len(rows) * N * SCALE), (255, 255, 255, 255))
    d = ImageDraw.Draw(sheet)
    for r, (label, pet, grids, bg) in enumerate(rows):
        y = r * N * SCALE
        sheet.paste(Image.new('RGBA', (width - label_w, N * SCALE), bg), (label_w, y))
        d.text((6, y + 8), label, fill=(60, 50, 40, 255))
        for c, g in enumerate(grids):
            sheet.alpha_composite(cell(pet, g, bg), (label_w + c * N * SCALE, y))
    scene = scenes([(before_pet, before), (after_pet, after)], label_w)
    ImageDraw.Draw(scene).text((6, 8), 'before\nidle 1x, 2x', fill=(60, 50, 40, 255))
    ImageDraw.Draw(scene).text((6, 152), 'after\nidle 1x, 2x', fill=(60, 50, 40, 255))
    out = Image.new('RGBA', (max(width, scene.width), sheet.height + scene.height + 10), (255, 255, 255, 255))
    out.paste(scene, (0, 0))
    out.paste(sheet, (0, scene.height + 10))
    path = os.path.join(sg.OUT, f'compare_{name}.png')
    out.convert('RGB').save(path)
    print('wrote', os.path.relpath(path, sg.ROOT))


if __name__ == '__main__':
    main(*sys.argv[1:])
