# Interiors, lighting, terrain and ruins

Read this when a design has rooms people walk into, needs light, sits in a landscape, or should
look old or ruined.

## Contents
1. Interiors: proportions, furniture, ceilings, floors, inner walls
2. Clutter and storytelling
3. Lighting: values, hiding sources, mood, spawning
4. Terrain: foundations, ground gradients, paths, water, vegetation
5. Weathering, decay and ruins
6. Mistakes checklist

---

## 1. Interiors

### Proportions
- Clear height 3-4 for small rooms. 2 is cramped and stops jumping; a top-half slab ceiling gives
  a 2.5 room when you need something squat (crawlspaces, crypt niches).
- Vary ceiling height room to room. Halls 5-8 or vaulted.
- Break big rooms with a level change (1-2 steps up to a dais), a row of columns, or a rug that
  marks a zone.

### Furniture from blocks
| Piece | How |
|---|---|
| Chair | stair facing the table + `*_trapdoor[open=true]` on each side as armrests (a sign also works) |
| Table | fence post + carpet or pressure plate on top; or `*_slab[type=top]`; or paired `*_stairs[half=top]`. Long table: row of top slabs |
| Bench / couch | row of stairs, open trapdoors on the ends, carpet or banner behind |
| Bed | real `*_bed` with trapdoor or slab nightstands, a lantern or candle on top. Canopy: 4 fences or walls + carpet/slab roof |
| Shelves | `bookshelf`, `chiseled_bookshelf` (vary `slot_N_occupied` so it looks used), trapdoor ledges, `barrel[facing=...]` as crates |
| Cabinets | barrel, chest with a trapdoor on the face, dropper + item frame |
| Kitchen | smoker or furnace stove, `water_cauldron` sink with a tripwire hook or lever tap, upside-down stair counters, campfire hearth with a wall chimney, brick stair range hood |
| Library | bookshelves 2-3 high with log posts every 3-4, lectern or enchanting table centrepiece, ladder to a mezzanine, chiseled bookshelves for variety |
| Fireplace | `campfire` (no fire spread) in a brick or stone alcove, upside-down stair mantel, iron bars grate |

### Ceilings
- **Beams:** logs (`axis=x` or `z`) every 3-4 blocks across the short span, one block below a
  plank ceiling. Upside-down stairs where beams meet walls soften the corner.
- **Coffered:** a grid of beams, panels between them recessed 1 up in a contrasting plank or top slab.
- **Chandelier:** chain 1-3 long -> fence or end rod hub -> trapdoor arms with candles, or a
  `lantern[hanging=true]`.
```
 side view          top view
   # ceiling         . c .
   | chain           c F c     F = fence / end_rod hub
   F                 . c .     c = candle on a trapdoor arm
 c-F-c
```
Note: since 1.21.9 the plain chain is `iron_chain` (copper chains also exist). Check the target
version's block list.

### Floors
- **Border + field:** a 1-block border in a darker plank or polished stone around the main floor;
  a 3x3 or 5x5 inlay in the centre (chiseled blocks, glazed terracotta).
- Mix 2-3 similar blocks about 70/20/10. Aged stone floor: stone bricks + cracked + mossy + a
  little cobble or gravel.
- Rugs: carpet rectangles with a contrasting 1-block border.
- Avoid glass, grass and dirt floors indoors unless it is a deliberate ruin.

### Inner walls
Wainscot of planks or stairs up to 1 block, a different block above, a log trim line between.
Recess shelves into thick walls. Hang banners, paintings, item frames with maps or tools, hanging
signs, flower pots.

## 2. Clutter and storytelling

- Fill about 30-50 % of the edge tiles of a room; keep walkways clear.
- Props: barrel, chest, `decorated_pot`, `candle[candles=1..4,lit=true]`, potted plants, skulls
  and heads, item frames, lectern with book, brewing stand, cauldron, grindstone,
  `chipped_anvil` / `damaged_anvil`, bell, hay bales, cobwebs in corners, sea pickles on tables.
  (Armor stands and item frames are entities: they go in the template's entity list, not as
  blocks.)
- **Group clutter into work zones.** A smithing corner = anvil + blast furnace + smithing table +
  barrel + chain hooks overhead. Zones say who lived here.
- **Dungeon story props:** smashed table (lone slabs, a trapdoor on the floor), a spilled chest,
  bone block piles with skulls, scorch marks (coal block, black concrete powder), an abandoned camp
  with `campfire[lit=false]`, a collapsed bookshelf.
- Every room should answer "who used this, and for what?" Never leave a 5x5 or larger floor bare
  unless it is an arena.

## 3. Lighting

### Light values (Java)
| Level | Blocks |
|---|---|
| 15 | lantern, glowstone, sea lantern, shroomlight, froglights, jack o'lantern, campfire, beacon, lit copper bulb (12/8/4 as it oxidises) |
| 14 | torch, end rod, cave vines with berries |
| 12 / 9 / 6 / 3 | 4 / 3 / 2 / 1 candles |
| 10 | soul lantern, soul torch, soul campfire, crying obsidian |
| 7 | glow lichen, redstone torch, enchanting table |
| 5 | amethyst cluster |
| 3 | magma block |
| 6/9/12/15 | sea pickles 1-4 (waterlogged only) |

Light drops by 1 per block of taxicab distance.

**Spawning (Java 1.18+):** hostile mobs spawn only at block light 0. To make an area mob-proof,
every walkable block needs block light 1 or more (a lantern every ~12 blocks along a corridor is
safe). For an intentionally dangerous dungeon, leave dark pockets on purpose and pair them with
spawners. Decide which you want per room, do not leave it to chance.

