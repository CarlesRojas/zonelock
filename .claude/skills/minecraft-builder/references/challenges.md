# Challenges: parkour, redstone puzzles and other trials

Read this when a dungeon path needs a challenge. Everything must work as pre-built blocks in a
structure template: no command blocks, no datapacks. In Zone Lock a path usually ends at a seal
the player touches (`mode=touch`) or a seal that goes out when it receives a redstone signal
(`mode=redstone`).

Source-backed numbers (jump distances, comparator formulas, sculk frequencies) come from the
Minecraft Wiki and mcpk.wiki. The concrete puzzle layouts in section 2.3 are untested designs:
place the template once and verify before shipping.

## Contents
0. Rules for pre-built challenges
1. Parkour
2. Redstone puzzles
3. Other challenge types
4. Challenge design principles

---

## 0. Rules for pre-built challenges

- **Puzzles never depend on items.** Players lose items: hoppers swallow them, dropped items
  despawn after 5 minutes or fall into lava, players carry them off, a book taken from a lectern
  or a frame punched empty breaks the puzzle for good. Use only redstone inputs the player
  operates in place: buttons, levers, pressure plates (stepping on them), tripwire, bells, note
  blocks, doors and trapdoors, and blocks the player can only toggle, never remove. Items may
  appear as decoration, loot or lore, never as a puzzle input. The item-based designs below (relic
  hopper, lectern page, item frame, archery) are reference only: do not use them in dungeons.
  Exception only when the user asks for it (the Amethyst Tower keeps its 1.x chiseled-bookshelf
  puzzle): the items live in the same room, there is no hopper or lava to lose them in, and the room
  holds more of them than the puzzle needs.
- **Clues come in two layers, and the penalty stays a real risk.** First a clue that narrows the
  choice to the inputs that matter (two gaps in otherwise full shelves: the right one and the trap,
  nothing else), then a second, quieter clue that tells them apart (green candles by the poisoned
  shelf, purple by the right one). Without the first the puzzle is blind guessing; without the
  second it is a coin toss. The user preferred no decoys: every candidate does something.
- **Chiseled bookshelf slots (verified in game, Amethyst Tower).** A comparator behind the shelf
  outputs the number of the slot touched last (1-3 top row, 4-6 bottom row; taking or inserting a book
  both count). Each dust loses 1, so a repeater after N dust fires from signal N+1: that alone only
  isolates slot 6 ("6 or more"). For any other slot use an exact match: a repeater "k or more" into
  the back of a comparator in **subtract** mode, a repeater "k+1 or more" into its side, both delay 1
  so they switch together. A threshold ("k or more") is fine for a trap shelf; the right answer must
  be exact or the books below it solve it too. Never run dust one block above and beside another
  circuit's wires (here the poison grid under the floor): wire climbs diagonally into it and the two
  circuits merge. Carry the signal across with a powered block and a repeater instead.
- **Headroom at a take-off (measured with a scripted player).** A block right above a ledge allows
  the jump only from its very edge (a window of ~0.1 block, sprint jump). An upside-down stair there,
  open half toward the landing, widens it to ~0.5 block without changing the look.
- **A builder's own playtest beats the model.** If the user has climbed a route in the game, keep it
  even when `check_paths.py` says it cannot be done: the model ignores sprint momentum and slime
  timing. Verified case: a +1 jump to a ladder from a ledge with a block right above the head fails
  standing or walking, but works with a one-block run-up and a sprint-jump at the very edge (the
  momentum carries the head out from under the block). Test such jumps with a scripted player
  (run-up, sprint, jump at the edge, release forward on arrival) before "fixing" them.
- **Prove playability:** run `scripts/check_paths.py` (see SKILL.md step 9) and pass the puzzle's
  inputs as `--target` so they are proven reachable, and the door the puzzle opens as
  `--puzzle-door`; and if the project can run the game, check the redstone once in game
  (simulators and in-head reasoning miss real update behaviour).
- **Puzzle gate that holds until solved (verified in game):** an iron door turned
  sideways in a 1-wide passage (`facing` along the passage's cross axis, e.g. `facing=east` in a
  north-south passage), stored `open=true,powered=true`, powered from the block above it by the
  puzzle's "wrong" bus. Open, its panel lies across the passage and blocks it; when the correct
  answer kills the bus, the door closes flat against the side and the way is free. No button near
  it (any button within 2 blocks lets the checker, and a player, work it by hand). Once solved, it
  stays solved as long as the inputs keep their state (copper bulbs), so a player beyond the gate
  is never shut in.

