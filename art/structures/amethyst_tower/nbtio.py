"""Minimal structure-template IO + a Build grid. Palette keys id/properties (MC 26.x)."""
import gzip, io, struct, random

DV = 5023


def norm(n):
    return n if ":" in n else "minecraft:" + n


def parse(s):
    s = s.strip()
    p = {}
    if "[" in s:
        n, r = s.split("[", 1)
        for part in r.rstrip("]").split(","):
            if part.strip():
                k, v = part.split("=", 1)
                p[k.strip()] = v.strip()
    else:
        n = s
    return norm(n), p


def sstr(n, p):
    return n if not p else n + "[" + ",".join(f"{k}={v}" for k, v in sorted(p.items())) + "]"


def canon(s):
    return sstr(*parse(s))


def bid(s):
    return parse(s)[0]


class Build:
    def __init__(s, w, h, d):
        s.w, s.h, s.d = w, h, d
        s.blocks = {}
        s.nbt = {}
        s.entities = []

    def inside(s, x, y, z):
        return 0 <= x < s.w and 0 <= y < s.h and 0 <= z < s.d

    def set(s, x, y, z, b, nbt=None):
        if not s.inside(x, y, z):
            return
        if callable(b):
            b = b(x, y, z)
        if b is None:
            s.blocks.pop((x, y, z), None); s.nbt.pop((x, y, z), None); return
        s.blocks[(x, y, z)] = canon(b)
        if nbt is not None:
            s.nbt[(x, y, z)] = nbt
        else:
            s.nbt.pop((x, y, z), None)

    def get(s, x, y, z):
        return s.blocks.get((x, y, z))

    def box(s, x1, y1, z1, x2, y2, z2, b):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    s.set(x, y, z, b)

    def save(s, path):
        pal, idx, bl = [], {}, []
        for p in sorted(s.blocks, key=lambda p: (p[1], p[2], p[0])):
            st = s.blocks[p]
            if st not in idx:
                idx[st] = len(pal); pal.append(st)
            e = {"pos": ("li", list(p)), "state": ("i", idx[st])}
            if p in s.nbt:
                e["nbt"] = s.nbt[p]
            bl.append(e)
        pt = []
        for st in pal:
            n, p = parse(st)
            e = {"id": n}
            if p:
                e["properties"] = dict(p)
            pt.append(e)
        root = {"DataVersion": ("i", DV), "size": ("li", [s.w, s.h, s.d]), "palette": pt, "blocks": bl,
                "entities": s.entities}
        buf = io.BytesIO(); buf.write(b"\x0a"); _ws(buf, ""); _wc(buf, root)
        with gzip.open(path, "wb") as f:
            f.write(buf.getvalue())
        print(f"saved {path} {s.w}x{s.h}x{s.d} blocks={len(s.blocks)} states={len(pal)} be={len(s.nbt)}")


def load(path):
    f = io.BytesIO(gzip.open(path).read())
    t = f.read(1)[0]; _rs(f); root = _rp(f, t)
    w, h, d = root["size"]
    b = Build(w, h, d)
    states = [sstr(e.get("id") or e.get("Name"), e.get("properties") or e.get("Properties") or {}) for e in root["palette"]]
    for e in root["blocks"]:
        p = tuple(e["pos"]); b.blocks[p] = states[e["state"]]
        if "nbt" in e:
            b.nbt[p] = e["nbt"]
    b.entities = root.get("entities", [])
    return b


def _ws(buf, s):
    x = s.encode(); buf.write(struct.pack(">H", len(x))); buf.write(x)


def _tt(v):
    if isinstance(v, tuple):
        return {"b": 1, "s": 2, "i": 3, "l": 4, "f": 5, "d": 6, "li": 9, "str": 8}[v[0]]
    if isinstance(v, bool): return 1
    if isinstance(v, int): return 3
    if isinstance(v, float): return 6
    if isinstance(v, str): return 8
    if isinstance(v, list): return 9
    if isinstance(v, dict): return 10
    if isinstance(v, bytes): return 7
    raise TypeError(type(v))


def _wp(buf, v):
    t = _tt(v)
    if isinstance(v, tuple):
        k, val = v
        if k == "li":
            buf.write(b"\x03"); buf.write(struct.pack(">i", len(val)))
            for i in val: buf.write(struct.pack(">i", i))
            return
        v = val
    if t == 1: buf.write(struct.pack(">b", int(v)))
    elif t == 2: buf.write(struct.pack(">h", v))
    elif t == 3: buf.write(struct.pack(">i", v))
    elif t == 4: buf.write(struct.pack(">q", v))
    elif t == 5: buf.write(struct.pack(">f", v))
    elif t == 6: buf.write(struct.pack(">d", v))
    elif t == 7: buf.write(struct.pack(">i", len(v))); buf.write(v)
    elif t == 8: _ws(buf, v)
    elif t == 9:
        if not v: buf.write(b"\x00" + struct.pack(">i", 0)); return
        buf.write(bytes([_tt(v[0])])); buf.write(struct.pack(">i", len(v)))
        for i in v: _wp(buf, i)
    elif t == 10: _wc(buf, v)


def _wc(buf, d):
    for k, v in d.items():
        buf.write(bytes([_tt(v)])); _ws(buf, k); _wp(buf, v)
    buf.write(b"\x00")


def _rs(f):
    n = struct.unpack(">H", f.read(2))[0]; return f.read(n).decode()


# reader keeps tag types for round-trip of block-entity nbt
def _rp(f, t):
    if t == 1: return ("b", struct.unpack(">b", f.read(1))[0])
    if t == 2: return ("s", struct.unpack(">h", f.read(2))[0])
    if t == 3: return struct.unpack(">i", f.read(4))[0]
    if t == 4: return ("l", struct.unpack(">q", f.read(8))[0])
    if t == 5: return ("f", struct.unpack(">f", f.read(4))[0])
    if t == 6: return struct.unpack(">d", f.read(8))[0]
    if t == 7: n = struct.unpack(">i", f.read(4))[0]; return f.read(n)
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
    raise ValueError(t)
