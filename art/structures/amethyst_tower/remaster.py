"""Amethyst Tower remaster: the Zone Lock 1.x template block for block, with only the changes the
new rules require. Kept at its original DataVersion (3953) so the game's data fixer upgrades the old
blocks and entities (chain -> iron_chain, item frames, the mage's armor stand) on load.

The library puzzle is the original one, untouched: two comparators read the last book slot touched on
two chiseled bookshelves. East bottom shelf (10,4,6): only slot 6 (signal 6) reaches the piston line
(it loses 5) and raises the amethyst step into the stair alcove. West bottom shelf (4,4,7): slots 4-6
(signal 4+) reach the floor grid (it loses 3), the observers see it and the dispensers gas the room.

Changes:
  1. Placement order. The game places a template in three passes (full blocks, other blocks, block
     entities). The 1.x carpets went down before the dispensers under them and dropped as items.
     Blocks that carry data are placed in the last pass in height order, so the carpets standing on
     a block entity get an empty data tag and go down after it. (The dispensers still fire once when
     the tower is placed, as in 1.x: the floor wires change while their neighbours are placed and the
     observers see it. It happens at chunk generation, with nobody there, and costs one potion of 9.)
  2. Shaft: two footholds the 1.x climb lacked with normal jumps (it relied on slime bounces).
  3. Core: the 1.x core becomes the current zonelock:amethyst_core with its config.
Writes amethyst_tower.nbt (as stored, step down) and amethyst_tower_solved.nbt (step up, for the
path check).
"""
import gzip
import io
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import nbtio  # noqa: E402

SRC = os.path.join(HERE, "amethyst_tower_1x.nbt")  # the Zone Lock 1.x template it starts from
# The template the mod ships, written in place; the solved copy (step up) only feeds the path check.
OUT = os.path.join(HERE, "..", "..", "..", "common", "src", "main", "resources", "resourcepacks", "dungeons", "data",
                   "zonelock", "structure", "amethyst_tower.nbt")
f = io.BytesIO(gzip.open(SRC).read())
t = f.read(1)[0]
nbtio._rs(f)
root = nbtio._rp(f, t)
assert root["DataVersion"] == 3953

pal = root["palette"]
W, H, D = root["size"]
blocks, nbt = {}, {}
for e in root["blocks"]:
    p = tuple(e["pos"])
    pe = pal[e["state"]]
    blocks[p] = (pe["Name"], dict(pe.get("Properties", {})))
    if "nbt" in e:
        nbt[p] = e["nbt"]


def S(p, name, props=None, be=None):
    blocks[p] = (name if ":" in name else "minecraft:" + name, dict(props or {}))
    nbt.pop(p, None)
    if be is not None:
        nbt[p] = be


def expect(p, name):
    assert blocks[p][0] == "minecraft:" + name, (p, blocks[p])


# ---------------------------------------------------------------- 1. placement order (puzzle itself untouched)
expect((3, 4, 7), "comparator")
expect((11, 4, 6), "comparator")
SUPPORTED = ("_carpet", "candle", "_pressure_plate", "torch", "redstone_wire", "repeater", "comparator", "rail")
ORIGINAL_BE = set(nbt)
moved = []
for q, (n, pr) in blocks.items():
    below = (q[0], q[1] - 1, q[2])
    if q not in nbt and below in ORIGINAL_BE and n.endswith(SUPPORTED):
        nbt[q] = {}                      # placed after the block entity it stands on
        moved.append(q)
print("placed in the last pass:", len(moved), "blocks")

# ---------------------------------------------------------------- 1b. clue and slot positions
# Shelves count from the floor: each chiseled bookshelf block holds two (slots 1-3 on top, 4-6 below),
# so the wall of three blocks has six. Comparators output the number of the slot touched last.
# Right answer: east bottom block, TOP shelf (2nd from the floor), top-right slot = signal 3 exactly.
# Poison: west middle block, TOP shelf (4th from the floor), top-right slot = signal 3 or more (the 3rd
# shelf, the bottom of that block, is poisoned too: it is the shelf by the green candles).
# Every other slot of every shelf holds a book; the two gaps are the choice. Green candles on the west
# pedestal mark the poison; the east ones stay purple like the rest of the tower.
GAP_SLOT = 2                                   # top-right
GAPS = {(10, 4, 6): "right", (4, 5, 7): "poison"}
for q, (n, pr) in blocks.items():
    if not n.endswith("chiseled_bookshelf"):
        continue
    items = [it for it in nbt[q].get("Items", [])]
    template = items[0]
    have = {it["Slot"][1] for it in items}
    for s in range(6):
        if q in GAPS and s == GAP_SLOT:
            pr[f"slot_{s}_occupied"] = "false"
            items = [it for it in items if it["Slot"][1] != s]
            continue
        pr[f"slot_{s}_occupied"] = "true"
        if s not in have:
            items.append(dict(template, Slot=("b", s)))
    # rest: every shelf "last touched" on slot 1 (signal 1): nothing powered
    nbt[q] = dict(nbt[q], Items=sorted(items, key=lambda it: it["Slot"][1]), last_interacted_slot=("i", 0))