- **Store redstone in its settled state.** Template placement does not recompute anything: write
  each torch's `lit`, each wire's real `power` (15 minus distance) and its `north/east/south/west`
  connections, `powered` on repeaters and comparators, a comparator's `OutputSignal` in its block
  entity, item frame `ItemRotation`.
- **The game places a template in three passes:** full blocks, then non-full blocks, then every
  block that carries data (`nbt` in the template: dispensers, chests, comparators, chiseled
  bookshelves...), each pass in height order. So (verified in game, Amethyst Tower):
  - anything that needs support (carpet, candle, pressure plate, torch, button on top) standing on a
    block-entity block goes down over air and drops as an item as soon as a neighbour is placed.
    **Fix: give it an empty data tag (`"nbt": {}`)**: it moves to the last pass and, being higher,
    goes down after the block under it. Nothing else changes (the game logs no warning);
  - an observer watching floor wiring can fire once while the template is placed (the wires change as
    their neighbours arrive; moving the observers to the last pass does not stop it). Fine for a
    dispenser trap (it happens at chunk generation and costs one shot); never let it drive something
    that must stay put.
- **Levers and buttons power all six neighbours,** not only the block they hang on. A dispenser,
  trapdoor, door or piston beside or under a lever fires or opens with it, and a dispenser whose
  upper neighbour touches the lever fires through quasi-connectivity. Keep traps and outputs at least
  one block away from every input, or the right answer sets off the penalty.
- **One output:** a repeater or dust pointing into the seal (or the block under it).
- **Falls cost time, never a life.** Water, slime, hay, powder snow, or a floor at most 3 below,
  plus a way back to the last checkpoint. Never lava, void or magma under a route.
- Wiring goes in a service layer under the floor or behind a 1-block wall; only feedback (lamps,
  bulbs) and inputs are visible.
- Rotation is applied by the structure system, which rotates states and entities together. Do not
  design anything that only works facing one compass direction.

## 1. Parkour

### Jump physics (Java)
"Gap" = air blocks between take-off and landing. With a sprint run-up:

| Height change | Practical limit |
|---|---|
| +1 (full block up) | 3-block gap is the max; 4+1 is impossible |
| +0.5 (slab up) | 3 easy, 4 hard |
| flat | 4 is the real max (needs 2+ blocks of run-up) |
| -1 | 4 comfortable, 5 expert |
| -2 | 5 hard |

Without sprinting about 2 is the limit.

| Special jump | Max | Note |
|---|---|---|
| neo (around a wall at least 2 high) | 3 | |
| fence to fence, wall to wall | 3 | tops at 1.5, only between same-level posts |
| bars / panes / chains | 3 | thin hitboxes |
| trapdoor (closed) | 4 | |
| ladder catch | 5 | sneak to hold |
| head-hitter | ~1-2 | ceiling 2 above the take-off |

Block behaviour: ice keeps momentum (packed or blue ice; plain ice melts near light); blue ice
under a trapdoor ceiling = speed tunnel; soul sand shortens jumps; honey slows and lets you slide
down walls; slime bounces unless sneaking (good for drop-and-bounce); big dripleaf tilts and drops
you shortly after you stand on it (timed steps); vines have no collision; scaffolding can be
climbed and stood on.

Landings from easiest to hardest: full block, slab/stair, trapdoor/fence/wall top, pane/bar/chain/
end rod, ladder catch.

### Difficulty tiers
- **Easy** (first third): flat gaps 1-3, +1 with gaps 1-2, -1 with gaps 2-3, full-block landings,
  2+ run-up, every landing visible from the take-off, no ceilings within 3.
- **Medium:** 3+1 or 4 flat with run-up, 4 down 1, diagonal offsets, slab/stair/trapdoor landings,
  single fence or wall tops, ladder jumps 2-3, a neo round a 1-wide wall, a gentle head-hitter.
- **Hard** (1-3 per path, each followed by a rest): 4 flat from 1 run-up, 3+1 from 1 run-up, 3-gap
  neo, 3-gap post jumps, ladder 4-5, trapdoor 4, ice-boosted long jumps, slime bounce onto a narrow
  landing.

### Pacing
- Groups of 4-7 jumps, then a lit **rest platform** (3x3+), ideally with a lore item, chest or
  food. One peak jump per group, at its end, never at the start.
- Teach each move alone before combining moves.
- 15-30 jumps for an easy/medium path, 30-45 for a hard one.

### Readability
- Jump blocks contrast with the background (polished blackstone on deepslate, gold or copper on
  stone). Light the route, keep the fall area darker.
- From each rest platform the next landing is visible. No blind jumps; mark unavoidable ones with a
  light.
