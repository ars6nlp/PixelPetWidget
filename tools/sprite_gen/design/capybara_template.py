"""Capybara: calm, friendly and boxy. Faces left on a 64x64 canvas, feet on row 59 over a ground
shadow.

Smooth motion: its walk moves the planted feet by exactly WALK_SHIFTS (PetSpec.CAPYBARA_GAIT);
it stands sideways, so it turns round through looking at you and facing you; it lowers itself to
lie down (asleep, in the puddle, on the warm field) and steps into its bath.

Drawn from text matrices: big shapes carry their own three tones (light from the top left), the
engine adds the one-pixel outline. Key poses are held by repeating a frame, since the widget plays
every frame for the same time.
"""
from sprite_gen import L, part

PALETTE = {
    'K': '#1f2133',  # outline (deep blue-grey, shared by all pixel pets); pupils, nostrils
    'B': '#b07a48',  # warm brown-ginger fur
    'h': '#d29d64',  # fur highlight
    'b': '#85552f',  # fur shadow, far legs, soft inner lines
    'd': '#4f3020',  # dark nose, eyelids, mouth, toes, inner ears
    'c': '#efc07e',  # lighter belly; orange highlight
    'W': '#ffffff',  # eye shine, water streaks, bubbles
    'A': '#6cbfe3',  # water
    'a': '#3f86b8',  # deep water
    'q': '#b4e2f4',  # light water, foam, ripples, steam, "Z"
    'O': '#f39237',  # orange
    'o': '#c9651f',  # orange shadow
    'G': '#78b84f',  # grass, leaf
    'g': '#3f7f3a',  # grass shadow, stalk
    'H': '#ef5b70',  # hearts, tongue, bow, Santa hat
    'S': '#3b3d52',  # ground shadow
    'L': '#f6a531',  # maple leaf (the maple park's colours)
    'R': '#e8702a',  # maple leaf, middle tone
    'r': '#b9452a',  # maple leaf shadow, stem
}
MAX_COLOURS = 19
OUTLINE = 'K'
LINE_OF = {'B': 'b', 'h': 'b', 'b': 'd', 'd': 'd', 'c': 'b', 'O': 'o', 'o': 'o', 'A': 'a', 'a': 'a',
           'q': 'a', 'G': 'g', 'g': 'g', 'H': 'd', 'W': 'a', 'L': 'r', 'R': 'r', 'r': 'r'}
SHADE = []                         # every part carries its own tones
# Single pixels that are details on purpose: (colour, a neighbour colour it must touch)
ORPHAN_OK = (('W', 'K'), ('W', 'A'), ('W', 'c'), ('q', 'A'), ('q', 'a'), ('H', 'd'), ('c', 'B'), ('r', 'R'))

FAR = {'B': 'b', 'h': 'B', 'c': 'B'}
# How far its planted feet move back in each walk frame (art pixels): PetSpec.CAPYBARA_GAIT
WALK_SHIFTS = (1, 1, 2, 1, 1, 1, 2, 1)

# --- big shapes (generated with three tones) ----------------------------------------------

# Two empty rows on top leave room for the fur tufts along the back
BODY = part('''
{BODY}
''')

LYING_BODY = part('''
{LYING_BODY}
''')

HEAD = part('''
{HEAD}
''')

# The head and the body seen from the front (turning round)
FRONT_HEAD = part('''
{FRONT_HEAD}
''')

BODY_FRONT = part('''
{BODY_FRONT}
''')

# The lower jaw: it drops a pixel when chewing, two when yawning
JAW = part('''
{JAW}
''')

POOL = part('''
{POOL}
''')
WATER_ROW = 4                      # the water surface in front of the capybara starts here
POOL_FRONT = POOL[WATER_ROW:]

SHADOW_STAND = part('''
{SHADOW_STAND}
''')

SHADOW_LYING = part('''
{SHADOW_LYING}
''')

SHADOW_POOL = part('''
{SHADOW_POOL}
''')

# --- small parts ----------------------------------------------------------------------------

EAR = part('''
    .BB.
    BdBB
    BBBB
''')
# Flicked back
EAR_FLICK = part('''
    ..BB
    .BdB
    BBBB
''')

# Calm, heavy-lidded eyes are the capybara's default
EYE = part('''
    dddd
    KWKK
    .KK.
''')
EYE_SHUT = part('''
    dddd
''')
EYE_HAPPY = part('''
    .dd.
    d..d
''')
EYE_SLEEP = part('''
    d..d
    .dd.
''')

