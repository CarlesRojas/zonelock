"""YouTube banner (2560x1440) and video thumbnail (1280x720) for Zone Lock, over a frame of the trailer.

    python -X utf8 art/logo/make_banner.py <trailer frame.png>

Everything that matters sits in the banner's safe area (the middle 1546x423, all that phones show): the logo on the
left, the tower on the right. TVs show the whole picture, desktops the full-width middle strip.
"""
import os
import sys

from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, os.path.join(ROOT, "art", "trailer"))

W, H = 2560, 1440
SAFE = ((W - 1546) // 2, (H - 423) // 2, (W + 1546) // 2, (H + 423) // 2)


def compose_module():
    argv = sys.argv
    sys.argv = [argv[0]]
    import compose as c
    sys.argv = argv
    c.MC = c.McFont()
    return c


def shade_left(img, strength):
    """A dark wash from the left, so the words read over the trees and sky."""
    w, h = img.size
    grad = Image.new("L", (w, 1))
    for x in range(w):
        t = max(0.0, 1 - x / (w * 0.62))
        grad.putpixel((x, 0), int(255 * strength * t ** 1.4))
    mask = grad.resize((w, h))
    dark = Image.new("RGBA", (w, h), (14, 6, 26, 255))
    out = img.copy()
    out.paste(dark, (0, 0), mask)
    return out


def logo(c, px):
    card = c.TitleCard("title", "ZONE LOCK", None, 0, 10, 1440, int(px * 1440 / 19))
    width = card.letters[-1][0] * card.px + card.letters[-1][1].width
    height = card.letters[0][1].height
    img = Image.new("RGBA", (width + 4, height + 4), (0, 0, 0, 0))
    for lx, letter in card.letters:
        img.alpha_composite(letter, (lx * card.px, 0))
    return img


def banner(frame, c):
    bg = Image.open(frame).convert("RGBA").resize((W, H), Image.LANCZOS)
    # The tower sits a little right of the middle in the trailer's opening: move it to the right of the safe area.
    shifted = Image.new("RGBA", (W, H))
    shift = 400
    shifted.paste(bg, (shift, 0))
    shifted.paste(bg.crop((0, 0, shift, H)).transpose(Image.FLIP_LEFT_RIGHT), (0, 0))
    img = shade_left(shifted, 0.78)
    mark = logo(c, 14)
    x, top = SAFE[0] + 40, SAFE[1] + 74
    img.alpha_composite(mark, (x, top))
    line = c.mc_text("Dungeons you have to beat before you can break them", 4)
    img.alpha_composite(line, (x + 6, top + mark.height + 24))
    tag = c.mc_text("Fabric & NeoForge  -  Minecraft 26.3", 4, (255, 255, 85))
    img.alpha_composite(tag, (x + 6, top + mark.height + 24 + line.height + 16))
    return img.convert("RGB")


def thumbnail(frame, c):
    bg = Image.open(frame).convert("RGBA").resize((1280, 720), Image.LANCZOS)
    shift = 290
    moved = Image.new("RGBA", bg.size)
    moved.paste(bg, (shift, 0))
    moved.paste(bg.crop((0, 0, shift, 720)).transpose(Image.FLIP_LEFT_RIGHT), (0, 0))
    img = shade_left(moved, 0.85)
    mark = logo(c, 10)
    x, top = 60, 200
    img.alpha_composite(mark, (x, top))
    line = c.mc_text("Dungeons you have to beat", 4)
    img.alpha_composite(line, (x + 4, top + mark.height + 18))
    line2 = c.mc_text("before you can break them", 4)
    img.alpha_composite(line2, (x + 4, top + mark.height + 18 + line.height + 6))
    tag = c.mc_text("Fabric & NeoForge", 4, (255, 255, 85))
    img.alpha_composite(tag, (x + 4, top + mark.height + 18 + 2 * line.height + 24))
    return img.convert("RGB")


def main():
    frame = sys.argv[1]
    c = compose_module()
    b = banner(frame, c)
    b.save(os.path.join(HERE, "zonelock_banner.png"), optimize=True)
    t = thumbnail(frame, c)
    t.save(os.path.join(HERE, "zonelock_thumbnail.png"), optimize=True)
    # A preview with the safe area and the desktop strip marked.
    p = b.copy()
    d = ImageDraw.Draw(p)
    d.rectangle(SAFE, outline=(255, 80, 80), width=6)
    d.rectangle((0, SAFE[1], W - 1, SAFE[3]), outline=(80, 200, 255), width=4)
    os.makedirs(os.path.join(HERE, "out"), exist_ok=True)
    p.resize((1280, 720)).save(os.path.join(HERE, "out", "banner_preview.png"))
    print("banner and thumbnail written")


if __name__ == "__main__":
    main()
