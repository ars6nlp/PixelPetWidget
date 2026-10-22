"""Makes the app font from Tiny5 (SIL OFL 1.1, see licenses/Tiny5-OFL.txt).

    python tools/font/prepare_font.py path/to/Tiny5-Regular.ttf path/to/OFL.txt

Tiny5: https://github.com/Gissio/font_tiny5 (also on Google Fonts). The font is drawn on a
grid of 8 pixels per em (128 of 1024 units), so text is crisp when its size in pixels is a
multiple of 8; PixelTheme.styleText() snaps text sizes to that.

Writes app/src/main/res/font/tiny5_regular.ttf cut down to Latin, Cyrillic, digits and common
punctuation, and copies the licence to licenses/ and to the app assets (shown in "About").
Tiny5 declares no Reserved Font Name, so the cut-down file may keep its name.
Needs fontTools (pip install fonttools).
"""
import os
import shutil
import sys

from fontTools import subset
from fontTools.ttLib import TTFont

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
FONT_OUT = os.path.join(ROOT, 'app', 'src', 'main', 'res', 'font', 'tiny5_regular.ttf')
LICENCE_OUT = [os.path.join(ROOT, 'licenses', 'Tiny5-OFL.txt'),
               os.path.join(ROOT, 'app', 'src', 'main', 'assets', 'licenses', 'Tiny5-OFL.txt')]

UNICODES = (
    list(range(0x20, 0x7F))            # Basic Latin
    + list(range(0xA0, 0x100))         # Latin-1: « » × · nbsp and accented letters
    + list(range(0x400, 0x460))        # Cyrillic, with Ё ё
    + [0x2013, 0x2014, 0x2018, 0x2019, 0x201C, 0x201D, 0x201E, 0x2022, 0x2026, 0x2116]
)
# Upper half of the Latin-1 range the app never shows and the font may not draw
OPTIONAL = set(range(0xA0, 0x100)) | set(range(0x400, 0x410)) | set(range(0x450, 0x460))
REQUIRED_EXTRA = {0x401, 0x451, 0xAB, 0xBB, 0xD7, 0xB7}


def main(source, licence):
    font = TTFont(source)
    options = subset.Options()
    options.layout_features = ['kern', 'liga', 'locl']
    options.name_IDs = ['*']               # keep copyright and licence records
    options.notdef_outline = True
    subsetter = subset.Subsetter(options)
    subsetter.populate(unicodes=UNICODES)
    subsetter.subset(font)
    os.makedirs(os.path.dirname(FONT_OUT), exist_ok=True)
    font.save(FONT_OUT)

    cmap = font.getBestCmap()
    missing = [hex(u) for u in UNICODES if u not in cmap and (u not in OPTIONAL or u in REQUIRED_EXTRA)]
    if missing:
        raise SystemExit(f'Font lacks required characters: {" ".join(missing)}')
    print(f'{os.path.relpath(FONT_OUT, ROOT)}: {os.path.getsize(FONT_OUT)} bytes, {len(cmap)} characters')

    for path in LICENCE_OUT:
        os.makedirs(os.path.dirname(path), exist_ok=True)
        shutil.copyfile(licence, path)
        print(f'{os.path.relpath(path, ROOT)}')


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])
