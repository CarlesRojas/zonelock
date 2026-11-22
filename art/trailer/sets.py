"""Sets for the trailer (art/trailer/build/*.nbt + marks.json), placed by TrailerTest.

    python -X utf8 art/trailer/sets.py          # builds every set and prints a top view of each

Each set is an enclosed, lit room (or a garden) with only vanilla blocks; the mod's blocks (cores, seals) are
put in by the test at the positions written to marks.json, so the zones are set up the same way a player would.
Coordinates in marks.json are template-relative.

  hall      a deepslate crypt hall: an empty dais in the middle for the core the player places, loculi in the
            side bays, a backlit amethyst window at the far end.
  sanctum   a blackstone and gold shrine: the core on its altar, a seal on a plinth either side, a lever by
            the redstone seal. Everything in one view.
  cistern   a flooded aqueduct: the broken piers of a collapsed walkway are the parkour.
  bridge    a nether-brick forge cut by a lava chasm: a button raises a path of stones out of the lava (sticky
            pistons under it) towards the core on the far bank, while the lamps along the walls light up.
  maze      a hedge maze with a garden wall round it and the core on a plinth in the middle.
"""
import json
import math
import os
import random
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "structures", "amethyst_tower"))
import nbtio  # noqa: E402

OUT = os.path.join(HERE, "build")


# ---------------------------------------------------------------------------------------------- texture

def _hash(x, y, z, s):
    h = (x * 374761393 + y * 668265263 + z * 1440662683 + s * 1274126177) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1103515245) & 0xFFFFFFFF
    return ((h ^ (h >> 16)) & 0xFFFFFF) / 0xFFFFFF


def _smooth(t):
    return t * t * (3 - 2 * t)


def noise(x, y, z, scale, s):
    fx, fy, fz = x / scale, y / scale, z / scale
    ix, iy, iz = math.floor(fx), math.floor(fy), math.floor(fz)
    tx, ty, tz = _smooth(fx - ix), _smooth(fy - iy), _smooth(fz - iz)
    out = 0.0
    for dx in (0, 1):
        for dy in (0, 1):
            for dz in (0, 1):
                w = (tx if dx else 1 - tx) * (ty if dy else 1 - ty) * (tz if dz else 1 - tz)
                out += w * _hash(ix + dx, iy + dy, iz + dz, s)
    return out


# Quantiles of the blob noise, so a fraction asks for that share of the blocks.
_SAMPLE = sorted(0.7 * noise(i * 1.37, i * 0.71, i * 2.13, 3.0, 5) + 0.3 * noise(i * 0.91, i * 1.7, i * 0.3, 1.4, 9)
                 for i in range(6000))


def _threshold(frac):
    frac = min(max(frac, 0.0), 1.0)
    return _SAMPLE[min(len(_SAMPLE) - 1, int((1 - frac) * len(_SAMPLE)))]


def blob(x, y, z, s):
    return 0.7 * noise(x, y, z, 3.0, s) + 0.3 * noise(x, y, z, 1.4, s + 101)


def tex(primary, *variants):
    """A block function: `primary`, with clusters of each (block, fraction) variant grown from blob noise.
    A fraction can be a function of y for gradients."""
    def f(x, y, z):
        for i, (block, frac) in enumerate(variants):
            fr = frac(y) if callable(frac) else frac
            if blob(x, y, z, 17 + i * 31) > _threshold(fr):
                return block
        return primary
    return f


# ---------------------------------------------------------------------------------------------- building

class Set(nbtio.Build):
    """A template with an interior origin: room coordinates (0, 0, 0) is the first floor block."""

    def __init__(s, name, w, h, d, origin):
        super().__init__(w, h, d)
        s.name = name
        s.o = origin
        s.marks = {}

    def at(s, x, y, z, b, nbt=None):
        ox, oy, oz = s.o
        s.set(x + ox, y + oy, z + oz, b, nbt)

    def get_at(s, x, y, z):
        ox, oy, oz = s.o
        return s.get(x + ox, y + oy, z + oz)

    def fill(s, x1, y1, z1, x2, y2, z2, b):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    s.at(x, y, z, b)

    def mark(s, key, x, y, z):
        ox, oy, oz = s.o
        s.marks[key] = [x + ox, y + oy, z + oz]

    def finish(s):
        os.makedirs(OUT, exist_ok=True)
        # Every unset cell inside the bounds is air: the sets carve their space wherever they are placed.
        for x in range(s.w):
            for y in range(s.h):
                for z in range(s.d):
                    if (x, y, z) not in s.blocks:
                        s.blocks[(x, y, z)] = "minecraft:air"
        s.save(os.path.join(OUT, s.name + ".nbt"))
        return s

    def top_view(s, y):
        """ASCII of one template layer (y template-relative), north up."""
        keys = {}
        rows = []
        for z in range(s.d):
            row = ""
            for x in range(s.w):
                b = s.get(x, y, z) or "minecraft:air"
                n = nbtio.bid(b).split(":")[1]
                if n == "air" or n == "light":
                    row += "."
                    continue
                if n not in keys:
                    keys[n] = "#ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@$%&*+=?"[len(keys) % 70]
                row += keys[n]
            rows.append(row)
        return "\n".join(rows) + "\n  " + "  ".join(f"{v}={k}" for k, v in keys.items())


