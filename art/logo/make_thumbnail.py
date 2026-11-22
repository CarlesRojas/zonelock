"""YouTube video thumbnail (1280x720) for the Zone Lock trailer, over a hero shot of the Amethyst Tower.

    ZONELOCK_TRAILER=thumbnail ./gradlew :fabric:runClientGameTest      # renders fabric/build/trailer/thumbnail/thumb_*.png
    python -X utf8 art/logo/make_thumbnail.py <thumb_N.png> [more shots...]

Thumbnails are mostly seen small, so it says little and says it big: the title, one short line, the tower in its zone.
With several shots it writes art/logo/out/thumbnail_N.png for each, to pick from; with one, art/logo/zonelock_thumbnail.png.
"""
import os
import sys

from PIL import Image, ImageEnhance

from make_banner import HERE, compose_module, shade_left

W, H = 1280, 720


def word(c, text, px):
    """One word of the title in the trailer's pixel letters."""
    card = c.TitleCard("title", text, None, 0, 10, 1440, int(px * 1440 / 19))
    width = card.letters[-1][0] * card.px + card.letters[-1][1].width
    img = Image.new("RGBA", (width + 4, card.letters[0][1].height + 4), (0, 0, 0, 0))
    for lx, letter in card.letters:
        img.alpha_composite(letter, (lx * card.px, 0))
    return img


def thumbnail(shot, c):
    bg = Image.open(shot).convert("RGB").resize((W, H), Image.LANCZOS)
    bg = ImageEnhance.Color(bg).enhance(1.15)
    bg = ImageEnhance.Contrast(bg).enhance(1.06)
    img = shade_left(bg.convert("RGBA"), 0.95)
    # The title on two lines, so it can be big without reaching the tower.
    zone, lock = word(c, "ZONE", 14), word(c, "LOCK", 14)
    line1 = c.mc_text("Beat the dungeon", 6)
    line2 = c.mc_text("to break it", 6, (215, 160, 255))
    gap = int(zone.height * 0.08)
    block = zone.height + gap + lock.height + 34 + line1.height + 12 + line2.height
    x, top = 50, (H - block) // 2
    img.alpha_composite(zone, (x, top))
    img.alpha_composite(lock, (x, top + zone.height + gap))
    y = top + zone.height + gap + lock.height + 34
    img.alpha_composite(line1, (x + 8, y))
    img.alpha_composite(line2, (x + 8, y + line1.height + 12))
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
