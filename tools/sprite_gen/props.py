"""Props for the widget: need bubbles for every pet and seasonal decorations."""
from sprite_gen import L, part

PALETTE = {
    'K': '#3a2a24',  # outline
    'W': '#ffffff',  # bubble, snow
    'w': '#dde3ee',  # bubble and snow shadow
    'G': '#7cbb52',  # leaves, bamboo
    'g': '#4c8a3b',  # dark green
    'A': '#6a4b33',  # seed stripes, stems
    'a': '#ece0c4',  # seed, bone
    'F': '#7fb7e0',  # fish
    'f': '#4a7fb0',  # fish shadow
    'O': '#f39237',  # orange, carrot, pumpkin
    'o': '#c9651f',  # orange shadow
    'H': '#ef5b70',  # heart, scarf
    'p': '#c4405a',  # heart shadow
    'Y': '#ffd447',  # glow, stars, bat eyes
    'B': '#3b2d4f',  # bats, snowman hat
    'b': '#5a4777',  # bat highlight
}
OUTLINE = 'K'
LINE_OF = {'W': 'w', 'w': 'w', 'O': 'o', 'o': 'o', 'G': 'g', 'H': 'p', 'F': 'f', 'a': 'A'}
SHADE = [
    ('W', 'w', (1, 1)),
    ('O', 'o', (1, 2)),
    ('G', 'g', (1, 1)),
    ('F', 'f', (1, 1)),
    ('H', 'p', (1, 1)),
    ('a', 'A', (1, 1)),
    ('B', 'b', (-1, -1)),
]

# --- need bubble -----------------------------------------------------------------------------

CLOUD = part('''
    .........WWWWWWWW.........
    ......WWWWWWWWWWWWWW......
    ....WWWWWWWWWWWWWWWWWW....
    ...WWWWWWWWWWWWWWWWWWWW...
    ..WWWWWWWWWWWWWWWWWWWWWW..
    .WWWWWWWWWWWWWWWWWWWWWWWW.
    .WWWWWWWWWWWWWWWWWWWWWWWW.
    WWWWWWWWWWWWWWWWWWWWWWWWWW
    WWWWWWWWWWWWWWWWWWWWWWWWWW
    WWWWWWWWWWWWWWWWWWWWWWWWWW
    WWWWWWWWWWWWWWWWWWWWWWWWWW
    WWWWWWWWWWWWWWWWWWWWWWWWWW
    .WWWWWWWWWWWWWWWWWWWWWWWW.
    .WWWWWWWWWWWWWWWWWWWWWWWW.
    ..WWWWWWWWWWWWWWWWWWWWWW..
    ...WWWWWWWWWWWWWWWWWWWW...
    ....WWWWWWWWWWWWWWWWWW....
    ......WWWWWWWWWWWWWW......
    .........WWWWWWWW.........
''')

TAIL_BIG = part('''
    .WWW.
    WWWWW
    WWWWW
    .WWW.
''')

TAIL_SMALL = part('''
    WW
    WW
''')

FISH = part('''
    ...FFFFF...F.
    .FFFFFFFFF.FF
    FFKFFFFFFFFFF
    FFFFFFFFFFFFF
    .FFFFFFFFF.FF
    ...FFFFF...F.
''')

BONE = part('''
    .aa.......aa.
    aaaa.....aaaa
    .aaaaaaaaaaa.
    .aaaaaaaaaaa.
    aaaa.....aaaa
    .aa.......aa.
''')

CARROT = part('''
    .........G.G
    ........GGG.
    .......OOGG.
    ......OOOO..
    .....OOOO...
    ....OOOO....
    ...OOOO.....
    ..OOO.......
    .OO.........
''')

BAMBOO = part('''
    ...GGg......
    ...GGg..GG..
    ...GGg.GGGG.
    ...ggg..gg..
    ...GGg......
    ...GGg......
    ...GGg......
    ...ggg......
    ...GGg......
    ...GGg......
''')

ORANGE = part('''
    .....gG....
    ....GG.....
    ..OOOOOO...
    .OOWOOOOO..
    OOWOOOOOOO.
    OOOOOOOOOO.
    OOOOOOOOOO.
    OOOOOOOOOO.
    .OOOOOOOO..
    ..OOOOOO...
''')