expect((6, 6, 8), "purple_candle")
S((6, 6, 8), "green_candle", dict(blocks[(6, 6, 8)][1]))

WIRE = lambda **c: ("minecraft:redstone_wire", {"north": c.get("n", "none"), "east": c.get("e", "none"),
                                               "south": c.get("s", "none"), "west": c.get("w", "none"),
                                               "power": str(c.get("p", 0))})
REP = lambda facing, powered=False: ("minecraft:repeater", {"delay": "1", "facing": facing, "locked": "false",
                                                           "powered": "true" if powered else "false"})
BRICK = ("minecraft:deepslate_bricks", {})

# --- west (poison): the comparator moves up to read the middle block (4,5,7)
expect((3, 5, 7), "air")
blocks[(3, 4, 7)] = BRICK; nbt.pop((3, 4, 7), None)        # old comparator (bottom block) -> its support
S((3, 5, 7), "comparator", {"facing": "east", "mode": "compare", "powered": "true"},
  {"id": "minecraft:comparator", "OutputSignal": ("i", 1)})
# it powers brick (2,5,7) strongly, which feeds the original dust (2,4,7) under it: the old line, one
# dust shorter: the repeater moves from (3,2,6) to (3,2,7) and reads (3,2,8) = signal - 2, so it fires
# from signal 3
blocks[(2, 4, 7)] = WIRE(e="side", s="side", p=1)
blocks[(2, 3, 8)] = WIRE(e="side", n="up", p=0)
expect((3, 2, 7), "redstone_wire")
blocks[(3, 2, 7)] = REP("south")
blocks[(3, 2, 6)] = WIRE(n="side", s="side", p=0)

# --- east (right): exactly signal 3, in the hollow under the floor (x10-12, y1-2, z8-9)
for x in (7, 8, 9, 10, 11):
    expect((x, 1, 8), "air")
    blocks[(x, 1, 8)] = BRICK                               # supports, out of sight
blocks[(12, 2, 8)] = WIRE(n="up", s="side", w="side")      # signal - 2: feeds R3 and the old line
blocks[(11, 2, 8)] = REP("east")                            # R3: fires from signal 3
S((10, 2, 8), "comparator", {"facing": "east", "mode": "subtract", "powered": "false"},
  {"id": "minecraft:comparator", "OutputSignal": ("i", 0)})  # C2: R3 minus R4
blocks[(11, 2, 9)] = REP("east")                            # R4: reads (12,2,9) = signal - 3, fires from 4
blocks[(10, 2, 9)] = WIRE(e="side", n="side", p=0)          # R4 -> C2 side
blocks[(9, 2, 9)] = BRICK                                   # the old repeater to the piston goes
blocks[(8, 2, 9)] = BRICK
blocks[(9, 2, 8)] = BRICK                                   # C2 powers it ...
blocks[(8, 2, 8)] = REP("east")                             # ... a repeater carries it on ...
blocks[(7, 2, 8)] = BRICK                                   # ... into the block beside the sticky piston
# (no dust over x5-9: grid wires at y1 would climb into it and the right answer would gas the room)

# ---------------------------------------------------------------- 2. shaft: two footholds the original lacked
# With normal jumps the 1.x climb stops at the shaft floor: its first holds are 2 up, or under a low
# ceiling, and the first ladder is only reachable from a spot with no headroom. It relied on slime
# bounces. Two holds in the original's own vocabulary close the gaps (found by search with the path
# checker); the slime stays as the landing cushion for falls.
EXTRA_STEPS = "--steps" in sys.argv          # off: the user wants the 1.x parkour exactly
if EXTRA_STEPS:
    expect((5, 11, 7), "air")      # sill in front of the west window
    S((5, 11, 7), "deepslate_brick_slab", {"type": "top", "waterlogged": "false"})
    expect((6, 14, 8), "air")      # the crystal growing out beside the cluster at (6,14,9)
    S((6, 14, 8), "amethyst_block")

