"""Pixel-art backgrounds for the widget (the autumn scenes).

A background module (bg_maple.py, bg_rainy.py, bg_pumpkin.py) draws:
  SCENE     the still picture on a canvas of CANVAS pixels, a grid of palette keys
  ELEMENTS  small animated decorations at fixed places of the picture (a falling leaf, the lamp's
            flicker): a sprite and a position per frame
  TILES     animated layers that tile the whole widget (the rain)
  PROPS     the things pets play with (a heap of leaves, puddles, pumpkins): sprites that stand on
            the ground line at a ground position (the pets' -1..1 of their walking range), so a pet
            can walk up to them on any widget size; each has named states (still, rustling, ...)
  EVENT_SPRITES  sprites the events draw (a falling leaf, a raindrop)

Nothing is drawn as whole frames of the scene: the picture is one image, the rest are tiny sprites.

Rules, as for the pets: alpha only 0 or 255, at most MAX_COLOURS colours per background, one
1 px outline colour (#1f2133) around near objects, three tones per form with the light from the
top left, no stray pixels. The widget scales everything by whole numbers, at the pet's pixel
scale, with the ground line where the pet stands.

    python tools/sprite_gen/scenery.py            pictures, sprites, GeneratedBackgrounds.java, previews
    python tools/sprite_gen/scenery.py check      the checks only
"""
import importlib
import math
import os
import random
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, 'design'))
import sprite_gen as sg  # noqa: E402
from shapes import clean, shade  # noqa: E402

BACKGROUNDS = ('maple', 'rainy', 'pumpkin')
MAX_COLOURS = 24
LIGHT = (236, 233, 226, 255)
DARK = (40, 42, 54, 255)
# In tiny sprites the darkest tones work as lines (a leaf's stalk, the shaded root of a blade)
SPRITE_LINES = ('K', 'r', 'd', 'v', 'j', 'q', 'h', 'm', 'n', 'k', 'x', 'z', 'l')


# --- painting on a grid of palette keys ---------------------------------------------------

class Canvas:
    """A grid of palette keys ('.' is empty) with the few tools a scene needs."""

    def __init__(self, width, height, fill='.'):
        self.width, self.height = width, height
        self.rows = [[fill] * width for _ in range(height)]

    def inside(self, x, y):
        return 0 <= x < self.width and 0 <= y < self.height

    def get(self, x, y):
        return self.rows[y][x] if self.inside(x, y) else '.'

    def put(self, x, y, key):
        if self.inside(x, y) and key != '.':
            self.rows[y][x] = key

    def rect(self, x0, y0, x1, y1, key):
        for y in range(max(0, y0), min(self.height, y1)):
            for x in range(max(0, x0), min(self.width, x1)):
                self.rows[y][x] = key

    def stamp(self, rows, x, y, only_over=None):
        """A text matrix at (x, y); '.' leaves the canvas as it is. only_over: keys it may paint on."""
        for dy, row in enumerate(rows):
            for dx, key in enumerate(row):
                if key == '.':
                    continue
                if only_over is not None and self.get(x + dx, y + dy) not in only_over:
                    continue
                self.put(x + dx, y + dy, key)

    def points(self, pts, key):
        for x, y in pts:
            self.put(x, y, key)

    def outline(self, pts, key='K', over=None):
        """A 1 px ring around a set of points, on the pixels next to it (4 neighbours)."""
        ring = set()
        for x, y in pts:
            for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
                if (nx, ny) not in pts and self.inside(nx, ny):
                    ring.add((nx, ny))
        for x, y in ring:
            if over is None or self.get(x, y) in over:
                self.put(x, y, key)
        return ring

    def grid(self):
        return [''.join(r) for r in self.rows]


def ellipse_points(cx, cy, rx, ry, power=2.0):
    pts = set()
    for y in range(int(cy - ry) - 1, int(cy + ry) + 2):
        for x in range(int(cx - rx) - 1, int(cx + rx) + 2):
            if abs((x + 0.5 - cx) / rx) ** power + abs((y + 0.5 - cy) / ry) ** power <= 1:
                pts.add((x, y))
    return pts


