# Parkour built into the architecture

Read this for any parkour section. `challenges.md` section 1 has the raw jump limits and
difficulty tiers; this file is how to make parkour that belongs to the building, plus fail states,
return paths and anti-skip rules. "Floating blocks over water" is what the parkour community calls
a design failure: avoid it.

## Contents
1. The rule
2. Catalogue: architectural elements as footholds
3. Vertical parkour in a tower
4. Movement blocks (exact behaviour)
5. Fail states and return routes
6. Anti-skip audit
7. Example layouts
8. Mistakes and checklist

---

## 1. The rule

**Every foothold has an in-world reason to exist**: a beam end, a shelf, a sconce, a broken
stair, a chandelier ring, a window sill, a balcony, a pipe bracket, a fallen column. If you cannot
say what a block *is*, it is a floating block. Design the building first, then decide where the
route runs through it (the room is the course: a library is climbed by its shelves, a barn through
its rafters, a tower by its broken stair).

- Parkour style matches the building: towers are scaled from inside, halls are crossed by rafters
  and chandeliers, ruins by collapsed floors, mines by beams and scaffolding.
- Vary the rhythm: not evenly spaced copies of the same jump. Mix shelf tops, slabs, trapdoor
  ledges, chandelier rings, beam runs.
- Wall-attached footholds blend better than free-standing ones.
- Use theme blocks with the right hitbox instead of exotic ones (a slab, trapdoor or carpet instead
  of a cake or repeater). What makes a jump interesting is the movement it asks for, not a strange
  block.
- Decorate the areas the route does not cross too, but never let a set piece outshine the route.

## 2. Catalogue: architectural elements as footholds

| Element | Parkour role | Notes |
|---|---|---|
| Rafters / floor joists (1-wide logs) | balance runs, gaps where beams are missing | leave every 2nd-3rd rafter missing for 1-3 gaps |
| Beam ends out of walls | wall-run footholds along a trim line | read as structure |
| Bookshelves of uneven height | climbing shelf stacks, jumping between tops | slab or trapdoor "shelf lip" as the ledge |
| Furniture (stair chairs, slab tables, beds) | low first steps | gentle opening jumps |
| Chandeliers | land on a slab or trapdoor ring around the chain | never on the chain itself |
| Collapsed floors | remaining floor pieces are islands | the floor below is the fall zone |
| Broken spiral staircase | missing treads are the gaps | the most natural tower parkour |
| Window sills, balconies, arrow-slit ledges | ledges; open trapdoor shutters as thin ledges | |
| Pipes and brackets (copper, bars, walls) | wall tops as rails (1.5 tall: jump on from a higher surface) | copper grate walkways in trial-chamber style |
| Scaffolding / repairs | climbable column with a top platform | ruins under repair, mines |
| Banners, signs, frames | route markers, never footholds | banner above each checkpoint |
| Heads on pikes, pots on shelves | mini-block jumps (0.5 / 0.375 wide) | sparingly, where they fit |
| Roofs, dormers, chimneys | ridge runs, half steps on stairs | |

Trial chambers are a good model: spiral stairs, ladders, trapdoor shafts, grate walkways, chain
chandeliers with grate platforms, and two intertwined spiral stairs that never connect, which is
exactly a jump route plus a separate return route in one tower.

## 3. Vertical parkour in a tower (5x5 to 9x9 interior)

Fair distances for ordinary players (stay well below the expert maxima): level gaps 1-3 (4 only for
an expert crux with run-up), +1 gaps 1-2 (3 rarely), downward up to 3-4. Diagonals are harder than
they look. Keep at least 3 blocks of air above every take-off and landing unless a head-hitter is
intended: low ceilings in towers create accidental head-hitters, the most common tower bug.

Patterns:
1. **Wall-hugging spiral (5x5-7x7):** footholds out of the walls, +1 per jump, turning at corners;
   about 12 jumps and 12 blocks of height per lap in a 5x5.
2. **Zig-zag between opposite walls (5 wide):** 3-gaps from shelf to shelf: a library stack climb.
3. **Central column (7x7-9x9):** a pillar, flue or chain-wrapped mast with ledges on it and on the
   outer wall; neos around the column's corners are the hard beats; the column hides the route
   above so players look up.
