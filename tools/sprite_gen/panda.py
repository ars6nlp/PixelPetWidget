"""Panda: big, soft and sleepy. Faces left on a 64x64 canvas, feet on row 62 over a ground shadow
(it sits on its soles, a row lower than the other pets: FOOT_ROW).

Drawn from text matrices: big round shapes carry their own three tones (light from the top
left), the engine adds the one-pixel outline. Animations hold key poses by repeating a frame,
since the widget plays every frame for the same time.

Smooth motion: every loop begins and ends sitting (the idle's first frame); the walk starts and
stops through a crouch and standing on all fours, and moves its planted paws by exactly
WALK_SHIFTS (PetSpec.PANDA_GAIT). The panda sits facing you, so it turns round sitting (the
turn is that frame) and sleeps sitting (no lying down).
"""
from sprite_gen import L, part

PALETTE = {
    'K': '#1f2133',  # outline (deep blue-grey, shared by all pixel pets); pupils, nose
    'D': '#4a4f6c',  # dark fur
    'd': '#363a52',  # dark fur shadow
    'h': '#6c7599',  # dark fur highlight, nose shine
    'W': '#fffcf6',  # white fur highlight, eye shine
    'w': '#efe9de',  # white fur
    'v': '#c8c3cf',  # white fur shadow (cool)
    'x': '#9894aa',  # soft inner lines, mouth, closed eyes
    'P': '#f4a3b1',  # blush, paw pads
    'p': '#cc6c7c',  # open mouth, pad shadow
    'G': '#7cbb52',  # bamboo
    'g': '#4a8a3a',  # bamboo shadow, nodes
    'L': '#bde67f',  # bamboo highlight, leaves
    'H': '#ef5b70',  # heart
    'z': '#c3cdf0',  # sleep "Z"
    'S': '#3b3d52',  # ground shadow
    'O': '#f6a531',  # maple leaf (the maple park's colours)
    'R': '#e8702a',  # maple leaf, middle tone
    'r': '#b9452a',  # maple leaf shadow, stem
}
MAX_COLOURS = 19
OUTLINE = 'K'
# Contour lines inside the figure take a soft colour from the fill next to them (darkest wins)
LINE_OF = {'w': 'x', 'W': 'x', 'v': 'x', 'x': 'x', 'D': 'K', 'd': 'K', 'h': 'K',
           'P': 'p', 'p': 'p', 'G': 'g', 'g': 'g', 'L': 'g', 'H': 'p', 'O': 'r', 'R': 'r', 'r': 'r'}
SHADE = []                         # every part carries its own tones
# Single pixels that are details on purpose: (colour, a neighbour colour it must touch)
ORPHAN_OK = (('W', 'K'), ('h', 'K'), ('h', 'D'), ('P', 'D'), ('P', 'd'), ('r', 'R'))

FAR = {'D': 'd', 'h': 'd', 'w': 'v', 'W': 'w', 'v': 'x', 'P': 'p'}
# The row of the outline under its soles (motion_check.py), and how far its planted paws move
# back in each walk frame (art pixels): PetSpec.PANDA_GAIT
FOOT_ROW = 62
WALK_SHIFTS = (2, 1, 2, 1, 2, 1, 2, 1)

# --- big shapes (generated with three tones, see the module docstring) ----------------------

HEAD = part('''
    ...........wwwwwwww...........
    ........wwwwWWWWWwwwww........
    ......wwwWWWWwwwwwwwwwwv......
    .....wwWWWwwwwwwwwwwwwwwv.....
    ....wwWWwwwwwwwwwwwwwwwwwv....
    ...wwWWwwwwwwwwwwwwwwwwwwwv...
    ..wwWWwwwwwwwwwwwwwwwwwwwwvv..
    ..wWWwwwwwwwwwwwwwwwwwwwwwwv..
    .wwWwwwwwwwwwwwwwwwwwwwwwwwvv.
    .wWWwwwwwwwwwwwwwwwwwwwwwwwvv.
    .wWwwwwwwwwwwwwwwwwwwwwwwwwvv.
    .wWwwwwwwwwwwwwwwwwwwwwwwwwvv.
    .wWwwwwwwwwwwwwwwwwwwwwwwwwvv.
    .wWwwwwwwwwwwwwwwwwwwwwwwwwvv.
    .wwwwwwwwwwwwwwwwwwwwwwwwwvvv.
    ..wwwwwwwwwwwwwwwwwwwwwwwwvv..
    ..wwwwwwwwwwwwwwwwwwwwwwwwvv..
    .wvwwwwwwwwwwwwwwwwwwwwwwvvvv.
    ...vwwwwwwwwwwwwwwwwwwwwvvv...
    ..v.vvwwwwwwwwwwwwwwwwvvvv.v..
    .....vvvvwwwwwwwwwwvvvvvv.....
    .......vvvvvvvvvvvvvvvv.......
    ..........vvvvvvvvvv..........
    ..............................
''')

BODY = part('''
    ..........wwwwwwww..........
    .......wwwwWWWWWwwwww.......
    .....wwwWWWWwwwwwwwwwwv.....
    ....wwWWWwwwwwwwwwwwwwwv....
    ...wwWWwwwwwwwwwwwwwwwwwv...
    ..wwWWwwwwwwwwwwwwwwwwwwvv..
    ..wWWwwwwwwwwwwwwwwwwwwwwv..
    .wwWwwwwwwwwwwwwwwwwwwwwwvv.
    .wWWwwwwwwwwwwwwwwwwwwwwwwv.
    wwWwwwwwwwwwwwwwwwwwwwwwwwvv
    wWWwwwwwwwwwwwwwwwwwwwwwwwvv
    wWwwwwwwwwwwwwwwwwwwwwwwwwvv
    wWwwwwwwwwwwwwwwwwwwwwwwwwvv
    wWwwwwwwwwwwwwwwwwwwwwwwwwvv
    wwwwwwwwwwwwwwwwwwwwwwwwwwvv
    wwwwwwwwwwwwwwwwwwwwwwwwwwvv
    wwwwwwwwwwwwwwwwwwwwwwwwwwvv
    wwwwwwwwwwwwwwwwwwwwwwwwwvvv
    .wwwwwwwwwwwwwwwwwwwwwwwwvv.
    .vwwwwwwwwwwwwwwwwwwwwwwvvv.
    ..vwwwwwwwwwwwwwwwwwwwwvvv..
    ...vvvwwwwwwwwwwwwwwvvvvv...
    ....vvvvvvvvvvvvvvvvvvvv....
    .......vvvvvvvvvvvvvv.......
''')