- A head-hitter used as a challenge gets a conspicuous ceiling block (hanging lantern, contrasting
  slab). Low ceilings elsewhere stop players skipping sections.

### Fail states and the way back
Water at least 1 deep (or slime, hay, powder snow, floor at most 3 down) under every section.
Then think about the return as its own little route:
- **Nothing climbable in the fall line.** Ladders, vines, scaffolding and twisting vines catch a
  falling player: if the climb-out ladder hangs where people fall, they grab it mid-fall and the
  fall becomes a free lift. Keep every climbable at least 2 blocks away from any spot a player can
  fall past, ideally in a side alcove or behind a corner.
- **The way back is not the way forward.** From the catch basin, a separate passage (a side door,
  a corridor, a stair behind a wall) leads back to the start of the section, not onto the jump
  route itself, and it must not let the player skip ahead. A short walk of 5-15 seconds is right.
- **One-way where needed.** If the return route ends above or beside the route, make its last
  step a drop or a door that only opens from the return side, so it cannot be used as a shortcut.
- In vertical shafts, add a catch ring or alcove every 6-8 blocks of height so a fall does not
  reset the whole shaft, each with its own way back to its own checkpoint.
- Verify with `scripts/check_paths.py`: a basin with no way back shows up as a trap.

### Parkour that belongs to the architecture
Never floating blocks in a void. Read `parkour.md` for the full method; in short, make the route
out of the place:
- **Broken bridge:** missing spans, stair and slab rubble, chains hanging where it collapsed.
- **Ruined floor over a flooded crypt:** surviving tiles are the landings, the water is the net.
- **Colonnade:** jump between broken pillar tops; walls and fences as capitals.
- **Library:** bookshelf stacks of different heights, ladders on shelves, trapdoor ledges, lectern
  clues on rest platforms.
- **Chandeliers:** chain-hung trapdoor or slab platforms lit by lanterns underneath.
- **Vertical shaft** (well, tower, mine): ladder and vine segments, trapdoor ledges, neos round
  buttresses, water at the bottom.
- **Mine:** rails on slabs, scaffolding, beams.

### Mistakes
Invisible landings; surprise head-hitters; 4+1; 4 flat without run-up; hardest jump first; long
stretches without rest; falls that reset everything; one repeated move; jump blocks that look like
the background; decoration that looks like a landing but is not.

## 2. Redstone puzzles

### 2.1 Mechanics reference
- **Comparator:** reads containers: signal = floor(1 + (sum of item fractions / slots) x 14).
  A **hopper (5 slots) with non-stackable items gives 3, 6, 9, 12, 15** for 1-5 items: perfect for
  "place N relics". A chest is useless for counting. Compare mode: output = rear unless a side is
  greater. Subtract mode: max(rear - max(side), 0). Reads through a solid block.
- **Item frame:** comparator output 1 + rotation (1-8); empty = 0. It is an entity (`ItemRotation`
  0-7, `Facing`, `Item`); do not set `Fixed` on puzzle frames.
- **Lectern:** with a 15-page book the signal equals the page number; turning a page also pulses.
- **Chiseled bookshelf:** comparator gives 1-6 for the last slot touched.
- **Crafter:** comparator gives the number of filled or disabled slots (0-9).
- **Target block:** 1-15 by accuracy; arrows keep it on 20 ticks.
- **Light weighted pressure plate:** signal = number of entities (items count) up to 15.
- **Copper bulb:** toggles on each pulse and remembers; a comparator reads a lit bulb as 15. Use
  `waxed_` variants so it never oxidises.
- **Sculk sensor:** range 8, active 30 ticks; ignores wool and wool blocks vibrations through it;
  sneaking suppresses frequencies 1-3. Frequencies: 1 step, 2 landing/projectile hit, 3 shooting or
  using an item, 9 close, 10 open/button/lever/note block, 11 block state change (bell, lectern,
  item frame), 12 break, 13 place, 15 death/explosion.
- **Calibrated sculk sensor:** range 16; a signal of strength N into its amethyst-side input filters
  it to frequency N.
- **Sculk shrieker:** `can_summon=false` is harmless; `true` brings Darkness and eventually a warden.
- **Observer:** `facing` is the direction it watches; 2-tick pulse out of the back.
- **Repeater:** `facing` points from output to input (opposite of the signal direction); `delay`
  1-4; locked by a powered repeater or comparator into its side.
- **Quasi-connectivity (Java):** pistons, droppers and dispensers count as powered when the space
  above them (or diagonally above) is powered. Keep powered dust away from those spaces unless
  intended.

