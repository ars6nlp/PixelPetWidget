"""Previews of the pets' interactions with the pixel-art backgrounds (BackgroundEvents.TABLE).

Reads the table from BackgroundEvents.java and the poses from PetSpec.java, so it shows what the
app has, and plays each interaction the way the widget does, on a 4x3 widget at 2x: the pet
notices the prop and walks over (or the leaf comes to it), plays with it while the background does
its part, then does its punchline. Timings follow PetBehavior roughly (one run of each part);
the widget's own randomness (where the pet stands, how often a loop repeats) is left out.

    python tools/sprite_gen/interactions.py      output/interaction_<bg>_<pet>.gif, output/interactions_<bg>.png

Run it after sprite_gen.py and scenery.py (it reads their resources and modules).
"""
import math
import os
import re
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import scenery  # noqa: E402
import sprite_gen as sg  # noqa: E402

JAVA = sg.JAVA_DIR
NODPI = os.path.join(sg.RES, 'drawable-nodpi')
SCALE = 2
TICK = 100                         # ms per GIF frame, the effects' interval
WINDOW = (240, 120)                # canvas pixels around the action
BG_OF = {'MAPLE_PARK': 'maple', 'RAINY_EVENING': 'rainy', 'PUMPKIN_FIELD': 'pumpkin'}
PET_OF = {'PET_CAT': 'cat', 'PET_DOG': 'dog', 'PET_RABBIT': 'rabbit', 'PET_PANDA': 'panda',
          'PET_HAMSTER': 'hamster', 'PET_CAPYBARA': 'capybara', 'PET_PARROT': 'parrot', 'PET_PUPPY': 'puppy',
          'PET_KITTEN': 'kitten'}
# The hand-drawn pets: animation -> frame array in PixelCatWidget.java
CLASSIC = {
    'cat': {'SLEEP': 'SLEEP_FRAMES', 'LICK': 'LICK_FRAMES', 'SEAT': 'CAT_SEAT_FRAMES', 'WALK': 'WALK_FRAMES'},
    'dog': {'SLEEP': 'DOG_SLEEP_FRAMES', 'EAT': 'DOG_EAT_FRAMES', 'LICK': 'DOG_SIT_FRAMES', 'WALK': 'DOG_WALK_FRAMES'},
    'rabbit': {'SLEEP': 'RABBIT_SLEEP_FRAMES', 'EAT': 'RABBIT_EAT_FRAMES', 'SEAT': 'RABBIT_SIT_FRAMES',
               'WALK': 'RABBIT_WALK_FRAMES'},
}
FACES_RIGHT = {'dog'}
SPECIAL = {'panda': 'roll', 'hamster': 'cheeks', 'capybara': 'bath', 'parrot': 'talk', 'puppy': 'chase',
           'kitten': 'yarn'}
WALK_STEP = 1.8 / 7
NOTICE_MS = 3500
LEAF_FRAMES = 30


def read(name):
    return open(os.path.join(JAVA, name), encoding='utf-8').read()


def table():
    """The rows of BackgroundEvents.TABLE as dicts."""
    src = read('BackgroundEvents.java')
    aliases = dict(re.findall(r'static final int (\w+) = (ANIMATION_POSE_\d);', src))
    body = src[src.index('static final Interaction[] TABLE'):]
    body = body[:body.index('};')]
    rows = []
    for chunk in re.split(r'\n\s*row\(', body)[1:]:
        pet, bg, weight, what = re.match(r'(\w+), (\w+), (\d+), "([^"]*)"', chunk).groups()
        row = {'pet': PET_OF[pet], 'bg': BG_OF[bg], 'what': what, 'prop': None, 'notice': None, 'effect': None}
        for call, args in re.findall(r'\.(\w+)\(([^)]*)\)', chunk):
            values = [a.strip().strip('"') for a in args.split(',')] if args else []
            values = [aliases.get(v, v).replace('ANIMATION_', '') for v in values]
            if call == 'goesTo':
                row['prop'], row['notice'] = values
            elif call in ('act', 'then'):
                row[call] = values[0]
            elif call == 'leaf':
                row['effect'] = ('leaf', None if values[0] == 'GROUND' else (int(values[0]), int(values[1])))
            elif call == 'drops':
                row['effect'] = ('drops', (int(values[0]), int(values[1])))
            elif call == 'state':
                row['effect'] = ('state', values[0], values[1])
        rows.append(row)
    return rows


