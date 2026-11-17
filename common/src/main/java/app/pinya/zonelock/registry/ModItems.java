package app.pinya.zonelock.registry;

import java.util.List;
import java.util.function.Supplier;

import app.pinya.zonelock.item.ResonanceWandItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public final class ModItems {
    public static final ModRegistry<Item> ITEMS = new ModRegistry<>(Registries.ITEM);

    public static final RegistryEntry<Item, BlockItem> CORE = ITEMS.register("amethyst_core",
            key -> new BlockItem(ModBlocks.CORE.get(),
                    new Item.Properties().useBlockDescriptionPrefix().rarity(Rarity.UNCOMMON).setId(key)));

    public static final RegistryEntry<Item, BlockItem> REDSTONE_CORE = ITEMS.register("redstone_core",
            key -> new BlockItem(ModBlocks.REDSTONE_CORE.get(),
                    new Item.Properties().useBlockDescriptionPrefix().rarity(Rarity.UNCOMMON).setId(key)));

    public static final RegistryEntry<Item, BlockItem> SEAL = ITEMS.register("amethyst_seal",
            key -> new BlockItem(ModBlocks.SEAL.get(), new Item.Properties().useBlockDescriptionPrefix().setId(key)));

    public static final RegistryEntry<Item, BlockItem> REDSTONE_SEAL = ITEMS.register("redstone_seal",
            key -> new BlockItem(ModBlocks.REDSTONE_SEAL.get(), new Item.Properties().useBlockDescriptionPrefix().setId(key)));

    public static final RegistryEntry<Item, ResonanceWandItem> WAND = ITEMS.register("resonance_wand",
            key -> new ResonanceWandItem(new Item.Properties().stacksTo(1).setId(key)));

    /** Not obtainable: the crystal floating over an armed core (a 3D amethyst cluster). */
    public static final RegistryEntry<Item, Item> CORE_CRYSTAL = ITEMS.register("core_crystal",
            key -> new Item(new Item.Properties().stacksTo(1).setId(key)));

    /** Contents of the creative tab, in order. */
    public static final List<Supplier<ItemStack>> CREATIVE_TAB = List.of(
            () -> new ItemStack(CORE.get()),
            () -> new ItemStack(REDSTONE_CORE.get()),
            () -> new ItemStack(SEAL.get()),
            () -> new ItemStack(REDSTONE_SEAL.get()),
            () -> new ItemStack(WAND.get()));

    private ModItems() {
    }
}
