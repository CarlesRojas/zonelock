---
name: minecraft-builder
description: Building craft for designing Minecraft structures that look hand-made by a skilled builder instead of flat boxes - depth in walls, block palettes and gradients, roofs with overhangs and corbels, arches from upside-down stairs, windows, columns, towers, interiors, lighting, terrain blending, ruins and dungeon layout. Use it whenever you design, generate, review or improve any Minecraft build in any form - structure templates (.nbt), jigsaw pieces, worldgen dungeons, schematics, block lists, generator code, or a plan in words - for castles, houses, temples, towers, crypts, ruins, dungeons, shrines or anything else, even if the user only says "make a dungeon" or "build X".
---

# Minecraft builder

The default way an AI builds in Minecraft is a hollow box of one block with a hole for a door.
Players read that instantly as amateur. Skilled builders follow a small set of habits that make
the same footprint look crafted: shape before detail, depth on every wall, a deliberate palette,
roofs and arches made from partial blocks, light and props that tell a story. This skill is
those habits, gathered from builder guides, and a workflow to apply them to anything.

It is about *what to build*, not about one output format. Use it with whatever the project needs
(template files, generator code, block lists). Format notes are at the end.

## Reference files (read what the design needs)

| File | Read when |
|---|---|
| `references/walls-texture-palettes.md` | Always. Depth, mixing blocks, gradients, palettes, proportions. |
| `references/roofs-arches-details.md` | Anything with a roof, arch, window, door, column, tower or battlement. Exact stair states and grids. |
| `references/interiors.md` | Every structure with rooms. Composition, wall bands, ceilings, floors, themed room recipes, three-layer lighting, example plans. |
| `references/interiors-lighting-terrain.md` | Furniture table, light values, foundations, landscaping, weathering and ruins. |
| `references/parkour.md` | Any parkour: footholds that are part of the architecture, tower and library patterns, movement blocks, fail states with separate return routes, anti-skip audit. |
| `references/challenges.md` | Any path with a challenge: parkour (jump limits, tiers, pacing, safe falls), redstone puzzles (mechanics, robust pre-built wiring, designs), stealth, mazes, arenas, swims and more. |
| `references/dungeons-worldgen.md` | Dungeons, adventure structures, anything generated in worlds: pacing, traps, jigsaw, air vs structure void, processors. |

## Workflow

Do the steps in order. Each step is cheap to change and expensive to change later, which is why
experienced builders never decorate before the shape works.

### 1. Brief
Write 3-5 lines before any block: what the place is, who built it, what happened to it, what the
player should feel, and the environment it sits in. "A crypt dug by a cult under a cliff,
half-flooded and abandoned, eerie but readable" gives you palette, lighting, decay and layout
decisions for free. A brief with a story beats a list of features.

**Recreating or reworking an existing build** (an older version, a screenshot, a structure someone
already likes): start from its blocks, not from a blank grid. Keep its size, silhouette, materials
and props, and change only what a rule forces (an item-based puzzle, an unplayable jump, a block that
breaks on placement). A redesign "applying the skill" grows it, squares it off and loses the
details that made people like it. Before calling it done, place the old and the new side by side in
the game, shoot both from the same angles at the same distance, and compare.

### 2. Massing and silhouette
- Combine 3-4 volumes of different heights and sizes, never one box. Pick one focal mass at least
  25 % bigger than the rest.
- Use odd widths (5, 7, 9, 11...) so doors, windows, arches and roof ridges have a centre.
- Build bigger than feels necessary: detail needs room, and cramped rooms cannot hold depth,
  furniture and light.
- Sketch the plan as ASCII per level (top view) and one elevation (side view). Check the
  silhouette: filled solid, is the outline still interesting?

### 3. Palette
Choose 3-5 blocks with roles: primary (walls), secondary (frame or roof), accent (trim), plus
floor and base. Order them by value (light to dark) and make sure roof and walls differ in value.
One accent colour only. Check every block you plan to use for stairs and slabs has those forms.
Start from a proven palette in `walls-texture-palettes.md` and adapt it to the brief and biome.