# library storey: the purple glass beside the shelves showed the redstone behind them from outside.
# Deepslate bricks like the wall around them (the shaft windows above stay glass).
for q in [(3, 6, 7), (3, 7, 7), (11, 6, 7), (11, 7, 7)]:
    expect(q, "purple_stained_glass")
    blocks[q] = ("minecraft:deepslate_bricks", {})

# the last jump (ledge (9,15,6) -> first ladder (6,16,6)): the brick right above the ledge becomes an
# upside-down stair whose open half faces the ladder, so the head has half a block more room where
# you take off (it still needs a sprint jump: the slabs of the middle floor cap the arc)
expect((9, 17, 6), "deepslate_bricks")
S((9, 17, 6), "deepslate_brick_stairs", {"facing": "east", "half": "top", "shape": "straight", "waterlogged": "false"})

# ---------------------------------------------------------------- 3. core
CORE = (7, 26, 4)
EXT = {"north": CORE[2], "south": D - 1 - CORE[2], "west": CORE[0], "east": W - 1 - CORE[0],
       "down": CORE[1], "up": H - 1 - CORE[1]}
S(CORE, "zonelock:amethyst_core", {"armed": "true", "mirror": "none", "rotation": "none"},
  {"id": "zonelock:amethyst_core",
   "config": {"name": "Amethyst Tower", "extents": EXT, "rules": ("i", 1023), "seals": ("b", 0), "key": ("b", 0),
              "redstone": ("b", 0), "key_color": ("i", 0), "config_locked": ("b", 0)},
   "armed": ("b", 1)})


# ---------------------------------------------------------------- save (old palette keys, old DataVersion)
def save(path, blocks):
    states, idx, bl = [], {}, []
    for p in sorted(blocks, key=lambda p: (p[1], p[2], p[0])):
        n, pr = blocks[p]
        key = (n, tuple(sorted(pr.items())))
        if key not in idx:
            idx[key] = len(states)
            e = {"Name": n}
            if pr:
                e["Properties"] = dict(sorted(pr.items()))
            states.append(e)
        e = {"pos": ("li", list(p)), "state": ("i", idx[key])}
        if p in nbt:
            e["nbt"] = nbt[p]
        bl.append(e)
    out = {"DataVersion": ("i", 3953), "size": ("li", [W, H, D]), "palette": states, "blocks": bl,
           "entities": root["entities"]}
    buf = io.BytesIO()
    buf.write(b"\x0a")
    nbtio._ws(buf, "")
    nbtio._wc(buf, out)
    with gzip.open(path, "wb") as g:
        g.write(buf.getvalue())
    print("saved", os.path.basename(path), f"{W}x{H}x{D}", len(bl), "blocks", len(states), "states")


# ---------------------------------------------------------------- 4. the core recipe on the wall, its ingredients in the chest
# The 3x3 item frames in the mage's room (wall z=10, facing north) show the Amethyst Core recipe:
#   P A P / A E A / P C P   (P polished deepslate, A amethyst shard, E ender eye, C amethyst block)
# Seen from the room the viewer faces south, so x=8 is the left column, x=6 the right (it is symmetric).
RECIPE = {28: ["polished_deepslate", "amethyst_shard", "polished_deepslate"],
          27: ["amethyst_shard", "ender_eye", "amethyst_shard"],
          26: ["polished_deepslate", "amethyst_block", "polished_deepslate"]}
frames = 0
for e in root["entities"]:
    n = e["nbt"]
    if n.get("id") != "minecraft:item_frame":
        continue
    x, y, z = e["blockPos"]
    n["Item"] = dict(n["Item"], id="minecraft:" + RECIPE[y][8 - x])
    n["Fixed"] = ("b", 1)                      # nobody takes the recipe off the wall or turns it
    n["Invulnerable"] = ("b", 1)
    frames += 1
assert frames == 9, frames
# The chest always holds at least one core's worth: 4 polished deepslate, 4+ shards (19 already), an ender
# eye and an amethyst block. The netherite sword and some deepslate stay.
CHEST = next(p for p, (n, _) in blocks.items() if n == "minecraft:chest")
items = {it["Slot"][1]: it for it in nbt[CHEST]["Items"]}
items[0] = dict(items[0], id="minecraft:polished_deepslate", count=4)
items[8] = dict(items[8], id="minecraft:ender_eye", count=1)
items[9] = dict(items[9], id="minecraft:amethyst_block", count=1)
nbt[CHEST] = dict(nbt[CHEST], Items=[items[s] for s in sorted(items)])
assert sum(it["count"] for it in items.values() if it["id"] == "minecraft:amethyst_shard") >= 4