# The dark, square nose at the front of the muzzle, a nostril in it
NOSE = part('''
    .ddd
    dddd
    Kddd
    ddd.
''')
MOUTH = part('''
    dddd
''')
# Yawning: the open mouth behind the dropped jaw
MOUTH_OPEN = part('''
    ddddddddd
    dHHHHHdd.
    dHHHHdd..
    dddddd...
''')

LEG = part('''
    .BBBB.
    .hBBb.
    .hBBb.
    .hBBb.
    .hBBb.
    .hBBb.
    hhBBBb
    hdBdBb
''')
# A front paw tucked under the chin while it sleeps
PAW = part('''
    hBBBb
    BdBdb
''')

GRASS = {
    10: part('''
{GRASS_10}
'''),
    9: part('''
{GRASS_9}
'''),
    8: part('''
{GRASS_8}
'''),
    7: part('''
{GRASS_7}
'''),
    6: part('''
{GRASS_6}
'''),
    5: part('''
{GRASS_5}
'''),
    4: part('''
{GRASS_4}
'''),
    3: part('''
{GRASS_3}
'''),
    2: part('''
{GRASS_2}
'''),
    1: part('''
{GRASS_1}
'''),
}

ORANGE = part('''
{ORANGE}
''')

HEART = part('''
    .HH.HH.
    HWHHHHH
    HHHHHHH
    .HHHHH.
    ..HHH..
    ...H...
''')
HEART_SMALL = part('''
    HH.HH
    HHHHH
    .HHH.
    ..H..
''')

Z_SMALL = part('''
    qqqq
    ..q.
    .q..
    qqqq
''')
Z_BIG = part('''
    qqqqq
    ...q.
    ..q..
    .q...
    qqqqq
''')

# Bath effects: the foam line where the water meets the fur, ripples, bubbles and steam
RIPPLE_SMALL = part('''
    .qqq.
    q...q
''')
RIPPLE_BIG = part('''
    ..qqqqq..
    qq.....qq
''')
BUBBLE = part('''
    .q.
    qWq
    .q.
''')
BUBBLE_POP = part('''
    q.q
    ...
    q.q
''')
STEAM = [part('''
    .q
    q.
    .q
'''), part('''
    q.
    .q
    q.
''')]
STEAM_THIN = part('''
    .q
    q.
''')

# --- accessories ----------------------------------------------------------------------------
# The engine sets `wearing` before building frames; in the bath it wears its orange instead.

ACCESSORIES = ('bow', 'halloween_hat', 'winter_hat')
NO_ACCESSORY = ('bath',)
wearing = None

BOW = part('''
    HH...HH
    HHH.HHH
    HHHdHHH
    HHH.HHH
    HH...HH
''')

PUMPKIN_HAT = part('''
    ......gg......
    ......g.......
    ..cccOOOoOO...
    .cOOoOOOoOOo..
    cOOOoOOOoOOoo.
    OOOOoOOOoOOooo
    .OOOoOOOoOooo.
    ..ooooooooooo.
''')

SANTA_HAT = part('''
    .......HHHH.....
    .....HHHHHHHH...
    ....HHHHHHHHHHWW
    ...HHHHHHHHHH.WW
    ..HHHHHHHHHHH...
    WWWWWWWWWWWWWW..
    qqqqqqqqqqqqqq..
''')


def accessory(hx, hy):
    """The worn accessory, placed relative to the head's top-left corner."""
    if wearing == 'bow':
        return [L(BOW, hx + 10, hy - 4)]
    if wearing == 'halloween_hat':
        return [L(PUMPKIN_HAT, hx + 4, hy - 6)]
    if wearing == 'winter_hat':
        return [L(SANTA_HAT, hx + 3, hy - 5)]
    return []


def accessory_icon(name):
    """The accessory on its own, for the wardrobe in the app."""
    rows = {'bow': BOW, 'halloween_hat': PUMPKIN_HAT, 'winter_hat': SANTA_HAT}[name]
    return [L(rows, 1, 1)], (len(rows[0]) + 2, len(rows) + 2)


# A maple leaf like the ones falling in the maple park (in the background's colours), as the pets
# hold it after their interactions with the backgrounds (BackgroundEvents)
MAPLE_LEAF = part('''
    .L.L.
    LLRLL
    .RRR.
    ..r..
''')


# --- poses ----------------------------------------------------------------------------------

BODY_X, BODY_Y = 17, 30
HEAD_X, HEAD_Y = 4, 22
LEG_X = (21, 26, 44, 49)           # front near, front far, back far, back near
LEG_FAR = (False, True, True, False)
LEG_Y = 52


