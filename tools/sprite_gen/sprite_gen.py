"""Pixel Pet sprite generator.

Pets are drawn from small text pixel matrices ("parts"): one character per pixel,
'.' is transparent, every other character is a palette key. A frame is a list of
layers (part + position + options); the engine composes them on a 64x64 canvas:

  * ring=True   the part is drawn like a sticker: a 1 px outline around it is painted
                over whatever lies below. Rings that end up on the silhouette edge
                become the outline colour, rings inside the figure become a softer
                line colour (LINE_OF), so contours stay uniform in every frame.
  * shade=True  the pet's SHADE rules add shadows/highlights from the part's own
                shape (light from the top left), so shading is consistent too.
  * clip=True   decals (eyes, patches, mouths) are only painted over filled pixels.

Any frame can be replaced by hand: `python sprite_gen.py dump panda idle 3` writes
overrides/panda_idle_003.txt; edit it and rebuild, the file wins over the composed
frame. Alpha is always 0 or 255 and a palette has at most 16 colours (a module may raise
MAX_COLOURS, up to 24).

A module may set CANVAS = (w, h) to draw a whole scene instead of a 64x64 pet: aquarium.py is
a 128x96 tank with three fish, exported as full frames that the widget shows without a background.

Pets: panda, hamster, capybara, parrot, puppy, kitten; scene: aquarium. Accessories: each pet module lists ACCESSORIES; the engine builds
every animation again with `pet.wearing` set, and exports those frames as extra arrays
(GeneratedPetFrames.withAccessory). props.py holds the widget's need bubbles and seasonal
decorations, exported as prop_* drawables.

The pet modules themselves are written by design/<pet>_design.py (see design/README.md).
Frames are exported as indexed PNGs: palette index 0 is transparent, the rest opaque.

Usage (from the repo root):
  python tools/sprite_gen/sprite_gen.py            build everything
  python tools/sprite_gen/sprite_gen.py preview    previews and GIFs only
  python tools/sprite_gen/sprite_gen.py dump PET ANIM N [ACCESSORY]
  python tools/sprite_gen/check.py [PET]           palette, alpha, clipping, pivot, stray pixels
  python tools/sprite_gen/compare.py PET [REV]     before/after sheet against a git revision
"""
import glob
import hashlib
import importlib
import os
import sys
import textwrap

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..', '..'))
RES = os.path.join(ROOT, 'app', 'src', 'main', 'res')
JAVA_DIR = os.path.join(ROOT, 'app', 'src', 'main', 'java', 'com', 'example', 'pixelpetwidget')
OUT = os.path.join(HERE, 'output')
OVERRIDES = os.path.join(HERE, 'overrides')

SIZE = 64            # canvas size of a pet frame, in pixels (a module may set CANVAS = (w, h))
MAX_COLOURS = 24     # a module may allow more than 16 colours with MAX_COLOURS, never more than this
PREVIEW_SCALE = 8
PETS = ('panda', 'hamster', 'capybara', 'parrot', 'aquarium', 'puppy', 'kitten')
RING = '#'


# --- parts -------------------------------------------------------------------------------

def part(text):
    """Parses a text matrix into a list of equal-length rows."""
    rows = [r.rstrip() for r in textwrap.dedent(text).strip('\n').split('\n')]
    width = max(len(r) for r in rows)
    return [r.ljust(width, '.') for r in rows]


def flip(rows):
    return [r[::-1] for r in rows]


def rotate(rows, quarter_turns):
    """Rotates clockwise by 90 degrees per turn; exact for pixel art."""
    for _ in range(quarter_turns % 4):
        rows = [''.join(rows[len(rows) - 1 - y][x] for y in range(len(rows))) for x in range(len(rows[0]))]
    return rows


def grow(rows, extra):
    """Stretches a part vertically by repeating its middle row (breathing, squash)."""
    if extra <= 0:
        return rows
    mid = len(rows) // 2
    return rows[:mid] + [rows[mid]] * extra + rows[mid:]


