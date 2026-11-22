"""Turns the recorded trailer (fabric/build/trailer/frames + events.jsonl, from TrailerTest) into the video.

    python -X utf8 art/trailer/compose.py [--preview]

- Cuts between the sets crossfade; the video fades in from and out to black.
- Words in the game's own font: the story in tooltip frames bottom left, rising on a soft spring; the title as a
  blocky amethyst logo with crystals round it, letters dropping in; the end card with the title screen's splash.
- Sound: every sound the game played while recording, at its volume, pitch, distance and side; under it a calm
  synthesised pad with amethyst chimes.
Output: fabric/build/trailer/zonelock_trailer.mp4 (2560x1440, 60 fps).
"""
import json
import math
import os
import shutil
import subprocess
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
BUILD = os.path.join(ROOT, "fabric", "build", "trailer")
PREVIEW = "--preview" in sys.argv
FRAMES = os.path.join(BUILD, "preview" if PREVIEW else "frames")
EVENTS = os.path.join(BUILD, "preview.jsonl" if PREVIEW else "events.jsonl")
OUT = os.path.join(BUILD, "zonelock_trailer_preview.mp4" if PREVIEW else "zonelock_trailer.mp4")
FPS = 12 if PREVIEW else 60
SRC_PER_TICK = 3
RATE = 48000
FONTS = "C:/Windows/Fonts/"
ASSETS = os.path.expanduser("~/.gradle/caches/fabric-loom/assets")
AMETHYST = (181, 124, 255)

# Scenes in order; a new set starts at each "cut" (crossfaded), the others follow on without a cut.
CUTS = {"place", "sanctum", "puzzle", "parkour", "maze", "crane", "ending"}
XFADE = 0.36          # seconds
SPLASH = {"ending": "Fabric & NeoForge!"}
FADE_IN, FADE_OUT = 0.9, 1.4

# (mark, start after it, end: seconds after it (> 0), before the next mark (<= 0, None = 0) or "mark:seconds",
#  kind, text, subtitle)
CAPTIONS = [
    ("opening", 0.9, -0.7, "title", "ZONE LOCK", "Dungeons you have to beat before you can break them"),
    ("place", 0.5, None, "line", "Place an Amethyst Core in your dungeon", None),
    ("wand", 0.4, -0.3, "line", "Shape its zone with the wand: each click moves a wall", None),
    ("arm", 0.3, -0.2, "line", "Seal it with an amethyst shard: the zone locks", None),
    ("deny", 0.4, -0.3, "line", "Inside, no block can be broken or placed", None),
    ("sanctum", 0.8, "unlock:-0.3", "line", "To switch the core off, first put out all its seals", None),
    ("unlock", 0.4, -0.2, "line", "Then the core lets go...", None),
    ("yours", 0.3, -0.4, "line", "...and the dungeon is yours to change", None),
    ("puzzle", 0.4, -0.3, "line", "Puzzles that can't be skipped: no bridging the lava", None),
    ("parkour", 0.4, -0.3, "line", "Parkour with no shortcuts", None),
    ("maze", 0.4, None, "line", "Mazes you can't cut through", None),
    ("crane", 0.8, "ending:-0.5", "line", "Hold the wand to see every locked zone", None),
    ("ending", 1.0, -0.2, "title", "ZONE LOCK", "for Minecraft 26.3"),
]


# ---------------------------------------------------------------------------------------------- edit list

def load_events():
    marks, sounds = [], []
    with open(EVENTS, encoding="utf8") as f:
        for line in f:
            e = json.loads(line)
            (marks if "mark" in e else sounds).append(e)
    return marks, sounds