def poses():
    """Pose names per pixel pet, from the .poses(...) lines of PetSpec.java."""
    src = read('PetSpec.java')
    out = {}
    for name, args in re.findall(r'pixelPet\(PET_\w+, "(\w+)"[\s\S]*?\.poses\(([^)]*)\)', src):
        out[name] = [a.strip().strip('"') for a in args.split(',')]
    return out


class Pet:
    """A pet's frames per animation id (names as in the table) and their intervals, at SCALE."""

    def __init__(self, name, pose_names):
        self.name = name
        self.anims = {}
        self.pixel = name not in CLASSIC
        size = 64 * SCALE
        if self.pixel:
            pet, anims = sg.build_pet(name)
            ids = {'SLEEP': 'sleep', 'HAPPY': 'happy', 'IDLE': 'idle', 'EAT': 'eat', 'SPECIAL': SPECIAL[name],
                   'WALK': 'walk'}
            for i, pose in enumerate(pose_names.get(name, [])):
                ids[f'POSE_{i + 1}'] = pose
                # A pose's way in and out (onto the pumpkin, into the heap), when drawn
                for end in ('in', 'out'):
                    if f'{pose}_{end}' in anims:
                        ids[f'POSE_{i + 1}_{end.upper()}'] = f'{pose}_{end}'
            # The smooth motion's transitions, when drawn
            for key in ('turn', 'walk_start', 'walk_stop'):
                if key in anims:
                    ids[key.upper()] = key
            for key, anim in ids.items():
                interval, grids = anims[anim]
                frames = [sg.to_image(g, pet).resize((size, size), Image.NEAREST) for g in grids][:24]
                self.anims[key] = (interval, frames)
            # Walking in step with its paws (PetSpec's gait): the module's shifts at the gait's tempo
            self.shifts = getattr(pet, 'WALK_SHIFTS', None)
            gait = re.search(name.upper() + r'_GAIT = new Gait\((\d+)', read('PetSpec.java'))
            if self.shifts and gait:
                self.anims['WALK'] = (int(gait.group(1)), self.anims['WALK'][1])
        else:
            self.shifts = None
            src = read('PixelCatWidget.java')
            for key, array in CLASSIC[name].items():
                body = re.search(array + r' = \{([^}]*)\}', src).group(1)
                frames = []
                for res in re.findall(r'R\.drawable\.(\w+)', body)[:16]:
                    path = next(os.path.join(NODPI, f) for f in os.listdir(NODPI) if f.startswith(f'raw_{res}.'))
                    frames.append(Image.open(path).convert('RGBA').resize((size, size), Image.LANCZOS))
                self.anims[key] = (130 if key == 'WALK' else 100, frames)

    def frame(self, anim, t_ms, facing_right):
        interval, frames = self.anims[anim]
        img = frames[(t_ms // interval) % len(frames)]
        mirrored = (self.name in FACES_RIGHT) != facing_right
        return img.transpose(Image.FLIP_LEFT_RIGHT) if mirrored else img

    def loop_ms(self, anim):
        interval, frames = self.anims[anim]
        return interval * len(frames)


def fall(start, end, frames, sway=10):
    (x0, y0), (x1, y1) = start, end
    out = []
    for i in range(frames):
        t = i / (frames - 1)
        swing = math.sin(t * 3 * math.pi) * (1 - 0.3 * t)
        turning = math.cos(t * 3 * math.pi)
        sprite = 0 if abs(turning) > 0.7 else (1 if turning > 0 else 3) if abs(turning) > 0.25 else 2
        out.append((round(x0 + (x1 - x0) * t + sway * swing), round(y0 + (y1 - y0) * (0.25 * t + 0.75 * t * t * (3 - 2 * t))),
                    sprite))
    return out


def smooth_timeline(row, pet, start, target, act_ms):
    """The interaction as the widget plays it for a pet with smooth motion (PetBehavior's timelines):
    a list of (start ms, length ms, [(animation, frame, art px along the way, facing) per tick],
    tick ms, from, to, facing, effect)."""
    amp = scenery.load(row['bg']).PREVIEW_AMPLITUDE
    out = []
    t = 0
    facing = False                                   # it sat looking left before
    walk_tick = pet.anims['WALK'][0]
    if row['prop']:
        right = target > start
        seq = []
        if right != facing and 'TURN' in pet.anims:
            n = len(pet.anims['TURN'][1])
            seq += [('TURN', i, 0, facing) for i in range(n)] + [('TURN', i, 0, right) for i in range(n - 2, -1, -1)]
        look = max(1, round(1200 / walk_tick))
        seq += [(row['notice'], j % len(pet.anims[row['notice']][1]), 0, right) for j in range(look)]
        if 'WALK_START' in pet.anims:
            seq += [('WALK_START', i, 0, right) for i in range(len(pet.anims['WALK_START'][1]))]
        distance = round(abs(target - start) * amp)
        px, k = 0, 0
        while px < distance:
            px = min(distance, px + pet.shifts[k % len(pet.shifts)])
            seq.append(('WALK', k % len(pet.anims['WALK'][1]), px, right))
            k += 1
        if 'WALK_STOP' in pet.anims:
            seq += [('WALK_STOP', i, distance, right) for i in range(len(pet.anims['WALK_STOP'][1]))]
        out.append((t, len(seq) * walk_tick, seq, walk_tick, start, target, right, None))
        t += len(seq) * walk_tick
        # There it looks at the prop until the interaction begins (the rest of the 5 s update)
        wait = max(0, 5000 - len(seq) * walk_tick)
        if wait:
            interval, frames = pet.anims[row['notice']]
            n = max(1, math.ceil(wait / (interval * len(frames))))
            seq = [(row['notice'], j, 0, right) for _ in range(n) for j in range(len(frames))]
            out.append((t, len(seq) * interval, seq, interval, target, target, right, None))
            t += len(seq) * interval
        facing = right
    act = row['act']
    interval, frames = pet.anims[act]
    seq = []
    if act + '_IN' in pet.anims:
        seq += [(act + '_IN', i, 0, facing) for i in range(len(pet.anims[act + '_IN'][1]))]
    loops = max(1, math.ceil(act_ms / (interval * len(frames))))
    seq += [(act, j, 0, facing) for _ in range(loops) for j in range(len(frames))]
    if act + '_OUT' in pet.anims:
        seq += [(act + '_OUT', i, 0, facing) for i in range(len(pet.anims[act + '_OUT'][1]))]
    out.append((t, len(seq) * interval, seq, interval, target, target, facing, row['effect']))
    return out


def play(row, pet):
    """GIF frames of one interaction, and a few key frames for the sheet."""
    bg = scenery.load(row['bg'])
    amp = bg.PREVIEW_AMPLITUDE
    mid = bg.CANVAS[0] / 2
    target = bg.PROPS[row['prop']]['offset'] if row['prop'] else 0.0
    start = target + (0.55 if target < 0.2 else -0.55) if row['prop'] else 0.1
    timeline = []                                    # (ms, animation, position, facing, effect)
    t = 0
    facing = target > start if row['prop'] else False
    act_loop = pet.loop_ms(row['act'])
    effect_ms = 3800 if row['effect'] and row['effect'][0] == 'leaf' else 2800 if row['effect'] and row['effect'][0] == 'drops' else 0
    if pet.shifts:
        smooth = smooth_timeline(row, pet, start, target, max(2 * act_loop, effect_ms))
        for s0, length, seq, tick, a, b, face, effect in smooth:
            timeline.append((s0, length, ('SEQ', seq, tick), a, b, face, effect))
        t = smooth[-1][0] + smooth[-1][1]
        facing = smooth[-1][6]
        act_start, act_ms = smooth[-1][0], smooth[-1][1]
    elif row['prop']:
        notice = row['notice']
        n = max(2, math.ceil(NOTICE_MS / pet.loop_ms(notice)))
        timeline.append((t, n * pet.loop_ms(notice), notice, start, start, facing, None))
        t += n * pet.loop_ms(notice)
        steps = max(1, round(abs(target - start) / WALK_STEP))
        step_ms = pet.anims['WALK'][0]
        timeline.append((t, steps * step_ms, 'WALK', start, target, facing, None))
        t += steps * step_ms
    if not pet.shifts:
        act_ms = max(2, math.ceil(effect_ms / act_loop)) * act_loop
        timeline.append((t, act_ms, row['act'], target, target, facing, row['effect']))
        act_start = t
        t += act_ms
    then_ms = pet.loop_ms(row["then"]) * (2 if pet.loop_ms(row["then"]) < 1500 else 1)
    away = target + (-0.5 if target > 0 else 0.5)
    timeline.append((t, then_ms, row['then'], target, away if row['then'] == 'WALK' else target,
                     (away > target) if row['then'] == 'WALK' else facing, None))
    total = t + then_ms

    base = scenery.image(bg.SCENE, bg.PALETTE)
    focus = mid + (target if row['prop'] else 0.1) * amp
    x0 = int(max(0, min(bg.CANVAS[0] - WINDOW[0], focus - WINDOW[0] / 2)))
    y0 = bg.GROUND + 14 - WINDOW[1]
    leaf_end = None
    frames = []
    for ms in range(0, total, TICK):
        seg = next(s for s in timeline if s[0] <= ms < s[0] + s[1])
        s0, length, anim, a, b, face, effect = seg
        local = ms - s0
        if isinstance(anim, tuple):
            # A smooth timeline: the entry on screen, and where it is along its way
            _, seq, tick = anim
            key, index, px, face = seq[min(len(seq) - 1, local // tick)]
            pos = a + (1 if b > a else -1) * px / amp
            sprite = pet.anims[key][1][index]
            mirrored = (pet.name in FACES_RIGHT) != face
            pet_image = sprite.transpose(Image.FLIP_LEFT_RIGHT) if mirrored else sprite
        elif anim == 'WALK' and a != b:
            steps = max(1, round(abs(b - a) / WALK_STEP))
            k = min(steps, local // pet.anims['WALK'][0])
            pos = a + (b - a) * k / steps
        else:
            pos = a
        if not isinstance(anim, tuple):
            pet_image = pet.frame(anim, local, face)
        img = base.copy()
        # Elements and props, as in scenery.scene_with, with the interaction's prop state
        tick = ms
        for element in bg.ELEMENTS.values():
            ex, ey, f = element['path'][(tick // element['interval']) % element['length']]
            img.alpha_composite(scenery.image(element['frames'][f], bg.PALETTE), (ex, ey))
        front = []
        for name, prop in bg.PROPS.items():
            interval, sprites = prop['states'][prop['ambient']]
            sprite = sprites[(tick // interval) % len(sprites)]
            changing = row['effect'] and row['effect'][0] == 'state' and name == row['prop']
            if changing and ms >= act_start:
                _, state, timing = row['effect']
                interval, sprites = prop['states'][state]
                since = ms - act_start
                if timing == 'ONCE':
                    # One pass, then it keeps still in its last frame until the next update
                    sprite = sprites[min(since // interval, len(sprites) - 1)]
                elif timing == 'PLAN' or ms < act_start + act_ms:
                    sprite = sprites[(since // interval) % len(sprites)]
                else:
                    interval, sprites = prop['states'][prop['ambient']]
                    sprite = sprites[(tick // interval) % len(sprites)]
            placed = (scenery.image(sprite, bg.PALETTE), scenery.prop_position(bg, prop, sprite))
            (front if prop['layer'] == 'front' else []).append(placed)
            if prop['layer'] != 'front':
                img.alpha_composite(*placed)
        big = img.resize((img.width * SCALE, img.height * SCALE), Image.NEAREST)
        px = round((mid + pos * amp - 32) * SCALE)
        py = (bg.GROUND - 61) * SCALE
        big.alpha_composite(pet_image, (px, py))
        overlay = Image.new('RGBA', img.size, (0, 0, 0, 0))
        for sprite, at in front:
            overlay.alpha_composite(sprite, at)
        if effect and effect[0] == 'leaf':
            anchor = effect[1]
            if anchor is None:
                lx, ly = round(mid + pos * amp + (1 if face else -1) * 0.42 * 64 - 3), bg.GROUND - 6
            else:
                ax = anchor[0] + 0.5 if (pet.name in FACES_RIGHT) == face else 64 - anchor[0] - 0.5
                lx, ly = round(mid + pos * amp - 32 + ax - 3.5), round(bg.GROUND - 61 + anchor[1] - 2.5)
            col = next(c for d in range(300) for c in (lx - 12 + d, lx - 12 - d) if 0 <= c < 576 and bg.CANOPY_EDGE[c] >= 0)
            path = fall((col, bg.CANOPY_EDGE[col] - 2), (lx, ly), LEAF_FRAMES)
            i = local // TICK
            if i < LEAF_FRAMES:
                x, y, s = path[i]
                overlay.alpha_composite(scenery.image(bg.EVENT_SPRITES[f'leaf_{s}'], bg.PALETTE), (x, y))
            elif anchor is None or ms < s0 + length:
                sprite = bg.EVENT_SPRITES['leaf_lying' if anchor is None else 'leaf_0']
                overlay.alpha_composite(scenery.image(sprite, bg.PALETTE), (lx, ly))
            leaf_end = leaf_end or (lx, ly)
        if (row['effect'] and row['effect'][0] == 'leaf' and row['effect'][1] is None and leaf_end
                and not (effect and effect[0] == 'leaf')):
            overlay.alpha_composite(scenery.image(bg.EVENT_SPRITES['leaf_lying'], bg.PALETTE), leaf_end)
        if effect and effect[0] == 'drops':
            ax = effect[1][0] + 0.5 if not face else 64 - effect[1][0] - 0.5
            mx, my = mid + pos * amp - 32 + ax, bg.GROUND - 61 + effect[1][1] + 0.5
            f = (local // TICK) % 14
            if f < 8:
                tt = (f + 1) / 8
                overlay.alpha_composite(scenery.image(bg.EVENT_SPRITES['drop'], bg.PALETTE),
                                        (round(mx - 0.5), round(my - 3 - 56 * (1 - tt * tt))))
        for tile in getattr(bg, 'TILES', {}).values():
            frame = scenery.image(tile['frames'][(tick // tile['interval']) % len(tile['frames'])], bg.PALETTE)
            for ty in range(0, img.height, frame.height):
                for tx in range(0, img.width, frame.width):
                    overlay.alpha_composite(frame, (tx, ty))
        big.alpha_composite(overlay.resize(big.size, Image.NEAREST))
        frames.append(big.crop((x0 * SCALE, y0 * SCALE, (x0 + WINDOW[0]) * SCALE, (y0 + WINDOW[1]) * SCALE)))
    keys = [frames[min(len(frames) - 1, (s[0] + s[1] // 2) // TICK)] for s in timeline]
    return frames, keys


def main():
    rows = table()
    pose_names = poses()
    pets = {}
    by_bg = {}
    for row in rows:
        pet = pets.setdefault(row['pet'], Pet(row['pet'], pose_names))
        frames, keys = play(row, pet)
        name = f'interaction_{row["bg"]}_{row["pet"]}'
        small = [f.convert('RGB').quantize(colors=64, method=Image.Quantize.MAXCOVERAGE, dither=Image.Dither.NONE)
                 for f in frames]
        small[0].save(os.path.join(sg.OUT, name + '.gif'), save_all=True, append_images=small[1:], duration=TICK,
                      loop=0, optimize=True)
        by_bg.setdefault(row['bg'], []).append((row, keys))
        print(name, len(frames), 'frames')
    for bgname, items in by_bg.items():
        w, h = WINDOW[0] * SCALE // 2, WINDOW[1] * SCALE // 2
        cols = 4
        sheet = Image.new('RGB', (cols * w, len(items) * (h + 16)), (255, 255, 255))
        draw = ImageDraw.Draw(sheet)
        for r, (row, keys) in enumerate(items):
            draw.text((4, r * (h + 16) + 2), f'{row["pet"]}: {row["what"]}', fill=(0, 0, 0))
            for c, key in enumerate(keys[-cols:] if len(keys) > cols else keys):
                sheet.paste(key.convert('RGB').resize((w, h), Image.NEAREST), (c * w, r * (h + 16) + 16))
        sheet.save(os.path.join(sg.OUT, f'interactions_{bgname}.png'))


if __name__ == '__main__':
    main()