def squash(rows, less):
    """Shrinks a part vertically by dropping rows around the middle."""
    if less <= 0:
        return rows
    mid = len(rows) // 2
    return rows[:mid - less // 2] + rows[mid + (less - less // 2):]


def remap(rows, mapping):
    return [''.join(mapping.get(c, c) for c in r) for r in rows]


class L:
    """One layer of a frame."""

    def __init__(self, rows, x, y, ring=True, shade=True, clip=False, flip_h=False, rot=0,
                 recolor=None, grow_y=0, squash_y=0, cut=None):
        rows = grow(rows, grow_y)
        rows = squash(rows, squash_y)
        if flip_h:
            rows = flip(rows)
        rows = rotate(rows, rot)
        if recolor:
            rows = remap(rows, recolor)
        self.rows, self.x, self.y = rows, x, y
        self.ring, self.shade, self.clip = ring, shade, clip
        # Canvas row from which on the layer is hidden (sunk into something drawn behind it), or None
        self.cut = cut


class Moved:
    """A frame that is another pose moved by (dx, dy) canvas pixels, and mirrored when flip: the
    pose is stored once and the widget draws it moved (a jump onto the pumpkin is one crouch at
    several heights, a spin shows the other side by mirroring). A module's animation may return
    these among its layer lists; previews and checks see the moved picture."""

    def __init__(self, layers, dx, dy, flip=False):
        self.layers, self.dx, self.dy, self.flip = layers, dx, dy, flip


class Grid(list):
    """A composed frame as it looks (rows of keys); `moved` is (base grid, dx, dy, flip) when the
    widget draws it as the base grid moved, else None."""
    moved = None


def shift_grid(grid, dx, dy, flip=False):
    """The picture mirrored (flip, about the canvas's middle as the widget mirrors it) and moved by
    (dx, dy) on a canvas of the same size, for previews and checks (the widget moves the whole
    stored pose, so a hat that rises above the canvas here still shows)."""
    h, w = len(grid), len(grid[0])
    out = [['.'] * w for _ in range(h)]
    for y, row in enumerate(grid):
        for x, c in enumerate(row):
            tx = (w - 1 - x if flip else x) + dx
            if c != '.' and 0 <= tx < w and 0 <= y + dy < h:
                out[y + dy][tx] = c
    return [''.join(r) for r in out]


# --- composition -------------------------------------------------------------------------

def _shade(rows, rules):
    h, w = len(rows), len(rows[0])

    def inside(x, y):
        return 0 <= x < w and 0 <= y < h and rows[y][x] != '.'

    out = [list(r) for r in rows]
    for base, colour, (dx, dy) in rules:
        for y in range(h):
            for x in range(w):
                if rows[y][x] == base and out[y][x] == base and not inside(x + dx, y + dy):
                    out[y][x] = colour
    return [''.join(r) for r in out]


def compose(layers, pet, size=(SIZE, SIZE)):
    width, height = size
    canvas = [['.'] * width for _ in range(height)]

    def inside_canvas(x, y):
        return 0 <= x < width and 0 <= y < height

    def filled(x, y):
        return inside_canvas(x, y) and canvas[y][x] not in ('.', RING)

    for layer in layers:
        rows = _shade(layer.rows, pet.SHADE) if layer.shade else layer.rows
        h, w = len(rows), len(rows[0])
        cut = layer.cut if layer.cut is not None else height
        mask = {(layer.x + x, layer.y + y) for y in range(h) for x in range(w)
                if rows[y][x] != '.' and layer.y + y < cut}
        if layer.ring:
            for (px, py) in mask:
                for nx, ny in ((px + 1, py), (px - 1, py), (px, py + 1), (px, py - 1)):
                    if (nx, ny) not in mask and inside_canvas(nx, ny) and ny <= cut:
                        canvas[ny][nx] = RING
        for y in range(h):
            for x in range(w):
                c = rows[y][x]
                cx, cy = layer.x + x, layer.y + y
                if c == '.' or not inside_canvas(cx, cy) or cy >= cut:
                    continue
                if layer.clip and not filled(cx, cy):
                    continue
                canvas[cy][cx] = c

    # Resolve rings: silhouette edge -> outline colour, inside the figure -> line colour
    def luminance(key):
        r, g, b = _rgb(pet.PALETTE[key])
        return 0.299 * r + 0.587 * g + 0.114 * b

    resolved = [row[:] for row in canvas]
    for y in range(height):
        for x in range(width):
            if canvas[y][x] != RING:
                continue
            around = [canvas[ny][nx] if inside_canvas(nx, ny) else '.'
                      for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1))]
            near8 = [canvas[ny][nx] for nx in (x - 1, x, x + 1) for ny in (y - 1, y, y + 1)
                     if inside_canvas(nx, ny) and (nx, ny) != (x, y)]
            fills = [c for c in around if c not in ('.', RING)]
            if not any(c not in ('.', RING) for c in near8):
                resolved[y][x] = '.'          # orphaned ring, its part was painted over
            elif '.' in around or not fills:
                resolved[y][x] = pet.OUTLINE
            else:
                resolved[y][x] = min((pet.LINE_OF.get(c, pet.OUTLINE) for c in fills), key=luminance)
    return [''.join(r) for r in resolved]