def stair(mat, facing, half="bottom", shape="straight"):
    return f"{mat}_stairs[facing={facing},half={half},shape={shape},waterlogged=false]"


def slab(mat, kind="bottom"):
    return f"{mat}_slab[type={kind},waterlogged=false]"


def lantern(kind="lantern", hanging=True):
    return f"{kind}[hanging={'true' if hanging else 'false'},waterlogged=false]"


def chain(axis="y"):
    return f"iron_chain[axis={axis},waterlogged=false]"


def candles(n, color="", lit=True):
    c = (color + "_candle") if color else "candle"
    return f"{c}[candles={n},lit={'true' if lit else 'false'},waterlogged=false]"


def wall_block(mat, **sides):
    st = ",".join(f"{d}={sides.get(d, 'none')}" for d in ("north", "east", "south", "west"))
    return f"{mat}_wall[{st},up=true,waterlogged=false]"


def light(level):
    return f"light[level={level},waterlogged=false]"


OPP = {"north": "south", "south": "north", "east": "west", "west": "east"}


# ---------------------------------------------------------------------------------------------- the crypt hall

def hall():
    W, D, H = 13, 19, 6             # interior x 0..12, z 0..18, air y 1..6, vault above
    s = Set("hall", W + 4, H + 7, D + 4 + 4, (2, 1, 2))
    wallf = tex("deepslate_bricks", ("cracked_deepslate_bricks", lambda y: 0.22 - 0.03 * y),
                ("deepslate_tiles", 0.14), ("cobbled_deepslate", lambda y: max(0.0, 0.16 - 0.06 * y)))
    vaultf = tex("deepslate_bricks", ("deepslate_tiles", 0.25), ("cracked_deepslate_bricks", 0.12))
    floorf = tex("deepslate_tiles", ("cracked_deepslate_tiles", 0.2), ("cobbled_deepslate", 0.08))
    rib = "polished_blackstone_bricks"

    # Shell: two blocks thick, floor, and a solid block above the vault.
    s.fill(-2, -1, -2, W + 1, H + 5, D + 1, wallf)
    s.fill(0, 1, 0, W - 1, H + 3, D - 1, "air")

    # Floor: dark border, tiled field, a runner from the door to the dais, an amethyst ring round the dais.
    for x in range(W):
        for z in range(D):
            border = x in (0, W - 1) or z in (0, D - 1)
            s.at(x, 0, z, "polished_deepslate" if border else floorf)
    for z in range(12, D):
        for x in (5, 6, 7):
            s.at(x, 0, z, "polished_blackstone_bricks" if x != 6 else "polished_blackstone")
    cx, cz = 6, 9
    for x in range(cx - 2, cx + 3):
        for z in range(cz - 2, cz + 3):
            ring = abs(x - cx) == 2 or abs(z - cz) == 2
            s.at(x, 0, z, "amethyst_block" if ring else "polished_deepslate")

    # The dais: the core goes on the chiseled block in the middle.
    s.at(cx, 1, cz, "chiseled_deepslate")
    s.at(cx, 1, cz - 1, stair("polished_deepslate", "south"))
    s.at(cx, 1, cz + 1, stair("polished_deepslate", "north"))
    s.at(cx - 1, 1, cz, stair("polished_deepslate", "east"))
    s.at(cx + 1, 1, cz, stair("polished_deepslate", "west"))
    for dx, dz in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
        s.at(cx + dx, 1, cz + dz, slab("polished_deepslate"))
    for dx, dz in ((-2, -2), (2, -2), (-2, 2), (2, 2)):
        s.at(cx + dx, 1, cz + dz, wall_block("polished_deepslate"))
        s.at(cx + dx, 2, cz + dz, candles(3, "purple"))
    s.mark("dais", cx, 1, cz)

    # Engaged pillars on both long walls, a loculus niche in each bay between them.
    pillars = [1, 5, 9, 13, 17]
    for side, px, wx in (("west", 0, -1), ("east", W - 1, W)):
        inward = "east" if side == "west" else "west"
        for pz in pillars:
            s.at(px, 1, pz, "polished_deepslate")
            for y in range(2, H):
                s.at(px, y, pz, "deepslate_tiles" if y % 2 else "polished_deepslate")
            s.at(px, H, pz, "chiseled_deepslate")
            s.at(px, H, pz - 1, stair("deepslate_tile", "south", "top"))
            s.at(px, H, pz + 1, stair("deepslate_tile", "north", "top"))
            s.at(px + (1 if side == "west" else -1), H, pz, stair("deepslate_tile", side, "top"))
            # A soul torch on the face of every other pillar.
            if pz in (5, 13):
                s.at(px + (1 if side == "west" else -1), 4, pz, f"soul_wall_torch[facing={inward}]")
        for bz in (2, 6, 10, 14):
            # Bay: wainscot and frieze, a 3-wide arched niche cut 1 into the wall.
            for z in range(bz, bz + 3):
                s.at(px, 1, z, "polished_deepslate")
                s.at(px, H, z, stair("deepslate_brick", side, "top"))
                for y in (2, 3):
                    s.at(px, y, z, "air")
                    s.at(wx, y, z, "air")
                s.at(px, 4, z, "air")
            # Arch head of the niche: stairs at the ends, a top slab in the middle.
            s.at(px, 4, bz, stair("polished_deepslate", "north", "top"))
            s.at(px, 4, bz + 1, slab("polished_deepslate", "top"))
            s.at(px, 4, bz + 2, stair("polished_deepslate", "south", "top"))
            # The loculus: a stone tomb with a slab lid, a skull at its head, candles or a lantern.
            for z in range(bz, bz + 3):
                s.at(wx, 1, z, "polished_deepslate")
                s.at(px, 1, z, "polished_deepslate")
                s.at(wx, 2, z, slab("deepslate_tile"))
            rot = 4 if side == "west" else 12
            s.at(px, 2, bz + (0 if bz % 8 == 2 else 2), f"skeleton_skull[rotation={rot}]")
            if bz in (2, 10):
                s.at(px, 2, bz + 2, candles(2))
            else:
                s.at(px, 2, bz, lantern("soul_lantern", False))
            if bz == 6:
                s.at(wx, 3, bz + 1, "cobweb")

    # Barrel vault over the 13-wide span: stepped upside-down stairs, dark ribs over the pillars.
    def vault(x, y, z, b):
        s.at(x, y, z, rib if z in pillars and "stairs" not in b and "slab" not in b else b)
    for z in range(D):
        for x in range(W):
            vault(x, H + 4, z, vaultf(x, H + 4, z))
        vault(0, H + 1, z, vaultf(0, H + 1, z))
        vault(1, H + 1, z, stair("deepslate_brick", "west", "top"))
        vault(W - 1, H + 1, z, vaultf(W - 1, H + 1, z))
        vault(W - 2, H + 1, z, stair("deepslate_brick", "east", "top"))
        for x in (0, 1, 2):
            vault(x, H + 2, z, vaultf(x, H + 2, z))
            vault(W - 1 - x, H + 2, z, vaultf(W - 1 - x, H + 2, z))
        vault(3, H + 2, z, stair("deepslate_brick", "west", "top"))
        vault(W - 4, H + 2, z, stair("deepslate_brick", "east", "top"))
        for x in range(0, 5):
            vault(x, H + 3, z, vaultf(x, H + 3, z))
            vault(W - 1 - x, H + 3, z, vaultf(W - 1 - x, H + 3, z))
        vault(5, H + 3, z, stair("deepslate_brick", "west", "top"))
        vault(W - 6, H + 3, z, stair("deepslate_brick", "east", "top"))
        vault(6, H + 3, z, slab("deepslate_brick", "top"))
    # Ribs: on the pillar lines the vault stairs are blackstone bricks too.
    for z in pillars:
        for (x, y, f) in ((1, H + 1, "west"), (W - 2, H + 1, "east"), (3, H + 2, "west"), (W - 4, H + 2, "east"),
                          (5, H + 3, "west"), (W - 6, H + 3, "east")):
            s.at(x, y, z, stair("polished_blackstone_brick", f, "top"))
        s.at(6, H + 3, z, slab("polished_blackstone_brick", "top"))
    # Soul lanterns hanging from the crown in the bays.
    for z in (3, 7, 11, 15):
        s.at(6, H + 2, z, chain())
        s.at(6, H + 1, z, lantern("soul_lantern"))

    # North end: a tall arched window of purple glass, lit from behind.
    for y in range(1, H + 1):
        for x in (4, 5, 6, 7, 8):
            s.at(x, y, -1, wallf(x, y, -1))
    for y in range(2, H + 2):
        for x in (5, 6, 7):
            s.at(x, y, -1, "purple_stained_glass_pane[east=true,west=true,north=false,south=false,waterlogged=false]"
                 if not (y == H + 1 and x != 6) else stair("polished_blackstone_brick", "west" if x == 5 else "east", "top"))
        for x in (5, 6, 7):
            s.at(x, y, -2, "pearlescent_froglight[axis=y]")
    for x in (4, 8):
        for y in range(1, H + 2):
            s.at(x, y, 0, "polished_blackstone_bricks" if y > 1 else "polished_blackstone")
        s.at(x, H + 2, 0, stair("polished_blackstone_brick", "west" if x == 4 else "east", "top"))
    s.at(6, 1, -1, "polished_blackstone_bricks")
    for x in (5, 7):
        s.at(x, 1, -1, stair("polished_blackstone_brick", "north"))
    s.at(6, 1, 0, "amethyst_cluster[facing=up,waterlogged=false]")

    # South end: the arched doorway the player came through, a dark passage behind.
    for x in (5, 6, 7):
        for y in (1, 2, 3):
            s.at(x, y, D, "air")
            s.at(x, y, D + 1, "air")
    s.at(5, 4, D, stair("polished_blackstone_brick", "west", "top"))
    s.at(6, 4, D, slab("polished_blackstone_brick", "top"))
    s.at(7, 4, D, stair("polished_blackstone_brick", "east", "top"))
    s.fill(4, 0, D + 2, 8, 5, D + 3, wallf)
    s.fill(5, 1, D + 2, 7, 3, D + 2, "air")
    s.at(5, 3, D + 2, "cobweb")
    s.at(7, 1, D + 2, lantern("soul_lantern", False))
    for x in (4, 8):
        for y in range(1, 5):
            s.at(x, y, D - 1, "polished_blackstone_bricks")

    # A little moss and rubble in the corners (damp, old).
    for (x, z) in ((1, 1), (11, 1), (1, 17), (11, 17), (2, 1), (1, 2)):
        s.at(x, 1, z, "moss_carpet" if (x + z) % 2 else "air")
    s.at(10, 1, 16, slab("cobbled_deepslate"))
    s.at(2, 1, 16, "cobbled_deepslate_wall[north=none,east=none,south=none,west=none,up=true,waterlogged=false]")

    # Hidden fill light so the floor isn't pitch black between the lanterns.
    for x in (2, 6, 10):
        for z in (2, 6, 10, 14, 17):
            if s.get_at(x, 5, z) in (None, "minecraft:air"):
                s.at(x, 5, z, light(12))

    s.mark("door", 6, 1, D - 1)
    return s.finish()