SIDE_BODY = part('''
    ...........wwwwwwwwwwww...........
    .......wwwwwWWWWWWWwwwwwwww.......
    .....wwwWWWWWwwwwwwwwwwwwwwww.....
    ...wwwWWWwwwwwwwwwwwwwwwwwwwwwv...
    ..wwWWWwwwwwwwwwwwwwwwwwwwwwwwwv..
    .wwWWwwwwwwwwwwwwwwwwwwwwwwwwwwvv.
    .wWWwwwwwwwwwwwwwwwwwwwwwwwwwwwwv.
    wwWwwwwwwwwwwwwwwwwwwwwwwwwwwwwwvv
    wWWwwwwwwwwwwwwwwwwwwwwwwwwwwwwwvv
    wWwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwvv
    wWwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwvv
    wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwvv
    wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwvvv
    .wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwvv.
    .vvwwwwwwwwwwwwwwwwwwwwwwwwwwvvvv.
    ..vvvwwwwwwwwwwwwwwwwwwwwwwvvvvv..
    ....vvvvwwwwwwwwwwwwwwwwvvvvvv....
    ......vvvvvvvvvvvvvvvvvvvvvv......
    .........vvvvvvvvvvvvvvvv.........
''')

SHADOW_WIDE = part('''
    ...SSSSSSSSSSSSSSSSSSSSSSSSSSSS...
    SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    ...SSSSSSSSSSSSSSSSSSSSSSSSSSSS...
''')

SHADOW_MID = part('''
    ...SSSSSSSSSSSSSSSSSSSSSS...
    SSSSSSSSSSSSSSSSSSSSSSSSSSSS
    ...SSSSSSSSSSSSSSSSSSSSSS...
''')

SHADOW_SMALL = part('''
    ...SSSSSSSSSSSSSSSS...
    SSSSSSSSSSSSSSSSSSSSSS
    ...SSSSSSSSSSSSSSSS...
''')

SHADOW_WALK = part('''
    ...SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS...
    SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    ...SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS...
''')

