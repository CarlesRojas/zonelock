# Roofs, arches, openings, columns, towers and detail blocks

Read this before drawing a roof, an arch, a window, a doorway, a column or a tower. It has exact
block states and front-view grids, because these elements look wrong when one stair faces the
wrong way.

The grids follow the rules stated by the sources (rise = half the span, upside-down stairs under
curves, odd widths) but were not all checked in-game. If something looks off, check stair
`facing` / `half` first.

## Contents
0. Block-state conventions
1. Roofs
2. Arches and vaults
3. Windows and doors
4. Columns, buttresses, corbels
5. Towers, battlements, conical roofs
6. Detail blocks
7. Mistakes checklist

---

## 0. Block-state conventions (Java)

- **Stairs:** `<mat>_stairs[facing=?,half=bottom|top,shape=straight|inner_left|inner_right|outer_left|outer_right]`.
  `facing` is the side of the tall back: you walk up toward `facing`. `half=top` is the upside-down
  stair: a full top slab plus a lower quarter on the `facing` side, leaving a notch on the other
  side.
- **Slabs:** `[type=bottom|top|double]`.
- **Trapdoors:** `[facing, half=bottom|top, open=true|false]`. Closed = thin horizontal plate (top or
  bottom of the block). Open = vertical panel on the edge *opposite* `facing`, so `facing` points
  away from the block it rests against.
- **Connecting blocks:** fences and iron bars `north/east/south/west=true|false`; walls
  `north/east/south/west=none|low|tall` + `up=true|false`; glass panes by direction;
  `lantern[hanging=true]`; chains `[axis=y]`; `end_rod[facing=up]`.
- **For generated structures, write connection states and stair `shape` yourself.** Template
  placement does not recompute them, so fences show as lone posts, walls as stubs and stair corners
  as broken unless the template already has the right states.

## 1. Roofs

### Pitch
| Pitch | Construction | Use |
|---|---|---|
| 22.5 deg | alternate bottom and top slabs, 1 over per half block up | porches, sheds, warm climates |
| 45 deg | one stair per row, 1 up 1 over | default |
| ~35 deg | repeat: slab, upside-down stair, stair | gentler, refined; needs a big roof |
| 63 deg | full block + stair, 2 up 1 over | gothic, nordic, towers, A-frames |

Mixed pitches: steep lower + shallow upper = gambrel / mansard; shallow lower + steep upper =
bell-cast.

### Sizing
- Roof 1-2 storeys tall on lived-in buildings; each gable rises about 5-7.
- Above ~15x15, one roof looks like a barn: use cross-gables (T, L plans) or several masses. A plain
  gable stops looking right past ~12 wide.
- **Odd spans end in a single ridge block.** Even spans leave a flat 2-wide top that needs a slab or
  back-to-back stair ridge.
- Run the ridge along the long axis. Line the peak up with a centred door.

### Gable cross-section, 45 deg, walls at x=0..8, 1-block overhang (span 11)
```
y6   . . . . . R . . . . .      R = ridge cap (block + slab, or wall / fence cresting)
y5   . . . . E u W . . . .
y4   . . . E u . u W . . .      E = stairs[facing=east,half=bottom]   (west slope)
y3   . . E u . . . u W . .      W = stairs[facing=west,half=bottom]   (east slope)
y2   . E u . . . . . u W .      u = stairs[half=top] facing AWAY from the ridge, under each roof stair
y1   E u . . . . . . . u W          (west side facing=west, east side facing=east)
y0   c # . . . . . . . # c      c = eave corbel: stairs[half=top] facing the wall
    -1 0 1 2 3 4 5 6 7 8 9          (west c facing=east, east c facing=west)
```
- The **u layer** turns the stepped underside into a smooth ceiling; needed whenever the inside of
  the roof is visible.
- The **eave corbels** make the overhang "fold into" the wall. Experienced builders always do it.
- **Overhang** 1 block on all sides (2 for big buildings), including past the gable ends, for a
  shadow line.
- **Gable trim:** a contrasting stair course along the sloped (rake) edge, 1 block proud of the gable
  wall. Fill the gable triangle with a different material from the walls below (timber or planks
  over stone), with a trapdoor vent or small window near the apex.

### Ridge caps
Slabs on the last stairs (even span); full block + bottom slab; a row of walls or fences as
cresting; back-to-back stairs with finials at the ends. A darker ridge material reads as ridge tile.

### Hips, valleys, dormers
- **Hip:** all four sides step in together; corners use `shape=outer_left/outer_right`.
- **Valley:** build the tallest roof fully, then run the lower roof into it with `shape=inner_*`
  stairs in the V; remove blocks hidden inside.
- **Dutch gable:** hip the lower part, small gable on top, often with a window.
- **Dormer:** a 3-wide mini gable sticking out of the slope, ridge at least 1 below the main ridge,
  1-2 block window, trim and corbels. Fake dormers are fine if they help the silhouette.