### Hiding light sources
- **Floor:** glowstone, sea lantern, shroomlight or froglight under carpet (carpet passes light),
  or flush in a floor pattern as an inlay.
- **Ceiling:** a light block behind a trapdoor or in a recess between beams.
- **Foliage:** shroomlight inside leaf clusters and tree canopies.
- **Behind decor:** behind a bookshelf row, under slab steps.
- **Invisible:** `minecraft:light[level=N]` is an invisible light block saved in templates. Best way
  to fix dark spots in generated structures without visible clutter.
- **Visible but styled:** lanterns hanging from chains (never floating), wall torches on pillars,
  candles in groups of 1-4, end rods as sconces, soul lanterns for eerie areas.

### Mood
- Contrast is the tool: pools of light with dark gaps build tension.
- Warm (torch, lantern, candle, campfire) = inhabited, safe. Cold blue (soul fire, sea lantern) =
  undead, ancient, magical. Green (verdant froglight, glow lichen) = swamp, overgrowth. Red-orange
  (magma, shroomlight) = forge, nether. Purple (amethyst clusters, pearlescent froglight) = arcane.
- Light the goal brighter than the path: players walk toward light.
- Never a uniform grid of torches, and never every room lit the same.

## 4. Terrain

### Foundations that flare out
```
   wall  ###
         ###
  plinth ####      <- 1 wider: stair or slab skirt
 rubble .####.     <- cobble, mossy cobble, stone, irregular
 ground ,..##..,   <- coarse dirt / gravel / rooted dirt into grass
```
Base courses go wider and darker at the bottom. Carry the foundation 2-4 blocks below grade so the
building never floats when the terrain dips.

### Ground gradients
Ragged concentric rings: `dirt_path` by the doors -> coarse dirt, gravel, rooted dirt -> patches of
podzol and moss -> grass. Each ring 1-3 wide and broken up, never a clean circle.

### Paths
2-3 wide, following contours. Mix about 60 % dirt path, 20 % coarse dirt, 10 % gravel, 10 % cobble
or andesite. Ragged edges with grass and ferns spilling in. Slabs and stairs on slopes.
No organic line (path, shore, ruin edge) should run straight for more than ~7 blocks.

### Water
Natural banks: dirt, coarse dirt, mud, stone, not bright sand rings. Lily pads, sugar cane,
dripleaf, moss carpet, mangrove roots at the edges. Shallow at the edges, 2-3 deep in the middle.

### Vegetation
- **Bush:** 1-3 leaf blocks (oak, azalea, flowering azalea) with `persistent=true`, which is critical
  in templates or they decay, plus ferns or moss carpet round them.
- **Custom tree:** 1x1 or 2x2 trunk, branches of sideways logs or `*_wood` at irregular heights, a
  lumpy layered canopy, a root flare at the base. No lollipops.
- **Hanging:** vines (set the facing), hanging roots under dirt ceilings, cave vines, glow lichen on
  cave walls, weeping vines for nether themes.
- **Ground cover:** short and tall grass, ferns, flowers, `pink_petals`, moss carpet, leaf litter.

## 5. Weathering, decay and ruins

- **Vanilla stronghold mix:** about 45 % stone bricks, 30 % mossy, 20 % cracked, 5 % special. The same
  idea works for every brick family with cracked variants (deepslate bricks and tiles, polished
  blackstone bricks, nether bricks) and for mud bricks + packed mud.
- **Wear has a direction.** Moss near water, on the ground and on the shaded side. Cracks at bases,
  corners and near the point of damage. Concentrate decay on one side or corner (where the damage
  came from), never as even noise.
- **How much:** keep 70-85 % of the original form readable so the silhouette still says what the
  building was. 10-30 % weathered variants on intact parts, 40-60 % on the damaged side.
- **Non-solid swaps:** swap some full blocks for the stair, slab or wall of the same family: broken
  silhouettes, missing bricks, divots in floors.
- **Broken walls** step down jaggedly, never a flat cut, with slab and stair caps on the broken tops.
```
 before ########     after  #  _
        ########            ## #_  #
        ########            ######_##     (_ = slab / stair caps)
```
  Rubble at the foot of the gap roughly matches the missing volume: cobble, gravel, slabs, mossy
  cobble, wall stubs.
- **Collapse:** a fallen ceiling makes a mound under the hole; beams fall diagonally; light shafts
  come through roof holes.
- **Overgrowth:** vines, moss, glow lichen, hanging roots, azalea, leaves growing through windows,
  roots pushing up through floors (rooted dirt, mangrove roots).
- **Abandonment props:** cobwebs in corners and high ceilings, damaged anvils, empty flower pots,
  trapdoor doors hanging open, oxidised copper (use `waxed_` to lock the stage), suspicious gravel
  or sand for archaeology.
- In worldgen, decay can also be applied per copy by processors (see dungeons-worldgen.md), so each
  generated copy is different. Design the "intact" template with good bones and let processors add
  the noise, plus hand-placed directional damage the processors cannot do.

## 6. Mistakes checklist

1. Empty rooms (a bed in a 9x9 box), or clutter blocking the walkway.
2. Torches spammed in a grid; lanterns floating with no chain; every room lit the same.
3. One block-light-0 tile in a room meant to be safe.
4. Salt-and-pepper weathering instead of decay with a direction; ruins so wrecked the shape is lost;
   flat cuts; no rubble under gaps.
5. Buildings floating on terrain with no foundation flare and no ground gradient.
6. Straight organic lines, perfectly round ponds, lollipop trees.
7. Leaves without `persistent=true` in a template.
