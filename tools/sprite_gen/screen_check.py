"""How far a pixel pet's body moves on screen from one shown frame to the next, over whole lives.

    ./gradlew testDebugUnitTest --tests "*SmoothMotionTest*"     (writes app/build/lives/)
    python tools/sprite_gen/screen_check.py kitten [pet ...]

SmoothMotionTest checks where the frames are; this adds what is drawn in them: the body is the
largest connected group of opaque pixels but the ground shadow (stars, notes, Z and crumbs float
apart from it), its centre is taken in art pixels where the widget puts the frame (mirrored
frames mirrored). Prints the largest jumps with the drawables on both sides, so a pose that jumps
inside its frame shows up even when the frame itself moves smoothly. The body's centre also moves
when the pose changes (sitting down, a wingbeat); the parrot's eye, a fixed point on its head,
shows how far it really flies between two frames.

Goes through build/lives/<pet>.txt and every build/lives/more/<pet>_<size>_<seed>.txt.
"""
import glob
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import sprite_gen as sg  # noqa: E402

NODPI = os.path.join(sg.RES, 'drawable-nodpi')
SHADOW = (0x3b, 0x3d, 0x52)                 # the ground shadow every pixel pet stands on
# The parrot's eye: the white shine between the yellow iris and the black pupil
EYES = {'parrot': ((0xfb, 0xf6, 0xec), (0xf7, 0xc6, 0x3c), (0x1f, 0x21, 0x33))}
_bodies = {}
_eyes = {}


def body(drawable):
    """Centre (x, y) of the largest 8-connected group of opaque pixels, and the drawable's width."""
    if drawable not in _bodies:
        img = Image.open(os.path.join(NODPI, f'raw_{drawable}.png')).convert('RGBA')
        w, h = img.size
        px = img.load()
        solid = [[px[x, y][3] > 0 and px[x, y][:3] != SHADOW for y in range(h)] for x in range(w)]
        seen = set()
        best = []
        for y in range(h):
            for x in range(w):
                if not solid[x][y] or (x, y) in seen:
                    continue
                group, todo = [], [(x, y)]
                seen.add((x, y))
                while todo:
                    gx, gy = todo.pop()
                    group.append((gx, gy))
                    for nx in (gx - 1, gx, gx + 1):
                        for ny in (gy - 1, gy, gy + 1):
                            if 0 <= nx < w and 0 <= ny < h and (nx, ny) not in seen and solid[nx][ny]:
                                seen.add((nx, ny))
                                todo.append((nx, ny))
                if len(group) > len(best):
                    best = group
        cx = sum(p[0] for p in best) / len(best)
        cy = sum(p[1] for p in best) / len(best)
        _bodies[drawable] = (cx, cy, w)
    return _bodies[drawable]


def eye(pet, drawable):
    """Where the pet's eye is in the drawable and the drawable's width, or None (shut, hidden, or a
    pet without EYES)."""
    if pet not in EYES or '_turn_' in drawable:      # turning, it looks at you with both eyes
        return None
    if drawable not in _eyes:
        shine, iris, pupil = EYES[pet]
        img = Image.open(os.path.join(NODPI, f'raw_{drawable}.png')).convert('RGB')
        w, h = img.size
        px = img.load()
        found = [(x, y) for y in range(h) for x in range(1, w - 1)
                 if px[x, y] == shine and px[x - 1, y] == iris and px[x + 1, y] == pupil]
        _eyes[drawable] = (found[0], w) if len(found) == 1 else None
    return _eyes[drawable]


def jumps(pet, path):
    """(body jump, ms, from, to, frame moved, eye jump or None) for every change of picture."""
    lines = open(path, encoding='utf-8').read().split('\n')
    out = []
    last = None
    for line in lines[1:]:
        event = line.split()
        if not event or event[1] == 'end':
            break
        t, drawable, x, y, mirrored = int(event[0]), event[1], float(event[2]), float(event[3]), event[4] == '1'
        cx, cy, w = body(drawable)
        here = (x + (w - 1 - cx if mirrored else cx), y + cy)
        found = eye(pet, drawable)
        seen = None
        if found:
            (ex, ey), w = found
            seen = (x + (w - 1 - ex if mirrored else ex), y + ey)
        if last is not None:
            d = ((here[0] - last[1][0]) ** 2 + (here[1] - last[1][1]) ** 2) ** 0.5
            moved = ((x - last[2][0]) ** 2 + (y - last[2][1]) ** 2) ** 0.5
            by_eye = None
            if seen and last[3]:
                by_eye = ((seen[0] - last[3][0]) ** 2 + (seen[1] - last[3][1]) ** 2) ** 0.5
            out.append((d, t, last[0], drawable, moved, by_eye))
        last = (drawable, here, (x, y), seen)
    return out


def main(names, top=8):
    lives = os.path.join(sg.ROOT, 'app', 'build', 'lives')
    for name in names:
        paths = [os.path.join(lives, f'{name}.txt')] + sorted(glob.glob(os.path.join(lives, 'more', f'{name}_*.txt')))
        found = []
        for path in paths:
            found += [j + (os.path.basename(path),) for j in jumps(name, path)]
        found.sort(key=lambda j: j[0], reverse=True)
        print(f'== {name}: {len(paths)} lives, {len(found)} frame changes, body jump max {found[0][0]:.1f} px')
        for d, t, a, b, moved, _, life in found[:top]:
            print(f'  {d:5.1f} px  {a} -> {b}  (frame moved {moved:.1f}; {life} at {t / 1000:.2f} s)')
        by_eye = sorted((j for j in found if j[5] is not None), key=lambda j: j[5], reverse=True)
        if by_eye:
            print(f'  eye: max {by_eye[0][5]:.1f} px')
            for d, t, a, b, moved, e, life in by_eye[:top]:
                print(f'  {e:5.1f} px  {a} -> {b}  (frame moved {moved:.1f}; {life} at {t / 1000:.2f} s)')


if __name__ == '__main__':
    main(sys.argv[1:] or ['kitten'])
