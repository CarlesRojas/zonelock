package app.pinya.zonelock.registry;

import app.pinya.zonelock.ZoneLock;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.Structure;

public final class ModTags {
    /** Blocks players may always break inside zones (cobwebs, glass panes meant to be smashed...). */
    public static final TagKey<Block> BREAKABLE_IN_ZONES = TagKey.create(Registries.BLOCK,
            ZoneLock.id("breakable_in_zones"));
    /** Blocks players may always place inside zones (for puzzles that need a block). */
    public static final TagKey<Block> PLACEABLE_IN_ZONES = TagKey.create(Registries.BLOCK,
            ZoneLock.id("placeable_in_zones"));

    /** Structures that get a zone when generated. Filled by the "Locked Structures" built-in data pack. */
    public static final TagKey<Structure> LOCKED_STRUCTURES = TagKey.create(Registries.STRUCTURE,
            ZoneLock.id("locked"));

    /** Items that enlarge the maximum size of a survival core, from smallest to largest. */
    @SuppressWarnings("unchecked")
    public static final TagKey<Item>[] FOCUS_TIERS = new TagKey[] {
            focus(1), focus(2), focus(3), focus(4), focus(5) };

    private static TagKey<Item> focus(int tier) {
        return TagKey.create(Registries.ITEM, ZoneLock.id("focus/tier_" + tier));
    }

    private ModTags() {
    }
}