# ---------------------------------------------------------------------------------------------- the sanctum

def sanctum():
    W, D, H = 11, 13, 5
    s = Set("sanctum", W + 4, H + 7, D + 4, (2, 1, 2))
    wallf = tex("polished_blackstone_bricks", ("cracked_polished_blackstone_bricks", 0.2), ("blackstone", 0.1))
    floorf = tex("polished_blackstone", ("blackstone", 0.15))
    s.fill(-2, -1, -2, W + 1, H + 4, D + 1, wallf)
    s.fill(0, 1, 0, W - 1, H + 2, D - 1, "air")
    cx = 5

    # Floor: blackstone border, a gilded runner to the altar.
    for x in range(W):
        for z in range(D):
            s.at(x, 0, z, "polished_blackstone_bricks" if x in (0, W - 1) or z in (0, D - 1) else floorf)
    for z in range(4, D):
        s.at(cx, 0, z, "chiseled_polished_blackstone" if z % 3 == 0 else "gilded_blackstone")

    # Engaged basalt pillars, gilded feet and capitals; candles in the bays.
    for px, side in ((0, "west"), (W - 1, "east")):
        inward = "east" if side == "west" else "west"
        for pz in (1, 5, 9):
            s.at(px, 1, pz, "gilded_blackstone")
            for y in range(2, H):
                s.at(px, y, pz, "polished_basalt[axis=y]")
            s.at(px, H, pz, "chiseled_polished_blackstone")
            s.at(px, H, pz - 1, stair("polished_blackstone_brick", "south", "top"))
            s.at(px, H, pz + 1, stair("polished_blackstone_brick", "north", "top"))
        for bz in (2, 6, 10):
            for z in range(bz, bz + 3):
                if z >= D:
                    continue
                s.at(px, 1, z, "polished_blackstone")
                s.at(px, H, z, stair("polished_blackstone_brick", side, "top"))
            # A niche with a candle cluster in the middle of each bay.
            mz = bz + 1
            if mz < D:
                wx = px - 1 if side == "west" else px + 1
                s.at(px, 3, mz, "air")
                s.at(wx, 3, mz, candles(3))
                s.at(px, 4, mz, slab("polished_blackstone_brick", "top"))
                s.at(px, 2, mz, slab("polished_blackstone", "top"))
    # Flat coffered ceiling: beams over the pillars, raised cells between with a hanging lantern.
    for z in range(D):
        for x in range(W):
            s.at(x, H + 2, z, wallf(x, H + 2, z))
            s.at(x, H + 1, z, "air")
    for pz in (1, 5, 9):
        for x in range(W):
            s.at(x, H + 1, pz, "dark_oak_log[axis=x]")
    for z in (3, 7, 11):
        s.at(cx, H + 1, z, chain())
        s.at(cx, H, z, lantern())

    # The altar at the north end: the core on a chiseled block, stairs round it, a gold reredos behind.
    az = 3
    s.at(cx, 1, az, "chiseled_polished_blackstone")
    s.at(cx, 1, az + 1, stair("polished_blackstone_brick", "north"))
    s.at(cx - 1, 1, az, stair("polished_blackstone_brick", "east"))
    s.at(cx + 1, 1, az, stair("polished_blackstone_brick", "west"))
    s.at(cx, 1, az - 1, "polished_blackstone_bricks")
    s.at(cx - 1, 1, az - 1, slab("polished_blackstone_brick"))
    s.at(cx + 1, 1, az - 1, slab("polished_blackstone_brick"))
    s.mark("core", cx, 2, az)
    for x in range(cx - 2, cx + 3):
        for y in range(1, H + 1):
            s.at(x, y, 0, "gilded_blackstone" if (x + y) % 3 == 0 else "polished_blackstone_bricks")
    for y in range(2, H):
        s.at(cx, y, -1, "air")
        s.at(cx, y, 0, "air")
    s.at(cx, 2, -1, "amethyst_cluster[facing=up,waterlogged=false]")
    s.at(cx, 1, -1, "amethyst_block")
    s.at(cx, H, -1, "gold_block")
    s.at(cx, H, 0, stair("polished_blackstone_brick", "south", "top"))
    for x in (cx - 1, cx + 1):
        for y in range(2, H):
            s.at(x, y, 0, "polished_basalt[axis=y]")
        s.at(x, H, 0, "gold_block")
    for x in (cx - 2, cx + 2):
        s.at(x, 1, 1, "polished_blackstone_bricks")
        s.at(x, 2, 1, candles(4))

    # The seals: one on a plinth either side of the altar, level with the core, so the pulses leave it by opposite
    # faces in plain view. A lever beside the redstone seal.
    sz = az
    for x, key in ((2, "seal_amethyst"), (8, "seal_redstone")):
        s.at(x, 1, sz, "chiseled_polished_blackstone")
        s.mark(key, x, 2, sz)
        for dz in (-1, 1):
            s.at(x, 1, sz + dz, slab("polished_blackstone"))
    s.at(9, 1, sz, "polished_blackstone")
    s.mark("lever", 9, 2, sz)

    # The entrance at the south end.
    for x in (cx - 1, cx, cx + 1):
        for y in (1, 2, 3):
            s.at(x, y, D, "air")
    s.at(cx - 1, 4, D, stair("polished_blackstone_brick", "west", "top"))
    s.at(cx, 4, D, slab("polished_blackstone_brick", "top"))
    s.at(cx + 1, 4, D, stair("polished_blackstone_brick", "east", "top"))
    s.at(cx, 2, D + 1, lantern(hanging=False))
    s.at(cx, 1, D + 1, "polished_blackstone")

    for x in (2, 5, 8):
        for z in (2, 5, 8, 11):
            if s.get_at(x, 4, z) in (None, "minecraft:air"):
                s.at(x, 4, z, light(13))
    return s.finish()


