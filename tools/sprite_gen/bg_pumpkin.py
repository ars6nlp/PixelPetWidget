"""Pumpkin field: a sunset over a field of pumpkins. A mauve-to-gold sky with the sun on the
horizon, a line of far trees, a split-rail fence, rows of furrows with a few far pumpkins on
their vines, a haystack. Props: a big and a small pumpkin, a scarecrow and a garden bed of
carrots. The wind carries a leaf and bits of straw across the field.

Canvas 576 x 288, the pets stand on row 256 (the field), as in the other scenes.
"""
import math
import random

from scenery import Canvas, ellipse_points, falling_path, shaded, smooth_noise, tidy, tidy_rows

CANVAS = (576, 288)
GROUND = 256
GROUND_MARGIN = 16
THUMB = (96, 104, 480, 280)
PREVIEW_AMPLITUDE = 96

PALETTE = {
    'K': '#1f2133',  # outline, shared with the pets
    'a': '#9b5f7a',  # sky, top (mauve)
    'b': '#d9746a',  # sky (coral)
    'c': '#ef9f6b',  # sky (orange)
    'e': '#f7c58a',  # sky by the horizon, sun rim
    'i': '#fde7a8',  # the sun
    'f': '#6f4e63',  # far trees, lit
    'h': '#523a4c',  # far trees, shade; hat
    'P': '#8a5a3c',  # soil
    'p': '#a8724a',  # soil, lit ridges
    'q': '#5e3c28',  # soil, furrows
    'G': '#a3a04f',  # dry grass, lit
    'H': '#7c7a3a',  # dry grass
    'Y': '#fbb24a',  # pumpkins, light
    'O': '#f08a2f',  # pumpkins
    'R': '#b8561f',  # pumpkins, shade and ribs
    'v': '#5f6f34',  # stems, vines, carrot tops
    'y': '#f0d27a',  # hay, light
    'n': '#c9a24a',  # hay
    'x': '#96742e',  # hay, shade; stitches
    'U': '#5b7bb0',  # scarecrow's shirt
    'u': '#3e5a8a',  # scarecrow's shirt, shade
    'w': '#7d5a3c',  # wood
}
SKY_KEY = 'a'
EARTH_KEY = 'P'
ORPHANS_OK = 0

W, H = CANVAS


# --- the scene ----------------------------------------------------------------------------

