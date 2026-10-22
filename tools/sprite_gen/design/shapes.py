"""Design-time helper: shaded pixel shapes as text matrices (pasted into the pet modules).

Light from the top left. Tones follow the contour with an even width, like a pixel artist
would place them:
  shadow    pixels whose neighbour (sx, sy) down-right is outside the shape
  highlight a one-pixel line just inside the top-left edge, only on the lit side
"""


def ellipse_mask(w, h, power=2.0, bottom_power=None, top_power=None):
    cx, cy = w / 2, h / 2
    mask = []
    for y in range(h):
        row = []
        for x in range(w):
            dx = abs((x + 0.5 - cx) / (w / 2))
            dy = (y + 0.5 - cy) / (h / 2)
            p = bottom_power if (bottom_power and dy > 0) else top_power if (top_power and dy < 0) else power
            row.append(dx ** p + abs(dy) ** p <= 1.0)
        mask.append(row)
    return mask


def inside(mask, x, y):
    return 0 <= y < len(mask) and 0 <= x < len(mask[0]) and mask[y][x]


def shade(mask, base, light, dark, shadow=((1, 2),), light_reach=2, lit_fraction=0.55):
    """shadow: offsets; a pixel is dark if any of them falls outside the shape."""
    h, w = len(mask), len(mask[0])
    out = []
    for y in range(h):
        row = []
        for x in range(w):
            if not mask[y][x]:
                row.append('.')
                continue
            if any(not inside(mask, x + sx, y + sy) for sx, sy in shadow):
                row.append(dark)
            elif (not inside(mask, x - light_reach, y - light_reach) and inside(mask, x - 1, y - 1)
                  and x < w * lit_fraction and y < h * lit_fraction):
                row.append(light)
            else:
                row.append(base)
        out.append(row)
    return clean(out)


def clean(rows):
    """Recolour orphans (no 8-neighbour shares their colour) to their most common neighbour."""
    h, w = len(rows), len(rows[0])
    out = [r[:] for r in rows]
    for y in range(h):
        for x in range(w):
            c = rows[y][x]
            if c == '.':
                continue
            n8 = [rows[y + dy][x + dx] for dx in (-1, 0, 1) for dy in (-1, 0, 1)
                  if (dx or dy) and 0 <= x + dx < w and 0 <= y + dy < h and rows[y + dy][x + dx] != '.']
            if n8 and c not in n8:
                n4 = [rows[ny][nx] for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1))
                      if 0 <= nx < w and 0 <= ny < h and rows[ny][nx] != '.'] or n8
                out[y][x] = max(sorted(set(n4)), key=n4.count)  # ties: always the same colour
    return out


def from_text(text):
    rows = [r.strip() for r in text.strip('\n').splitlines() if r.strip()]
    return [list(r) for r in rows]


def mask_of(rows):
    return [[c != '.' for c in r] for r in rows]


def block(rows, indent='    '):
    return '\n'.join(indent + ''.join(r) for r in rows)