BALLS = [part('''
    .......DDDDdd..........DDDDDd.......
    ......DDhhhdd..........DDDDDdd......
    ......DhhDDdD..........DDDDDdd......
    ......DhDDDDD.wwwwwwww.DDDDddd......
    .......hDDDwwwwWWWWWwwwwwDDdd.......
    .......DDDwwwwwWwwwwwwwwwwddd.......
    ........wwwwwwwwwwwwwwwwwwwv........
    .......wwwwwwwwwwwwwwwwwwwwwv.......
    ......wwWWwwwwwwwwwwwwwwwwwwvv......
    ......wWWwwwwwwwwwwwwwwwwwwwwv......
    .....wwWwwwwwwwwwwwwwwwwwwwwwwv.....
    ....wwWWwwwwwwwwwwwwwwwwwwwwwwvv....
    ....wWWwwwwDDDwwwwwwwwDDDwwwwwvv....
    ....wWwwwwDDDDDwwwwwwDDDDDwwwwwv....
    ...wwWwwwwDDDDDDwwwwDDDDDDwwwwwvv...
    ...wWWwwwwDDDDDwwwwwwDDDDDwwwwwvv...
    ...wWwwwwwwDDDDwwwwwwDDDDwwwwwwvv...
    ...wWwwwwwwwwwwwwKKwwwwwwwwwwwwvv...
    ...wWwwwwwwwwwwwwKKwwwwwwwwwwwwvv...
    ...wWwwwwwwwwwwwwKKwwwwwwwwwwwwvv...
    ...wwwwwwwwwwwwwwwwwwwwwwwwwwwwvv...
    ...DDDDDDDDDDDDwwwwwwDDDDDDDDDddd...
    ....DDDDDDDDDDDDDDDDDDDDDDDDDDdd....
    ....DDDDDDDDDDDDDDDDDDDDDDDDDDdd....
    ....DDDDDDDDDDDDDDDDDDDDDDDDDddd....
    .....DDDDDDDDDDDDDDDDDDDDDDDddd.....
    ......DDDDDDDDDDDDDDDDDDDDDDdd......
    ......dDDDDDDDDDDDDDDDDDDDDddd......
    .......ddDDDDDDDDDDDDDDDDdddd.......
    ........ddDDDDDDDDDDDDDDdddd........
    ..........dddDDDDDDDDddddd..........
    ...........dddddddddddddd...........
    ..............dddddddd..............
    ....................................
    ....................................
    ....................................
'''), part('''
    ..........DDDDDdd...................
    .........DDhhhddd...................
    ..........hhDDDD....................
    ..........DDDDwwwwwwww..............
    ...........wwwwwwwWWwwwww...........
    ..........wwwwwwwwwwwwwwww..........
    ........wwwWWwwwwwwwwwwwwwwv........
    .......wwWWWwwwwwwwwwwwwwwwwv.......
    ......wwWWwwwwwwwwwwwwwwwwwwvv......
    .D....wWWwwwwwwwwwwDDwwwwwwwwv......
    DDhD.wwWwwwwwwwwwDDDDDwwwwwwwDd.....
    DhhDwwWWwwwwwwwwwDDDDDwwwwwwDDdd....
    DhDDwwWwwwwwwwwwwDDDDDwwwwwDDDdd....
    DhDDwwwwwwwwwwwwwDDDDwwwwwDDDDDd....
    DdDwwwwwwwwwwwwwwwDwwwwwwDDDDDDdd...
    ddDwwwwwwwwwwwwwwwwwwwwwDDDDDDDdd...
    dd.wwwwwwwwwwwwwwwwwwwwDDDDDDDDdd...
    ...wwwwwwwDDDDwwwKKwwwDDDDDDDDDdd...
    ...wWwwwwwDDDDDwwKKKwwDDDDDDDDDdd...
    ...wWwwwwDDDDDwwwwKwwwDDDDDDDDDdd...
    ...wwwwwwDDDDDwwwwwwwwDDDDDDDDDdd...
    ...wwwwwwwDDDwwwwwwwwDDDDDDDDDddd...
    ....wwwwwwwwwwwwwDDDDDDDDDDDDDdd....
    ....wwwwwwwwwwwwDDDDDDDDDDDDDDdd....
    ....wwwwwwwwwwwDDDDDDDDDDDDDDddd....
    .....wwwwwwwwwDDDDDDDDDDDDDDddd.....
    ......wwwwwwwDDDDDDDDDDDDDDDdd......
    ......vwwwwwDDDDDDDDDDDDDDDddd......
    .......vvwwDDDDDDDDDDDDDDdddd.......
    ........vvDDDDDDDDDDDDDDdddd........
    ..........dddDDDDDDDDddddd..........
    ...........dddddddddddddd...........
    ..............dddddddd..............
    ....................................
    ....................................
    ....................................
'''), part('''
    ....................................
    ....................................
    ....................................
    ..............wwwwwwwD..............
    ...........wwwwWWWWWwDDDD...........
    ..........wwWWWWwwwwwDDDDD..........
    .DDD....wwwWWwwwwwwwwDDDDDDd........
    DDhhhD.wwWWWwwwwwwwwwDDDDDDDd.......
    DhhDDDwwWWwwwwwwwwwwwDDDDDDDdd......
    DhDDDDwwWwwwwwwwwwwwwDDDDDDDDd......
    DhDDDwwwwwwwwDDDwwwwwDDDDDDDDDd.....
    dddDwwwwwwwwDDDDDwwwwDDDDDDDDDdd....
    ddDDwwwwwwwwDDDDDwwwwDDDDDDDDDdd....
    ....wwwwwwwwDDDDDwwwwDDDDDDDDDDd....
    ...wwwwwwwwwwDDDDwwwwDDDDDDDDDDdd...
    ...wWWwwwwwwwwDwwwwwwwDDDDDDDDDdd...
    ...wWwwwwwwwwwwwwwwwwwDDDDDDDDDdd...
    ...wWwwwwwwwwwwwwKKKwwDDDDDDDDDdd...
    ...wWwwwwwwwwwwwwKKKwwDDDDDDDDDdd...
    ...wWwwwwwwwwwwwwwwwwwDDDDDDDDDdd...
    ...wwwwwwwwwwwDwwwwwwwDDDDDDDDDdd...
    ...wwwwwwwwwwDDDDwwwwDDDDDDDDDddd...
    ....wwwwwwwwDDDDDwwwwDDDDDDDDDdd....
    DDDDwwwwwwwwDDDDDwwwwDDDDDDDDDdd....
    DDDDwwwwwwwwDDDDDwwwwDDDDDDDDddd....
    DDDDDwwwwwwwwDDDwwwwwDDDDDDDddd.....
    DDDDDdwwwwwwwwwwwwwwwDDDDDDDdd......
    DDDdddvwwwwwwwwwwwwwwDDDDDDddd......
    dddddd.vvwwwwwwwwwwwwDDDDdddd.......
    .ddd....vvwwwwwwwwwwwDDDdddd........
    ..........vvvwwwwwwwwddddd..........
    ...........vvvvvvvvvvdddd...........
    ..............vvvvvvvd..............
    ....................................
    ....................................
    ....................................
'''), part('''
    ....................................
    ....................................
    ....................................
    ..............DDDDDDDD..............
    ...........DDDDhhhhhDDDDD...........
    ..........DDhhhhDDDDDDDDDD..........
    ........wwDhhDDDDDDDDDDDDDDd........
    .......wwWWhDDDDDDDDDDDDDDDDd.......
    ......wwWWwwDDDDDDDDDDDDDDDDdd......
    ......wWWwwwwDDDDDDDDDDDDDDDDd......
    .....wwWwwwwwwDDDDDDDDDDDDDDDDd.....
    ....wwWWwwwwwwwDDDDDDDDDDDDDDDdd....
    ....wWWwwwwwwwwwDDDDDDDDDDDDDDdd....
    ....wWwwwwwwwwwwwDDDDDDDDDDDDDDd....
    ...wwWwwwwDDDwwwwwwwwDDDDDDDDDDdd...
    ...wWWwwwDDDDDwwwwwwwwDDDDDDDDDdd...
    ...wWwwwwDDDDDwwwwKwwwDDDDDDDDDdd...
    ...wWwwwwwDDDDDwwKKKwwDDDDDDDDDdd...
    ...wWwwwwwDDDDwwwKKwwwDDDDDDDDDdd...
    DD.wWwwwwwwwwwwwwwwwwwwDDDDDDDDdd...
    DDDwwwwwwwwwwwwwwwwwwwwwDDDDDDDdd...
    DDDwwwwwwwwwwwwwwwDwwwwwwDDDDDddd...
    DDDDwwwwwwwwwwwwwDDDDwwwwwDDDDdd....
    DDDdwwwwwwwwwwwwwDDDDDwwwwwDDDdd....
    DdddwwwwwwwwwwwwwDDDDDwwwwwwDddd....
    dddd.wwwwwwwwwwwwDDDDDwwwwwwvdd.....
    .d....wwwwwwwwwwwwwDDwwwwwwwvv......
    ......vwwwwwwwwwwwwwwwwwwwwvvv......
    .......vvwwwwwwwwwwwwwwwwvvvv.......
    ........vvwwwwwwwwwwwwwwvvvv........
    ..........wwwwwwwwwwwvvvvv..........
    ...........wwwwvvvvvvvvvv...........
    ..........DDDDvvvvvvvv..............
    ..........DDDDDd....................
    .........dddddddd...................
    ..........ddddddd...................
'''), part('''
    ....................................
    ....................................
    ....................................
    ..............DDDDDDDD..............
    ...........DDDDhhhhhDDDDD...........
    ..........DDhhhhDDDDDDDDDD..........
    ........DDDhhDDDDDDDDDDDDDDd........
    .......DDhhhDDDDDDDDDDDDDDDDd.......
    ......DDhhDDDDDDDDDDDDDDDDDDdd......
    ......DhhDDDDDDDDDDDDDDDDDDDDd......
    .....DDhDDDDDDDDDDDDDDDDDDDDDDd.....
    ....DDhhDDDDDDDDDDDDDDDDDDDDDDdd....
    ....DhhDDDDDDDDDDDDDDDDDDDDDDDdd....
    ....DhDDDDDDDDDDDDDDDDDDDDDDDDDd....
    ...DDhDDDDDDDDDwwwwwwDDDDDDDDDDdd...
    ...wWWwwwwwwwwwwwwwwwwwwwwwwwwwvv...
    ...wWwwwwwwwwwwwwKKwwwwwwwwwwwwvv...
    ...wWwwwwwwwwwwwwKKwwwwwwwwwwwwvv...
    ...wWwwwwwwwwwwwwKKwwwwwwwwwwwwvv...
    ...wWwwwwwwDDDDwwwwwwDDDDwwwwwwvv...
    ...wwwwwwwDDDDDwwwwwwDDDDDwwwwwvv...
    ...wwwwwwwDDDDDDwwwwDDDDDDwwwwvvv...
    ....wwwwwwDDDDDwwwwwwDDDDDwwwwvv....
    ....wwwwwwwDDDwwwwwwwwDDDwwwwwvv....
    ....wwwwwwwwwwwwwwwwwwwwwwwwwvvv....
    .....wwwwwwwwwwwwwwwwwwwwwwwvvv.....
    ......wwwwwwwwwwwwwwwwwwwwwwvv......
    ......wwwwwwwwwwwwwwwwwwwwwvvv......
    .......wwwwwwwwwwwwwwwwwwwvvv.......
    ........wwwwwwwwwwwwwwwwwwwv........
    .......DDDwwvwwwwwwwwvwwwwDdd.......
    .......DDDDvvvvvvvvvvvwwwDDDd.......
    ......DDDDDdd.vvvvvvvv.DDDDDdd......
    ......DDDDDdd..........DDDDDdd......
    ......ddddddd..........ddddddd......
    .......dddddd..........dddddd.......
'''), part('''
    ....................................
    ....................................
    ....................................
    ..............DDDDDDDD..............
    ...........DDDDhhhhhDDDDD...........
    ..........DDhhhhDDDDDDDDDD..........
    ........DDDhhDDDDDDDDDDDDDwv........
    .......DDhhhDDDDDDDDDDDDDwwwv.......
    ......DDhhDDDDDDDDDDDDDDwwwwvv......
    ......DhhDDDDDDDDDDDDDDwwwwwwv......
    .....DDhDDDDDDDDDDDDDDwwwwwwwwv.....
    ....DDhhDDDDDDDDDDDDDwwwwwwwwwvv....
    ....DhhDDDDDDDDDDDDDwwwwwwwwwwvv....
    ....DhDDDDDDDDDDDDDwwwwwwwwwwwwv....
    ...DDhDDDDDDDDDwwwwwwwwDDDwwwwwvv...
    ...DhhDDDDDDDDwwwwwwwwDDDDDwwwwvv...
    ...DhDDDDDDDDDwwwKwwwwDDDDDwwwwvv...
    ...DhDDDDDDDDDwwKKKwwDDDDDwwwwwvv...
    ...DhDDDDDDDDDwwwKKwwwDDDDwwwwwvw...
    ...DhDDDDDDDDwwwwwwwwwwwwwwwwwwww.dd
    ...DDDDDDDDDwwwwwwwwwwwwwwwwwwwwwDdd
    ...DDDDDDDDwwwwwwDwwwwwwwwwwwwwwwDdd
    ....DDDDDDwwwwwDDDDwwwwwwwwwwwwwDDdd
    ....DDDDDwwwwwDDDDDwwwwwwwwwwwvwDDdd
    ....DDDDwwwwwwDDDDDwwwwwwwwwwvvvdDdd
    .....DDwwwwwwwDDDDDwwwwwwwwwvvv.dddd
    ......wwwwwwwwwDDwwwwwwwwwwwvv....d.
    ......vwwwwwwwwwwwwwwwwwwwwvvv......
    .......vvwwwwwwwwwwwwwwwwvvvv.......
    ........vvwwwwwwwwwwwwwwvvvv........
    ..........vvvwwwwwwwwwwvvv..........
    ...........vvvvvvvvwwwwwv...........
    ..............vvvvwwwwDDdd..........
    ....................DDDDDd..........
    ...................dddddddd.........
    ...................ddddddd..........
'''), part('''
    ....................................
    ....................................
    ....................................
    ..............Dwwwwwww..............
    ...........DDDDWWWWWwwwww...........
    ..........DDhhhWwwwwwwwwww..........
    ........DDDhhDDwwwwwwwwwwwww....DDd.
    .......DDhhhDDDwwwwwwwwwwwwww.DDDDdd
    ......DDhhDDDDDwwwwwwwwwwwwwwwDDDDdd
    ......DhhDDDDDDwwwwwwwwwwwwwwwDDDDdd
    .....DDhDDDDDDDwwwwwDDDwwwwwwwwDDDdd
    ....DDhhDDDDDDDwwwwDDDDDwwwwwwwvdddd
    ....DhhDDDDDDDDwwwwDDDDDwwwwwwvvdddd
    ....DhDDDDDDDDDwwwwDDDDDwwwwwwwv....
    ...DDhDDDDDDDDDwwwwDDDDwwwwwwwwvv...
    ...DhhDDDDDDDDwwwwwwwDwwwwwwwwwvv...
    ...DhDDDDDDDDDwwwwwwwwwwwwwwwwwvv...
    ...DhDDDDDDDDDwwKKKwwwwwwwwwwwwvv...
    ...DhDDDDDDDDDwwKKKwwwwwwwwwwwwvv...
    ...DhDDDDDDDDDwwwwwwwwwwwwwwwwwvv...
    ...DDDDDDDDDDDwwwwwwwDwwwwwwwwwvv...
    ...DDDDDDDDDDDDwwwwDDDDwwwwwwwvvv...
    ....DDDDDDDDDDDwwwwDDDDDwwwwwwww....
    ....DDDDDDDDDDDwwwwDDDDDwwwwwwwwDDdd
    ....DDDDDDDDDDDwwwwDDDDDwwwwwwwwDDdd
    .....DDDDDDDDDDwwwwwDDDwwwwwwwwDDDdd
    ......DDDDDDDDDwwwwwwwwwwwwwvwDDDDdd
    ......dDDDDDDDDwwwwwwwwwwwwvvvdDDDdd
    .......ddDDDDDDwwwwwwwwwwvvvv.dddddd
    ........ddDDDDDwwwwwwwwwvvvv....ddd.
    ..........dddDDwwwwwwvvvvv..........
    ...........ddddvvvvvvvvvv...........
    ..............dvvvvvvv..............
    ....................................
    ....................................
    ....................................
'''), part('''
    ...................DDDDDDd..........
    ...................DDDDDddd.........
    ....................DDDDdd..........
    ..............wwwwwwwwDddd..........
    ...........wwwwWWWWWwwwww...........
    ..........wwWWWWwwwwwwwwww..........
    ........wwwWWwwwwwwwwwwwwwwv........
    .......wwWWWwwwwwwwwwwwwwwwwv.......
    ......wwWWwwwwwwwwwwwwwwwwwwvv......
    ......wWWwwwwwwDDwwwwwwwwwwwwv....d.
    .....DDWwwwwwwDDDDDwwwwwwwwwwww.DDdd
    ....DDhhwwwwwwDDDDDwwwwwwwwwwwwwDDdd
    ....DhhDDwwwwwDDDDDwwwwwwwwwwwwwDDdd
    ....DhDDDDwwwwwDDDDwwwwwwwwwwwwwDDdd
    ...DDhDDDDDwwwwwwDwwwwwwwwwwwwwwvDdd
    ...DhhDDDDDDwwwwwwwwwwwwwwwwwwwvvddd
    ...DhDDDDDDDDwwwwwwwwwwwwwwwwwwvv.dd
    ...DhDDDDDDDDDwwwKKwwwDDDDwwwwwvv...
    ...DhDDDDDDDDDwwKKKwwDDDDDwwwwwvv...
    ...DhDDDDDDDDDwwwKwwwwDDDDDwwwwvv...
    ...DDDDDDDDDDDwwwwwwwwDDDDDwwwwvv...
    ...DDDDDDDDDDDDwwwwwwwwDDDwwwwvvv...
    ....DDDDDDDDDDDDDDDwwwwwwwwwwwvv....
    ....DDDDDDDDDDDDDDDDwwwwwwwwwwvv....
    ....DDDDDDDDDDDDDDDDDwwwwwwwwvvv....
    .....DDDDDDDDDDDDDDDDDwwwwwwvvv.....
    ......DDDDDDDDDDDDDDDDDwwwwwvv......
    ......dDDDDDDDDDDDDDDDDDwwwvvv......
    .......ddDDDDDDDDDDDDDDDDvvvv.......
    ........ddDDDDDDDDDDDDDDddvv........
    ..........dddDDDDDDDDddddd..........
    ...........dddddddddddddd...........
    ..............dddddddd..............
    ....................................
    ....................................
    ....................................
''')]