def shaded(pts, light, base, dark, shadow=((1, 2), (2, 1)), light_reach=2):
    """Three tones for a shape given as points: light rim at the top left, shadow at the bottom right."""
    xs = [p[0] for p in pts]
    ys = [p[1] for p in pts]
    x0, y0 = min(xs), min(ys)
    w, h = max(xs) - x0 + 1, max(ys) - y0 + 1
    mask = [[(x + x0, y + y0) in pts for x in range(w)] for y in range(h)]
    rows = shade(mask, base, light, dark, shadow=shadow, light_reach=light_reach)
    return {(x + x0, y + y0): rows[y][x] for y in range(h) for x in range(w) if rows[y][x] != '.'}


def smooth_noise(rng, length, scale, octaves=3):
    """A smooth bumpy line: a sum of a few sine waves with random phases."""
    waves = [(rng.uniform(0, math.tau), rng.uniform(0.6, 1.4) * (2 ** o), 1 / (2 ** o)) for o in range(octaves)]
    return [sum(a * math.sin(phase + x * freq / scale) for phase, freq, a in waves) for x in range(length)]


def tidy(c, keep=('K',)):
    """Pixels whose colour no neighbour shares take their neighbours' colour; the outline stays."""
    rows = [list(r) for r in c.grid()]
    cleaned = clean([['.' if k in keep else k for k in r] for r in rows])
    for y, row in enumerate(rows):
        for x, k in enumerate(row):
            if k not in keep and k != '.':
                c.rows[y][x] = cleaned[y][x]


def tidy_rows(rows, keep=('K',)):
    """tidy() for a sprite given as rows; returns new rows."""
    c = Canvas(len(rows[0]), len(rows))
    c.rows = [list(r) for r in rows]
    tidy(c, keep)
    return c.grid()


# --- trees (shared by the scenes) ---------------------------------------------------------

def leafy_clump(cx, cy, r, local):
    """A clump of leaves: round, with small bumps all along its rim (the leaves' tips)."""
    pts = ellipse_points(cx, cy, r, r * 0.82)
    bumps = max(6, int(r * 0.9))
    for k in range(bumps):
        a = math.tau * (k + local.uniform(-0.3, 0.3)) / bumps
        br = local.uniform(2.6, 4.2)
        bx = cx + (r - 1.5) * math.cos(a)
        by = cy + (r - 1.5) * 0.82 * math.sin(a)
        pts |= ellipse_points(bx, by, br, br)
    return pts


