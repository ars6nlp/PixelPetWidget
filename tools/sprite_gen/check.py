"""Formal checks of the generated pets.

    python tools/sprite_gen/check.py [pet ...]

For every animation:
  colours   at most 16 in the palette (or the module's MAX_COLOURS), alpha only 0 or 255
            (exported PNGs too)
  clipping  nothing on the top row or the left and right columns (the bottom row is the ground)
  pivot     the ground shadow keeps its centre in every frame (the pet does not drift)
  orphans   pixels whose colour none of their 8 neighbours shares, outline and inner lines aside
            (stray pixels, noise); details on purpose are declared by ORPHAN_OK in the pet module
  seam      the jump from the last frame back to the first is no bigger than the biggest jump
            between neighbouring frames (plus 10 %), so the loop has no hitch; animations the module
            lists in ONE_SHOT (the parrot's take-off and landing) play once and are not compared
A module may add checks of its own with check(anims) -> [problem, ...] (the aquarium checks
that its fish stay in the water and out of the decor).
Exits with an error when a check fails.
"""
import glob
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import sprite_gen as sg  # noqa: E402

SHADOW = 'S'


def diff(a, b):
    return sum(1 for ra, rb in zip(a, b) for ca, cb in zip(ra, rb) if ca != cb)


def orphans(grid, pet):
    """Stray pixels. Single pixels a pet declares as details (ORPHAN_OK: colour and a colour it
    must touch, like an eye shine on a pupil or toe beans on a paw) are fine."""
    allowed = {pet.OUTLINE} | set(pet.LINE_OF.values())
    details = getattr(pet, 'ORPHAN_OK', ())
    count = 0
    h, w = len(grid), len(grid[0])
    for y in range(h):
        for x in range(w):
            c = grid[y][x]
            if c == '.' or c in allowed:
                continue
            near = [grid[y + dy][x + dx] for dx in (-1, 0, 1) for dy in (-1, 0, 1)
                    if (dx or dy) and 0 <= x + dx < w and 0 <= y + dy < h]
            if c in near:
                continue
            if any(c == colour and touch in near for colour, touch in details):
                continue
            count += 1
    return count


def shadow_centre(grid):
    """Middle of the ground shadow on its lowest row (paws may stand on its upper rows)."""
    rows = [row for row in grid if SHADOW in row]
    if not rows:
        return None
    xs = [x for x, c in enumerate(rows[-1]) if c == SHADOW]
    return (min(xs) + max(xs)) / 2


def check_pet(name):
    problems = []
    pet, anims = sg.build_pet(name)
    if len(pet.PALETTE) > getattr(pet, 'MAX_COLOURS', 16):
        problems.append(f'{len(pet.PALETTE)} colours')
    centres = set()
    print(f'== {name}: {len(pet.PALETTE)} colours')
    for anim, (interval, grids) in anims.items():
        clipped = [i for i, g in enumerate(grids)
                   if any(c != '.' for c in g[0]) or any(r[0] != '.' or r[-1] != '.' for r in g)]
        orph = [orphans(g, pet) for g in grids]
        steps = [diff(grids[i], grids[i + 1]) for i in range(len(grids) - 1)]
        seam = diff(grids[-1], grids[0]) if len(grids) > 1 else 0
        biggest = max(steps) if steps else 0
        for g in grids:
            # A moved frame keeps the pivot of its stored pose (the widget moves pet and shadow together)
            c = shadow_centre(g.moved[0] if getattr(g, 'moved', None) else g)
            if c is not None:
                centres.add(c)
        unique = len({tuple(g) for g in grids})
        flags = []
        if clipped:
            flags.append(f'clipped {clipped}')
        if max(orph) > 3:
            flags.append(f'orphans up to {max(orph)}')
        if seam > biggest * 1.1 and anim not in getattr(pet, 'ONE_SHOT', ()):  # slack for busy scenes
            flags.append(f'seam {seam} > biggest step {biggest}')
        print(f'  {anim:7s} {len(grids):2d} x {interval:3d} ms = {len(grids) * interval:5d} ms, {unique:2d} unique, '
              f'steps {min(steps) if steps else 0}-{biggest}, seam {seam}, orphans {max(orph)}'
              + (f'   !! {"; ".join(flags)}' if flags else ''))
        problems += [f'{anim}: {f}' for f in flags]
    if len(centres) > 1:
        problems.append(f'shadow centre moves: {sorted(centres)}')
        print(f'  !! shadow centre moves: {sorted(centres)}')
    else:
        print(f'  pivot (shadow centre) {centres.pop() if centres else "none"} in every frame')
    if hasattr(pet, 'check'):
        extra = pet.check(anims)
        for p in extra:
            print(f'  !! {p}')
        problems += extra
    # Exported frames: alpha 0 or 255 only
    res = os.path.join(sg.RES, 'drawable-nodpi')
    for path in glob.glob(os.path.join(res, f'raw_{name}_*.png')):
        alpha = set(Image.open(path).convert('RGBA').getchannel('A').getdata())
        if not alpha <= {0, 255}:
            problems.append(f'{os.path.basename(path)}: alpha {sorted(alpha)[:5]}')
            break
    return problems


def main(names):
    failed = {}
    for name in names:
        problems = check_pet(name)
        if problems:
            failed[name] = problems
    if failed:
        raise SystemExit('Checks failed: ' + '; '.join(f'{n}: {", ".join(p)}' for n, p in failed.items()))
    print('All checks passed')


if __name__ == '__main__':
    main(sys.argv[1:] or list(sg.PETS))
