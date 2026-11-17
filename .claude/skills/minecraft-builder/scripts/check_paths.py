#!/usr/bin/env python3
"""
check_paths.py - playability check for a Minecraft structure template (.nbt).

Simulates how a survival player moves (cannot place or break blocks: inside a Zone Lock zone that is
the rule) and answers:
  1. Can a player get from outside to every target (core, seals, key chests, extra --target)?
  2. From EVERY place a player can reach, can they get back outside?   (no traps)
  3. From every reachable place, can they get back to the core?        (no dead ends that strand them)

Usage:
  python check_paths.py dungeon.nbt --ground-y 33
  python check_paths.py dungeon.nbt --ground-y 33 --target lever=12,20,4 --json report.json

--ground-y is the template y of the terrain surface layer: template cells left as structure void
at or below it are treated as solid terrain, void above it as open air. Outside the template is
flat ground at that height. The player starts anywhere outside the template.

Movement model (approximate, deliberately conservative about traps):
  * body is 2 blocks tall; stands on a block with a solid top, or in water, or on a climbable
  * walks, steps/jumps up 1 block, drops down any height up to --max-fall (default 20) blocks onto
    solid ground, or any height into water
  * climbs ladders, vines, twisting/weeping vines, scaffolding; swims in water in any direction
  * parkour jumps between standing spots: +1 up to 3-block gaps, level up to 4, down 1 up to 4,
    down 2+ up to 5 (gap = air blocks between take-off and landing), with clear headroom on the arc
  * opens wooden doors, fence gates and wooden trapdoors by hand; iron doors / iron trapdoors only
    if a button or lever is within 2 blocks of the door
  * a door only blocks movement across its panel: an open door blocks the other axis than a closed
    one (an iron door turned sideways and powered open is a gate across the passage)
  * --puzzle-door x,y,z marks a door / trapdoor / fence gate that the puzzle's redstone toggles when
    solved. The check then runs twice: BEFORE (template as stored: the --target buttons must be
    reachable, no traps) and AFTER solving (door toggled: core, seals, key chests, no traps, no
    stranded places)
  * lava, magma, fire, cactus, sweet berries and powder snow are never stood on
It is a check, not a proof: it does not know about redstone states, timing, mobs or momentum
tricks. A failure is almost always a real problem; a pass still deserves an in-game test.
Exit code 0 = all checks pass, 1 = problems found.
"""
import argparse
import gzip
import io
import json
import math
import struct
import sys
from collections import deque

# ----------------------------------------------------------------------------- NBT


def _rs(f):
    n = struct.unpack(">H", f.read(2))[0]
    return f.read(n).decode("utf-8")


def _rp(f, t):
    if t == 1: return struct.unpack(">b", f.read(1))[0]
    if t == 2: return struct.unpack(">h", f.read(2))[0]
    if t == 3: return struct.unpack(">i", f.read(4))[0]
    if t == 4: return struct.unpack(">q", f.read(8))[0]
    if t == 5: return struct.unpack(">f", f.read(4))[0]
    if t == 6: return struct.unpack(">d", f.read(8))[0]
    if t == 7:
        n = struct.unpack(">i", f.read(4))[0]; return f.read(n)
    if t == 8: return _rs(f)
    if t == 9:
        et = f.read(1)[0]; n = struct.unpack(">i", f.read(4))[0]
        return [_rp(f, et) for _ in range(n)]
    if t == 10:
        d = {}
        while True:
            tt = f.read(1)[0]
            if tt == 0: return d
            k = _rs(f); d[k] = _rp(f, tt)
    if t == 11:
        n = struct.unpack(">i", f.read(4))[0]; return [struct.unpack(">i", f.read(4))[0] for _ in range(n)]
    if t == 12:
        n = struct.unpack(">i", f.read(4))[0]; return [struct.unpack(">q", f.read(8))[0] for _ in range(n)]
    raise ValueError(t)


def load_template(path):
    raw = gzip.open(path).read()
    f = io.BytesIO(raw)
    t = f.read(1)[0]; _rs(f)
    root = _rp(f, t)
    w, h, d = root["size"]
    pal = root.get("palette") or root["palettes"][0]
    states = []
    for e in pal:
        name = e.get("id") or e.get("Name")
        props = e.get("properties") or e.get("Properties") or {}
        states.append((name, props))
    blocks, bents = {}, {}
    for e in root["blocks"]:
        p = tuple(e["pos"])
        blocks[p] = states[e["state"]]
        if "nbt" in e:
            bents[p] = e["nbt"]
    return (w, h, d), blocks, bents