# --- small parts ----------------------------------------------------------------------------

EAR = part('''
    ..DDDD..
    .DhhDDD.
    DhhDDDDd
    DhDDDDdd
    DDDDDDdd
    .DDDDdd.
''')

# Sole of a foot stretched forward: pad and three toe beans
FOOT = part('''
    ..DDDDDDD..
    .DhPDPDPDD.
    DhDDDDDDDDd
    DDDPPPPPDDd
    DDPPPPPPPDd
    DDPPPPPPPDd
    DDDPPPPPDdd
    .Ddddddddd.
    ..ddddddd..
''')

ARM = part('''
    .DDDDD.
    DhhDDDD
    DhDDDDd
    DhDDDDd
    DDDDDDd
    DDDDDDd
    DDDDDDd
    DDDDDdd
    DDDDDdd
    .DDDdd.
    ..ddd..
''')

# Arm flung out sideways and up ("ta-da!"), the far arm; flipped for the near one
ARM_SPREAD = part('''
    ......DDD.
    .....DhDDd
    ...DDhDDDd
    .DDDDDDDd.
    DDDDDDDd..
    DDDDDdd...
    .dddd.....
''')

# Near arm raised to rub the eye: paw on top, elbow down towards the belly
ARM_RUB = part('''
    .DDDD..
    DhDDDd.
    DhDDDd.
    .DDDDd.
    ..DDDd.
    ..DDDd.
    ..DDDdd
    ..DDDdd
    ...DDdd
    ...DDdd
    ...Ddd.
''')

