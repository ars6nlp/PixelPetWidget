"""Builds tools/sprite_gen/kitten.py from kitten_template.py: big shaded shapes (head, body, the
lying loaf, the tails) are generated here once and written into the module as text matrices.

The stripes run with the form: a few even one-pixel bands across the back and the haunch, at
1:1 or 2:1 slopes only (no ragged diagonals); the forehead's "M", the cheeks, the whiskers and the
face are parts of the template, drawn on top of the plain shaded head.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from shapes import block, clean, ellipse_mask, shade  # noqa: E402

HERE = os.path.dirname(__file__)
OUT = os.path.join(HERE, '..', 'kitten.py')
FUR = ('G', 'L', 'g')        # base, highlight, shadow
CREAM = ('C', 'C', 'c')


def shaded(w, h, tones=FUR, **mask_args):
    m = ellipse_mask(w, h, **mask_args)
    return shade(m, *tones, shadow=((1, 2), (2, 1)))


def cream_below(rows, fraction):
    """Repaints the lower part of a shape in cream (the belly of the lying and walking kitten)."""
    out = [r[:] for r in rows]
    for y in range(int(len(rows) * fraction), len(rows)):
        for x, c in enumerate(rows[y]):
            if c in 'GL':
                out[y][x] = 'C'
            elif c == 'g':
                out[y][x] = 'c'
    return clean(out)


def tabby(rows, stripes):
    """Stripes across a shape: (x, from_y, to_y, step) bands one pixel wide that move one pixel
    sideways every `step` rows (0: straight down; 2: a 2:1 slope; negative: the other way),
    painted only over the fur. Ends that would stick out as single pixels are trimmed."""
    out = [r[:] for r in rows]
    h, w = len(rows), len(rows[0])
    for x0, y0, y1, step in stripes:
        for y in range(y0, min(y1, h)):
            x = x0 + ((y - y0) // abs(step) * (1 if step > 0 else -1) if step else 0)
            if 0 <= x < w and out[y][x] in 'GLg':
                out[y][x] = 's'
    return clean(out)


def tail(points, width=3, ring_every=4):
    """A thin tail along a polyline (root first): a mask of discs, shaded, with an even dark ring
    every few pixels along it and a darker tip. Returns (rows, root) with the root's position."""
    samples = []
    for (x0, y0), (x1, y1) in zip(points, points[1:]):
        n = max(1, int(math.hypot(x1 - x0, y1 - y0) * 2))
        samples += [(x0 + (x1 - x0) * i / n, y0 + (y1 - y0) * i / n) for i in range(n)]
    samples.append(points[-1])
    xs = [x for x, _ in samples]
    ys = [y for _, y in samples]
    left, top = int(min(xs)) - width, int(min(ys)) - width
    w, h = int(max(xs)) - left + width + 1, int(max(ys)) - top + width + 1
    mask = [[False] * w for _ in range(h)]
    along = [[None] * w for _ in range(h)]
    r = width / 2
    dist = 0.0
    for i, (sx, sy) in enumerate(samples):
        if i:
            dist += math.hypot(sx - samples[i - 1][0], sy - samples[i - 1][1])
        for y in range(h):
            for x in range(w):
                if (x + 0.5 + left - sx) ** 2 + (y + 0.5 + top - sy) ** 2 <= r * r:
                    mask[y][x] = True
                    if along[y][x] is None:
                        along[y][x] = dist
    rows = shade(mask, *FUR, shadow=((1, 1),))
    total = dist
    for y in range(h):
        for x in range(w):
            d = along[y][x]
            if d is None:
                continue
            if total - d < 2.5:
                rows[y][x] = 's'                     # the darker tip
            elif 3 < d < total - 4 and int(d) % ring_every == 0:
                rows[y][x] = 's'                     # a ring
    root = (int(points[0][0]) - left, int(points[0][1]) - top)
    return clean(rows), root


