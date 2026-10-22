"""Parrot: a bright, chatty macaw. Faces left on a 64x64 canvas, feet on row 59 over a ground
shadow.

Drawn from text matrices: big shapes carry their own three tones (light from the top left), the
engine adds the one-pixel outline. Wings and tail feathers are placed by their root point, so they
stay attached in every pose. Key poses are held by repeating a frame, since the widget plays every
frame for the same time.

Flight (takeoff, fly, land) uses a body stretched out and tilted, spread wings at a few angles
and the tail streaming behind; the widget moves the frame along the flight path.

Smooth motion: its waddle moves the planted feet by exactly WALK_SHIFTS (PetSpec.PARROT_GAIT); it
turns round through looking at you and facing you; it fluffs up before it tucks its head in to
sleep, lifts a foot before it holds a seed up. Taking off, its head and body rise evenly while the
frame starts to climb, and the legs fold up over three frames; landing is the same backwards.
"""
import math

from sprite_gen import L, part

PALETTE = {
    'K': '#1f2133',  # outline (deep blue-grey, shared by all pixel pets); pupils
    'R': '#e0473e',  # scarlet plumage
    'q': '#f27a62',  # red highlight, feather lines on the face
    'r': '#a8302d',  # red shadow, chest scales
    'Y': '#f7c63c',  # yellow wing band, iris, stars, notes
    'o': '#d9952a',  # yellow shadow
    'U': '#3d72d9',  # blue flight feathers, bow
    'p': '#6f9ff0',  # blue highlight
    'u': '#284c9e',  # blue shadow, feather separations
    'W': '#fbf6ec',  # white face mask, shines, "Z", hat trim
    'M': '#e3dccb',  # pale upper beak
    'g': '#9a96a3',  # beak shadow, feet
    'm': '#2e2a36',  # beak tip, lower beak, witch hat
    'S': '#3b3d52',  # ground shadow
    'L': '#f6a531',  # maple leaf (the maple park's colours)
    'E': '#e8702a',  # maple leaf, middle tone
    'e': '#b9452a',  # maple leaf shadow, stem
}
MAX_COLOURS = 17
OUTLINE = 'K'
LINE_OF = {'R': 'r', 'q': 'r', 'r': 'r', 'Y': 'o', 'o': 'o', 'U': 'u', 'p': 'u', 'u': 'u', 'W': 'g',
           'M': 'g', 'g': 'm', 'm': 'm', 'L': 'e', 'E': 'e', 'e': 'e'}
SHADE = []                         # every part carries its own tones
# Single pixels that are details on purpose: (colour, a neighbour colour it must touch)
ORPHAN_OK = (('W', 'K'), ('W', 'M'), ('q', 'W'), ('M', 'm'), ('Y', 'K'), ('e', 'E'))

FAR = {'g': 'm', 'R': 'r', 'q': 'R'}
# How far its planted feet move back in each walk frame (art pixels): PetSpec.PARROT_GAIT
WALK_SHIFTS = (0, 2, 2, 0, 0, 2, 2, 0)

# --- big shapes (generated with three tones) ----------------------------------------------

HEAD = part('''
{HEAD}
''')

# Leaning forward a little, chest scales in front
BODY = part('''
{BODY}
''')
# Feathers puffed up (a shake in the idle loop, asleep)
BODY_FLUFF = part('''
{BODY_FLUFF}
''')
# Seen from the front (turning round), with a folded wing on each side and the tail's tip below
BODY_FRONT = part('''
{BODY_FRONT}
''')
WING_FRONT = part('''
{WING_FRONT}
''')
WING_FRONT_ROOT = {WING_FRONT_ROOT}
TAIL_FRONT = part('''
{TAIL_FRONT}
''')
TAIL_FRONT_ROOT = {TAIL_FRONT_ROOT}

# Wings grow from the shoulder: red, then the yellow band, then blue flight feathers. Each comes
# with the position of its root inside the matrix.
WING_FOLD = part('''
{WING_FOLD}
''')
WING_FOLD_ROOT = {WING_FOLD_ROOT}
WING_OUT = part('''
{WING_OUT}
''')
WING_OUT_ROOT = {WING_OUT_ROOT}
WING_UP = part('''
{WING_UP}
''')
WING_UP_ROOT = {WING_UP_ROOT}
WINGS = {'fold': (WING_FOLD, WING_FOLD_ROOT), 'out': (WING_OUT, WING_OUT_ROOT), 'up': (WING_UP, WING_UP_ROOT)}

