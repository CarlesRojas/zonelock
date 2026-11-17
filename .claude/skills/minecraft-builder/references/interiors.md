# Interior design (deep guide)

Read this for every structure people walk into. `interiors-lighting-terrain.md` has the basics
(furniture table, light values, terrain); this file is how to compose rooms that feel designed.
The usual AI interior is a box with one chest and a torch: this file exists to stop that.

## Contents
1. Placement rules for generated interiors
2. Room composition
3. Walls inside: three bands, depth, niches
4. Ceilings
5. Floors
6. Themed room recipes
7. Detail blocks and when they fit
8. Lighting in three layers
9. Example room plans
10. Checklist

---

## 1. Placement rules for generated interiors

- Connections are not automatic: write fence, wall, bar, pane and chain states explicitly.
- Stair chairs: `facing` is the side of the tall back, so set it pointing away from the table
  (the sitter faces the table, the back is behind them).
- Open trapdoors rest on the side opposite `facing`. Wall torches, wall banners, ladders, wall
  signs and wall skulls use `facing` = pointing away from the wall.
- Entities (armor stands, item frames, glow item frames, paintings) are not blocks: they go in the
  template's entity list. Item frame `Facing`: 0 down, 1 up, 2 north, 3 south, 4 west, 5 east;
  `Invisible:1b` for a floating item.
- Things that change by themselves: `ice` and snow layers melt near light (use `packed_ice` /
  `blue_ice`); an unpowered `redstone_lamp[lit=true]` turns off (a lit `copper_bulb` stays lit);
  water and lava flow (contain sources or use waterlogged blocks and filled cauldrons); sand,
  gravel, anvils, concrete powder and loose pointed dripstone fall; fire spreads (use campfires).
  Hanging lanterns need a solid block or chain above.
- `light[level=N]` fills dead corners invisibly; never as the main light, light from nowhere
  looks wrong.

## 2. Room composition

- **One focal point per room, on the main axis**: opposite the entrance or at the end of the long
  axis. Throne, altar, fireplace, great window, lectern on a dais, the mod's core. Detail rises as
  you approach it. It is at least 25 % bigger than anything near it, gets the room's one unique
  material and the strongest light.
- **Zoning without walls:** change of floor material, a 1-step level change, a rug, a different
  ceiling height or beam pattern, a row of pillars.
- **Circulation:** a clear path at least 2 wide (3+ in halls) from each door to the focal point and
  every exit. A rug runner or floor stripe marks it. Clutter lives on edges and in corners.
- **Symmetry:** sacred, formal and royal rooms are symmetric about the entrance-to-focal axis.
  Lived-in rooms: symmetric bones (pillars, windows, beams), asymmetric contents.
- **Windows and pillars alternate, never stack:** lay out the pillar rhythm and the window rhythm
  together. A pillar centred on a window (or a bookcase, wardrobe or bed head in front of it)
  blocks it from both sides and leaves a 1x1 pocket in the reveal. Frame a window with a pillar on
  each side instead, and keep tall furniture between windows; low furniture (a chest, a bed, a
  desk) can sit under a window whose sill is 1 above the floor.
- **Scale:** furniture is always 1 block tall, so big rooms scale the things around it: daises,
  pillars, 2x2-3x3 chandeliers, tapestries 2-3 banners wide. 1-2 blocks of air around each group.
  Heights: 4-5 normal rooms, 7-8 halls and libraries, 10-12+ naves and great halls.
- **Negative space:** 30-40 % of the floor and one wall area stay empty. Contrast is what makes
  detail read.
- **Every wall has a job, and not the same one:** a focal wall, a storage or shelf wall, a window
  wall, a quiet wall with a single accent.
- **Odd spans** (5, 7, 9) so features centre; in a 5-wide bay, a 3-wide feature between 1-wide
  pillars.

## 3. Walls inside

**Double-thick walls** let the inside differ from the outside (stone outside, planks or plaster
inside) and let you cut niches without holes to the outside.

**Three bands, floor to ceiling:**
1. **Wainscot** (1-2 high): dark and heavy (stripped dark oak wood, polished deepslate, spruce
   planks, mud bricks), capped by a rail of `*_trapdoor[half=top,open=false]` in front of the wall
   or an upside-down stair.
