"""Maple park: a golden autumn evening. Two big maples frame the scene and their crowns meet
over the middle, a lamp post and a bench stand by a sandy path. Leaves come loose and drift
down; the lamp flickers; a tuft of grass sways. A heap of fallen leaves (a prop) lies on the path.

Canvas 576 x 288, the pets stand on row 256 (the path), GROUND_MARGIN rows above the bottom of
the widget. A widget shows the middle of the canvas from the ground up, so everything that matters
sits within ~180 rows above the ground; the top rows are plain sky and the bottom ones plain grass,
so the widget can continue them with flat colour when it is bigger than the canvas.
"""
import math
import random

from scenery import Canvas, crown, dome, ellipse_points, falling_path, smooth_noise, tidy, tidy_rows, trunk

CANVAS = (576, 288)
GROUND = 256
# Rows of ground the widget shows under the pets' feet
GROUND_MARGIN = 16
# The part of the scene on its card in the app (x0, y0, x1, y1)
THUMB = (96, 104, 480, 280)
# Canvas pixels a pet moves at ground position 1.0 on a 4x3 widget (where the previews put props)
PREVIEW_AMPLITUDE = 96

PALETTE = {
    'K': '#1f2133',  # outline, shared with the pets
    'a': '#f2b470',  # sky, top
    'b': '#f6c98a',  # sky
    'c': '#fae0ae',  # sky by the horizon, sun rim
    'e': '#fff1c4',  # sun, lamp light
    'f': '#c98a64',  # far trees, lit
    'h': '#a8705a',  # far trees, shade
    'Y': '#ffd26a',  # leaves, light
    'O': '#f6a531',  # leaves
    'R': '#e8702a',  # leaves, shade
    'r': '#b9452a',  # leaves, deep shade
    'd': '#7a2f2b',  # inside the crowns
    'w': '#8a5a3c',  # bark and wood, lit
    'W': '#6e4a37',  # bark and wood
    'v': '#4b3328',  # bark and wood, shade
    'm': '#3c3f55',  # iron: lamp post, bench legs
    'g': '#ffd36b',  # lamp glass
    'G': '#9aa64e',  # grass, lit
    'H': '#7a8a3c',  # grass
    'j': '#56622c',  # grass, shade
    'p': '#e2be84',  # path, lit
    'P': '#c9a06e',  # path
    'q': '#a47e52',  # path, shade
}
SKY_KEY = 'a'
EARTH_KEY = 'H'
ORPHANS_OK = 0

W, H = CANVAS
SKYLINE = 'abcefh'


# --- small parts --------------------------------------------------------------------------

# A maple leaf seen flat, and turning (the falling leaf's four frames)
LEAF_FRAMES = [
    ['..K.K..',
     '.KOKOK.',
     'KOOROOK',
     '.KRRRK.',
     '..KrK..',
     '...K...'],
    ['...KK..',
     '..KOOK.',
     '.KOORK.',
     'KORRK..',
     '.KrK...',
     '..K....'],
    ['.......',
     '.......',
     'KKKKKK.',
     'KOORRK.',
     '.KKKK..',
     '.......'],
    ['..KK...',
     '.KOOK..',
     '.KROOK.',
     '..KRROK',
     '...KrK.',
     '....K..'],
]
# The same leaf lying on the ground after its fall
LEAF_LYING = ['.KK.KK.',
              'KOOKOOK',
              'KRRRRrK',
              '.KKKKK.']

# Lamp glass: bright, a little dimmer, and a flicker
GLASS = [
    ['ggggggg', 'geeeeeg', 'geeeeeg', 'geeeeeg', 'geeeeeg', 'geeeeeg', 'ggeeegg', 'ggggggg'],
    ['ggggggg', 'ggeeegg', 'ggeeegg', 'geeeeeg', 'geeeeeg', 'ggeeegg', 'ggggggg', 'ggggggg'],
    ['ggggggg', 'ggggggg', 'ggeeegg', 'ggeeegg', 'ggeeegg', 'ggggggg', 'ggggggg', 'ggggggg'],
]