4. **Ruined staircase:** build a real spiral stair, remove treads (1 missing = easy, 2 = harder),
   leave rubble, cobwebs and mossy slabs on the surviving treads.
5. **Library tower:** shelf walls with missing books, slab/trapdoor shelf edges, lectern or
   chiseled-shelf ledges, a chandelier as a mid-shaft island.

Short ladder runs (2-4 high) can link sequences, but only outside every fall column (section 5).
**Rest landings** at least every 8-12 vertical blocks: a full floor 3x3+ touching the wall, lit and
marked (sconce, banner, different floor), which is the checkpoint and the start of its own return
route.

## 4. Movement blocks (exact behaviour)

- **Ladder:** 0.1875 thick; climb by pushing or holding jump; sneak to hold. Entering a ladder
  space cancels fall speed and fall damage, so a ladder in a fall column is a free catch.
- **Trapdoor:** flat 0.1875 plate or vertical panel; an open trapdoor directly above a ladder with
  the same facing is climbable (verify in game).
- **Vines** (also twisting, weeping, cave vines): climbable even hanging free; cancel all fall
  damage; no collision.
- **Scaffolding:** jump inside to rise, sneak to descend, solid top; sneaking landing negates fall
  damage.
- **Fences** (0.25 post) and **walls** (0.5 post) are 1.5 tall: cannot be jumped onto from level
  ground, only from a surface 0.5+ higher. Panes and bars are 0.125 rails; a lone pane post is a
  near-impossible landing.
- **Chains are not climbable** and have a thin hitbox: decoration, or a precision landing at most.
- Small blocks: flower pot 0.375 wide and tall, head 0.5x0.5, anvil 0.75 tall, bed 0.5625 tall.
- **Soul sand** slows run-ups; **blue/packed ice** is slippery and does not melt.
- **Slime** bounces and cancels fall damage (sneak to stop); never in a fall zone (it throws players
  back up the shaft onto later ledges).
- **Honey** walls slow a slide down; landing on honey takes 20 % damage; jumping off it is harder.
- **Powder snow**: falls over 2.5 blocks go through even with leather boots and you freeze inside;
  not a universal safe landing. Prefer water.
- **Bubble columns** (source water over soul sand up, magma down): up columns are elevators; never
  let one bypass parkour; using one as a return lift that exits only onto the checkpoint is fine.
- **Water** (1 source block deep is enough), cobweb, sweet berries, powder snow negate fall damage;
  hay 20 %.
- **Pointed dripstone** tips hurt more: never in a fall zone.

## 5. Fail states and return routes

Without commands the checkpoint is a **place** (the last rest landing). After a fall the player
must land safely and walk back to the start of the section they failed.

1. Every jump has a defined landing zone below: the floor of the level (fall at most ~3 blocks) or
   water (1 deep), hay, or powder snow with carpet.
2. **No climbable block in any column a player can fall through** (ladder, any vine, scaffolding,
   ladder-trapdoor). They catch the falling player, who then climbs back mid-route and skips, or
   hangs there confused. Put climbing aids outside fall columns, or behind bars or glass.
3. **The return route differs from the jump route:** stairs, a walkway or a corridor around the
   parkour, behind a wall, through a side door, or a separate stair shaft.
4. **The return route ends at the start of the failed section, never higher.** Close its upper end
   with a one-way element: a ledge 2+ high with no climb, a bar gate, a drop.
5. One-way drops (a plain shaft into water) finish a section and stop backtracking.
6. Narrow shafts end in 1 block of source water with a lit doorway at water level and a short stair
   out.
7. A rest landing every 6-10 jumps and always before the hardest jump; never two hard jumps in a
   row without a rest.
8. Readability: lit landings, one consistent foothold material, route banners; safe ground and fall
   zones must look different.

## 6. Anti-skip audit

Zone Lock stops block placing and breaking, ender pearls and elytra inside the zone, but geometry
must do the rest.
- **Ceilings:** close the gap between wall tops and the roof (or keep it under 2 blocks) so nobody
  can walk along wall tops.
