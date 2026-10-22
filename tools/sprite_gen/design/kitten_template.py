"""Kitten: small, grey and striped, curious and a little lazy. Faces left on a 64x64 canvas, its paws
on row 59 (the outline under them on row 60) over a ground shadow.

A grey tabby with a cream chest and socks, a pink nose and ears, big green eyes with a slit pupil
and a spark of light, a tabby "M" on the forehead, whiskers and a blush, and a thin ringed tail
with a darker tip. Drawn from text matrices: big round shapes carry their own three tones (light
from the top left), the engine adds the one-pixel outline. Key poses are held by repeating a
frame, since the widget plays every frame of an animation for the same time.

Every loop begins and ends in the same sitting pose (the idle's first frame), so one activity
follows another without a jump; the walk starts and stops through a half-standing pose, a turn
goes through the kitten facing you, sleep through lying down. The walk moves its planted paws by
exactly WALK_SHIFTS (PetSpec.KITTEN_GAIT), so on the widget they never slide.
"""
import copy

from sprite_gen import L, Moved, part

PALETTE = {
    'K': '#1f2133',  # outline (deep blue-grey, shared by all pixel pets); pupils
    'G': '#b2afbb',  # silver-grey fur
    'L': '#d8d6de',  # fur highlight
    'g': '#8a8794',  # fur shadow, far legs
    's': '#5a5766',  # tabby stripes, tail tip, soft lines, mouth, witch hat
    'C': '#f4efe9',  # cream chest, muzzle, socks, whiskers, Santa hat trim
    'c': '#d7cec6',  # cream shadow
    'P': '#f4a0b0',  # nose, inner ears, blush, tongue, toe beans
    'p': '#c9677a',  # pink shadow, open mouth
    'E': '#86c96a',  # green eyes
    'W': '#ffffff',  # eye shine, milk
    'R': '#e2545f',  # yarn ball, milk bowl, heart, bow, Santa hat
    'r': '#a83a4a',  # red shadow, yarn strands
    'Y': '#f5c84c',  # witch hat band
    'z': '#c3cdf0',  # sleep "Z", purr notes
    'S': '#3b3d52',  # ground shadow
}
OUTLINE = 'K'
# Contour lines inside the figure take a soft colour from the fill next to them (darkest wins)
LINE_OF = {'G': 'g', 'L': 'g', 'g': 's', 's': 's', 'C': 'c', 'c': 'g', 'P': 'p', 'p': 'p', 'R': 'r', 'r': 'r',
           'W': 'c', 'E': 'K', 'Y': 's', 'z': 'g'}
SHADE = []                         # every part carries its own tones
CLEAN_ORPHANS = True               # stray pixels where layers meet take their neighbours' colour
# Single pixels that are details on purpose: (colour, a neighbour colour it must touch)
ORPHAN_OK = (('W', 'K'), ('W', 'E'), ('W', 'R'), ('W', 'C'), ('P', 's'), ('P', 'C'), ('P', 'G'), ('p', 'P'),
             ('s', 'G'), ('s', 'C'), ('s', 'L'), ('s', 'g'), ('E', 'K'), ('r', 'R'), ('z', 'z'), ('C', 'G'),
             ('C', 'L'), ('K', 'E'))

FAR = {'G': 'g', 'L': 'G', 'C': 'c', 'P': 'p', 's': 's'}
# How far the planted paws move back in each walk frame (art pixels): PetSpec.KITTEN_GAIT
WALK_SHIFTS = (2, 1, 2, 1, 2, 1, 2, 1)

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

HAUNCH = part('''
{HAUNCH}
''')

CHEST = part('''
{CHEST}
''')

MUZZLE = part('''
{MUZZLE}
''')

WALK_BODY = part('''
{WALK_BODY}
''')