def _rgb(hex_colour):
    h = hex_colour.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def clean_orphans(grid, pet):
    """Stray pixels left where layers meet (a highlight cut off by a stripe, a pixel of fur between
    two lines) take the colour most of their neighbours have. Outline and line colours and the
    module's ORPHAN_OK details stay. For modules that set CLEAN_ORPHANS."""
    allowed = {pet.OUTLINE} | set(pet.LINE_OF.values())
    details = getattr(pet, 'ORPHAN_OK', ())
    h, w = len(grid), len(grid[0])
    out = [list(r) for r in grid]
    for y in range(h):
        for x in range(w):
            c = grid[y][x]
            if c == '.' or c in allowed:
                continue
            near = [grid[y + dy][x + dx] for dx in (-1, 0, 1) for dy in (-1, 0, 1)
                    if (dx or dy) and 0 <= x + dx < w and 0 <= y + dy < h]
            if c in near or any(c == colour and touch in near for colour, touch in details):
                continue
            n4 = [grid[ny][nx] for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1))
                  if 0 <= nx < w and 0 <= ny < h and grid[ny][nx] not in ('.', pet.OUTLINE)]
            if n4:
                out[y][x] = max(sorted(set(n4)), key=n4.count)  # ties: always the same colour
    return [''.join(r) for r in out]


