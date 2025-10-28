package app.pinya.pinyazonelock.block.entity;

import app.pinya.pinyazonelock.ZoneLock;
import app.pinya.pinyazonelock.block.ModBlocks;
import app.pinya.pinyazonelock.block.entity.custom.CoreEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocksEntities {
  public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
      .create(ForgeRegistries.BLOCK_ENTITY_TYPES, ZoneLock.MOD_ID);

  public static final RegistryObject<BlockEntityType<CoreEntity>> ZONE_LOCK_CORE_ENTITY = BLOCK_ENTITIES
      .register("zonelockcore_entity",
          () -> BlockEntityType.Builder.of(CoreEntity::new, ModBlocks.ZONE_LOCK_CORE.get()).build(null));

  public static void register(IEventBus eventBus) {
    BLOCK_ENTITIES.register(eventBus);
  }
}