### 2.2 Robust wiring in a template
1. Settled state stored for everything (see section 0).
2. Explicit wire connections; a dust dot with all sides `none` does not power neighbours.
3. At most 15 dust between repeaters.
4. Repeaters as diodes at junctions.
5. Service layer: components at floor -1 on blocks at -2, opaque floor above; windows only for
   feedback. Use repeaters or glowstone / upside-down slab towers to change level.
6. A single gated output into the seal.
7. **Feedback for every correct input:** a lamp or bulb ("2 of 3 lit"), a note block on success, a
   visible change next to the seal on unlock.
8. **Clues in the world:** a lectern riddle, banners or art above inputs, a "master" item frame
   elsewhere showing the target rotation, candle counts equal to a page or rotation number.

### 2.3 Puzzle designs (untested, verify once in game)
Conventions: y=1 component layer, y=0 solid floor, x east, z south, player at z <= -2.

**P1. Lever combination lock (torch NOR).**
```
        x=0  1   2   3   4   5   6   7   8   9   10  11
z=-1    L1   .   L2  .   L3  .   L4                      levers on the wall, facing=north
z= 0    W    W   W   W   W   W   W   W   W               solid wall y=1..3
z= 1    T    .   d   .   T   .   d   .   .
z= 2    d    d   d   d   d   d   d   d   d   R   B   T'  -> dust -> seal
```
T = `redstone_wall_torch[facing=south,lit=true]` on the wall behind its lever; d = dust;
R = `repeater[facing=west,delay=1,powered=true]`; B = solid; T' = `redstone_wall_torch[facing=east,lit=false]`.
Levers over T must be ON (turns the torch off); levers over d must be OFF (an ON lever powers the
bus). T' lights only when the bus is unpowered = correct combination. Settled bus powers x0..8:
15,14,13,14,15,14,13,12,11; the side dust at (2,1) and (6,1) = 12. Use 5-6 levers or a clue so it
cannot be brute-forced (4 levers = 16 combinations).

**N-input AND** (same torch pattern): each input -> repeater -> its own block Bi with a lit wall
torch on the far face; all torches touch one dust bus; bus -> repeater -> block O with an unlit
output torch. Output lights only when every input is on.

**P2. Exact item-frame rotation ("window" detector).** Comparator behind the frame's block reads
1-8. Split into two branches: branch A with k dust then a repeater fires when signal >= k; branch B
with k+1 dust then a repeater fires when signal >= k+1. A subtract comparator with A at the rear and
B at the side outputs 15 only when the signal is exactly k. Clue: a mural or master frame showing
the correct angle. Combine several frames with the N-input AND.

**P3. Relic offering.** `hopper[facing=down]` pointing into a solid non-container block (items stay
in), comparator reading it, then 8 dust, then a repeater, then the seal. Three non-stackable relics
give 9 >= 8; two give 6. The block above the hopper must be air or solid, never a container. It
cannot check which item, so lore must say what to bring. Pairs with a scavenger hunt.

