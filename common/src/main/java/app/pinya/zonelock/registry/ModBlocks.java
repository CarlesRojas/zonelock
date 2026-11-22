package app.pinya.zonelock.registry;

import java.util.Set;

import app.pinya.zonelock.block.CoreBlock;
import app.pinya.zonelock.block.CoreBlockEntity;
import app.pinya.zonelock.block.SealBlock;
import app.pinya.zonelock.block.SealBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class ModBlocks {
    public static final ModRegistry<Block> BLOCKS = new ModRegistry<>(Registries.BLOCK);
    public static final ModRegistry<BlockEntityType<?>> BLOCK_ENTITIES = new ModRegistry<>(
            Registries.BLOCK_ENTITY_TYPE);

    /** Seals (or, with none in its zone, just reaching it). */
    public static final RegistryEntry<Block, CoreBlock> CORE = BLOCKS.register("amethyst_core",
            key -> new CoreBlock(coreProperties(key), CoreBlock.Lock.SEALS));
    public static final RegistryEntry<Block, CoreBlock> REDSTONE_CORE = BLOCKS.register("redstone_core",
            key -> new CoreBlock(coreProperties(key), CoreBlock.Lock.REDSTONE));

    /** Put out by touching it. */
    public static final RegistryEntry<Block, SealBlock> SEAL = BLOCKS.register("amethyst_seal",
            key -> new SealBlock(sealProperties(key), false));

    /** Put out by a redstone signal. */
    public static final RegistryEntry<Block, SealBlock> REDSTONE_SEAL = BLOCKS.register("redstone_seal",
            key -> new SealBlock(sealProperties(key), true));

    public static final RegistryEntry<BlockEntityType<?>, BlockEntityType<CoreBlockEntity>> CORE_ENTITY = BLOCK_ENTITIES
            .register("amethyst_core", key -> new BlockEntityType<>(CoreBlockEntity::new,
                    Set.of(CORE.get(), REDSTONE_CORE.get())));

    private static BlockBehaviour.Properties coreProperties(net.minecraft.resources.ResourceKey<Block> key) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(5.0F, 1200.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.AMETHYST)
                .lightLevel(state -> state.getValue(CoreBlock.ARMED) || state.getValue(CoreBlock.PENDING) ? 11 : 3)
                .pushReaction(PushReaction.IMMOVEABLE)
                .noOcclusion()
                .setId(key);
    }

    public static final RegistryEntry<BlockEntityType<?>, BlockEntityType<SealBlockEntity>> SEAL_ENTITY = BLOCK_ENTITIES
            .register("amethyst_seal", key -> new BlockEntityType<>(SealBlockEntity::new, Set.of(SEAL.get(), REDSTONE_SEAL.get())));

    private static BlockBehaviour.Properties sealProperties(net.minecraft.resources.ResourceKey<Block> key) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(3.0F, 1200.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.AMETHYST_CLUSTER)
                .lightLevel(state -> state.getValue(SealBlock.LIT) ? 9 : 0)
                .pushReaction(PushReaction.IMMOVEABLE)
                .noOcclusion()
                .setId(key);
    }

    private ModBlocks() {
    }
}
