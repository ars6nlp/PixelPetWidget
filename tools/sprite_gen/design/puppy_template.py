"""Puppy: a corgi pup, cheerful, energetic and kind. Faces left on a 64x64 canvas, its paws on row 59
(the outline under them on row 60) over a ground shadow.

Red-ginger with a white mask (a blaze down the forehead that widens into the muzzle), fluffy white
cheeks and chest, white socks, big upright ears pink inside, a long body on short legs, a fluffy
rear and a little tail, round dark eyes with a spark of light, a shiny black nose, a red collar
with a gold tag. Drawn from text matrices: big round shapes carry their own three tones (light
from the top left), the engine adds the one-pixel outline. Key poses are held by repeating a
frame, since the widget plays every frame of an animation for the same time.

Every loop begins and ends in the same sitting pose (the idle's first frame), so one activity
follows another without a jump; the walk starts and stops through a half-standing pose, a turn
goes through the pup facing you, sleep through lying down; gnawing the bone, spinning after its
tail and nosing the pumpkin have their own way in and out. Spinning shows the far side by
mirroring (sg.Moved), so it needs few drawings. The walk moves its planted paws by exactly
WALK_SHIFTS (PetSpec.PUPPY_GAIT), so on the widget they never slide.
"""
import copy

from sprite_gen import L, Moved, part

PALETTE = {
    'K': '#1f2133',  # outline (deep blue-grey, shared by all pixel pets); eyes, nose
    'O': '#e0803a',  # red-ginger fur
    'y': '#f6b26b',  # fur highlight
    'o': '#b55a28',  # fur shadow, far legs
    'W': '#fff8ee',  # white mask, cheeks, chest, socks; eye and nose shine; bone
    'w': '#e6d8c6',  # white shadow
    'x': '#8f6a58',  # soft inner lines, mouth
    'P': '#f29aa8',  # tongue, inner ears, blush
    'p': '#c9677a',  # tongue shadow, open mouth
    'R': '#e0505a',  # collar, heart, bow, Santa hat
    'Y': '#f5c84c',  # collar tag, witch hat band
    's': '#3f3a4a',  # witch hat
    'z': '#c3cdf0',  # sleep "Z", water drops
    'S': '#3b3d52',  # ground shadow
}
OUTLINE = 'K'
# Contour lines inside the figure take a soft colour from the fill next to them (darkest wins)
LINE_OF = {'O': 'o', 'y': 'o', 'o': 'x', 'x': 'x', 'W': 'w', 'w': 'x', 'P': 'p', 'p': 'p', 'R': 'p', 'Y': 'o',
           's': 'K', 'z': 'w'}
SHADE = []                         # every part carries its own tones
CLEAN_ORPHANS = True               # stray pixels where layers meet take their neighbours' colour
# Single pixels that are details on purpose: (colour, a neighbour colour it must touch)
ORPHAN_OK = (('W', 'K'), ('W', 'Y'), ('Y', 'R'), ('p', 'P'), ('P', 'p'), ('P', 'O'), ('P', 'y'), ('x', 'W'),
             ('x', 'w'), ('z', 'z'), ('w', 'W'))

FAR = {'O': 'o', 'y': 'O', 'W': 'w', 'P': 'p'}
# How far the planted paws move back in each walk frame (art pixels): PetSpec.PUPPY_GAIT
WALK_SHIFTS = (2, 2, 2, 1, 2, 2, 2, 1)

# --- big shapes (generated with three tones) ----------------------------------------------

HEAD = part('''
{HEAD}
''')

BODY = part('''
{BODY}
''')

BODY_FRONT = part('''
{BODY_FRONT}
''')

BODY_34 = part('''
{BODY_34}
''')

BODY_BACK = part('''
{BODY_BACK}
''')

CHEST = part('''
{CHEST}
''')

MUZZLE = part('''
{MUZZLE}
''')

BLAZE = part('''
{BLAZE}
''')

HAUNCH = part('''
{HAUNCH}
''')

WALK_BODY = part('''
{WALK_BODY}
''')

LOAF = part('''
{LOAF}
''')

BACK_BODY = part('''
{BACK_BODY}
''')

SHADOW_SIT = part('''
{SHADOW_SIT}
''')

SHADOW_SMALL = part('''
{SHADOW_SMALL}
''')

SHADOW_LONG = part('''
{SHADOW_LONG}
''')

# --- small parts ----------------------------------------------------------------------------

