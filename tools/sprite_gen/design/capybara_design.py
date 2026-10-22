"""Builds tools/sprite_gen/capybara.py from capybara_template.py: big shaded shapes are generated
here once and written into the module as text matrices."""
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from shapes import block, clean, ellipse_mask, shade  # noqa: E402

HERE = os.path.dirname(__file__)
OUT = os.path.join(HERE, '..', 'capybara.py')
FUR = ('B', 'h', 'b')        # base, highlight, shadow
WATER = ('A', 'q', 'a')
ORANGE = ('O', 'c', 'o')


def shaded(mask, tones=FUR):
    return shade(mask, *tones, shadow=((1, 2), (2, 1)))


def pad_top(mask, rows):
    return [[False] * len(mask[0]) for _ in range(rows)] + mask


def tufts(mask, columns):
    """Fur combed back along the top contour: a low bump with a tip pointing to the rump."""
    for x in columns:
        top = next(y for y in range(len(mask)) if mask[y][x])
        for dx in range(5):
            mask[top - 1][x + dx] = True
        mask[top - 2][x + 4] = True
    return mask


def belly(rows, depth, start, end):
    """Lighter fur on the underside: a flat half-ellipse `depth` of the height deep, between two
    fractions of the width. Its rim keeps the darker tone."""
    h, w = len(rows), len(rows[0])
    cx, rx = w * (start + end) / 2, w * (end - start) / 2
    ry = h * depth
    out = [r[:] for r in rows]
    for y in range(h):
        for x in range(w):
            if ((x + 0.5 - cx) / rx) ** 2 + ((y + 0.5 - h) / ry) ** 2 > 1:
                continue
            if rows[y][x] in 'Bh':
                out[y][x] = 'c'
            elif rows[y][x] == 'b':
                out[y][x] = 'B'
    return clean(out)


def body():
    m = pad_top(ellipse_mask(40, 22, power=2.8, bottom_power=3.2), 2)
    m = tufts(m, (12, 27))
    return belly(shaded(m), 0.3, 0.1, 0.8)


def lying_body():
    m = pad_top(ellipse_mask(42, 17, power=2.6, bottom_power=4.0), 2)
    m = tufts(m, (14, 29))
    return belly(shaded(m), 0.22, 0.12, 0.8)


def head():
    """A blunt brick: flat brow sloping a little to a deep square muzzle."""
    w, h = 24, 19
    m = ellipse_mask(w, h, power=3.2)
    for y in range(3):                      # the brow slopes down towards the nose
        for x in range(max(0, 7 - 3 * y)):
            m[y][x] = False
    return shaded(m)


def front_head():
    """The head seen from the front: a blunt brick, the dark square nose across its lower half."""
    w, h = 18, 17
    rows = shaded(ellipse_mask(w, h, power=3.0))
    for y in range(h):
        for x in range(w):
            if rows[y][x] != '.' and 12 <= y <= 14 and abs(x + 0.5 - w / 2) <= (3.5 if y < 14 else 2.5):
                rows[y][x] = 'd'
    return clean(rows)


def front_body():
    """The body seen from the front, behind the head: round, the lighter belly low in the middle."""
    return belly(shaded(ellipse_mask(30, 21, power=2.8, bottom_power=3.2)), 0.35, 0.25, 0.75)


def jaw():
    return shade(ellipse_mask(11, 5, power=2.6), 'B', 'B', 'b', shadow=((1, 2), (2, 1)))


def orange():
    m = ellipse_mask(8, 7, power=2.0)
    rows = shaded(m, ORANGE)
    rows[1][2] = 'W'                        # a shine on the lit side
    leaf = [list('...gGG..'), list('...g....')]
    return leaf + rows


def pool():
    rows = shade(ellipse_mask(58, 14, power=2.4), *WATER, shadow=((1, 2), (2, 1)), lit_fraction=0.5)
    # Light streaks on the surface
    for x, y, n in ((9, 4, 4), (40, 9, 3), (16, 10, 2)):
        for i in range(n):
            rows[y][x + i] = 'W'
    return rows


def grass(length):
    """A grass blade hanging from the mouth, `length` pixels of it still outside: it droops forward
    at 2:1, light on top and dark below."""
    path = [(-i, i // 2) for i in range(10)]
    pts = path[10 - length:] if length else []
    if not pts:
        return [['.']]
    xs = [x for x, _ in pts]
    ys = [y for _, y in pts]
    x0, y0 = min(xs), min(ys)
    w, h = max(xs) - x0 + 1, max(ys) - y0 + 2
    rows = [['.'] * w for _ in range(h)]
    for x, y in pts:
        rows[y - y0][x - x0] = 'G'
        rows[y - y0 + 1][x - x0] = 'g'
    tip_x, tip_y = pts[-1]
    rows[tip_y - y0 + 1][tip_x - x0] = '.'   # the tip narrows to one pixel
    return rows


def shadow(width):
    return [['.'] * 3 + ['S'] * (width - 6) + ['.'] * 3, ['S'] * width, ['.'] * 3 + ['S'] * (width - 6) + ['.'] * 3]


def main():
    template = open(os.path.join(HERE, 'capybara_template.py'), encoding='utf-8').read()
    values = {
        'BODY': block(body()),
        'LYING_BODY': block(lying_body()),
        'HEAD': block(head()),
        'FRONT_HEAD': block(front_head()),
        'BODY_FRONT': block(front_body()),
        'JAW': block(jaw()),
        'ORANGE': block(orange()),
        'POOL': block(pool()),
        'SHADOW_STAND': block(shadow(44)),
        'SHADOW_LYING': block(shadow(48)),
        'SHADOW_POOL': block(shadow(56)),
    }
    for n in range(1, 11):
        values[f'GRASS_{n}'] = block(grass(n))
    for key, value in values.items():
        template = template.replace('{' + key + '}', value)
    assert '{' not in ''.join(line for line in template.splitlines() if line.startswith('    ') and
                              set(line.strip()) <= set('{}ABCDEFGHIJKLMNOPQRSTUVWXYZ_0123456789'))
    with open(OUT, 'w', encoding='utf-8', newline='\n') as f:
        f.write(template)
    print('wrote', os.path.normpath(OUT))


if __name__ == '__main__':
    main()
