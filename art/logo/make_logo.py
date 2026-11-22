"""Profile-picture logos for Zone Lock (YouTube, Instagram...): 1024x1024, everything inside the circle a round
crop keeps, readable down to ~40 px.

    python -X utf8 art/logo/make_logo.py        # writes art/logo/out/*.png and a preview sheet
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
OUT = os.path.join(HERE, "out")
TEX = os.path.join(ROOT, "common", "src", "main", "resources", "assets", "zonelock", "textures")
SIZE = 1024

# Amethyst, gold and blackstone ramps, darkest first.
AME = ["#1c0f30", "#2e1a52", "#4b2a7a", "#6b3fae", "#8f5fd8", "#b48af2", "#d6bfff", "#f4ecff"]
GOLD = ["#3d2606", "#7a4e10", "#b57a1c", "#e3a93a", "#f7d36b", "#fff0b0"]
STONE = ["#0f0c13", "#1d1924", "#2c2633", "#3e3647", "#544a5e"]
INK = "#120a1d"


def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def background():
    """Deep purple, lighter in the middle, a faint grid of amethyst dust."""
    yy, xx = np.mgrid[0:SIZE, 0:SIZE].astype(np.float32)
    d = np.sqrt((xx - SIZE / 2) ** 2 + (yy - SIZE / 2) ** 2) / (SIZE * 0.62)
    d = np.clip(d, 0, 1) ** 1.3
    inner, outer = np.array(hexc("#3b2068")[:3]), np.array(hexc("#100818")[:3])
    rgb = inner[None, None, :] * (1 - d[..., None]) + outer[None, None, :] * d[..., None]
    img = Image.fromarray(rgb.astype(np.uint8), "RGB").convert("RGBA")
    rng = np.random.default_rng(4)
    draw = ImageDraw.Draw(img)
    for _ in range(26):
        a = rng.random() * 2 * math.pi
        r = SIZE * (0.36 + rng.random() * 0.12)
        x, y = SIZE / 2 + math.cos(a) * r, SIZE / 2 + math.sin(a) * r
        s = 8 if rng.random() < 0.7 else 16
        draw.rectangle([x, y, x + s, y + s], fill=hexc(AME[5], int(60 + rng.random() * 90)))
    return img


def pixels(grid_size, painter):
    """A pixel-art sprite: `painter(x, y)` returns a colour or None, on a grid_size canvas."""
    img = Image.new("RGBA", (grid_size, grid_size), (0, 0, 0, 0))
    px = img.load()
    for y in range(grid_size):
        for x in range(grid_size):
            c = painter(x, y)
            if c is not None:
                px[x, y] = hexc(c) if isinstance(c, str) else c
    return img


def outline(sprite, color=INK):
    """A 1-pixel dark outline round a sprite (on its own pixel grid)."""
    a = np.array(sprite)[:, :, 3] > 0
    grown = a.copy()
    for dy, dx in ((-1, 0), (1, 0), (0, -1), (0, 1)):
        grown |= np.roll(np.roll(a, dy, 0), dx, 1)
    out = np.array(sprite)
    edge = grown & ~a
    out[edge] = hexc(color)
    return Image.fromarray(out, "RGBA")


def glow(img, radius, color, strength):
    a = img.getchannel("A").filter(ImageFilter.GaussianBlur(radius))
    g = Image.new("RGBA", img.size, hexc(color, 0))
    g.putalpha(a.point(lambda v: int(v * strength)))
    return g


def place(sprite, scale, center=(SIZE // 2, SIZE // 2), glow_color=AME[5]):
    big = sprite.resize((sprite.width * scale, sprite.height * scale), Image.NEAREST)
    canvas = background()
    layer = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    layer.alpha_composite(big, (center[0] - big.width // 2, center[1] - big.height // 2))
    canvas.alpha_composite(glow(layer, 60, glow_color, 0.55))
    canvas.alpha_composite(layer)
    return canvas


def shade(ramp, t):
    t = min(max(t, 0.0), 0.999)
    return ramp[int(t * len(ramp))]


# ------------------------------------------------------------------ A: the core, with its crystal over it

def logo_core():
    G = 32

    def paint(x, y):
        # Crystal: a tall gem floating over the block (x 12..19, y 2..13).
        cx = 15.5
        if 2 <= y <= 13:
            half = (y - 1) * 0.75 if y < 7 else (14 - y) * 0.75
            half = min(half, 3.6)
            if abs(x - cx) <= half:
                t = 0.95 - (x - (cx - half)) / (2 * half + 0.01) * 0.55 - (y - 2) * 0.02
                if abs(x - cx) < 0.6:
                    t += 0.15
                return shade(AME[2:], t)
        # Block: blackstone body with gold corners and an amethyst glyph, x 7..24, y 15..30.
        if 7 <= x <= 24 and 16 <= y <= 30:
            gx, gy = x - 7, y - 16
            corner = (gx < 3 or gx > 14) and (gy < 3 or gy > 11)
            if corner:
                return shade(GOLD[1:], 0.75 - (gy % 3) * 0.12 + (0.1 if gx % 3 == 0 else 0))
            if gx in (0, 17) or gy in (0, 14):
                return STONE[3]
            if 5 <= gx <= 12 and 3 <= gy <= 11:
                d = abs(gx - 8.5) + abs(gy - 7)
                if d <= 4.5:
                    return shade(AME[3:], 1 - d / 5.5)
            return STONE[1] if (gx + gy) % 5 else STONE[2]
        return None

    return place(outline(pixels(G, paint)), 24)


# ------------------------------------------------------------------ B: a padlock of amethyst with a gold shackle

def logo_padlock():
    G = 32

    def paint(x, y):
        # Shackle: a gold arch, 3 px thick, x 9..22, y 3..15.
        cx, cy, r_out, r_in = 15.5, 13.0, 7.5, 4.6
        d = math.hypot(x - cx, y - cy)
        if y <= 13 and r_in <= d <= r_out:
            t = 0.85 - (d - r_in) / (r_out - r_in) * 0.5 - (0.15 if x > cx else 0)
            return shade(GOLD[1:], t)
        if 13 < y <= 15 and (8 <= x <= 10 or 21 <= x <= 23):
            return shade(GOLD[1:], 0.55 if x < 16 else 0.4)
        # Body: an amethyst block with facets, x 5..26, y 15..29, a keyhole.
        if 5 <= x <= 26 and 15 <= y <= 29:
            kx, ky = x - 15.5, y - 21
            if math.hypot(kx, ky) <= 2.2 or (abs(kx) <= 1.0 + (y - 21) * 0.2 and 21 <= y <= 26):
                return INK
            if x in (5, 26) or y in (15, 29):
                return AME[2]
            facet = ((x - 5) // 4 + (y - 15) // 4) % 3
            t = 0.75 - (y - 15) / 14 * 0.45 + (0.12 if facet == 0 else 0) - (0.08 if facet == 2 else 0)
            if y == 16 or x == 6:
                t += 0.2
            return shade(AME[2:], t)
        return None

    return place(outline(pixels(G, paint)), 26, center=(SIZE // 2, SIZE // 2 + 10))


# ------------------------------------------------------------------ C: the gem with a keyhole, in a gold ring

def logo_gem():
    G = 32

    def paint(x, y):
        cx = cy = 15.5
        d = math.hypot(x - cx, y - cy)
        if d > 14.6:
            return None
        if d > 12.4:
            # Gold ring, lit from the top left.
            ang = math.atan2(y - cy, x - cx)
            t = 0.55 + 0.35 * math.cos(ang + 2.36) - (0.15 if d > 13.6 else 0)
            return shade(GOLD[1:], t)
        if d > 11.4:
            return STONE[1]
        # The gem: facets around a keyhole, light in the top left.
        kx, ky = x - cx, y - cy + 2
        if math.hypot(kx, ky) <= 3.0 or (abs(kx) <= 1.2 + max(0, ky) * 0.25 and 0 <= ky <= 8):
            return INK
        ang = math.atan2(y - cy, x - cx)
        facet = int((ang + math.pi) / (math.pi / 4)) % 2
        t = 0.62 + 0.3 * math.cos(ang + 2.36) * (d / 11) + (0.08 if facet else -0.04) - d * 0.012
        if d < 6 and (x + y) % 7 == 0:
            t += 0.15
        return shade(AME[1:], t)

    return place(outline(pixels(G, paint)), 26)


# ------------------------------------------------------------------ D: ZL in the trailer's logo letters

def monogram_letter(mask, px):
    """One blocky letter like the trailer's logo (bold game font, deep extrusion, dark outline), its face a clean
    amethyst gradient instead of the block texture: at avatar size the texture only reads as noise."""
    h, w = mask.shape
    depth = 2
    H2, W2 = h + depth + 2, w + 2
    img = np.zeros((H2, W2, 4), dtype=np.uint8)
    face = np.zeros((H2, W2), dtype=bool)
    face[1:1 + h, 1:1 + w] = mask
    body = face.copy()
    for d in range(1, depth + 1):
        body[1 + d:1 + d + h, 1:1 + w] |= mask
    out = body.copy()
    for dy, dx in ((-1, 0), (1, 0), (0, -1), (0, 1)):
        out |= np.roll(np.roll(body, dy, 0), dx, 1)
    img[out] = hexc(INK)
    for d in range(depth, 0, -1):
        m = np.zeros_like(face)
        m[1 + d:1 + d + h, 1:1 + w] = mask
        img[m & ~face] = hexc(AME[1] if d == 1 else AME[0])
    ramp = AME[3:]
    for y, x in zip(*np.nonzero(face)):
        t = 1.0 - (y - 1) / max(1, h - 1) * 0.5
        if not face[y - 1, x]:
            img[y, x] = hexc(AME[7])
        elif not face[y, x - 1]:
            img[y, x] = hexc(shade(ramp, t + 0.1))
        else:
            img[y, x] = hexc(shade(ramp, t))
    sprite = Image.fromarray(img, "RGBA")
    return sprite.resize((W2 * px, H2 * px), Image.NEAREST)


def logo_monogram():
    """ZL in the trailer's logo letters, as big as the round crop allows, whole pixels only."""
    sys.path.insert(0, os.path.join(ROOT, "art", "trailer"))
    sys.argv = [sys.argv[0]]
    import compose as c
    font = c.McFont()
    masks, x, positions = [], 0, []
    for ch in "ZL":
        m = font.mask(ch, bold=True)
        m = m[:, :m.any(axis=0).nonzero()[0].max() + 1]
        masks.append(m)
        positions.append(x)
        x += font.advance[ch] + 1
    logo_w = positions[-1] + masks[-1].shape[1] + 2
    px = int(SIZE * 0.68 / logo_w)
    canvas = background()
    layer = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    total = logo_w * px
    for pos, m in zip(positions, masks):
        letter = monogram_letter(m, px)
        layer.alpha_composite(letter, (int(SIZE / 2 - total / 2 + pos * px), int(SIZE / 2 - letter.height / 2 + px)))
    canvas.alpha_composite(glow(layer, 70, AME[5], 0.6))
    canvas.alpha_composite(layer)
    return canvas


