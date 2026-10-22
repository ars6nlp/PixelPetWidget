"""Builds tools/sprite_gen/parrot.py from parrot_template.py: big shaded shapes (head, body, wings,
tail feathers) are generated here once and written into the module as text matrices."""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from shapes import block, clean, ellipse_mask, shade  # noqa: E402

HERE = os.path.dirname(__file__)
OUT = os.path.join(HERE, '..', 'parrot.py')
RED = ('R', 'q', 'r')        # base, highlight, shadow
YELLOW = ('Y', 'Y', 'o')
BLUE = ('U', 'p', 'u')


def shaded(mask, tones=RED):
    return shade(mask, *tones, shadow=((1, 2), (2, 1)))


def shear(mask, rate):
    """Leans a shape forward (to the left at the top): lower rows move right."""
    h, w = len(mask), len(mask[0])
    extra = math.ceil((h - 1) * rate)
    out = [[False] * (w + extra) for _ in range(h)]
    for y in range(h):
        s = round(y * rate)
        for x in range(w):
            out[y][x + s] = mask[y][x]
    return out


def chest_scales(rows, x_frac, y0, y1):
    """One repeating motif on the chest: small "v" feather tips in a brick pattern, only on the
    plain red in front (the first x_frac of every row's span)."""
    h = len(rows)
    out = [r[:] for r in rows]
    for y in range(int(h * y0), int(h * y1)):
        span = [x for x, c in enumerate(rows[y]) if c != '.']
        if not span:
            continue
        left, right = span[0], span[-1]
        limit = left + (right - left) * x_frac
        off = 2 if (y // 3) % 2 else 0
        for x in range(left + 2, int(limit)):
            xx, yy = (x + off) % 4, y % 3
            if rows[y][x] == 'R' and ((yy == 0 and xx in (0, 3)) or (yy == 1 and xx in (1, 2))):
                out[y][x] = 'r'
    return out


def no_light_on_top(rows, depth):
    """The top of the body is under the head: no highlight there to leave stray pixels."""
    return [['R' if c == 'q' and y < depth else c for c in row] for y, row in enumerate(rows)]


def body():
    m = shear(ellipse_mask(17, 27, top_power=2.2, bottom_power=2.0), 0.25)
    return no_light_on_top(chest_scales(shaded(m), 0.55, 0.3, 0.64), 5)


def fluffed_body():
    """The body with its feathers puffed up: one pixel bigger, with a ragged rim."""
    m = shear(ellipse_mask(19, 28, top_power=2.2, bottom_power=2.0), 0.25)
    h, w = len(m), len(m[0])
    rim = set()
    for y in range(h):
        for x in range(w):
            if not m[y][x]:
                continue
            for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1)):
                if 0 <= nx < w and 0 <= ny < h and not m[ny][nx] and (nx + 3 * ny) % 4 == 0:
                    rim.add((nx, ny))
    for x, y in rim:
        m[y][x] = True
    return no_light_on_top(chest_scales(shaded(m), 0.55, 0.3, 0.64), 5)


def front_body():
    """The body seen from the front (turning round): an upright oval, the chest scales in the
    middle."""
    m = ellipse_mask(20, 26, top_power=2.2, bottom_power=2.0)
    return no_light_on_top(chest_scales(shaded(m), 0.85, 0.3, 0.64), 4)