# Forearm bent up from the belly to a paw holding something
ARM_BENT = part('''
    ....DD
    ...DDd
    ..DDDd
    .DhDdd
    DhDDd.
    DDDd..
    DDdd..
    .dd...
''')

# Paws around the bamboo: near paw in front of it, far paw behind
PAW_GRIP = part('''
    .DDDD.
    DhDDDd
    DDDDDd
    DDDDdd
    .dddd.
''')

TAIL = part('''
    .ww.
    wwwv
    wwvv
    .vv.
''')

# Walking leg seen from the side: thick thigh that starts on the body, round paw, toes
LEG = part('''
    ..DDDDD..
    .DhDDDDD.
    DhDDDDDDd
    DhDDDDDDd
    DDDDDDDDd
    .DDDDDDd.
    .DDDDDDd.
    .DDDDDDd.
    .DDDDDDd.
    .DDDDDDdd
    DDdDdDDdd
    DDdDdDddd
''')

# Black band over the shoulders while walking (clipped to the body)
SADDLE = part('''
    ...DDDDDD...
    ..DhDDDDDDd.
    .DhDDDDDDDdd
    DhDDDDDDDDdd
    DDDDDDDDDDdd
    DDDDDDDDDDd.
    DDDDDDDDDdd.
    DDDDDDDDdd..
    DDDDDDDDd...
    DDDDDDDdd...
    DDDDDDDd....
    DDDDDDdd....
    DDDDDDd.....
    DDDDDdd.....
''')

# Eye patches ("glasses"), drooping outwards
PATCH_LEFT = part('''
    ...DD.
    ..DDDD
    .DhDDD
    DhDDDD
    DDDDD.
    DDDd..
    .dd...
''')
PATCH_RIGHT = part('''
    .DD...
    DDDD..
    DDDDD.
    DDDDDd
    .DDDDd
    ..dDdd
    ...dd.
''')

EYE = part('''
    WK
    KK
    KK
''')
EYE_HALF = part('''
    KK
    KK
''')
EYE_SHUT = part('''
    xx
''')
EYE_SLEEP = part('''
    v..v
    .vv.
''')
EYE_HAPPY = part('''
    .vv.
    v..v
''')
EYE_DIZZY = part('''
    v.v
    .v.
    v.v
''')

NOSE = part('''
    hKK
    .K.
''')
MOUTH = part('''
    x..x
    .xx.
''')
MOUTH_SMILE = part('''
    x...x
    .xxx.
''')
MOUTH_OPEN = part('''
    xppx
    .pp.
''')
MOUTH_CHEW = part('''
    .xx.
    .xx.
''')
MOUTH_O = part('''
    .x.
    x.x
    .x.
''')
BLUSH = part('''
    PP
''')

BAMBOO = part('''
    LGg
    LGg
    LGg
    Lgg
    LGg
    LGg
    LGg
    LGg
    LGg
    Lgg
    LGg
    LGg
    LGg
    LGg
    LGg
    Lgg
    LGg
    LGg
    LGg
''')
# Leaf on a twig at the top of the stalk, in two positions so it can sway
LEAF_A = part('''
    LL....
    LLLG..
    .LGGg.
    ...Ggg
    ....gG
''')
LEAF_B = part('''
    ......
    LLLL..
    .LLGGg
    ...ggg
    ....gG
''')
# A maple leaf like the ones falling in the maple park (in the background's colours), as the pets
# hold it after their interactions with the backgrounds (BackgroundEvents)
MAPLE_LEAF = part('''
    .O.O.
    OOROO
    .RRR.
    ..r..
''')
# The same leaf tilted, swung down in a fanning stroke
MAPLE_LEAF_TILT = part('''
    OO...
    OORO.
    .ORR.
    ...r.
''')

# Arm laid over the top of something big in front (the pumpkin it hugs), the paw drooping over
ARM_HUG = part('''
    ...DDDDDD
    .DDhDDDDD
    DhhDDDDDd
    DhDDDDDdd
    DDDDDddd.
    .dddd....
''')