### Texture and contrast
- Mix 2-3 similar blocks, ~70/20/10: `deepslate_tile_stairs` + `cobbled_deepslate_stairs` +
  blackstone; spruce + dark oak; bricks + mud bricks + granite. Wear stays inside the roof's
  material family: a stone roof gets mossy or cracked stairs near the eaves; a wooden roof gets
  other wood tones (spruce patches in dark oak), missing stairs and moss carpet or leaves on top,
  never stone or mossy cobblestone blocks (see walls-texture-palettes.md, "Weather each material
  within its own family").
- Roof contrasts with the walls: dark over light, or warm terracotta/brick over grey stone.
- Avoid plain smooth stone or polished blackstone as the main roof surface.

### Curved and flat roofs
- **Bell-cast / pagoda eave:** the last 1-2 rows go shallower (bottom slabs + stairs) for an upward
  flick, with a contrasting edge. One tier per floor for pagodas.
- **Curved:** follow part of a large circle, built from stairs, top slabs and full blocks.
- **Flat roofs** always get an edge: slab border, backwards stairs as a gutter, or a 1-block parapet
  in a different material.

## 2. Arches and vaults

Rules:
- Odd opening widths so the arch has one keystone (swap it for a chiseled or polished block).
- Round arch rises about width/2; gothic about 0.87 x width.
- The arch ring and jambs are a different shade from the wall so the shape reads.
- Pier width: 1 up to a 5-wide arch, 2 from 7 wide, 3 from 11 wide.
- Recess the infill (door, glass, wall) 1 block behind the arch ring. A second larger ring 1 block
  proud of the wall gives the classic receding portal.

Front view, wall running east-west, viewer on the south side looking north (east is on the right):
`L` = `stairs[half=top,facing=west]` (notch toward the opening on its right),
`R` = `stairs[half=top,facing=east]`, `T` = `slab[type=top]`, `#` = wall, `.` = opening.
General rule: each arch stair faces away from the opening's centre. The block states are the same
whichever side of the wall you view from (only the viewer's left and right swap). For a wall
running north-south, use facing=north on the north side of the opening and facing=south on the
south side.
```
3 wide       5 wide        7 wide          9 wide (near-semicircle)
#####        #######       #########       ###########
#LTR#        ##LTR##       ###LTR###       ###LTTTR###
#...#        #L...R#       ##L...R##       ##L.....R##
#...#        #.....#       #L.....R#       #L.......R#
             #.....#       #.......#       #.........#
                           #.......#       #.........#
```

Pointed (gothic) arches need width 6-7+; the cleanest point is on an even width, where L and R
meet:
```
6 wide gothic        7 wide gothic
########             #########
###LR###             ####.####    <- 1-wide slot capped by a block
##L..R##             ###L.R###
##....##             ##L...R##
#L....R#             ##.....##
#......#             #L.....R#
#......#             #.......#
```
Steeper gothic arches step 2 rows per column near the top; rounder ones step 1.

- **Barrel vault:** run a large arch profile (upside-down stairs + top slabs) along a corridor.
- **Rib vault:** darker rib lines (e.g. `deepslate_brick_stairs`) along the arch profiles and the
  diagonals over a lighter fill (calcite, smooth quartz); ribs come down onto engaged pillars.
- **Arcade:** repeat the arch on 1-2 wide piers; 3-5 block rhythm for cloisters, 8-10 for large
  works.

## 3. Windows and doors

- **Never glass flush with the wall.** Frame +1, glass -1. Panes add ~0.44 more recess.
- Window kit for an opening in a south-facing wall:
  - **Sill:** `stairs[half=top,facing=north]` under the window, sticking out 1; or a top slab.
  - **Head:** a stair or slab above, or an upside-down stair hood.
  - **Shutters:** open trapdoors beside the glass, in the air block in front of the wall:
    `trapdoor[open=true,facing=south]` (panel rests against the wall).
  - **Mullions / jambs:** stone walls or trapdoors inside the reveal.
  - **Window box:** sill slab, a row of dirt or moss with flowers, trapdoors in front.
  - **Bay window:** push the glass 1 out in a small box with its own slab roof and a corbel below.
- **Rhythm:** odd bays (5 = 1 pillar + 3 window + 1 pillar; 7 = two windows around a central
  pillar). Align windows vertically between floors; ground floor windows 1-2 above the floor.
  Castles: arrow slits (1-wide opening or half-open trapdoor).
- **Entrances get the densest detail of the facade:** at least 25 % taller or wider than other
  openings; proud pillars, an arch or upside-down-stair lintel, a small porch roof on corbels, 1-2
  steps, a path leading in, lanterns on both sides, a banner above, door recessed 1.

## 4. Columns, buttresses, corbels

- **Column:** base (stairs ringed around the foot facing out, or a polished block) + shaft
  (stripped log, `*_pillar` block, polished stone) + capital (ring of `stairs[half=top]` facing out,
  or a chiseled block). Columns stand 1 proud of the wall, at corners and every 3-5 blocks.
- **Buttress:** every 8-12 on long walls; 2-3 out at the bottom, stepping in as it rises (3, 2, 1),
  each set-back capped with a bottom stair facing the wall. Fake flying buttresses: an arch of
  upside-down stairs from a pier to the wall.
- **Corbels and brackets:** `stairs[half=top]` facing the wall under everything that overhangs:
  jettied upper floors, balconies, eaves, tower tops. Timber version: fence post under the stair,
  or a log end with a trapdoor under it. An upper floor jettied 1 out over corbels is a strong
  medieval detail.

## 5. Towers, battlements, conical roofs

### Round tower footprints (outline cells)
Number in brackets = blocks in one ring.
```
d=5 (12)   d=7 (16)   d=9 (24)    d=11 (28)     d=13 (36)
.###.      ..###..    ..#####..   ...#####...   ....#####....
#...#      .#...#.    .#.....#.   ..#.....#..   ..##.....##..
#...#      #.....#    #.......#   .#.......#.   .#.........#.
#...#      #.....#    #.......#   #.........#   .#.........#.
.###.      #.....#    #.......#   #.........#   #...........#
           .#...#.    #.......#   #.........#   #...........#
           ..###..    #.......#   #.........#   #...........#
                      .#.....#.   #.........#   #...........#
                      ..#####..   .#.......#.   #...........#
                                  ..#.....#..   .#.........#.
                                  ...#####...   .#.........#.
                                                ..##.....##..
                                                ....#####....
```
- Under 9 a circle reads as an octagon; 15+ reads as round. Usable towers: 9-15+.
- Diagonal-only joins leave thin corners; on thick walls, double the ring.
- Smooth the ring with stairs on the ends of straight runs and outer-corner stairs on single steps.

### Proportions
Towers 2-3 wider than the curtain wall, 5-10 taller; vary heights, corner towers tallest.

### Battlements (front view)
```
1:1 simple       2:1 heavy         capped
#.#.#.#.         ##.##.##.         S.S.S.     S = bottom slab or stair cap on each merlon
#########        #########         #.#.#.
                                   ######
```
Merlon variants: wall posts (light, small scale), full blocks with slab caps, a contrasting
polished block on brick.

### Machicolations (section through the wall top)
```
  M . M        <- parapet with merlons, pushed 1 OUT past the wall face
  # # #        <- parapet / walkway
  c . c . c    <- corbels: stairs[half=top] facing the tower; gaps = murder holes
  #            <- tower wall face
```
On round towers corbel the whole top ring out 1, then put the battlement ring (d+2) on it.

### Conical roofs
Stacked shrinking circles of outward-facing stairs, `shape=outer_*` on diagonals. Radius shrinks 1
per layer for 45 deg, 1 per 2 layers for a steep witch hat. Start 1 wider than the wall with a
corbel ring under the eave. Finish with slab -> fence -> lightning rod or end rod. Spruce, dark
oak, blackstone, deepslate tiles. Cathedral spires about 2x the tower height.

### Gatehouse
Arched opening 3-4 wide x 4-5 tall between taller towers; portcullis of iron bars or chains;
murder holes in the passage ceiling.

## 6. Detail blocks

- **Trapdoors:** shutters, sill lips, chair arms, cabinet fronts, gable vents, railings, raised wall
  panels (open, against the wall), a ring around a pillar base as a plinth.
- **Fences / walls:** corner posts for inside-corner shadow, lantern hangers under eaves,
  balustrades, mullions, thin columns, ridge cresting.
- **Chains:** hanging lanterns, portcullises, hanging signs.
- **Buttons:** rivets and studs on blank stretches, sparingly.
- **End rods / lightning rods:** spire tips, finials, candles, railings.
- **Lanterns:** in rhythm, on the facade's axes; soul lanterns for gloomy places.
- **Banners and heads:** heraldry over gates, statue finials.
- **Slabs in floors:** sunken half-block borders and patterns.

## 7. Mistakes checklist

1. Glass flush with the wall; full glass blocks everywhere.
2. No roof overhang, or overhang with no corbels and no underside fill.
3. Even roof spans with an ugly flat top; ridge not centred on the door.
4. One huge roof over a big footprint.
5. Roof and walls in the same material or value.
6. Arches of full blocks only (jagged), flat 3-wide tops, uneven sides; arch the same shade as wall.
7. Small circles expected to look round.
8. Detail everywhere with no focal point; entrance not the most detailed part.
9. Wrong stair `half` / `facing`; missing stair `shape` and fence/wall connection states in
   generated structures.
10. Identical towers; battlements with no caps or no walkway.
