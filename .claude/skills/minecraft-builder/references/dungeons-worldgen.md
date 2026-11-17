# Dungeon design and worldgen

Read this when designing a dungeon, adventure structure or anything that generates on its own in
worlds (structure templates, jigsaw pools).

## Contents
1. Flow and pacing
2. Guiding the player
3. Traps and puzzles
4. Secrets and verticality
5. Spawner, boss and treasure rooms
6. Theme coherence
7. What makes vanilla structures memorable
8. Worldgen: templates, air vs structure void, jigsaw, processors
9. Mistakes checklist

---

## 1. Flow and pacing

- **Difficulty curve:** entrance (low threat, hints) -> corridors with minor traps -> spawner
  chambers and tighter spaces -> a final room that feels like a real threat. Best loot deepest.
- **Compression and release:** a narrow corridor (3x3 or 3x4, 8-15 long) opens into a reveal room
  2-4x taller. Frame a landmark in the very first view from the doorway (a statue, a waterfall, the
  sealed door, a glowing core).
- **Teach, test, combine:** show a mechanic safely, test it, then combine it with another.
- **Mix experiences:** dark cramped spaces next to bright open ones, puzzles, combat, key gates.
- **No pure grids or symmetry:** angled corridors, irregular rooms, slopes, drops, 2-3 levels.
  Symmetry is fine *inside* a formal room (a chapel, a throne room), not for the whole layout.
- **Dead ends** create tension and suggest secrets. A good dead end has a reward or a hint.
- **Size:** small ~30 blocks across, medium 50-80. Over 100 feels empty unless densely detailed.
  Underground dungeons around Y 16-40 avoid sunlight leaks (adjust for the target terrain).

## 2. Guiding the player

- Light, colour and contrast lead. Warm light on the true path; the goal is the brightest point.
- Material change signals importance: a copper doorway, chiseled blocks around key doors, a carpet
  runner, a gold or amethyst accent before the important room.
- Environmental clues instead of signs: a distant tower seen through a window, a broken bridge
  suggesting another route, a clue carved into the floor pattern.
- A landmark visible from several rooms (central shaft, waterfall, giant statue, a glowing core
  behind bars) keeps players oriented and builds anticipation.

## 3. Traps and puzzles (vanilla-buildable)

- Tripwire hook + tripwire + dispenser with arrows.
- Pressure plate -> piston crusher, closing wall, or sand / gravel dropped from the ceiling.
- Hidden lava under a false floor; TNT under a chest (as in desert and jungle temples).
- Sculk sensor doors (vibration-triggered piston doors; frequency filters with comparators).
- Puzzles: lever combinations, item frame rotation read by a comparator, lectern page, note block
  sequences, target blocks, key items opening a vault.
- **Fairness:** a trap should be noticeable to an attentive player and punish carelessness without
  one-hit kills. Every puzzle needs its clue somewhere in the dungeon.
- Redstone in a template must be complete and self-contained: every wire, repeater state and
  observer facing written exactly, and no redstone that depends on blocks outside the template.

## 4. Secrets and verticality

- Hidden rooms behind plain walls, a piston door under a bridge, a hidden bedroom with a chest, a
  vault in a thick wall.
- Leave tells for observant players: a window with no room behind it, a wall 1 block too thick, a
  cobweb where air would draft, one odd block in a pattern.
- Verticality: spiral stairs, ladders to mezzanine walkways, balconies over arenas, drop-down
  shortcuts that loop back to the start.

## 5. Spawner, boss and treasure rooms

- **Spawners:** visible but not obvious (dark corner, raised platform, behind a grate), fenced so
  mobs cannot rush the player, a single-door bottleneck, and an exit route.
- **Arena / boss room:** clear floor 15-25 across, pillars for cover, a raised throne or altar as the
  focal point, a ring of light on the walls with darkness in the centre (or the reverse), one
  dramatic entrance, the reward visible behind the threat.
- **Treasure room:** the payoff. Most detail per block of the whole dungeon, brightest light,
  richest accent material, a clear focal object.

## 6. Theme coherence

Materials hint at the threat before mobs appear: bone blocks for undead, gold for temples,
sculk for the deep dark, cobwebs for spiders. One palette per dungeon, plus at most one accent per
zone. Copper next to sandstone next to prismarine with no reason reads as random.

## 7. What makes vanilla structures memorable

- **Ancient city:** huge scale, one strict palette (deepslate family, basalt, dark oak, soul
  light), grey wool floors that mute sound, a giant central landmark, a stealth mechanic, intact
  and ruined variants of the same pieces.
- **Trial chambers:** tuff bricks + copper, a corridor spine branching to chambers, an atrium with a
  grand staircase, a combat -> key -> vault loop, copper doors gating areas.
