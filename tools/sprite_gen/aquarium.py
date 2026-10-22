"""Aquarium: a glass tank with three goldfish, plants and decor, drawn as one 128x96 scene.

Unlike the other pets the whole scene is the "pet": the tank stands still in the widget and the
fish swim inside the frames. Big regular areas (water, sand, glass) are filled by small functions
below; everything with a shape (fish, plants, castle, chest, shell, starfish) is either a text
matrix or a polygon traced on the pixel grid, so every frame is reproducible.

Light comes from the top left. The outline is the shared deep blue-grey K; objects in the water
get it too (LINE_OF of every water tone is K).
"""
import math

from sprite_gen import L, part

CANVAS = (128, 96)
MAX_COLOURS = 24        # two sets of water tones (day and night) need more than 16

PALETTE = {
    'K': '#1f2133',  # outline, pupils
    'W': '#fbf6ec',  # shine, white patches, bubbles, lamp
    'A': '#bdeef6',  # surface line, light rays in the upper water
    'B': '#6fc8e3',  # upper water
    'C': '#409fcf',  # middle water, rays in the deep
    'D': '#2b73a6',  # deep water; night surface
    'M': '#1c4c78',  # night water, upper
    'N': '#163b60',  # night water, middle
    'Q': '#112c49',  # night water, deep
    'Y': '#f2d699',  # sand light
    'y': '#d4ae6c',  # sand, chest wood light
    't': '#9f7a45',  # sand shadow, chest wood
    'G': '#ccd3df',  # glass, stone light
    'g': '#8f99ad',  # stone, lid and stand light
    'k': '#596278',  # stone shadow, lid and stand
    'P': '#9ed870',  # plants light
    'p': '#56ad4c',  # plants
    'q': '#2e7a46',  # plants shadow
    'F': '#ffd45c',  # gold: fins, chest bands, lamp glow
    'O': '#f6922f',  # orange fish
    'o': '#cc5726',  # orange shadow, gills
    'R': '#e0473e',  # red patches, hearts
    'S': '#3b3d52',  # ground shadow
}
OUTLINE = 'K'
WATER = 'ABCDMNQ'
LINE_OF = {c: 'K' for c in WATER}
LINE_OF.update({'O': 'o', 'F': 'O', 'o': 'o', 'R': 'o', 'W': 'G',
                'P': 'q', 'p': 'q', 'q': 'q', 'G': 'k', 'g': 'k', 'k': 'k',
                'Y': 't', 'y': 't', 't': 't'})
SHADE = []
ORPHAN_OK = (('W', 'K'), ('W', 'O'), ('W', 'B'), ('W', 'C'), ('W', 'D'), ('A', 'B'), ('A', 'C'),
             ('C', 'D'), ('t', 'y'), ('g', 'y'), ('G', 'y'), ('g', 'k'),
             ('Y', 'y'), ('Y', 't'), ('p', 'P'), ('P', 'p'), ('F', 'O'),
             # food flakes and coins, night bubbles, water seen through a notch in a fin or tail
             ('F', 'y'), ('y', 'F'), ('W', 'F'), ('D', 'N'), ('D', 'Q'), ('D', 'M'),
             ('B', 'K'), ('C', 'K'), ('D', 'K'))

ACCESSORIES = ()
NO_ACCESSORY = ()
wearing = None

# --- the tank ------------------------------------------------------------------------------
W_, H_ = CANVAS
TANK_LEFT, TANK_RIGHT = 8, 119          # outer edge of the glass walls
LID_TOP, LID_BOTTOM = 3, 9
WATER_LEFT, WATER_RIGHT = 10, 117       # inside the glass
SURFACE = 15                            # row of the water surface (the wave goes 1 row up)
SAND_TOP = 74                           # sand begins about here
TANK_BOTTOM = 83                        # last row of water and sand
STAND_TOP, STAND_BOTTOM = 84, 91


def blank(w, h):
    return [['.'] * w for _ in range(h)]


def rows_of(grid):
    return [''.join(r) for r in grid]