def feathered(length, width, angle, bands, profile, split=None, tips=None):
    """A long shape grown from a root point along `angle` (degrees, 0 = right, 90 = down).
    bands: [(t_end, tones)] colour bands from the root (t = 0) to the tip (t = 1), their borders
    scalloped like rows of covert feathers. split: t from which dark lines part the flight
    feathers; tips: how much shorter the outer feathers are than the middle one.
    Returns the rows and the root's position in them."""
    a = math.radians(angle)
    ux, uy = math.cos(a), math.sin(a)
    vx, vy = -uy, ux
    reach = int(length + width) + 2
    pts = {}
    for py in range(-reach, reach + 1):
        for px in range(-reach, reach + 1):
            cx, cy = px + 0.5, py + 0.5
            u, v = cx * ux + cy * uy, cx * vx + cy * vy
            t = u / length
            if not 0 <= t <= 1:
                continue
            vn = v / (width / 2)
            if abs(vn) > profile(t):
                continue
            if tips and abs(vn) > 0.33 * profile(t) and t > 1 - (tips[0] if vn < 0 else tips[1]):
                continue
            pts[(px, py)] = (t, vn / max(profile(t), 0.01))
    xs = [p[0] for p in pts]
    ys = [p[1] for p in pts]
    x0, y0 = min(xs), min(ys)
    w, h = max(xs) - x0 + 1, max(ys) - y0 + 1
    mask = [[(x + x0, y + y0) in pts for x in range(w)] for y in range(h)]
    tones = shade(mask, '1', '2', '3', shadow=((1, 2), (2, 1)))
    rows = [['.'] * w for _ in range(h)]
    for (px, py), (t, vn) in pts.items():
        x, y = px - x0, py - y0
        tone = tones[y][x]
        if tone == '.':
            continue
        band = bands[-1][1]
        for t_end, band_tones in bands:
            if t < t_end + 0.03 * math.cos(vn * math.pi * 2.5):
                band = band_tones
                break
        rows[y][x] = band['123'.index(tone)]
        if split is not None and t > split and abs(abs(vn) - 0.36) < 0.13 and rows[y][x] != band[2]:
            rows[y][x] = band[2]
    return clean(rows), (-x0, -y0)


def wing_profile(t):
    return min(1.0, t / 0.15) ** 0.5 * max(0.0, 1 - t ** 3) ** 0.5


def feather_profile(t):
    return min(1.0, 0.45 + t / 0.12) * (max(0.0, 1 - ((t - 0.7) / 0.3) ** 2) ** 0.5 if t > 0.7 else 1.0)


def wing(angle):
    bands = [(0.3, RED), (0.46, YELLOW), (1.0, BLUE)]
    return feathered(26, 12, angle, bands, wing_profile, split=0.55, tips=(0.1, 0.05))


def front_wing():
    """A folded wing seen from the front, hanging along the side of the body."""
    bands = [(0.3, RED), (0.46, YELLOW), (1.0, BLUE)]
    return feathered(20, 7, 94, bands, wing_profile, split=0.55, tips=(0.1, 0.05))


def tail_feather(length, width, angle, tones_root, tip_from, light=True):
    if not light:
        tones_root = (tones_root[0], tones_root[0], tones_root[2])
    bands = [(tip_from, tones_root), (1.0, BLUE if light else ('U', 'U', 'u'))]
    return feathered(length, width, angle, bands, feather_profile)


# --- flight -------------------------------------------------------------------------------

# Body tilts (degrees the head end rises), wing angles and the tail angle the flight poses use
FLIGHT_TILTS = (28, 38, 50)
FLIGHT_WINGS = (-70, -50, -30, -20, 15, 40, 50, 70)
FLIGHT_TAIL = 16


def tilted_ellipse(length, width, deg, power=2.1):
    """An ellipse whose long axis rises to the left by deg degrees (towards the head).
    Returns the mask and the centre's position in it."""
    a = math.radians(deg)
    ux, uy = -math.cos(a), -math.sin(a)
    vx, vy = -uy, ux
    r = int(length / 2 + 2)
    pts = set()
    for y in range(-r, r + 1):
        for x in range(-r, r + 1):
            cx, cy = x + 0.5, y + 0.5
            u, v = cx * ux + cy * uy, cx * vx + cy * vy
            if abs(u / (length / 2)) ** power + abs(v / (width / 2)) ** power <= 1:
                pts.add((x, y))
    xs = [p[0] for p in pts]
    ys = [p[1] for p in pts]
    x0, y0 = min(xs), min(ys)
    mask = [[(x + x0, y + y0) in pts for x in range(max(xs) - x0 + 1)] for y in range(max(ys) - y0 + 1)]
    return mask, (-x0, -y0)