**P4. Lectern page.** 15-page written book (`has_book=true`), comparator, window detector with
k = target page. The riddle is in the book or the room ("the knight sleeps where the candles number
seven").

**P5. Archery gallery.** 3 target blocks 10-15 away, each pulsing a `waxed_copper_bulb` beside it
(visible feedback); comparators read the bulbs into an N-input AND -> seal. Twist: wire one target
to two bulbs so it becomes a small lights-out puzzle; draw the wiring as a clue. Supply a bow and
arrows before the room.

**P6. Listening altar (sculk + set-only latch).** Redstone block -> 5 dust (15..11) into the
amethyst input of a calibrated sculk sensor (now hears only frequency 11, e.g. ringing a bell;
footsteps are ignored). Its pulse fires a dropper holding one item into a hopper; a comparator
reads the hopper and stays on forever (later pulses find the dropper empty). Keep the dropper clear
of quasi-connectivity.

**Redstone-only designs (preferred).**

**P7. Toggle pairs ("bells that still hang must ring") - verified in the real game.** A row of
`waxed_copper_bulb` (the "bells") with a button between each pair; a button's own neighbours are the
two bulbs beside it, so one press toggles both (no wiring needed for the input). A comparator
behind each bulb reads it (15 lit / 0 off) through a solid block. The bulbs that must end ON drive
blocks carrying `redstone_wall_torch` (lit when the bulb is off) into a shared bus; the bulbs that
must end OFF feed the bus directly through their comparators. The bus is unpowered only in the
target state; an inverter after it powers the seal. Settled start state: all bulbs lit. Clue in the
room (fallen bells on the floor under the ones that must be silent). It is a small lights-out
puzzle: buttons only, nothing to lose, and the bulbs themselves are the feedback.

**P8. Pull the right books.** Buttons placed on the faces of bookshelves (or chiseled bookshelves
used as decoration only) among many plain ones; the right combination of 3-4 "books" lights lamps
and opens the way. Wire each correct button into a copper bulb (toggles, remembers, shows state)
and wrong buttons into a reset (a pulse that toggles any lit bulb off, or a trap such as a
splash-potion dispenser of slowness or poison that fires upward from under the carpet). Clue: a
lectern book or wall banners/paintings that pick out the shelves. Read the bulbs into an N-input
AND that powers the seal or a piston door.

**P9. Floor path (pressure plates).** A tiled floor with pressure plates styled into the floor
pattern (stone, polished blackstone or weighted plates on matching blocks; a plate cannot sit under
carpet). Stepping on a wrong tile fires a trap (dispenser, a piston pulling the floor into water) and the
right sequence toggles bulbs in order. Show the safe pattern somewhere else in the room (a mosaic,
a map on the wall).

Also possible with the same parts: a weighted plate "lay exactly 4 items here" (window detector
k=4), a chiseled bookshelf "the 4th tome", an observer that fires when something is placed on a
marked pedestal (latched with the dropper-hopper latch). Daylight sensors only work under sky.

## 3. Other challenge types

- **Stealth sculk gallery:** sculk, sensors and harmless shriekers, a safe route of wool or carpet;
  sneaking helps. Penalty: pistons pull the floor (drop into water, walk back) or an iron door
  closes for a while. Start with a demo room where a sensor visibly lights a lamp. Projectiles and
  dropped items still make noise.
- **Maze:** walls 3+ high with a ceiling; unique landmarks at junctions (fountain, statue,
  coloured lamp, banner); a wall map or lectern directions; 1-2 minutes of wandering; small loot in
  dead ends; the exit visible from a balcony once found.
- **Combat arena:** a `trial_spawner` (finite waves, ejects rewards, cools down) rather than an
  endless spawner. It gives no redstone, so the seal stays physically reachable past the fight.
  Cover, light, an escape route, food before the fight; telegraph with bones and arrows on the
  floor.
- **Archery gallery:** see P5; targets revealed by pistons for rhythm; a way to recover arrows.
- **Flooded swim:** air pockets every 8-10 blocks of swimming (an alcove above the water line, or a
  door or trapdoor inside the water); the first within 6 blocks; kelp, sea pickles and glow lichen
  to light and guide. Players have about 15 seconds of air.
- **Collapsing path:** big dripleaf stepping stones, or a sticky-piston floor retracting behind the
  player; always over water; show one harmless collapse first.
- **Darkness:** an unlit vault with a spawn-proof floor (slabs or carpet), torches supplied at the
  entrance, or copper bulbs the player toggles to light the route; glow lichen hints; no drops in
  the dark.
- **Minecart or boat ride:** a breather between harder paths; detector rail at the end powers the
  seal; entities and chunk loading make it fragile.
- **Scavenger hunt:** three relics hidden in side rooms with visible tells (lit alcove, cracked
  wall, lectern clue), offered at the P3 altar; one lamp per relic as a counter.
- **Descent drop:** a deep shaft into water at least 2 deep and 3x3; ledges to dodge, honey walls,
  slime pads; obstacles visible from the top.
- **Vault room:** a `vault` opened by a trial key found elsewhere; gives loot, not redstone.

## 4. Challenge design principles

1. **Teach, develop, twist, conclude:** introduce a mechanic safely, make it harder, twist it once,
   end with a short combined test and the reward.
2. **Telegraph:** the goal (seal) visible early, through bars or across the pit; the mechanism
   shown before it is asked for (a lamp wired to a lever in plain view).
3. **Teach without text:** landmarks, contrasting materials for interactive things (levers on gold,
   frames on polished blocks), worn paths, light lines. Signs and lecterns are for lore and riddles,
   never "PUZZLE: pull the levers".
4. **Fair:** solvable by observing and thinking, not luck or brute force. Complex is fine, obtuse is
   not. Never punish a first attempt the player could not have predicted.
5. **Curve and contrast:** difficulty rises; alternate dark and bright, cramped and open, puzzle and
   movement; no back-to-back puzzles.
6. **Feedback** for every correct input, and the final unlock is an event (light, sound, a route
   opening).
7. **Rewards** at rest platforms and dead ends; hidden optional chests reward exploring.
8. **Theme:** the challenge fits the room (an ice slide in a frozen crypt, a page riddle in a
   library).
9. **Second hints** for stuck players: a harder-to-find secondary clue that does not spoil the first.