# ------------------------------------------------------------------ preview

def circle(img, size):
    small = img.resize((size, size), Image.LANCZOS)
    mask = Image.new("L", (size * 4, size * 4), 0)
    ImageDraw.Draw(mask).ellipse([0, 0, size * 4 - 1, size * 4 - 1], fill=255)
    mask = mask.resize((size, size), Image.LANCZOS)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(small, (0, 0), mask)
    return out


def main():
    os.makedirs(OUT, exist_ok=True)
    logos = [("A_core", logo_core()), ("B_padlock", logo_padlock()), ("C_gem", logo_gem()), ("D_monogram", logo_monogram())]
    for name, img in logos:
        img.convert("RGB").save(os.path.join(OUT, f"zonelock_{name}.png"))
    # Preview: each logo as a round avatar at 320, 96, 48 and 32 px, on light and dark grounds.
    W = 1600
    sheet = Image.new("RGBA", (W, 520 * len(logos) // 2 + 40), "#f2f2f2")
    for i, (name, img) in enumerate(logos):
        ox, oy = (i % 2) * 800 + 30, (i // 2) * 520 + 30
        sheet.alpha_composite(circle(img, 320), (ox, oy))
        x = ox + 350
        for s in (96, 48, 32):
            sheet.alpha_composite(circle(img, s), (x, oy + 20))
            x += s + 20
        dark = Image.new("RGBA", (380, 150), "#0f0f0f")
        x = 20
        for s in (96, 48, 32):
            dark.alpha_composite(circle(img, s), (x, 20))
            x += s + 20
        sheet.alpha_composite(dark, (ox + 350, oy + 150))
        ImageDraw.Draw(sheet).text((ox, oy + 335), name.split("_")[0], fill="#222222")
    sheet.convert("RGB").save(os.path.join(OUT, "preview.png"))
    print("wrote", OUT)


if __name__ == "__main__":
    main()