2. **Field:** lighter, set back 1 if the wall is thick enough (smooth sandstone, white terracotta,
   calcite, stone bricks, birch planks).
3. **Frieze / cornice:** a row of upside-down stairs facing the wall or top slabs under the
   ceiling, softening the wall-ceiling joint.

**Breaking up a flat wall** (any run over 5 needs one):
- **Engaged pillars** every 3-5, 1 block into the room (stripped logs, polished basalt, quartz
  pillar, deepslate tiles), with a base and a capital (upside-down stairs fanning out, or a stair
  bracket under a beam).
- **Niches:** 1x1 for a candle, skull, pot, amethyst cluster or plant; 1x2 for an armor stand or
  statue; 3-wide alcove for a bench or bed. Top them with an arched lintel (upside-down stairs from
  both sides) or a top slab.
- **Shelves in the wall thickness:** bookshelves, chiseled bookshelves, barrels facing the room,
  trapdoor ledges with pots and candles.
- **Half-timber inside:** log grid with plaster infill and diagonal braces.
- **Arches between rooms** (see roofs-arches-details.md).
- **Texture:** 70/20/10 mixes, smoother and more refined than the outside.
- **Surface details:** button rivets, trapdoor panels, ladders as trellis, hanging-sign plaques.

## 4. Ceilings

Never a flat sheet of one block. Choose one:
- **Beams with brackets:** a log beam every 2-4 across the short span, aligned with the engaged
  pillars; an upside-down stair bracket under each beam end; the fill between beams 1 higher.
- **Coffers:** beam grid every 3, cells raised 1, a light at the centre of each cell.
- **Barrel vault:** upside-down stairs and top slabs stepping in to the crown; dark ribs on a light
  fill read as gothic.
- **Height change:** lower over the entrance and aisles, higher over the focal zone. Compression
  then release makes the room feel grand.
- **Gallery / mezzanine:** 2-3 wide balcony at 4-5 up around a double-height room, fence or
  trapdoor railing, stair brackets, ladder or corner stair.
- **Hanging decor:** chain + hanging lantern; chandeliers (a cross of upside-down stairs or a block
  with 4 open trapdoors, hung on chains, candles on top); banners from beams; spore blossoms
  (overgrown); cobwebs in corners (crypts, ruins); hanging roots, cave vines, pointed dripstone
  (natural ceilings); `bell[attachment=ceiling]` in temples.

## 5. Floors

- **Border + field:** darkest material in a 1-block border, lighter field, an inset band for formal
  rooms.
- **Contrast with walls:** floor darker than the wall field and different in material. Same
  material for floor and walls = a box.
- **Patterns:** stepped herringbone with log axes (A = `axis=x`, B = `axis=z`, rows `AABB / BAAB /
  BBAA / ABBA`); chevrons pulling the visitor forward; rotated glazed terracotta medallions under
  altars; planks running along the long axis. **Checkerboard only on purpose** (alchemy lab,
  ritual circle with a border), never by default.
- **Rugs:** carpet border + field, 1-2 short of the walls; runners mark paths; moss carpet patches
  near walls suggest age.
- **Level changes:** daises 1-3 steps (slabs and stairs, accent material on top); sunken areas for
  pools and pits; slab half-steps for subtle terraces.

## 6. Themed room recipes

**Throne room / great hall.** Throne on a 3-step dais at the far end; banners 2-3 wide as a
tapestry behind it, flanked by tall windows or engaged columns; a 3-wide carpet runner; aisle
columns every 3-4; long table of top-half stairs or slabs on fences with stair chairs and 3-candle
groups every 3; a side fireplace (campfire in brick, stair hood, chiseled mantel); chandeliers on
chains every 5-6.

**Library / study.** Bookshelf walls broken every 3-4 by a log pillar or chiseled bookshelves
(partly filled); staggered heights, low stacks with candles on top; a mezzanine with ladder and
railing; a desk of top-half stairs with a lectern; a reading nook with an armchair and a lantern;
warm low light only, no glowstone; rare books behind an iron door.

**Alchemy / enchanting room.** Enchanting table on a 1-step dais with bookshelves 2 away (air
between); brewing stands on counters; water and lava cauldrons; amethyst clusters and buds on
shelves; pots with crimson and warped fungus and wither roses; a purple glazed terracotta ritual
inlay; cool purple light (amethyst, soul lanterns, crying obsidian, end rods).

