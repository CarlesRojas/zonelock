# Walls, depth, texture and palettes

Read this before choosing materials or drawing any wall. It is the difference between "a box
of stone bricks" and a wall that looks built by someone who cares.

## Contents
1. Depth: no flat walls
2. Sub-block toolkit
3. Framing patterns
4. Texture: mixing blocks
5. Gradients and weathering
6. Choosing a palette
7. Twelve proven palettes
8. Massing and proportions
9. Mistakes checklist

---

## 1. Depth: no flat walls

A wall is read by its shadows. Minecraft has no ambient occlusion worth relying on, so the only
way to get shadows is to make blocks sit on different planes.

- Any wall wider than about 5 blocks needs at least one change of plane.
- **Frame stands proud, infill sits back.** Pillars, beams and frames go 1 block in front of the
  infill. Never put frame and infill on the same plane: it then reads as a flat two-colour pattern.
- **The 1-2-1 system:** columns at +1, infill at 0, glass at -1. On big builds recess glass 2.
- A good wall usually has 3 layers of depth (e.g. infill, log pillars, then pillar bases or a
  stair skirt at ground level). Total depth is typically about +/-1 block from the main plane,
  with sub-block pieces on top.
- Put the parts that stick out furthest at the **bottom** (plinth, buttress feet). Heavy at the
  base looks stable; heavy at the top looks wrong unless it is a deliberate overhang carried on
  corbels.

How much depth each piece reads as:

| Element | Effective depth |
|---|---|
| Full block forward / back | 1 |
| Stair or slab used as trim, sill, cornice | ~0.5 |
| Trapdoor, wall, fence, button | < 0.5 (thin overlay) |
| Glass pane (sits in the block centre) | recessed ~0.44 |
| Frame pulled out 1 + pane | ~1.5 blocks of shadow |

That is why panes beat full glass blocks in most windows: free depth.

Top view of one bay (outside is at the bottom):
```
  L . . . . . L      L = log pillar (+1)
  L W W G W W L      W = infill (0), G = glass pane (-1)
  S           S      S = stair at the pillar foot (+2, ground row only)
```

Elevation of a 5-wide bay between pillars:
```
 y+5  =============   upside-down stair cornice / beam (+1)
 y+4  L  W  W  W  L
 y+3  L  W [G G] W L   slab or trapdoor frame round the window (+0.5)
 y+2  L  W [G G] W L
 y+1  L  W  _  _ W L   sill: stair or slab under the window (+0.5)
 y+0  S# ####### #S    plinth: darker, rougher row (+1), stair bases at pillars
```

## 2. Sub-block toolkit

These blocks exist to break the 1-metre grid. Use them where a real building would have a
moulding, a ledge or a bracket.

- **Upside-down stairs** (`half=top`): cornices under eaves, beam ends, window heads, corbels
  under overhangs and jettied upper floors, arch shoulders, column capitals.
- **Slabs:** window surrounds, thin ledges, soffits, roof edges, half-steps in floors.
- **Trapdoors:** shutters beside windows (open, against the wall), flat panelling on walls and
  doors, arrow slits, awnings, table tops, the face of drawers.
- **Walls and fences:** mullions, railings, posts, table legs. They auto-connect to neighbours,
  so check what they touch.
- **Buttons:** rivets and bolts on doors, beams and metalwork.
- **Chains, iron bars, copper grates:** metal straps, cages, lattice windows, hanging lights.

**Detail flow.** Detail must connect along edges and look like it does a job: it supports,
frames, shelters or drains. Slabs that touch only at a diagonal corner look floating. A bracket
behind the thing it "holds up" looks wrong. If you cannot say what a detail is for, cut it.

## 3. Framing patterns

- **Timber frame (medieval, Tudor):** log frame with infill 1 block behind. Frame cells 3-5 wide,
  roughly square; storeys about 4 high. Diagonal braces read well in the corner cells.
- **Horizontal bands:** grey stone base, then planks or brick, then a light top (calcite, white
  terracotta, quartz). Three bands of value bottom-dark to top-light.
- **Pillar rhythm:** a contrasting pillar every 4-6 blocks on houses, a buttress every 8-10 on
  castle and dungeon walls.
- **Classic stone wall:** put the detail at the **foot** (plinth, stair skirt) and the **top**
  (cornice, machicolations, crenellations). A plainer middle lets those two read.

## 4. Texture: mixing blocks

Two kinds of texture, use both: **visual** (mixing variants on a plane) and **physical** (depth,
section 1). When there is no room for depth, a detailed block beside a plain one (stone next to
stone bricks) fakes relief.

Ratios:
- 60-70 % dominant, 20-30 % secondary, 5-10 % accent. Never 50/50: it reads as checkerboard or
  noise.
- Stone wall example: `stone_bricks` 45, `stone` 25, `andesite` 20, `cobblestone` 10.
- Simpler house wall: `stone_bricks` 70, `cracked_stone_bricks` 15, `mossy_stone_bricks` 10,
  `andesite` 5. Keep moss under 10 % unless the place is damp.