def dome(cx, cy, rx, ry, spacing, seed):
    """Clumps of leaves filling a dome: centres on a jittered grid inside an ellipse, smaller
    towards the rim, so the crown is one round mass."""
    local = random.Random(seed)
    out = []
    y = cy - ry + spacing // 2
    row = 0
    while y < cy + ry * 0.7:
        x = cx - rx + (spacing // 2 if row % 2 else 0)
        while x < cx + rx:
            d = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2
            if d < 0.92:
                r = int(spacing * (1.05 - 0.35 * d)) + local.randint(-2, 2)
                out.append((x + local.randint(-3, 3), y + local.randint(-3, 3), r))
            x += spacing
        y += int(spacing * 0.8)
        row += 1
    return out


def crown(c, clumps, tone_sets, seed, outline_over):
    """A crown of leaves: clumps with bumpy rims over a dark core. The whole crown is lit from the
    top left: clumps there take the light tone set, those at the bottom right the shaded one; each
    clump still has its own three tones. Outlined where it meets the keys in outline_over.
    clumps: (x, y, radius); tone_sets: (lit, middle, shaded), each (light, base, shade)."""
    local = random.Random(seed)
    shapes = [leafy_clump(cx, cy, r, local) for cx, cy, r in clumps]
    pts = set().union(*shapes)
    xs = [x for x, _, _ in clumps]
    ys = [y for _, y, _ in clumps]
    mx, my = (min(xs) + max(xs)) / 2, (min(ys) + max(ys)) / 2
    sx, sy = (max(xs) - min(xs)) / 2 or 1, (max(ys) - min(ys)) / 2 or 1
    for (cx, cy, r) in clumps:
        for x, y in ellipse_points(cx + 2, cy + 3, r + 1, (r + 1) * 0.82):
            pts.add((x, y))
    for x, y in pts:
        c.put(x, y, tone_sets[2][2])
    for (cx, cy, r), clump in sorted(zip(clumps, shapes), key=lambda k: k[0][1]):
        side = (cx - mx) / sx * 0.6 + (cy - my) / sy
        tones = tone_sets[0] if side < -0.45 else tone_sets[2] if side > 0.55 else tone_sets[1]
        light, base, dark = tones
        for (x, y), t in shaded(clump, light, base, dark, shadow=((1, 2), (2, 2)), light_reach=2).items():
            c.put(x, y, t)
    c.outline(pts, 'K', over=outline_over)
    return pts


def trunk(c, x0, top, bottom, width_bottom, width_top, lean, tones=('w', 'W', 'v')):
    """A trunk widening into roots, lit on the left, with grooves in the bark and a knot."""
    light, base, dark = tones
    pts = set()
    centre = {}
    for y in range(top, bottom):
        t = (y - top) / (bottom - top)
        w = width_top + (width_bottom - width_top) * t ** 1.6
        cx = x0 + lean * (1 - t)
        centre[y] = (cx, w)
        if y > bottom - 9:
            w += (y - (bottom - 9)) * 1.6     # roots
        for x in range(int(cx - w / 2), int(cx + w / 2) + 1):
            pts.add((x, y))
    for p, t in shaded(pts, light, base, dark, shadow=((2, 0), (3, 0))).items():
        c.put(p[0], p[1], t)
    local = random.Random(int(x0))
    y = top + 3
    while y < bottom - 10:
        cx, w = centre[y]
        gx = int(cx + local.uniform(-w / 2 + 3, w / 2 - 3))
        for k in range(local.randint(4, 9)):
            if c.get(gx, y + k) in (light, base):
                c.put(gx, y + k, dark)
                if local.random() < 0.5 and c.get(gx - 1, y + k) == base:
                    c.put(gx - 1, y + k, light)
        y += local.randint(3, 6)
    kx, _ = centre[(top + bottom) // 2]
    c.stamp([f'.{dark}{dark}.', f'{dark}{base}{light}{dark}', f'.{dark}{dark}.'], int(kx) - 2, (top + bottom) // 2)
    c.outline(pts, 'K')
    return pts


def falling_path(start, end, frames, sway, turns, sprites=4):
    """A leaf's way down: a swaying fall from start to end, with a sprite for each step (0: flat,
    1 and 3: tilted, 2: edge on)."""
    (x0, y0), (x1, y1) = start, end
    path = []
    for i in range(frames):
        t = i / (frames - 1)
        x = x0 + (x1 - x0) * t + sway * math.sin(t * turns * math.tau)
        y = y0 + (y1 - y0) * (0.25 * t + 0.75 * t * t * (3 - 2 * t))
        swing = math.cos(t * turns * math.tau)
        frame = 0 if abs(swing) > 0.7 else (1 if swing > 0 else 3) if abs(swing) > 0.25 else 2
        path.append((int(round(x)), int(round(y)), frame % sprites))
    return path


# --- checks -------------------------------------------------------------------------------

def orphans(grid, allowed=('K',)):
    """Pixels whose colour none of their 8 neighbours shares (outline aside)."""
    h, w = len(grid), len(grid[0])
    found = []
    for y in range(h):
        for x in range(w):
            c = grid[y][x]
            if c == '.' or c in allowed:
                continue
            near = [grid[y + dy][x + dx] for dx in (-1, 0, 1) for dy in (-1, 0, 1)
                    if (dx or dy) and 0 <= x + dx < w and 0 <= y + dy < h]
            if c not in near:
                found.append((x, y))
    return found


def check(bg):
    problems = []
    grid = bg.SCENE

    def colours(rows, what):
        unknown = {c for r in rows for c in r} - set(bg.PALETTE) - {'.'}
        if unknown:
            problems.append(f'{what}: unknown colours {sorted(unknown)}')

    colours(grid, 'scene')
    if len(bg.PALETTE) > MAX_COLOURS:
        problems.append(f'{len(bg.PALETTE)} colours > {MAX_COLOURS}')
    if any(c == '.' for row in grid for c in row):
        problems.append('holes in the picture')
    if len(grid) != bg.CANVAS[1] or any(len(r) != bg.CANVAS[0] for r in grid):
        problems.append('wrong canvas size')
    stray = orphans(grid)
    if len(stray) > bg.ORPHANS_OK:
        problems.append(f'{len(stray)} stray pixels, first at {stray[:6]}')
    for name, element in bg.ELEMENTS.items():
        for i, sprite in enumerate(element['frames']):
            colours(sprite, f'{name} {i}')
            loose = orphans(sprite, SPRITE_LINES)
            if len(loose) > element.get('orphans_ok', 0):
                problems.append(f'{name} frame {i}: stray pixels {loose[:4]}')
        path = element['path']
        if len(path) != element['length']:
            problems.append(f'{name}: {len(path)} positions for {element["length"]} frames')
        for x, y, f in path:
            w, h = len(element['frames'][f][0]), len(element['frames'][f])
            if x < 0 or y < 0 or x + w > bg.CANVAS[0] or y + h > bg.CANVAS[1]:
                problems.append(f'{name}: off the canvas at {(x, y)}')
                break
        # A loop that stays in place must close up; a falling leaf may start again from its branch
        if not element.get('respawns'):
            (x0, y0, _), (x1, y1, _) = path[0], path[-1]
            if abs(x1 - x0) > 2 or abs(y1 - y0) > 2:
                problems.append(f'{name}: the loop jumps from {(x1, y1)} to {(x0, y0)}')
    for name, tile in getattr(bg, 'TILES', {}).items():
        sizes = {(len(f[0]), len(f)) for f in tile['frames']}
        if len(sizes) != 1:
            problems.append(f'tile {name}: frames of different sizes')
        for i, frame in enumerate(tile['frames']):
            colours(frame, f'tile {name} {i}')
    for name, prop in bg.PROPS.items():
        sizes = set()
        for state, (interval, frames) in prop['states'].items():
            for i, sprite in enumerate(frames):
                colours(sprite, f'{name} {state} {i}')
                sizes.add((len(sprite[0]), len(sprite)))
                loose = orphans(sprite, SPRITE_LINES)
                if len(loose) > prop.get('orphans_ok', 0):
                    problems.append(f'prop {name} {state} {i}: stray pixels {loose[:4]}')
        if len(sizes) != 1:
            problems.append(f'prop {name}: frames of different sizes {sizes}')
        if prop['ambient'] not in prop['states']:
            problems.append(f'prop {name}: no ambient state')
        if not -0.9 <= prop['offset'] <= 0.9:
            problems.append(f'prop {name}: off the ground')
    for name, sprite in bg.EVENT_SPRITES.items():
        colours(sprite, name)
        if orphans(sprite, SPRITE_LINES):
            problems.append(f'event sprite {name}: stray pixels {orphans(sprite, SPRITE_LINES)[:4]}')
    return problems


# --- export -------------------------------------------------------------------------------

def load(name):
    return importlib.import_module(f'bg_{name}')


def image(grid, palette):
    img = Image.new('RGBA', (len(grid[0]), len(grid)), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(grid):
        for x, c in enumerate(row):
            if c != '.':
                px[x, y] = sg._rgb(palette[c]) + (255,)
    return img


def prop_position(bg, prop, sprite):
    """Where a prop stands in the previews (a 4x3 widget): its ground position from the middle."""
    x = int(bg.CANVAS[0] / 2 + prop['offset'] * bg.PREVIEW_AMPLITUDE - len(sprite[0]) / 2)
    return x, bg.GROUND - prop['baseline']


TILED_WRAPPER = '''<?xml version="1.0" encoding="utf-8"?>
<!-- Generated by tools/sprite_gen/scenery.py. Pixel art tiled over the whole widget, no filtering. -->
<bitmap xmlns:android="http://schemas.android.com/apk/res/android"
    android:src="@drawable/{raw}"
    android:filter="false"
    android:antialias="false"
    android:dither="false"
    android:tileMode="repeat" />
'''


def _ints(values):
    return ', '.join(str(v) for v in values)


def export(backgrounds):
    nodpi = os.path.join(sg.RES, 'drawable-nodpi')
    drawable = os.path.join(sg.RES, 'drawable')
    java = ['// Generated by tools/sprite_gen/scenery.py - do not edit by hand.',
            'package com.example.pixelpetwidget;', '',
            '/** The pixel-art backgrounds: pictures, ground lines, animated elements, props and event sprites. */',
            'final class GeneratedBackgrounds {', '',
            '    private GeneratedBackgrounds() {}', '']
    for name, bg in backgrounds.items():
        for old in [p for p in os.listdir(nodpi) if p.startswith(f'raw_bg_{name}')]:
            os.remove(os.path.join(nodpi, old))
        for old in [p for p in os.listdir(drawable) if p.startswith(f'bg_{name}')]:
            os.remove(os.path.join(drawable, old))
        const = name.upper()
        sg.save_png(image(bg.SCENE, bg.PALETTE), os.path.join(nodpi, f'raw_bg_{name}.png'))
        sg._write_wrapper(drawable, f'bg_{name}', f'raw_bg_{name}')
        # The app's preview (the whole picture) and card (its middle) show the props in their usual places
        sg.save_png(scene_with(bg, 0), os.path.join(nodpi, f'raw_bg_{name}_preview.png'))
        sg._write_wrapper(drawable, f'bg_{name}_preview', f'raw_bg_{name}_preview')
        x0, y0, x1, y1 = bg.THUMB
        thumb = scene_with(bg, 0).crop((x0, y0, x1, y1))
        sg.save_png(thumb, os.path.join(nodpi, f'raw_bg_{name}_thumb.png'))
        sg._write_wrapper(drawable, f'bg_{name}_thumb', f'raw_bg_{name}_thumb')
        w, h = bg.CANVAS
        java += [f'    static final int {const}_IMAGE = R.drawable.bg_{name};',
                 f'    static final int {const}_PREVIEW = R.drawable.bg_{name}_preview;',
                 f'    static final int {const}_WIDTH = {w};',
                 f'    static final int {const}_HEIGHT = {h};',
                 '    /** Row the pets stand on, and rows of ground the widget shows under their feet. */',
                 f'    static final int {const}_GROUND = {bg.GROUND};',
                 f'    static final int {const}_GROUND_MARGIN = {bg.GROUND_MARGIN};',
                 '    /** Colours that continue the picture above and below it on a widget bigger than the canvas. */',
                 f'    static final int {const}_SKY = 0xff{bg.PALETTE[bg.SKY_KEY].lstrip("#")};',
                 f'    static final int {const}_EARTH = 0xff{bg.PALETTE[bg.EARTH_KEY].lstrip("#")};', '']
        edge = getattr(bg, 'CANOPY_EDGE', [-1] * w)
        java += ['    /** Lowest pixel of the canopy above each column, -1 where there is none. */',
                 f'    static final short[] {const}_CANOPY = {{{_ints(edge)}}};', '']
        written = {}

        def store(sprite, res):
            key = '\n'.join(sprite)
            if key not in written:
                sg.save_png(image(sprite, bg.PALETTE), os.path.join(nodpi, f'raw_{res}.png'))
                sg._write_wrapper(drawable, res, f'raw_{res}')
                written[key] = res
            return written[key]

        java.append(f'    /** Animated elements of the {name} background: frames, interval, size and per frame x, y. */')
        names = []
        for ename, element in bg.ELEMENTS.items():
            ids = [store(s, f'bg_{name}_{ename}_{i + 1:02d}') for i, s in enumerate(element['frames'])]
            econst = f'{const}_{ename.upper()}'
            path = element['path']
            java.append(f'    static final int[] {econst}_FRAMES = {{{", ".join("R.drawable." + ids[f] for _, _, f in path)}}};')
            java.append(f'    static final int[] {econst}_X = {{{_ints(x for x, _, _ in path)}}};')
            java.append(f'    static final int[] {econst}_Y = {{{_ints(y for _, y, _ in path)}}};')
            java.append(f'    static final int {econst}_INTERVAL_MS = {element["interval"]};')
            java.append(f'    static final int {econst}_WIDTH = {len(element["frames"][0][0])};')
            java.append(f'    static final int {econst}_HEIGHT = {len(element["frames"][0])};')
            names.append(econst)
        java.append(f'    static final Backdrop.Element[] {const}_ELEMENTS = {{')
        java += [f'        new Backdrop.Element({e}_FRAMES, {e}_X, {e}_Y, {e}_INTERVAL_MS, {e}_WIDTH, {e}_HEIGHT),'
                 for e in names]
        java.append('    };')
        java.append('')
        java.append('    /** Layers tiled over the whole widget (the rain): frames and interval. */')
        tiles = []
        for tname, tile in getattr(bg, 'TILES', {}).items():
            ids = []
            for i, frame in enumerate(tile['frames']):
                res = f'bg_{name}_{tname}_{i + 1:02d}'
                sg.save_png(image(frame, bg.PALETTE), os.path.join(nodpi, f'raw_{res}.png'))
                with open(os.path.join(drawable, f'{res}.xml'), 'w', encoding='utf-8', newline='\n') as f:
                    f.write(TILED_WRAPPER.format(raw=f'raw_{res}'))
                ids.append(f'R.drawable.{res}')
            tiles.append(f'        new Backdrop.Tile(new int[] {{{", ".join(ids)}}}, {tile["interval"]}),')
        java.append(f'    static final Backdrop.Tile[] {const}_TILES = {{')
        java += tiles
        java.append('    };')
        java.append('')
        java.append('    /** Props pets play with: name, ground position, in front of the pet, size, row on the')
        java.append('     * ground line, perch height, ambient state, states. */')
        java.append(f'    static final Backdrop.Prop[] {const}_PROPS = {{')
        for pname, prop in bg.PROPS.items():
            states = []
            for sname, (interval, frames) in prop['states'].items():
                ids = [store(s, f'bg_{name}_{pname}_{sname}_{i + 1:02d}') for i, s in enumerate(frames)]
                states.append(f'new Backdrop.State("{sname}", new int[] {{{", ".join("R.drawable." + r for r in ids)}}}, '
                              f'{interval})')
            first = prop['states'][prop['ambient']][1][0]
            java.append(f'        new Backdrop.Prop("{pname}", {prop["offset"]}f, {str(prop["layer"] == "front").lower()}, '
                        f'{len(first[0])}, {len(first)}, {prop["baseline"]}, {prop.get("perch", 0)}, '
                        f'"{prop["ambient"]}",')
            java.append('            ' + ',\n            '.join(states) + '),')
        java.append('    };')
        java.append('')
        java.append(f'    /** Sprites the events of the {name} background use, and their sizes. */')
        for sname, sprite in bg.EVENT_SPRITES.items():
            res = store(sprite, f'bg_{name}_{sname}')
            sconst = f'{const}_{sname.upper()}'
            java.append(f'    static final int {sconst} = R.drawable.{res};')
            java.append(f'    static final int {sconst}_WIDTH = {len(sprite[0])};')
            java.append(f'    static final int {sconst}_HEIGHT = {len(sprite)};')
        java.append('')
    java.append('}')
    java.append('')
    with open(os.path.join(sg.JAVA_DIR, 'GeneratedBackgrounds.java'), 'w', encoding='utf-8', newline='\n') as f:
        f.write('\n'.join(java))


# --- previews -----------------------------------------------------------------------------

def scene_with(bg, t, extra=(), states=None, tick=None):
    """The picture with its elements, props (ambient state unless states names another) and
    tiles at time step t (in units of tick ms, or frames when tick is None), and extra
    (sprite, x, y) on top of the back props."""
    img = image(bg.SCENE, bg.PALETTE)

    def frame_of(interval, count):
        return (t * tick // interval) % count if tick else t % count

    for element in bg.ELEMENTS.values():
        x, y, f = element['path'][frame_of(element['interval'], element['length'])]
        img.alpha_composite(image(element['frames'][f], bg.PALETTE), (x, y))
    front = []
    for pname, prop in bg.PROPS.items():
        interval, frames = prop['states'][(states or {}).get(pname, prop['ambient'])]
        sprite = frames[frame_of(interval, len(frames))]
        placed = (image(sprite, bg.PALETTE), prop_position(bg, prop, sprite))
        if prop['layer'] == 'front':
            front.append(placed)
        else:
            img.alpha_composite(placed[0], placed[1])
    for sprite, x, y in extra:
        img.alpha_composite(image(sprite, bg.PALETTE) if isinstance(sprite, list) else sprite, (x, y))
    for sprite, at in front:
        img.alpha_composite(sprite, at)
    for tile in getattr(bg, 'TILES', {}).values():
        frame = image(tile['frames'][frame_of(tile['interval'], len(tile['frames']))], bg.PALETTE)
        for ty in range(0, img.height, frame.height):
            for tx in range(0, img.width, frame.width):
                img.alpha_composite(frame, (tx, ty))
    return img


def previews(name, bg):
    out = sg.OUT
    w, h = bg.CANVAS
    scene_with(bg, 0).resize((w * 2, h * 2), Image.NEAREST).save(os.path.join(out, f'bg_{name}_x2.png'))
    # Elements, props, tiles and event sprites at x4 on the light and the dark background
    sprites = [s for e in bg.ELEMENTS.values() for s in e['frames']]
    for prop in bg.PROPS.values():
        for _, frames in prop['states'].values():
            sprites += frames
    sprites += list(bg.EVENT_SPRITES.values())
    sprites += [t['frames'][0] for t in getattr(bg, 'TILES', {}).values()]
    scale = 4
    cells = [image(s, bg.PALETTE).resize((len(s[0]) * scale, len(s) * scale), Image.NEAREST) for s in sprites]
    cols = 10
    cw = max(c.width for c in cells) + 8
    ch = max(c.height for c in cells) + 8
    rows = (len(cells) + cols - 1) // cols
    sheet = Image.new('RGB', (cw * cols, ch * rows * 2), LIGHT[:3])
    for i, cell in enumerate(cells):
        for r, back in enumerate((LIGHT, DARK)):
            tile = Image.new('RGBA', (cw, ch), back)
            tile.alpha_composite(cell, (4, 4))
            sheet.paste(tile.convert('RGB'), ((i % cols) * cw, ((i // cols) * 2 + r) * ch))
    sheet.save(os.path.join(out, f'bg_{name}_sprites_x4.png'))
    # The animated parts in a GIF (x2, the whole scene)
    intervals = [e['interval'] for e in bg.ELEMENTS.values()] + [t['interval'] for t in getattr(bg, 'TILES', {}).values()]
    intervals += [p['states'][p['ambient']][0] for p in bg.PROPS.values()]
    tick = min(intervals)
    loops = [e['length'] * e['interval'] for e in bg.ELEMENTS.values()]
    loops += [len(t['frames']) * t['interval'] for t in getattr(bg, 'TILES', {}).values()]
    frames = [scene_with(bg, i, tick=tick).resize((w * 2, h * 2), Image.NEAREST).convert('RGB')
              .quantize(colors=64, method=Image.Quantize.MAXCOVERAGE, dither=Image.Dither.NONE)
              for i in range(max(loops) // tick)]
    frames[0].save(os.path.join(out, f'bg_{name}_elements.gif'), save_all=True, append_images=frames[1:],
                   duration=tick, loop=0, optimize=True)


def main(argv):
    names = [a for a in argv if a != 'check'] or list(BACKGROUNDS)
    backgrounds = {}
    failed = {}
    for name in names:
        bg = load(name)
        backgrounds[name] = bg
        problems = check(bg)
        used = len({c for row in bg.SCENE for c in row})
        print(f'{name}: {len(bg.PALETTE)} colours ({used} used), {len(orphans(bg.SCENE))} stray pixels'
              + ''.join(f'\n  !! {p}' for p in problems))
        if problems:
            failed[name] = problems
    if failed:
        raise SystemExit('Checks failed')
    if 'check' in argv:
        return
    if set(names) != set(BACKGROUNDS):
        print('(the export needs every background: previews only)')
    else:
        export(backgrounds)
    for name, bg in backgrounds.items():
        previews(name, bg)
    print('ok')


if __name__ == '__main__':
    main(sys.argv[1:])