def main():
    template = open(os.path.join(HERE, 'kitten_template.py'), encoding='utf-8').read()
    # A plain round head: its markings (the "M", the cheeks) are parts drawn over it
    head = shaded(28, 23, top_power=2.4, bottom_power=2.2)
    # Three even bands over the back and down the side, following the curve of the body
    body = tabby(shaded(23, 21, top_power=1.9, bottom_power=3.0),
                 [(11, 1, 6, 2), (15, 2, 8, 2), (19, 4, 9, 2)])
    body_front = tabby(shaded(24, 21, top_power=1.9, bottom_power=3.0),
                       [(2, 6, 10, -2), (21, 6, 10, 2)])
    walk_body = tabby(cream_below(shaded(25, 12, power=2.4), 0.72),
                      [(8, 0, 5, 0), (12, 0, 6, 0), (16, 0, 6, 0), (20, 0, 4, 2)])
    loaf = tabby(cream_below(shaded(30, 14, bottom_power=3.4), 0.8),
                 [(10, 0, 5, 0), (14, 0, 6, 0), (18, 0, 6, 0), (22, 0, 5, 2)])
    haunch = shaded(12, 11, bottom_power=2.6)
    values = {
        'HEAD': block(head),
        'BODY': block(body),
        'BODY_FRONT': block(body_front),
        'HAUNCH': block(haunch),
        'CHEST': block(shaded(9, 12, CREAM)),
        'MUZZLE': block(shaded(10, 6, CREAM)),
        'WALK_BODY': block(walk_body),
        'LOAF': block(loaf),
        'SHADOW_SIT': block([['.'] * 3 + ['S'] * 26 + ['.'] * 3, ['S'] * 32, ['.'] * 3 + ['S'] * 26 + ['.'] * 3]),
        'SHADOW_SMALL': block([['.'] * 3 + ['S'] * 16 + ['.'] * 3, ['S'] * 22, ['.'] * 3 + ['S'] * 16 + ['.'] * 3]),
        'SHADOW_LONG': block([['.'] * 3 + ['S'] * 34 + ['.'] * 3, ['S'] * 40, ['.'] * 3 + ['S'] * 34 + ['.'] * 3]),
    }
    # Tails: (name, polyline from the root). Sitting: up the back, the tip curling over; its sway
    # bends it a pixel either way. Walking: up like a question mark, three steps of a sway. Lying:
    # curled up behind. High: straight up (crouching). Curl: round the paws (sitting facing you).
    tails = {
        'TAIL_SIT_0': [(0, 18), (5, 14), (8, 8), (8, 2), (5, 0)],
        'TAIL_SIT_1': [(0, 18), (5, 14), (9, 8), (10, 2), (8, 0)],
        'TAIL_SIT_2': [(0, 18), (4, 14), (7, 8), (6, 2), (3, 0)],
        'TAIL_WALK_0': [(0, 16), (3, 10), (4, 4), (2, 0), (-1, 0)],
        'TAIL_WALK_1': [(0, 16), (4, 10), (5, 4), (4, 0), (1, -1)],
        'TAIL_WALK_2': [(0, 16), (4, 10), (6, 4), (5, 0), (2, -1)],
        'TAIL_REST': [(0, 9), (5, 8), (8, 5), (8, 1), (5, 0)],
        'TAIL_HIGH': [(0, 14), (2, 8), (2, 2), (0, 0)],
        'TAIL_CURL': [(0, 0), (4, 2), (7, 4), (6, 7), (2, 8)],
    }
    for name, points in tails.items():
        rows, root = tail(points)
        values[name] = block(rows)
        values[name + '_ROOT'] = repr(root)
    for key, value in values.items():
        template = template.replace('{' + key + '}', value)
    with open(OUT, 'w', encoding='utf-8', newline='\n') as f:
        f.write(template)
    print('wrote', os.path.normpath(OUT))


if __name__ == '__main__':
    main()