# ----------------------------------------------------------------------------- block classes

def short(name):
    return name.split(":", 1)[1] if ":" in name else name


CLIMBABLE = ("ladder", "vine", "twisting_vines", "weeping_vines", "scaffolding", "cave_vines")
DANGER = ("lava", "magma_block", "fire", "soul_fire", "cactus", "sweet_berry_bush", "powder_snow", "campfire",
          "wither_rose", "pointed_dripstone")
# names are matched exactly or by suffix, never by substring ("stairs" contains "air"!)
NO_COLLISION = {
    "air", "cave_air", "void_air", "light", "structure_void", "redstone_wire", "lever", "tripwire", "tripwire_hook",
    "cobweb", "snow", "short_grass", "tall_grass", "fern", "large_fern", "dead_bush", "seagrass", "tall_seagrass",
    "glow_lichen", "sculk_vein", "pink_petals", "leaf_litter", "lily_pad", "nether_sprouts", "sugar_cane",
    "hanging_roots", "spore_blossom", "frogspawn", "torch", "wall_torch", "soul_torch", "soul_wall_torch",
    "redstone_torch", "redstone_wall_torch", "copper_torch", "copper_wall_torch", "rail", "powered_rail",
    "detector_rail", "activator_rail", "crimson_roots", "warped_roots", "crimson_fungus", "warped_fungus",
    "brown_mushroom", "red_mushroom", "bamboo_sapling", "small_dripleaf", "poppy", "dandelion", "blue_orchid",
    "allium", "azure_bluet", "oxeye_daisy", "cornflower", "lily_of_the_valley", "torchflower", "sunflower",
    "lilac", "rose_bush", "peony", "pitcher_plant", "firefly_bush", "bush", "short_dry_grass", "tall_dry_grass",
    "wildflowers", "resin_clump", "string",
}
NO_COLLISION_SUFFIX = ("_carpet", "_button", "_pressure_plate", "_sign", "_banner", "candle", "_sapling", "_tulip",
                       "_coral_fan", "_coral")
WOOD_OPENABLE = ("door", "fence_gate", "trapdoor")


def door_block_axis(props):
    """axis of movement ('x' or 'z') a door panel blocks. Closed: the panel lies across the facing axis;
    open: it swings to lie along it, blocking the other axis."""
    along = "z" if props.get("facing", "north") in ("north", "south") else "x"
    if props.get("open") != "true":
        return along
    return "x" if along == "z" else "z"


def classify(state):
    """-> (passable, solid_top, climb, water, danger, door)  door in {None,'wood','iron'}"""
    if state is None:
        return None
    name, props = state
    n = short(name)
    water = n in ("water", "bubble_column") or props.get("waterlogged") == "true" and n in ("seagrass", "kelp", "kelp_plant", "tall_seagrass")
    if n in ("water", "bubble_column", "kelp", "kelp_plant", "seagrass", "tall_seagrass"):
        return (True, False, False, True, False, None)
    if any(k in n for k in DANGER):
        return (n == "powder_snow", False, False, False, True, None)
    climb = any(k == n or n.endswith(k) for k in CLIMBABLE)
    if climb:
        return (True, n == "scaffolding", True, False, False, None)
    if n.endswith("_door") or n.endswith("_trapdoor") or n.endswith("fence_gate"):
        iron = n.startswith("iron_")
        is_open = props.get("open") == "true"
        if n.endswith("_trapdoor"):
            # closed trapdoor: thin floor plate (top half = ceiling plate); treat as standable floor
            if is_open:
                return (True, False, False, False, False, None)
            return (False, True, False, False, False, "iron" if iron else "wood")
        return (is_open, False, False, False, False, "iron" if iron else "wood")
    if n in NO_COLLISION or n.endswith(NO_COLLISION_SUFFIX):
        return (True, False, False, False, False, None)
    # everything else (full blocks, slabs, stairs, fences, walls, panes, bars, chains, lanterns...) blocks the
    # body and can be stood on
    return (False, True, False, False, False, None)


# ----------------------------------------------------------------------------- world model

