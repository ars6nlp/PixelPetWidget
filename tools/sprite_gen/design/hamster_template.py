"""Hamster: tiny, quick and thrifty. Faces left on a 64x64 canvas, feet on row 61 over a ground
shadow.

Drawn from text matrices: big round shapes carry their own three tones (light from the top
left), the engine adds the one-pixel outline. Key poses are held by repeating a frame, since the
widget plays every frame for the same time.

Smooth motion: every loop begins and ends sitting (the idle's first frame); the walk starts and
stops on all fours and moves its planted feet by exactly WALK_SHIFTS (PetSpec.HAMSTER_GAIT),
eight drawings a cycle. The hamster sits facing you, so it turns round sitting (the turn is that
frame); it curls up to sleep and uncurls to wake. It climbs onto the little pumpkin and down again
a few pixels a frame (its sitting pose moved up, sg.Moved).
"""
import copy

from sprite_gen import L, Moved, part

PALETTE = {
    'K': '#1f2133',  # outline (deep blue-grey, shared by all pixel pets); pupils
    'O': '#eba04a',  # golden-ginger fur
    'o': '#c4762f',  # fur shadow, darker back
    'y': '#f8c774',  # fur highlight
    'x': '#8f4f25',  # back stripe, soft inner lines, whiskers, mouth
    'C': '#fff2e0',  # cream muzzle, belly, cheeks
    'c': '#e6ccad',  # cream shadow
    'P': '#f4a0ab',  # ears, nose, paws
    'p': '#cf6f80',  # pink shadow, toes
    'W': '#ffffff',  # eye shine, teeth, seed stripe, star shine
    's': '#4a403d',  # sunflower seed, husks, witch hat
    'Y': '#ffd34d',  # stars, hat band
    'R': '#e2545f',  # bow, Santa hat
    'z': '#c3cdf0',  # sleep "Z"
    'S': '#3b3d52',  # ground shadow
    'F': '#f6a531',  # maple leaf (the maple park's colours)
    'L': '#e8702a',  # maple leaf, middle tone
    'l': '#b9452a',  # maple leaf shadow, stem
    'E': '#f2c14e',  # the lime leaf from the puddle (the rainy evening's colours)
    'e': '#c98f2e',  # its shadow, veins and stalk
}
MAX_COLOURS = 20
OUTLINE = 'K'
LINE_OF = {'F': 'l', 'L': 'l', 'l': 'l', 'E': 'e', 'e': 'e', 'O': 'o', 'o': 'o', 'y': 'o', 'x': 'x', 'C': 'c', 'c': 'o', 'P': 'p', 'p': 'p',
           'W': 'c', 's': 'K', 'Y': 'x', 'R': 'p'}
SHADE = []                         # every part carries its own tones
# Single pixels that are details on purpose: (colour, a neighbour colour it must touch)
ORPHAN_OK = (('W', 'K'), ('W', 's'), ('W', 'x'), ('W', 'Y'), ('p', 'P'), ('s', 'S'), ('x', 'C'), ('l', 'L'))

FAR = {'O': 'o', 'y': 'O', 'P': 'p', 'C': 'c'}
# How far its planted feet move back in each walk frame (art pixels): PetSpec.HAMSTER_GAIT
WALK_SHIFTS = (2, 1, 2, 1, 2, 1, 2, 1)

# --- big shapes (generated with three tones) ----------------------------------------------

HEAD = part('''
{HEAD}
''')

BODY = part('''
{BODY}
''')

BELLY = part('''
{BELLY}
''')

CHEST = part('''
{CHEST}
''')

MUZZLE = part('''
{MUZZLE}
''')

CHEEKS = [part('''
{CHEEK_0}
'''), part('''
{CHEEK_1}
'''), part('''
{CHEEK_2}
'''), part('''
{CHEEK_3}
''')]

RUN_BODY = part('''
{RUN_BODY}
''')

