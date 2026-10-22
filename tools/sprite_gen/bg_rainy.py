"""Rainy evening: soft rain over a cobbled lane at dusk. A grey-lilac sky, roofs with warm
windows behind a wet hedge, a lamp throwing a warm glow on the wet stones, a bench, yellow
leaves. Props: two puddles with rings spreading on them (a yellow leaf floats in the small one)
and a red umbrella left by the bench. The rain is a tile layer over everything.

Canvas 576 x 288, the pets stand on row 256 (the cobbles), as in the other scenes.
"""
import math
import random

from scenery import Canvas, crown, dome, ellipse_points, shaded, smooth_noise, tidy, tidy_rows, trunk

CANVAS = (576, 288)
GROUND = 256
GROUND_MARGIN = 16
THUMB = (96, 104, 480, 280)
PREVIEW_AMPLITUDE = 96

# 24 colours: the rain, the wet light and the warm lamp light need their own tones (no alpha)
PALETTE = {
    'K': '#1f2133',  # outline, shared with the pets
    'a': '#6f6890',  # sky, top
    'b': '#857ca3',  # sky
    'c': '#9d93b4',  # sky by the horizon
    'f': '#5a5478',  # roofs and trees, lit
    'h': '#4a4466',  # roofs and trees, shade; lit iron
    'e': '#fff0c2',  # lamp light, lit windows
    'g': '#ffd27a',  # lamp glass, glow
    'o': '#e8a35c',  # warm light on the wet stones
    'm': '#2f2c40',  # iron
    'P': '#6d6a7d',  # cobbles
    'p': '#858196',  # cobbles, lit tops
    'q': '#4e4b5e',  # gaps between the cobbles, hedge shade
    'l': '#9fb2e0',  # wet shine, ripples
    'u': '#4f5f8e',  # puddle water
    'U': '#6f84b8',  # sky in the puddles
    'z': '#c7d2f0',  # rain drops
    'Y': '#f2c14e',  # yellow leaves
    'O': '#c98f2e',  # yellow leaves, shade
    'R': '#d9534f',  # umbrella
    'r': '#a53a3c',  # umbrella, shade
    'w': '#7a5a48',  # wood
    'v': '#57402f',  # wood, shade
    'G': '#5f7a5a',  # hedge
}
SKY_KEY = 'a'
EARTH_KEY = 'P'
ORPHANS_OK = 0

W, H = CANVAS


# --- the scene ----------------------------------------------------------------------------