HEART = part('''
    .HHH...HHH.
    HHWHH.HHHHH
    HWHHHHHHHHH
    HHHHHHHHHHH
    .HHHHHHHHH.
    ..HHHHHHH..
    ...HHHHH...
    ....HHH....
    .....H.....
''')


def bubble(icon, icon_shade=True):
    """Thought bubble at the top right of the widget; the tail points down-left to the pet."""
    w, h = len(icon[0]), len(icon)
    return [
        L(TAIL_SMALL, 0, 24),
        L(TAIL_BIG, 2, 19),
        L(CLOUD, 4, 0),
        L(icon, 4 + (26 - w) // 2, (19 - h) // 2, shade=icon_shade),
    ]


# Fish food: three flakes, for the aquarium
FLAKES = part('''
    ....YO....
    ...YOOo...
    ....oo..Yo
    .YO....YOo
    YOOo...oo.
    .ooo.YO...
    ....YOOo..
    .....oo...
''')

# Sunflower seed: dark shell with light stripes
SUNFLOWER_SEED = part('''
    ...A...
    ..AaA..
    .AaAaA.
    .AaAaA.
    AaAaAaA
    AaAaAaA
    AaAaAaA
    .AaAaA.
    ..AAA..
''')


# --- Halloween -------------------------------------------------------------------------------

PUMPKIN = part('''
    .......OOOOOOOO.......
    ....OOOOOOOOOOOOOO....
    ...OOOOOOOOOOOOOOOO...
    ..OOOOOOOOOOOOOOOOOO..
    .OOOOOOOOOOOOOOOOOOOO.
    OOOOOOOOOOOOOOOOOOOOOO
    OOOOOOOOOOOOOOOOOOOOOO
    OOOOOOOOOOOOOOOOOOOOOO
    OOOOOOOOOOOOOOOOOOOOOO
    OOOOOOOOOOOOOOOOOOOOOO
    .OOOOOOOOOOOOOOOOOOOO.
    ..OOOOOOOOOOOOOOOOOO..
    ...OOOOOOOOOOOOOOOO...
    ....OOOOOOOOOOOOOO....
    .......OOOOOOOO.......
''')

PUMPKIN_FACE = part('''
    ...Y..........Y...
    ..YYY........YYY..
    .YYYYY..Y...YYYYY.
    ...............
    .Y..YY.YYY.YY..Y.
    ..YYYYYYYYYYYYYY..
    ....YYY.YYY.YYY...
''')

PUMPKIN_SMALL = part('''
    ....OOOOOO....
    ..OOOOOOOOOO..
    .OOOOOOOOOOOO.
    OOOOOOOOOOOOOO
    OOOOOOOOOOOOOO
    OOOOOOOOOOOOOO
    OOOOOOOOOOOOOO
    OOOOOOOOOOOOOO
    .OOOOOOOOOOOO.
    ..OOOOOOOOOO..
    ....OOOOOO....
''')

STEM = part('''
    .gg
    gG.
    gG.
''')

RIB = part('''
    o
    o
    o
    o
    o
    o
    o
''')

BAT = part('''
    .B.........B.
    BBB..B.B..BBB
    BBBBBYBYBBBBB
    .BBBBBBBBBBB.
    ..B..BBB..B..
    ......B......
''')

STAR = part('''
    .Y.
    YYY
    .Y.
''')


def pumpkin_big():
    return [
        L(STEM, 10, 0),
        L(PUMPKIN, 0, 3),
        L(RIB, 7, 6, ring=False, shade=False, clip=True),
        L(RIB, 14, 6, ring=False, shade=False, clip=True),
        L(PUMPKIN_FACE, 2, 6, ring=False, shade=False, clip=True),
    ]


def pumpkin_small():
    return [
        L(STEM, 6, 0),
        L(PUMPKIN_SMALL, 0, 3),
        L(RIB, 4, 5, ring=False, shade=False, clip=True),
        L(RIB, 9, 5, ring=False, shade=False, clip=True),
    ]


def halloween_strip():
    # One tile of the garland along the top edge; repeats seamlessly
    return [L(BAT, 3, 2), L(STAR, 23, 6, shade=False)]


# --- Winter ----------------------------------------------------------------------------------

SNOW_STRIP = part('''
    WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW
    WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW
    WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW
    .WWWW.WWWW..WWWW.WWWWW..WWWW.WWW
    ..WW...WW....WW...WWW....WW...W.
    ..W.....W....W.....W.....W......
''')

SNOWBALL_BIG = part('''
    ......WWWWWW......
    ....WWWWWWWWWW....
    ..WWWWWWWWWWWWWW..
    .WWWWWWWWWWWWWWWW.
    .WWWWWWWWWWWWWWWW.
    WWWWWWWWWWWWWWWWWW
    WWWWWWWWWWWWWWWWWW
    WWWWWWWWWWWWWWWWWW
    WWWWWWWWWWWWWWWWWW
    WWWWWWWWWWWWWWWWWW
    .WWWWWWWWWWWWWWWW.
    .WWWWWWWWWWWWWWWW.
    ..WWWWWWWWWWWWWW..
    ....WWWWWWWWWW....
    ......WWWWWW......
''')

SNOWBALL_SMALL = part('''
    ....WWWWWW....
    ...WWWWWWWW...
    .WWWWWWWWWWWW.
    .WWWWWWWWWWWW.
    WWWWWWWWWWWWWW
    WWWWWWWWWWWWWW
    WWWWWWWWWWWWWW
    WWWWWWWWWWWWWW
    WWWWWWWWWWWWWW
    .WWWWWWWWWWWW.
    .WWWWWWWWWWWW.
    ...WWWWWWWW...
    ....WWWWWW....
''')

TOP_HAT = part('''
    ..BBBBBB..
    ..BBBBBB..
    ..BBBBBB..
    ..HHHHHH..
    BBBBBBBBBB
''')

SNOWMAN_FACE = part('''
    K....K
    ......
    ..OOO.
''')

SCARF = part('''
    HHHHHHHHHHHH
    HHHHHHHHHHHH
    .........HH.
    .........HH.
''')

BUTTONS = part('''
    K
    .
    .
    K
''')

SNOW_PILE = part('''
    ........WWWWWWWWWWWW........
    ....WWWWWWWWWWWWWWWWWWWW....
    .WWWWWWWWWWWWWWWWWWWWWWWWWW.
    WWWWWWWWWWWWWWWWWWWWWWWWWWWW
    WWWWWWWWWWWWWWWWWWWWWWWWWWWW
    WWWWWWWWWWWWWWWWWWWWWWWWWWWW
''')


def snowman():
    return [
        L(SNOWBALL_BIG, 1, 17),
        L(SNOWBALL_SMALL, 3, 6),
        L(SNOWMAN_FACE, 7, 10, ring=False, shade=False),
        L(SCARF, 4, 17, shade=False),
        L(BUTTONS, 10, 23, ring=False, shade=False),
        L(TOP_HAT, 5, 1),
    ]


def snow_strip():
    return [L(SNOW_STRIP, 0, 0)]


def snow_pile():
    return [L(SNOW_PILE, 0, 0)]


# name: (frame builder, canvas size, export scale, tiled); the parrot and the hamster share the seeds
PROPS = {
    'bubble_food_cat': (lambda: bubble(FISH), (30, 26), 1, False),
    'bubble_food_dog': (lambda: bubble(BONE), (30, 26), 1, False),
    'bubble_food_rabbit': (lambda: bubble(CARROT), (30, 26), 1, False),
    'bubble_food_seeds': (lambda: bubble(SUNFLOWER_SEED, icon_shade=False), (30, 26), 1, False),
    'bubble_food_capybara': (lambda: bubble(ORANGE), (30, 26), 1, False),
    'bubble_food_panda': (lambda: bubble(BAMBOO), (30, 26), 1, False),
    'bubble_food_aquarium': (lambda: bubble(FLAKES), (30, 26), 1, False),
    'bubble_lonely': (lambda: bubble(HEART), (30, 26), 1, False),
    'decor_pumpkin': (pumpkin_big, (22, 18), 1, False),
    'decor_pumpkin_small': (pumpkin_small, (14, 14), 1, False),
    'decor_halloween_strip': (halloween_strip, (32, 12), 4, True),
    'decor_snowman': (snowman, (20, 32), 1, False),
    'decor_snow_pile': (snow_pile, (28, 6), 1, False),
    'decor_winter_strip': (snow_strip, (32, 8), 4, True),
}