def belly_scales(rows, centre, deg):
    """The chest's v-shaped feather tips on the lower front of a tilted body, in the same brick
    pattern as on the standing body."""
    a = math.radians(deg)
    ux, uy = -math.cos(a), -math.sin(a)
    vx, vy = -uy, ux
    out = [r[:] for r in rows]
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            if c != 'R':
                continue
            dx, dy = x + 0.5 - centre[0], y + 0.5 - centre[1]
            u, v = dx * ux + dy * uy, dx * vx + dy * vy
            if u > -2 and v < -2:
                off = 2 if (y // 3) % 2 else 0
                xx, yy = (x + off) % 4, y % 3
                if (yy == 0 and xx in (0, 3)) or (yy == 1 and xx in (1, 2)):
                    out[y][x] = 'r'
    return out


def flight_body(deg):
    """The body stretched out in flight: slimmer than the standing one, tilted by deg."""
    mask, centre = tilted_ellipse(23, 15, deg)
    rows = shade(mask, 'R', 'q', 'r', shadow=((1, 2), (2, 1)))
    return clean(belly_scales(rows, centre, deg)), centre


def flight_wing(angle):
    """A spread wing, a little shorter than the folded one so it fits the frame when raised."""
    bands = [(0.3, RED), (0.46, YELLOW), (1.0, BLUE)]
    return feathered(22, 11, angle, bands, wing_profile, split=0.55, tips=(0.1, 0.05))


def flight_tail():
    """The tail streaming behind: the long red-and-blue middle feather over two shorter ones."""
    return [tail_feather(21, 4, FLIGHT_TAIL - 7, BLUE, 0.0, False),
            tail_feather(17, 4, FLIGHT_TAIL + 9, RED, 0.6, False),
            tail_feather(26, 5, FLIGHT_TAIL, RED, 0.72)]


def entry(rows, root, indent='        '):
    return "(part('''\n" + block(rows, indent) + "\n'''), " + repr(root) + ')'


def flight_shapes():
    """Python source for the flight shapes, pasted into the template."""
    lines = ['# Body tilted in flight: tilt -> (rows, centre)', 'BODY_FLY = {']
    for deg in FLIGHT_TILTS:
        rows, centre = flight_body(deg)
        lines.append(f'    {deg}: ' + entry(rows, centre) + ',')
    lines += ['}', '# Spread wings: angle (degrees, 0 = back, 90 = down) -> (rows, root)', 'WING_FLY = {']
    for angle in FLIGHT_WINGS:
        rows, root = flight_wing(angle)
        lines.append(f'    {angle}: ' + entry(rows, root) + ',')
    lines += ['}', '# The tail in flight, drawn from back to front: (rows, root)', 'TAIL_FLY = [']
    for rows, root in flight_tail():
        lines.append('    ' + entry(rows, root) + ',')
    lines.append(']')
    return '\n'.join(lines)


def shadow(width):
    return [['.'] * 3 + ['S'] * (width - 6) + ['.'] * 3, ['S'] * width, ['.'] * 3 + ['S'] * (width - 6) + ['.'] * 3]


def main():
    template = open(os.path.join(HERE, 'parrot_template.py'), encoding='utf-8').read()
    values = {
        'HEAD': block(shaded(ellipse_mask(18, 16, bottom_power=2.3))),
        'BODY': block(body()),
        'BODY_FLUFF': block(fluffed_body()),
        'BODY_FRONT': block(front_body()),
        'SHADOW_STAND': block(shadow(36)),
        'SHADOW_SMALL': block(shadow(28)),
        'FLIGHT_SHAPES': flight_shapes(),
    }
    for name, angle in (('WING_FOLD', 78), ('WING_OUT', 20), ('WING_UP', -55)):
        rows, root = wing(angle)
        values[name] = block(rows)
        values[name + '_ROOT'] = repr(root)
    for name, args in (('TAIL_MAIN', (25, 5, 20, RED, 0.72)), ('TAIL_LOW', (21, 5, 11, BLUE, 0.0, False)),
                       ('TAIL_HIGH', (18, 4, 30, RED, 0.6, False))):
        rows, root = tail_feather(*args)
        values[name] = block(rows)
        values[name + '_ROOT'] = repr(root)
    rows, root = front_wing()
    values['WING_FRONT'] = block(rows)
    values['WING_FRONT_ROOT'] = repr(root)
    rows, root = tail_feather(12, 5, 90, RED, 0.5)
    values['TAIL_FRONT'] = block(rows)
    values['TAIL_FRONT_ROOT'] = repr(root)
    for key, value in values.items():
        template = template.replace('{' + key + '}', value)
    # Pixels set by hand in the flight shapes (the flying body's edge), kept when the module is
    # rebuilt
    for drawn, by_hand in (('        qqRRRRRRRRRRRRRRRRRr..', '        rqRRRRRRRRRRRRRRRRRr..'),):
        assert template.count(drawn) == 1
        template = template.replace(drawn, by_hand)
    with open(OUT, 'w', encoding='utf-8', newline='\n') as f:
        f.write(template)
    print('wrote', os.path.normpath(OUT))


if __name__ == '__main__':
    main()