class World:
    def __init__(self, size, blocks, ground_y, margin=3):
        self.w, self.h, self.d = size
        self.blocks = blocks
        self.g = ground_y
        self.m = margin
        self.cache = {}

    def inside(self, x, y, z):
        return 0 <= x < self.w and 0 <= y < self.h and 0 <= z < self.d

    def cls(self, x, y, z):
        key = (x, y, z)
        c = self.cache.get(key)
        if c is not None:
            return c
        if self.inside(x, y, z):
            st = self.blocks.get(key)
            if st is None:   # structure void: terrain below grade, open air above
                c = (False, True, False, False, False, None) if y <= self.g else (True, False, False, False, False, None)
            else:
                c = classify(st)
        else:
            c = (False, True, False, False, False, None) if y <= self.g else (True, False, False, False, False, None)
        self.cache[key] = c
        return c

    def in_box(self, x, y, z):
        return -self.m <= x < self.w + self.m and -self.m <= z < self.d + self.m and 0 <= y < self.h + 2

    def door_at(self, x, y, z):
        """(kind, blocked_axis) for a door cell, else None. Trapdoors and fence gates are not directional."""
        st = self.blocks.get((x, y, z)) if self.inside(x, y, z) else None
        if not st:
            return None
        n = short(st[0])
        if not n.endswith("_door"):
            return None
        return ("iron" if n.startswith("iron_") else "wood", door_block_axis(st[1]))

    def body_ok(self, x, y, z):
        # door / trapdoor / gate cells count as room for the body: whether the player can get INTO them
        # is decided per move (panel side, can it be opened)
        a, b = self.cls(x, y, z), self.cls(x, y + 1, z)
        return (a[0] or a[5] is not None) and (b[0] or b[5] is not None) and not a[4] and not b[4]

    def openable(self, x, y, z):
        return self.cls(x, y, z)[5] is not None

    def can_stand(self, x, y, z):
        if not self.body_ok(x, y, z):
            return False
        here, below = self.cls(x, y, z), self.cls(x, y - 1, z)
        if here[2] or here[3]:          # climbable or water
            return True
        return below[1] and not below[4]

    def is_step(self, x, y, z):
        """stairs or a bottom slab: walked up without jumping (step height 0.6)"""
        st = self.blocks.get((x, y, z)) if self.inside(x, y, z) else None
        if not st:
            return False
        n = short(st[0])
        return n.endswith("_stairs") and st[1].get("half", "bottom") == "bottom" or n.endswith("_slab") and st[1].get("type") == "bottom"

    def outside(self, x, z):
        return not (0 <= x < self.w and 0 <= z < self.d)


def toggleable(world, door_pos):
    """an iron door / trapdoor the player can work: a button or lever within 2 blocks of it"""
    fx, fy, fz = door_pos
    for dx in range(-2, 3):
        for dy in range(-2, 3):
            for dz in range(-2, 3):
                st = world.blocks.get((fx + dx, fy + dy, fz + dz))
                if st and (short(st[0]).endswith("_button") or short(st[0]) == "lever"):
                    return True
    return False


H4 = ((1, 0), (-1, 0), (0, 1), (0, -1))


def moves(world, p, max_fall):
    x, y, z = p
    out = []
    here = world.cls(x, y, z)

    def through(q, axis):
        """a body moving along `axis` can enter cell q (doors by panel side; closed things it can open)"""
        d = world.door_at(*q)
        if d is not None:
            return d[1] != axis or d[0] == "wood" or toggleable(world, q)
        c = world.cls(*q)
        if c[0] and not c[4]:
            return True
        return c[5] is not None and (c[5] == "wood" or toggleable(world, q))

    def leave_ok(axis):
        for q in ((x, y, z), (x, y + 1, z)):
            d = world.door_at(*q)
            if d is not None and d[1] == axis and d[0] != "wood" and not toggleable(world, q):
                return False
        return True

    def stand_from(q, axis):
        qx, qy, qz = q
        if not (through(q, axis) and through((qx, qy + 1, qz), axis)):
            return False
        c, below = world.cls(qx, qy, qz), world.cls(qx, qy - 1, qz)
        if c[2] or c[3]:
            return True
        return below[1] and not below[4]

    # climb / swim vertically
    if here[2] or here[3]:
        if world.can_stand(x, y + 1, z):
            out.append((x, y + 1, z))
        if world.can_stand(x, y - 1, z):
            out.append((x, y - 1, z))
    for dx, dz in H4:
        nx, nz = x + dx, z + dz
        axis = "x" if dx else "z"
        if not leave_ok(axis):
            continue
        if stand_from((nx, y, nz), axis):
            out.append((nx, y, nz))
        elif stand_from((nx, y + 1, nz), axis) and (world.cls(x, y + 2, z)[0] or world.is_step(nx, y, nz) or world.is_step(x, y - 1, z)):
            out.append((nx, y + 1, nz))       # jump up 1 (needs headroom) or walk up a stair / slab step
        elif through((nx, y, nz), axis) and through((nx, y + 1, nz), axis):
            # walk off an edge and fall
            yy = y - 1
            land = None
            while yy >= 0:
                c = world.cls(nx, yy, nz)
                if c[4]:
                    break                      # lava / fire below: never jump there
                if c[2] or c[3]:
                    land = yy                  # caught by water or a climbable
                    break
                if not c[0]:
                    land = yy + 1
                    break
                yy -= 1
            if land is not None and world.can_stand(nx, land, nz):
                c = world.cls(nx, land, nz)
                if y - land <= max_fall or c[2] or c[3]:
                    out.append((nx, land, nz))
    return out