def sky(c):
    """Sunset bands from mauve at the top to gold at the horizon, with stepped edges."""
    edges = [(82, 'b'), (136, 'c'), (176, 'e')]
    for x in range(W):
        for y in range(H):
            c.rows[y][x] = 'a'
        for edge, key in edges:
            line = edge + (2 if (x // 11) % 3 == 0 else 0) - (2 if (x // 15) % 4 == 1 else 0)
            for y in range(line, H):
                c.rows[y][x] = key
    # Thin clouds lit from below
    local = random.Random(6)
    for i in range(9):
        cx, cy = local.randrange(0, W), local.randrange(60, 150)
        for px, py in ellipse_points(cx, cy, local.randint(20, 40), 2.5):
            c.put(px, py, 'c' if c.get(px, py) == 'b' else 'b' if c.get(px, py) == 'a' else 'e')
    # The sun, half below the horizon
    sun = {p for p in ellipse_points(404, 206, 26, 26)}
    c.points({p for p in ellipse_points(404, 206, 29, 29)} - sun, 'e')
    c.points(sun, 'i')


def far_trees(c):
    """A line of far trees in two tones, no outline."""
    for base, key, seed in ((200, 'f', 61), (210, 'h', 62)):
        local = random.Random(seed)
        x = -10
        while x < W + 10:
            r = local.randint(6, 12)
            top = base - local.randint(0, 8)
            for px, py in ellipse_points(x, top + r, r, r):
                if py < 226:
                    c.put(px, py, key)
            c.rect(x - r, top + r, x + r + 1, 226, key)
            x += r + local.randint(4, 10)


def field(c):
    """Dry grass behind the fence, then the field: rows of furrows with lit ridges."""
    c.rect(0, 224, W, H, 'H')
    edge = smooth_noise(random.Random(9), W, 15, 2)
    for x in range(W):
        top = 224 + int(2 * edge[x])
        for y in range(top - 2, top):
            c.put(x, y, 'G')
    for x in range(0, W, 4):
        c.stamp(['G.', 'GH'], x + (x * 3) % 3, 228 + (x * 7) % 4)
    # Furrows: wider and further apart towards the viewer
    y = 240
    gap = 4
    while y < H:
        wave = smooth_noise(random.Random(y), W, 40, 1)
        for x in range(W):
            yy = y + int(wave[x])
            for k in range(gap):
                c.put(x, yy + k, 'P')
            c.put(x, yy, 'p')
            c.put(x, yy + gap - 1, 'q')
        y += gap
        gap += 1


def fence(c):
    """A split-rail fence along the back of the field."""
    rails = set()
    for y0 in (214, 222):
        rails |= {(x, y) for x in range(W) for y in (y0, y0 + 1)}
    posts = set()
    for x0 in range(10, W, 44):
        posts |= {(x, y) for x in (x0, x0 + 1, x0 + 2) for y in range(208, 234)}
    for x, y in rails | posts:
        c.put(x, y, 'w' if (x, y - 1) not in rails | posts else 'x')
    c.outline(rails | posts, 'K')


def pumpkin_shape(c, cx, bottom, w, h, seed, outline=True):
    """A pumpkin: a squat round body whose ribs curve round it (they crowd towards its sides),
    lit from the top left, a stem on top. Returns its points."""
    ribs = max(4, w // 6)
    cy = bottom - h / 2
    body = ellipse_points(cx, cy, w / 2, h / 2, power=2.15)
    # A dip at the top where the stem grows
    body -= {(x, y) for x, y in body if y < cy - h / 2 + 2 and abs(x + 0.5 - cx) < w * 0.12}
    for x, y in body:
        u = max(-0.999, min(0.999, (x + 0.5 - cx) / (w / 2)))
        v = (y + 0.5 - cy) / (h / 2)
        s_ = math.asin(u) / (math.pi / 2)                  # around the pumpkin, -1..1
        band = (s_ + 1) / 2 * ribs
        edge = band - math.floor(band)
        light = u * 0.8 + v < -0.55
        dark = u * 0.7 + v > 0.62
        if (edge < 0.12 or edge > 0.92) and abs(u) < 0.93:
            key = 'R'                                        # the groove between two ribs
        elif dark:
            key = 'R'
        elif light or (edge < 0.4 and u < 0.2 and v < 0.35):
            key = 'Y'
        else:
            key = 'O'
        c.put(x, y, key)
    top = int(cy - h / 2)
    stem = {(int(cx) + dx, top - k) for k in range(max(2, h // 5)) for dx in (0, 1)}
    stem.add((int(cx) + 2, top - max(2, h // 5) + 1))
    c.points(stem, 'v')
    if outline:
        c.outline(body | stem, 'K')
    return body | stem


def far_pumpkins(c):
    """A few pumpkins on their vines further off in the field."""
    for x, y, w, h in ((40, 246, 14, 9), (150, 243, 12, 8), (470, 244, 13, 8), (540, 248, 16, 10)):
        for k in range(-18, 18):
            c.put(x + k, y + int(1.5 * math.sin(k / 3)), 'v')
        pumpkin_shape(c, x, y + 1, w, h, x)


def build():
    c = Canvas(W, H)
    sky(c)
    far_trees(c)
    field(c)
    fence(c)
    far_pumpkins(c)
    # The haystack is part of the picture (as a prop it crowded the others on a narrow widget)
    c.stamp(haystack(), 150, GROUND - 36)
    tidy(c, keep=('K', 'i'))
    return c



# --- props --------------------------------------------------------------------------------

def big_pumpkin():
    c = Canvas(46, 34)
    pts = pumpkin_shape(c, 23, 33, 42, 26, 1)
    # A leaf of its vine on the ground
    c.stamp(['.vv..', 'vvvv.', '.vvvv', '..vv.'], 2, 29)
    return tidy_rows(c.grid())


def small_pumpkin(lean=0):
    c = Canvas(22, 16)
    pumpkin_shape(c, 11, 15, 18, 12, 2)
    rows = c.grid()
    if lean:
        # Rocking: the top rows shift a pixel, the bottom stays on the ground
        rows = [(r[-lean:] + r[:-lean]) if lean > 0 and y < 9 else (r[-lean:] + r[:-lean]) if lean < 0 and y < 9
                else r for y, r in enumerate(rows)]
    return tidy_rows(rows)


def scarecrow(sway=0, flutter=0):
    """A scarecrow on a post: a sack head with a stitched face and a hat, a blue shirt on the
    crossbar, straw at the cuffs and the collar. sway leans everything above the bar."""
    c = Canvas(64, 80)
    cx = 32
    post = {(x, y) for x in (cx - 1, cx, cx + 1) for y in range(30, 80)}
    bar = {(x, y) for x in range(cx - 24, cx + 25) for y in (33, 34)}
    shirt = set()
    for y in range(30, 52):
        half = 9 + (y - 30) // 4
        shirt |= {(x, y) for x in range(cx - half, cx + half + 1)}
    sleeves = {(x, y) for x in range(cx - 23, cx + 24) for y in range(31, 37)}
    head = ellipse_points(cx, 22, 8, 8)
    brim = {(x, y) for x in range(cx - 14, cx + 15) for y in (13, 14)}
    crown_ = {(x, y) for x in range(cx - 7, cx + 8) for y in range(5, 13)}
    straw = set()
    for sx in (cx - 24, cx + 24):
        straw |= {(sx + dx, y) for dx in (-2, -1, 0, 1, 2) for y in range(37, 42) if (dx + y) % 2 == 0}
    c.points(post, 'w')
    for (x, y), t in shaded(shirt | sleeves, 'U', 'U', 'u', shadow=((1, 1),)).items():
        c.put(x, y, t)
    for y in range(31, 50, 6):
        for x in range(cx - 8, cx + 9):
            if (x + y) % 6 == 0 and c.get(x, y) == 'U':
                c.put(x, y, 'u')
    for (x, y), t in shaded(head, 'y', 'n', 'x').items():
        c.put(x, y, t)
    # Stitched face: two eyes and a smile
    c.stamp(['xx...xx', 'xx...xx', '.......', 'x.....x', '.xxxxx.'], cx - 3, 19)
    for (x, y), t in shaded(brim | crown_, 'f', 'h', 'h').items():
        c.put(x, y, t)
    c.points({(x, 12) for x in range(cx - 7, cx + 8)}, 'R')
    for x, y in straw:
        c.put(x, y, 'y' if (x + y) % 3 else 'n')
    pts = post | bar | shirt | sleeves | head | brim | crown_ | straw
    c.outline(pts, 'K')
    rows = c.grid()
    if sway or flutter:
        out = []
        for y, r in enumerate(rows):
            shift = 0
            if y < 33:
                shift = sway
            elif y < 52:
                shift = (sway + 1) // 2 if sway > 0 else -((-sway + 1) // 2)
            if flutter and 31 <= y < 37:
                shift += flutter
            out.append((r[-shift:] + r[:-shift]) if shift > 0 else (r[-shift:] + r[:-shift]) if shift < 0 else r)
        rows = out
    return tidy_rows(rows, keep=('K', 'x', 'R'))


def haystack():
    c = Canvas(58, 38)
    pts = {p for p in ellipse_points(29, 30, 27, 26, power=2.3) if p[1] <= 36}
    for (x, y), t in shaded(pts, 'y', 'n', 'x').items():
        c.put(x, y, t)
    local = random.Random(71)
    for i in range(60):
        x, y = local.choice(sorted(pts))
        if c.get(x, y) == 'n' and c.get(x, y + 1) == 'n':
            c.put(x, y, 'x' if i % 2 else 'y')
            c.put(x, y + 1, 'x' if i % 2 else 'y')
    tops = {(x, y - 1) for x, y in pts if (x, y - 1) not in pts and x % 3 == 0}
    c.points(tops, 'y')
    c.outline(pts | tops, 'K')
    return tidy_rows(c.grid(), keep=('K',))


def bed(eaten=False):
    """A garden bed: a ridge of soil with three carrots, their orange shoulders and feathery tops."""
    c = Canvas(48, 22)
    ridge = {p for p in ellipse_points(24, 19, 23, 5) if p[1] <= 21}
    for (x, y), t in shaded(ridge, 'p', 'P', 'q').items():
        c.put(x, y, t)
    tops = set()
    leaf = ['.v..v.', 'vGv.vG', '.vGvv.', 'v.vGv.', '.vvvvv', '..vv..', '..vv..']
    for k, x in enumerate((10, 24, 38)):
        if eaten and k == 1:
            c.stamp(['qqq', 'qqq'], x - 1, 16)
            continue
        c.stamp(['OOO', 'OYO'], x - 1, 15)
        c.stamp(leaf, x - 2, 8)
        tops |= {(x - 2 + dx, 8 + dy) for dy, r in enumerate(leaf) for dx, ch in enumerate(r) if ch != '.'}
        tops |= {(x - 1 + dx, 15 + dy) for dx in range(3) for dy in range(2)}
    c.outline(ridge | tops, 'K')
    return tidy_rows(c.grid(), keep=('K',))


SCENE = build().grid()

PROPS = {
    'pumpkin_big': {'offset': -0.7, 'layer': 'front', 'baseline': 33, 'ambient': 'still',
                    'states': {'still': (500, [big_pumpkin()])}},
    'bed': {'offset': -0.1, 'layer': 'back', 'baseline': 21, 'ambient': 'still',
            'states': {'still': (500, [bed()]), 'eaten': (500, [bed(eaten=True)])}},
    'pumpkin_small': {'offset': 0.35, 'layer': 'back', 'baseline': 15, 'ambient': 'still',
                      'states': {'still': (500, [small_pumpkin()]),
                                 'wobble': (120, [small_pumpkin(1), small_pumpkin(), small_pumpkin(-1),
                                                  small_pumpkin()])}},
    # The parrot perches on the end of the crossbar, 46 rows above the ground
    'scarecrow': {'offset': 0.8, 'layer': 'back', 'baseline': 79, 'perch': 46, 'ambient': 'breeze',
                  'states': {'breeze': (400, [scarecrow(), scarecrow(flutter=1), scarecrow(), scarecrow(flutter=-1)]),
                             'shake': (110, [scarecrow(1), scarecrow(2), scarecrow(1), scarecrow(),
                                             scarecrow(-1), scarecrow(-2), scarecrow(-1), scarecrow()])}},
}


# --- the wind -----------------------------------------------------------------------------

OAK_LEAF = [
    ['.KKKK.', 'KOOOOK', 'KORRRK', '.KRRK.', '..KK..'],
    ['..KK..', '.KOOK.', 'KOORK.', 'KRRK..', '.KK...'],
    ['......', 'KKKKK.', 'KOOOK.', '.KKK..', '......'],
    ['.KKKK.', 'KOOOOK', 'KRRROK', '.KRRK.', '..KK..'],
]
STRAW = [['yy.', '.nn'], ['.yy', 'nn.'], ['yy', 'nn']]


def gust(start, end, frames, lift, sprites):
    """Something carried by the wind: from start to end, bobbing up and down, turning."""
    (x0, y0), (x1, y1) = start, end
    path = []
    for i in range(frames):
        t = i / (frames - 1)
        x = x0 + (x1 - x0) * t
        y = y0 + (y1 - y0) * t - lift * math.sin(t * math.pi * 3) * (1 - t * 0.5)
        path.append((int(round(x)), int(round(y)), i % sprites))
    return path


ELEMENTS = {
    'leaf': {'frames': OAK_LEAF, 'interval': 110, 'length': 20, 'respawns': True,
             'path': gust((4, 214), (560, 236), 20, 12, 4)},
    'straw': {'frames': STRAW, 'interval': 130, 'length': 12, 'respawns': True,
              'path': gust((380, 226), (570, 244), 12, 6, 3)},
}

EVENT_SPRITES = {'leaf_%d' % i: f for i, f in enumerate(OAK_LEAF)}