# ---------------------------------------------------------------------------------------------- the cistern

CISTERN_ROUTE = []   # foothold tops (x, y, z) in room coordinates, in jump order


def cistern():
    W, D, H = 9, 23, 13
    s = Set("cistern", W + 4, H + 4, D + 4, (2, 1, 2))
    wallf = tex("stone_bricks", ("mossy_stone_bricks", lambda y: max(0.04, 0.45 - 0.05 * y)),
                ("cracked_stone_bricks", 0.15), ("andesite", 0.08))
    s.fill(-2, -1, -2, W + 1, H + 2, D + 1, wallf)
    s.fill(0, 0, 0, W - 1, H, D - 1, "air")
    # Water three deep over a mossy floor: the fall zone.
    for x in range(W):
        for z in range(D):
            s.at(x, 0, z, "mossy_cobblestone" if blob(x, 0, z, 3) > 0.5 else "gravel")
            for y in (1, 2, 3):
                s.at(x, y, z, "water[level=0]")
    # Start and end ledges (the walkway that stayed up), stairs in the corner out of the water.
    for z in range(D - 3, D):
        for x in range(W):
            for y in range(1, 6):
                s.at(x, y, z, wallf(x, y, z))
    for z in range(0, 3):
        for x in range(W):
            for y in range(1, 8):
                s.at(x, y, z, wallf(x, y, z))
    for x in range(W):
        s.at(x, 6, D - 3, slab("stone_brick"))
        s.at(x, 8, 2, slab("stone_brick"))
    # Return route: a stair cut in the east wall from the water up to the start ledge.
    for i, z in enumerate(range(D - 4, D - 9, -1)):
        pass
    # The piers of the collapsed walkway: broken tops at different heights, an arch stub on some.
    piers = [(4, 5, D - 6), (2, 6, D - 9), (5, 6, D - 11), (6, 7, D - 14), (3, 7, D - 16), (4, 8, 4)]
    for (x, top, z) in piers:
        for y in range(1, top + 1):
            s.at(x, y, z, wallf(x, y, z) if y < top else "chiseled_stone_bricks")
        CISTERN_ROUTE.append((x, top, z))
    # Arch stubs springing from two piers (what is left of the walkway).
    s.at(4, 5, D - 7, stair("stone_brick", "north", "top"))
    s.at(5, 6, D - 12, stair("stone_brick", "south", "top"))
    s.at(6, 7, D - 15, stair("stone_brick", "south", "top"))
    # A fallen column lying in the water.
    for x in range(1, 5):
        s.at(x, 3, D - 13, "stone_brick_wall[north=none,east=low,south=none,west=low,up=false,waterlogged=true]")
    # Beam ends and corbels along the walls, chains and lanterns from the vault.
    for z in range(3, D - 3, 4):
        s.at(0, H - 2, z, stair("stone_brick", "west", "top"))
        s.at(W - 1, H - 2, z, stair("stone_brick", "east", "top"))
    for x in range(W):
        for z in range(D):
            s.at(x, H, z, wallf(x, H, z))
    for (x, z) in ((1, 6), (7, 12), (1, 18)):
        for y in range(H - 3, H):
            s.at(x, y, z, chain())
        s.at(x, H - 4, z, lantern())
    # Overgrowth: vines and glow lichen down the walls, hanging roots.
    for z in range(3, D - 3):
        for x, face in ((0, "west"), (W - 1, "east")):
            if blob(x, 0, z, 41) > 0.55:
                for y in range(H - 1, H - 6 - int(blob(x, 1, z, 7) * 4), -1):
                    s.at(x, y, z, f"vine[{face}=true,north=false,south=false,up=false,"
                                  + ("east=false" if face == "west" else "west=false") + "]")
            elif blob(x, 0, z, 43) > 0.6:
                s.at(x, 6, z, f"glow_lichen[{face}=true,north=false,south=false,up=false,down=false,waterlogged=false,"
                              + ("east=false" if face == "west" else "west=false") + "]")
    for (x, z) in ((2, 8), (6, 10), (3, 16), (7, 5)):
        s.at(x, H - 1, z, "hanging_roots[waterlogged=false]")
    # The exit: an arched door in the north wall above the end ledge.
    for x in (3, 4, 5):
        for y in (9, 10, 11):
            s.at(x, y, -1, "air")
    s.at(3, 11, -1, stair("stone_brick", "west", "top"))
    s.at(4, 11, -1, slab("stone_brick", "top"))
    s.at(5, 11, -1, stair("stone_brick", "east", "top"))
    s.at(4, 10, -2, "sea_lantern")
    for (x, y, z) in ((4, 11, D - 4), (2, 11, 9), (6, 11, 15), (2, 7, D - 2), (4, 10, 1), (6, 11, 5), (1, 6, 12),
                      (7, 6, 7)):
        s.at(x, y, z, light(13))
    s.mark("start", 4, 6, D - 2)
    s.mark("core", 4, 8, 0)
    s.marks["route"] = [[x + s.o[0], y + s.o[1], z + s.o[2]] for (x, y, z) in CISTERN_ROUTE]
    return s.finish()


