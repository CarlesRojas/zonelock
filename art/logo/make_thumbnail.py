"""YouTube video thumbnail (1280x720) for the Zone Lock trailer, over a hero shot of the Amethyst Tower.

    ZONELOCK_TRAILER=thumbnail ./gradlew :fabric:runClientGameTest      # renders fabric/build/trailer/thumbnail/thumb_*.png
    python -X utf8 art/logo/make_thumbnail.py <thumb_N.png> [more shots...]

Thumbnails are mostly seen small, so it says little and says it big: the title, one short line, the tower in its zone.
With several shots it writes art/logo/out/thumbnail_N.png for each, to pick from; with one, art/logo/zonelock_thumbnail.png.
"""
import os
import sys

from PIL import Image, ImageEnhance

from make_banner import HERE, compose_module, logo, shade_left

W, H = 1280, 720


def thumbnail(shot, c):
    bg = Image.open(shot).convert("RGB").resize((W, H), Image.LANCZOS)
    bg = ImageEnhance.Color(bg).enhance(1.15)
    bg = ImageEnhance.Contrast(bg).enhance(1.06)
    img = shade_left(bg.convert("RGBA"), 0.9)
    mark = logo(c, 11)
    x = 54
    line1 = c.mc_text("Beat the dungeon", 5)
    line2 = c.mc_text("to break it", 5, (215, 160, 255))
    block = mark.height + 30 + line1.height + 10 + line2.height
    top = (H - block) // 2 - 10
    img.alpha_composite(mark, (x, top))
    y = top + mark.height + 30
    img.alpha_composite(line1, (x + 6, y))
    img.alpha_composite(line2, (x + 6, y + line1.height + 10))
    return img.convert("RGB")


def main():
    shots = sys.argv[1:]
    c = compose_module()
    if len(shots) == 1:
        out = os.path.join(HERE, "zonelock_thumbnail.png")
        thumbnail(shots[0], c).save(out, optimize=True)
        print("written", out)
        return
    os.makedirs(os.path.join(HERE, "out"), exist_ok=True)
    for i, shot in enumerate(shots):
        out = os.path.join(HERE, "out", f"thumbnail_{i}.png")
        thumbnail(shot, c).save(out)
        print("written", out)


if __name__ == "__main__":
    main()
