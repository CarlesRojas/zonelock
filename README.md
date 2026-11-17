<p align="center"><img src="art/logo.png" width="128" alt="Zone Lock logo"></p>

# Zone Lock

**Seal an area with an Amethyst Core so nothing inside can be broken, built or skipped until the challenge is beaten.**

Build a dungeon around a hidden core and players have to actually play through it: no digging straight to the
end, no towering over the walls, no ender pearls past the puzzle. Reach the core, switch it off, and the place is
yours. Or find the Amethyst Tower in the wild.

Fabric and NeoForge · Minecraft 26.3 · Everything craftable in survival

## How it works

1. Craft a core and place it. Right-click it to open its screen (name and rules). Cores belong to nobody: anyone
   can set up a switched-off core.
2. Shape the zone in the world with a **Resonance Wand**: point at a wall of the switched-off zone; use pushes it
   away from you, sneak + use pulls it towards you (from inside that grows the zone, from outside it shrinks it).
   Zones never cross: a wall stops against another zone, and a core can't be placed inside one.
3. Place an **amethyst shard** on top: the crystal rises and the zone is sealed.
4. To open it, someone has to beat the core's challenge. Its crystal shatters and the zone is free.

There are two cores. The Amethyst Core is crafted; adding redstone turns it into a Redstone Core. Every core needs
the seals of its zone put out first; the core then decides what comes last:

| Core | What the challenger has to do |
|---|---|
| **Amethyst Core** | Put out every seal inside the zone, then switch the core off. With no seals in the zone, reaching it is enough. An **Amethyst Seal** goes out when touched; a **Redstone Seal** goes out with a redstone signal. Right-clicking a locked core sends a pulse of energy along the corridors to each seal that still burns. |
| **Redstone Core** | Put out its seals (if any), then power the core. Perfect as the output of a redstone puzzle. Its redstone glows bright red while it gets a signal and dark red while it doesn't, like a repeater (the Redstone Seal too). |

**An armed core is a challenge for everyone, whoever armed it included**: nothing about it or its seals can be changed
until it is beaten (creative map makers excepted). A beaten core keeps its configuration and is anyone's to set up
again; placing a shard on it restarts the challenge: every seal burns again.

## What a sealed zone blocks

Each rule can be turned off per core.

- Breaking and placing blocks, fluids, boats, armor stands (doors, buttons, levers and chests always work)
- Explosions, pistons pushing across the border, fluids flowing in or out, fire
- Mob griefing (endermen, withers, ravagers, trampling...)
- Ender pearls and chorus fruit landing inside
- Elytra gliding and riptide launches inside
- Breaking paintings, item frames and armor stands
- Tools changing blocks (stripping, tilling, bone meal, flint and steel...)

Creative players bypass everything, so builders can keep working.

## Feedback without breaking immersion

- The border is invisible. Hold a **Resonance Wand** to see it: whole walls of small amethyst runes, sliding along
  while the zone is armed, still and dimmer while it is off (the wall the wand points at lights up).
  Without the wand, coming close to a sealed zone shows a patch of its wall around you, fading out (it can be turned
  off in the mod's settings: Mod Menu on Fabric, the mods list on NeoForge). A block that refuses to change ripples where you hit
  it, over its real shape: the solid surface of blocks, the model of things you walk through (wheat, flowers, rails,
  torches), the plate of an item frame. No text pops up.
- Inside a zone its quest card shows in the top-left corner: the zone name and what's left ("Put out the seals 2/4").
- The core's floating crystal spins while armed.
- The core's screen closes with Escape or the inventory key, like a chest.

## Survival progression

A core reaches 4 blocks per side. Put a focus block in the slot of its screen (like a beacon's payment) to grow it:
iron/copper 8 · gold/lapis 16 · emerald/amethyst 32 · diamond 64 · netherite 128.
That is the same for everyone, creative included. A dungeon's core comes with the zone its dungeon needs, but once
beaten it is an ordinary core: armed again, the zone shrinks to what its focus allows.

## World generation

- **Zone Lock: Dungeons**: the **Amethyst Tower**, a rare hand-built tower with a library puzzle and a parkour
  shaft, found in dark forests, pale gardens and old-growth taigas. The amethyst eats the ground around it.
  It is a built-in data pack, enabled by default: turn it off in **Create World > Data Packs** for worlds without it.

## For map makers

- In creative you can edit any core. **Map lock** stops everyone but creative operators from touching it: while it
  is armed players only get the challenge, and once it is off it does not react to them at all.
- `/zonelock list | here | lock | unlock` for operators.
- Tags: `#zonelock:breakable_in_zones` and `#zonelock:placeable_in_zones` let you mark blocks that players may
  break or place inside zones (cobwebs to cut, blocks for a puzzle...). `#zonelock:focus/tier_1..5` set the focus
  blocks.
- Core block entity data (for structure templates):
  `{config:{name:"...",extents:{up:8,down:2,north:8,south:8,east:8,west:8},rules:1023,seals:1b,redstone:0b,config_locked:1b},armed:1b}`.

## Building from source

Requires **JDK 25** (Gradle itself must run on it).

```
./gradlew build                      # both jars in fabric/build/libs and neoforge/build/libs
./gradlew :fabric:runClient          # or :neoforge:runClient
./gradlew :neoforge:runGameTestServer :fabric:runServerSmokeTest   # boots servers, checks every mixin applies
./gradlew :fabric:runClientGameTest  # plays a scripted scenario in the real client and takes screenshots
```

Project layout ([MultiLoader template](https://github.com/jaredlll08/MultiLoader-Template)): almost everything
lives in `common/`, compiled against vanilla, with protection done through Mixins shared by both loaders.
`fabric/` and `neoforge/` only hold the loader glue (registration, events, networking, renderers).

Releases: push a `v*` tag; GitHub Actions builds and publishes to Modrinth, CurseForge and GitHub Releases
(set `modrinth_id` / `curseforge_id` in `gradle.properties` and the `MODRINTH_TOKEN` / `CURSEFORGE_TOKEN` secrets).

## License

MIT