# ---------------------------------------------------------------------------------------------- the lava bridge

def bridge():
    """A nether-brick forge hall cut by a lava chasm; the core waits on the far side. A button raises a path of
    stones out of the lava, one after another (sticky pistons under it), while the lamps along the walls light up."""
    W, D, H = 11, 21, 8
    s = Set("bridge", W + 4, H + 7, D + 4, (2, 4, 2))
    wallf = tex("nether_bricks", ("red_nether_bricks", lambda y: 0.18 + 0.02 * y), ("cracked_nether_bricks", 0.14))
    floorf = tex("polished_blackstone_bricks", ("cracked_polished_blackstone_bricks", 0.2), ("blackstone", 0.08))
    s.fill(-2, -4, -2, W + 1, H + 2, D + 1, wallf)
    s.fill(0, 1, 0, W - 1, H, D - 1, "air")
    C0, C1 = 5, 13                 # the chasm, z
    cx = 5                          # the bridge, x
    for x in range(W):
        for z in range(D):
            if C0 <= z <= C1:
                s.at(x, -1, z, "magma_block" if blob(x, 0, z, 9) > 0.45 else "blackstone")
                s.at(x, 0, z, "lava[level=0]")
            else:
                edge = z in (C0 - 1, C1 + 1)
                s.at(x, 0, z, "polished_blackstone" if edge else floorf)
    # The hidden bridge: a stone under the lava on a sticky piston, an empty cell under each for the signal.
    for z in range(C0, C1 + 1):
        s.at(cx, -1, z, "polished_blackstone_bricks")
        s.at(cx, -2, z, "sticky_piston[facing=up,extended=false]")
        s.at(cx, -3, z, "blackstone")
        s.mark(f"bridge{z - C0}", cx, -3, z)
    s.marks["bridge_count"] = C1 - C0 + 1
    # Lamps along both walls, a signal block behind each (put in by the test, one after another).
    k = 0
    for z in range(C0, C1 + 1, 2):
        for x, behind in ((-1, -2), (W, W + 1)):
            s.at(x, 3, z, "redstone_lamp[lit=false]")
            s.at(x, 4, z, "polished_blackstone_brick_stairs[facing=" + ("west" if x < 0 else "east")
                 + ",half=top,shape=straight,waterlogged=false]")
        s.mark(f"lamp{k}", -2, 3, z)
        s.mark(f"lampe{k}", W + 1, 3, z)
        k += 1
    s.marks["lamp_count"] = k
    # Basalt pillars on both banks, chiseled caps, a beam across the ceiling over each pair.
    for z in (0, 4, 14, 17, 20):
        for x in (0, W - 1):
            for y in range(1, H):
                s.at(x, y, z, "polished_basalt[axis=y]")
            s.at(x, H, z, "chiseled_polished_blackstone")
        for x in range(W):
            s.at(x, H + 1, z, "polished_blackstone_bricks")
    for z in range(D):
        for x in range(W):
            if s.get_at(x, H + 1, z) in (None, "minecraft:air"):
                s.at(x, H + 1, z, wallf(x, H + 1, z))
    # Two lavafalls from openings high in the side walls.
    for x, z, out in ((-1, 7, 0), (W, 11, W - 1)):
        s.at(x, 6, z, "lava[level=0]")
        s.at(x, 7, z, "polished_blackstone_bricks")
        s.at(x, 5, z, "polished_blackstone_bricks")
    # The far bank: the core on a gilded dais, a gold-and-amethyst reredos behind.
    s.at(cx, 1, 2, "gilded_blackstone")
    s.at(cx, 1, 3, "polished_blackstone_brick_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]")
    s.at(cx - 1, 1, 2, "polished_blackstone_brick_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]")
    s.at(cx + 1, 1, 2, "polished_blackstone_brick_stairs[facing=west,half=bottom,shape=straight,waterlogged=false]")
    s.mark("core", cx, 2, 2)
    for x in range(cx - 2, cx + 3):
        for y in range(1, 6):
            s.at(x, y, -1, "gold_block" if (x == cx and y in (3, 4)) else ("gilded_blackstone" if (x + y) % 2 else
                                                                            "polished_blackstone_bricks"))
    s.at(cx, 5, -1, "amethyst_block")
    s.at(cx, 6, -1, "amethyst_cluster[facing=up,waterlogged=false]")
    for x in (cx - 3, cx + 3):
        s.at(x, 1, 1, "polished_blackstone_wall[north=none,east=none,south=none,west=none,up=true,waterlogged=false]")
        s.at(x, 2, 1, "lantern[hanging=false,waterlogged=false]")
    # The near bank: the button on a post, two braziers at the edge.
    s.at(3, 1, 15, "chiseled_polished_blackstone")
    s.mark("button", 3, 2, 15)
    for x in (1, W - 2):
        s.at(x, 1, C1 + 1, "campfire[facing=south,lit=true,signal_fire=false,waterlogged=false]")
    for (x, z) in ((5, 18), (2, 19), (8, 19)):
        s.at(x, 6, z, light(12))
    # The way in at the south end.
    for x in (cx - 1, cx, cx + 1):
        for y in (1, 2, 3):
            s.at(x, y, D, "air")
    s.at(cx - 1, 4, D, stair("polished_blackstone_brick", "west", "top"))
    s.at(cx, 4, D, slab("polished_blackstone_brick", "top"))
    s.at(cx + 1, 4, D, stair("polished_blackstone_brick", "east", "top"))
    return s.finish()