**Clusters, not speckle.** Uniform random scattering (every block an independent dice roll) rarely
looks good. Grow irregular blobs of a variant, of different sizes. Never line variants up in rows,
columns or regular diagonals. When designing by coordinates, that means: choose a few seed points,
and spread the variant to neighbours with decreasing probability, instead of rolling per block.

## 5. Gradients and weathering

**Vertical gradient: dark and rough at the bottom, light and clean at the top.** It reads as weight,
age and ground moisture.
```
caps / tower tops : calcite / diorite / smooth_quartz / polished_andesite
upper wall        : stone / andesite / stone_bricks
mid wall          : stone_bricks + cracked_stone_bricks (+ a little mossy)
lower wall        : cobblestone / cobbled_deepslate / mossy_cobblestone
plinth/foundation : deepslate_bricks / deepslate_tiles / polished_deepslate
```

Gradient rules:
- Step through neighbours in value. **Do not skip a step**: a very dark block directly against a
  very light one breaks the gradient (skip only as a rare deliberate accent).
- Transitions must be wavy and interlocking, never straight horizontal bands ("sandwiching").
- Big facades get several clusters, not one.

Where weathering goes (place it first, then grade around it):
- the bottom 1-3 rows (splash zone);
- under windows, as drip stains 2-4 blocks long;
- corners, edges, openings and under eaves, where water runs;
- the shaded or wet side (moss), near water.

Use lighter variants around arches and window edges to define them, and on crenellations and
protruding parts to pop them forward.

**Weather each material within its own family.** Age and wear come from variants of the same
material, never from a different material pasted on top:
- **Wood** (roofs, beams, floors, walls) ages through other woods and wood forms: mix 2-3 woods of
  neighbouring tones (dark oak + spruce, spruce + oak, jungle + dark oak), stripped logs, a few
  slabs or missing stairs for holes, and natural growth that would really sit on wood (moss carpet,
  vines, leaves, a fallen branch). Never cobblestone, mossy stone or stone bricks in a wooden roof:
  stone patches on a timber roof read as a mistake, not as age.
- **Stone** ages through its own family: cracked, mossy and chiseled variants, the same block's
  bricks mixed with its raw and cobbled forms (stone bricks + cracked + mossy + cobble + andesite;
  deepslate bricks + cracked + cobbled deepslate + tiles). Moss blocks and moss carpet suit stone.
- **Metal** ages by oxidation stages (copper exposed -> weathered -> oxidized), not by other metals.
- **Plaster and terracotta** age with neighbouring light tones and a darker base, not with stone.
- Overgrowth is the exception that may cross materials (leaves, vines, moss carpet, roots), because
  it is clearly something growing on the building, not part of it.
- The test: if a patch could only exist because someone rebuilt the roof with a different material,
  it is wrong.

Approximate stone value ladder, light to dark. Pick 3-5 adjacent steps for one gradient:
`diorite, calcite, smooth_stone, stone, andesite, stone_bricks, cobblestone, tuff, tuff_bricks,
deepslate, cobbled_deepslate, deepslate_bricks, deepslate_tiles, basalt, blackstone,
polished_blackstone_bricks`

## 6. Choosing a palette

- **3-5 core materials** plus their stair, slab and wall forms. Assign roles: primary (walls),
  secondary (frame or roof), accent (trim). Roughly 60/30/10.
- Sort candidates by **value first**, then hue, then texture.
- **Value contrast is what reads.** Frame darker than infill or the reverse. Roof darker than walls
  (dark wood or deepslate over light stone is the classic). Push contrast further than feels
  natural: similar values blur together at a distance.
- **One accent colour**, sparingly. Complementary pairs give the most punch, with one dominant:
  warm wood or sandstone + cool `oxidized_copper` or warped; grey stone + red banners; deepslate
  + gold or `gilded_blackstone`; sandstone + blue or cyan terracotta.
- **At most two wood types** per build.
- Warm: granite, terracotta, mangrove, acacia, bricks. Cool: diorite, prismarine, deepslate.
  Neutral bridges: andesite, tuff, stone. Mud bricks are a mid-tone between deepslate and calcite.
- Every block must have stair and slab forms if you will need them for roofs and trims. Check
  before committing (e.g. `cracked_stone_bricks` has no stairs; use `stone_brick_stairs`).

## 7. Twelve proven palettes