def ground(rows=SHADOW_STAND):
    return [L(rows, 31 - len(rows[0]) // 2, 61, ring=False)]


def head(hx, hy, eyes='calm', jaw=0, ear=0):
    """The brick-shaped head. jaw: how far the lower jaw has dropped (0-2); ear 1 flicks the near
    ear back."""
    layers = [L(EAR, hx + 18, hy - 3, recolor=FAR), L(HEAD, hx, hy)]
    if jaw >= 2:
        layers.append(L(MOUTH_OPEN, hx, hy + 13, ring=False, clip=True))
    if jaw:
        layers.append(L(JAW, hx + 1, hy + 14 + jaw))
    layers.append(L(MOUTH, hx + 1, hy + 13 + jaw, ring=False))
    layers.append(L(NOSE, hx, hy + 5, ring=False, clip=True))
    if eyes == 'calm':
        layers.append(L(EYE, hx + 11, hy + 5, ring=False))
    elif eyes == 'shut':
        layers.append(L(EYE_SHUT, hx + 11, hy + 6, ring=False))
    elif eyes == 'happy':
        layers.append(L(EYE_HAPPY, hx + 11, hy + 5, ring=False))
    elif eyes == 'sleep':
        layers.append(L(EYE_SLEEP, hx + 11, hy + 6, ring=False))
    layers.append(L(EAR_FLICK if ear else EAR, hx + 15, hy - 2 - ear))
    return layers + accessory(hx, hy)


def standing(dy=0, head_dy=0, breath=0, eyes='calm', jaw=0, ear=0, legs=None, props=(), effects=(), hat=False,
             shadow=True, facing_you=False):
    """Standing on four short legs. dy moves body and head (the feet stay on the ground), breath
    lifts the back. legs: (dx, lift) per leg, in LEG_X order. hat: a maple leaf on its head.
    facing_you: the head turned to look at you (turning round)."""
    legs = legs or [(0, 0)] * 4
    layers = list(ground()) if shadow else []
    for i in (1, 2, 0, 3):             # far legs first; the body covers the tops of all of them
        dx, lift = legs[i]
        layers.append(L(LEG, LEG_X[i] + dx, LEG_Y - lift, recolor=FAR if LEG_FAR[i] else None))
    layers.append(L(BODY, BODY_X, BODY_Y + dy - breath, grow_y=breath))
    if facing_you:
        return layers + front_head(HEAD_X + 4, HEAD_Y + dy + head_dy - 1, eyes)
    layers += head(HEAD_X, HEAD_Y + dy + head_dy, eyes, jaw, ear)
    if hat:
        layers.append(L(MAPLE_LEAF, HEAD_X + 7, HEAD_Y + dy + head_dy - 4))
    return layers + list(props) + list(effects)


def front_head(hx, hy, eyes='calm'):
    """The head seen from the front: both ears up, an eye on each side, the dark nose below."""
    eye = {'calm': EYE, 'shut': EYE_SHUT, 'happy': EYE_HAPPY, 'sleep': EYE_SLEEP}[eyes]
    layers = [L(EAR, hx + 1, hy - 2), L(EAR, hx + 13, hy - 2, flip_h=True), L(FRONT_HEAD, hx, hy),
              L(eye, hx + 2, hy + 6, ring=False), L(eye, hx + 12, hy + 6, ring=False, flip_h=True),
              L(MOUTH, hx + 7, hy + 15, ring=False, clip=True)]
    # The accessory sits as on the side head, centred on this narrower one
    return layers + accessory(hx - 2, hy)


def turning(step):
    """Turning round: step 1 standing sideways looking at you, step 2 facing you."""
    if step == 1:
        return standing(facing_you=True)
    layers = list(ground())
    layers.append(L(LEG, 22, LEG_Y - 1, recolor=FAR))
    layers.append(L(LEG, 36, LEG_Y - 1, recolor=FAR))
    layers.append(L(BODY_FRONT, 17, 32))
    layers.append(L(LEG, 25, LEG_Y))
    layers.append(L(LEG, 33, LEG_Y))
    return layers + front_head(23, 22)


def lowered():
    """Lowering itself to lie down, or getting up: the body sinks between the legs."""
    return standing(dy=4)


def lying(breath, eyes='sleep', ear=0):
    """Asleep (or just lying) on its belly, chin on the ground, a paw tucked under it."""
    hx, hy = 4, 41
    layers = list(ground(SHADOW_LYING))
    layers.append(L(LYING_BODY, 16, 41 - breath, grow_y=breath))
    layers += head(hx, hy, eyes, ear=ear)
    layers.append(L(PAW, hx + 21, 58))
    return layers


def frames(*pattern):
    """Expands (layers, count) pairs: a key pose held for count frames."""
    out = []
    for layers, count in pattern:
        out += [layers] * count
    return out


# --- animations -----------------------------------------------------------------------------

def idle_frames():
    """Breathes, blinks slowly, flicks an ear, then a big unhurried yawn."""
    calm = standing()
    inhale = standing(breath=1)
    return frames(
        (calm, 2), (inhale, 2), (calm, 1),
        (standing(eyes='shut'), 2),
        (calm, 1), (standing(ear=1), 1), (calm, 1),
        (standing(head_dy=-1, eyes='shut'), 1),
        (standing(head_dy=-1, eyes='shut', jaw=1), 1),
        (standing(head_dy=-1, eyes='shut', jaw=3), 2),
        (standing(eyes='shut', jaw=1), 1),
        (standing(eyes='happy'), 1),
    )


def eat_frames():
    """Chews a blade of grass, pulling it in bit by bit; the jaw drops a pixel with each bite."""
    out = []
    for t in range(16):
        jaw = 1 if t % 4 in (1, 2) else 0
        length = 10 - round(t * 9 / 13) if t < 14 else (0, 10)[t - 14]
        length = 10 if t == 0 else length
        eyes = 'happy' if 6 <= t <= 9 or t == 14 else 'calm'
        head_dy = 1 if t == 14 else 0          # dips down for the next blade
        hx, hy = HEAD_X, HEAD_Y + head_dy
        grass = []
        if length:
            blade = GRASS[length]
            grass.append(L(blade, hx + 9 - len(blade[0]), hy + 15 + jaw))
        out.append(standing(head_dy=head_dy, eyes=eyes, jaw=jaw, props=grass))
    return out


def happy_frames():
    """Squints with contentment, bobs once, hearts float up from the head."""
    def hearts(t):
        layers = []
        for start, x in ((0, 22), (5, 30)):
            age = (t - start) % 10
            if age < 7:
                rows = HEART if 1 <= age <= 4 else HEART_SMALL
                layers.append(L(rows, x + age // 3, 14 - 2 * age))
        return layers
    poses = [(0, 0), (1, 0), (-1, 0), (-1, -1), (0, -1), (0, 0), (0, 0), (0, 0), (0, 0), (0, 0)]
    return [standing(dy=dy, head_dy=hdy, eyes='happy', ear=1 if t in (2, 3) else 0, effects=hearts(t))
            for t, (dy, hdy) in enumerate(poses)]


def sleep_frames():
    """Asleep on its belly: slow breathing, Z rising from the head."""
    breath = [0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0]
    out = []
    for t in range(12):
        z = []
        for phase in (0, 6):
            a = (t + phase) % 12
            if a < 9:
                z.append(L(Z_SMALL if a < 4 else Z_BIG, 27 + a // 2 + (2 if phase else 0), 33 - 3 * a))
        out.append(lying(breath[t]) + z)
    return out


def bath_frames():
    """Soaks in a warm pool with an orange on its head: floats up and down a pixel, steam rises,
    ripples spread, bubbles pop."""
    out = []
    pool_y = 46
    surface = pool_y + WATER_ROW
    for t in range(16):
        bob = 1 if t % 8 >= 4 else 0
        lag = 1 if (t - 1) % 8 >= 4 else 0     # the orange follows the head a frame later
        hx, hy = 6, 27 + bob
        layers = list(ground(SHADOW_POOL))
        layers.append(L(POOL, 2, pool_y))
        layers.append(L(BODY, 19, 33 + bob))
        layers += head(hx, hy, eyes='shut' if t in (10, 11) else 'calm')
        layers.append(L(ORANGE, hx + 6, 27 + lag - 9))
        layers.append(L(POOL_FRONT, 2, surface, ring=False))
        layers.append(L(['q' * 38], 20, surface, ring=False, clip=True))
        # Ripples spread from the chest and from the back
        for start, x in ((0, 14), (4, 50)):
            age = (t - start) % 8
            if age < 6:
                rows = RIPPLE_SMALL if age < 3 else RIPPLE_BIG
                layers.append(L(rows, x - len(rows[0]) // 2, surface + 2 + age // 3, ring=False, clip=True))
        # A bubble rises and pops
        for start, x, y in ((2, 30, 55), (10, 42, 56)):
            age = (t - start) % 16
            if age < 3:
                layers.append(L(BUBBLE if age < 2 else BUBBLE_POP, x, y - age, ring=False, clip=True))
        # Steam drifts up from the water and thins out
        for start, x in ((0, 34), (5, 54), (10, 45)):
            age = (t - start) % 16
            if age < 8:
                rows = STEAM[(age // 2) % 2] if age < 6 else STEAM_THIN
                layers.append(L(rows, x, surface - 2 - 2 * age))
        out.append(layers)
    return out


def leg_x(f, phase, base):
    """Where a leg is (and how high it is lifted) in walk frame f: planted for five frames, moving
    back exactly by WALK_SHIFTS while the body goes forward, then swung forward in three."""
    p = (f + phase) % 8
    start = f - p
    if p <= 4:
        return base + sum(WALK_SHIFTS[k % 8] for k in range(start + 1, f + 1)), 0
    travel = sum(WALK_SHIFTS[k % 8] for k in range(start + 1, start + 5))
    back = base + travel
    return back - round(travel * (p - 4) / 4), (1, 2, 1)[p - 5]


def walking(f):
    """Slow, swaying walk, frame f of 8: diagonal pairs of legs step together, the planted ones
    moving back by WALK_SHIFTS; the body dips as the feet land, the head follows a frame later."""
    legs = [leg_x(f, offset, -2) for offset in (0, 4, 0, 4)]
    bob = (1, 0, 0, 0, 1, 0, 0, 0)
    return standing(dy=bob[f], head_dy=bob[f - 1] - bob[f], legs=legs)


def walk_frames():
    return [walking(f) for f in range(8)]


def turn_frames():
    """Turning round (PetBehavior plays them there and back, mirrored on the way back)."""
    return [turning(1), turning(2)]


def lie_down_frames():
    return [lowered()]


def get_up_frames():
    return [lowered()]


def lie_in_frames():
    return [lowered()]


def lie_out_frames():
    return [lowered()]


def bath_step():
    """Stepping into (or out of) its warm pool, the water up to its knees."""
    surface = 46 + WATER_ROW
    layers = list(ground(SHADOW_POOL)) + [L(POOL, 2, 46)]
    layers += standing(dy=2, shadow=False)
    layers.append(L(POOL_FRONT, 2, surface, ring=False))
    return layers


def bath_in_frames():
    return [bath_step()]


def bath_out_frames():
    return [bath_step()]


# --- the backgrounds' interactions (BackgroundEvents) -------------------------------------

def leaf_hat_frames():
    """Wears the maple leaf that settled on its head, calm as ever: breathes, shuts its eyes with
    contentment, flicks an ear."""
    calm = standing(hat=True)
    inhale = standing(breath=1, hat=True)
    return frames(
        (calm, 2), (inhale, 2), (calm, 1),
        (standing(eyes='shut', hat=True), 2),
        (calm, 1), (standing(ear=1, hat=True), 1), (calm, 1),
        (standing(eyes='happy', hat=True), 3),
        (inhale, 2), (calm, 1),
    )


def lie_frames():
    """Lies down on its belly, awake and content (in the puddle, on the warm field): slow breaths, a
    blink, an ear flick, a happy squint."""
    breath = [0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0]
    eyes = ['calm'] * 5 + ['shut'] + ['calm'] * 3 + ['happy'] * 2 + ['calm']
    return [lying(breath[t], eyes[t], ear=1 if t == 7 else 0) for t in range(12)]


ANIMATIONS = {
    'sleep': {'interval': 170, 'frames': sleep_frames},
    'happy': {'interval': 110, 'frames': happy_frames},
    'idle': {'interval': 150, 'frames': idle_frames},
    'eat': {'interval': 120, 'frames': eat_frames},
    'bath': {'interval': 150, 'frames': bath_frames},
    'bath_in': {'interval': 150, 'frames': bath_in_frames},
    'bath_out': {'interval': 150, 'frames': bath_out_frames},
    'walk': {'interval': 180, 'frames': walk_frames},
    'turn': {'interval': 180, 'frames': turn_frames},
    'lie_down': {'interval': 170, 'frames': lie_down_frames},
    'get_up': {'interval': 170, 'frames': get_up_frames},
    'leaf_hat': {'interval': 150, 'frames': leaf_hat_frames},
    'lie': {'interval': 170, 'frames': lie_frames},
    'lie_in': {'interval': 170, 'frames': lie_in_frames},
    'lie_out': {'interval': 170, 'frames': lie_out_frames},
}
# Played once inside other animations, never looped (check.py does not compare their seams)
ONE_SHOT = ('turn', 'lie_down', 'get_up', 'bath_in', 'bath_out', 'lie_in', 'lie_out')
# Feet on row 59 (its outline on row 60) like the other pets
OFF_GROUND = ()