# The long tail: three feathers fanned from one root
TAIL_MAIN = part('''
{TAIL_MAIN}
''')
TAIL_MAIN_ROOT = {TAIL_MAIN_ROOT}
TAIL_LOW = part('''
{TAIL_LOW}
''')
TAIL_LOW_ROOT = {TAIL_LOW_ROOT}
TAIL_HIGH = part('''
{TAIL_HIGH}
''')
TAIL_HIGH_ROOT = {TAIL_HIGH_ROOT}

SHADOW_STAND = part('''
{SHADOW_STAND}
''')
SHADOW_SMALL = part('''
{SHADOW_SMALL}
''')

{FLIGHT_SHAPES}

# --- small parts ----------------------------------------------------------------------------

# Bare white skin around the eye with thin rows of red feathers
MASK = part('''
    ..WWWWW..
    .WWWWWWW.
    WWWWWWWWW
    WWWWWWWWW
    WqqWWWWWW
    .WWWqqWW.
    ..WWWWW..
''')

EYE = part('''
    .YY.
    YWKY
    YKKY
    .YY.
''')
EYE_SHUT = part('''
    mmmm
''')
# The face from the front: a white patch on each side of the beak, the beak hooked down towards you
FRONT_MASK = part('''
    .WWWW
    WWWWW
    WWWWW
    WqqWW
    .WWqq
''')
FRONT_BEAK = part('''
    .MMMM.
    MMWMMM
    MMMMMM
    MMMMMg
    .mMMg.
    ..mm..
''')
EYE_HAPPY = part('''
    .mm.
    m..m
''')

# Upper beak: pale, hooked, with a shine and a dark tip
BEAK = part('''
    ...MMMMM
    ..MWMMMM
    .MMMMMMM
    MMMMMMgg
    MMMMMgg.
    MMMMg...
    mMMg....
    mm......
''')
BEAK_LOW = part('''
    ..mmm
    .mmmm
    .mmm.
''')

LEG = part('''
    gg
    gg
    gg
    gg
''')
# Zygodactyl feet: two toes forward, two back
FOOT = part('''
    .gggg.
    gg..gg
''')
# Raised, gripping a seed
FOOT_GRIP = part('''
    g...g
    gg.gg
    .ggg.
''')
# The leg reaching up from the belly to the raised foot
LEG_REACH = part('''
    gg....
    .gg...
    ..gg..
    ...gg.
    ....gg
''')

SEED = part('''
    .m.
    mMm
    mMm
    mMm
    mmm
''')
CRUMB = part('''
    M.
    .M
''')

NOTE = part('''
    ..UU.
    ..U.U
    ..U..
    UUU..
    UUU..
''')
NOTE_DOUBLE = part('''
    .YYYY
    .Y..Y
    .Y..Y
    YY.YY
    YY.YY
''')
STAR = part('''
    ..Y..
    ..Y..
    YYWYY
    ..Y..
    ..Y..
''')
STAR_SMALL = part('''
    .Y.
    YWY
    .Y.
''')
Z_SMALL = part('''
    WWWW
    ..W.
    .W..
    WWWW
''')
Z_BIG = part('''
    WWWWW
    ...W.
    ..W..
    .W...
    WWWWW
''')

# Feet tucked under the belly in flight
FEET_TUCKED = part('''
    gg.
    ggg
    .gg
''')
# Two feathers shaken loose by the take-off
FEATHER_RED = part('''
    .R
    RR
    r.
''')
FEATHER_BLUE = part('''
    U.
    UU
    .u
''')
# The far wing, in shadow behind the body
FAR_WING = {'R': 'r', 'q': 'R', 'Y': 'o', 'U': 'u', 'p': 'U'}

# --- accessories ----------------------------------------------------------------------------
# The engine sets `wearing` before building frames; every pose places it on the head.

ACCESSORIES = ('bow', 'halloween_hat', 'winter_hat')
NO_ACCESSORY = ()
wearing = None