def to_image(grid, pet):
    img = Image.new('RGBA', (len(grid[0]), len(grid)), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(grid):
        for x, c in enumerate(row):
            if c != '.':
                px[x, y] = _rgb(pet.PALETTE[c]) + (255,)
    return img


def save_png(img, path):
    """Saves pixel art as an indexed PNG: palette index 0 is transparent, every other pixel is
    opaque. About half the size of RGBA and decoded to the same pixels."""
    pixels = list(img.getdata())
    colours = sorted({p[:3] for p in pixels if p[3] == 255})
    if len(colours) > 255 or any(0 < p[3] < 255 for p in pixels):
        img.save(path, optimize=True)
        return
    index = {c: i + 1 for i, c in enumerate(colours)}
    indexed = Image.new('P', img.size, 0)
    indexed.putdata([index[p[:3]] if p[3] == 255 else 0 for p in pixels])
    indexed.putpalette([0, 0, 0] + [v for c in colours for v in c])
    indexed.save(path, optimize=True, transparency=0)


# --- building ----------------------------------------------------------------------------

def load_pet(name):
    if HERE not in sys.path:
        sys.path.insert(0, HERE)
    pet = importlib.import_module(name)
    limit = getattr(pet, 'MAX_COLOURS', 16)
    assert limit <= MAX_COLOURS and len(pet.PALETTE) <= limit, f'{name}: palette has {len(pet.PALETTE)} colours'
    return pet


def canvas(pet):
    """Frame size (width, height) of a pet: 64x64 unless the module sets CANVAS."""
    return getattr(pet, 'CANVAS', (SIZE, SIZE))


def build_pet(name, accessory=None):
    """Returns (pet, {anim: (interval_ms, [grid, ...])}) with overrides applied.

    With an accessory every frame is built with the pet wearing it; animations listed in
    the pet's NO_ACCESSORY are skipped (the widget falls back to the plain frames).
    """
    pet = load_pet(name)
    pet.wearing = accessory
    suffix = f'_{accessory}' if accessory else ''
    result = {}
    try:
        for anim, spec in pet.ANIMATIONS.items():
            if accessory and anim in pet.NO_ACCESSORY:
                continue
            grids = []
            for i, layers in enumerate(spec['frames']()):
                path = os.path.join(OVERRIDES, f'{name}_{anim}{suffix}_{i:03d}.txt')
                w, h = canvas(pet)
                # (by its fields: run as a script, this module is not the one the pet modules import)
                moved = layers if hasattr(layers, 'layers') and hasattr(layers, 'dy') else None
                if moved:
                    layers = moved.layers
                grid = part(open(path, encoding='utf-8').read()) if os.path.exists(path) else compose(layers, pet, (w, h))
                if getattr(pet, 'CLEAN_ORPHANS', False) and not os.path.exists(path):
                    grid = clean_orphans(grid, pet)
                if moved:
                    base = grid
                    flip = getattr(moved, 'flip', False)
                    grid = Grid(shift_grid(base, moved.dx, moved.dy, flip))
                    grid.moved = (base, moved.dx, moved.dy, 1 if flip else 0)
                else:
                    grid = Grid(grid)
                assert len(grid) == h and all(len(r) == w for r in grid), \
                    f'{name} {anim}{suffix} {i}: not {w}x{h}'
                unknown = {c for r in grid for c in r} - set(pet.PALETTE) - {'.'}
                assert not unknown, f'{name} {anim}{suffix} {i}: unknown colours {unknown}'
                grids.append(grid)
            result[anim] = (spec['interval'], grids)
    finally:
        pet.wearing = None
    return pet, result


def build_accessories(name):
    pet = load_pet(name)
    return {acc: build_pet(name, acc)[1] for acc in pet.ACCESSORIES}


WRAPPER = '''<?xml version="1.0" encoding="utf-8"?>
<!-- Generated by tools/sprite_gen. Pixel art: no filtering, so scaling stays crisp. -->
<bitmap xmlns:android="http://schemas.android.com/apk/res/android"
    android:src="@drawable/{raw}"
    android:filter="false"
    android:antialias="false"
    android:dither="false"{extra} />
'''


def _write_wrapper(drawable, res, raw, tiled=False):
    # Tiled strips repeat sideways only; their transparent bottom row fills any extra height
    extra = '\n    android:tileModeX="repeat"\n    android:tileModeY="clamp"' if tiled else ''
    with open(os.path.join(drawable, f'{res}.xml'), 'w', encoding='utf-8', newline='\n') as f:
        f.write(WRAPPER.format(raw=raw, extra=extra))


def _java_array(java, const, interval, ids):
    if interval is not None:
        java.append(f'    static final int {const}_INTERVAL_MS = {interval};')
    java.append(f'    static final int[] {const}_FRAMES = {{')
    java.extend(f'        R.drawable.{res},' for res in ids)
    java[-1] = java[-1].rstrip(',')
    java.append('    };')
    java.append('')


def export_resources(all_pets, all_accessories):
    nodpi = os.path.join(RES, 'drawable-nodpi')
    drawable = os.path.join(RES, 'drawable')
    java = ['// Generated by tools/sprite_gen/sprite_gen.py - do not edit by hand.',
            'package com.example.pixelpetwidget;', '',
            '/** Frames and frame intervals of the generated pixel-art pets. */',
            'final class GeneratedPetFrames {', '',
            '    private GeneratedPetFrames() {}', '']
    cases = []
    tables = []                                  # (pet, animation, frames constant, interval constant)
    shift_tables = []                            # (pet/animation, shifts constant)
    for name, (pet, anims) in all_pets.items():
        for old in glob.glob(os.path.join(nodpi, f'raw_{name}_*.png')) + \
                glob.glob(os.path.join(drawable, f'{name}_*_frame_*.xml')) + \
                glob.glob(os.path.join(drawable, f'pixel_{name}_icon.xml')) + \
                glob.glob(os.path.join(drawable, f'pixel_{name}_acc_*.xml')):
            os.remove(old)
        # Identical frames are stored once per pet, even when two animations share them
        written = {}

        def store(grid, res_prefix, counter):
            key = hashlib.sha1('\n'.join(grid).encode()).hexdigest()
            if key not in written:
                counter[0] += 1
                res = f'{res_prefix}_frame_{counter[0]:03d}'
                save_png(to_image(grid, pet), os.path.join(nodpi, f'raw_{res}.png'))
                _write_wrapper(drawable, res, f'raw_{res}')
                written[key] = res
            return written[key]

        for anim, (interval, grids) in anims.items():
            counter = [0]
            # A moved frame is its pose stored once, drawn moved by the widget (PetSpec.Anim.shifts)
            ids = [store(g.moved[0] if getattr(g, 'moved', None) else g, f'{name}_{anim}', counter) for g in grids]
            const = f'{name.upper()}_{anim.upper()}'
            _java_array(java, const, interval, ids)
            tables.append((name, anim, f'{const}_FRAMES', f'{const}_INTERVAL_MS'))
            moves = [g.moved[1:] if getattr(g, 'moved', None) else (0, 0, 0) for g in grids]
            if any(any(m) for m in moves):
                java.append(f'    static final int[] {const}_MOVES = {{{", ".join(f"{dx}, {dy}, {f}" for dx, dy, f in moves)}}};')
                java.append('')
                shift_tables.append((f'{name}/{anim}', f'{const}_MOVES'))
        for acc, acc_anims in all_accessories.get(name, {}).items():
            for anim, (interval, grids) in acc_anims.items():
                counter = [0]
                plain = [g.moved[1:] if getattr(g, 'moved', None) else (0, 0, 0) for g in anims[anim][1]]
                worn = [g.moved[1:] if getattr(g, 'moved', None) else (0, 0, 0) for g in grids]
                assert plain == worn, f'{name} {anim} {acc}: moved differently than without it'
                ids = [store(g.moved[0] if getattr(g, 'moved', None) else g, f'{name}_{anim}_{acc}', counter)
                       for g in grids]
                const = f'{name.upper()}_{anim.upper()}_{acc.upper()}'
                _java_array(java, const, None, ids)
                cases.append((f'{name}/{anim}/{acc}', f'{const}_FRAMES'))
            # Wardrobe icon: the accessory on its own
            layers, size = pet.accessory_icon(acc)
            save_png(to_image(compose(layers, pet, size), pet), os.path.join(nodpi, f'raw_{name}_acc_{acc}.png'))
            _write_wrapper(drawable, f'pixel_{name}_acc_{acc}', f'raw_{name}_acc_{acc}')
        # Icon for the pet list: first idle frame cropped to the pet
        icon = _square_crop(to_image(anims['idle'][1][0], pet))
        save_png(icon, os.path.join(nodpi, f'raw_{name}_icon.png'))
        _write_wrapper(drawable, f'pixel_{name}_icon', f'raw_{name}_icon')

    def switch(doc, signature, key_expr, rows, default):
        java.append(f'    /** {doc} */')
        java.append(f'    static {signature} {{')
        java.append(f'        switch ({key_expr}) {{')
        java.extend(f'            case "{key}": return {value};' for key, value in rows)
        java.append(f'            default: return {default};')
        java.append('        }')
        java.append('    }')
        java.append('')

    switch('Frames of an animation, or null if the pet or the animation does not exist.',
           'int[] frames(String pet, String animation)', 'pet + "/" + animation',
           [(f'{n}/{a}', c) for n, a, c, _ in tables], 'null')
    switch('Time per frame of an animation in ms, or 0 if it does not exist.',
           'int interval(String pet, String animation)', 'pet + "/" + animation',
           [(f'{n}/{a}', i) for n, a, _, i in tables], '0')
    shifts = [(n, pet.WALK_SHIFTS) for n, (pet, _) in all_pets.items() if hasattr(pet, 'WALK_SHIFTS')]
    for n, values in shifts:
        java.append(f'    static final int[] {n.upper()}_WALK_SHIFTS = {{{", ".join(str(v) for v in values)}}};')
    if shifts:
        java.append('')
    switch('Art pixels the planted paws move back in each walk frame (the stride in the drawing), or null.',
           'int[] walkShifts(String pet)', 'pet', [(n, f'{n.upper()}_WALK_SHIFTS') for n, _ in shifts], 'null')
    switch('Where the widget moves each frame of an animation, in canvas pixels, and whether it mirrors it: '
           'dx, dy, flip for every frame (a pose stored once and drawn at several places), or null when it '
           'moves none. Worn accessories move alike.', 'int[] shifts(String pet, String animation)',
           'pet + "/" + animation', shift_tables, 'null')
    switch('Picture of the pet for lists in the app, or 0.',
           'int icon(String pet)', 'pet', [(n, f'R.drawable.pixel_{n}_icon') for n in all_pets], '0')
    switch('Picture of an accessory for the wardrobe, or 0.',
           'int accessoryIcon(String pet, String accessory)', 'pet + "/" + accessory',
           [(f'{n}/{acc}', f'R.drawable.pixel_{n}_acc_{acc}')
            for n in all_pets for acc in all_accessories.get(n, {})], '0')
    java.append('    /** Frames of an animation with the pet wearing an accessory, or null if there are none. */')
    java.append('    static int[] withAccessory(String pet, String animation, String accessory) {')
    java.append('        switch (pet + "/" + animation + "/" + accessory) {')
    java.extend(f'            case "{key}": return {const};' for key, const in cases)
    java.append('            default: return null;')
    java.append('        }')
    java.append('    }')
    java.append('}')
    java.append('')
    with open(os.path.join(JAVA_DIR, 'GeneratedPetFrames.java'), 'w', encoding='utf-8', newline='\n') as f:
        f.write('\n'.join(java))


def export_props():
    """Need bubbles and seasonal decorations used by the widget (tools/sprite_gen/props.py)."""
    props = load_pet('props')
    nodpi = os.path.join(RES, 'drawable-nodpi')
    drawable = os.path.join(RES, 'drawable')
    for old in glob.glob(os.path.join(nodpi, 'raw_prop_*.png')) + glob.glob(os.path.join(drawable, 'prop_*.xml')):
        os.remove(old)
    images = {}
    for name, (builder, size, scale, tiled) in props.PROPS.items():
        img = to_image(compose(builder(), props, size), props)
        images[name] = img
        if scale > 1:
            # Tiled strips repeat at their bitmap size, so they are enlarged here
            img = img.resize((img.width * scale, img.height * scale), Image.NEAREST)
        img.save(os.path.join(nodpi, f'raw_prop_{name}.png'), optimize=True)
        _write_wrapper(drawable, f'prop_{name}', f'raw_prop_{name}', tiled)
    # Preview of every prop at x6 on the light and the dark background
    scale = 6
    cell_w = max(i.width for i in images.values()) * scale + 12
    cell_h = max(i.height for i in images.values()) * scale + 12
    sheet = Image.new('RGB', (cell_w * len(images), cell_h * 2), LIGHT[:3])
    for c, img in enumerate(images.values()):
        big = img.resize((img.width * scale, img.height * scale), Image.NEAREST)
        for r, bg in enumerate((LIGHT, DARK)):
            tile = Image.new('RGBA', big.size, bg)
            tile.alpha_composite(big)
            sheet.paste(bg[:3], (c * cell_w, r * cell_h, (c + 1) * cell_w, (r + 1) * cell_h))
            sheet.paste(tile.convert('RGB'), (c * cell_w + 6, r * cell_h + 6))
    sheet.save(os.path.join(OUT, 'props_sheet.png'))
    return props


def _square_crop(img, margin=1):
    left, top, right, bottom = img.getbbox()
    side = max(right - left, bottom - top) + 2 * margin
    out = Image.new('RGBA', (side, side), (0, 0, 0, 0))
    out.paste(img.crop((left, top, right, bottom)),
              ((side - (right - left)) // 2, (side - (bottom - top)) // 2))
    return out


# Group photo for the widget picker: (pet, centre x, ground y); back row first
PREVIEW_GROUP = (('panda', 196, 298), ('capybara', 396, 296), ('parrot', 466, 342), ('hamster', 124, 352),
                 ('kitten', 266, 358), ('puppy', 356, 352))


def export_widget_preview(all_pets):
    """Widget picker preview: the pixel pets on the default background.

    3:2 with the group inside the middle square and above the bottom edge: launchers crop the
    picture to whatever shape they show (wide in the pin dialog, square in the picker).
    """
    width, height = 600, 400
    bg = Image.open(os.path.join(RES, 'drawable-nodpi', 'widget_background.webp')).convert('RGBA')
    crop_w = min(bg.width, bg.height * width // height)
    left = (bg.width - crop_w) // 2
    preview = bg.crop((left, 0, left + crop_w, crop_w * height // width)).resize((width, height), Image.LANCZOS)
    scale = 2
    for name, centre_x, ground_y in PREVIEW_GROUP:
        pet, anims = all_pets[name]
        sprite = to_image(anims['idle'][1][0], pet)
        sprite = sprite.crop(sprite.getbbox())
        sprite = sprite.resize((sprite.width * scale, sprite.height * scale), Image.NEAREST)
        preview.alpha_composite(sprite, (centre_x - sprite.width // 2, ground_y - sprite.height))
    preview.save(os.path.join(RES, 'drawable-nodpi', 'widget_preview.webp'), lossless=True, method=6)
    preview.save(os.path.join(OUT, 'widget_preview.png'))


# --- previews ----------------------------------------------------------------------------

LIGHT = (236, 233, 226, 255)
DARK = (40, 42, 54, 255)


def _on(grid, pet, bg, scale):
    """One frame enlarged with nearest-neighbour on a solid background (RGB, small)."""
    w, h = len(grid[0]), len(grid)
    cell = Image.new('RGBA', (w, h), bg)
    cell.alpha_composite(to_image(grid, pet))
    return cell.convert('RGB').resize((w * scale, h * scale), Image.NEAREST)


def _sheet(all_pets, scale, max_cols=None):
    """All animations of all pets, one row per animation on the light and the dark background.
    max_cols wraps long animations onto further rows."""
    rows = []
    for pet, anims in all_pets.values():
        for _, grids in anims.values():
            step = max_cols or len(grids)
            rows += [(pet, grids[i:i + step]) for i in range(0, len(grids), step)]
    cell_w = max(len(g[0][0]) for _, g in rows) * scale
    cell_h = max(len(g[0]) for _, g in rows) * scale
    cols = max(len(grids) for _, grids in rows)
    sheet = Image.new('RGB', (cols * cell_w, len(rows) * cell_h * 2), LIGHT[:3])
    for r, (pet, grids) in enumerate(rows):
        for bg_index, bg in enumerate((LIGHT, DARK)):
            y0 = (r * 2 + bg_index) * cell_h
            sheet.paste(bg[:3], (0, y0, cols * cell_w, y0 + cell_h))
            for c, grid in enumerate(grids):
                sheet.paste(_on(grid, pet, bg, scale), (c * cell_w, y0))
    return sheet


def _gif(path, grids, pet, interval):
    frames = [_on(g, pet, LIGHT, PREVIEW_SCALE).convert('P', palette=Image.ADAPTIVE, colors=32) for g in grids]
    frames[0].save(path, save_all=True, append_images=frames[1:], duration=interval, loop=0, disposal=2)


def export_previews(all_pets, all_accessories=None):
    os.makedirs(OUT, exist_ok=True)
    for name in all_pets:
        for old in glob.glob(os.path.join(OUT, f'{name}_*.gif')):
            if not old.endswith('_life.gif'):               # life_gif.py's, from the app's engine
                os.remove(old)
    for name, entry in all_pets.items():
        pet, anims = entry
        # x8 sheet per pet; built in RGB from small cells to keep memory use modest
        # Wide scenes wrap after 8 frames so the sheet stays a sensible size
        wide = canvas(pet)[0] > SIZE
        _sheet({name: entry}, PREVIEW_SCALE, 8 if wide else None).save(
            os.path.join(OUT, f'preview_sheet_{name}.png'))
        for anim, (interval, grids) in anims.items():
            _gif(os.path.join(OUT, f'{name}_{anim}.gif'), grids, pet, interval)
        accessories = (all_accessories or {}).get(name)
        if accessories:
            # Idle and happy with every accessory, x4
            rows = {f'{acc}_{anim}': acc_anims[anim] for acc, acc_anims in accessories.items()
                    for anim in ('idle', 'happy', 'walk') if anim in acc_anims}
            _sheet({name: (pet, rows)}, PREVIEW_SCALE // 2).save(
                os.path.join(OUT, f'preview_sheet_{name}_accessories.png'))
            for acc, acc_anims in accessories.items():
                interval, grids = acc_anims['idle']
                _gif(os.path.join(OUT, f'{name}_idle_{acc}.gif'), grids, pet, interval)
    # Overview of every pet at x4 (only when every pet was built)
    if set(all_pets) >= set(PETS):
        _sheet(all_pets, PREVIEW_SCALE // 2, 16).save(os.path.join(OUT, 'preview_sheet.png'))


def dump(name, anim, index, accessory=None):
    pet, anims = build_pet(name, accessory)
    os.makedirs(OVERRIDES, exist_ok=True)
    suffix = f'_{accessory}' if accessory else ''
    path = os.path.join(OVERRIDES, f'{name}_{anim}{suffix}_{int(index):03d}.txt')
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write('\n'.join(anims[anim][1][int(index)]) + '\n')
    print('wrote', path)


def main(argv):
    if argv[:1] == ['dump']:
        dump(*argv[1:5])
        return
    names = argv[1:] if argv[:1] == ['preview'] and len(argv) > 1 else PETS
    all_pets = {name: build_pet(name) for name in names}
    all_accessories = {name: build_accessories(name) for name in names}
    export_previews(all_pets, all_accessories)
    if argv[:1] != ['preview']:
        export_resources(all_pets, all_accessories)
        export_widget_preview(all_pets)
        export_props()
    for name, (pet, anims) in all_pets.items():
        summary = ', '.join(f'{a} {len(g)}x{i}ms' for a, (i, g) in anims.items())
        print(f'{name}: {len(pet.PALETTE)} colours; {summary}; accessories: {", ".join(pet.ACCESSORIES)}')


if __name__ == '__main__':
    main(sys.argv[1:])