### 4. Structure and depth
Frame first: pillars, beams, floor lines, buttresses, a plinth. Frame 1 block proud, infill at 0,
glass 1 back. Every wall wider than ~5 needs a change of plane. Heaviest at the bottom.

### 5. Roof and openings
Roofs with overhang, eave corbels and underside fill; arches from upside-down stairs; windows
recessed with sills, heads and shutters; the entrance as the most detailed spot of the facade.
Use the exact states in `roofs-arches-details.md`.

### 6. Texture, gradient, weathering
Mix variants in clusters (60-70 / 20-30 / 5-10), never 50/50 and never uniform speckle. Dark and
rough at the base, lighter at the top. Put wear where water and damage would put it, with a
direction, not as even noise.

### 7. Interior, light, props
Furniture from partial blocks, ceiling beams, floor borders and inlays, clutter grouped into work
zones that say who used the room. Hide or style light sources; decide per room whether it should
be safe (no block light 0) or dangerous. Light the goal brighter than the path.

### 8. Ground and surroundings
Foundation that flares and goes below grade, ragged ground gradients, paths, vegetation. For
generated structures, decide what is air (carves terrain) and what is structure void (keeps
terrain). To let a building mark the land around it (a tower "eating" the ground, a crypt rotting
the grass), pad the template with structure void on every side and paint only the ground layer in
the padding: dense against the walls, thinning out with distance and noise, a few accents (crystals,
roots) on top, and keep the approach to the door clear. The terrain everywhere else stays untouched.
A fixed template layer only works on flat ground (on a slope the hill buries it on one side and it
floats on the other), so in Zone Lock mark each of those blocks with the template data
`{"zonelock_surface": layer}` (0 replaces the top of the ground, 1 sits on it, -1 goes under it) and put
`"processors": "zonelock:surface_snap"` on the dungeon's pool element: the mod's processor lays them on
the real ground of their column when the structure generates (verified on hillsides, nothing buried
or floating). Vanilla's `minecraft:gravity` would move the whole building, not just the marked blocks.

### 9. Playability check (mandatory for anything a player walks into)
Run the bundled checker on the final template, after the very last generator pass:
```
python <skill>/scripts/check_paths.py structure.nbt --ground-y <template y of the terrain surface>
```
It simulates a survival player who cannot place or break blocks (walking, 1-block step-ups,
stairs, survivable drops, ladders and vines, swimming, parkour jumps within real limits including
neos, wooden doors, iron doors only next to a button or lever) and reports:
- whether the core, every seal and every key chest can be reached from outside (pass more with
  `--target name=x,y,z`, e.g. levers and buttons of a puzzle);
- **traps**: places a player can reach but never leave (a one-way drop with no way back, a door
  that only opens from one side, a pit). Every reachable place must be able to get back outside
  without switching the core off;
- **stranded** places with no way back to the core.

Doors follow their real hitbox: a door blocks only movement across its panel, and an open door
blocks the other axis than a closed one. **If a puzzle opens the way on** (an iron door, trapdoor
or fence gate its redstone toggles), pass it with `--puzzle-door x,y,z`: the check then runs twice,
BEFORE solving (the `--target` buttons must be reachable, no traps, and the gate must really hold)
and AFTER solving (door toggled: core, seals, key chests, no traps, nothing stranded). Without it a
puzzle-held door reads as a wall, or worse as a free way round the puzzle.

Fix every FAIL and run it again until it says PASS. Typical causes: a late pass (floor, wall,
roof, terrain) overwrote a carved stair or doorway; a drop into a room with no exit; an iron door
with its button on the wrong side; a parkour fall basin with no way back; a window reveal or
niche closed off by a pillar or furniture in front of it (a 1x1 pocket only reachable by a
diagonal jump). To find out why something fails, trace one route by hand from the report's example
cell: which move gets in, and why none gets out. It is a model, not a proof: still test in the game
when you can.

### 10. Critique pass
Review the finished design against the checklist below, by looking at it, not by remembering what
you intended. When you build by coordinates you cannot see the result, so print it: an ASCII top
view of every level and an ASCII front view of every facade, generated from the actual block data.
Read those prints the way a player would see them and fix what fails. Do at least one full fix
round; the first version always has flat spots, mis-facing stairs and empty rooms.