# Big upright ear, pink inside; the far one is mirrored and darker
EAR = part('''
    .y.......
    .yy......
    .yOo.....
    .yPOo....
    yyPPOo...
    yOPPPOo..
    yOPPPPOo.
    OOPPPPPOo
    OOPPPPPOo
    OOOPPPPOO
    OOOOOOOOO
''')
# The near ear turned back for a moment (a wiggle, flying when it shakes)
EAR_BACK = part('''
    .........
    .........
    .........
    yy.......
    yOOo.....
    yPPOOo...
    OPPPPOOo.
    OOPPPPPOo
    OOPPPPPOo
    OOOPPPPOO
    OOOOOOOOO
''')
# The ear from behind: all fur
EAR_REAR = part('''
    .y.......
    .yy......
    .yOo.....
    .yOOo....
    yyOOOo...
    yOOOOOo..
    yOOOOOOo.
    OOOOOOOOo
    OOOOOOOOo
    OOOOOOOOO
    OOOOOOOOO
''')

# Round dark eyes with a spark of light
EYE = part('''
    .KK.
    KWKK
    KKKK
    KKKK
    .KK.
''')
EYE_HALF = part('''
    xxxx
    KKKK
    .KK.
''')
EYE_SHUT = part('''
    xxxx
''')
EYE_HAPPY = part('''
    .xx.
    x..x
''')
# A shiny black nose
NOSE = part('''
    KWKK
    .KK.
''')
MOUTH = part('''
    x....x
    .xxxx.
''')
# Panting: the tongue out
MOUTH_OPEN = part('''
    x....x
    .xppx.
    ..PP..
    ..Pp..
''')
MOUTH_CHEW = part('''
    .xxxx.
    x....x
''')
BLUSH = part('''
    PP
''')
# Fluffy white cheeks, sticking out of the head's outline a little
TUFT = part('''
    .W
    WW
    .W
''')
# Fluff on the chest, and on the rear
CHEST_FLUFF = part('''
    .WW
    WWW
    .WW
    WWW
    .Ww
''')
REAR_FLUFF = part('''
    WW.
    WWW
    Ww.
''')

# Front legs of the sitting pup: short and white
LEG_SIT = part('''
    WWWW
    WWWw
    WWWw
    WWWw
    WWWw
    wWwW
''')
# A walking leg: ginger above, a white sock
LEG = part('''
    OOOO
    OOOo
    OOoo
    WWWW
    WWWw
    wwww
''')
# A hind leg: a thick thigh
LEG_BACK = part('''
    OOOOO
    OOOOo
    .OOoo
    .WWWW
    .WWWw
    .wwww
''')

# The fluffy little tail, white at the tip, in three wags
TAIL = [part('''
    ....yyy.
    ..yyyOOy
    .yOOOOOo
    yOOOOWWo
    .oOOWWw.
    ..oooo..
'''), part('''
    .....yyy
    ...yyyOo
    ..yOOOOo
    .yOOOWWo
    yOOOWWw.
    .ooooo..
'''), part('''
    ........
    ..yyyyy.
    .yyOOOOy
    yOOOOOWW
    .oOOOWWw
    ..oooww.
''')]
# The tail seen from behind, up and wagging
TAIL_UP = part('''
    .yy.
    yOOo
    yOOo
    yWWo
    .Ww.
''')

# The head in profile, the snout to the left (nosing the pumpkin): ear up behind, one eye
HEAD_SIDE = part('''
{HEAD_SIDE}
''')
# Its eye and nose (decals)
EYE_SIDE = part('''
    KK
    WK
''')
NOSE_SIDE = part('''
    KK
    K.
''')

COLLAR = part('''
    RRRRRRRRRRRR
    pRRRRRRRRRRp
''')
# The gold tag, with a glint
TAG = part('''
    WY
    YY
''')

# A bone to chew
BONE = part('''
    WW......WW
    WWWWWWWWWW
    .wWWWWWWw.
    WWwwwwwwWW
    ww......ww
''')

HEART = part('''
    .RR.RR.
    RRRRRRR
    RRRRRRR
    .RRRRR.
    ..RRR..
    ...R...
''')
DROP = part('''
    z
    z
''')
Z_SMALL = part('''
    zzzz
    ..z.
    .z..
    zzzz
''')
Z_BIG = part('''
    zzzzz
    ...z.
    ..z..
    .z...
    zzzzz
''')

# --- accessories ------------------------------------------------------------------------------

ACCESSORIES = ('bow', 'halloween_hat', 'winter_hat')
NO_ACCESSORY = ()
wearing = None

BOW = part('''
    RR...RR
    RRR.RRR
    RRRpRRR
    RRR.RRR
    RR...RR
''')