def wave(x, t, period=32):
    """0 or 1: the surface line bobs up a row in slow travelling humps."""
    return 1 if ((x * 4 + t * 48 // period) // 16) % 3 == 0 else 0


def water_tone(x, y, night=False):
    """Three depth bands with soft stepped borders."""
    ripple = (x // 7) % 2
    if y < 30 + ripple:
        tone = 'B'
    elif y < 54 - ripple:
        tone = 'C'
    else:
        tone = 'D'
    if night:
        tone = {'B': 'M', 'C': 'N', 'D': 'Q'}[tone]
    return tone


def sand_top(x):
    """Height of the sand: gentle dunes."""
    return SAND_TOP + (1 if (x // 11) % 3 == 1 else 0) - (1 if (x // 17) % 4 == 2 else 0)


def scene_back(t, period=32, night=False):
    """Glass, water with its surface and light rays, and sand: everything behind the decor."""
    g = blank(W_, H_)
    for y in range(SURFACE - 1, TANK_BOTTOM + 1):
        for x in range(WATER_LEFT, WATER_RIGHT + 1):
            if y < SURFACE - wave(x, t, period):
                continue
            if y == SURFACE - wave(x, t, period) or (y == SURFACE and wave(x, t, period)):
                g[y][x] = 'D' if night else 'A'
                continue
            g[y][x] = water_tone(x, y, night)
    if not night:
        rays(g, t, period)
    for x in range(WATER_LEFT, WATER_RIGHT + 1):
        top = sand_top(x)
        for y in range(top, TANK_BOTTOM + 1):
            g[y][x] = 'Y' if y == top else 't' if y >= TANK_BOTTOM - 1 or (x * 7 + y * 3) % 23 == 0 else 'y'
    pebbles(g)
    return g


RAYS = ((26, 6), (58, 5), (88, 6))      # (x at the surface, width)


def rays(g, t, period):
    """Light rays from the lamp: lighter bands sloping down to the right, swaying a pixel or two."""
    sway = (0, 1, 1, 2, 2, 1, 1, 0, 0, -1, -1, -2, -2, -1, -1, 0)[(t * 16 // period) % 16]
    for index, (x0, width) in enumerate(RAYS):
        s = sway if index % 2 == 0 else -sway
        for y in range(SURFACE + 1, 60):
            fade = (y - SURFACE) // 14            # the ray narrows with depth
            for dx in range(max(1, width - fade)):
                x = x0 + s + (y - SURFACE) // 2 + dx
                if WATER_LEFT <= x <= WATER_RIGHT:
                    c = g[y][x]
                    g[y][x] = {'B': 'A', 'C': 'B', 'D': 'C'}.get(c, c)


PEBBLES = ((17, 'gG'), (45, 'kg'), (69, 'Gg'), (73, 'gk'), (96, 'gG'), (113, 'kg'))


def pebbles(g):
    for x, colours in PEBBLES:
        y = sand_top(x)
        g[y][x], g[y][x + 1] = colours[0], colours[1]
        g[y + 1][x] = 'k'


def tank_frame(night=False):
    """Walls, lid with its lamp and the stand: one shape, so the outline runs round all of it and
    along the inside of the glass."""
    g = blank(W_, H_)
    for y in range(LID_BOTTOM + 1, TANK_BOTTOM + 1):
        g[y][TANK_LEFT], g[y][TANK_LEFT + 1] = 'G', 'g'
        g[y][TANK_RIGHT - 1], g[y][TANK_RIGHT] = 'G', 'g'
    for y in range(LID_TOP, LID_BOTTOM + 1):
        for x in range(TANK_LEFT - 2, TANK_RIGHT + 3):
            g[y][x] = 'g' if y == LID_TOP else 'k'
    lamp = 'g' if night else 'W'
    for x in range(30, 98):
        g[LID_BOTTOM - 2][x] = lamp
        g[LID_BOTTOM - 1][x] = 'g' if night else 'F'
    for y in range(STAND_TOP, STAND_BOTTOM + 1):
        for x in range(TANK_LEFT - 2, TANK_RIGHT + 3):
            g[y][x] = 'g' if y == STAND_TOP else 'k'
    for x in range(TANK_LEFT + 6, TANK_RIGHT - 5, 4):
        g[STAND_TOP + 4][x] = 'g'            # a row of rivets
    return rows_of(g)


def glass_shine():
    """Two short streaks on the glass near the top left and one at the right."""
    g = blank(W_, H_)
    for x, y, n in ((12, 19, 3), (12, 23, 1), (114, 60, 2)):
        for i in range(n):
            g[y + i][x] = 'W'
    return rows_of(g)


def ground_shadow():
    g = blank(W_, H_)
    for y, inset in ((STAND_BOTTOM + 2, 3), (STAND_BOTTOM + 3, 0), (STAND_BOTTOM + 4, 3)):
        for x in range(TANK_LEFT - 2 + inset, TANK_RIGHT + 3 - inset):
            g[y][x] = 'S'
    return rows_of(g)


# --- shapes on the pixel grid ---------------------------------------------------------------

def polygon(points, w, h):
    """Mask of a polygon (pixel centres inside)."""
    mask = [[False] * w for _ in range(h)]
    for y in range(h):
        for x in range(w):
            px, py = x + 0.5, y + 0.5
            inside = False
            j = len(points) - 1
            for i in range(len(points)):
                xi, yi = points[i]
                xj, yj = points[j]
                if (yi > py) != (yj > py) and px < (xj - xi) * (py - yi) / (yj - yi) + xi:
                    inside = not inside
                j = i
            mask[y][x] = inside
    return mask


def ellipse(cx, cy, rx, ry, w, h):
    return [[((x + 0.5 - cx) / rx) ** 2 + ((y + 0.5 - cy) / ry) ** 2 <= 1 for x in range(w)] for y in range(h)]


def paint(grid, mask, light, base, dark):
    """Three tones, light from the top left: a highlight rim top-left, a shadow rim bottom-right."""
    h, w = len(mask), len(mask[0])

    def inside(x, y):
        return 0 <= x < w and 0 <= y < h and mask[y][x]

    for y in range(h):
        for x in range(w):
            if not mask[y][x]:
                continue
            if not inside(x + 1, y + 1) or not inside(x, y + 1):
                grid[y][x] = dark
            elif not inside(x - 1, y - 1) or not inside(x, y - 1):
                grid[y][x] = light
            else:
                grid[y][x] = base


# --- fish --------------------------------------------------------------------------------
# A fish is drawn facing left in its own box; the widget never mirrors it, the scene does.

FLAP = (0, 1, 2, 1, 0, -1, -2, -1)      # tail swing over the 8 swim frames


# Body of the classic goldfish, facing left: light rim top-left, shadow rim bottom-right,
# eye with a shine (W on K), a gill line (o) and a shine on the side
CLASSIC_BODY = part('''
    ....FFFFFF....
    ..FFOOOOOOOO..
    .FOOOOOOOOOOO.
    FOWKOoOFFOOOOO
    FOKKOoOOFOOOOO
    FOOOOoOOOOOOOo
    .OOOOOOOOOOOOo
    .oOOOOOOOOOOo.
    ..ooOOOOOOoo..
    ....oooooo....
''')
CLASSIC_BLINK = [r.replace('W', 'o').replace('K', 'O') if i == 3 else r.replace('KK', 'oo') if i == 4 else r
                 for i, r in enumerate(CLASSIC_BODY)]
CLASSIC_GILL = [r[:5] + ('O' if 3 <= i <= 5 else r[5]) + r[6:] if i in (3, 4, 5) else r
                for i, r in enumerate(CLASSIC_BODY)]


def flat(grid, mask, fill, rim):
    """A fin: flat light fill with a darker rim along the bottom right, for its shape."""
    h, w = len(mask), len(mask[0])
    for y in range(h):
        for x in range(w):
            if mask[y][x] and grid[y][x] == '.':
                edge = not (x + 1 < w and y + 1 < h and mask[y + 1][x + 1])
                grid[y][x] = rim if edge else fill


def classic(frame, blink=False, gill=False, turn=0):
    """Classic goldfish, 21x14: an egg-shaped body, a fan tail, a shine on the side.
    frame 0-7 swings the tail (the fins follow a frame later); turn 1 squeezes it mid-turn."""
    w, h = 21, 14
    g = blank(w, h)
    swing = FLAP[frame % 8]
    lag = FLAP[(frame - 1) % 8]
    body = CLASSIC_BLINK if blink else CLASSIC_GILL if gill else CLASSIC_BODY
    if turn:
        # Mid-turn the fish is seen almost head-on: a narrower body and a short tail
        body = [r[:4] + r[7:12] for r in body]
        stamp_grid(g, body, 2, 3)
        flat(g, polygon([(10.5, 5.5), (14.5, 3.0 + swing * 0.3), (14.5, 11.0 + swing * 0.3), (10.5, 9.5)], w, h), 'F', 'O')
        flat(g, polygon([(5.0, 3.6), (7.0, 1.0), (9.0, 3.6)], w, h), 'F', 'O')
        return rows_of(g)
    stamp_grid(g, body, 0, 3)
    # Fan tail: two lobes and a notch, swinging with the body
    flat(g, polygon([(12.6, 6.0), (19.6, 1.4 + swing * 0.7), (20.6, 2.8 + swing * 0.7),
                     (18.0, 7.6 + swing * 0.4), (20.6, 12.4 + swing * 0.7), (19.6, 13.6 + swing * 0.7),
                     (12.6, 9.4)], w, h), 'F', 'O')
    # Dorsal sail and the small fins under the belly; they trail a frame behind. The sail sits
    # on a darker line, so it doesn't melt into the light back
    sail = polygon([(4.8, 3.6), (6.8, 0.4 + abs(lag) * 0.3), (9.6, 0.6), (11.2, 3.6)], w, h)
    flat(g, sail, 'F', 'O')
    for y in range(h - 1):
        for x in range(w):
            if sail[y][x] and g[y + 1][x] in 'FO' and not sail[y + 1][x]:
                g[y][x] = 'O'
    flat(g, polygon([(5.2, 12.4), (5.6 - lag * 0.3, 14.0), (7.8, 12.4)], w, h), 'F', 'O')
    flat(g, polygon([(9.8, 12.0), (11.4 + lag * 0.2, 14.0), (12.0, 11.8)], w, h), 'F', 'O')
    # Pectoral fin on the side, fluttering
    for x, y in ((6, 9), (7, 9), (7, 10 + (1 if lag > 0 else 0)), (8, 10)):
        if g[y][x] not in '.':
            g[y][x] = 'F'
    return rows_of(g)


def stamp_grid(g, rows, x, y):
    for dy, row in enumerate(rows):
        for dx, c in enumerate(row):
            if c != '.':
                g[y + dy][x + dx] = c


def veil(frame, blink=False, gill=False, turn=0):
    """Red-and-white veiltail, 26x17: a round white body with red patches and a long veil of a
    tail that trails two frames behind the body, the long fins one frame behind."""
    w, h = 26, 17
    g = blank(w, h)
    swing = FLAP[(frame - 2) % 8]
    lag = FLAP[(frame - 1) % 8]
    body = VEIL_BLINK if blink else VEIL_GILL if gill else VEIL_BODY
    if turn:
        stamp_grid(g, [r[:4] + r[7:11] for r in body], 3, 4)
        flat(g, polygon([(10.0, 6.0), (15.0, 3.0 + swing * 0.4), (15.0, 13.0 + swing * 0.4), (10.0, 10.0)], w, h), 'W', 'G')
        flat(g, polygon([(5.0, 4.6), (7.0, 1.0), (9.0, 4.6)], w, h), 'R', 'o')
        return rows_of(g)
    stamp_grid(g, body, 0, 4)
    # The veil: two long, soft lobes; red at the root, white towards the tips
    veil_mask = polygon([(11.4, 7.0), (17.0, 2.6 + swing * 0.8), (23.4, 1.4 + swing * 1.1),
                         (25.6, 4.6 + swing * 1.1), (21.0, 9.2 + swing * 0.5), (25.6, 13.4 + swing * 1.1),
                         (23.4, 16.6 + swing * 1.1), (17.0, 15.6 + swing * 0.8), (11.4, 11.4)], w, h)
    for y in range(h):
        for x in range(w):
            if veil_mask[y][x] and g[y][x] == '.':
                edge = not (x + 1 < w and y + 1 < h and veil_mask[y + 1][x + 1])
                ray = x < 19 and y in (round(6 + swing * 0.6), round(12 + swing * 0.6))
                g[y][x] = 'G' if edge else 'R' if x < 14 or ray else 'W'
    # Tall dorsal fin and long trailing belly fins
    sail = polygon([(4.4, 4.6), (6.4, 0.4 + abs(lag) * 0.4), (10.6, 1.2), (11.6, 4.6)], w, h)
    flat(g, sail, 'R', 'o')
    for y in range(h - 1):
        for x in range(w):
            if sail[y][x] and g[y + 1][x] in 'WRG' and not sail[y + 1][x]:
                g[y][x] = 'o'
    flat(g, polygon([(5.0, 13.4), (6.6 - lag * 0.5, 16.8), (8.4, 13.4)], w, h), 'W', 'G')
    flat(g, polygon([(9.2, 13.2), (11.6 - lag * 0.4, 16.6), (11.8, 12.8)], w, h), 'W', 'G')
    return rows_of(g)


# White body with red patches; the shadow of white is the pale glass grey G
VEIL_BODY = part('''
    ....RRRRRR...
    ..RRRRRWWWRR.
    .RRRRWWWWWRRR
    WWWRRWWWWRRRR
    WWKKWWWWWRRRo
    WWKKWGWWWRRRo
    WWWWWGWWRRRRo
    .WWWWWWWWRRo.
    .GWWWWWWWWGo.
    ..GGWWWWWGG..
    ....GGGGGG...
''')
VEIL_BLINK = [r.replace('KK', 'GG') if i == 4 else r.replace('KK', 'WW') if i == 5 else r
              for i, r in enumerate(VEIL_BODY)]
VEIL_GILL = [r[:5] + 'W' + r[6:] if i in (5, 6) else r for i, r in enumerate(VEIL_BODY)]


COMET_BODY = part('''
    ...FFFFF..
    .FWKOOOOOO
    FOKKoOFFOO
    FOOOoOOOOo
    .oOOOOOOo.
    ...oooo...
''')
COMET_BLINK = [r.replace('WK', 'OO') if i == 1 else r.replace('KK', 'oo') if i == 2 else r
               for i, r in enumerate(COMET_BODY)]
COMET_GILL = [r[:4] + 'O' + r[5:] if i in (2, 3) else r for i, r in enumerate(COMET_BODY)]


def comet(frame, blink=False, gill=False, turn=0):
    """Small comet, 20x10: a slim gold body and a long, deeply forked tail that whips quickly."""
    w, h = 20, 10
    g = blank(w, h)
    swing = FLAP[frame % 8]
    lag = FLAP[(frame - 1) % 8]
    body = COMET_BLINK if blink else COMET_GILL if gill else COMET_BODY
    if turn:
        stamp_grid(g, [r[:4] + r[6:9] for r in body], 2, 2)
        flat(g, polygon([(8.0, 3.0), (11.0, 1.0 + swing * 0.3), (11.0, 8.0 + swing * 0.3), (8.0, 6.0)], w, h), 'F', 'O')
        return rows_of(g)
    stamp_grid(g, body, 0, 2)
    # Forked tail: two thin lobes far apart
    flat(g, polygon([(9.4, 3.0), (19.8, 0.0 + swing * 0.8), (19.6, 1.8 + swing * 0.8), (13.6, 5.0 + swing * 0.3),
                     (19.6, 8.2 + swing * 0.8), (19.8, 10.0 + swing * 0.8), (9.4, 6.4)], w, h), 'F', 'O')
    flat(g, polygon([(4.0, 2.2), (6.0, 0.4 + abs(lag) * 0.3), (8.4, 2.2)], w, h), 'F', 'O')
    flat(g, polygon([(5.0, 7.6), (6.0 - lag * 0.3, 9.6), (7.4, 7.6)], w, h), 'F', 'O')
    return rows_of(g)


FISH = {
    # name: (drawing, width, height); speeds and routes are set per animation below
    'veil': (veil, 26, 17),
    'classic': (classic, 21, 14),
    'comet': (comet, 20, 10),
}


# --- decor ---------------------------------------------------------------------------------

CASTLE = part('''
    ..gg..gg..gg..gg..gg....
    ..gGG.gGG.gGG.gGG.gGk...
    ..gGgggGgggGgggGgggGk...
    ..gGggggggggggggggggk...
    ..gGgggkkggggggkkgggk...
    ..gGggkDDkggggkDDkggk...
    ..gGggkDDkggggkDDkggk...
    ..gGggkkkkggggkkkkggk...
    ..gGgggggggggggggggg....
    ..gGggggggpgggggggk.....
    ..gGgggggppgggggggk.....
    ..gGgggkkkkkkgggggk.....
    ..gGggkDDDDDDkggggk.....
    ..gGgkDDDDDDDDkgggk.....
    ..gGgkDDDDDDDDkgggkk....
    ..gGgkDDDDDDDDkggggk....
    ..gGgkDDDDDDDDkggggk....
    .ggGgkDDDDDDDDkggggk....
    .gGGgkDDDDDDDDkgggggk...
    ggGggkDDDDDDDDkggggggk..
''')
# Night: the windows and the gate in the dark water colour
CASTLE_NIGHT = [r.replace('D', 'Q') for r in CASTLE]

# The chest: a lid and a box, so the lid can lift
CHEST_LID = part('''
    .tyyyyyyyyyt.
    tyFyyyyyyyFyt
    tyFyyyyyyyFyt
    ttFttttttFttt
''')
CHEST_BOX = part('''
    tyFyyyFyyyFyt
    tyFyyFFFyyFyt
    tyFyyyFyyyFyt
    tyFyyyyyyyFyt
    ttFttttttFttt
''')
# Open: gold coins and a gleam inside, the lid thrown back behind the box
CHEST_GOLD = part('''
    tFFWFFFFWFFFt
    tFWFFFFFFFWFt
''')
CHEST_LID_BACK = part('''
    .tyyyyyyyyyt.
    tyFyyyyyyyFyt
    tkkkkkkkkkkkt
    tkkkkkkkkkkkt
''')

SHELL = part('''
    ..WW..
    .WGWG.
    WGWGWG
    GWGWGk
''')

STARFISH = part('''
    ...o...
    ...O...
    oOOFOOo
    .oOOOo.
    .oO.Oo.
    o.....o
''')

HEART = part('''
    .R.R.
    RWRRR
    RRRRR
    .RRR.
    ..R..
''')
STAR = part('''
    ..F..
    .FWF.
    FWWWF
    .FWF.
    ..F..
''')
SPARK = part('''
    .F.
    FWF
    .F.
''')
Z_SMALL = part('''
    GGG
    .G.
    GGG
''')
Z_BIG = part('''
    GGGG
    ..G.
    .G..
    GGGG
''')


def seaweed(height, t, phase, period=32):
    """A long ribbon of seaweed, 7 wide: it sways more towards the top."""
    w = 7
    g = blank(w, height)
    sway = FLAP[((t * 8 // period) + phase) % 8]
    for y in range(height):
        bend = round(sway * (height - y) / height * 0.9)
        wiggle = 1 if (y // 4) % 2 else 0
        x = 2 + bend + wiggle
        for dx, c in ((0, 'P'), (1, 'p'), (2, 'q')):
            if 0 <= x + dx < w:
                g[y][x + dx] = c
    return rows_of(g)


def bush(t, phase, period=32):
    """A round bush of short leaves, 18x13; the tips shiver."""
    w, h = 18, 13
    g = blank(w, h)
    sway = FLAP[((t * 8 // period) + phase) % 8]
    for i, (bx, top) in enumerate(((2, 4), (5, 1), (8, 0), (11, 2), (14, 5))):
        lean = round(sway * 0.5) if i % 2 == 0 else -round(sway * 0.5)
        mask = polygon([(bx + 1.5, h), (bx + 0.2 + lean, top + 2), (bx + 1.5 + lean, top),
                        (bx + 3.0 + lean, top + 2), (bx + 3.2, h)], w, h)
        paint(g, mask, 'P', 'p', 'q')
    return rows_of(g)


def grass(t, phase, period=32):
    sway = FLAP[((t * 8 // period) + phase) % 8]
    lean = 1 if sway > 0 else 0
    return [f'.{"p" if lean else "."}.p', f'p{"." if lean else "p"}.p', 'p.pq', 'qpqq']


AERATOR_X = 14


def aerator_tube():
    """The air tube down the back wall to an air stone in the sand."""
    height = sand_top(AERATOR_X) - SURFACE + 1
    g = blank(4, height)
    for y in range(height - 2):
        g[y][1], g[y][2] = 'G', 'g'
    for x in range(4):
        g[height - 2][x] = 'g'
        g[height - 1][x] = 'k'
    return rows_of(g)


def aerator(g, t, period=32, night=False):
    """Bubbles from the air stone: four in the water at any time, rising to the surface."""
    x0 = AERATOR_X
    rise = sand_top(x0) - 3 - (SURFACE + 2)
    for k in range(4):
        age = (t * 4 + k * period) % (period * 4) / (period * 4)   # 0..1 over the loop
        y = sand_top(x0) - 3 - round(age * rise)
        x = x0 + 4 + (1 if (round(age * 24) // 3) % 2 else 0)
        bubble(g, x, y, (k % 3) + 1, night)


def bubble(g, x, y, size, night=False):
    light = 'D' if night else 'A'
    if not (SURFACE + 1 <= y < SAND_TOP and WATER_LEFT <= x <= WATER_RIGHT - 2):
        return
    if size == 1:
        g[y][x] = light if night else 'W'
    elif size == 2:
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
            g[y + dy][x + dx] = light
        g[y][x] = 'W'
    else:
        for dx, dy in ((1, 0), (0, 1), (2, 1), (1, 2)):
            g[y + dy][x + dx] = light
        g[y][x + 1] = 'W'


CHEST_X = 24
CHEST_Y = SAND_TOP - len(CHEST_LID) - len(CHEST_BOX) + 3    # top of the closed lid
CASTLE_AT = (82, SAND_TOP - len(CASTLE) + 3)

# Places the fish keep out of: (x, y, width, height). The chest box includes room for its open lid.
OBSTACLES = {
    'aerator': (WATER_LEFT, SURFACE, AERATOR_X + 7 - WATER_LEFT, SAND_TOP - SURFACE),
    'chest': (CHEST_X, CHEST_Y - 4, len(CHEST_BOX[0]), len(CHEST_LID) + len(CHEST_BOX) + 4),
    'bush': (46, SAND_TOP - 10, 18, 13),
    'short seaweed': (72, SAND_TOP - 32, 7, 34),
    'castle': (CASTLE_AT[0], CASTLE_AT[1], len(CASTLE[0]), len(CASTLE)),
    'long seaweed': (106, SAND_TOP - 44, 7, 46),
}


def chest_layers(state):
    """closed, ajar (lid lifted, a glow in the gap) or open (lid back, gold showing)."""
    box_y = CHEST_Y + len(CHEST_LID)
    if state == 'open':
        return [L(CHEST_LID_BACK, CHEST_X, box_y - len(CHEST_LID_BACK)), L(CHEST_BOX, CHEST_X, box_y),
                L(CHEST_GOLD, CHEST_X, box_y, ring=False)]
    if state == 'ajar':
        glow = ['.' + 'F' * 11 + '.', '.' + 'FFWFFFFFWFF' + '.']
        return [L(CHEST_BOX, CHEST_X, box_y), L(glow, CHEST_X, box_y - 1, ring=False),
                L(CHEST_LID, CHEST_X, CHEST_Y - 2)]
    return [L(CHEST_BOX, CHEST_X, box_y), L(CHEST_LID, CHEST_X, CHEST_Y)]


def decor_layers(t, period, night=False, chest='closed'):
    """Decor in front of the water, each with the K outline."""
    return [
        L(aerator_tube(), AERATOR_X - 1, SURFACE + 1),
        L(seaweed(46, t, 0, period), 106, SAND_TOP - 44),
        L(seaweed(34, t, 3, period), 72, SAND_TOP - 32),
        L(CASTLE_NIGHT if night else CASTLE, *CASTLE_AT),
        L(bush(t, 2, period), 46, SAND_TOP - 10),
    ] + chest_layers(chest) + [
        L(grass(t, 1, period), 40, SAND_TOP - 2),
        L(grass(t, 5, period), 100, SAND_TOP - 2),
        L(SHELL, 64, SAND_TOP),
        L(STARFISH, 54, SAND_TOP - 1),
    ]


# --- routes ----------------------------------------------------------------------------------
# A fish position is (x, y, facing_right, turning). Routes are deterministic. Nothing is random.
#
# Smooth motion: every animation starts and ends with the fish at home (HOME, where they also
# sleep), so one animation follows another without a jump, and no fish moves more than MAX_STEP
# pixels from one frame to the next, the loop's seam included. Each keeps to its own part of the
# tank (the veiltail the top, the classic the middle on the left, the comet the right over the
# castle), so they never swim into one another (check() makes sure of all three).

HOME = {'veil': (46, 22, 'L'), 'classic': (24, 40, 'R'), 'comet': (84, 40, 'L')}
MAX_STEP = 4


def keyframes(t, keys):
    """keys: [(frame, x, y, 'L' or 'R'), ...] from frame 0 to the loop length, where the last key
    repeats the first. Positions move in straight lines between keys; the frame where the fish
    changes side shows the turn pose."""
    def at(frame):
        for (t1, x1, y1, f1), (t2, x2, y2, _) in zip(keys, keys[1:]):
            if t1 <= frame < t2:
                k = (frame - t1) / (t2 - t1)
                return x1 + (x2 - x1) * k, y1 + (y2 - y1) * k, f1 == 'R'
        _, x, y, f = keys[-1]
        return x, y, f == 'R'

    x, y, right = at(t)
    _, _, before = at((t - 1) % keys[-1][0])
    return round(x), round(y), right, before != right


def legs_route(period, name, legs):
    """Every frame of a fish's loop from its legs, starting at home in frame 0:
      ('to', x, y[, frames])   swims there facing the way it goes (in as many frames, or in a share
                               of the frames left over, so that all such legs go at one pace)
      ('drift', x, y, frames)  drifts there without turning
      ('face', 'L' or 'R')     turns round on the spot (one frame, the turn pose)
      ('hold', frames)         hovers where it is
    The last leg brings it home, facing its home way. Returns [(x, y, facing_right, turning)]."""
    x, y, side = HOME[name]
    right = side == 'R'
    fixed = 0
    free_length = 0.0
    for leg in legs:
        if leg[0] == 'face':
            fixed += 1
        elif leg[0] == 'hold':
            fixed += leg[1]
        elif leg[0] == 'drift' or len(leg) > 3:
            fixed += leg[3]
    free = period - 1 - fixed
    # Lengths of the legs that share the free frames
    px, py = x, y
    for leg in legs:
        if leg[0] in ('to', 'drift'):
            if leg[0] == 'to' and len(leg) == 3:
                free_length += ((leg[1] - px) ** 2 + (leg[2] - py) ** 2) ** 0.5
            px, py = leg[1], leg[2]
    frames = [(x, y, right, False)]
    used = 0
    for leg in legs:
        if leg[0] == 'face':
            right = leg[1] == 'R'
            frames.append((x, y, right, True))
        elif leg[0] == 'hold':
            frames += [(x, y, right, False)] * leg[1]
        else:
            tx, ty = leg[1], leg[2]
            if len(leg) > 3:
                n = leg[3]
            else:
                length = ((tx - x) ** 2 + (ty - y) ** 2) ** 0.5
                share = free * length / free_length if free_length else 0
                n = max(1, round(used + share) - round(used))
                used += share
            if leg[0] == 'to' and tx != x:
                assert (tx > x) == right, f'{name}: swims backwards to {tx, ty}; turn first'
            for k in range(1, n + 1):
                frames.append((x + (tx - x) * k / n, y + (ty - y) * k / n, right, False))
            x, y = tx, ty
    assert len(frames) == period, f'{name}: {len(frames)} frames, not {period}'
    assert (x, y, 'R' if right else 'L') == HOME[name], f'{name} ends at {x, y}, not at home'
    return even_steps(frames)


def even_steps(frames):
    """Rounds the positions to whole pixels, each one up or down so that the steps on both sides
    of it stay as short as they can (plain rounding can turn a 3.3 px step into a 4.1 px one)."""
    out = [[round(fx), round(fy), r, turning] for fx, fy, r, turning in frames]
    n = len(out)

    def step(i, j):
        return ((out[i][0] - out[j][0]) ** 2 + (out[i][1] - out[j][1]) ** 2) ** 0.5

    for _ in range(3):
        for i in range(n):
            fx, fy = frames[i][0], frames[i][1]
            if fx == int(fx) and fy == int(fy):
                continue
            best = None
            for x in {math.floor(fx), math.ceil(fx)}:
                for y in {math.floor(fy), math.ceil(fy)}:
                    out[i][0], out[i][1] = x, y
                    worst = max(step(i - 1, i), step(i, (i + 1) % n))
                    if best is None or worst < best[0] - 1e-9:
                        best = (worst, x, y)
            out[i][0], out[i][1] = best[1], best[2]
    return [tuple(f) for f in out]


# --- frames ---# --- frames ----------------------------------------------------------------------------------

def frame(t, period, fish=(), night=False, chest='closed', effects=(), back_effects=None):
    """One frame: the water as a base layer, decor, fish, effects, then the glass and the frame."""
    g = scene_back(t, period, night)
    aerator(g, t, period, night)
    if back_effects:
        back_effects(g)
    layers = [L(ground_shadow(), 0, 0, ring=False), L(rows_of(g), 0, 0, ring=False)]
    layers += decor_layers(t, period, night, chest)
    layers += list(fish)
    layers += list(effects)
    layers.append(L(tank_frame(night), 0, 0))
    layers.append(L(glass_shine(), 0, 0, ring=False))
    return layers


# Per animation: frames, interval, and for every fish its legs from home and back (or keyframes
# while asleep), plus the speed of the tail (tail frames per frame) and when it blinks.
ROUTES = {
    'idle': dict(period=32, interval=140, tail_rate=2, fish={
        # each in its own part of the tank: the veiltail drifts along the top, the classic swims
        # out to the middle and back, the comet rises over the castle and looks round
        'veil': [('to', 30, 20), ('face', 'R'), ('to', 56, 19), ('hold', 2), ('face', 'L'), ('to', 46, 22)],
        'classic': [('to', 46, 39), ('hold', 4), ('face', 'L'), ('to', 24, 40), ('face', 'R')],
        'comet': [('to', 84, 20), ('face', 'R'), ('to', 96, 19), ('hold', 2), ('face', 'L'), ('to', 84, 22),
                  ('to', 84, 40)],
    }),
    'walk': dict(period=32, interval=150, tail_rate=2, fish={
        # a longer swim, each in its part of the tank
        'veil': [('to', 22, 22), ('face', 'R'), ('to', 56, 17), ('hold', 1), ('face', 'L'), ('to', 46, 22)],
        'classic': [('to', 48, 44), ('to', 50, 50), ('face', 'L'), ('to', 26, 50), ('to', 24, 40), ('face', 'R')],
        'comet': [('to', 84, 18), ('face', 'R'), ('to', 97, 17), ('hold', 1), ('face', 'L'), ('to', 85, 19),
                  ('to', 84, 47), ('to', 84, 40)],
    }),
    'sleep': dict(period=12, interval=200, night=True, tail_rate=2 / 3, fish={
        # resting at home, rising and sinking a pixel
        'veil': ('keys', [(0, 46, 22, 'L'), (6, 46, 23, 'L'), (12, 46, 22, 'L')]),
        'classic': ('keys', [(0, 24, 40, 'R'), (6, 24, 41, 'R'), (12, 24, 40, 'R')]),
        'comet': ('keys', [(0, 84, 40, 'L'), (6, 84, 41, 'L'), (12, 84, 40, 'L')]),
    }),
    'happy': dict(period=16, interval=100, tail_rate=2, fish={
        # a quick turn, up towards the surface, a moment there, and back down home
        'veil': [('face', 'R'), ('to', 52, 17), ('hold', 1), ('face', 'L'), ('to', 46, 22)],
        'classic': [('face', 'L'), ('to', 24, 24), ('hold', 1), ('face', 'R'), ('to', 24, 40)],
        'comet': [('face', 'R'), ('to', 84, 18), ('hold', 1), ('face', 'L'), ('to', 84, 40)],
    }),
    'eat': dict(period=24, interval=120, tail_rate=1, fish={
        # mouth meets the flake on its catch frame (see FLAKES): facing left the mouth is the
        # left edge of the box, facing right the right edge
        'classic': [('face', 'L'), ('drift', 26, 21, 9), ('hold', 2), ('drift', 24, 40, 10), ('face', 'R')],
        'veil': [('drift', 52, 20, 12), ('hold', 2), ('drift', 46, 22, 9)],
        'comet': [('face', 'R'), ('drift', 79, 23, 13), ('hold', 2), ('to', 84, 40), ('face', 'L')],
    }),
    'treasure': dict(period=24, interval=120, tail_rate=1, fish={
        # They come to look at the chest: the classic just above it, the veiltail from higher up,
        # the comet from over the seaweed; everyone looks at it
        'classic': [('drift', 24, 48, 5), ('hold', 13), ('drift', 24, 40, 5)],
        'veil': [('drift', 46, 34, 5), ('hold', 13), ('drift', 46, 22, 5)],
        'comet': [('to', 84, 26), ('to', 70, 24), ('hold', 3), ('face', 'R'), ('to', 84, 27), ('face', 'L'),
                  ('to', 84, 40)],
    }),
}
_routes = {}


def fish_at(anim, name, t):
    """(x, y, facing_right, turning, swim frame) of a fish in a frame of an animation."""
    cfg = ROUTES[anim]
    period = cfg['period']
    route = cfg['fish'][name]
    if route[0] == 'keys':
        x, y, right, turning = keyframes(t % period, route[1])
    else:
        if (anim, name) not in _routes:
            _routes[(anim, name)] = legs_route(period, name, route)
        x, y, right, turning = _routes[(anim, name)][t % period]
    return x, y, right, turning, int(t * cfg.get('tail_rate', 1))


def fish_layers(anim, t, blink_at=(), extra=None):
    out = []
    for i, name in enumerate(FISH):
        draw, w, h = FISH[name]
        x, y, right, turning, swim = fish_at(anim, name, t)
        rows = draw(swim + i * 3, blink=(t + i * 5) % ROUTES[anim]['period'] in blink_at,
                    gill=((t + i) // 3) % 2 == 0, turn=1 if turning else 0)
        out.append(L(rows, x, y, flip_h=right))
    return out


def mouth(anim, name, t):
    """Where the fish's mouth is: the front edge, middle height."""
    draw, w, h = FISH[name]
    x, y, right, _, _ = fish_at(anim, name, t)
    return (x + w - 1 if right else x), y + h // 2


def build(anim, effects=None, chest_at=None, back=None):
    cfg = ROUTES[anim]
    period = cfg['period']
    blink = (5, period // 2 + 3)
    out = []
    for t in range(period):
        fx = effects(t) if effects else ()
        chest = chest_at(t) if chest_at else 'closed'
        out.append(frame(t, period, fish_layers(anim, t, blink), cfg.get('night', False), chest, fx,
                         (lambda g, t=t: back(g, t)) if back else None))
    return out


def idle_frames():
    return build('idle')


def walk_frames():
    return build('walk')


def sleep_frames():
    """Lamp off, night water; the fish rest near the bottom, a Z drifts up from each now and then."""
    def zs(t):
        layers = []
        for i, name in enumerate(FISH):
            age = (t + i * 4) % 12
            if age < 8:
                mx, my = mouth('sleep', name, 0)
                layers.append(L(Z_SMALL if age < 4 else Z_BIG, mx - 2 + age // 3, my - 6 - age * 2))
        return layers
    return build('sleep', zs)


def happy_frames():
    """A quick turn, a dash to the surface, hearts and stars; bubbles trail behind."""
    def joy(t):
        layers = []
        for i, name in enumerate(FISH):
            age = t - 4 - i
            if 0 <= age < 8:
                mx, my = mouth('happy', name, 4 + i)
                layers.append(L(HEART if i != 1 else STAR, mx - 2 + (age % 2), max(LID_BOTTOM + 2, my - 6 - age)))
        if 6 <= t < 12:
            layers.append(L(SPARK, 92 - (t % 3), 24 - t))
        return layers

    def bubbles(g, t):
        for i, name in enumerate(FISH):
            if 2 <= t < 12:
                x, y, right, _, _ = fish_at('happy', name, t)
                w = FISH[name][1]
                bubble(g, x + (-3 if right else w + 1), y + 4 + (t % 3), 1 + (t + i) % 2)
    return build('happy', joy, back=bubbles)


FLAKES = (('classic', 26, 0, 10), ('veil', 52, 2, 12), ('comet', 98, 4, 14))   # owner, x, drop, catch
FLAKE = part('''
    YFy
    .t.
''')


def flake_y(drop, t):
    """Flakes sink a pixel a frame from just under the surface."""
    return SURFACE + 3 + (t - drop)


def eat_frames():
    """Flakes drift down from the top; each fish rises to its own flake and snaps it up."""
    def back(g, t):
        for owner, x, drop, catch in FLAKES:
            if drop <= t < drop + 2:                    # a ring of ripples where it lands
                for dx in (-2, -1, 2, 3):
                    g[SURFACE][x + dx] = 'W'
            if catch <= t < catch + 3:                  # a bubble from the snap
                bubble(g, x + (-3 if owner == 'comet' else 1), flake_y(drop, catch) - 3 - (t - catch) * 2, 2)

    def flakes(t):
        return [L(FLAKE, x, flake_y(drop, t), ring=False)
                for owner, x, drop, catch in FLAKES if drop <= t < catch]
    return build('eat', flakes, back=back)


def treasure_state(t):
    if 4 <= t < 6 or 16 <= t < 18:
        return 'ajar'
    if 6 <= t < 16:
        return 'open'
    return 'closed'


def treasure_frames():
    """The chest lifts its lid, a burst of bubbles and a gleam come out, the fish come to look."""
    def back(g, t):
        if 6 <= t < 18:
            # A burst of bubbles of every size, spreading as they rise
            for k in range(12):
                age = t - 6 - k // 3
                if age < 0:
                    continue
                spread = (k % 5) - 2
                x = CHEST_X + 5 + spread * (1 + age // 2) + (1 if (age + k) % 4 < 2 else 0)
                y = CHEST_Y - 1 - age * 3 - (k % 3) * 2
                bubble(g, x, y, (k % 3) + 1)

    def glints(t):
        layers = []
        if 6 <= t < 15:
            layers.append(L(STAR if t % 4 < 2 else SPARK, CHEST_X + 9 + (t % 3), CHEST_Y - 8 - (t - 6)))
        if 8 <= t < 14:
            layers.append(L(SPARK, CHEST_X - 3, CHEST_Y - 4 - (t - 8)))
        return layers
    return build('treasure', glints, treasure_state, back)


def check(anims):
    """Every fish in every frame stays in the water, clear of the glass, the sand and the decor; it
    is at home in the first frame of every animation and moves at most MAX_STEP pixels a frame."""
    problems = []
    for anim, cfg in ROUTES.items():
        for name in FISH:
            x, y, right, turning, _ = fish_at(anim, name, 0)
            if (x, y, 'R' if right else 'L') != HOME[name] or turning:
                problems.append(f'{anim} 0: {name} is not at home')
        for t in range(cfg['period']):
            for name, (draw, w, h) in FISH.items():
                x, y, right, turning, _ = fish_at(anim, name, t)
                nx, ny = fish_at(anim, name, (t + 1) % cfg['period'])[:2]
                step = ((nx - x) ** 2 + (ny - y) ** 2) ** 0.5
                if step > MAX_STEP:
                    problems.append(f'{anim} {t}: {name} moves {step:.1f} px')
                for other, (_, ow, oh) in FISH.items():
                    if other > name:
                        ox, oy = fish_at(anim, other, t)[:2]
                        if x < ox + ow and ox < x + w and y < oy + oh and oy < y + h:
                            problems.append(f'{anim} {t}: {name} swims into the {other}')
                if x < WATER_LEFT + 1 or x + w > WATER_RIGHT or y < SURFACE + 1 or y + h > SAND_TOP - 1:
                    problems.append(f'{anim} {t}: {name} leaves the water at ({x}, {y})')
                for obstacle, (ox, oy, ow, oh) in OBSTACLES.items():
                    if x < ox + ow and ox < x + w and y < oy + oh and oy < y + h:
                        problems.append(f'{anim} {t}: {name} swims into the {obstacle}')
    return problems


ANIMATIONS = {
    'sleep': {'interval': ROUTES['sleep']['interval'], 'frames': sleep_frames},
    'happy': {'interval': ROUTES['happy']['interval'], 'frames': happy_frames},
    'idle': {'interval': ROUTES['idle']['interval'], 'frames': idle_frames},
    'eat': {'interval': ROUTES['eat']['interval'], 'frames': eat_frames},
    'treasure': {'interval': ROUTES['treasure']['interval'], 'frames': treasure_frames},
    'swim': {'interval': ROUTES['walk']['interval'], 'frames': walk_frames},
}