# ---------------------------------------------------------------------------------------------- the hedge maze

def _carve(cells, n, seed):
    rng = random.Random(seed)
    grid = [[1] * n for _ in range(n)]
    stack = [(0, 0)]
    seen = {(0, 0)}
    grid[1][1] = 0
    while stack:
        cx, cz = stack[-1]
        nbrs = [(cx + dx, cz + dz) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))
                if 0 <= cx + dx < cells and 0 <= cz + dz < cells and (cx + dx, cz + dz) not in seen]
        if not nbrs:
            stack.pop()
            continue
        nx, nz = rng.choice(nbrs)
        grid[cz * 2 + 1 + (nz - cz)][cx * 2 + 1 + (nx - cx)] = 0
        grid[nz * 2 + 1][nx * 2 + 1] = 0
        seen.add((nx, nz))
        stack.append((nx, nz))
    return grid, rng


def _path_length(grid, n, start, goal):
    from collections import deque
    dist = {start: 0}
    queue = deque([start])
    while queue:
        x, z = queue.popleft()
        if (x, z) == goal:
            return dist[(x, z)]
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            p = (x + dx, z + dz)
            if 0 <= p[0] < n and 0 <= p[1] < n and not grid[p[1]][p[0]] and p not in dist:
                dist[p] = dist[(x, z)] + 1
                queue.append(p)
    return None