- **Woodland mansion:** dark foyer with a grand stair, a grid of themed rooms, secret rooms with no
  entrance, found by spotting gaps between rooms.
- **Stronghold:** a crossing hub, branching corridors, dead-end fillers, libraries with cobwebs, the
  portal room placed far from the start.

The common thread: one strong palette, a landmark, a loop of challenge and reward, and variants of
modules so it never feels copy-pasted.

## 8. Worldgen

### Air vs structure void (most important rule for buried structures)
- **Air in a template overwrites the world** (it carves). **Structure void is not saved**, so the
  terrain that was there stays.
- Buried dungeon: room interiors and corridors = air (to clear stone out); everything outside the
  walls = structure void, so terrain meets the walls seamlessly. A template padded with air carves
  ugly cubic holes into hillsides.
- Surface ruins: voids around the rubble so grass and trees survive.
- `legacy_single_pool_element` does not place air at all.

### Templates
- The structure block UI saves at most 48x48x48. Split big halls into several pieces.
- Leaves need `persistent=true`. Waterlogged blocks follow the structure's `liquid_settings`.
- Block entities (chests with loot tables, spawners, signs, lecterns) carry their data in the
  template; entities (armor stands, item frames, paintings) go in the entity list.
- Templates rotate and mirror at placement: blocks with direction (stairs, doors, trapdoors, logs,
  redstone) rotate with them, so design in one orientation and let the game handle the rest. Never
  rely on a compass direction for meaning (e.g. "light from the south").

### Jigsaw (modular, multi-piece structures)
- Two jigsaws connect when one's *target name* equals the other's *name*; the attached piece comes
  from the *target pool* (weights 1-150). If nothing fits, the pool's `fallback` is used; that is
  how corridor ends get capped.
- Put jigsaw blocks on the boundary layer of the piece, in the doorway, facing out, so the two
  pieces' bounding boxes touch without overlapping. Overlapping pieces are rejected (except a piece
  fully nested inside its parent).
- `final_state` = `minecraft:air` for open doorways, or the wall block when a sealed end is fine.
- `selection_priority` / `placement_priority` force the main path to generate before side rooms.
- Joints: `aligned` for vertical shafts and stairs where orientation matters, `rollable` otherwise.
- Structure fields: `start_pool`, `size` (depth, 0-20), `max_distance_from_center` (max 128),
  `start_height` + `project_start_to_heightmap`, `start_jigsaw_name`, `pool_aliases` (theme
  variants), `liquid_settings`.
- Projection: `rigid` for rooms, `terrain_matching` for surface paths and roads.
- `terrain_adaptation`: `beard_thin` (ground built up under, villages), `beard_box` (ancient city),
  `bury` (wrapped in terrain, strongholds), `encapsulate` (sealed solid shell, trial chambers). For
  underground dungeons `encapsulate` or `bury` stops caves gutting the rooms.

Modular design rules:
- Standardise doorway sizes (e.g. 3 wide x 4 tall, centred on the piece edge) and corridor widths.
- Name connection types (`zonelock:corridor_3x4`, `zonelock:shaft`) so pieces link only where they
  should.
- Make rooms multiples of the corridor module.
- Rare weighted special rooms (secret, treasure) at low weight; `empty_pool_element` to sometimes end
  branches.
- Each module must look finished from every side a player can see it from, including where it
  meets another module: a doorway frame on both sides, not a raw hole.

### Processors (per-copy variation)
- `minecraft:block_age` {mossiness: 0.3}.
- `minecraft:block_rot` {integrity: 0.85, rottable_blocks: tag}: removed blocks keep the world's
  block, so terrain shows through.
- `minecraft:rule` with `random_block_match`: e.g. stone bricks -> cracked at 0.2, then mossy at
  0.3 (the first matching rule wins).
- Position predicates (`linear_pos`, `axis_aligned_linear_pos`) make decay heavier further from the
  start.
- `capped` limits how many blocks a processor changes; `protected_blocks` (or rule ordering) keeps
  chests, spawners and mod blocks untouched.
- `block_ignore` skips chosen blocks at placement.

Processors give noise; they cannot give directional damage or story. Do both: hand-design the
meaningful damage, let processors vary the rest.

## 9. Mistakes checklist

1. Grid-perfect symmetric layouts with no reveal room, no verticality, no landmark, no loot curve.
2. Unfair traps (invisible one-hit kills), puzzles with no clue.
3. Templates padded with air carving cubic holes; leaves decaying.
4. Jigsaw doorways that do not line up; overlapping pieces; no cap pieces, so corridors end open
   into stone.
5. Modules that look raw where they join.
6. Mixed themes with no reason.
7. Redstone that only works in one rotation or relies on blocks outside the template.