| # | Style | Primary walls | Frame / secondary | Trim / accent | Roof | Floor / base |
|---|---|---|---|---|---|---|
| 1 | Medieval stone | `stone_bricks` + cracked, mossy, `andesite` | `spruce_log`, `stripped_spruce_log` | `chiseled_stone_bricks`, `lantern`, `spruce_trapdoor` | `dark_oak_stairs` | `cobblestone`, `mossy_cobblestone` plinth |
| 2 | Tudor cottage | `white_terracotta` or `calcite` infill | `dark_oak_log` frame | `spruce_trapdoor` shutters, `glass_pane` | `spruce_stairs`, dark oak eave trim | `cobblestone` + `stone_bricks` |
| 3 | Deepslate keep / crypt | `deepslate_bricks` + cracked, `deepslate_tiles` | `polished_deepslate`, `cobbled_deepslate` | `chiseled_deepslate`, `soul_lantern`, `iron_bars` | `deepslate_tile_stairs` | `deepslate_tiles`, `polished_basalt` |
| 4 | Sandstone desert | `sandstone`, `smooth_sandstone` | `cut_sandstone` bands, `chiseled_sandstone` | `blue_terracotta` / `cyan_terracotta`, `stripped_acacia_log` | flat, `smooth_sandstone_slab` parapets | `sandstone`, `terracotta` |
| 5 | Dark oak + spruce lodge | `spruce_planks` | `dark_oak_log` | `spruce_trapdoor`, `lantern` | `dark_oak_stairs` | `cobblestone`, `stone_bricks` |
| 6 | Mangrove swamp | `mud_bricks`, `packed_mud` | `mangrove_log` | `mangrove_roots`, `muddy_mangrove_roots` | `mangrove_stairs` | `mud`, `packed_mud` |
| 7 | Cherry / eastern | `cherry_planks` or `calcite` panels | `stripped_cherry_log`, `dark_oak_log` | `cherry_leaves`, `lantern`, `bamboo` | `deepslate_tile_stairs` | `polished_deepslate` |
| 8 | Ocean temple | `prismarine_bricks` | `dark_prismarine` | `sea_lantern`, `light_blue_stained_glass` | `dark_prismarine_stairs` | `prismarine`, `sand` |
| 9 | Nether fortress | `polished_blackstone_bricks` + cracked, `blackstone` | `polished_basalt`, `basalt` | `gilded_blackstone`, `soul_lantern`, crimson wood | `blackstone_stairs` | `polished_blackstone`, `nether_bricks` |
| 10 | Copper + tuff (trial chamber feel) | `tuff_bricks`, `polished_tuff`, `tuff` | `chiseled_tuff`, `chiseled_tuff_bricks` | `copper_grate`, `chiseled_copper`, `copper_bulb` (use `waxed_` to freeze oxidation) | `waxed_weathered_cut_copper_stairs` | `polished_tuff` |
| 11 | Warm brick villa | `bricks` | `granite`, `polished_granite` | `terracotta`, `stripped_acacia_log` | `brick_stairs` | `polished_granite`, `mud_bricks` |
| 12 | Adobe three-tone | `mud_bricks` | `calcite` bands and caps | `stripped_spruce_log` lintels, `decorated_pot` | `deepslate_tile_stairs` | `packed_mud`, `polished_deepslate` |

Amethyst accent (fits a mod themed on amethyst): `amethyst_block`, `budding_amethyst` (decor only),
`amethyst_cluster`, `calcite` and `smooth_basalt` (the natural geode shell), with `tinted_glass`.

## 8. Massing and proportions

- **Never a single cube.** Combine 3-4 boxes of different heights; secondary masses about 1/3-1/2
  of the main one. Footprints: L, T, cross, offset wings, attached tower, lean-to.
- **One focal mass**, at least 25 % taller or wider than its neighbours. It gets the most detail;
  the rest supports it.
- **Silhouette test:** filled solid black, the shape should still be interesting. Break the
  roofline into sections at different heights; add gables, dormers, an off-centre chimney.
- **Build bigger than feels necessary.** Detail needs room; a 5x5 room cannot hold a frame,
  furniture and a lamp.
- **Odd widths** (5, 7, 9) so walls have a centre for doors and windows, and roofs end in a single
  ridge block.

| Element | Value |
|---|---|
| Door | 1x2 single, 2x2 double, grand 3-4 wide x 4-5 tall |
| Window | at least 2x2, often 3x3; grouped in 2-3, not a perfect grid |
| Ceiling | 3 is cramped; 4-5 normal rooms, 7-8+ halls and chapels |
| Storey (timber frame) | ~4 |
| Roof overhang | 1-2 past the wall |
| Foundation | 1-2 buried, darker block |
| Castle curtain wall | 3-4 thick |
| Tower | 2-3 wider than the wall, 5-10 taller |
| Gate | 3-4 wide x 4-5 tall, arched |
| Buttress spacing | 8-10 |

## 9. Mistakes checklist

1. One block type for a whole wall.
2. Pillars, infill and glass all on one plane.
3. 50/50 mixes, checkerboards, or uniform random speckle.
4. Variants in neat rows or straight bands (sandwiching), or lone dots (smallpox).
5. A dark block straight against a light one (skipped gradient step).
6. Full glass blocks in rustic windows instead of panes.
7. A perfect box footprint; a pyramid roof on a square plan.
8. No roof overhang.
9. A building floating on the ground with no plinth or transition.
10. Even-width walls with an off-centre door; uneven window spacing.
11. Ceilings of 3 everywhere; cramped scale.
12. Floating detail (corner-only contacts, unsupported fences).
13. More than one accent colour, more than two woods, too many strong textures together.
14. Decorating before the shape and roof are settled.
15. Walls and roof in the same value.
