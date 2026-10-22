"""A pixel pet's whole awake spell as the widget shows it, as a GIF with the real frame times.

    ./gradlew testDebugUnitTest --tests "*SmoothMotionTest*"     (writes app/build/lives/<pet>.txt)
    python tools/sprite_gen/life_gif.py kitten [pet ...]         output/<pet>_life.gif

The life comes from the app's own engine (PetBehavior and PlanFrames, played by SmoothMotionTest's
model of the launcher): a tap wakes the pet, it lives on its own and falls asleep. The pet is
drawn on a plain ground x3, every frame for as long as the widget keeps it on screen (the
aquarium, a whole scene, as it is).
"""
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import sprite_gen as sg  # noqa: E402

SCALE = 3
NODPI = os.path.join(sg.RES, 'drawable-nodpi')
GROUND = (196, 186, 160)


def main(names):
    for name in names:
        path = os.path.join(sg.ROOT, 'app', 'build', 'lives', f'{name}.txt')
        lines = open(path, encoding='utf-8').read().split('\n')
        head = dict(zip(lines[0].split()[1::2], lines[0].split()[2::2]))
        pixel = int(head['pixel'])
        events = [line.split() for line in lines[1:] if line]
        size = Image.open(os.path.join(NODPI, f'raw_{events[0][1]}.png')).size
        scene = size != (64, 64)
        left = 0 if scene else int(head['left']) / pixel
        top = 0 if scene else int(head['top']) / pixel
        width = int(2 * left + size[0])
        sky = max(0, int(top) - 30)                      # room above the pet for its hops and hearts
        height = size[1] if scene else int(top + 64 + 6) - sky
        frames, durations = [], []
        sprites = {}
        for i, event in enumerate(events):
            if event[1] == 'end':
                break
            t, drawable, x, y, mirrored = int(event[0]), event[1], float(event[2]), float(event[3]), event[4] == '1'
            if drawable not in sprites:
                sprites[drawable] = Image.open(os.path.join(NODPI, f'raw_{drawable}.png')).convert('RGBA')
            sprite = sprites[drawable].transpose(Image.FLIP_LEFT_RIGHT) if mirrored else sprites[drawable]
            img = Image.new('RGBA', (width, height), sg.LIGHT)
            if scene:
                img.alpha_composite(sprite, (0, 0))
            else:
                img.paste(GROUND + (255,), (0, int(top) + 62 - sky, width, height))
                img.alpha_composite(sprite, (round(x), round(y) - sky))
            frames.append(img.convert('RGB').resize((width * SCALE, height * SCALE), Image.NEAREST))
            durations.append(max(20, int(events[i + 1][0]) - t))
        out = os.path.join(sg.OUT, f'{name}_life.gif')
        palette = [f.convert('P', palette=Image.ADAPTIVE, colors=64) for f in frames]
        palette[0].save(out, save_all=True, append_images=palette[1:], duration=durations, loop=0, disposal=2)
        print(f'wrote {os.path.relpath(out, sg.ROOT)}: {len(frames)} frames, {sum(durations) / 1000:.1f} s')


if __name__ == '__main__':
    main(sys.argv[1:] or ['kitten'])
