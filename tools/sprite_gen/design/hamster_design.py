"""Builds tools/sprite_gen/hamster.py from hamster_template.py: big shaded shapes are generated
here once and written into the module as text matrices."""
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from shapes import block, clean, ellipse_mask, inside, shade  # noqa: E402

HERE = os.path.dirname(__file__)
OUT = os.path.join(HERE, '..', 'hamster.py')
FUR = ('O', 'y', 'o')       # base, highlight, shadow
CREAM = ('C', 'C', 'c')


def shaded(w, h, tones=FUR, **mask_args):
    m = ellipse_mask(w, h, **mask_args)
    return shade(m, *tones, shadow=((1, 2), (2, 1)))


def cream_below(rows, fraction):
    """Repaints the lower part of a shape in cream (cheeks: ginger on top, cream below)."""
    h = len(rows)
    out = [r[:] for r in rows]
    for y in range(int(h * fraction), h):
        for x, c in enumerate(rows[y]):
            if c in 'Oy':
                out[y][x] = 'C'
            elif c == 'o':
                out[y][x] = 'c'
    return clean(out)


def back_stripe(rows, start, end, depth=2):
    """A dark line along the back: `depth` pixels under the top contour, between two fractions of
    the width."""
    w = len(rows[0])
    out = [r[:] for r in rows]
    for x in range(int(w * start), int(w * end)):
        top = next((y for y in range(len(rows)) if rows[y][x] != '.'), None)
        if top is not None and top + depth < len(rows):
            out[top + depth][x] = 'x'
            if out[top + depth - 1][x] == 'y':
                out[top + depth - 1][x] = 'O'
    return out


def belly_side(rows, fraction):
    """Cream underside for the side views: rows below a fraction of the height."""
    return cream_below(rows, fraction)


def main():
    template = open(os.path.join(HERE, 'hamster_template.py'), encoding='utf-8').read()
    values = {
        'HEAD': block(shaded(28, 23, top_power=2.1, bottom_power=2.5)),
        'BODY': block(shaded(32, 23, bottom_power=2.9)),
        'BELLY': block(shaded(18, 16, CREAM)),
        'CHEST': block(shaded(12, 7, CREAM)),
        'MUZZLE': block(shaded(10, 7, CREAM)),
        'RUN_BODY': block(back_stripe(belly_side(shaded(38, 21, power=2.2), 0.72), 0.12, 0.9)),
        'RUN_HEAD': block(shaded(22, 19)),
        'LOAF': block(cream_below(shaded(40, 20, bottom_power=3.2), 0.85)),
        'SHADOW_LOAF': block([['.'] * 3 + ['S'] * 34 + ['.'] * 3, ['S'] * 40, ['.'] * 3 + ['S'] * 34 + ['.'] * 3]),
        'SHADOW_SIT': block([['.'] * 3 + ['S'] * 28 + ['.'] * 3, ['S'] * 34, ['.'] * 3 + ['S'] * 28 + ['.'] * 3]),
        'SHADOW_SMALL': block([['.'] * 3 + ['S'] * 18 + ['.'] * 3, ['S'] * 24, ['.'] * 3 + ['S'] * 18 + ['.'] * 3]),
        'SHADOW_LONG': block([['.'] * 3 + ['S'] * 36 + ['.'] * 3, ['S'] * 42, ['.'] * 3 + ['S'] * 36 + ['.'] * 3]),
    }
    for stage, (w, h) in enumerate(((9, 8), (11, 10), (12, 12), (13, 13))):
        values[f'CHEEK_{stage}'] = block(cream_below(shaded(w, h), 0.62))
    for key, value in values.items():
        template = template.replace('{' + key + '}', value)
    with open(OUT, 'w', encoding='utf-8', newline='\n') as f:
        f.write(template)
    print('wrote', os.path.normpath(OUT))


if __name__ == '__main__':
    main()