WITCH_HAT = part('''
    .........ss...
    ........ss....
    .......sss....
    ......ssss....
    .....sssss....
    ....ssssss....
    ....YYYYYY....
    .ssssssssssss.
    ssssssssssssss
''')

SANTA_HAT = part('''
    ....RRRR....
    ...RRRRRR...
    ..RRRRRRRRpW
    .RRRRRRRRR.W
    WWWWWWWWWWW.
    wwwwwwwwww..
''')


def accessory(hx, hy):
    """The worn accessory, placed relative to the head's top-left corner."""
    if wearing == 'bow':
        return [L(BOW, hx + 14, hy - 1)]
    if wearing == 'halloween_hat':
        return [L(WITCH_HAT, hx + 6, hy - 8)]
    if wearing == 'winter_hat':
        return [L(SANTA_HAT, hx + 7, hy - 5)]
    return []


def accessory_icon(name):
    """The accessory on its own, for the wardrobe in the app."""
    rows = {'bow': BOW, 'halloween_hat': WITCH_HAT, 'winter_hat': SANTA_HAT}[name]
    return [L(rows, 1, 1)], (len(rows[0]) + 2, len(rows) + 2)


# --- the head -------------------------------------------------------------------------------

def ground(rows=SHADOW_SIT):
    return [L(rows, 31 - len(rows[0]) // 2, 61, ring=False)]


def face(hx, hy, eyes='open', mouth='closed', look=0, up=0):
    """The mask (blaze and muzzle), eyes, blush, nose and mouth, relative to the head's top-left
    corner; up raises the face inside the head (the nose high, the chin showing)."""
    kw = dict(ring=False)
    layers = [L(BLAZE, hx + 10 + look, hy + 2 - up, clip=True, **kw),
              L(MUZZLE, hx + 6 + look, hy + 11 - up, clip=True, **kw)]
    if up:
        layers.append(L(MUZZLE, hx + 6, hy + 13, clip=True, **kw))
    nx, fx, ey = hx + 5 + look, hx + 17 + look, hy + 7 - up
    if eyes == 'open':
        layers += [L(EYE, nx, ey, **kw), L(EYE, fx, ey, **kw)]
    elif eyes == 'up':
        layers += [L(EYE, nx, ey - 1, **kw), L(EYE, fx, ey - 1, **kw)]
    elif eyes == 'half':
        layers += [L(EYE_HALF, nx, ey + 2, **kw), L(EYE_HALF, fx, ey + 2, **kw)]
    elif eyes in ('shut', 'sleep'):
        layers += [L(EYE_SHUT, nx, ey + 3, **kw), L(EYE_SHUT, fx, ey + 3, **kw)]
    elif eyes == 'happy':
        layers += [L(EYE_HAPPY, nx, ey + 2, **kw), L(EYE_HAPPY, fx, ey + 2, **kw)]
    layers += [L(BLUSH, nx - 2, hy + 13 - up, clip=True, **kw), L(BLUSH, fx + 4, hy + 13 - up, clip=True, **kw)]
    layers.append(L(NOSE, hx + 11 + look, hy + 12 - up, **kw))
    mouths = {'closed': MOUTH, 'open': MOUTH_OPEN, 'chew': MOUTH_CHEW}
    layers.append(L(mouths[mouth], hx + 10 + look, hy + 15 - up, **kw))
    return layers


def head(hx, hy, eyes='open', mouth='closed', look=0, ear_near=0, ear_far=0, ear_back=False, back=False, up=0,
         front=False):
    """Fluffy cheeks, ears, the round head and the face (back: seen from behind, no face); ear_*
    lift (-) or drop (+) an ear, ear_back folds the near one; front: both ears alike."""
    layers = []
    if not back:
        layers += [L(TUFT, hx - 1, hy + 13), L(TUFT, hx + 25, hy + 13, flip_h=True)]
    layers += [
        L(EAR_REAR if back else EAR, hx + 18, hy - 10 + ear_far, flip_h=True, recolor=None if front or back else FAR),
        L(EAR_REAR if back else EAR_BACK if ear_back else EAR, hx - 1, hy - 10 + ear_near),
        L(HEAD, hx, hy),
    ]
    if not back:
        layers += face(hx, hy, eyes, mouth, look, up)
    return layers + accessory(hx, hy)


def collar(hx, hy):
    return [L(COLLAR, hx + 7, hy + 19), L(TAG, hx + 12, hy + 21)]


# --- poses ----------------------------------------------------------------------------------

BODY_X, BODY_Y = 24, 40
HEAD_X, HEAD_Y = 10, 21


def sitting(dy=0, squash=0, head_dx=0, head_dy=0, eyes='open', mouth='closed', look=0, ear_near=0, ear_far=0,
            ear_back=False, tail=0, effects=(), shadow=True, props=(), front=(), up=0):
    """Sitting up, the tail wagging behind. dy lifts the whole pup (hops), squash > 0 flattens the
    body, < 0 stretches it; head_* move only the head."""
    lift = max(squash, 0) - max(-squash, 0)
    body_top = BODY_Y + dy + lift
    hx = HEAD_X + head_dx
    hy = HEAD_Y + dy + head_dy + lift
    layers = list(ground(SHADOW_SIT if dy > -3 else SHADOW_SMALL)) if shadow else []
    layers.append(L(TAIL[tail], 44, 46 + dy))
    layers.append(L(BODY, BODY_X, body_top, squash_y=max(squash, 0), grow_y=max(-squash, 0)))
    layers.append(L(HAUNCH, 35, 49 + dy, squash_y=1 if squash > 1 else 0))
    layers.append(L(REAR_FLUFF, 45, 53 + dy))
    layers.append(L(CHEST, BODY_X, body_top + 1, ring=False, clip=True))
    layers.append(L(CHEST_FLUFF, BODY_X - 1, body_top + 6))
    layers.append(L(LEG_SIT, 31, 54 + dy, recolor=FAR))
    layers.append(L(LEG_SIT, 25, 54 + dy))
    layers += list(props)
    layers += head(hx, hy, eyes, mouth, look, ear_near, ear_far, ear_back, False, up)
    layers += collar(hx, hy)
    return layers + list(front) + list(effects)


def turning(step):
    """Sitting and turning round to face you: step 1 three quarters, step 2 straight on."""
    layers = list(ground(SHADOW_SIT))
    if step == 2:
        layers.append(L(TAIL[1], 42, 46))
        layers.append(L(BODY_FRONT, 20, BODY_Y + 1))
        layers.append(L(CHEST_FLUFF, 27, BODY_Y + 7))
        layers.append(L(LEG_SIT, 25, 54))
        layers.append(L(LEG_SIT, 34, 54))
        return layers + head(19, HEAD_Y, front=True) + collar(19, HEAD_Y)
    layers.append(L(TAIL[0], 43, 46))
    layers.append(L(BODY, 22, BODY_Y))
    layers.append(L(HAUNCH, 33, 49))
    layers.append(L(CHEST, 23, BODY_Y + 1, ring=False, clip=True))
    layers.append(L(CHEST_FLUFF, 22, BODY_Y + 6))
    layers.append(L(LEG_SIT, 31, 54, recolor=FAR))
    layers.append(L(LEG_SIT, 25, 54))
    return layers + head(14, HEAD_Y) + collar(14, HEAD_Y)


def frames(*pattern):
    """Expands (layers, count) pairs: a key pose held for count frames."""
    out = []
    for layers, count in pattern:
        out += [layers] * count
    return out


def shifted(layers, dx, dy=0):
    """The layers moved (copies: frames share their layers)."""
    out = []
    for layer in layers:
        moved = copy.copy(layer)
        moved.x += dx
        moved.y += dy
        out.append(moved)
    return out


def lying(breath=0, eyes='sleep', mouth='closed', head_dy=0, tail=0, bone=None, ear_back=False, look=0):
    """Lying down, front paws forward, chin up or down on the paws; bone: a bone held in the mouth,
    at that position relative to the head."""
    layers = list(ground(SHADOW_LONG))
    layers.append(L(TAIL[tail], 47, 48))
    layers.append(L(LOAF, 22, 47 - breath, grow_y=breath))
    layers.append(L(REAR_FLUFF, 51, 54))
    layers.append(L(LEG, 17, 54, recolor=FAR))
    layers.append(L(LEG, 12, 54))
    hx, hy = 6, 32 + head_dy
    layers += head(hx, hy, eyes, mouth, look=look, ear_back=ear_back)
    if bone is not None:
        # Held across the mouth, it moves with the head
        layers.append(L(BONE, hx + bone[0], hy + bone[1]))
    return layers


def leg_x(f, phase, base):
    """Where a walking leg is (and how high it is lifted) in walk frame f: planted for five frames,
    moving back exactly by WALK_SHIFTS while the body goes forward, then swung forward in three."""
    p = (f + phase) % 8
    start = f - p
    if p <= 4:
        return base + sum(WALK_SHIFTS[k % 8] for k in range(start + 1, f + 1)), 0
    travel = sum(WALK_SHIFTS[k % 8] for k in range(start + 1, start + 5))
    back = base + travel
    return back - round(travel * (p - 4) / 4), (1, 2, 1)[p - 5]


# Legs: (base x, phase, far, hind); diagonal pairs move together
LEGS = ((22, 4, True, False), (37, 0, True, True), (18, 0, False, False), (33, 4, False, True))


def walking(f, mouth='open'):
    """A brisk trot on short legs, frame f of 8: the long body dips a pixel when both pairs of paws
    are down, the head follows a frame later, the tail wags two frames behind."""
    bob = 1 if f % 4 == 0 else 0
    head_bob = 1 if (f - 1) % 4 == 0 else 0
    tail = (0, 1, 2, 1)[((f - 2) % 8) // 2]

    def leg(i):
        base, phase, far, hind = LEGS[i]
        x, lift = leg_x(f, phase, base)
        return L(LEG_BACK if hind else LEG, x, 54 - lift, recolor=FAR if far else None)

    layers = list(ground(SHADOW_LONG))
    layers.append(L(TAIL[tail], 47, 39 + bob))
    layers += [leg(0), leg(1)]
    layers.append(L(WALK_BODY, 20, 41 + bob))
    layers.append(L(REAR_FLUFF, 48, 48 + bob))
    layers += [leg(2), leg(3)]
    hx, hy = 4, 22 + head_bob
    layers += head(hx, hy, mouth=mouth)
    layers += collar(hx, hy)
    return layers


def half_standing(step):
    """Between sitting and walking: step 1 the hindquarters rise, step 2 nearly up."""
    drop = 2 if step == 1 else 1
    layers = list(ground(SHADOW_LONG))
    layers.append(L(TAIL[0], 47, 40 + drop + (3 if step == 1 else 0)))
    for base, _, far, hind in LEGS[:2]:
        layers.append(L(LEG_BACK if hind else LEG, base + (2 if step == 1 else 1), 54 + drop,
                        recolor=FAR if far else None, squash_y=drop))
    layers.append(L(WALK_BODY, 20 + (2 if step == 1 else 1), 41 + drop + (2 if step == 1 else 0)))
    for base, _, far, hind in LEGS[2:]:
        layers.append(L(LEG_BACK if hind else LEG, base + (2 if step == 1 else 1), 54 + drop,
                        recolor=FAR if far else None, squash_y=drop))
    hx, hy = (6, 21 + drop) if step == 1 else (4, 21 + drop)
    return layers + head(hx, hy) + collar(hx, hy)


def standing_view(view):
    """Standing, seen from three quarters in front ('34f'), straight in front ('front'), three
    quarters behind ('34b') or straight behind ('back'): the turns of a spin after its tail."""
    layers = list(ground(SHADOW_LONG))
    if view == 'front':
        layers.append(L(TAIL[1], 44, 40))
        layers.append(L(LEG_BACK, 21, 54, recolor=FAR))
        layers.append(L(LEG_BACK, 37, 54, recolor=FAR, flip_h=True))
        layers.append(L(BODY_FRONT, 20, 39))
        layers.append(L(LEG, 26, 54))
        layers.append(L(LEG, 33, 54))
        return layers + head(19, 21, mouth='open', front=True) + collar(19, 21)
    if view == 'back':
        layers.append(L(LEG, 26, 54, recolor=FAR))
        layers.append(L(LEG, 33, 54, recolor=FAR))
        layers += head(19, 24, back=True)
        layers.append(L(BODY_BACK, 20, 38))
        layers.append(L(LEG_BACK, 21, 54))
        layers.append(L(LEG_BACK, 37, 54, flip_h=True))
        layers.append(L(TAIL_UP, 30, 35))
        return layers
    if view == '34f':
        layers.append(L(TAIL[2], 42, 39))
        layers.append(L(LEG, 22, 54, recolor=FAR))
        layers.append(L(LEG_BACK, 36, 54, recolor=FAR))
        layers.append(L(BODY_34, 22, 41))
        layers.append(L(LEG, 18, 54))
        layers.append(L(LEG_BACK, 32, 54))
        return layers + head(8, 22, mouth='open', look=1) + collar(8, 22)
    # Three quarters behind: the head turned away, the rear and the tail towards you
    layers.append(L(LEG, 22, 54, recolor=FAR))
    layers += head(10, 21, back=True)
    layers.append(L(BODY_34, 21, 41))
    layers.append(L(REAR_FLUFF, 42, 48))
    layers.append(L(LEG, 18, 54))
    layers.append(L(LEG_BACK, 31, 54))
    layers.append(L(LEG_BACK, 36, 54, recolor=FAR))
    layers.append(L(TAIL_UP, 40, 37))
    return layers


# --- animations -----------------------------------------------------------------------------

def idle_frames():
    """Breathes, pants a little (the tongue in and out), blinks through half-shut eyes, wiggles an
    ear, tilts its head and looks aside; the tail wags a frame behind."""
    tails = [0, 0, 1, 1, 2, 2, 1, 1, 0, 0, 0, 1, 1, 2, 2, 1, 0, 0, 0, 0, 1, 1, 0, 0]
    breath = [0, 0, -1, -1, 0, 0, 0, 0, 0, 0, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]

    def pose(t, **kw):
        return sitting(tail=tails[t], squash=breath[t], **kw)
    return [
        pose(0), pose(1, mouth='open'), pose(2, mouth='open'), pose(3, mouth='open'), pose(4, mouth='open'),
        pose(5), pose(6, eyes='half'), pose(7, eyes='shut'), pose(8, eyes='half'), pose(9), pose(10, ear_back=True),
        pose(11), pose(12, head_dx=1, ear_far=1), pose(13, head_dx=1, ear_far=1, look=1),
        pose(14, head_dx=1, ear_far=1, look=1), pose(15, head_dx=1, ear_far=1, look=1, mouth='open'),
        pose(16, head_dx=1, ear_far=1), pose(17), pose(18, ear_near=-1), pose(19), pose(20, mouth='open'),
        pose(21, mouth='open'), pose(22), pose(23),
    ]


def happy_frames():
    """Jumps for joy: crouches, springs up with the tongue out, hangs a moment, lands with a squash
    and settles, never more than two pixels a frame; the ears and the tail follow a frame later, a
    heart floats up."""
    poses = [(0, 0), (0, 1), (0, 2), (0, 2), (-2, -1), (-4, -1), (-6, 0), (-7, 0), (-7, 0), (-6, 0), (-4, 0), (-2, 0),
             (0, 2), (0, 1), (0, 0), (0, 0)]

    def heart(t):
        return [L(HEART, 46 + (t - 4) // 3, 18 - 2 * (t - 4))] if 4 <= t <= 11 else []
    out = []
    for t, (dy, sq) in enumerate(poses):
        before = poses[t - 1][0] if t else 0
        rising = dy < before
        ears = 1 if rising else -1 if dy < 0 and not rising else 0
        out.append(sitting(dy=dy, squash=sq, tail=t % 3 if dy < 0 else 0, eyes='happy' if 3 <= t <= 13 else 'open',
                           mouth='open' if 3 <= t <= 13 else 'closed', ear_near=ears, ear_far=ears,
                           effects=heart(t)))
    return out


def eat_frames():
    """Gnaws a bone held between the paws, lying down: the head bobs with each bite, a happy squint
    now and then."""
    out = []
    for t in range(16):
        bite = t % 4 in (1, 2)
        eyes = 'happy' if 8 <= t <= 10 else 'half' if t in (4, 5) else 'open'
        out.append(lying(eyes=eyes, mouth='chew' if bite else 'closed', head_dy=1 if bite else 0,
                         tail=(t // 2) % 3, bone=(8, 15)))
    return out


def eat_in_frames():
    """Lies down with its bone."""
    return [sitting(squash=2, head_dy=2, look=-1), sitting(squash=3, head_dy=5), lying(eyes='open', bone=(8, 15))]


def eat_out_frames():
    """Gets up, leaving the bone on the ground."""
    bone = [L(BONE, 14, 55)]
    return [lying(eyes='happy', bone=(8, 15)), sitting(squash=3, head_dy=5, front=bone), sitting(front=bone)]


def chase_frames():
    """Spins round after its own tail: from the side, three quarters, from the front, the other
    three quarters and side (the same drawings mirrored), from behind; the tail always just out of
    reach. One turn a loop."""
    side, side_b = walking(0), walking(2)
    side_c, side_d = walking(4), walking(6)
    front34, front, back34, back = (standing_view(v) for v in ('34f', 'front', '34b', 'back'))
    return [side, side_b, front34, front, Moved(front34, 0, 0, True), Moved(side_c, 0, 0, True),
            Moved(side_d, 0, 0, True), Moved(back34, 0, 0, True), back, back34]


def chase_in_frames():
    """Gets up to spin."""
    return [half_standing(1), half_standing(2)]


def chase_out_frames():
    """Dizzy, sits down again."""
    return [half_standing(1), sitting(eyes='happy', mouth='open')]


def sleep_frames():
    """Lying with the chin on its paws, fast asleep: slow breathing, an ear twitch, Z rise."""
    breath = [0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0]
    out = []
    for t in range(12):
        z = []
        for phase in (0, 6):
            a = (t + phase) % 12
            if a < 9:
                z.append(L(Z_SMALL if a < 4 else Z_BIG, 42 + a // 2 + (2 if phase else 0), 32 - 2 * a))
        out.append(lying(breath=breath[t], head_dy=4, ear_back=t == 7) + z)
    return out


def walk_frames():
    return [walking(f) for f in range(8)]


def turn_frames():
    """Turning round to face you (PetBehavior plays them there and back, mirrored on the way back)."""
    return [turning(1), turning(2)]


def walk_start_frames():
    return [half_standing(1), half_standing(2)]


def walk_stop_frames():
    return [half_standing(2), half_standing(1)]


def lie_down_frames():
    """From sitting down onto its belly, then the chin goes down onto the paws."""
    return [sitting(squash=3, head_dy=5, eyes='half'), lying(eyes='half')]


def get_up_frames():
    """Awake: the head comes up, then it sits up (the reaction goes on from there)."""
    return [lying(eyes='open'), sitting(squash=3, head_dy=5)]


# --- the backgrounds' interactions (BackgroundEvents) -------------------------------------

def on_back(t):
    """Lying on its back, paws in the air kicking, tongue out, the tail sweeping the ground."""
    kick = [0, 1, 0, -1][t % 4]
    layers = list(ground(SHADOW_LONG))
    layers.append(L(TAIL[t % 3], 49, 47))
    layers.append(L(BACK_BODY, 20, 46))
    for i, x in enumerate((25, 31, 41, 47)):
        k = kick if i % 2 == 0 else -kick
        layers.append(L(LEG, x, 39 + k, rot=2, recolor=FAR if i % 2 else None))
    hx, hy = 4, 39
    layers += head(hx, hy, eyes='happy', mouth='open', ear_near=4, ear_far=4, ear_back=True)
    return layers


def flop_frames():
    """Dizzy after spinning round after the falling leaf: lies down, rolls over on its back and kicks
    its paws in the air, tongue out, rolls back and sits up; then again."""
    return ([sitting(eyes='happy', mouth='open'), sitting(squash=3, head_dy=5, eyes='happy', mouth='open'),
             lying(eyes='happy', mouth='open')]
            + [on_back(t) for t in range(8)]
            + [lying(eyes='happy', mouth='open'), sitting(squash=3, head_dy=5, eyes='happy'), sitting()])


def look_up_frames():
    """Sits in the rain looking up, nose high, catching drops on it: one lands every 1.4 s (in
    step with this loop, BackgroundEvents.DROP_FALL_FRAMES), it blinks and sneezes, looks up again."""
    up = sitting(head_dy=-2, up=2, tail=0)
    return [
        sitting(), sitting(head_dy=-1, up=1, tail=1), up, sitting(head_dy=-2, up=2, tail=1), up,
        sitting(head_dy=-2, up=2, tail=2), up, up,
        sitting(head_dy=-1, eyes='shut', ear_back=True, tail=0),
        sitting(head_dy=1, squash=1, eyes='shut', mouth='open', ear_back=True, tail=2),
        sitting(head_dy=0, eyes='shut', mouth='open', tail=2),
        sitting(head_dy=-1, up=1, eyes='half', tail=1), sitting(head_dy=-1, up=1, tail=0), sitting(),
    ]


def shake_frames():
    """Shakes the rain off, nose to tail: the body swings out and back through a middle pose, the
    ears fly, drops spray off; sits still again at the end."""
    def spray(t):
        spots = [((6, 30), (52, 34), (14, 20)), ((2, 36), (56, 28), (44, 18)), ((8, 24), (58, 40), (20, 14)),
                 ((4, 40), (50, 22), (36, 12))]
        return [L(DROP, x, y, ring=False) for x, y in spots[t % 4]]
    swings = [0, -1, -2, -1, 0, 1, 2, 1, 0, -1, -2, -1, 0, 1, 0, 0]
    out = []
    for t, swing in enumerate(swings):
        if t >= 14:
            out.append(sitting(eyes='happy' if t == 14 else 'open', mouth='open' if t == 14 else 'closed'))
            continue
        pose = sitting(head_dx=swing, eyes='shut', ear_back=swing < 0, ear_far=-1 if swing > 0 else 1 if swing < 0 else 0,
                       tail=t % 3, effects=spray(t) if swing else (), shadow=False)
        # The body swings, the shadow stays
        out.append(ground() + shifted(pose, swing // 2))
    return out


# The play bow stands this far right of where the pup sits (beside the pumpkin): it is drawn where
# the pup sits and moved there (sg.Moved), like the hop over to it
BOW_SHIFT = 10


def bow(t):
    """A play bow beside the little pumpkin (the background draws it at the frame's middle, x 21-42,
    and wobbles it): the head in profile low, the nose on the pumpkin's side, the rear up."""
    # Its shadow stays under it: drawn where the pup sits, it moves along with the bow
    return Moved(ground(SHADOW_LONG) + shifted(bow_layers(t), -BOW_SHIFT), BOW_SHIFT, 0)


def bow_layers(t):
    """The play bow as it looks beside the pumpkin (without its shadow)."""
    push = [1, 0, -1, 0][t % 4]
    layers = []
    layers.append(L(TAIL[t % 3], 53, 34))
    layers.append(L(LEG_BACK, 49, 54, recolor=FAR))
    layers.append(L(BODY, 38, 36))
    layers.append(L(REAR_FLUFF, 58, 47))
    layers.append(L(LEG_BACK, 54, 54))
    layers.append(L(LEG, 40, 54, recolor=FAR))
    hx, hy = 33 - push, 37
    layers.append(L(EAR, hx + 13, hy - 8, recolor=FAR))
    layers.append(L(HEAD_SIDE, hx, hy))
    layers.append(L(EAR, hx + 9, hy - 7))
    layers.append(L(EYE_SIDE, hx + 7, hy + 7, ring=False))
    layers.append(L(NOSE_SIDE, hx, hy + 10, ring=False))
    layers.append(L(LEG, 44, 54))
    return layers


def nose_push_frames():
    """Noses the little pumpkin along, pushing in time with its wobble (4 frames at 120 ms), the
    rear up and the tail going."""
    return [bow(t) for t in range(12)]


def nose_push_in_frames():
    """Steps aside to the pumpkin's right and goes down into a play bow."""
    return [sitting(look=-1), Moved(sitting(squash=2, head_dy=3), 3, 0), Moved(sitting(squash=3, head_dy=6), 6, 0),
            Moved(sitting(squash=3, head_dy=6), 8, 0), Moved(sitting(squash=3, head_dy=8), BOW_SHIFT, 0), bow(3)]


def nose_push_out_frames():
    """Up from the bow, a hop back to the middle, sits."""
    return [bow(3), Moved(sitting(squash=3, head_dy=8), BOW_SHIFT, 0), Moved(sitting(squash=3, head_dy=6), 8, 0),
            Moved(sitting(squash=3, head_dy=6), 6, 0), Moved(sitting(squash=2, head_dy=3), 3, 0), sitting()]


ANIMATIONS = {
    'sleep': {'interval': 160, 'frames': sleep_frames},
    'happy': {'interval': 80, 'frames': happy_frames},
    'idle': {'interval': 110, 'frames': idle_frames},
    'eat': {'interval': 90, 'frames': eat_frames},
    'eat_in': {'interval': 90, 'frames': eat_in_frames},
    'eat_out': {'interval': 90, 'frames': eat_out_frames},
    'chase': {'interval': 100, 'frames': chase_frames},
    'chase_in': {'interval': 100, 'frames': chase_in_frames},
    'chase_out': {'interval': 100, 'frames': chase_out_frames},
    'walk': {'interval': 90, 'frames': walk_frames},
    'turn': {'interval': 90, 'frames': turn_frames},
    'walk_start': {'interval': 90, 'frames': walk_start_frames},
    'walk_stop': {'interval': 90, 'frames': walk_stop_frames},
    'lie_down': {'interval': 160, 'frames': lie_down_frames},
    'get_up': {'interval': 160, 'frames': get_up_frames},
    'flop': {'interval': 100, 'frames': flop_frames},
    'look_up': {'interval': 100, 'frames': look_up_frames},
    'shake': {'interval': 80, 'frames': shake_frames},
    'nose_push': {'interval': 120, 'frames': nose_push_frames},
    'nose_push_in': {'interval': 120, 'frames': nose_push_in_frames},
    'nose_push_out': {'interval': 120, 'frames': nose_push_out_frames},
}
# Played once inside other animations, never looped (check.py does not compare their seams)
ONE_SHOT = ('turn', 'walk_start', 'walk_stop', 'lie_down', 'get_up', 'eat_in', 'eat_out', 'chase_in', 'chase_out',
            'nose_push_in', 'nose_push_out')
# Poses that are not on the ground row (on its back, nosing the pumpkin): motion_check.py skips their feet
OFF_GROUND = ()