CRUMBS = part('''
    L.G
    .g.
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
HEART = part('''
    .HH.HH.
    HWHHHHH
    HHHHHHH
    .HHHHH.
    ..HHH..
    ...H...
''')
HEART_SMALL = part('''
    H.H
    HHH
    .H.
''')

# --- accessories ----------------------------------------------------------------------------
# The engine sets `wearing` before building frames; every pose that draws a head wears it.

ACCESSORIES = ('bow', 'halloween_hat', 'winter_hat')
NO_ACCESSORY = ('roll',)       # a tumbling ball can't keep a hat on
wearing = None

BOW = part('''
    HH.....HH
    HHHH.HHHH
    HHHHpHHHH
    HHHH.HHHH
    HH.....HH
''')

WITCH_HAT = part('''
    .......DDD......
    ......DDDD......
    .....DhDDD......
    .....DhDDDD.....
    ....DhDDDDDD....
    ....GGGGGGGG....
    ....GGLLGGGG....
    .DDDDDDDDDDDDdd.
    DhhDDDDDDDDDDDdd
    .dddddddddddddd.
''')

SANTA_HAT = part('''
    .....HHHH.......
    ....HHHHHH......
    ...HHHHHHHH.....
    ..HHHHHHHHHHH...
    ..HHHHHHHHHHHHp.
    .HHHHHHHHHH.pWWW
    .HHHHHHHHHp..WwW
    WWWWWWWWWWWW.www
    wwwwwwwwwwww....
''')


def accessory(hx, hy):
    """The worn accessory, placed relative to the head's top-left corner."""
    if wearing == 'bow':
        return [L(BOW, hx + 21, hy - 5, shade=False)]
    if wearing == 'halloween_hat':
        return [L(WITCH_HAT, hx + 7, hy - 7)]
    if wearing == 'winter_hat':
        return [L(SANTA_HAT, hx + 7, hy - 6)]
    return []


def accessory_icon(name):
    """The accessory on its own, for the wardrobe in the app."""
    rows = {'bow': BOW, 'halloween_hat': WITCH_HAT, 'winter_hat': SANTA_HAT}[name]
    return [L(rows, 1, 1)], (len(rows[0]) + 2, len(rows) + 2)


# --- poses ----------------------------------------------------------------------------------

def face(hx, hy, eyes='open', mouth='closed', look=0):
    """Face decals relative to the head's top-left corner; look shifts the eyes sideways."""
    layers = [
        L(PATCH_LEFT, hx + 6, hy + 8, ring=False, clip=True),
        L(PATCH_RIGHT, hx + 18, hy + 8, ring=False, clip=True),
    ]
    lx, rx = hx + 9 + look, hx + 19 + look
    if eyes == 'open':
        layers += [L(EYE, lx, hy + 10, ring=False), L(EYE, rx, hy + 10, ring=False)]
    elif eyes == 'half':
        layers += [L(EYE_HALF, lx, hy + 11, ring=False), L(EYE_HALF, rx, hy + 11, ring=False)]
    elif eyes == 'shut':
        layers += [L(EYE_SHUT, lx, hy + 12, ring=False), L(EYE_SHUT, rx, hy + 12, ring=False)]
    elif eyes == 'sleep':
        layers += [L(EYE_SLEEP, hx + 7, hy + 11, ring=False), L(EYE_SLEEP, hx + 18, hy + 11, ring=False)]
    elif eyes == 'happy':
        layers += [L(EYE_HAPPY, hx + 7, hy + 10, ring=False), L(EYE_HAPPY, hx + 18, hy + 10, ring=False)]
    elif eyes == 'dizzy':
        layers += [L(EYE_DIZZY, hx + 8, hy + 10, ring=False), L(EYE_DIZZY, hx + 18, hy + 10, ring=False)]
    layers.append(L(NOSE, hx + 13, hy + 15, ring=False))
    mouths = {'closed': (MOUTH, 12), 'smile': (MOUTH_SMILE, 12), 'open': (MOUTH_OPEN, 12),
              'chew': (MOUTH_CHEW, 12), 'o': (MOUTH_O, 13)}
    rows, mx = mouths[mouth]
    layers.append(L(rows, hx + mx, hy + 17, ring=False))
    layers += [L(BLUSH, hx + 4, hy + 16, ring=False, clip=True),
               L(BLUSH, hx + 24, hy + 16, ring=False, clip=True)]
    return layers


def head(hx, hy, eyes='open', mouth='closed', ear_near=0, ear_far=0, look=0):
    return [
        L(EAR, hx + 21, hy - 3 + ear_far, recolor=FAR),
        L(EAR, hx + 1, hy - 3 + ear_near),
        L(HEAD, hx, hy),
    ] + face(hx, hy, eyes, mouth, look) + accessory(hx, hy)


def ground(height=0, walking=False):
    """The shadow stays on the ground; it narrows as the panda leaves it."""
    if walking:
        return [L(SHADOW_WALK, 11, 61, ring=False)]
    rows = SHADOW_WIDE if height < 3 else SHADOW_MID if height < 6 else SHADOW_SMALL
    return [L(rows, 31 - len(rows[0]) // 2, 61, ring=False)]


BODY_X, BODY_Y = 17, 36
HEAD_X, HEAD_Y = 11, 13


def sitting(dy=0, squash=0, head_dx=0, head_dy=0, eyes='open', mouth='closed', ear_near=0, ear_far=0,
            arms='rest', arm_dy=0, look=0, bamboo_top=None, leaf='a', crumbs=False, effects=(), shadow=True):
    """Sitting with the soles forward. dy lifts the whole panda (jumps); squash > 0 flattens the
    body (landing, crouching), < 0 stretches it; head_dy moves only the head (breathing, nodding).
    arms: rest (hugging the belly), up (flung out, "ta-da!"), rub (near paw rubbing the eye),
    hold (both paws around a bamboo stalk whose top is at bamboo_top), fan (the near paw waving a
    maple leaf, leaf 'a' upright or 'b' tilted), hug (laid over something big in front of it)."""
    body_top = BODY_Y + dy + max(squash, 0) - max(-squash, 0)
    hx = HEAD_X + head_dx
    hy = HEAD_Y + dy + head_dy + 2 * max(squash, 0) - max(-squash, 0)
    layers = list(ground(-dy)) if shadow else []
    layers.append(L(BODY, BODY_X, body_top, squash_y=max(squash, 0), grow_y=max(-squash, 0)))
    layers.append(L(TAIL, 44, BODY_Y + dy + 17))
    layers.append(L(FOOT, 34, 53 + dy, recolor=FAR))
    layers.append(L(FOOT, 15, 53 + dy))
    if arms in ('rest', 'rub', 'fan'):
        layers.append(L(ARM, 32, body_top + 5 + (0 if arms == 'fan' else arm_dy), recolor=FAR))
    if arms == 'rest':
        layers.append(L(ARM, 20, body_top + 4 + arm_dy))
    if arms == 'hold':
        layers.append(L(ARM_BENT, 31, body_top + 4, recolor=FAR, flip_h=True))
        layers.append(L(PAW_GRIP, 28, hy + 26 + arm_dy, recolor=FAR))
    elif arms == 'up':
        layers.append(L(ARM_SPREAD, BODY_X + 24, body_top - 1 + arm_dy, recolor=FAR))
        layers.append(L(ARM_SPREAD, BODY_X - 6, body_top - 1 + arm_dy, flip_h=True))
    layers += head(hx, hy, eyes, mouth, ear_near, ear_far, look)
    if arms == 'hold':
        top = hy + 17 if bamboo_top is None else bamboo_top
        layers.append(L(BAMBOO, hx + 13, top))
        layers.append(L(LEAF_A if leaf == 'a' else LEAF_B, hx + 7, top + 2))
        if crumbs:
            layers.append(L(CRUMBS, hx + 10, hy + 19, ring=False))
        layers.append(L(ARM_BENT, 18, body_top + 6))
        layers.append(L(PAW_GRIP, hx + 9, hy + 27 + arm_dy))
    elif arms == 'rub':
        layers.append(L(ARM_RUB, hx + 5, hy + 10 + arm_dy))
    elif arms == 'fan':
        # The near paw holds the maple leaf by its stem beside the face
        layers.append(L(ARM_RUB, hx + 1, hy + 16 + arm_dy))
        layers.append(L(MAPLE_LEAF if leaf == 'a' else MAPLE_LEAF_TILT, hx - 2, hy + 12 + arm_dy))
    elif arms == 'hug':
        # Both arms laid over the shoulders of the pumpkin in front of it
        layers.append(L(ARM_HUG, 42, body_top - 2 + arm_dy, recolor=FAR, flip_h=True))
        layers.append(L(ARM_HUG, 12, body_top - 3 + arm_dy))
    return layers + list(effects)


def zzz(t, period=16):
    """Z letters drifting up and to the right from the nodding head, growing as they rise."""
    layers = []
    for phase in (0, period // 2):
        s = (t + phase) % period
        if s >= 12:
            continue
        x = 40 + s // 2 + (2 if phase else 0)
        y = 13 - s
        layers.append(L(Z_SMALL if s < 6 else Z_BIG, x, y))
    return layers


# --- animations -----------------------------------------------------------------------------

def frames(*pattern):
    """Expands (layers, count) pairs: a key pose held for count frames."""
    out = []
    for layers, count in pattern:
        out += [layers] * count
    return out


def idle_frames():
    """Breathes, blinks (through half-shut eyes), glances left, rubs a sleepy eye with a paw,
    flicks an ear; ends as it began."""
    calm = sitting()
    inhale = sitting(squash=-1)
    return frames(
        (calm, 2), (inhale, 2), (calm, 1),
        (sitting(eyes='half'), 1), (sitting(eyes='shut'), 1), (sitting(eyes='half'), 1),
        (calm, 1), (sitting(look=-1), 2),
        (sitting(arms='rub', eyes='half'), 1),
        (sitting(arms='rub', eyes='half', arm_dy=1), 1),
        (sitting(arms='rub', eyes='half'), 1),
        (sitting(arms='rub', eyes='half', arm_dy=1), 1),
        (sitting(eyes='half', ear_far=-1), 1),
        (inhale, 2), (calm, 1),
    )


def sleep_frames():
    """Sits and nods off: the head sinks slowly, jerks up a little, sinks again. Z rise."""
    nod = [0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 4, 1, 1, 0, 0, 0]
    breath = [0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0]
    out = []
    for f in range(16):
        ears = nod[f - 1] if f else nod[-1]          # ears follow the head a frame late
        out.append(sitting(head_dy=nod[f], squash=-breath[f], eyes='sleep', mouth='o' if nod[f] >= 4 else 'closed',
                           ear_near=ears - nod[f], ear_far=ears - nod[f], arm_dy=1, effects=zzz(f)))
    return out


def eat_frames():
    """Holds the bamboo upright in both paws: lifts it to the mouth, bites, chews; the leaf
    on top sways."""
    hy = HEAD_Y

    def pose(top, mouth='chew', eyes='open', leaf='a', crumbs=False, head_dy=0):
        return sitting(arms='hold', bamboo_top=top, mouth=mouth, eyes=eyes, leaf=leaf, crumbs=crumbs,
                       head_dy=head_dy)
    low, bite = hy + 20, hy + 16
    return frames(
        (pose(low, 'closed', leaf='a'), 2),
        (pose(low - 2, 'open', leaf='b'), 1),
        (pose(bite, 'open', leaf='b', head_dy=1), 1),
        (pose(low, 'chew', leaf='a', crumbs=True), 1),
        (pose(low, 'closed', leaf='a', eyes='half'), 1),
        (pose(low, 'chew', leaf='b', eyes='half'), 1),
        (pose(low, 'closed', leaf='b', eyes='half'), 1),
        (pose(low, 'chew', leaf='a', eyes='happy'), 1),
        (pose(low, 'smile', leaf='a', eyes='happy'), 2),
        (pose(low, 'closed', leaf='b'), 2),
        (pose(low, 'closed', leaf='a'), 3),
    )


def roll_frames():
    """Crouches, curls into a ball, tumbles over once, lands, shakes off the dizziness."""
    hops = [0, -2, -4, -5, -5, -4, -2, 0]
    out = [sitting(), sitting(squash=2, head_dy=2, eyes='shut')]
    for step, dy in enumerate(hops):
        out.append(ground(-dy) + [L(BALLS[step], 13, 27 + dy)])
    out += [
        ground(0) + [L(BALLS[0], 13, 29, squash_y=2)],
        sitting(squash=2, head_dy=1, eyes='dizzy'),
        sitting(head_dx=-1, eyes='dizzy', ear_near=1),
        sitting(head_dx=1, eyes='dizzy', ear_far=1),
        sitting(eyes='happy'),
        sitting(eyes='happy'),
    ]
    return out


def happy_frames():
    """Crouch, jump with the paws up (rising and falling a few pixels a frame), a heart floats
    out, land with a squash and settle."""
    def heart(t):
        return [L(HEART if t < 4 else HEART_SMALL, 44 + t // 2, 14 - 2 * t)]
    return frames(
        (sitting(eyes='happy', mouth='smile'), 1),
        (sitting(squash=2, head_dy=1, eyes='happy'), 1),
        (sitting(dy=-3, squash=-1, eyes='happy', mouth='open', arms='up', ear_near=1, ear_far=1), 1),
        (sitting(dy=-5, eyes='happy', mouth='open', arms='up', ear_near=1, ear_far=1), 1),
        (sitting(dy=-6, eyes='happy', mouth='open', arms='up', effects=heart(0)), 1),
        (sitting(dy=-7, eyes='happy', mouth='open', arms='up', effects=heart(1)), 1),
        (sitting(dy=-7, eyes='happy', mouth='open', arms='up', effects=heart(2)), 1),
        (sitting(dy=-4, eyes='happy', mouth='open', arms='up', ear_near=-1, ear_far=-1, effects=heart(3)), 1),
        (sitting(dy=-2, eyes='happy', mouth='open', arms='up', ear_near=-1, ear_far=-1), 1),
        (sitting(squash=2, head_dy=1, eyes='happy', effects=heart(4)), 1),
        (sitting(squash=-1, head_dy=-1, eyes='happy', effects=heart(5)), 1),
        (sitting(eyes='happy', mouth='smile', effects=heart(6)), 1),
        (sitting(eyes='happy', mouth='smile'), 1),
        (sitting(), 1),
    )


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
LEGS = ((21, 4, True), (39, 0, True), (15, 0, False), (33, 4, False))


def walking(f):
    """Waddles on all fours, frame f of 8: diagonal legs step together, the planted paws moving
    back by WALK_SHIFTS, the body bobs a pixel when both pairs are down and the head follows a
    frame late."""
    bob = 1 if f % 4 == 0 else 0
    head_bob = 1 if (f - 1) % 4 == 0 else 0

    def leg(i):
        base, phase, far = LEGS[i]
        x, lift = leg_x(f, phase, base)
        return L(LEG, x, 50 - lift, recolor=FAR if far else None)

    layers = list(ground(walking=True))
    layers.append(L(TAIL, 49, 42 + bob))
    layers += [leg(0), leg(1)]
    layers.append(L(SIDE_BODY, 18, 36 + bob))
    layers.append(L(SADDLE, 18, 36 + bob, ring=False, clip=True))
    layers += [leg(2), leg(3)]
    layers += head(2, 19 + head_bob, eyes='open')
    return layers


def standing():
    """On all fours and still, between sitting and walking."""
    layers = list(ground(walking=True))
    layers.append(L(TAIL, 49, 43))
    for base, _, far in LEGS[:2]:
        layers.append(L(LEG, base + 3, 50, recolor=FAR if far else None))
    layers.append(L(SIDE_BODY, 18, 37))
    layers.append(L(SADDLE, 18, 37, ring=False, clip=True))
    for base, _, far in LEGS[2:]:
        layers.append(L(LEG, base + 3, 50, recolor=FAR if far else None))
    layers += head(3, 20, eyes='open')
    return layers


def turn_frames():
    """It sits facing you: turning round is that pose for a moment (PetBehavior mirrors after it)."""
    return [sitting()]


def walk_start_frames():
    """Leans forward and gets onto all fours."""
    return [sitting(squash=2, head_dy=1), standing()]


def walk_stop_frames():
    return [standing(), sitting(squash=2, head_dy=1)]


def walk_frames():
    return [walking(f) for f in range(8)]


# --- the backgrounds' interactions (BackgroundEvents) -------------------------------------

def fan_frames():
    """Fans itself with the maple leaf it caught in the maple park: the near paw waves it beside
    the face, eyes half shut with pleasure, a happy blink now and then."""
    def stroke(up, eyes='half'):
        return sitting(arms='fan', arm_dy=-1 if up else 1, leaf='a' if up else 'b', eyes=eyes, mouth='smile')
    return frames(
        (stroke(True), 1), (stroke(False), 1), (stroke(True), 1), (stroke(False), 1),
        (stroke(True, 'happy'), 1), (stroke(False, 'happy'), 1),
        (stroke(True), 1), (stroke(False), 1),
        (sitting(arms='fan', leaf='a', eyes='shut', mouth='smile'), 2),
        (stroke(True), 1), (stroke(False), 1),
    )


def drink_in_frames():
    """Lifts its face to the rain."""
    return [sitting(head_dy=-1, eyes='half', mouth='closed')]


def drink_out_frames():
    """Lowers it again, smiling."""
    return [sitting(head_dy=-1, eyes='shut', mouth='smile')]


def drink_frames():
    """Lifts its face to the rain with the mouth open and catches a drop: the drop falls every
    1.4 s, in step with this loop (BackgroundEvents.DROP_FALL_FRAMES), then it gulps and smiles."""
    up = sitting(head_dy=-1, eyes='shut', mouth='o')
    return frames(
        (up, 8),
        (sitting(eyes='shut', mouth='chew'), 1),
        (sitting(eyes='happy', mouth='chew'), 1),
        (sitting(eyes='happy', mouth='smile'), 2),
        (sitting(head_dy=-1, eyes='shut', mouth='smile'), 1),
        (up, 1),
    )


def hug_reach():
    """Reaching up over the big pumpkin, before and after the hug."""
    return sitting(head_dx=-2, head_dy=1, arms='up', eyes='happy', mouth='smile', shadow=False)


def hug_in_frames():
    return [sitting(eyes='happy', mouth='smile'), hug_reach()]


def hug_out_frames():
    return [hug_reach(), sitting(eyes='happy', mouth='smile')]


def hug_frames():
    """Leans on the big pumpkin from behind (the background draws the pumpkin in front of it):
    arms over its shoulders, chin on top, swaying with a happy face while a heart floats up."""
    def heart(t):
        return [L(HEART if t < 3 else HEART_SMALL, 44 + t // 2, 14 - 2 * t)] if t < 6 else []
    sway = [0, 0, -1, -1, 0, 0, 1, 1, 0, 0, 0, 0]
    return [sitting(head_dx=-3 + sway[t], head_dy=2, arms='hug', arm_dy=1 if t in (3, 4, 9) else 0,
                    eyes='happy', mouth='smile', shadow=False, effects=heart(t)) for t in range(12)]


ANIMATIONS = {
    'sleep': {'interval': 140, 'frames': sleep_frames},
    'happy': {'interval': 80, 'frames': happy_frames},
    'idle': {'interval': 120, 'frames': idle_frames},
    'eat': {'interval': 110, 'frames': eat_frames},
    'roll': {'interval': 80, 'frames': roll_frames},
    'walk': {'interval': 160, 'frames': walk_frames},
    'turn': {'interval': 160, 'frames': turn_frames},
    'walk_start': {'interval': 160, 'frames': walk_start_frames},
    'walk_stop': {'interval': 160, 'frames': walk_stop_frames},
    'fan': {'interval': 110, 'frames': fan_frames},
    'drink': {'interval': 100, 'frames': drink_frames},
    'drink_in': {'interval': 100, 'frames': drink_in_frames},
    'drink_out': {'interval': 100, 'frames': drink_out_frames},
    'hug': {'interval': 110, 'frames': hug_frames},
    'hug_in': {'interval': 110, 'frames': hug_in_frames},
    'hug_out': {'interval': 110, 'frames': hug_out_frames},
}
# Played once inside other animations, never looped (check.py does not compare their seams)
ONE_SHOT = ('turn', 'walk_start', 'walk_stop', 'drink_in', 'drink_out', 'hug_in', 'hug_out')
# Poses whose soles the pumpkin hides (motion_check.py skips their feet)
OFF_GROUND = ('hug', 'hug_in', 'hug_out')
