# Amethyst Tower

The mod's dungeon (`zonelock:amethyst_tower`). `remaster.py` builds the shipped template
(`common/src/main/resources/resourcepacks/dungeons/data/zonelock/structure/amethyst_tower.nbt`)
from the Zone Lock 1.x tower (`amethyst_tower_1x.nbt`), keeping it block for block and applying only
the 2.0 changes: placement order of the blocks with data, the library puzzle's book slots and hidden
circuit, the stair over the last jump, the current Amethyst Core, the core recipe in the item frames,
the chest with everything for one core, and the corrupted ground ring (marked for the
`zonelock:surface_snap` processor so it follows the terrain).

```
python art/structures/amethyst_tower/remaster.py
python .claude/skills/minecraft-builder/scripts/check_paths.py art/structures/amethyst_tower/amethyst_tower_solved.nbt
```

The second line checks every target is reachable (it ignores sprint jumps, so the last jump of the
shaft shows as unreachable: it was tested in the game with a bot).