def edit_list(marks):
    """Output timeline: a list of (src_a, src_b, weight_b) per output frame; src_b None when not blending."""
    step = 1
    # Source frame index per mark (preview frames are one per 5 ticks).
    def src(m):
        return m["frame"] if not PREVIEW else m["tick"] // 5
    names = [m["mark"] for m in marks]
    pos = [src(m) for m in marks]
    xf = int(round(XFADE * FPS))
    # Pieces: from each cut mark to the next cut mark (or the end).
    pieces = []
    start = pos[0]
    for i in range(1, len(marks)):
        if names[i] in CUTS or names[i] == "end":
            pieces.append((start, pos[i]))
            start = pos[i]
    out = []
    mapping = {}
    for i, (a, b) in enumerate(pieces):
        head = xf if i > 0 else 0
        tail = xf if i < len(pieces) - 1 else 0
        frames = list(range(a + head, b - tail, step))
        if i > 0:
            pa, pb = pieces[i - 1]
            # Crossfade: the previous piece's last xf frames over this piece's first xf frames.
            prev_tail = list(range(pb - xf, pb))
            this_head = list(range(a, a + xf))
            for k in range(xf):
                w = 0.5 - 0.5 * math.cos(math.pi * (k + 0.5) / xf)
                mapping.setdefault(prev_tail[k], len(out))
                mapping.setdefault(this_head[k], len(out))
                out.append((prev_tail[k], this_head[k], w))
        for f in frames:
            mapping[f] = len(out)
            out.append((f, None, 0.0))
    # Marks on the output timeline (where the source frame of the mark ended up).
    marks_out = {}
    for n, p in zip(names, pos):
        best = None
        for d in range(0, 400):
            if p + d in mapping:
                best = mapping[p + d]
                break
        marks_out[n] = best if best is not None else len(out) - 1
    return out, mapping, marks_out


# ---------------------------------------------------------------------------------------------- captions

def font(name, size):
    return ImageFont.truetype(FONTS + name, size)


def spring(t, zeta=0.62, omega=10.5):
    """Step response of a damped spring: 0 at t<=0, overshoots a little, settles at 1."""
    if t <= 0:
        return 0.0
    wd = omega * math.sqrt(1 - zeta * zeta)
    return 1 - math.exp(-zeta * omega * t) * (math.cos(wd * t) + zeta * omega / wd * math.sin(wd * t))


def smooth(t):
    t = min(max(t, 0.0), 1.0)
    return t * t * (3 - 2 * t)


JAR = os.path.expanduser("~/.gradle/caches/fabric-loom/26.3/minecraft-merged.jar")


def jar_image(path):
    import io
    import zipfile
    with zipfile.ZipFile(JAR) as z:
        return Image.open(io.BytesIO(z.read(path))).convert("RGBA")