def clear_path(world, pts, top, a, b):
    """body (2 high, feet at `top`) fits along the polyline pts, ignoring the take-off and landing columns
    themselves. A diagonal step between two cells needs one of the two corner cells clear too (a 0.6-wide
    body does not squeeze between two blocks that touch at a corner)."""
    def body_clear(cx, cz):
        return world.cls(cx, top, cz)[0] and world.cls(cx, top + 1, cz)[0]

    prev = None
    for (x1, z1), (x2, z2) in zip(pts, pts[1:]):
        seg = math.hypot(x2 - x1, z2 - z1)
        steps = int(seg * 3) + 1
        for i in range(0, steps + 1):
            t = i / steps
            sx, sz = math.floor(x1 + (x2 - x1) * t + 0.5), math.floor(z1 + (z2 - z1) * t + 0.5)
            if prev is not None and prev != (sx, sz) and prev[0] != sx and prev[1] != sz:
                if not (body_clear(prev[0], sz) or body_clear(sx, prev[1])):
                    return False
            prev = (sx, sz)
            if (sx, sz) in (a, b):
                continue
            if not body_clear(sx, sz):
                return False
            for yy in (top, top + 1):
                d = world.door_at(sx, yy, sz)       # an open door still has its panel on one side
                if d and (d[1] == "x" and abs(x2 - x1) > 0.01 or d[1] == "z" and abs(z2 - z1) > 0.01):
                    return False
    return True


def landing_entry_ok(world, p, q, top):
    """a jump that ends lower than it flies must get INTO the landing column: either straight down (open
    from the landing up to the flight height) or from the side, falling in front of it first and moving
    in under an overhang (the column before the landing open from that height up)"""
    qx, qy, qz = q
    if all(world.cls(qx, yy, qz)[0] for yy in range(qy + 2, top + 2)):
        return True
    dist = math.hypot(p[0] - qx, p[2] - qz)
    ex, ez = math.floor(qx + (p[0] - qx) / dist + 0.5), math.floor(qz + (p[2] - qz) / dist + 0.5)
    if (ex, ez) == (qx, qz):
        return False
    for h in range(top - 1, qy - 1, -1):
        if all(world.cls(qx, yy, qz)[0] for yy in range(qy, h + 2)) and                 all(world.cls(ex, yy, ez)[0] for yy in range(h, top + 2)):
            return True
    return False


def jump_moves(world, p, stands_near):
    """Parkour jumps to standing spots up to 5 blocks away (gap limits by height change)."""
    x, y, z = p
    if not world.cls(x, y + 2, z)[0]:
        return []
    out = []
    for q in stands_near:
        qx, qy, qz = q
        dy = qy - y
        dist = math.hypot(qx - x, qz - z)
        if dist < 2:
            continue
        gap = dist - 1
        if dy >= 2 or dy < -6:
            continue
        lim = 3 if dy == 1 else 4 if dy in (0, -1) else 5
        if gap > lim + 0.01:
            continue
        # headroom along the arc: the body passes at the higher of the two heights
        top = max(y, qy)
        if not landing_entry_ok(world, p, q, top):
            continue
        ok = clear_path(world, [(x, z), (qx, qz)], top, (x, z), (qx, qz))
        if not ok and gap <= 3.01 and dy <= 0:
            # neo: curve around a wall, through a point 1-2 blocks to the side of the straight line
            mx, mz = (x + qx) / 2, (z + qz) / 2
            vx, vz = (qx - x) / dist, (qz - z) / dist
            for side in (1, -1, 2, -2):
                pm = (mx - vz * side, mz + vx * side)
                if clear_path(world, [(x, z), pm, (qx, qz)], top, (x, z), (qx, qz)):
                    ok = True
                    break
        if ok:
            out.append(q)
    return out