## Critique checklist

Shape
- [ ] Not a single box; varied heights; a clear focal mass; interesting silhouette.
- [ ] Odd widths where things need a centre; door under the ridge or on an axis.

Walls
- [ ] No flat wall wider than ~5 blocks; frame proud of infill; glass recessed.
- [ ] Plinth / heavier base; details have a reason to exist and touch along edges.
- [ ] Texture mixed in clusters with a gradient; no checkerboard, no speckle, no straight bands.
- [ ] Wear stays within each material's family (wood ages with other woods, stone with cracked,
      mossy and brick variants); no stone patches on a wooden roof.

Palette
- [ ] 3-5 blocks with roles; roof and walls contrast in value; one accent; at most two woods.

Roof and openings
- [ ] Overhang with corbels and underside fill; ridge capped; big roofs split into several.
- [ ] Arches built from upside-down stairs, symmetric, contrasting with the wall.
- [ ] Every stair has the right `facing`, `half` and corner `shape`; fences, walls, panes and bars
      have their connection states.

Inside
- [ ] Ceilings 4+ in normal rooms, higher in halls; varied heights.
- [ ] No bare floor 5x5 or bigger unless it is an arena; props grouped by purpose.
- [ ] Lighting intentional: styled or hidden sources, contrast, safe vs dangerous decided.

Ground and age
- [ ] Sits in the terrain (foundation below grade, ground gradient); nothing floats.
- [ ] Decay has a direction; 70-85 % of the original form still readable; rubble under gaps.

Playability
- [ ] `scripts/check_paths.py` says PASS on the final template: every target reachable, no traps,
      no stranded areas.
- [ ] Puzzles use redstone inputs only (buttons, levers, pressure plates, tripwire, bells, note
      blocks); no item has to be found, carried, inserted or kept.
- [ ] Every fall zone lands somewhere soft and returns by a separate route, with no ladder, vine or
      scaffolding in the fall line.

Dungeons (if applicable)
- [ ] Compression and release; a landmark in the first view of the big rooms; loot curve; 2+
      levels; no pure grid.
- [ ] Every trap fair, every puzzle with a clue; redstone complete inside the template.
- [ ] Air vs structure void decided; modules finished where they join.

## Common traps for AI builders

- **Decorating a box.** Adding torches and a carpet to a hollow cube does not fix it. Go back to
  massing and depth.
- **Detail everywhere.** Even detail is as flat as no detail. Concentrate it at the entrance, the
  focal mass and the goal room; keep calmer surfaces between.
- **Symmetric everything.** Formal rooms can be symmetric. Whole layouts, ruins and landscapes
  should not be.
- **Random as texture.** Per-block random choice gives salt-and-pepper noise. Grow clusters from a
  few seed points instead, and follow the gradient.
- **Trusting intent over output.** Stair orientation is the most common bug. Print elevations and
  check the arches and roofs look like the grids in the reference.

## Output notes

- Block ids are Java ids (`minecraft:stone_bricks`). Before relying on a block that might not exist
  in the target version, check the project's game sources or registry: names change (e.g. the
  chain became `iron_chain` in 1.21.9).
- Structure templates do not recompute block states on placement: write stair shapes, fence and
  wall connections, door halves, bed parts and `persistent=true` on leaves yourself.
- Templates rotate and mirror at placement: directional blocks rotate with them, so never rely on a
  compass direction for meaning.
- Unset cells = structure void (terrain stays). Explicit `minecraft:air` carves terrain.
- **This project (Zone Lock, Minecraft 26.3):** dungeon templates live in
  `common/src/main/resources/resourcepacks/dungeons/data/zonelock/structure/`. In 26.x the template
  palette uses the keys `id` and `properties` (not the old `Name` / `Properties`, which load as all
  air) and DataVersion 5023. The mod's own blocks (core, seals) carry their config as block entity
  data; copy the format from an existing template rather than guessing.