LOAF = part('''
{LOAF}
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

# Tails: thin, ringed, with a darker tip (roots are where they leave the body)
TAIL_SIT = [part('''
{TAIL_SIT_0}
'''), part('''
{TAIL_SIT_1}
'''), part('''
{TAIL_SIT_2}
''')]
TAIL_SIT_ROOT = [{TAIL_SIT_0_ROOT}, {TAIL_SIT_1_ROOT}, {TAIL_SIT_2_ROOT}]

TAIL_WALK = [part('''
{TAIL_WALK_0}
'''), part('''
{TAIL_WALK_1}
'''), part('''
{TAIL_WALK_2}
''')]
TAIL_WALK_ROOT = [{TAIL_WALK_0_ROOT}, {TAIL_WALK_1_ROOT}, {TAIL_WALK_2_ROOT}]

TAIL_REST = part('''
{TAIL_REST}
''')
TAIL_REST_ROOT = {TAIL_REST_ROOT}

TAIL_HIGH = part('''
{TAIL_HIGH}
''')
TAIL_HIGH_ROOT = {TAIL_HIGH_ROOT}

TAIL_CURL = part('''
{TAIL_CURL}
''')
TAIL_CURL_ROOT = {TAIL_CURL_ROOT}

# --- small parts ----------------------------------------------------------------------------

# Big pointed ear, pink inside; the far one is mirrored and darker
EAR = part('''
    .L......
    .LL.....
    .LPG....
    .LPPG...
    LLPPPG..
    LGPPPPG.
    GGPPPPGG
    GGGGGGGG
''')
# The near ear turned back for a moment
EAR_FLICK = part('''
    ........
    ........
    LL......
    LPGG....
    LPPPGG..
    GGPPPPG.
    GGPPPPGG
    GGGGGGGG
''')
# Ears poking out of a heap of leaves: outlined by hand but for the bottom, which the leaves hide
EAR_POKE = part('''
    .K.......
    KLK......
    KLLK.....
    KLPGK....
    KLPPGK...
    KLPPPGK..
    KGPPPPGK.
    KGPPPPGGK
''')
EAR_POKE_FLICK = part('''
    .........
    .........
    KKK......
    KLLKK....
    KLPPGKK..
    KGPPPPGK.
    KGPPPPGGK
    KGGPPPGGK
''')

# The tabby "M" on the forehead: three bands that meet towards the nose
FOREHEAD = part('''
    s..s..s
    s..s..s
    .s.s.s.
    ...s...
''')

# Big round eyes: a dark pupil that runs down into a green iris, one pixel of light
EYE = part('''
    .KKKK.
    KWKKKK
    KKKKEK
    KEKKEK
    KEEEEK
    .KEEK.
''')
# Wide open (surprised, watching): a round pupil
EYE_WIDE = part('''
    .KKKK.
    KWKKKK
    KKKKKK
    KKKKEK
    KEKKEK
    KEEEEK
    .KEEK.
''')
EYE_HALF = part('''
    ssssss
    KEKKEK
    .KEEK.
''')
# Asleep: a line; happy: a little arch
EYE_SHUT = part('''
    .ssss.
''')
EYE_HAPPY = part('''
    ..ss..
    .s..s.
    s....s
''')

NOSE = part('''
    PPP
    .P.
''')
MOUTH = part('''
    .s.
    s.s
''')
MOUTH_OPEN = part('''
    sps
    .s.
''')
MOUTH_LICK = part('''
    .s.
    sPs
''')
BLUSH = part('''
    PP
''')
WHISKER = part('''
    CC
''')

# Front paws, resting, the toes marked
PAW = part('''
    .CCCC.
    CCCCCC
    CCCCCC
    cCcCcc
''')
# Near front leg raised: the paw up at the mouth (washing, the pink pad shows) or swiping
PAW_UP = part('''
    .CCC.
    CPPPC
    cCPCc
    GGGGG
    GGGGg
    .GGGg
    .GGGg
    .GGgg
    ..Ggg
''')
# Half raised (on its way up or down)
PAW_MID = part('''
    .CCC.
    CCCCC
    cCCCc
    GGGGg
    .GGGg
    .GGgg
''')
# Near front leg reaching forward, low (batting at the ball)
PAW_SWAT = part('''
    .CC......
    CCCCGGGGG
    cCCGGGGGg
    ..ccggggg
''')
# A walking leg: short and soft, a cream sock
LEG = part('''
    GGG
    GGG
    GGg
    GGg
    GGg
    CCC
    CCC
    CCc
    ccc
''')

# A hind leg: the thigh wider at the top
LEG_BACK = part('''
    GGGG
    GGGg
    .GGg
    .GGg
    .GGg
    .CCC
    .CCC
    .CCc
    .ccc
''')

# Milk in a red bowl
BOWL = part('''
    ..WWWWWWWWWW..
    .RWWWWWWWWWWR.
    RRRRRRRRRRRRRR
    RRRRRRRRRRRRrr
    .rRRRRRRRRRRr.
    ..rrrrrrrrrr..
''')
BOWL_RIPPLE = part('''
    ..WWWWWWWWWW..
    .RWWWWCCWWWWR.
    RRRRRRRRRRRRRR
    RRRRRRRRRRRRrr
    .rRRRRRRRRRRr.
    ..rrrrrrrrrr..
''')

# Ball of yarn, its strands turning as it rolls, and a loose end
YARN = [part('''
    ..RRRRR..
    .RRrRRRR.
    RRRRrRRRR
    RrRRRrRRr
    RRrRRRrRr
    RRRrRRRrr
    .RRRrRrr.
    ..rrrrr..
'''), part('''
    ..RRRRR..
    .RRRRrRR.
    RRrRRRrRR
    RRRrRRRrr
    RrRRrRRRr
    RRrRRrRrr
    .RRrRRrr.
    ..rrrrr..
''')]
THREAD = part('''
    rr...
    ..rr.
    ....r
''')

HEART = part('''
    .RR.RR.
    RRRRRRR
    RRRRRRr
    .RRRRr.
    ..RRr..
    ...r...
''')
NOTE = part('''
    .zzz
    .z.z
    zz.z
    zz..
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
    CCCCCCCCCCC.
    cccccccccc..
''')


def accessory(hx, hy):
    """The worn accessory, placed relative to the head's top-left corner."""
    if wearing == 'bow':
        return [L(BOW, hx + 19, hy - 3)]
    if wearing == 'halloween_hat':
        return [L(WITCH_HAT, hx + 7, hy - 9)]
    if wearing == 'winter_hat':
        return [L(SANTA_HAT, hx + 8, hy - 5)]
    return []


def accessory_icon(name):
    """The accessory on its own, for the wardrobe in the app."""
    rows = {'bow': BOW, 'halloween_hat': WITCH_HAT, 'winter_hat': SANTA_HAT}[name]
    return [L(rows, 1, 1)], (len(rows[0]) + 2, len(rows) + 2)


# --- the head -------------------------------------------------------------------------------

def at_root(rows, root, x, y, **kw):
    """A tail placed so that its root lands on (x, y)."""
    return L(rows, x - root[0], y - root[1], **kw)


def ground(rows=SHADOW_SIT, cx=31):
    return [L(rows, cx - len(rows[0]) // 2, 61, ring=False)]


def face(hx, hy, eyes='open', mouth='closed', look=0, cut=None):
    """Markings, muzzle, eyes, nose and mouth, relative to the head's top-left corner; look moves
    the eyes and the nose a pixel aside."""
    kw = dict(ring=False, cut=cut)
    layers = [L(FOREHEAD, hx + 11, hy + 2, clip=True, **kw),
              L(MUZZLE, hx + 9 + look, hy + 12, clip=True, **kw)]
    nx, fx, ey = hx + 4 + look, hx + 18 + look, hy + 7
    if eyes == 'open':
        layers += [L(EYE, nx, ey, **kw), L(EYE, fx, ey, **kw)]
    elif eyes == 'wide':
        layers += [L(EYE_WIDE, nx, ey - 1, **kw), L(EYE_WIDE, fx, ey - 1, **kw)]
    elif eyes == 'half':
        layers += [L(EYE_HALF, nx, ey + 3, **kw), L(EYE_HALF, fx, ey + 3, **kw)]
    elif eyes in ('shut', 'sleep'):
        layers += [L(EYE_SHUT, nx, ey + 4, **kw), L(EYE_SHUT, fx, ey + 4, **kw)]
    elif eyes == 'happy':
        layers += [L(EYE_HAPPY, nx, ey + 2, **kw), L(EYE_HAPPY, fx, ey + 2, **kw)]
    layers += [L(BLUSH, hx + 3 + look, hy + 14, clip=True, **kw), L(BLUSH, hx + 23 + look, hy + 14, clip=True, **kw),
               L(WHISKER, hx + 7 + look, hy + 14, clip=True, **kw), L(WHISKER, hx + 7 + look, hy + 16, clip=True, **kw),
               L(WHISKER, hx + 19 + look, hy + 14, clip=True, **kw),
               L(WHISKER, hx + 19 + look, hy + 16, clip=True, **kw)]
    layers.append(L(NOSE, hx + 12 + look, hy + 13, **kw))
    mouths = {'closed': MOUTH, 'open': MOUTH_OPEN, 'lick': MOUTH_LICK}
    layers.append(L(mouths[mouth], hx + 12 + look, hy + 14 if mouth != 'open' else hy + 15, **kw))
    return layers


def head(hx, hy, eyes='open', mouth='closed', look=0, ear_near=0, ear_far=0, flick=False, front=False, cut=None):
    """Ears, the round head and the face; ear_* lift (-) or drop (+) an ear, flick turns the near
    one back; front: both ears alike (facing you)."""
    layers = [
        L(EAR, hx + 19, hy - 6 + ear_far, flip_h=True, recolor=None if front else FAR, cut=cut),
        L(EAR_FLICK if flick else EAR, hx + 1, hy - 6 + ear_near, cut=cut),
        L(HEAD, hx, hy, cut=cut),
    ]
    return layers + face(hx, hy, eyes, mouth, look, cut) + accessory(hx, hy)


# --- poses ----------------------------------------------------------------------------------

BODY_X, BODY_Y = 22, 38
HEAD_X, HEAD_Y = 9, 17


def sitting(dy=0, squash=0, head_dx=0, head_dy=0, eyes='open', mouth='closed', look=0, ear_near=0, ear_far=0,
            flick=False, tail=0, tail_dy=None, paw='rest', paw_at=(0, 0), effects=(), shadow=True, props=(),
            front=()):
    """Sitting upright, the tail curling up behind. dy lifts the whole kitten (hops), squash > 0
    flattens the body, < 0 stretches it; head_* move only the head; tail_dy places the tail a
    frame behind the body (its last dy). paw: rest, mid (half raised), up (at paw_at, relative to
    the head) or swat (reaching forward low, at paw_at). props go behind the head, front in front."""
    lift = max(squash, 0) - max(-squash, 0)
    body_top = BODY_Y + dy + lift
    hx = HEAD_X + head_dx
    hy = HEAD_Y + dy + head_dy + lift
    layers = list(ground(SHADOW_SIT if dy > -3 else SHADOW_SMALL)) if shadow else []
    layers.append(at_root(TAIL_SIT[tail], TAIL_SIT_ROOT[tail], 43, 55 + (dy if tail_dy is None else tail_dy)))
    layers.append(L(BODY, BODY_X, body_top, squash_y=max(squash, 0), grow_y=max(-squash, 0)))
    layers.append(L(HAUNCH, 33, 48 + dy, squash_y=1 if squash > 1 else 0))
    layers.append(L(CHEST, BODY_X, body_top + 2, ring=False, clip=True))
    layers.append(L(PAW, 29, 56 + dy, recolor=FAR))
    if paw == 'rest':
        layers.append(L(PAW, 21, 56 + dy))
    elif paw != 'swat':
        layers.append(L(PAW, 21, 56 + dy, recolor=FAR))
    layers += list(props)
    layers += head(hx, hy, eyes, mouth, look, ear_near, ear_far, flick)
    if paw == 'up':
        layers.append(L(PAW_UP, hx + paw_at[0], hy + paw_at[1]))
    elif paw == 'mid':
        layers.append(L(PAW_MID, hx + paw_at[0], hy + paw_at[1]))
    elif paw == 'swat':
        layers.append(L(PAW_SWAT, paw_at[0], paw_at[1] + dy))
    return layers + list(front) + list(effects)


def turning(step):
    """Sitting and turning round to face you: step 1 three quarters, step 2 straight on."""
    if step == 2:
        layers = list(ground(SHADOW_SIT))
        layers.append(at_root(TAIL_CURL, TAIL_CURL_ROOT, 42, 51))
        layers.append(L(BODY_FRONT, 20, BODY_Y))
        layers.append(L(CHEST, 27, BODY_Y + 2, ring=False, clip=True))
        layers.append(L(PAW, 24, 56))
        layers.append(L(PAW, 34, 56))
        return layers + head(18, HEAD_Y, front=True)
    layers = list(ground(SHADOW_SIT))
    layers.append(at_root(TAIL_SIT[2], TAIL_SIT_ROOT[2], 42, 55))
    layers.append(L(BODY, 21, BODY_Y))
    layers.append(L(HAUNCH, 31, 48))
    layers.append(L(CHEST, 23, BODY_Y + 2, ring=False, clip=True))
    layers.append(L(PAW, 30, 56, recolor=FAR))
    layers.append(L(PAW, 23, 56))
    return layers + head(13, HEAD_Y)


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


def lying(x=16, y=45, breath=0, eyes='sleep', mouth='closed', tail=True, shadow=True, paw=None,
          ear_far=0, flick=False, head_up=10, look=0):
    """Lying down like a loaf (asleep, on the pumpkin): body, head resting forward on the paws;
    the tail curls up behind; paw: a near paw raised to the mouth (washing)."""
    layers = list(ground(SHADOW_LONG)) if shadow else []
    if tail:
        layers.append(at_root(TAIL_REST, TAIL_REST_ROOT, x + 29, y + 12))
    layers.append(L(LOAF, x + 2, y + 1 - breath, grow_y=breath))
    layers.append(L(PAW, x - 2, y + 11))
    hx, hy = x - 10, y - head_up + (1 if eyes == 'sleep' else 0)
    layers += head(hx, hy, eyes, mouth, look=look, ear_far=ear_far, flick=flick)
    if paw is not None:
        layers.append(L(PAW_UP, hx + paw[0], hy + paw[1]))
    return layers


def crouch(dx=0, dy=0, squash=0, tail=0, eyes='wide', look=0, cut=None, shadow=True):
    """Low and ready to spring, the tail up and twitching; cut hides it from that row down (in the
    heap of leaves)."""
    x, y = 20 + dx, 46 + dy
    layers = list(ground(SHADOW_LONG if dy > -2 else SHADOW_SMALL)) if shadow else []
    layers.append(at_root(TAIL_HIGH if tail == 0 else TAIL_WALK[1], TAIL_HIGH_ROOT if tail == 0 else TAIL_WALK_ROOT[1],
                          x + 28, y + 2, cut=cut))
    layers.append(L(LOAF, x, y + max(squash, 0), squash_y=max(squash, 0), cut=cut))
    layers.append(L(PAW, x - 8, y + 10, cut=cut))
    layers.append(L(PAW, x - 2, y + 10, recolor=FAR, cut=cut))
    hx, hy = x - 12, y - 14 + squash
    layers += head(hx, hy, eyes, look=look, cut=cut)
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


# Legs: (base x, phase, far); diagonal pairs move together
LEGS = ((20, 4, True), (35, 0, True), (16, 0, False), (31, 4, False))
HIND = (False, True, False, True)


def walking(f):
    """Soft little steps seen from the side, frame f of 8: the body dips a pixel when both pairs of
    paws are down, the head follows a frame later, the tail sways two frames behind."""
    bob = 1 if f % 4 == 0 else 0
    head_bob = 1 if (f - 1) % 4 == 0 else 0
    tail = (0, 1, 2, 1)[((f - 2) % 8) // 2]

    def leg(i):
        base_x, phase, far = LEGS[i]
        x, lift = leg_x(f, phase, base_x)
        return L(LEG_BACK if HIND[i] else LEG, x, 51 - lift, recolor=FAR if far else None)

    layers = list(ground(SHADOW_LONG))
    layers.append(at_root(TAIL_WALK[tail], TAIL_WALK_ROOT[tail], 43, 43 + bob))
    layers += [leg(0), leg(1)]
    layers.append(L(WALK_BODY, 19, 40 + bob))
    layers += [leg(2), leg(3)]
    layers += head(2, 21 + head_bob)
    return layers


def half_standing(step):
    """Between sitting and walking: step 1 the hindquarters rise, step 2 nearly up."""
    drop = 2 if step == 1 else 1
    layers = list(ground(SHADOW_LONG))
    layers.append(at_root(TAIL_WALK[0] if step == 2 else TAIL_SIT[0], TAIL_WALK_ROOT[0] if step == 2 else TAIL_SIT_ROOT[0],
                          43, 43 + drop + (4 if step == 1 else 0)))
    for i, (base, _, far) in enumerate(LEGS[:2]):
        layers.append(L(LEG_BACK if HIND[i] else LEG, base + (2 if step == 1 else 1), 51 + drop,
                        recolor=FAR if far else None, squash_y=drop))
    layers.append(L(WALK_BODY, 19 + (2 if step == 1 else 1), 40 + drop + (2 if step == 1 else 0)))
    for i, (base, _, far) in enumerate(LEGS[2:], 2):
        layers.append(L(LEG_BACK if HIND[i] else LEG, base + (2 if step == 1 else 1), 51 + drop,
                        recolor=FAR if far else None, squash_y=drop))
    layers += head(6 if step == 1 else 4, 19 + drop)
    return layers


# --- animations -----------------------------------------------------------------------------

def idle_frames():
    """Breathes, blinks through half-shut eyes, glances aside, flicks an ear, washes a paw (the paw
    goes up and down through a middle pose); the tail sways a frame behind."""
    tails = [0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 2, 2, 2, 2, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0]
    breath = [0, 0, -1, -1, -1, 0, 0, 0, 0, 0, -1, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]

    def pose(t, **kw):
        return sitting(tail=tails[t], squash=breath[t], **kw)
    return [
        pose(0), pose(1), pose(2), pose(3), pose(4, eyes='half'), pose(5, eyes='shut'), pose(6, eyes='half'),
        pose(7), pose(8, look=-1), pose(9, look=-1), pose(10, look=-1), pose(11, flick=True), pose(12),
        pose(13, paw='mid', paw_at=(7, 20), eyes='half'),
        pose(14, paw='up', paw_at=(7, 15), head_dy=1, eyes='half'),
        pose(15, paw='up', paw_at=(7, 14), head_dy=1, eyes='shut', mouth='lick'),
        pose(16, paw='up', paw_at=(7, 15), head_dy=1, eyes='half'),
        pose(17, paw='up', paw_at=(7, 14), head_dy=1, eyes='shut', mouth='lick'),
        pose(18, paw='up', paw_at=(7, 15), head_dy=1, eyes='half'),
        pose(19, paw='mid', paw_at=(7, 20), eyes='half'),
        pose(20, eyes='half'), pose(21), pose(22), pose(23),
    ]


def happy_frames():
    """A happy little hop: it crouches, springs up, hangs in the air a moment, lands with a squash
    and settles; never more than two pixels a frame. It purrs (notes) and a heart floats up; the
    tail and the ears follow the body a frame later."""
    poses = [(0, 0), (0, 1), (0, 2), (0, 2), (-2, -1), (-4, -1), (-6, 0), (-7, 0), (-7, 0), (-6, 0),
             (-4, 0), (-2, 0), (0, 2), (0, 1), (0, 0), (0, 0)]

    def float_up(t):
        layers = []
        if 4 <= t <= 11:
            age = t - 4
            layers.append(L(HEART, 44 + age // 3, 16 - 2 * age))
        if 7 <= t <= 13:
            layers.append(L(NOTE, 4, 14 - (t - 7)))
        return layers
    out = []
    for t, (dy, sq) in enumerate(poses):
        before = poses[t - 1][0] if t else 0
        rising = dy < before
        out.append(sitting(dy=dy, squash=sq, tail=1 if dy < 0 else 0, tail_dy=before,
                           eyes='happy' if 3 <= t <= 13 else 'open', mouth='open' if 4 <= t <= 11 else 'closed',
                           ear_near=1 if rising else -1 if dy < 0 and not rising else 0,
                           ear_far=1 if rising else -1 if dy < 0 and not rising else 0, effects=float_up(t)))
    return out


def eat_frames():
    """Drinks milk from a bowl: bends down, laps (the tongue in and out, the milk ripples), lifts
    its head and licks its lips, sits up again."""
    bowl = [L(BOWL, 9, 54)]
    ripple = [L(BOWL_RIPPLE, 9, 54)]

    def down(lap, eyes='half'):
        return sitting(squash=2, head_dx=-2, head_dy=9 + (1 if lap else 0), eyes=eyes,
                       mouth='lick' if lap else 'closed', tail=1 if lap else 0, front=ripple if lap else bowl)
    return [
        sitting(front=bowl), sitting(front=bowl, look=-1),
        sitting(squash=1, head_dx=-1, head_dy=4, eyes='half', front=bowl),
        down(False), down(True), down(False), down(True), down(False, 'shut'), down(True, 'shut'), down(False),
        down(True), down(False), down(True), down(False),
        sitting(squash=1, head_dx=-1, head_dy=4, eyes='half', front=bowl),
        sitting(mouth='lick', eyes='happy', front=bowl), sitting(mouth='lick', eyes='happy', front=bowl),
        sitting(eyes='half', front=bowl), sitting(front=bowl), sitting(front=bowl),
    ]


def yarn_frames():
    """Plays with a ball of yarn: watches it, swats it away (the paw goes out through a middle
    pose), it rolls off and back, the eyes follow it, the tail twitches; ends sitting."""
    path = [16, 16, 16, 16, 15, 13, 11, 10, 9, 8, 8, 9, 10, 11, 13, 15, 16, 16, 16, 16]
    out = []
    for t in range(20):
        x = path[t]
        ball = [L(YARN[(x // 2) % 2], x - 6, 52), L(THREAD, x + 2, 58, ring=False)]
        look = -1 if x < 13 else 0
        if t == 2:
            out.append(sitting(paw='mid', paw_at=(5, 24), eyes='wide', tail=1, props=ball))
        elif t in (3, 4):
            out.append(sitting(paw='swat', paw_at=(14 - (t - 3) * 2, 48), look=look, eyes='wide', tail=1, props=ball))
        elif t == 5:
            out.append(sitting(paw='mid', paw_at=(5, 24), look=look, eyes='wide', tail=2, props=ball))
        else:
            out.append(sitting(look=look, eyes='wide' if 1 <= t <= 15 else 'open', tail=(t // 4) % 3,
                               squash=1 if t in (1, 16) else 0, props=ball))
    return out


def sleep_frames():
    """Curled up like a little loaf, the tail round its paws; slow breathing, Z rise."""
    breath = [0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0]
    out = []
    for t in range(12):
        z = []
        for phase in (0, 6):
            a = (t + phase) % 12
            if a < 9:
                z.append(L(Z_SMALL if a < 4 else Z_BIG, 40 + a // 2 + (2 if phase else 0), 31 - 2 * a))
        out.append(lying(breath=breath[t]) + z)
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
    """From sitting down onto its belly, then the head goes down to sleep."""
    return [Moved(crouch(eyes='half'), -3, 0), lying(eyes='half')]


def get_up_frames():
    """Awake: the head comes up, then it gets up onto its front paws (the reaction sits it up)."""
    return [lying(eyes='open'), Moved(crouch(eyes='open'), -3, 0)]


# --- the backgrounds' interactions (BackgroundEvents) -------------------------------------

HEAP_TOP = 49


def hide_ears(flick_near=False, flick_far=False, dy=0):
    return [L(EAR_POKE_FLICK if flick_far else EAR_POKE, 34, 41 + dy, ring=False, flip_h=True, recolor=FAR),
            L(EAR_POKE_FLICK if flick_near else EAR_POKE, 22, 41 + dy, ring=False)]


def hide_frames():
    """Hidden under the heap of leaves in the maple park (the background draws the heap): only the
    ears poke out of it, twitching now and then (their bases on the heap's top, row 49)."""
    return frames(
        (hide_ears(), 3), (hide_ears(flick_near=True), 1), (hide_ears(), 2), (hide_ears(dy=1), 1),
        (hide_ears(), 2), (hide_ears(flick_far=True), 1), (hide_ears(), 2),
    )


def hide_in_frames():
    """Into the heap: crouches, springs and dives in head first, the leaves close over it (at most
    6 px a frame, diagonally too)."""
    return [sitting(squash=3, head_dy=6, eyes='wide'), Moved(crouch(squash=1), -2, 0), in_air(-1, -3), in_air(1, -6),
            in_air(3, -5), crouch(dx=5, dy=-4, tail=1, cut=HEAP_TOP, shadow=False),
            head(16, 36, eyes='shut', cut=HEAP_TOP), hide_ears(dy=2)]


def hide_out_frames():
    """Out of the heap: the head pops up, it springs out and lands, sits up."""
    return [hide_ears(dy=-1), head(16, 33, eyes='wide', cut=HEAP_TOP), in_air(3, -5), in_air(1, -6), in_air(0, -3),
            Moved(crouch(squash=1), -2, 0), sitting(squash=3, head_dy=6), sitting(squash=1)]


# On the big pumpkin the kitten lies this far above the ground: its ground poses drawn moved up
PUMPKIN_RISE = -25


def on_top(**kw):
    """Lying on top of the big pumpkin: the lying pose (no shadow, no tail) moved up onto it."""
    return Moved(lying(x=18, y=45, head_up=9, tail=False, shadow=False, eyes=kw.pop('eyes', 'half'), **kw),
                 0, PUMPKIN_RISE)


def in_air(dx, dy):
    """Springing through the air: one crouch, drawn wherever the jump takes it."""
    return Moved(crouch(tail=1, shadow=False), dx, dy)


def pounce_frames():
    """Sits, sinks into a crouch, wiggles, springs onto the leaf in front of it in an arc (one pose
    drawn along the arc), pins it with its paws, steps back and sits up again."""
    return frames(
        (sitting(), 1), (sitting(squash=2, eyes='wide'), 1), (sitting(squash=3, head_dy=6, eyes='wide'), 1),
        (crouch(), 1), (crouch(squash=1, tail=1), 1), (crouch(), 1), (crouch(squash=1, tail=1), 1),
        (in_air(-1, -2), 1), (in_air(-2, -4), 1), (in_air(-3, -4), 1), (in_air(-4, -2), 1),
        (Moved(crouch(squash=2, eyes='happy'), -4, 0), 2),
        (Moved(crouch(eyes='open', look=-1), -4, 0), 1), (Moved(crouch(), -3, 0), 1), (Moved(crouch(), -1, 0), 1),
        (crouch(), 1),
        (sitting(squash=3, head_dy=6, eyes='open'), 1), (sitting(squash=1), 1), (sitting(), 1),
    )


def bat_frames():
    """Under the umbrella on the rainy evening: looks up at the drips running off its edge and
    swipes at them with a paw (up through a middle pose), misses, shakes the wet paw, sits."""
    look_up = dict(head_dy=-1, head_dx=1, eyes='wide')
    return frames(
        (sitting(), 1), (sitting(**look_up), 1),
        (sitting(paw='mid', paw_at=(4, 16), **look_up), 1),
        (sitting(paw='up', paw_at=(2, 10), **look_up), 1),
        (sitting(paw='up', paw_at=(0, 3), squash=-1, **look_up), 2),
        (sitting(paw='up', paw_at=(2, 7), **look_up), 1),
        (sitting(paw='up', paw_at=(0, 2), squash=-1, **look_up), 1),
        (sitting(paw='up', paw_at=(4, 14), eyes='shut'), 1),
        (sitting(paw='up', paw_at=(5, 15), eyes='shut', flick=True), 1),
        (sitting(paw='up', paw_at=(4, 14), eyes='half'), 1),
        (sitting(paw='mid', paw_at=(6, 20), eyes='half'), 1),
        (sitting(eyes='open'), 3),
    )


def throne_frames():
    """On top of the big pumpkin like a little queen (the background draws the pumpkin in front,
    its top at row 34): lies there, washes a paw, looks down proudly."""
    return frames(
        (on_top(), 2), (on_top(breath=1), 2), (on_top(), 1),
        (on_top(paw=(7, 15), eyes='half'), 1), (on_top(paw=(7, 14), eyes='shut', mouth='lick'), 1),
        (on_top(paw=(7, 15), eyes='half'), 1), (on_top(paw=(7, 14), eyes='shut', mouth='lick'), 1),
        (on_top(eyes='open'), 2), (on_top(eyes='open', flick=True), 1), (on_top(eyes='shut'), 1),
        (on_top(breath=1), 2), (on_top(), 1),
    )


def throne_in_frames():
    """Up onto the pumpkin: looks up at it, crouches, springs (five pixels a frame), lands on top and
    lies down."""
    return [sitting(head_dy=-1, eyes='wide'), sitting(squash=3, head_dy=6, eyes='wide'), Moved(crouch(squash=1), -2, 0),
            in_air(-1, -5), in_air(-1, -10), in_air(0, -15), in_air(1, -20), in_air(2, PUMPKIN_RISE + 1),
            on_top(eyes='open')]


def throne_out_frames():
    """Down again: gets up on top, jumps off in an arc and lands sitting."""
    return [on_top(eyes='open'), in_air(2, PUMPKIN_RISE + 1), in_air(1, -20), in_air(0, -15), in_air(-1, -10),
            in_air(-1, -5), Moved(crouch(squash=2), -2, 0), sitting(squash=3, head_dy=6), sitting(squash=1)]


ANIMATIONS = {
    'sleep': {'interval': 170, 'frames': sleep_frames},
    'happy': {'interval': 80, 'frames': happy_frames},
    'idle': {'interval': 120, 'frames': idle_frames},
    'eat': {'interval': 100, 'frames': eat_frames},
    'yarn': {'interval': 90, 'frames': yarn_frames},
    'walk': {'interval': 110, 'frames': walk_frames},
    'turn': {'interval': 110, 'frames': turn_frames},
    'walk_start': {'interval': 110, 'frames': walk_start_frames},
    'walk_stop': {'interval': 110, 'frames': walk_stop_frames},
    'lie_down': {'interval': 170, 'frames': lie_down_frames},
    'get_up': {'interval': 160, 'frames': get_up_frames},
    'hide': {'interval': 130, 'frames': hide_frames},
    'hide_in': {'interval': 130, 'frames': hide_in_frames},
    'hide_out': {'interval': 130, 'frames': hide_out_frames},
    'pounce': {'interval': 80, 'frames': pounce_frames},
    'bat': {'interval': 100, 'frames': bat_frames},
    'throne': {'interval': 140, 'frames': throne_frames},
    'throne_in': {'interval': 140, 'frames': throne_in_frames},
    'throne_out': {'interval': 140, 'frames': throne_out_frames},
}
# Played once inside other animations, never looped (check.py does not compare their seams)
ONE_SHOT = ('turn', 'walk_start', 'walk_stop', 'lie_down', 'get_up', 'hide_in', 'hide_out', 'throne_in',
            'throne_out')
# Poses that are not on the ground (in the heap, on the pumpkin): motion_check.py skips their feet
OFF_GROUND = ('hide', 'throne', 'hide_out', 'throne_out')