BOW = part('''
    UU...UU
    UpU.UUU
    UUUYUUU
    UUU.UUu
    UU...uu
''')

WITCH_HAT = part('''
    .........mm...
    ........mm....
    .......mmm....
    ......mmmm....
    .....mmmmm....
    ....mmmmmm....
    ....YYYYYY....
    .mmmmmmmmmmmm.
    mmmmmmmmmmmmmm
''')

SANTA_HAT = part('''
    .......RRRR.....
    .....RRRRRRRR...
    ....RRRRRRRRRrWW
    ...RRRRRRRRRr.WW
    ..RRRRRRRRRRr...
    WWWWWWWWWWWWWW..
    MMMMMMMMMMMMMM..
''')


def accessory(hx, hy):
    """The worn accessory, placed relative to the head's top-left corner."""
    if wearing == 'bow':
        return [L(BOW, hx + 11, hy - 3)]
    if wearing == 'halloween_hat':
        return [L(WITCH_HAT, hx + 2, hy - 8)]
    if wearing == 'winter_hat':
        return [L(SANTA_HAT, hx + 1, hy - 7)]
    return []


def accessory_icon(name):
    """The accessory on its own, for the wardrobe in the app."""
    rows = {'bow': BOW, 'halloween_hat': WITCH_HAT, 'winter_hat': SANTA_HAT}[name]
    return [L(rows, 1, 1)], (len(rows[0]) + 2, len(rows) + 2)


# A maple leaf like the ones falling in the maple park (in the background's colours), as the pets
# hold it after their interactions with the backgrounds (BackgroundEvents): the parrot holds it by the stem, hanging from its beak
MAPLE_LEAF = part('''
    ..e..
    .EEE.
    LLELL
    .L.L.
''')


# --- poses ----------------------------------------------------------------------------------

BODY_X, BODY_Y = 17, 27
LEG_X = (26, 31)                   # near, far
FEET_Y = 58


def at_root(rows, root, x, y, **kw):
    """A wing or a feather placed so that its root lands on (x, y)."""
    return L(rows, x - root[0], y - root[1], **kw)