# ---------------------------------------------------------------- 5. the tower eats the ground around it
# The template grows by PAD on each side (structure void: the terrain stays) and the ground layer around
# the base turns to blackstone (some smooth basalt and cobbled deepslate), dense against the base and
# thinning out; amethyst set into it near the walls and clusters and buds growing on the black stone.
# The path to the door stays free of crystals.
PAD = 4
# The ring follows the real ground: each of its blocks carries this tag with its layer, and the mod's
# zonelock:surface_snap processor (in the tower's template pool) moves it onto the top of its column when
# the tower generates. Without the processor (a plain template placement) it stays at the template's level.
MARK = "zonelock_surface"
GY = 2                                         # template y of the ground layer


def h(x, z, s):
    v = (x * 73856093) ^ (z * 19349663) ^ (s * 83492791)
    v = (v ^ (v >> 13)) * 1274126177
    return ((v ^ (v >> 16)) & 0xFFFF) / 65535.0


blocks = {(x + PAD, y, z + PAD): v for (x, y, z), v in blocks.items()}
nbt = {(x + PAD, y, z + PAD): v for (x, y, z), v in nbt.items()}
for e in root["entities"]:
    e["blockPos"] = [e["blockPos"][0] + PAD, e["blockPos"][1], e["blockPos"][2] + PAD]
    e["pos"] = [e["pos"][0] + PAD, e["pos"][1], e["pos"][2] + PAD]
W, D = W + 2 * PAD, D + 2 * PAD
CORE = (CORE[0] + PAD, CORE[1], CORE[2] + PAD)
nbt[CORE]["config"]["extents"] = {"north": CORE[2], "south": D - 1 - CORE[2], "west": CORE[0], "east": W - 1 - CORE[0],
                                  "down": CORE[1], "up": H - 1 - CORE[1]}
# footprint of the base: every column with a built block at ground level or just above
foot = {(x, z) for (x, y, z), (n, _) in blocks.items() if y in (GY, GY + 1) and n != "minecraft:air"}
DOOR_X, DOOR_Z = 7 + PAD, 4 + PAD             # the way in is on the north side
ground_n = crystals = 0
for x in range(W):
    for z in range(D):
        if (x, z) in foot:
            continue
        d = min(max(abs(x - fx), abs(z - fz)) for (fx, fz) in foot)
        if d > 5:
            continue
        noise = 0.55 * h(x, z, 1) + 0.45 * h(x // 2, z // 2, 2)
        if noise > (1.05 - 0.17 * d):          # ~all at d=1, about a quarter at d=5
            continue
        r = h(x, z, 3)
        if d <= 2 and r < 0.06:
            blk = "minecraft:budding_amethyst" if r < 0.02 else "minecraft:amethyst_block"
        elif r < 0.70:
            blk = "minecraft:blackstone"
        elif r < 0.85:
            blk = "minecraft:smooth_basalt"
        else:
            blk = "minecraft:cobbled_deepslate"
        blocks[(x, GY, z)] = (blk, {})
        nbt[(x, GY, z)] = {MARK: ("i", 0)}                              # replaces the real top of the ground
        if d <= 2 and h(x, z, 4) < 0.5:
            blocks[(x, GY - 1, z)] = ("minecraft:blackstone", {})      # a little depth where it is densest
            nbt[(x, GY - 1, z)] = {MARK: ("i", -1)}
        ground_n += 1
        on_path = abs(x - DOOR_X) <= 1 and z < DOOR_Z
        above = (x, GY + 1, z)
        if not on_path and above not in blocks and h(x, z, 5) < (0.42 - 0.06 * d):
            r2 = h(x, z, 6)
            kind = ("amethyst_cluster" if r2 < 0.3 else "large_amethyst_bud" if r2 < 0.6 else
                    "medium_amethyst_bud" if r2 < 0.85 else "small_amethyst_bud")
            blocks[above] = ("minecraft:" + kind, {"facing": "up", "waterlogged": "false"})
            nbt[above] = {MARK: ("i", 1)}                               # sits on the real ground
            crystals += 1
print(f"ground: {ground_n} blocks turned, {crystals} crystals; template now {W}x{H}x{D}")

save(OUT, blocks)
solved = dict(blocks)
STEP = (7 + PAD, 4, 9 + PAD)
solved[STEP] = solved[(STEP[0], 3, STEP[2])]   # the piston has pushed the amethyst step up
solved[(STEP[0], 3, STEP[2])] = ("minecraft:piston_head", {"facing": "up", "short": "false", "type": "sticky"})
save(os.path.join(HERE, "amethyst_tower_solved.nbt"), solved)