RUN_HEAD = part('''
{RUN_HEAD}
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

LOAF = part('''
{LOAF}
''')
LOAF_Y = 61 - len(LOAF)

SHADOW_LOAF = part('''
{SHADOW_LOAF}
''')

# --- small parts ----------------------------------------------------------------------------

EAR = part('''
    ..OOO..
    .OPPPO.
    OPPPPOo
    OPPPPOo
    .OOooo.
''')

EYE = part('''
    .KK.
    KWKK
    KKKK
    KKKK
    .KK.
''')
EYE_HALF = part('''
    KKKK
    .KK.
''')
EYE_SHUT = part('''
    xxxx
''')
EYE_SLEEP = part('''
    x..x
    .xx.
''')
EYE_HAPPY = part('''
    .xx.
    x..x
''')
# Squeezed shut with effort: > on the near eye, < on the far one
SQUINT_NEAR = part('''
    K..
    .KK
    K..
''')
SQUINT_FAR = part('''
    ..K
    KK.
    ..K
''')

NOSE = part('''
    PP
''')
NOSE_UP = part('''
    PP
    ..
''')
MOUTH = part('''
    .xx.
    x..x
''')
MOUTH_GNAW = part('''
    .xx.
    xWWx
''')
MOUTH_OPEN = part('''
    .xx.
    xppx
    .pp.
''')

WHISKERS = part('''
    xxx.
    ....
    .xxx
''')
WHISKERS_TWITCH = part('''
    .xxx
    ....
    xxx.
''')
WHISKERS_FAR = part('''
    .xx
    ...
    xx.
''')

# Front paws: resting on the chest, raised, holding something at the mouth
PAWS_CHEST = part('''
    PP.PP
    pp.pp
''')
PAWS_UP = part('''
    PP........PP
    Pp........pP
''')
PAWS_HOLD = part('''
    PP.....PP
    Pp.....pP
''')
FOOT = part('''
    .PPPP.
    PPPPPP
    PpPpPp
''')
FOOT_SIDE = part('''
    PPPP
    pppp
''')

# Sunflower seed: a dark teardrop, pointed end up, with a light stripe
SEED = part('''
    .s.
    sWs
    sWs
    sWs
    sss
''')
# A maple leaf like the ones falling in the maple park (in the background's colours), as the pets
# hold it after their interactions with the backgrounds (BackgroundEvents)
MAPLE_LEAF = part('''
    .F.F.
    FFLFF
    .LLL.
    ..l..
''')

# The lime leaf fished out of the puddle, held up as an umbrella: a dome seen from below
LIME_LEAF = part('''
    ......EEEEEEE.......
    ...EEEEEEEEEEEEE....
    .EEEEEeEEEEEeEEEEE..
    EEEeEEEEeEEEEEeEEEE.
    eeeeeeeeeeeeeeeeeeee
''')

# The near paw gripping the leaf's stalk
PAW_STALK = part('''
    .OO.
    OOOO
    .oo.
''')

# A drop of rain running off the leaf
DRIP = part('''
    z
    z
''')

HUSK = part('''
    ss
''')
HUSK_TURNED = part('''
    s.
    .s
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

# --- accessories ----------------------------------------------------------------------------
# The engine sets `wearing` before building frames; every pose that draws a head wears it.

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
        return [L(BOW, hx + 19, hy - 4)]
    if wearing == 'halloween_hat':
        return [L(WITCH_HAT, hx + 7, hy - 7)]
    if wearing == 'winter_hat':
        return [L(SANTA_HAT, hx + 9, hy - 4)]
    return []


def accessory_icon(name):
    """The accessory on its own, for the wardrobe in the app."""
    rows = {'bow': BOW, 'halloween_hat': WITCH_HAT, 'winter_hat': SANTA_HAT}[name]
    return [L(rows, 1, 1)], (len(rows[0]) + 2, len(rows) + 2)


# --- poses ----------------------------------------------------------------------------------

def ground(height=0, long=False):
    if long:
        return [L(SHADOW_LONG, 10, 61, ring=False)]
    rows = SHADOW_SIT if height < 4 else SHADOW_SMALL
    return [L(rows, 31 - len(rows[0]) // 2, 61, ring=False)]


def face(hx, hy, eyes='open', mouth='closed', look=0, nose=0, whisk=0):
    layers = [L(MUZZLE, hx + 4, hy + 11, ring=False, clip=True)]
    nx, ny = hx + 6 + look, hy + 6
    fx = hx + 17 + look
    if eyes == 'open':
        layers += [L(EYE, nx, ny, ring=False), L(EYE, fx, ny, ring=False)]
    elif eyes == 'half':
        layers += [L(EYE_HALF, nx, ny + 2, ring=False), L(EYE_HALF, fx, ny + 2, ring=False)]
    elif eyes == 'shut':
        layers += [L(EYE_SHUT, nx, ny + 2, ring=False), L(EYE_SHUT, fx, ny + 2, ring=False)]
    elif eyes == 'sleep':
        layers += [L(EYE_SLEEP, nx, ny + 2, ring=False), L(EYE_SLEEP, fx, ny + 2, ring=False)]
    elif eyes == 'happy':
        layers += [L(EYE_HAPPY, nx, ny + 1, ring=False), L(EYE_HAPPY, fx, ny + 1, ring=False)]
    elif eyes == 'squint':
        layers += [L(SQUINT_NEAR, nx + 1, ny + 1, ring=False), L(SQUINT_FAR, fx, ny + 1, ring=False)]
    layers.append(L(NOSE, hx + 8 + look, hy + 12 - nose, ring=False))
    mouths = {'closed': MOUTH, 'gnaw': MOUTH_GNAW, 'open': MOUTH_OPEN}
    layers.append(L(mouths[mouth], hx + 7 + look, hy + 13, ring=False))
    return layers


def head(hx, hy, eyes='open', mouth='closed', look=0, nose=0, whisk=0, ear_near=0, ear_far=0, cheek=0):
    w, h = len(CHEEKS[cheek][0]), len(CHEEKS[cheek])
    near_x = hx + 2 - w // 2 - cheek // 2
    far_x = hx + 25 - w // 2 + cheek // 2
    layers = [
        L(EAR, hx + 18, hy - 3 + ear_far, recolor=FAR),
        L(EAR, hx + 3, hy - 3 + ear_near),
        L(HEAD, hx, hy),
    ]
    # Cheeks puff out past the head on both sides
    layers.append(L(CHEEKS[cheek], far_x, hy + 12, recolor=FAR))
    layers.append(L(CHEEKS[cheek], near_x, hy + 12))
    layers += face(hx, hy, eyes, mouth, look, nose, whisk)
    layers.append(L(WHISKERS_TWITCH if whisk else WHISKERS, near_x - 2, hy + 13, ring=False))
    layers.append(L(WHISKERS_FAR, far_x + w - 1, hy + 13, ring=False))
    return layers + accessory(hx, hy)


BODY_X, BODY_Y = 15, 37
HEAD_X, HEAD_Y = 12, 19


def sitting(dy=0, squash=0, head_dx=0, head_dy=0, eyes='open', mouth='closed', look=0, nose=0, whisk=0,
            ear_near=0, ear_far=0, cheek=0, paws='chest', seed=None, held='seed', shadow=True, effects=()):
    """Sitting up like a little ball. dy lifts the whole hamster, squash > 0 flattens the body,
    < 0 stretches it. paws: chest, up (cheering), hold (a seed at `seed`, relative to the head; held
    'leaf': the maple leaf instead) or umbrella (the lime leaf held up over the head by its stalk)."""
    body_top = BODY_Y + dy + max(squash, 0) - max(-squash, 0)
    hx = HEAD_X + head_dx
    hy = HEAD_Y + dy + head_dy + max(squash, 0) - max(-squash, 0)
    layers = list(ground(-dy)) if shadow else []
    layers.append(L(BODY, BODY_X, body_top, squash_y=max(squash, 0), grow_y=max(-squash, 0)))
    layers.append(L(BELLY, BODY_X + 4, body_top + 6, ring=False, clip=True))
    layers.append(L(FOOT, 33, 56 + dy, recolor=FAR))
    layers.append(L(FOOT, 17, 56 + dy))
    layers += head(hx, hy, eyes, mouth, look, nose, whisk, ear_near, ear_far, cheek)
    layers.append(L(CHEST, hx + 8, hy + 19, ring=False))
    if paws == 'up':
        layers.append(L(PAWS_UP, hx + 4, hy + 19))
    elif paws == 'hold':
        sx, sy = seed
        layers.append(L(PAWS_HOLD, hx + sx - 3, hy + sy + 2))
        layers.append(L(SEED if held == 'seed' else MAPLE_LEAF, hx + sx, hy + sy))
        if mouth == 'gnaw':
            # The teeth bite into the tip of the seed
            layers.append(L(MOUTH_GNAW, hx + 7 + look, hy + 13, ring=False))
    elif paws == 'umbrella':
        # The stalk runs up beside the near cheek to the leaf over the head
        layers.append(L(['e'] * 24, hx - 3, hy - 4, ring=False))
        layers.append(L(LIME_LEAF, hx - 4, hy - 9))
        layers.append(L(PAW_STALK, hx - 5, hy + 17))
    return layers + list(effects)


def frames(*pattern):
    """Expands (layers, count) pairs: a key pose held for count frames."""
    out = []
    for layers, count in pattern:
        out += [layers] * count
    return out


# --- animations -----------------------------------------------------------------------------

def idle_frames():
    """Sniffs (nose and whiskers twitch), looks left and right, blinks, flicks an ear; ends as it
    began."""
    calm = sitting()
    sniff = sitting(nose=1, whisk=1)
    return frames(
        (calm, 2), (sniff, 1), (calm, 1), (sniff, 1), (calm, 1),
        (sitting(look=-1, head_dx=-1), 2),
        (sitting(look=1, head_dx=1), 2),
        (calm, 1), (sitting(eyes='shut'), 1),
        (sitting(squash=-1), 2), (sitting(ear_far=-1), 1), (calm, 1),
    )


def eat_frames():
    """Holds a seed in both paws at the mouth and nibbles fast; bits of husk fall."""
    def husks(t):
        layers = []
        for start, x, drift in ((1, 19, -1), (6, 23, 1), (11, 20, -1)):
            age = (t - start) % 16
            if age < 5:
                layers.append(L(HUSK_TURNED if age % 2 else HUSK, x + drift * (age // 2), 40 + 4 * age, ring=False))
        return layers
    out = []
    for t in range(16):
        gnaw = t % 2 == 0
        eyes = 'happy' if 11 <= t <= 13 else 'shut' if t == 6 else 'open'
        out.append(sitting(paws='hold', seed=(8, 15 - (0 if gnaw else 1)), mouth='gnaw' if gnaw else 'closed',
                           eyes=eyes, head_dy=0 if gnaw else 1, effects=husks(t)))
    return out


def cheeks_frames():
    """Stuffs two seeds into its cheeks: they puff up in steps while it squints with effort,
    stay full for a proud moment, then empty again."""
    return frames(
        (sitting(paws='hold', seed=(8, 16)), 1),
        (sitting(paws='hold', seed=(8, 14), mouth='open', head_dy=1), 1),
        (sitting(cheek=1, eyes='squint'), 1),
        (sitting(cheek=1), 1),
        (sitting(paws='hold', seed=(8, 16), cheek=1), 1),
        (sitting(paws='hold', seed=(8, 14), mouth='open', cheek=1, head_dy=1), 1),
        (sitting(cheek=2, eyes='squint'), 1),
        (sitting(cheek=3, eyes='squint'), 1),
        (sitting(cheek=3), 3),
        (sitting(cheek=3, eyes='happy'), 2),
        (sitting(cheek=2), 1),
        (sitting(cheek=1), 1),
        (sitting(eyes='happy'), 1),
    )


def happy_frames():
    """A quick hop: crouch, spring up with the paws raised (a few pixels a frame), stars, land with
    a squash and settle."""
    def stars(t):
        layers = []
        for x, y, phase in ((5, 20, 0), (50, 16, 1), (46, 6, 2)):
            s = t - phase
            if 0 <= s < 5:
                layers.append(L(STAR if s in (1, 2) else STAR_SMALL, x, y - s))
        return layers
    return frames(
        (sitting(eyes='happy'), 1),
        (sitting(squash=2, eyes='happy', ear_near=1, ear_far=1), 1),
        (sitting(dy=-3, squash=-1, eyes='happy', mouth='open', ear_near=1, ear_far=1, effects=stars(0)), 1),
        (sitting(dy=-6, squash=-1, eyes='happy', mouth='open', ear_near=1, ear_far=1), 1),
        (sitting(dy=-7, eyes='happy', mouth='open', effects=stars(1)), 1),
        (sitting(dy=-8, eyes='happy', mouth='open', ear_near=-1, ear_far=-1, effects=stars(2)), 1),
        (sitting(dy=-5, eyes='happy', mouth='open', effects=stars(3)), 1),
        (sitting(dy=-2, eyes='happy', mouth='open', ear_near=-1, ear_far=-1), 1),
        (sitting(squash=2, eyes='happy', effects=stars(4)), 1),
        (sitting(squash=-1, eyes='happy', effects=stars(5)), 1),
        (sitting(eyes='happy', effects=stars(6)), 1),
        (sitting(), 1),
    )


def curled(breath, eyes='sleep'):
    """Curled up into a round loaf, head sunk into the fur, paws tucked under the chin.
    Breathing lifts the back and the head by one pixel."""
    top = LOAF_Y - 1 - breath
    hx, hy = 13, 27 - breath
    layers = [L(SHADOW_LOAF, 31 - len(SHADOW_LOAF[0]) // 2, 61, ring=False),
              L(LOAF, 11, top, grow_y=breath)]
    layers += head(hx, hy, eyes=eyes)
    layers.append(L(PAWS_CHEST, hx + 9, hy + 22))
    return layers


def sleep_frames():
    breath = [0, 0, 1, 1, 1, 0, 0, 0, 1, 1, 1, 0]
    out = []
    for t in range(12):
        s = t % 12
        z = []
        for phase in (0, 6):
            a = (t + phase) % 12
            if a < 9:
                z.append(L(Z_SMALL if a < 4 else Z_BIG, 44 + a // 2 + (2 if phase else 0), 30 - 2 * a))
        out.append(curled(breath[s]) + z)
    return out


def leg_x(f, phase, base):
    """Where a foot is (and how high it is lifted) in walk frame f: planted for five frames, moving
    back exactly by WALK_SHIFTS while the body goes forward, then swung forward in three."""
    p = (f + phase) % 8
    start = f - p
    if p <= 4:
        return base + sum(WALK_SHIFTS[k % 8] for k in range(start + 1, f + 1)), 0
    travel = sum(WALK_SHIFTS[k % 8] for k in range(start + 1, start + 5))
    back = base + travel
    return back - round(travel * (p - 4) / 4), (1, 2, 1)[p - 5]


# Feet: (base x, phase, far); diagonal pairs move together
FEET = ((15, 4, True), (38, 0, True), (12, 0, False), (35, 4, False))


def running(f=None):
    """Pitter-patter trot seen from the side, frame f of 8: the planted feet move back by
    WALK_SHIFTS, the body dips a pixel when both pairs are down, the ears and whiskers follow a
    frame later. f None: standing still on all fours."""
    bob = 0 if f is None else 1 if f % 4 == 0 else 0
    lag = 0 if f is None else 1 if (f - 1) % 4 == 0 else 0
    hx, hy = 6, 30 + bob
    by = 37 + bob

    def foot(i):
        base, phase, far = FEET[i]
        x, lift = (base + 3, 0) if f is None else leg_x(f, phase, base)
        return L(FOOT_SIDE, x, 58 - lift, recolor=FAR if far else None)

    layers = list(ground(long=True))
    layers += [foot(0), foot(1)]
    layers.append(L(EAR, hx + 13, hy - 3 + lag, recolor=FAR))
    layers.append(L(RUN_BODY, 13, by))
    layers.append(L(RUN_HEAD, hx, hy))
    layers.append(L(EAR, hx + 7, hy - 3 + lag))
    layers.append(L(MUZZLE, hx - 1, hy + 9, ring=False, clip=True))
    layers.append(L(CHEEKS[0], hx + 6, hy + 10))
    layers.append(L(EYE, hx + 6, hy + 5, ring=False))
    layers.append(L(NOSE, hx + 1, hy + 10, ring=False))
    layers.append(L(WHISKERS_TWITCH if f is not None and f % 4 in (1, 2) else WHISKERS, hx - 4, hy + 10, ring=False))
    layers += [foot(2), foot(3)]
    return layers + accessory(hx - 3, hy)


def walk_frames():
    return [running(f) for f in range(8)]


def turn_frames():
    """It sits facing you: turning round is that pose for a moment (PetBehavior mirrors after it)."""
    return [sitting()]


def walk_start_frames():
    """Down onto all fours."""
    return [running(None)]


def walk_stop_frames():
    return [running(None)]


def lie_down_frames():
    """Curls up, the eyes closing."""
    return [curled(0, eyes='half')]


def get_up_frames():
    """Uncurls, awake."""
    return [curled(0, eyes='open')]


# --- the backgrounds' interactions (BackgroundEvents) -------------------------------------

def nibble_frames():
    """Nibbles at the maple leaf it caught in the maple park, holding it in both paws like a seed,
    with a pleased blink now and then."""
    out = []
    for t in range(16):
        gnaw = t % 2 == 0
        eyes = 'happy' if 9 <= t <= 11 else 'shut' if t == 4 else 'open'
        out.append(sitting(paws='hold', seed=(7, 15 - (0 if gnaw else 1)), held='leaf',
                           mouth='gnaw' if gnaw else 'closed', eyes=eyes, head_dy=0 if gnaw else 1))
    return out


def umbrella_in_frames():
    """Bends down to fish the leaf out of the puddle."""
    return [sitting(squash=2, head_dy=1, look=-1)]


def umbrella_out_frames():
    return [sitting(squash=2, head_dy=1)]


def umbrella_frames():
    """Holds the lime leaf from the puddle over its head like an umbrella: it bobs a little, rain
    runs off its tips, the whiskers twitch, a happy blink."""
    def drips(t):
        layers = []
        for start, x in ((0, 7), (6, 27)):
            age = (t - start) % 12
            if age < 4:
                layers.append(L(DRIP, x, 16 + 3 * age, ring=False))
        return layers
    bob = [0, 0, 1, 1, 0, 0, 0, 0, 1, 1, 0, 0]
    eyes = ['open'] * 5 + ['happy'] * 3 + ['open'] * 4
    return [sitting(paws='umbrella', head_dy=bob[t], eyes=eyes[t], whisk=1 if t in (2, 9) else 0,
                    effects=drips(t)) for t in range(12)]


def shifted(layers, dx):
    """The layers moved dx pixels sideways (copies: frames share their layers)."""
    out = []
    for layer in layers:
        moved = copy.copy(layer)
        moved.x += dx
        out.append(moved)
    return out


# On the little pumpkin the hamster sits this far above the ground: its sitting pose drawn moved up
PUMPKIN_RISE = -12


def balance_frames():
    """Has climbed onto the little pumpkin (the background draws it underneath) and keeps its
    balance as it wobbles: sways with it (the pumpkin's 4 frames at 120 ms), paws out, then a
    proud look."""
    wobble = [1, 0, -1, 0]
    out = []
    for t in range(16):
        paws = 'up' if 4 <= t < 10 else 'chest'
        eyes = 'squint' if 4 <= t < 8 else 'happy' if t >= 12 else 'open'
        out.append(Moved(sitting(paws=paws, eyes=eyes, shadow=False, ear_far=-1 if t % 4 == 1 else 0),
                         wobble[t % 4], PUMPKIN_RISE))
    return out


def balance_in_frames():
    """Climbs up onto the pumpkin, four pixels a frame."""
    return [sitting(squash=2), Moved(sitting(squash=-1, shadow=False), 0, -4),
            Moved(sitting(squash=-1, shadow=False), 1, -8), Moved(sitting(shadow=False), 1, PUMPKIN_RISE)]


def balance_out_frames():
    """Hops down again."""
    return [Moved(sitting(shadow=False), 1, PUMPKIN_RISE), Moved(sitting(squash=-1, shadow=False), 0, -8),
            Moved(sitting(squash=-1, shadow=False), -1, -4), sitting(squash=2), sitting()]


ANIMATIONS = {
    'sleep': {'interval': 160, 'frames': sleep_frames},
    'happy': {'interval': 80, 'frames': happy_frames},
    'idle': {'interval': 100, 'frames': idle_frames},
    'eat': {'interval': 90, 'frames': eat_frames},
    'cheeks': {'interval': 90, 'frames': cheeks_frames},
    'walk': {'interval': 90, 'frames': walk_frames},
    'turn': {'interval': 90, 'frames': turn_frames},
    'walk_start': {'interval': 90, 'frames': walk_start_frames},
    'walk_stop': {'interval': 90, 'frames': walk_stop_frames},
    'lie_down': {'interval': 160, 'frames': lie_down_frames},
    'get_up': {'interval': 160, 'frames': get_up_frames},
    'nibble': {'interval': 90, 'frames': nibble_frames},
    'umbrella': {'interval': 100, 'frames': umbrella_frames},
    'umbrella_in': {'interval': 100, 'frames': umbrella_in_frames},
    'umbrella_out': {'interval': 100, 'frames': umbrella_out_frames},
    'balance': {'interval': 120, 'frames': balance_frames},
    'balance_in': {'interval': 120, 'frames': balance_in_frames},
    'balance_out': {'interval': 120, 'frames': balance_out_frames},
}
# Played once inside other animations, never looped (check.py does not compare their seams)
ONE_SHOT = ('turn', 'walk_start', 'walk_stop', 'lie_down', 'get_up', 'umbrella_in', 'umbrella_out', 'balance_in',
            'balance_out')
# On the little pumpkin (motion_check.py skips their feet)
OFF_GROUND = ('balance', 'balance_out')