**Temple sanctum.** Strictly symmetric. Raised altar (chiseled quartz or stone, gilded blackstone)
with a relic (lectern, decorated pot, heavy core) and 4-candle clusters at the corners; a chancel
arch between altar and nave; pews of stairs facing the altar; stained glass behind the altar,
candles in side niches; a bell above.

**Crypt / tomb.** Deepslate bricks, polished deepslate, tuff bricks, cracked and mossy variants.
Sarcophagus 1x2-1x3: polished deepslate base, slab or stair lid, a skull at the head, 1-2
candles. Wall loculi: 1x2 recesses with sideways bone blocks, wall skulls and cobwebs. Soul light
only (level 1-7). Chains across doorways, suspicious gravel patches.

**Armory.** Armor stands in 2-tall niches, weapons in item frames on a plank field, banners as
shields, anvils (all damage states), grindstone, smithing table, blast furnace, barrel stacks,
iron bar partitions.

**Kitchen / dining.** Counters of top slabs over barrels or trapdoor-faced planks; smoker or
furnace under a brick hood, a campfire beside; cauldron sink; pantry of hay, barrels and a
trapdoor-fronted cupboard; hanging signs and herbs from the ceiling; carpet tablecloth and a
candle cake centrepiece.

**Bedroom.** Bed head against the quiet wall with a trapdoor or stair headboard; barrel bedside
table with a pot or candle; wardrobe; rug; one painting above the bed; banner curtains.

**Treasure vault.** Iron door or bar gate; piles of gold and raw gold blocks and gilded
blackstone; chests and barrels; decorated pot urns; a vault block or heavy core as the jewel on a
pedestal.

**Prison cells.** 3x3 cells with connected iron bars; iron trapdoor doors; hay bed, cauldron,
chains, cobwebs, cracked bricks; one soul torch in the corridor; a guard post with table, lantern,
bell.

**Water shrine.** Symmetric basins round a raised shrine; slab or stair rims; waterlogged sea
pickles and seagrass; prismarine channels; a contained waterfall from a niche; lily pads; potted
bamboo and azalea; stone lantern posts.

**Greenhouse / overgrown.** Planters of rooted dirt or moss framed by trapdoors; flowering azalea
leaves (persistent); spore blossoms, glow lichen and cave vines above; moss carpet creeping in
from the edges; big dripleaf; vines on walls. Pale garden variant: pale oak, pale moss, pale
hanging moss, a creaking heart as an eerie focal point, resin accents.

**Snow / ice temple.** Packed and blue ice (never plain ice near light), snow blocks, calcite,
polished diorite; quartz or packed ice columns; cold light (soul lanterns, sea lanterns behind
light blue glass, end rods); blue ice inlay in a snow border; powder snow pits as traps; dripstone
icicles; light blue carpet runners.

**Desert temple.** Cut and smooth sandstone, chiseled sandstone friezes, orange and blue
terracotta accents; glazed terracotta floor centre; decorated pot urns; suspicious sand; a gold
sun motif; torches in niches or campfire braziers; red sandstone columns; a hidden pit.

**Nether room.** Nether bricks and red nether bricks with chiseled and cracked variants; polished
blackstone bricks with gilded accents; crimson or warped furniture; shroomlight in wart-block
"fungus lamps", magma in the floor, soul fire on soul soil, lava behind glass, crying obsidian;
weeping vines; a charged respawn anchor as an altar.

## 7. Detail blocks and when they fit

| Block | Use |
|---|---|
| `decorated_pot` | ancient, desert, temple, treasure; groups of 1-3, never long rows |
| `chiseled_bookshelf` | breaks bookshelf walls; partly filled reads as used |
| `lectern[has_book=true]` | focal point of a study or altar; one per room |
| `candle` | 1 on tables, 3-4 on altars and in niches; vary the counts |
| heads, item frames, armor stands | armories, trophy rooms; wall skulls above crypt doors |
| flower pots | homes, shrines; `potted_dead_bush` in ruins |
| banners | tapestries, every 2 blocks; 2-3 wide behind thrones |
| paintings | above beds and fireplaces; one per wall at most |
| chains, iron bars | dungeons, prisons, lantern hangers, chandeliers |
| barrels, hay | storage zones; 1-2-1 pyramids, mixed facing |
| cauldron, brewing stand | alchemy, kitchen sink |
| anvil, grindstone, smithing table | smithy, armory |
| bell | temples, guard posts, entrances |
| sea pickles | waterlogged light in shrines and pools |
| amethyst clusters | magic rooms (light 5) |
| glow lichen | caves, crypts (light 7) |
| moss carpet | age and overgrowth at room edges |
| waxed copper bulbs | workshops, trial-chamber style |
| heavy core, vault | relic on a pedestal, treasure rooms |
| trial spawner | live: only in hostile rooms |
| pale blocks, creaking heart | haunted rooms |

