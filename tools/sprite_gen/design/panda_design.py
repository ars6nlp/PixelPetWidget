"""Builds tools/sprite_gen/panda.py: shaded big shapes are generated here once and written into
the module as text matrices; everything else lives in panda_template.py."""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from shapes import block, clean, ellipse_mask, inside, shade  # noqa: E402

HERE = os.path.dirname(__file__)
OUT = os.path.join(HERE, '..', 'panda.py')


def head():
    """Round head, a little wider at the cheeks; fur tufts along the lower cheek curve."""
    w, h = 30, 24
    m = ellipse_mask(28, 23, top_power=2.0, bottom_power=2.35)
    mask = [[False] + r + [False] for r in m] + [[False] * w]
    # Tufts: one pixel out of the curve, low on each cheek, two per side, stepped
    for x, y in ((1, 17), (2, 19), (28, 17), (27, 19)):
        mask[y][x] = True
    for x, y in ((0, 17), (29, 17)):
        mask[y][x] = False
    return shade(mask, 'w', 'W', 'v', shadow=((1, 2), (2, 1)))


def body():
    m = ellipse_mask(28, 24, power=2.0, bottom_power=3.0)
    return shade(m, 'w', 'W', 'v', shadow=((1, 2), (2, 1)))


def side_body():
    m = ellipse_mask(34, 19, power=2.2, bottom_power=2.6)
    return shade(m, 'w', 'W', 'v', shadow=((1, 2), (2, 1)))


def shadow(width):
    rows = []
    for y, inset in ((0, 3), (1, 0), (2, 3)):
        rows.append(['.'] * inset + ['S'] * (width - 2 * inset) + ['.'] * inset)
    return rows


def ball(step, size=30):
    """Panda curled into a ball, turned by step * 45 degrees counter-clockwise (it rolls left).
    The ears stick out of the circle so the turn shows in the silhouette; light stays fixed."""
    pad = 3
    full = size + 2 * pad
    r = size / 2
    c = full / 2
    angle = -step * math.pi / 4

    def turn(ux, uy):                       # screen -> ball coordinates
        return (ux * math.cos(-angle) - uy * math.sin(-angle), ux * math.sin(-angle) + uy * math.cos(-angle))

    ears = [(-0.55, -1.02), (0.55, -1.02)]
    patches = [(-0.36, -0.22), (0.36, -0.22)]
    body = [[False] * full for _ in range(full)]
    rows = []
    for y in range(full):
        row = []
        for x in range(full):
            ux, uy = (x + 0.5 - c) / r, (y + 0.5 - c) / r
            bx, by = turn(ux, uy)
            in_ball = math.hypot(ux, uy) <= 1.0
            in_ear = any(math.hypot(bx - ex, by - ey) <= 0.24 for ex, ey in ears)
            if not (in_ball or in_ear):
                row.append('.')
                continue
            body[y][x] = True
            colour = 'w'
            if in_ear and not in_ball:
                colour = 'D'
            elif by > 0.18 and math.hypot(bx, by) > 0.3:        # arms and legs hugging the belly
                colour = 'D'
            elif any(math.hypot(bx - px, (by - py) * 1.25) <= 0.2 for px, py in patches):
                colour = 'D'
            elif math.hypot(bx, by - 0.02) <= 0.09:             # nose
                colour = 'K'
            row.append(colour)
        rows.append(row)
    rows = clean(rows)
    out = []
    for y in range(full):
        line = []
        for x in range(full):
            ch = rows[y][x]
            if ch == '.' or ch == 'K':
                line.append(ch)
                continue
            dark = not inside(body, x + 1, y + 2) or not inside(body, x + 2, y + 1)
            lit = (not inside(body, x - 2, y - 2)) and inside(body, x - 1, y - 1) and x < full * 0.55 and y < full * 0.55
            if ch == 'w':
                line.append('v' if dark else 'W' if lit else 'w')
            else:
                line.append('d' if dark else 'h' if lit else 'D')
        out.append(line)
    return clean(out)


def main():
    template = open(os.path.join(HERE, 'panda_template.py'), encoding='utf-8').read()
    values = {
        'HEAD': block(head()),
        'BODY': block(body()),
        'SIDE_BODY': block(side_body()),
        'SHADOW_WIDE': block(shadow(34)),
        'SHADOW_MID': block(shadow(28)),
        'SHADOW_SMALL': block(shadow(22)),
        'SHADOW_WALK': block(shadow(40)),
    }
    for step in range(8):
        values[f'BALL_{step}'] = block(ball(step))
    for key, value in values.items():
        template = template.replace('{' + key + '}', value)
    with open(OUT, 'w', encoding='utf-8', newline='\n') as f:
        f.write(template)
    print('wrote', os.path.normpath(OUT))


if __name__ == '__main__':
    main()