def maze_grid(cells, n, mid):
    """A perfect maze (one way through, no loops) whose entrance in the south goes two steps in, meets a hedge and
    turns west, with the hedge north of the next cell standing between the player and the middle (what the trailer
    tries to cut through), and a long walk from the gate to the core."""
    for seed in range(1, 5000):
        grid, rng = _carve(cells, n, seed)
        for dz in (-1, 0, 1):
            for dx in (-1, 0, 1):
                grid[mid + dz][mid + dx] = 0
        grid[n - 1][mid] = 0
        e = n - 4                                  # the first cell row inside (grid z), two blocks in from the edge
        if not (grid[n - 2][mid] == 0 and grid[n - 3][mid] == 0 and grid[e][mid] == 0):
            continue
        if not (grid[e - 1][mid] == 1 and grid[e][mid - 1] == 0 and grid[e][mid - 2] == 0 and grid[e - 1][mid - 2] == 1):
            continue
        length = _path_length(grid, n, (mid, n - 1), (mid, mid))
        if length is None or length < 70:
            continue
        print(f"maze seed {seed}: {length} steps from the gate to the core")
        return grid, rng
    raise RuntimeError("no maze fits")


def maze():
    CELLS = 9
    N = CELLS * 2 + 1      # 19 x 19 grid of hedge or path
    PAD = 3                # lawn and garden wall round it
    SIZE = N + PAD * 2
    DEPTH = 5              # foundation under the lawn
    UP = 9
    s = Set("maze", SIZE, DEPTH + UP, SIZE, (PAD, DEPTH, PAD))
    mid = N // 2
    grid, rng = maze_grid(CELLS, N, mid)
    hedge = tex("oak_leaves[distance=7,persistent=true,waterlogged=false]",
                ("flowering_azalea_leaves[distance=7,persistent=true,waterlogged=false]", 0.12),
                ("azalea_leaves[distance=7,persistent=true,waterlogged=false]", 0.25))
    path = tex("coarse_dirt", ("gravel", 0.25), ("moss_block", 0.15))
    for gx in range(-PAD, N + PAD):
        for gz in range(-PAD, N + PAD):
            for y in range(-DEPTH, -1):
                s.at(gx, y, gz, "stone" if y < -3 else "dirt")
            s.at(gx, -1, gz, "dirt")
            inside = 0 <= gx < N and 0 <= gz < N
            if inside and grid[gz][gx]:
                s.at(gx, 0, gz, "grass_block[snowy=false]")
                for y in (1, 2, 3):
                    s.at(gx, y, gz, hedge(gx, y, gz))
            elif inside:
                s.at(gx, 0, gz, path(gx, 0, gz))
            else:
                s.at(gx, 0, gz, "grass_block[snowy=false]")
    # Garden wall round the lawn: mossy stone with posts and lanterns, a gate in the south.
    lo, hi = -PAD + 1, N + PAD - 2
    for i in range(lo, hi + 1):
        for (x, z) in ((i, lo), (i, hi), (lo, i), (hi, i)):
            post = (i - lo) % 4 == 0
            s.at(x, 1, z, "mossy_stone_bricks" if blob(x, 1, z, 5) > 0.55 else "stone_bricks")
            if post:
                s.at(x, 2, z, "stone_brick_wall[north=none,east=none,south=none,west=none,up=true,waterlogged=false]")
                s.at(x, 3, z, lantern(hanging=False))
            else:
                s.at(x, 2, z, slab("stone_brick"))
    for x in (mid - 1, mid, mid + 1):
        s.at(x, 1, hi, path(x, 0, hi))
        s.at(x, 2, hi, "air")
        s.at(x, 0, hi, "gravel")
        s.at(x, 0, hi - 1, "gravel")
    for x in (mid - 2, mid + 2):
        s.at(x, 1, hi, "chiseled_stone_bricks")
        s.at(x, 2, hi, "stone_brick_wall[north=none,east=none,south=none,west=none,up=true,waterlogged=false]")
        s.at(x, 3, hi, lantern(hanging=False))
    # Flowers on the lawn.
    for gx in range(-PAD + 2, N + PAD - 2):
        for gz in range(-PAD + 2, N + PAD - 2):
            if 0 <= gx < N and 0 <= gz < N:
                continue
            r = _hash(gx, 0, gz, 77)
            if r < 0.12:
                s.at(gx, 1, gz, rng.choice(["short_grass", "poppy", "cornflower", "oxeye_daisy", "short_grass"]))
    # The middle: a round plinth for the core, four lanterns on posts.
    s.at(mid, 0, mid, "chiseled_stone_bricks")
    s.at(mid, 1, mid, "polished_andesite")
    for dx, dz, f in ((0, -1, "south"), (0, 1, "north"), (-1, 0, "east"), (1, 0, "west")):
        s.at(mid + dx, 1, mid + dz, stair("stone_brick", f))
    for dx, dz in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
        s.at(mid + dx, 1, mid + dz, "stone_brick_wall[north=none,east=none,south=none,west=none,up=true,waterlogged=false]")
        s.at(mid + dx, 2, mid + dz, lantern(hanging=False))
    s.mark("core", mid, 2, mid)
    s.mark("gate", mid, 1, hi)
    s.marks["maze_size"] = N
    return s.finish()


if __name__ == "__main__":
    sets = [hall(), sanctum(), cistern(), bridge(), maze()]
    marks = {st.name: {"size": [st.w, st.h, st.d], "o": list(st.o), **st.marks} for st in sets}
    with open(os.path.join(OUT, "marks.json"), "w") as f:
        json.dump(marks, f, indent=1)
    for st in sets:
        y = st.o[1] + 1
        print(f"\n== {st.name} layer y={y} (floor+1)")
        print(st.top_view(y))