Clutter covers 30-50 % of edge cells, in purposeful groups (storage by the door, tools by the
workstation).

## 8. Lighting in three layers

1. **Ambient:** hidden sources that remove dark holes (under carpet, behind top trapdoors in the
   ceiling, inside beams, behind leaves, `light` blocks).
2. **Accent:** visible, small, rhythmic: wall torches on pillars, lanterns on chains centred in beam
   cells, candles in niches. 4-7 blocks apart, so pools of light alternate with shade.
3. **Focal:** the brightest and most unique source, at the focal point: candle clusters on the
   altar, a fireplace, backlit stained glass, a beacon, the glowing core.

One colour temperature per room (warm: torches, lanterns, candles, campfires; cool: soul fire,
sea lanterns, end rods, amethyst, froglights). A single contrasting source can mark the focal
point. Even lighting kills mood.

## 9. Example room plans (top view at floor+1, north up)

**9x9 library**
```
  # # # # # # # # # # #
  # P B C B W B C B P #
  # B . . . L . . . B #
  # C . r r r r r . C #
  # B . r h . h r . B #
  # P . r T T T r . P #
  # B . r h . h r . B #
  # C . r r r r r . C #
  # A . . . . . . k B #
  # P B B . . . B B P #
  # # # # # D # # # # #
```
P stripped dark oak log pillar, B bookshelf, C chiseled bookshelf facing the room, W recessed
window, L lectern on a slab dais, T top-half dark oak stairs table with 2 candles, h spruce stair
chairs, r carpet border, A armchair, k barrel + lantern, D door. Ceiling beams over each P pair,
chandelier over T.

**11x15 temple sanctum** (entrance south)
```
   1 2 3 4 5 6 7 8 9 10 11
 1 N . c . G G G . c . N
 2 . . s s s s s s s . .
 3 . . s s c A c s s . .
 4 . . t t t t t t t . .
 5 N O . . R R R . . O N
 6 . . w w R R R w w . .
 7 . . . . R R R . . . .
 8 N O . p R R R p . O N
 9 . . . p R R R p . . .
10 . . w w R R R w w . .
11 N O . p R R R p . O N
12 . . . p R R R p . . .
13 . . . . R R R . . . .
14 N O . . R R R . . O N
15 . . . . . E . . . . .
```
G stained glass wall, s dais top (+1), t steps, A altar with relic, c 4 candles, O column with
stair capital, N wall niche with lantern or skull, R carpet runner, w sunken basin with sea
pickles, p pews, E entrance arch. Barrel vault over columns, lower ceiling over aisles.

**7x7 crypt**
```
  # L # L # L # L #
  L . . . . . . . L
  # . k . . . k . #
  L . . S S S . . L
  # . . S S S . . #
  L . k . . . k . L
  # . . . . . . . #
  L . . . . . . . L
  # # # # D # # # #
```
L loculus niche (sideways bone block, wall skull, cobweb), S sarcophagus (polished deepslate base,
tile slab lid, skull and 3 candles at the head), k soul lantern on a chain, D iron bar gate.
Floor: deepslate tile border, tuff brick field, a moss carpet patch in one corner.

## 10. Checklist

1. Focal point on the axis, raised, accented, brightest?
2. Clear 2-wide paths from every door?
3. Walls in bands with depth at least every 5 blocks?
4. Ceiling with beams, vault, coffers or a height change; wall-ceiling joint softened?
5. Floor with border and field, contrasting the walls, a level change for zones?
6. Each wall with a different job; negative space left?
7. Clutter on 30-50 % of edges, in purposeful groups?
8. Three layers of light, one temperature, visible pools, spawn-safe or deliberately dark?
9. Block states right (facing, half, connections, hanging, lit, waterlogged)? Entities as
   entities?
