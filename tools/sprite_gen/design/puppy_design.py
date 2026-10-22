"""Builds tools/sprite_gen/puppy.py from puppy_template.py: big shaded shapes (head, body, the long
corgi body for walking and lying, the one lying on its back, the bodies seen from the front, from
three quarters and from behind) are generated here once and written into the module as text
matrices."""
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from shapes import block, clean, ellipse_mask, shade  # noqa: E402

HERE = os.path.dirname(__file__)
OUT = os.path.join(HERE, '..', 'puppy.py')
FUR = ('O', 'y', 'o')        # base, highlight, shadow
WHITE = ('W', 'W', 'w')


def shaded(w, h, tones=FUR, **mask_args):
    m = ellipse_mask(w, h, **mask_args)
    return shade(m, *tones, shadow=((1, 2), (2, 1)))


def white_below(rows, fraction):
    """Repaints the lower part of a shape white (the corgi's white belly)."""
    out = [r[:] for r in rows]
    for y in range(int(len(rows) * fraction), len(rows)):
        for x, c in enumerate(rows[y]):
            if c in 'Oy':
                out[y][x] = 'W'
            elif c == 'o':
                out[y][x] = 'w'
    return clean(out)


def white_above(rows, fraction):
    """Repaints the upper part of a shape white (the belly of a corgi lying on its back)."""
    out = [r[:] for r in rows]
    for y in range(int(len(rows) * fraction)):
        for x, c in enumerate(rows[y]):
            if c in 'Oy':
                out[y][x] = 'W'
            elif c == 'o':
                out[y][x] = 'w'
    return clean(out)


def white_middle(rows, width):
    """Repaints a band down the middle white (the chest seen from the front)."""
    out = [r[:] for r in rows]
    w = len(rows[0])
    for y in range(len(rows)):
        half = width / 2 * min(1.0, 0.45 + y / len(rows))
        for x, c in enumerate(rows[y]):
            if abs(x + 0.5 - w / 2) <= half:
                if c in 'Oy':
                    out[y][x] = 'W'
                elif c == 'o':
                    out[y][x] = 'w'
    return clean(out)


def blaze():
    """The white blaze: a narrow stripe down the forehead that widens into the muzzle."""
    widths = [2, 2, 2, 2, 2, 4, 4, 6, 6, 6]
    w = max(widths)
    mask = [[abs(x + 0.5 - w / 2) <= widths[y] / 2 for x in range(w)] for y in range(len(widths))]
    return shade(mask, *WHITE, shadow=((1, 1),))


def side_head():
    """The head in profile, snout to the left: a round skull and a snout below and in front of it,
    the snout and cheek white."""
    w, h = 24, 19
    skull = ellipse_mask(19, 17, top_power=2.2)
    snout = ellipse_mask(11, 8, power=2.3)
    mask = [[False] * w for _ in range(h)]
    for y in range(17):
        for x in range(19):
            if skull[y][x]:
                mask[y + 1][x + 5] = True
    for y in range(8):
        for x in range(11):
            if snout[y][x]:
                mask[y + 9][x] = True
    rows = shade(mask, *FUR, shadow=((1, 2), (2, 1)))
    for y in range(h):
        for x in range(w):
            if rows[y][x] != '.' and ((y >= 13 and x < 14) or (y >= 15 and x < 18)):
                rows[y][x] = 'w' if rows[y][x] == 'o' else 'W'
    return clean(rows)


def main():
    template = open(os.path.join(HERE, 'puppy_template.py'), encoding='utf-8').read()
    values = {
        'HEAD': block(shaded(26, 20, top_power=2.2, bottom_power=2.0)),
        'BODY': block(shaded(24, 19, top_power=1.9, bottom_power=2.8)),
        'BODY_FRONT': block(white_middle(shaded(24, 18, top_power=1.9, bottom_power=2.6), 12)),
        'BODY_34': block(white_below(shaded(23, 13, power=2.5), 0.7)),
        'BODY_BACK': block(white_below(shaded(24, 17, top_power=1.9, bottom_power=2.4), 0.62)),
        'CHEST': block(shaded(12, 14, WHITE)),
        'MUZZLE': block(shaded(14, 8, WHITE)),
        'BLAZE': block(blaze()),
        'HAUNCH': block(shaded(12, 10, bottom_power=2.6)),
        'HEAD_SIDE': block(side_head()),
        'WALK_BODY': block(white_below(shaded(30, 13, power=2.5), 0.7)),
        'LOAF': block(white_below(shaded(32, 13, bottom_power=3.4), 0.78)),
        'BACK_BODY': block(white_above(shaded(30, 13, power=2.4), 0.45)),
        'SHADOW_SIT': block([['.'] * 3 + ['S'] * 26 + ['.'] * 3, ['S'] * 32, ['.'] * 3 + ['S'] * 26 + ['.'] * 3]),
        'SHADOW_SMALL': block([['.'] * 3 + ['S'] * 16 + ['.'] * 3, ['S'] * 22, ['.'] * 3 + ['S'] * 16 + ['.'] * 3]),
        'SHADOW_LONG': block([['.'] * 3 + ['S'] * 36 + ['.'] * 3, ['S'] * 42, ['.'] * 3 + ['S'] * 36 + ['.'] * 3]),
    }
    for key, value in values.items():
        template = template.replace('{' + key + '}', value)
    with open(OUT, 'w', encoding='utf-8', newline='\n') as f:
        f.write(template)
    print('wrote', os.path.normpath(OUT))


if __name__ == '__main__':
    main()