def ground(rows=SHADOW_STAND):
    return [L(rows, 31 - len(rows[0]) // 2, 61, ring=False)]


def tail(tx, ty, sway=0):
    """Three feathers; sway swings the tips a pixel up or down."""
    return [at_root(TAIL_LOW, TAIL_LOW_ROOT, tx, ty + min(sway, 0)),
            at_root(TAIL_HIGH, TAIL_HIGH_ROOT, tx, ty + max(sway, 0)),
            at_root(TAIL_MAIN, TAIL_MAIN_ROOT, tx, ty + sway)]


def head(hx, hy, eyes='open', beak='closed', tilt=0):
    """Big round head with the white mask and the hooked beak. beak 'open' drops the lower beak;
    tilt 1 lifts the face a pixel, as if it looked up curiously."""
    layers = [L(HEAD, hx, hy), L(MASK, hx + 2, hy + 3 - tilt, ring=False, clip=True)]
    if eyes == 'open':
        layers.append(L(EYE, hx + 5, hy + 4 - tilt, ring=False))
    elif eyes == 'shut':
        layers.append(L(EYE_SHUT, hx + 5, hy + 6 - tilt, ring=False))
    elif eyes == 'happy':
        layers.append(L(EYE_HAPPY, hx + 5, hy + 5 - tilt, ring=False))
    layers.append(L(BEAK_LOW, hx - 1, hy + 10 - tilt + (2 if beak == 'open' else 0)))
    layers.append(L(BEAK, hx - 5, hy + 4 - tilt))
    return layers + accessory(hx, hy)


def head_front(hx, hy, eyes='open'):
    """The head turned to look at you: an eye in a white patch on each side, the beak between."""
    layers = [L(HEAD, hx, hy), L(FRONT_MASK, hx + 1, hy + 4, ring=False, clip=True),
              L(FRONT_MASK, hx + 12, hy + 4, ring=False, clip=True, flip_h=True)]
    if eyes == 'open':
        layers += [L(EYE, hx + 2, hy + 5, ring=False), L(EYE, hx + 12, hy + 5, ring=False, flip_h=True)]
    else:
        layers += [L(EYE_SHUT, hx + 2, hy + 7, ring=False), L(EYE_SHUT, hx + 12, hy + 7, ring=False)]
    layers.append(L(FRONT_BEAK, hx + 6, hy + 8))
    return layers + accessory(hx, hy)


def perched(dy=0, sway=0, head_dx=0, head_dy=0, tilt=0, eyes='open', beak='closed', wing='fold',
            tail_sway=0, lift=(0, 0), fluff=False, grip=None, effects=(), leaf=False, feet=(0, 0),
            facing_you=False):
    """Standing on the ground. dy moves everything but the feet (negative: a hop lifts them too);
    sway leans the body a pixel; lift raises the near or far foot, feet moves it sideways; grip: a
    seed held up in the near foot at that position, relative to the head; leaf: a maple leaf held
    in the beak; facing_you: the head turned to look at you."""
    bx, by = BODY_X + sway, BODY_Y + dy
    hop = min(dy, 0)
    hx, hy = bx - 5 + head_dx, by - 13 + head_dy
    layers = list(ground(SHADOW_SMALL if dy < -2 else SHADOW_STAND))
    layers += tail(bx + 17, by + 21, tail_sway)
    if wing == 'up':
        layers.append(at_root(*WINGS['up'], bx + 12, by + 4))
    # Far leg, then the body, then the near leg in front
    layers.append(L(LEG, LEG_X[1] + feet[1], FEET_Y - 4 - lift[1] + hop, recolor=FAR))
    layers.append(L(FOOT, LEG_X[1] + feet[1] - 2, FEET_Y - lift[1] + hop, recolor=FAR))
    layers.append(L(BODY_FLUFF if fluff else BODY, bx - (1 if fluff else 0), by - (1 if fluff else 0)))
    if grip is None:
        layers.append(L(LEG, LEG_X[0] + feet[0], FEET_Y - 4 - lift[0] + hop))
        layers.append(L(FOOT, LEG_X[0] + feet[0] - 2, FEET_Y - lift[0] + hop))
    if wing in ('fold', 'out'):
        layers.append(at_root(*WINGS[wing], bx + 12, by + 4))
    if facing_you:
        return layers + head_front(hx + 1, hy, eyes) + list(effects)
    layers += head(hx, hy, eyes, beak, tilt)
    if leaf:
        layers.append(L(MAPLE_LEAF, hx - 5, hy + 12 - tilt))
    if grip is not None:
        gx, gy = hx + grip[0], hy + grip[1]
        layers.append(L(LEG_REACH, gx + 3, gy + 6))
        layers.append(L(SEED, gx, gy))
        layers.append(L(FOOT_GRIP, gx - 1, gy + 3))
    return layers + list(effects)


def tucked(breath):
    """Asleep on one leg, fluffed up, the head turned back with the beak hidden in the feathers."""
    bx, by = BODY_X, BODY_Y - breath
    hx, hy = bx + 3, by - 8
    layers = list(ground())
    layers += tail(bx + 17, BODY_Y + 21)
    layers.append(L(BODY_FLUFF, bx - 1, by - 1, grow_y=breath))
    layers.append(L(LEG, LEG_X[0], FEET_Y - 4))
    layers.append(L(FOOT, LEG_X[0] - 2, FEET_Y))
    layers.append(L(HEAD, hx, hy, flip_h=True))
    layers.append(L(MASK, hx + 7, hy + 3, ring=False, clip=True, flip_h=True))
    layers.append(L(EYE_SHUT, hx + 9, hy + 6, ring=False))
    layers.append(L(BEAK, hx + 15, hy + 5, flip_h=True))
    layers.append(at_root(*WINGS['fold'], bx + 13, by - 1))
    return layers + accessory(hx, hy)


def frames(*pattern):
    """Expands (layers, count) pairs: a key pose held for count frames."""
    out = []
    for layers, count in pattern:
        out += [layers] * count
    return out


# --- animations -----------------------------------------------------------------------------

def idle_frames():
    """Blinks, tilts its head with curiosity, shifts its weight, then fluffs up and shakes."""
    calm = perched()
    return frames(
        (calm, 2), (perched(eyes='shut'), 1), (calm, 1),
        (perched(tilt=1, head_dx=-1), 2), (calm, 1),
        (perched(lift=(1, 0)), 1), (perched(sway=-1), 1), (calm, 1),
        (perched(dy=1), 1),
        (perched(fluff=True, head_dx=-1, eyes='shut'), 1),
        (perched(fluff=True, head_dx=1, eyes='shut', tail_sway=1), 1),
        (perched(tail_sway=-1), 1), (calm, 2),
    )


def eat_frames():
    """Holds a seed up in its foot and pecks at it; crumbs fall."""
    def crumbs(t):
        layers = []
        for start, x in ((4, 8), (10, 10)):
            age = (t - start) % 16
            if age < 4:
                layers.append(L(CRUMB, x - age // 2, 38 + 5 * age, ring=False))
        return layers
    out = []
    pattern = [0, 0, 1, 2, 2, 1, 0, 0, 1, 2, 2, 1, 0, 0, 0, 0]
    for t, peck in enumerate(pattern):
        beak = 'open' if peck == 1 else 'closed'
        eyes = 'happy' if t in (13, 14) else 'shut' if t == 6 else 'open'
        # The head comes down to the seed; the foot holds it still
        out.append(perched(head_dy=peck, head_dx=0, beak=beak, eyes=eyes,
                           grip=(-5, 13 - peck), effects=crumbs(t)))
    return out


def talk_frames():
    """Chatters: the beak opens in rhythm, the head bobs, notes rise."""
    def notes(t):
        layers = []
        for start, x, rows in ((0, 2, NOTE), (6, 6, NOTE_DOUBLE)):
            age = (t - start) % 12
            if age < 6:
                layers.append(L(rows, x + age % 2, 12 - 2 * age))
        return layers
    beat = [0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 1, 0]
    return [perched(beak='open' if b else 'closed', head_dy=b, head_dx=-b, tail_sway=beat[t - 1],
                    effects=notes(t)) for t, b in enumerate(beat)]


def happy_frames():
    """Flaps its wings and hops, with stars."""
    def stars(t):
        layers = []
        for start, x, y in ((1, 4, 14), (3, 50, 12), (5, 44, 8)):
            age = t - start
            if 0 <= age < 5:
                layers.append(L(STAR if age in (1, 2) else STAR_SMALL, x, y - age))
        return layers
    poses = [
        dict(dy=1, wing='fold'),
        dict(dy=-2, wing='up', beak='open'),
        dict(dy=-4, wing='out', beak='open'),
        dict(dy=-5, wing='up', beak='open'),
        dict(dy=-4, wing='out'),
        dict(dy=-2, wing='up'),
        dict(dy=1, wing='out', tail_sway=1),
        dict(dy=0, wing='fold', tail_sway=-1),
        dict(dy=0, wing='fold'),
        dict(dy=0, wing='fold'),
    ]
    return [perched(eyes='happy', effects=stars(t), **pose) for t, pose in enumerate(poses)]


def sleep_frames():
    """Asleep on one leg: slow breathing, Z rising."""
    breath = [0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0]
    out = []
    for t in range(12):
        z = []
        for phase in (0, 6):
            a = (t + phase) % 12
            if a < 9:
                z.append(L(Z_SMALL if a < 4 else Z_BIG, 40 + a // 2 + (2 if phase else 0), 18 - 2 * a))
        out.append(tucked(breath[t]) + z)
    return out


def leg_x(f, phase):
    """Where a foot is (and how high it is lifted) in walk frame f: planted for five frames, moving
    back exactly by WALK_SHIFTS while the body goes forward, then swung forward in three."""
    p = (f + phase) % 8
    start = f - p
    if p <= 4:
        return sum(WALK_SHIFTS[k % 8] for k in range(start + 1, f + 1)) - 2, 0
    travel = sum(WALK_SHIFTS[k % 8] for k in range(start + 1, start + 5))
    return travel - 2 - round(travel * (p - 4) / 4), (1, 2, 1)[p - 5]


def walking(f):
    """Waddle, frame f of 8: a foot steps while the other one stays planted (moving back by
    WALK_SHIFTS), the body rocks over the planted foot, the head bobs forward and back, the tail
    swings a frame behind."""
    near, far = leg_x(f, 0), leg_x(f, 4)
    # A wider stance than standing, so a foot just set down never lands on the other one
    sway = (0, -1, -1, 0, 0, 1, 1, 0)
    bob = (1, 0, 0, 0, 1, 0, 0, 0)
    head = (0, -1, -1, 0, 0, 1, 1, 0)
    return perched(dy=bob[f], sway=sway[f], head_dx=head[f - 1] - sway[f], lift=(near[1], far[1]),
                   feet=(near[0] - 1, far[0] + 2), tail_sway=-sway[f - 1])


def walk_frames():
    return [walking(f) for f in range(8)]


def setting_off():
    """Between standing and the walk's wider stance: the feet half apart (the first or last step)."""
    return perched(feet=(-2, 2), dy=1)


def walk_start_frames():
    return [setting_off()]


def walk_stop_frames():
    return [setting_off()]


def front():
    """Facing you: the body upright, a wing folded on each side, the tail's tip between the feet."""
    layers = list(ground())
    layers.append(at_root(TAIL_FRONT, TAIL_FRONT_ROOT, 31, 47))
    for x in (27, 35):
        layers.append(L(LEG, x, FEET_Y - 4))
        layers.append(L(FOOT, x - 2, FEET_Y))
    layers.append(L(BODY_FRONT, 22, 28))
    w = len(WING_FRONT[0])
    layers.append(at_root(WING_FRONT, WING_FRONT_ROOT, 24, 32))
    layers.append(L(WING_FRONT, 39 - (w - 1 - WING_FRONT_ROOT[0]), 32 - WING_FRONT_ROOT[1], flip_h=True))
    return layers + head_front(23, 14)


def turn_frames():
    """Turning round (PetBehavior plays them there and back, mirrored on the way back)."""
    return [perched(facing_you=True), front()]


def dozing():
    """Fluffing up before it sleeps (or after it wakes): eyes shut, the far foot drawn up."""
    return perched(fluff=True, eyes='shut', head_dy=1, lift=(0, 3))


def lie_down_frames():
    return [dozing()]


def get_up_frames():
    return [dozing()]


def reaching():
    """Lifting the near foot to take a seed (or putting it down), looking at it."""
    return perched(lift=(3, 0), head_dy=1, head_dx=1)


def eat_in_frames():
    return [reaching()]


def eat_out_frames():
    return [reaching()]


# --- flight ---------------------------------------------------------------------------------

def flyer(wing, far_wing, tilt=28, bob=0, legs=0):
    """In the air, facing left: the body tilted by tilt degrees (head up), the near wing at angle
    wing and the far one at far_wing (shown only while it is up, above the back); legs 0: tucked
    under the belly, 1 to 3: reaching down for the ground, longer and longer."""
    rows, centre = BODY_FLY[tilt]
    cx, cy = 27, 32 + bob
    a = math.radians(tilt)
    ux, uy = -math.cos(a), -math.sin(a)          # towards the head
    sx, sy = cx + ux * 4 + 1, cy + uy * 4 - 4    # the shoulder
    layers = []
    if far_wing < -10:
        fw, froot = WING_FLY[far_wing]
        layers.append(L(fw, round(sx - froot[0]) - 2, round(sy - froot[1]) - 2, recolor=FAR_WING))
    tx, ty = cx - ux * 9, cy - uy * 9 + 1
    for feather, root in TAIL_FLY:
        layers.append(L(feather, round(tx - root[0]), round(ty - root[1])))
    for x, near in ((cx + 1, False), (cx - 3, True)):
        if legs:
            for k in range(legs):
                layers.append(L(LEG, x, cy + 6 + 4 * k, recolor=None if near else FAR))
            layers.append(L(FOOT, x - 2, cy + 6 + 4 * legs, recolor=None if near else FAR))
        if not near:
            layers.append(L(rows, cx - centre[0], cy - centre[1]))
    if not legs:
        layers.append(L(FEET_TUCKED, cx + 3, cy + 6))
    layers += head(round(cx + ux * 10 - 9), round(cy + uy * 10 - 8) - 3)
    w, root = WING_FLY[wing]
    layers.append(L(w, round(sx - root[0]), round(sy - root[1])))
    return layers


# One wingbeat: up, the power stroke down under the belly, and back up; the far wing a frame
# behind; the body rises a pixel on the down stroke
FLAP = (-70, -50, -20, 15, 50, 70, 40, -30)
FLAP_BOB = (1, 1, 0, -1, -1, -1, 0, 1)


def fly_frames():
    return [flyer(FLAP[f], FLAP[f - 1], bob=FLAP_BOB[f]) for f in range(8)]


def feathers(t):
    """The two loose feathers, drifting down and back for four frames."""
    out = []
    for start, x, rows in ((2, 30, FEATHER_RED), (3, 22, FEATHER_BLUE)):
        age = t - start
        if 0 <= age < 4:
            out.append(L(rows, x + age + age % 2, 44 + 4 * age))
    return out


def takeoff_frames():
    """Crouches, leans forward and springs with the wings raised, beats into the air folding its
    legs up. The widget lifts the frame from the fourth one on, slowly at first; the bird rises
    inside the frame meanwhile, so its head and body go up evenly and the head never drops when
    the standing pose turns into the flying one (and its body moves at most 6 px a frame)."""
    poses = [
        perched(dy=1, wing='fold'),
        perched(dy=2, wing='out', eyes='happy', head_dy=1),
        perched(dy=-2, wing='up', beak='open', head_dy=3, head_dx=-1),
        flyer(70, 50, tilt=50, bob=3, legs=3),
        flyer(-50, -30, tilt=38, bob=0, legs=2),
        flyer(15, -20, bob=-2, legs=1),
    ]
    return [layers + feathers(t) for t, layers in enumerate(poses)]


def land_frames():
    """Brakes with the wings up and the legs reaching down, touches down, sinks in, bounces once
    and stands. The widget sets the frame on the ground from the third one on; the take-off
    backwards, so its head and body come down evenly."""
    return [
        flyer(-50, -70, tilt=38, bob=0, legs=2),
        flyer(-70, -50, tilt=50, bob=3, legs=3),
        perched(dy=1, wing='up', head_dy=2),
        perched(dy=2, wing='out'),
        perched(dy=-1, wing='fold', tail_sway=-1),
        perched(),
    ]


# Played once each, not looped (check.py doesn't compare their last frame with the first)
ONE_SHOT = ('takeoff', 'land', 'turn', 'walk_start', 'walk_stop', 'lie_down', 'get_up', 'eat_in', 'eat_out')
# In the air: the wingbeat and the landing's first frame
OFF_GROUND = ('fly', 'land')

# --- the backgrounds' interactions (BackgroundEvents) -------------------------------------

def leaf_frames():
    """Shows off the maple leaf it caught in its beak: tilts its head, a proud blink, shifts its
    weight."""
    calm = perched(leaf=True)
    return frames(
        (calm, 2), (perched(tilt=1, head_dx=-1, leaf=True), 2), (calm, 1),
        (perched(eyes='happy', leaf=True), 2),
        (perched(lift=(1, 0), leaf=True), 1), (perched(sway=-1, leaf=True), 1), (calm, 1),
        (perched(eyes='shut', leaf=True), 1), (calm, 1),
    )


ANIMATIONS = {
    'sleep': {'interval': 160, 'frames': sleep_frames},
    'happy': {'interval': 90, 'frames': happy_frames},
    'idle': {'interval': 130, 'frames': idle_frames},
    'eat': {'interval': 110, 'frames': eat_frames},
    'talk': {'interval': 110, 'frames': talk_frames},
    'eat_in': {'interval': 110, 'frames': eat_in_frames},
    'eat_out': {'interval': 110, 'frames': eat_out_frames},
    'walk': {'interval': 120, 'frames': walk_frames},
    'walk_start': {'interval': 120, 'frames': walk_start_frames},
    'walk_stop': {'interval': 120, 'frames': walk_stop_frames},
    'turn': {'interval': 120, 'frames': turn_frames},
    'lie_down': {'interval': 160, 'frames': lie_down_frames},
    'get_up': {'interval': 160, 'frames': get_up_frames},
    # The flight plays at one pace from take-off to landing (PetSpec.FLIGHT_INTERVAL_MS)
    'takeoff': {'interval': 80, 'frames': takeoff_frames},
    'fly': {'interval': 80, 'frames': fly_frames},
    'land': {'interval': 80, 'frames': land_frames},
    'leaf': {'interval': 130, 'frames': leaf_frames},
}