def analyse(world, targets, max_fall=20):
    # enumerate standing cells in the box
    stands = set()
    for x in range(-world.m, world.w + world.m):
        for z in range(-world.m, world.d + world.m):
            if world.outside(x, z):
                y = world.g + 1
                if world.can_stand(x, y, z):
                    stands.add((x, y, z))
                continue
            for y in range(0, world.h + 1):
                if world.can_stand(x, y, z):
                    stands.add((x, y, z))
    # spatial buckets for jump candidates
    buckets = {}
    for s in stands:
        buckets.setdefault((s[0] // 6, s[2] // 6), []).append(s)

    def near(p):
        bx, bz = p[0] // 6, p[2] // 6
        for i in (-1, 0, 1):
            for j in (-1, 0, 1):
                for q in buckets.get((bx + i, bz + j), ()):
                    if world.openable(*q) or world.openable(q[0], q[1] + 1, q[2]):
                        continue           # never parkour-land inside a door / gate / trapdoor cell
                    if abs(q[0] - p[0]) <= 5 and abs(q[2] - p[2]) <= 5 and -6 <= q[1] - p[1] <= 1:
                        yield q

    graph = {}
    for s in stands:
        nb = set(m for m in moves(world, s, max_fall) if m in stands)
        nb.update(jump_moves(world, s, list(near(s))))
        graph[s] = nb
    exits = {s for s in stands if world.outside(s[0], s[2])}

    def bfs(starts, g):
        seen = set(starts); dq = deque(starts)
        while dq:
            c = dq.popleft()
            for n in g.get(c, ()):
                if n not in seen:
                    seen.add(n); dq.append(n)
        return seen

    reach = bfs(exits, graph)
    rev = {}
    for a, ns in graph.items():
        for b in ns:
            rev.setdefault(b, set()).add(a)
    can_exit = bfs(exits, rev)

    def interact_cells(t):
        tx, ty, tz = t
        res = []
        for s in reach:
            ex, ey, ez = s[0] + 0.5, s[1] + 1.6, s[2] + 0.5
            cx, cy, cz = tx + 0.5, ty + 0.5, tz + 0.5
            dist = math.dist((ex, ey, ez), (cx, cy, cz))
            if dist > 4.5:
                continue
            n = max(2, int(dist * 3))
            ok = True
            for i in range(1, n):
                k = i / n
                px, py, pz = math.floor(ex + (cx - ex) * k), math.floor(ey + (cy - ey) * k), math.floor(ez + (cz - ez) * k)
                if (px, py, pz) == (tx, ty, tz):
                    break
                if not world.cls(px, py, pz)[0]:
                    ok = False
                    break
            if ok:
                res.append(s)
        return res

    report = {"standing_cells": len(stands), "reachable": len(reach), "targets": {}, "traps": [], "stranded": []}
    core_cells = set()
    for name, t in targets.items():
        cells = interact_cells(t["pos"])
        report["targets"][name] = {"pos": t["pos"], "reachable": bool(cells), "kind": t.get("kind", "")}
        if name == "core" or t.get("kind") == "core":
            core_cells |= set(cells)
    traps = sorted(reach - can_exit)
    report["traps"] = cluster(traps)
    if core_cells:
        to_core = bfs(core_cells, rev)
        report["stranded"] = cluster(sorted(reach - to_core))
    return report


def cluster(cells, limit=12):
    """Group cells into connected blobs; report size and a representative position."""
    cells = set(cells)
    groups = []
    while cells:
        c = cells.pop(); blob = [c]; dq = deque([c])
        while dq:
            a = dq.popleft()
            for dx in (-1, 0, 1):
                for dy in (-1, 0, 1):
                    for dz in (-1, 0, 1):
                        b = (a[0] + dx, a[1] + dy, a[2] + dz)
                        if b in cells:
                            cells.remove(b); blob.append(b); dq.append(b)
        blob.sort()
        groups.append({"cells": len(blob), "example": list(blob[len(blob) // 2])})
    groups.sort(key=lambda g: -g["cells"])
    return groups[:limit]


def auto_targets(blocks, bents):
    t = {}
    seals = 0
    for p, (name, props) in blocks.items():
        n = short(name)
        if name.endswith("amethyst_core") or n == "amethyst_core":
            t["core"] = {"pos": list(p), "kind": "core"}
        elif n == "amethyst_seal":
            seals += 1
            mode = props.get("mode", "touch")
            t[f"seal{seals}"] = {"pos": list(p), "kind": f"seal ({mode})"}
    for p, e in bents.items():
        items = e.get("Items") or []
        if any("amethyst_key" in str(i.get("id", "")) for i in items if isinstance(i, dict)):
            t[f"key@{p[0]},{p[1]},{p[2]}"] = {"pos": list(p), "kind": "key chest"}
    return t


def toggle_doors(blocks, positions):
    """copy of blocks with each puzzle door (both halves) / trapdoor / gate flipped open<->closed"""
    nb = dict(blocks)
    for (x, y, z) in positions:
        st = blocks.get((x, y, z))
        if not st or "open" not in st[1]:
            sys.exit(f"--puzzle-door {x},{y},{z}: no door, trapdoor or fence gate there ({st})")
        cells = [(x, y, z)]
        if short(st[0]).endswith("_door"):
            other = (x, y + 1, z) if st[1].get("half") == "lower" else (x, y - 1, z)
            if blocks.get(other) and blocks[other][0] == st[0]:
                cells.append(other)
        for c in cells:
            name, props = blocks[c]
            props = dict(props)
            props["open"] = "false" if props.get("open") == "true" else "true"
            nb[c] = (name, props)
    return nb


def print_phase(rep, title, names, check_stranded):
    print(f"--- {title}: standing spots {rep['standing_cells']}, reachable from outside {rep['reachable']}")
    ok = True
    for name in names:
        v = rep["targets"][name]
        if v["reachable"]:
            flag = "OK "
        elif "redstone" in v["kind"]:
            flag = "-- (redstone seal: reach not needed)"
        else:
            flag = "FAIL"
            ok = False
        print(f"  [{flag}] {name} {v['kind']} at {v['pos']}")
    if rep["traps"]:
        ok = False
        print("  [FAIL] traps: places a player can reach but never leave (size, example x,y,z):")
        for g in rep["traps"]:
            print(f"         {g['cells']:5d} cells around {g['example']}")
    else:
        print("  [OK ] no traps: every reachable place can get back outside")
    if check_stranded:
        if rep["stranded"]:
            ok = False
            print("  [FAIL] stranded: reachable places with no way back to the core:")
            for g in rep["stranded"]:
                print(f"         {g['cells']:5d} cells around {g['example']}")
        elif "core" in rep["targets"]:
            print("  [OK ] every reachable place can get back to the core")
    return ok


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("template")
    ap.add_argument("--ground-y", type=int, required=True)
    ap.add_argument("--target", action="append", default=[], help="name=x,y,z (extra things the player must reach)")
    ap.add_argument("--puzzle-door", action="append", default=[], metavar="X,Y,Z",
                    help="door/trapdoor/fence gate the puzzle toggles when solved (repeatable)")
    ap.add_argument("--max-fall", type=int, default=20)
    ap.add_argument("--json")
    a = ap.parse_args()
    size, blocks, bents = load_template(a.template)
    targets = auto_targets(blocks, bents)
    extra = []
    for spec in a.target:
        name, xyz = spec.split("=")
        targets[name] = {"pos": [int(v) for v in xyz.split(",")], "kind": "extra"}
        extra.append(name)
    print(f"template {size[0]}x{size[1]}x{size[2]}")
    if not a.puzzle_door:
        rep = analyse(World(size, blocks, a.ground_y), targets, a.max_fall)
        ok = print_phase(rep, "template as stored", list(targets), True)
        out = rep
    else:
        doors = [tuple(int(v) for v in d.split(",")) for d in a.puzzle_door]
        before = analyse(World(size, blocks, a.ground_y), targets, a.max_fall)
        after = analyse(World(size, toggle_doors(blocks, doors), a.ground_y), targets, a.max_fall)
        ok1 = print_phase(before, "BEFORE solving (puzzle doors as stored)", extra, False)
        ok2 = print_phase(after, "AFTER solving (puzzle doors toggled)", [n for n in targets if n not in extra], True)
        ok = ok1 and ok2
        out = {"before": before, "after": after}
    print("RESULT:", "PASS" if ok else "FAIL")
    if a.json:
        json.dump(out, open(a.json, "w"), indent=1)
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
