"""Checks how smoothly a pixel pet moves, frame by frame (after check.py's formal checks).

    python tools/sprite_gen/motion_check.py kitten [pet ...]

For every animation of the pet:
  feet      the outline under the paws is on row 60 in the first and last frame of every loop and
            transition, and in every walk frame (poses on props, ONE_SHOT jumps and the
            interactions listed in OFF_GROUND are exempt)
  pivot     the ground shadow keeps its centre
  jumps     how far the body's centre moves between neighbouring frames, and the loop seam
  joins     every loop and transition starts and ends close to the neutral pose (the first idle
            frame), so one activity follows another without a jump
  walk      a paw that stays on the ground from one walk frame to the next moves back by exactly
            the module's WALK_SHIFTS for that frame (= PetSpec's gait), so the paws never slide on
            the widget; and every frame has such a paw (no frame where all of them slide)
Exits with an error when a hard rule fails (feet, pivot, walk, alpha, colours, clipping).
Writes output/<pet>_walk_on_ground.png: two cycles of the walk, each frame where the widget puts it
(moved by the shifts), one under the other, with a mark under every paw on the ground: a paw that
stays down keeps its mark in the same column.
"""
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import sprite_gen as sg  # noqa: E402

FOOT_ROW = 60
SHADOW = 'S'


def body(grid):
    """Pixels of the pet (no shadow, no effects outside the figure)."""
    return [(x, y) for y, row in enumerate(grid) for x, c in enumerate(row) if c not in ('.', SHADOW)]


def centre(grid):
    pts = body(grid)
    if not pts:
        return 0.0, 0.0
    return sum(p[0] for p in pts) / len(pts), sum(p[1] for p in pts) / len(pts)


def foot_row(grid, outline):
    """Lowest row with the outline colour under the figure (above the shadow rows it may cross)."""
    rows = [y for y, row in enumerate(grid) for c in row if c == outline]
    return max(rows) if rows else None


def diff(a, b):
    return sum(1 for ra, rb in zip(a, b) for ca, cb in zip(ra, rb) if ca != cb)


def paws_on_ground(grid, outline, row=FOOT_ROW - 1):
    """Runs of the figure on the last row above the outline: the paws standing on the ground,
    as (start, end) columns."""
    runs, start = [], None
    line = grid[row]
    for x, c in enumerate(line + '.'):
        filled = c not in ('.', SHADOW, outline)
        if filled and start is None:
            start = x
        elif not filled and start is not None:
            runs.append((start, x - 1))
            start = None
    return runs


def check_walk(name, pet, grids):
    """The planted paws move back by WALK_SHIFTS[f] from frame f-1 to frame f."""
    shifts = getattr(pet, 'WALK_SHIFTS', None)
    row = getattr(pet, 'FOOT_ROW', FOOT_ROW) - 1
    problems = []
    if shifts is None:
        return ['no WALK_SHIFTS']
    if len(shifts) != len(grids):
        return [f'{len(shifts)} shifts for {len(grids)} walk frames']
    n = len(grids)
    for f in range(n):
        prev, now = paws_on_ground(grids[f - 1], pet.OUTLINE, row), paws_on_ground(grids[f], pet.OUTLINE, row)
        expected = shifts[f]
        planted = [(a, b) for a in prev for b in now if b[0] - a[0] == expected and b[1] - a[1] == expected]
        sliding = [(a, b) for a in prev for b in now
                   if b[1] - b[0] == a[1] - a[0] and 0 < abs(b[0] - a[0] - expected) <= 1]
        if not planted:
            problems.append(f'walk {(f - 1) % n}->{f}: no paw stays on the ground (expected a move of {expected})')
        print(f'    walk {(f - 1) % n}->{f}: shift {expected}, paws before {prev}, after {now}, '
              f'planted {len(planted)}{", near misses " + str(len(sliding)) if sliding else ""}')
    return problems


def walk_on_ground(name, pet, grids, scale=3):
    """Two walk cycles as the widget moves them (facing left, so forward is to the left)."""
    shifts = pet.WALK_SHIFTS
    n = len(grids)
    travel = sum(shifts) * 2
    width = (64 + travel) * scale
    sheet = Image.new('RGB', (width, 2 * n * 64 * scale), sg.LIGHT[:3])
    draw = ImageDraw.Draw(sheet)
    x = travel
    for f in range(2 * n):
        x -= shifts[f % n]
        y = f * 64 * scale
        sheet.paste(sg._on(grids[f % n], pet, sg.LIGHT, scale), (x * scale, y))
        for a, b in paws_on_ground(grids[f % n], pet.OUTLINE, getattr(pet, 'FOOT_ROW', FOOT_ROW) - 1):
            draw.rectangle([((x + a) * scale, y + 63 * scale), ((x + b + 1) * scale - 1, y + 64 * scale - 1)],
                           fill=(220, 40, 40))
    sheet.save(os.path.join(sg.OUT, f'{name}_walk_on_ground.png'))


def check_pet(name):
    pet, anims = sg.build_pet(name)
    problems = []
    one_shot = set(getattr(pet, 'ONE_SHOT', ()))
    off_ground = set(getattr(pet, 'OFF_GROUND', ()))
    neutral = anims['idle'][1][0]
    # A pet may stand on its own row (the panda sits on its soles, a row lower: FOOT_ROW)
    feet = getattr(pet, 'FOOT_ROW', FOOT_ROW)
    print(f'== {name}: motion')
    print(f'  {"animation":11s} frames  ms  | feet first/last | body centre jump max / seam | join start/end '
          f'(pixels from the neutral pose)')
    for anim, (interval, grids) in anims.items():
        n = len(grids)
        jumps = [abs(centre(grids[i])[0] - centre(grids[i - 1])[0]) + abs(centre(grids[i])[1] - centre(grids[i - 1])[1])
                 for i in range(1, n)]
        seam = (abs(centre(grids[0])[0] - centre(grids[-1])[0]) + abs(centre(grids[0])[1] - centre(grids[-1])[1])
                if n > 1 else 0)
        first, last = foot_row(grids[0], pet.OUTLINE), foot_row(grids[-1], pet.OUTLINE)
        joins = (diff(grids[0], neutral), diff(grids[-1], neutral))
        flags = []
        if anim not in off_ground:
            ends = [first] if anim in one_shot else [first, last]
            if any(r != feet for r in ends):
                flags.append(f'feet on row {ends}, not {feet}')
        if anim == 'walk':
            rows = sorted({foot_row(g, pet.OUTLINE) for g in grids})
            if rows != [feet]:
                flags.append(f'walk feet on rows {rows}')
        print(f'  {anim:11s} {n:3d} x{interval:4d} | {first}/{last}          | {max(jumps) if jumps else 0:5.1f} / '
              f'{seam:5.1f}{"" if anim not in one_shot else " (once)"}     | {joins[0]:4d} / {joins[1]:4d}'
              + (f'   !! {"; ".join(flags)}' if flags else ''))
        problems += [f'{anim}: {f}' for f in flags]
    problems += check_walk(name, pet, anims['walk'][1])
    if getattr(pet, 'WALK_SHIFTS', None):
        walk_on_ground(name, pet, anims['walk'][1])
    return problems


def main(names):
    failed = {}
    for name in names:
        problems = check_pet(name)
        if problems:
            failed[name] = problems
    if failed:
        raise SystemExit('Motion checks failed: ' + '; '.join(f'{n}: {", ".join(p)}' for n, p in failed.items()))
    print('All motion checks passed')


if __name__ == '__main__':
    main(sys.argv[1:] or ['kitten'])