def sky(c):
    """Bands of rain clouds, darker at the top, with stepped edges."""
    edges = [(70, 'b'), (150, 'c')]
    for x in range(W):
        for y in range(H):
            c.rows[y][x] = 'a'
        for edge, key in edges:
            line = edge + (3 if (x // 17) % 3 == 0 else 0) - (2 if (x // 11) % 4 == 2 else 0)
            for y in range(line, H):
                c.rows[y][x] = key
    # Heavier clouds: darker lumps along the band edges
    local = random.Random(4)
    for i in range(14):
        cx, cy = local.randrange(0, W), local.choice((64, 144))
        for px, py in ellipse_points(cx, cy, local.randint(18, 34), local.randint(5, 8)):
            if c.get(px, py) in 'bc':
                c.put(px, py, 'a' if cy < 100 else 'b')


def roofs(c):
    """Houses behind the hedge: gables and chimneys in two tones, a few warm windows."""
    local = random.Random(14)
    x = -20
    while x < W + 20:
        w = local.randint(46, 70)
        top = local.randint(150, 178)
        body = {(px, py) for px in range(x, x + w) for py in range(top + 14, 230)}
        roof = set()
        for k in range(16):
            roof |= {(px, top + k) for px in range(x + w // 2 - k * w // 32 - 2, x + w // 2 + k * w // 32 + 3)}
        chimney = {(px, py) for px in range(x + w - 14, x + w - 8) for py in range(top - 4, top + 10)}
        for px, py in body | chimney:
            c.put(px, py, 'f')
        for px, py in roof:
            c.put(px, py, 'h')
        for wx in range(x + 8, x + w - 10, 16):
            for wy in (top + 22, top + 40):
                if local.random() < 0.5:
                    c.rect(wx, wy, wx + 6, wy + 8, 'e' if local.random() < 0.6 else 'g')
                    c.rect(wx, wy + 3, wx + 6, wy + 4, 'h')
                else:
                    c.rect(wx, wy, wx + 6, wy + 8, 'h')
        x += w + local.randint(-6, 4)


def hedge(c):
    """A clipped hedge along the lane, wet: rounded top, darker underneath."""
    top_line = smooth_noise(random.Random(21), W, 9, 2)
    for x in range(W):
        top = 214 + int(3 * top_line[x])
        for y in range(top, 246):
            c.put(x, y, 'G' if y < 238 else 'q')
        c.put(x, top, 'l' if (x // 7) % 5 == 0 else 'G')
    for x in range(0, W, 6):
        for y in range(222, 236, 7):
            c.stamp(['.q.', 'qGq'], x + (y // 7) % 3, y)


def cobbles(c):
    """The lane: rows of rounded cobbles with dark gaps and lit tops, wet shine here and there."""
    c.rect(0, 242, W, H, 'q')
    local = random.Random(31)
    for row, y in enumerate(range(242, H, 6)):
        x = -((row * 5) % 9)
        while x < W:
            w = local.randint(7, 10)
            stone = {(px, py) for px in range(x + 1, x + w) for py in range(y + 1, y + 5)}
            stone -= {(x + 1, y + 1), (x + w - 1, y + 1), (x + 1, y + 4), (x + w - 1, y + 4)}
            for px, py in stone:
                c.put(px, py, 'p' if py == y + 1 else 'P')
            if local.random() < 0.18:
                c.put(x + 3, y + 2, 'l')
                c.put(x + 4, y + 2, 'l')
            x += w


def lamp(c, x):
    """A street lamp: iron post and lantern; a pool of warm light on the stones under it and a
    long wet reflection. Returns where the glass goes (an animated element)."""
    top = GROUND - 100
    post = {(x + dx, y) for dx in (0, 1, 2) for y in range(top + 24, GROUND)}
    foot = {(x + dx, y) for dx in range(-3, 6) for y in range(GROUND - 4, GROUND)}
    head = {(x + dx, y) for dx in range(-5, 8) for y in range(top + 6, top + 22)}
    roof = set()
    for k in range(5):
        roof |= {(x + dx, top + 5 - k) for dx in range(-6 + k, 9 - k)}
    arm = {(x + dx, top + 22) for dx in range(-2, 5)}
    pts = post | foot | head | roof | arm
    c.points(pts, 'm')
    for y in range(top + 26, GROUND - 4):
        c.put(x, y, 'h')
    c.outline(pts, 'K')
    # Warm light on the wet cobbles: an outer pool on the stones' lit tops, an inner one on all of them
    outer = ellipse_points(x + 1, GROUND + 4, 38, 9)
    inner = ellipse_points(x + 1, GROUND + 3, 20, 5)
    for px, py in outer:
        if c.get(px, py) == 'p':
            c.put(px, py, 'o')
    for px, py in inner:
        if c.get(px, py) in 'Pp':
            c.put(px, py, 'o')
    # The lamp's reflection: a streak down the wet stones, bright near the lamp
    for py in range(GROUND + 1, H - 2):
        if py % 6 in (2, 3, 4):
            for px in (x, x + 1, x + 2):
                c.put(px, py, 'g' if py < GROUND + 12 else 'o')
    return (x - 3, top + 8)


def bench(c, x, y):
    """A wet park bench: three back slats, a seat, iron legs, shine on the top edges."""
    slats = set()
    for row in (y - 22, y - 17, y - 12):
        slats |= {(px, py) for px in range(x, x + 64) for py in range(row, row + 3)}
    seat = {(px, py) for px in range(x - 3, x + 67) for py in range(y - 7, y - 3)}
    legs = {(px, py) for px0 in (x + 2, x + 58) for px in range(px0, px0 + 3) for py in range(y - 24, y)}
    c.points(legs, 'm')
    wood = slats | seat
    for px, py in wood:
        c.put(px, py, 'l' if (px, py - 1) not in wood and px % 9 < 3 else
              'w' if (px, py - 1) not in wood else 'v' if (px, py + 1) not in wood else 'w')
    c.outline(wood | legs, 'K')


def tree(c, x, seed):
    """A lime tree with the last of its yellow leaves: a trunk and a round crown in three tones,
    dimmer than in the sun (it is raining)."""
    trunk(c, x, 118, GROUND + 2, 16, 9, 2, tones=('w', 'w', 'v'))
    clumps = dome(x, 92, 64, 44, 20, seed)
    crown(c, clumps, (('Y', 'Y', 'O'), ('Y', 'O', 'O'), ('O', 'O', 'v')), seed, 'abcfhe')


def leaves_on_stones(c):
    shapes = [['.YY.', 'YOOY', '.OO.'], ['YY.', '.OO'], ['.YY', 'OO.']]
    local = random.Random(41)
    for i in range(18):
        c.stamp(shapes[i % 3], local.randrange(8, W - 8), local.randrange(248, 282), only_over='Ppqo')


def build():
    c = Canvas(W, H)
    sky(c)
    roofs(c)
    hedge(c)
    cobbles(c)
    tree(c, 96, 51)
    tree(c, 488, 52)
    glass = lamp(c, 196)
    bench(c, 358, GROUND - 2)
    leaves_on_stones(c)
    tidy(c, keep=('K', 'e', 'g'))
    return c, glass


SCENE_CANVAS, GLASS_AT = build()
SCENE = SCENE_CANVAS.grid()


# --- animated parts -----------------------------------------------------------------------

GLASS = [
    ['ggggggg', 'geeeeeg', 'geeeeeg', 'geeeeeg', 'geeeeeg', 'gggeggg', 'ggggggg'],
    ['ggggggg', 'ggeeegg', 'geeeeeg', 'geeeeeg', 'ggeeegg', 'ggggggg', 'ggggggg'],
]


def rain_tile(shift):
    """48 x 48 of soft rain: thin drops of two lengths, falling 12 pixels a frame."""
    c = Canvas(48, 48)
    local = random.Random(77)
    drops = [(local.randrange(0, 48), local.randrange(0, 48), local.choice((3, 4))) for _ in range(9)]
    for x, y, length in drops:
        for k in range(length):
            yy = (y + shift + k) % 48
            c.put(x, yy, 'z')
            if k == length - 1 and x + 1 < 48:
                pass
    return c.grid()


def puddle(width, height, rings=(), leaf=None):
    """A puddle: dark water with the sky's light on its far side, outlined, rings spreading on it
    ((x, radius) each) and maybe a yellow leaf floating."""
    c = Canvas(width, height)
    pts = ellipse_points(width / 2, height / 2, width / 2 - 1, height / 2 - 1, power=2.2)
    for (x, y), t in shaded(pts, 'U', 'u', 'u', shadow=((0, 2),)).items():
        c.put(x, y, t)
    c.outline(pts, 'K')
    for rx, rr in rings:
        for px, py in ellipse_points(rx, height / 2, rr, max(1, rr / 3)):
            if (px, py) in pts and not (px, py) in ellipse_points(rx, height / 2, rr - 1.2, max(0.5, rr / 3 - 0.8)):
                c.put(px, py, 'l')
    if leaf:
        # Big enough to make an umbrella for the hamster (the hamster's LIME_LEAF)
        c.stamp(['...KKKKKKKKKK...', '.KKYYYYYYYYYYKK.', 'KYYOYYYYOYYYYOYK', '.KOOOOOOOOOOOOK.',
                 '..KKKKKKKKKKKK..'], *leaf)
    return tidy_rows(c.grid(), keep=('K', 'l'))


def ring_frames(width, height, spots, leaf=None):
    """Rain on a puddle: rings at the spots grow from a dot, each starting at its own time."""
    frames = []
    for f in range(6):
        rings = []
        for k, x in enumerate(spots):
            age = (f + k * 2) % 6
            if age < 4:
                rings.append((x, 1.5 + age * 1.6))
        frames.append(puddle(width, height, rings, leaf))
    return frames


def splash_frames(width, height):
    """Something stepped in the puddle: a big ring and drops thrown up out of it."""
    frames = []
    for f in range(6):
        c = Canvas(width, height + 10)
        base = puddle(width, height, [(width / 2, 2 + f * 3.4)])
        c.stamp(base, 0, 10)
        for k, dx in enumerate((-8, -4, 4, 8)):
            up = 9 - abs(f - 2) * 3 - (k % 2)
            if 0 < f < 5 and up > 0:
                c.stamp(['z', 'l'], int(width / 2 + dx * (1 + f * 0.4)), 10 - up)
        frames.append(c.grid())
    return frames


def umbrella(drip=None):
    """A red umbrella stuck in the ground, open: a canopy in three tones with ribs, a long shaft
    with a crook handle; drip (x, y) is a drop running off its rim."""
    c = Canvas(56, 76)
    canopy = {p for p in ellipse_points(28, 22, 26, 18) if p[1] <= 22}
    for (x, y), t in shaded(canopy, 'R', 'R', 'r', shadow=((2, 2),)).items():
        c.put(x, y, t)
    for x in range(3, 54):
        if x % 9 in (0, 1):
            c.put(x, 22, 'r')
    # Scalloped rim
    for k in range(6):
        for px, py in ellipse_points(6 + k * 9, 22, 4.6, 2.2):
            if py >= 22:
                c.put(px, py, 'R' if py == 22 else 'r')
                canopy.add((px, py))
    c.put(28, 3, 'm')
    c.put(28, 2, 'm')
    shaft = {(x, y) for x in (27, 28) for y in range(24, 72)}
    hook = {(29, 72), (30, 73), (31, 73), (32, 72)}
    c.points(shaft | hook, 'm')
    c.outline(canopy | shaft | hook | {(28, 2), (28, 3)}, 'K')
    # Shine on the wet canopy
    for x, y in ((14, 10), (15, 9), (16, 9), (20, 7), (21, 7)):
        c.put(x, y, 'l')
    if drip:
        c.stamp(['z', 'z'], *drip)
    return tidy_rows(c.grid(), keep=('K', 'l', 'z'))


PROPS = {
    'puddle': {'offset': 0.12, 'layer': 'back', 'baseline': 6, 'ambient': 'rain',
               'states': {'rain': (150, ring_frames(84, 12, (16, 42, 66))),
                          'splash': (100, [f[10:] for f in splash_frames(84, 12)])}},
    'puddle_leaf': {'offset': -0.58, 'layer': 'back', 'baseline': 4, 'ambient': 'rain',
                    'states': {'rain': (150, ring_frames(40, 8, (7, 33), leaf=(12, 1))),
                               'empty': (150, ring_frames(40, 8, (7, 20, 33)))}},
    'umbrella': {'offset': 0.72, 'layer': 'back', 'baseline': 74, 'ambient': 'drip',
                 'states': {'drip': (180, [umbrella(None), umbrella((5, 25)), umbrella((5, 28)),
                                           umbrella((5, 32)), umbrella(None), umbrella((49, 26))])}},
}

TILES = {
    'rain': {'frames': [rain_tile(12 * i) for i in range(4)], 'interval': 90},
}

ELEMENTS = {
    'glass': {'frames': GLASS, 'interval': 260, 'length': 4,
              'path': [(GLASS_AT[0], GLASS_AT[1] + 2, f) for f in (0, 1, 0, 0)]},
}

# Sprites events use: a raindrop falling into the panda's open mouth
DROP = ['z', 'z', 'l']
EVENT_SPRITES = {'drop': DROP}