- **Audit every standable surface**, not only intended footholds: chair backs, chest tops, lantern
  tops, cornices, shelf tops near the ceiling. Any surface one jump from a later stage is a
  shortcut. Make decoration non-standable where needed (bars, panes, candles, cobwebs).
- Fall zones return to the start of the section, never the end.
- Decorative climbables are free elevators: use chains, glow lichen, moss carpet instead of vines
  or ladders where no climb is intended.
- Bypasses the mod may not block (test in 26.x): wind charges (self-launch ~7 high), riptide
  tridents in water or rain, chorus fruit (random teleport up to 8 blocks), boats and minecarts.
  Keep water shallow and covered, ceilings low, and 3+ blocks of solid wall between stages.
- Make the intended route obvious (light, banners, consistent foothold material) so nobody hunts
  for shortcuts.
- Verify with `scripts/check_paths.py`: it lists everything reachable; a later checkpoint that is
  reachable without the route is a skip, and a fall basin with no exit is a trap.

## 7. Example layouts

**Tower interior, 7x7, one wall unrolled** (`=` ledge or slab, `T` trapdoor ledge, `H` ladder,
`C` chain, `L` lantern, `X` rest landing, `R` separate return shaft):
```
 y   N wall              E wall            S wall
30 |##########|##########|##########|
29 |XXXXXXX R |          |          |   landing 2 (checkpoint B), return hole R in its corner
28 |       .  |          |  H       |
27 |       .  |          |  H       |   short ladder from the S-wall ledge to the landing
26 |       .  |        T |  =       |
25 |       .  |   =      |          |   trapdoor/slab ledges on the E wall, +1 each
24 |       .  |=      C  |          |   chandelier chain: decoration, not a foothold
23 |       .  |       L  |          |
22 |  =    .  |          |          |   N-wall beam end
21 |XXXXXXX R |          |          |   landing 1 (checkpoint A)
```
The return shaft R is a walled 1x1 or 2x1 shaft beside the room, closed off from the jump space,
with water at the bottom and a door back to landing 1 only. Nothing climbable in column R.

**Library hall** (`B` bookshelf, `b` shelf with a slab/trapdoor lip = foothold, `=` rafter,
`C` chain, `O` chandelier slab ring, `W` water, `D` door, `>` return corridor, `X` checkpoint
balcony, `|` iron-bar screen):
```
y14 ############################################
y13  ===   ===       ===   ===        XXXXX      rafters (missing beams = gaps) to balcony X2
y12   .     .   C     .     .   C     X   |
y11         C   C               C     X   |
y10   b     C  OOO              OOO       |      chandelier rings: mid-hall islands
y9    B    OOO  L       b        L        |
y8    B     L          bB                 |
y7  XXB          b     BB                 |      X1 = start balcony / checkpoint 1
y1-6  shelf stacks
y0  WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW   reading pool = fall zone
    >>> D  return corridor under X1 (stairs, no ladders) ---> back up to X1 only
```

## 8. Mistakes and checklist

Mistakes: floating evenly spaced blocks; exotic off-theme blocks; set pieces outshining the
route; holes that allow skips; an easy jump followed by an extreme one with no checkpoint;
climbables in fall zones; accidental head-hitters; chains used as climbs; fences or walls as
landings from level ground; return paths through the jump route or past the checkpoint; dark
exitless pits; untested parkour.

Checklist per section:
- [ ] Every foothold is a named architectural element.
- [ ] Gaps: level 1-3, +1 up to 2 (3 rare), down up to 4; diagonals counted harder.
- [ ] 3+ blocks of headroom over take-offs and landings unless a head-hitter is intended.
- [ ] No ladder, vine, scaffolding or ladder-trapdoor in any fall column.
- [ ] Safe fall zone under every jump; separate return route to the section start only.
- [ ] Rest landing every 6-10 jumps and before each crux.
- [ ] Every non-route standable surface audited for skips; wall tops closed off.
- [ ] `check_paths.py` passes (route end reachable, fall zones not traps).