class McFont:
    """Minecraft's own bitmap font (textures/font/ascii.png): 8x8 cells, each glyph as wide as its ink + 1."""

    def __init__(s):
        sheet = jar_image("assets/minecraft/textures/font/ascii.png")
        a = np.array(sheet)[:, :, 3] > 0
        s.glyphs, s.advance = {}, {}
        for code in range(32, 127):
            cx, cy = (code % 16) * 8, (code // 16) * 8
            cell = a[cy:cy + 8, cx:cx + 8]
            cols = np.where(cell.any(axis=0))[0]
            width = int(cols.max()) + 1 if len(cols) else 0
            s.glyphs[chr(code)] = cell[:, :max(width, 1)]
            s.advance[chr(code)] = width + 1 if code != 32 else 4

    def width(s, text, bold=False):
        return sum(s.advance.get(c, 6) + (1 if bold and c != " " else 0) for c in text) - 1

    def mask(s, text, bold=False):
        """The text as a boolean array, one cell per font pixel (8 tall)."""
        out = np.zeros((8, s.width(text, bold) + 2), dtype=bool)
        x = 0
        for c in text:
            g = s.glyphs.get(c)
            if g is not None and c != " ":
                out[:, x:x + g.shape[1]] |= g
                if bold:
                    out[:, x + 1:x + 1 + g.shape[1]] |= g
            x += s.advance.get(c, 6) + (1 if bold and c != " " else 0)
        return out


MC = None


def mc_text(text, scale, color=(255, 255, 255), shadow=True, bold=False):
    """Text in the game's font, `scale` screen pixels per font pixel, with the game's drop shadow."""
    m = MC.mask(text, bold)
    h, w = m.shape
    img = np.zeros((h + 1, w + 1, 4), dtype=np.uint8)
    if shadow:
        sh = tuple(int(c * 0.25) for c in color)
        img[1:, 1:][m] = sh + (255,)
    img[:h, :w][m] = tuple(color) + (255,)
    out = Image.fromarray(img, "RGBA")
    return out.resize((out.width * scale, out.height * scale), Image.NEAREST)


def tooltip_panel(text, scale):
    """The text in a frame like the game's tooltips: near-black purple fill, a purple border fading down."""
    tw = MC.width(text)
    pw, ph = tw + 8, 8 + 8
    img = np.zeros((ph, pw, 4), dtype=np.uint8)
    img[1:-1, :] = (16, 0, 16, 240)
    img[:, 1:-1] = (16, 0, 16, 240)
    for y in range(1, ph - 1):
        k = (y - 1) / max(1, ph - 3)
        c = (int(80 + (40 - 80) * k), 0, int(255 + (127 - 255) * k), 255)
        img[y, 1] = c
        img[y, pw - 2] = c
    img[1, 1:pw - 1] = (80, 0, 255, 255)
    img[ph - 2, 1:pw - 1] = (40, 0, 127, 255)
    panel = Image.fromarray(img, "RGBA").resize((pw * scale, ph * scale), Image.NEAREST)
    words = mc_text(text, scale)
    panel.alpha_composite(words, (4 * scale, 4 * scale))
    return panel


class Caption:
    """A line of the story in a tooltip frame, bottom left above the hotbar; it rises in on a soft spring."""

    def __init__(s, kind, text, sub, t0, t1, W, H):
        s.t0, s.t1, s.W, s.H = t0, t1, W, H
        s.scale = max(2, round(6 * H / 1440))
        s.panel = tooltip_panel(text, s.scale)

    def active(s, t):
        return s.t0 - 0.05 <= t <= s.t1 + 0.6

    def draw(s, frame, t):
        local = t - s.t0
        leave = smooth((t - s.t1) / 0.4)
        if leave >= 1 or local < 0:
            return
        k = spring(local, 0.6, 11)
        alpha = smooth(local / 0.25) * (1 - leave)
        x = int(120 * s.H / 1440)
        y = int(s.H - 300 * s.H / 1440 - s.panel.height + (1 - k) * 8 * s.scale + leave * 3 * s.scale)
        _blit(frame, s.panel, x, y, alpha)


def _star(size, color):
    """A four-pointed twinkle, pixel art."""
    m = np.zeros((size, size, 4), dtype=np.uint8)
    c = size // 2
    m[c, :] = color + (255,)
    m[:, c] = color + (255,)
    m[c, c] = (255, 255, 255, 255)
    if size >= 5:
        m[c, 0] = m[c, -1] = m[0, c] = m[-1, c] = color + (140,)
    return Image.fromarray(m, "RGBA")


class TitleCard:
    """
    The logo, like the game's own: blocky letters in its font, bold, faced with amethyst, a deep extrusion and a dark
    outline; amethyst clusters round it and twinkles. A line under it, and the yellow tilted splash of the title
    screen if given.
    """

    def __init__(s, kind, text, sub, t0, t1, W, H, splash=None):
        s.t0, s.t1, s.W, s.H = t0, t1, W, H
        s.px = max(4, round(19 * H / 1440))          # screen pixels per logo pixel
        s.sub = mc_text(sub, max(2, round(5 * H / 1440))) if sub else None
        s.splash = mc_text(splash, max(2, round(5 * H / 1440)), (255, 255, 85)) if splash else None
        tex = np.array(jar_image("assets/minecraft/textures/block/amethyst_block.png"))[:, :, :3].astype(np.float32)
        s.letters = []
        x = 0
        for c in text:
            if c == " ":
                x += 5
                continue
            s.letters.append((x, s._letter(c, tex, x)))
            x += MC.advance[c] + 1
        s.width = x * s.px
        crystals = ["amethyst_cluster", "large_amethyst_bud", "medium_amethyst_bud", "small_amethyst_bud"]
        s.sprites = {n: jar_image(f"assets/minecraft/textures/block/{n}.png").crop((0, 0, 16, 16)) for n in crystals}
        # Where the crystals sit round the logo (logo pixels; size in logo pixels), and when they pop in.
        w = x
        s.crystals = []          # the clusters round the logo looked cut off: the letters alone read better
        s.twinkles = [(-3, -3, 0.0), (w + 2, -2, 0.6), (w * 0.6, 12, 1.3), (w * 0.2, 13, 0.9), (w + 4, 9, 1.8),
                      (w * 0.75, -4, 2.2)]

    def _letter(s, c, tex, offset):
        g = MC.mask(c, bold=True)
        g = g[:, :g.any(axis=0).nonzero()[0].max() + 1]
        h, w = g.shape
        depth = 3
        H2, W2 = h + depth + 2, w + 2
        img = np.zeros((H2, W2, 4), dtype=np.uint8)
        face = np.zeros((H2, W2), dtype=bool)
        face[1:1 + h, 1:1 + w] = g
        body = face.copy()
        for d in range(1, depth + 1):
            body[1 + d:1 + d + h, 1:1 + w] |= g
        outline = body.copy()
        for dy, dx in ((-1, 0), (1, 0), (0, -1), (0, 1)):
            outline |= np.roll(np.roll(body, dy, 0), dx, 1)
        img[outline] = (27, 13, 46, 255)
        # The extrusion: darker purples going down.
        for d in range(depth, 0, -1):
            m = np.zeros_like(face)
            m[1 + d:1 + d + h, 1:1 + w] = g
            shade = (72 - d * 10, 40 - d * 6, 120 - d * 14)
            img[m & ~face] = shade + (255,)
        # The face: the amethyst block texture, one texel per logo pixel, brightened; a light top edge.
        ys, xs = np.nonzero(face)
        for y, x in zip(ys, xs):
            t = tex[(y - 1) % 16, (x - 1 + offset) % 16]
            light = 1.18 - 0.05 * (y - 1)
            col = np.clip(t * light + 18, 0, 255).astype(np.uint8)
            img[y, x] = tuple(col) + (255,)
            if not face[y - 1, x]:
                img[y, x] = (236, 214, 255, 255)
        im = Image.fromarray(img, "RGBA")
        return im.resize((W2 * s.px, H2 * s.px), Image.NEAREST)

    def active(s, t):
        return s.t0 - 0.05 <= t <= s.t1 + 0.8

    def draw(s, frame, t):
        local = t - s.t0
        leave = smooth((t - s.t1) / 0.6)
        if leave >= 1 or local < 0:
            return
        px = s.px
        left = int(s.W * 0.075)
        top = int(s.H * 0.30)
        wash = smooth(local / 0.8) * (1 - leave)
        _wash(frame, 0, int(s.H * 0.12), int(s.W * 0.62), int(s.H * 0.62), 0.42 * wash, radial=True)
        # The letters drop in one after another and settle with a little bounce.
        for i, (x, img) in enumerate(s.letters):
            lt = local - 0.07 * i
            k = spring(lt, 0.55, 9.5)
            a = smooth(lt / 0.25) * (1 - leave)
            _blit(frame, img, left + x * px - px, int(top - px - (1 - k) * 9 * px), a)
        # Crystals pop up round it.
        for name, cx, cy, size, delay in s.crystals:
            lt = local - delay
            k = spring(lt, 0.5, 12)
            if k <= 0.02:
                continue
            sz = max(1, int(size * px * min(k, 1.25)))
            spr = s.sprites[name].resize((sz, sz), Image.NEAREST)
            bx = left + int(cx * px) + (size * px - sz) // 2
            by = top + int(cy * px) + (size * px - sz)
            _blit(frame, spr, bx, by, smooth(lt / 0.2) * (1 - leave))
        # Twinkles: each blinks in its own time.
        for tx, ty, phase in s.twinkles:
            lt = local - 0.9
            if lt < 0:
                continue
            p = (lt * 0.8 + phase) % 2.4
            a = max(0.0, math.sin(min(p, 1.2) / 1.2 * math.pi)) * (1 - leave)
            if a <= 0.02:
                continue
            size = 5 if p % 1.2 < 0.6 else 3
            star = _star(size, (225, 200, 255)).resize((size * px // 2, size * px // 2), Image.NEAREST)
            _blit(frame, star, left + int(tx * px), top + int(ty * px), a)
        logo_bottom = top + 13 * px
        if s.sub:
            lt = local - 0.8
            k = spring(lt, 0.65, 9)
            _blit(frame, s.sub, left + px, int(logo_bottom + 2.2 * px + (1 - k) * 2 * px),
                  smooth(lt / 0.35) * (1 - leave))
        if s.splash:
            lt = local - 1.1
            k = spring(lt, 0.5, 10)
            if k > 0.02:
                pulse = 1 + 0.06 * math.sin(lt * 2 * math.pi * 1.1) ** 2 if lt > 0.5 else 1.0
                sc = max(0.01, k * pulse)
                spl = s.splash.resize((max(1, int(s.splash.width * sc)), max(1, int(s.splash.height * sc))),
                                      Image.NEAREST).rotate(18, expand=True, resample=Image.NEAREST)
                cx = left + s.width - int(0.05 * s.width)
                cy = top + 12 * px
                _blit(frame, spl, cx - spl.width // 2, cy - spl.height // 2, smooth(lt / 0.2) * (1 - leave))


def _blit(frame, img, x, y, alpha):
    if alpha <= 0.003:
        return
    if alpha < 0.999:
        img = img.copy()
        a = img.getchannel("A").point(lambda v: int(v * alpha))
        img.putalpha(a)
    frame.alpha_composite(img, (max(x, 0), max(y, 0)))


_WASH = {}


def _wash(frame, x, y, w, h, strength, radial=False):
    if strength <= 0.003:
        return
    key = (w, h, radial)
    if key not in _WASH:
        yy, xx = np.mgrid[0:h, 0:w]
        if radial:
            d = np.sqrt(((xx - w / 2) / (w * 0.42)) ** 2 + ((yy - h / 2) / (h * 0.5)) ** 2)
        else:
            d = np.sqrt(((xx - w * 0.35) / (w * 0.6)) ** 2 + ((yy - h * 0.55) / (h * 0.55)) ** 2)
        a = np.clip(1 - d, 0, 1) ** 1.6
        _WASH[key] = a
    a = (_WASH[key] * 255 * strength).astype(np.uint8)
    img = Image.new("RGBA", (w, h), (8, 4, 14, 0))
    img.putalpha(Image.fromarray(a))
    frame.alpha_composite(img, (max(x, 0), max(y, 0)))


# ---------------------------------------------------------------------------------------------- sound

_DECODED = {}
_INDEX = None


def sound_file(path):
    global _INDEX
    if _INDEX is None:
        names = [n for n in os.listdir(os.path.join(ASSETS, "indexes")) if n.startswith("26.3")]
        with open(os.path.join(ASSETS, "indexes", names[0]), encoding="utf8") as f:
            _INDEX = json.load(f)["objects"]
    ns, p = path.split(":", 1)
    key = f"{ns}/{p}"
    if key in _INDEX:
        h = _INDEX[key]["hash"]
        return os.path.join(ASSETS, "objects", h[:2], h)
    local = os.path.join(ROOT, "common", "src", "main", "resources", "assets", ns, p)
    return local if os.path.exists(local) else None


def decode(path):
    if path not in _DECODED:
        raw = subprocess.run(["ffmpeg", "-v", "error", "-i", path, "-f", "f32le", "-ac", "1", "-ar", str(RATE), "-"],
                             capture_output=True, check=True).stdout
        _DECODED[path] = np.frombuffer(raw, dtype=np.float32).copy()
    return _DECODED[path]


def sfx(sounds, mapping, length):
    out = np.zeros((length, 2), dtype=np.float32)
    skipped = 0
    for e in sounds:
        ev = e["event"]
        if ".ambient" in ev or "trial_spawner" in ev or "step" in ev:
            continue
        src = e["frame"] if not PREVIEW else e["frame"] // 5
        if src not in mapping:
            skipped += 1
            continue
        start = int(mapping[src] / FPS * RATE)
        f = sound_file(e["sound"])
        if f is None:
            print("no file for", e["sound"])
            continue
        data = decode(f)
        pitch = max(0.5, min(2.0, e["pitch"]))
        if abs(pitch - 1) > 0.01:
            n = int(len(data) / pitch)
            data = np.interp(np.arange(n) * pitch, np.arange(len(data)), data).astype(np.float32)
        vol = min(e["volume"], 1.0)
        dx, dy, dz = e["x"] - e["cx"], e["y"] - e["cy"], e["z"] - e["cz"]
        dist = math.sqrt(dx * dx + dy * dy + dz * dz)
        if not e["relative"]:
            vol *= max(0.0, 1 - dist / max(e["range"], 1))
        if vol <= 0.002:
            continue
        pan = 0.0
        if not e["relative"] and dist > 0.5:
            yaw = math.radians(e["yaw"])
            right = (-math.cos(yaw), 0, -math.sin(yaw))
            pan = (dx * right[0] + dz * right[2]) / dist * 0.7
        left, rightg = math.cos((pan + 1) * math.pi / 4), math.sin((pan + 1) * math.pi / 4)
        end = min(length, start + len(data))
        if end <= start:
            continue
        seg = data[: end - start] * vol
        out[start:end, 0] += seg * left
        out[start:end, 1] += seg * rightg
    print(f"sfx mixed, {skipped} sounds fell in cut frames")
    return out


def music(length, marks_out):
    """A slow pad (Bm G D A) with a soft bass, amethyst chimes on top, a long hall of reverb."""
    t = np.arange(length) / RATE
    out = np.zeros((length, 2), dtype=np.float64)
    bar = 60 / 64 * 4
    prog = [(47, [59, 62, 66, 71]), (43, [59, 62, 67, 71]), (50, [57, 62, 66, 69]), (45, [57, 61, 64, 69])]

    def hz(n):
        return 440 * 2 ** ((n - 69) / 12)

    def env(n, start, dur, a, r):
        x = (np.arange(n) / RATE)
        e = np.clip(x / a, 0, 1) * np.clip((start + dur + r - (start + x)) / r, 0, 1)
        return e

    sec = lambda name: marks_out.get(name, 0) / FPS
    build = sec("sanctum")
    montage = sec("puzzle")
    end_t = length / RATE
    bars = int(end_t / bar) + 2
    rng = np.random.default_rng(3)
    for b in range(bars):
        start = b * bar
        if start > end_t:
            break
        root, notes = prog[b % 4]
        last = start + bar > end_t - 4
        if last:
            root, notes = 50, [57, 62, 66, 69, 74]
        dur = bar + (4 if last else 0)
        i0 = int(start * RATE)
        n = min(length - i0, int((dur + 2.5) * RATE))
        if n <= 0:
            continue
        x = np.arange(n) / RATE
        e = env(n, 0, dur, 1.6, 2.2)
        lift = 0.75 + 0.25 * min(1, max(0, (start - build) / 6)) if start > build else 0.75
        for k, note in enumerate(notes):
            f0 = hz(note)
            for det in (-0.004, 0.004):
                ph = 2 * np.pi * f0 * (1 + det) * x + 0.3 * np.sin(2 * np.pi * 0.21 * x + k)
                v = sum(np.sin(ph * h) / h * math.exp(-h / 2.2) for h in range(1, 6))
                pan = (k / max(1, len(notes) - 1) - 0.5) * 0.8 + det * 50
                out[i0:i0 + n, 0] += v * e * 0.05 * lift * (1 - pan * 0.5)
                out[i0:i0 + n, 1] += v * e * 0.05 * lift * (1 + pan * 0.5)
        if start >= build - 0.5:
            vb = np.sin(2 * np.pi * hz(root - 12) * x) + 0.3 * np.sin(4 * np.pi * hz(root - 12) * x)
            out[i0:i0 + n, :] += (vb * env(n, 0, dur, 0.8, 1.8) * 0.07)[:, None]
        # Chimes: a few notes of the chord up high, more in the montage.
        count = 2 if start < build else (3 if start < montage else 6)
        for c in range(count):
            ct = start + bar * (c / count) + (rng.random() * 0.05)
            note = notes[rng.integers(len(notes))] + 24
            ci = int(ct * RATE)
            m = min(length - ci, int(3.2 * RATE))
            if m <= 0:
                continue
            y = np.arange(m) / RATE
            f0 = hz(note)
            bell = (np.sin(2 * np.pi * f0 * y) + 0.35 * np.sin(2 * np.pi * f0 * 2.76 * y) * np.exp(-y * 3)
                    + 0.15 * np.sin(2 * np.pi * f0 * 5.4 * y) * np.exp(-y * 6)) * np.exp(-y * 1.5)
            bell *= np.clip(y / 0.004, 0, 1)
            pan = rng.random() * 1.2 - 0.6
            g = 0.035 if start < montage else 0.03
            out[ci:ci + m, 0] += bell * g * (1 - pan * 0.5)
            out[ci:ci + m, 1] += bell * g * (1 + pan * 0.5)
    # Reverb: convolve with a long, dark stereo tail.
    ir_len = int(3.8 * RATE)
    tt = np.arange(ir_len) / RATE
    wet = np.zeros_like(out)
    for ch in range(2):
        noise = rng.standard_normal(ir_len) * np.exp(-tt * 1.6)
        # Darken the tail: a running mean as a gentle lowpass.
        k = 9
        noise = np.convolve(noise, np.ones(k) / k, mode="same")
        size = 1 << int(math.ceil(math.log2(length + ir_len)))
        wet[:, ch] = np.fft.irfft(np.fft.rfft(out[:, ch], size) * np.fft.rfft(noise, size), size)[:length]
    wet /= np.max(np.abs(wet)) + 1e-9
    dry_peak = np.max(np.abs(out)) + 1e-9
    mix = out / dry_peak * 0.55 + wet * 0.6
    # Fade in and out with the picture.
    fade = np.clip(t / 2.0, 0, 1) * np.clip((t[-1] - t) / 2.5, 0, 1)
    return (mix * fade[:, None]).astype(np.float32)


# ---------------------------------------------------------------------------------------------- main

def main():
    global MC
    MC = McFont()
    marks, sounds = load_events()
    timeline, mapping, marks_out = edit_list(marks)
    n = len(timeline)
    first = Image.open(os.path.join(FRAMES, _name(timeline[0][0])))
    W, H = first.size
    print(f"{n} output frames ({n / FPS:.1f}s) at {W}x{H}, marks {marks_out}")

    names = [m["mark"] for m in marks]
    caps = []
    for (mark, a, b, kind, text, sub) in CAPTIONS:
        if mark not in marks_out:
            continue
        t0 = marks_out[mark] / FPS + a
        nxt = None
        i = names.index(mark)
        if i + 1 < len(names):
            nxt = marks_out[names[i + 1]] / FPS
        if isinstance(b, str):
            other, off = b.split(":")
            t1 = marks_out[other] / FPS + float(off)
        else:
            t1 = (marks_out[mark] / FPS + b) if (b is not None and b > 0) else ((nxt if nxt else n / FPS) + (b or 0))
        if kind == "title":
            caps.append(TitleCard(kind, text, sub, t0, t1, W, H, SPLASH.get(mark)))
        else:
            caps.append(Caption(kind, text, sub, t0, t1, W, H))

    # Sound first (quick), then the picture straight into ffmpeg.
    length = int(n / FPS * RATE) + RATE
    # The music stays under the game: the sounds of what happens lead.
    mix = music(length, marks_out) * 0.26 + sfx(sounds, mapping, length) * 1.0
    peak = np.max(np.abs(mix)) + 1e-9
    mix = np.tanh(mix / peak * 1.1) / math.tanh(1.1) * 0.89
    wav = os.path.join(BUILD, "trailer_audio.f32")
    mix.astype(np.float32).tofile(wav)

    ffmpeg = ["ffmpeg", "-y", "-v", "error", "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", f"{W}x{H}", "-r", str(FPS),
              "-i", "-", "-f", "f32le", "-ar", str(RATE), "-ac", "2", "-i", wav,
              "-c:v", "libx264", "-preset", "slow", "-crf", "19", "-pix_fmt", "yuv420p", "-profile:v", "high",
              "-c:a", "aac", "-b:a", "256k", "-shortest", "-movflags", "+faststart", OUT]
    proc = subprocess.Popen(ffmpeg, stdin=subprocess.PIPE)
    cache = {}

    def get(i):
        if i not in cache:
            if len(cache) > 8:
                cache.clear()
            cache[i] = Image.open(os.path.join(FRAMES, _name(i))).convert("RGB")
        return cache[i]

    for k, (a, b, w) in enumerate(timeline):
        img = get(a)
        if b is not None:
            img = Image.blend(img, get(b), w)
        frame = img.convert("RGBA")
        t = k / FPS
        for c in caps:
            if c.active(t):
                c.draw(frame, t)
        fade = min(1.0, smooth(t / FADE_IN), smooth((n / FPS - t) / FADE_OUT))
        rgb = frame.convert("RGB")
        if fade < 0.999:
            rgb = Image.eval(rgb, lambda v, f=fade: int(v * f))
        proc.stdin.write(rgb.tobytes())
        if k % 300 == 0:
            print(f"  frame {k}/{n}")
    proc.stdin.close()
    proc.wait()
    print("wrote", OUT)


def _name(i):
    return (f"p{i * 5:05d}.png" if PREVIEW else f"f{i:05d}.png")


if __name__ == "__main__":
    if shutil.which("ffmpeg") is None:
        sys.exit("ffmpeg not found")
    main()