# A tuft of grass leaning with the wind
TUFT = [
    ['..G..G.', '.GH..GH', '.GH.GH.', 'GHHGHH.', 'HHjHHj.', 'Hjjjjj.'],
    ['...G.G.', '..GH.GH', '.GH.GH.', '.GHGHH.', 'HHjHHj.', 'Hjjjjj.'],
    ['....GG.', '...GHG.', '..GHGH.', '.GHGHH.', 'HHjHHj.', 'Hjjjjj.'],
]


# --- the scene ----------------------------------------------------------------------------

def sky(c):
    """Bands from amber at the top to pale gold at the horizon, with stepped edges, and the low sun."""
    edges = [(118, 'a', 'b'), (166, 'b', 'c')]
    for x in range(W):
        for y in range(H):
            c.rows[y][x] = 'a'
        for edge, upper, lower in edges:
            step = edge + (2 if (x // 9) % 3 == 0 else 0) - (2 if (x // 13) % 4 == 1 else 0)
            for y in range(step, H):
                c.rows[y][x] = lower
    sun = ellipse_points(288, 196, 20, 20)
    c.points(sun, 'e')
    c.points({p for p in ellipse_points(288, 196, 23, 23) if p not in sun}, 'c')


def far_trees(c):
    """Two rows of hazy crowns, no outline: round tops, the far row lit, the near row in shade."""
    for row, (base, key, r_min, r_max, step, seed) in enumerate(((192, 'f', 8, 13, 12, 11), (206, 'h', 7, 11, 10, 12))):
        local = random.Random(seed)
        x = -10
        while x < W + 10:
            r = local.randint(r_min, r_max)
            top = base - local.randint(0, 9)
            for px, py in ellipse_points(x, top + r, r, r):
                if py < 232:
                    c.put(px, py, key)
            c.rect(x - r, top + r, x + r + 1, 232, key)
            x += step + local.randint(-3, 4)


def meadow(c):
    """Grass behind the path, the path the pets walk on, and grass in front."""
    c.rect(0, 224, W, H, 'H')
    edge = smooth_noise(random.Random(3), W, 19, 2)
    for x in range(W):
        top = 224 + int(2 * edge[x])
        for y in range(top - 2, top):
            c.put(x, y, 'G')
    top_edge = smooth_noise(random.Random(5), W, 31, 2)
    bottom_edge = smooth_noise(random.Random(6), W, 27, 2)
    for x in range(W):
        top = 244 + int(1.6 * top_edge[x])
        bottom = 276 + int(1.4 * bottom_edge[x])
        for y in range(top, bottom):
            c.put(x, y, 'P')
        c.put(x, top, 'p')
        c.put(x, top + 1, 'p')
        c.put(x, bottom - 1, 'q')
        c.put(x, bottom, 'q')
    local = random.Random(8)
    for i in range(16):
        c.stamp(['.pp.', 'pPPq', '.qq.'], local.randrange(12, W - 12), local.randrange(252, 272), only_over='P')
    tuft = ['.G..G.', '.GH.GH', 'GHHGHH', 'HHjHHj']
    for x in range(-2, W, 9):
        c.stamp(tuft, x + (x * 3) % 4, 278 + (x * 7) % 3)


def lamp(c, x):
    """A park lantern on an iron post: a stepped foot, the post, a lantern with a pointed roof
    and a finial. Its glass is an animated element (it flickers). Returns where the glass goes."""
    top = GROUND - 92
    post = {(x + dx, y) for dx in (0, 1, 2) for y in range(top + 26, GROUND)}
    foot = ({(x + dx, y) for dx in range(-4, 7) for y in range(GROUND - 3, GROUND)}
            | {(x + dx, y) for dx in range(-2, 5) for y in range(GROUND - 6, GROUND - 3)})
    collar = {(x + dx, y) for dx in range(-2, 5) for y in range(top + 23, top + 27)}
    head = {(x + dx, y) for dx in range(-5, 8) for y in range(top + 6, top + 23)}
    roof = set()
    for k in range(6):
        roof |= {(x + dx, top + 5 - k) for dx in range(-6 + k, 9 - k)}
    finial = {(x + 1, top - 1), (x + 1, top - 2)}
    pts = post | foot | collar | head | roof | finial
    c.points(pts, 'm')
    for y in range(top + 28, GROUND - 6):
        c.put(x, y, 'h')
    for k in range(6):
        c.put(x - 6 + k, top + 5 - k, 'h')
    c.outline(pts, 'K')
    for px, py in ellipse_points(x + 1, GROUND - 1, 22, 4):
        if c.get(px, py) == 'P':
            c.put(px, py, 'p')
    return (x - 3, top + 8)


def bench(c, x, y):
    """A park bench: three back slats, a seat, iron legs."""
    slats = set()
    for row in (y - 22, y - 17, y - 12):
        slats |= {(px, py) for px in range(x, x + 70) for py in range(row, row + 3)}
    seat = {(px, py) for px in range(x - 3, x + 73) for py in range(y - 7, y - 3)}
    legs = {(px, py) for px0 in (x + 2, x + 64) for px in range(px0, px0 + 3) for py in range(y - 24, y)}
    c.points(legs, 'm')
    wood = slats | seat
    for px, py in wood:
        c.put(px, py, 'w' if (px, py - 1) not in wood else ('v' if (px, py + 1) not in wood else 'W'))
    c.outline(wood | legs, 'K')


def fallen_leaves(c):
    """A few leaves lying on the path and the grass."""
    shapes = [['.OO.', 'ORRO', '.rr.'], ['YO.', 'ORR', '.R.'], ['.OY', 'RRO', '.R.']]
    local = random.Random(12)
    for i in range(16):
        c.stamp(shapes[i % 3], local.randrange(10, W - 10), local.randrange(250, 272), only_over='Pp')
    for i in range(8):
        c.stamp(shapes[(i + 1) % 3], local.randrange(10, W - 10), local.randrange(226, 240), only_over='GHj')


# The crowns meet over the middle of the scene, low enough to show on every widget size
LEFT_CLUMPS = dome(150, 94, 124, 62, 24, 31)
RIGHT_CLUMPS = dome(432, 96, 122, 60, 24, 41)


def build():
    c = Canvas(W, H)
    sky(c)
    far_trees(c)
    meadow(c)
    trunk(c, 146, 110, GROUND + 2, 22, 12, 4)
    left = crown(c, LEFT_CLUMPS, (('Y', 'O', 'R'), ('O', 'R', 'r'), ('R', 'r', 'd')), 21, SKYLINE)
    trunk(c, 438, 112, GROUND + 2, 22, 12, -3)
    right = crown(c, RIGHT_CLUMPS, (('Y', 'Y', 'O'), ('Y', 'O', 'R'), ('O', 'R', 'r')), 23, SKYLINE)
    glass = lamp(c, 236)
    bench(c, 330, GROUND - 2)
    fallen_leaves(c)
    tidy(c)
    return c, left | right, glass


SCENE_CANVAS, _CROWNS, GLASS_AT = build()
SCENE = SCENE_CANVAS.grid()

# Lowest leafy pixel above each column: where an event's leaf comes loose (-1: open sky)
CANOPY_EDGE = []
for _x in range(W):
    _ys = [y for (x, y) in _CROWNS if x == _x and y < GROUND - 60]
    CANOPY_EDGE.append(max(_ys) if _ys else -1)


# --- the heap of leaves (a prop) ----------------------------------------------------------

HEAP_W, HEAP_H = 84, 34        # room above the heap for the leaves it throws up
HEAP_BASE = HEAP_H - 2         # its row on the ground line

LEAVES = [['.OO..', 'OOROO', '.ORR.', '..R..'],
          ['..OO.', '.OORO', 'ORRO.', '.R...'],
          ['.OO.', 'OORR', 'ROR.', '.R..'],
          ['OO...', 'ORRO.', '.RROO', '...R.']]
TONES = {0: {'O': 'Y', 'R': 'O'}, 1: {}, 2: {'O': 'R', 'R': 'r'}}
FLYING = [['OO.', '.RR'], ['.YY', 'OO.'], ['RR', 'OO'], ['O.', 'OR', '.R']]


def heap(height=11, shuffle=0, flying=()):
    """The heap: leaves piled up from the ground, lit ones on top, darker underneath, outlined.
    height lowers it (leaves thrown up); shuffle moves a few top leaves (rustling); flying:
    (x, y, k) leaves in the air."""
    c = Canvas(HEAP_W, HEAP_H)
    local = random.Random(9)
    x, y, rx = HEAP_W // 2, HEAP_BASE, 38
    placed = []
    for row in range(height, -1, -1):
        half = rx * math.sqrt(max(0.0, 1 - (row / (height + 1)) ** 2))
        n = int(half * 2 / 4) + 1
        for i in range(n):
            px = int(x - half + i * (2 * half / max(n - 1, 1)) + local.randint(-2, 2))
            py = y - row - local.randint(0, 1)
            if shuffle and row >= height - 2 and (i + shuffle) % 3 == 0:
                px += (1 if shuffle % 2 else -1)
                py -= 1
            placed.append((py, px, local.randrange(len(LEAVES))))
    pts = set()
    for py, px, k in sorted(placed, key=lambda q: -q[0]):
        band = 0 if (y - py) / max(height, 1) > 0.6 else 1 if (y - py) / max(height, 1) > 0.25 else 2
        shape = [''.join(TONES[band].get(ch, ch) for ch in r) for r in LEAVES[k]]
        for dy, r in enumerate(shape):
            for dx, ch in enumerate(r):
                if ch != '.' and py + dy <= y + 1:
                    c.put(px + dx, py + dy, ch)
                    pts.add((px + dx, py + dy))
    c.outline(pts, 'K')
    for fx, fy, k in flying:
        c.stamp(FLYING[k % len(FLYING)], fx, fy)
    return tidy_rows(c.grid())


def burst():
    """The heap thrown up: it sinks, ten leaves fly up and out along arcs, and settle back on it."""
    frames = []
    arcs = [(-30, 14, 0), (-18, 20, 1), (-6, 24, 2), (6, 23, 3), (18, 19, 0), (30, 13, 1),
            (-24, 9, 2), (24, 10, 3), (-12, 16, 1), (12, 17, 2)]
    heights = [11, 7, 5, 4, 4, 5, 7, 9, 10, 11]
    for f, h in enumerate(heights):
        t = f / (len(heights) - 1)
        flying = []
        if 0 < f < len(heights) - 1:
            for k, (dx, up, s) in enumerate(arcs):
                lift = up * 4 * t * (1 - t)
                fx = int(HEAP_W / 2 + dx * (0.4 + t))
                fy = int(HEAP_BASE - 10 - lift)
                if 0 <= fx < HEAP_W - 3 and 0 <= fy < HEAP_H - 3:
                    flying.append((fx, fy, s + f))
        frames.append(heap(h, flying=flying))
    return frames


PROPS = {
    'heap': {'offset': -0.62, 'layer': 'back', 'baseline': HEAP_BASE, 'ambient': 'still',
             'states': {'still': (400, [heap()]),
                        'rustle': (150, [heap(shuffle=s) for s in (1, 2, 1, 0)]),
                        'burst': (100, burst())}},
}

ELEMENTS = {
    # A leaf drifts down from the left crown into the grass, then the next one comes loose
    'leaf': {'frames': LEAF_FRAMES, 'interval': 150, 'length': 16, 'respawns': True,
             'path': falling_path((122, 150), (104, 222), 16, 9, 1.5)},
    'glass': {'frames': GLASS, 'interval': 220, 'length': 4,
              'path': [(GLASS_AT[0], GLASS_AT[1] + 2, f) for f in (0, 1, 0, 2)]},
    'tuft': {'frames': TUFT, 'interval': 300, 'length': 4,
             'path': [(486, 276, f) for f in (0, 1, 2, 1)]},
}

# Sprites events use: the leaf turning as it falls, and lying on the ground
EVENT_SPRITES = {'leaf_%d' % i: f for i, f in enumerate(LEAF_FRAMES)}
EVENT_SPRITES['leaf_lying'] = LEAF_LYING
