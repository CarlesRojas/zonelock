package app.pinya.pinyazonelock.block;

import app.pinya.pinyazonelock.ZoneLock;
import app.pinya.pinyazonelock.block.custom.ZoneLockCore;
import app.pinya.pinyazonelock.item.ModItems;
import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
  public static final DeferredRegister<Block> BLOCKS =
      DeferredRegister.create(ForgeRegistries.BLOCKS, ZoneLock.MOD_ID);

  public static final RegistryObject<Block> ZONE_LOCK_CORE =
      registerBlock(
          "zonelockcore",
          () ->
              new ZoneLockCore(
                  BlockBehaviour.Properties.of()
                      .strength(4f)
                      .lightLevel(state -> state.getValue(ZoneLockCore.ACTIVE) ? 10 : 0)
                      .sound(SoundType.DEEPSLATE_TILES))); // TODO change sound

  private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
    RegistryObject<T> toReturn = BLOCKS.register(name, block);
    registerBlockItem(name, toReturn);
    return toReturn;
  }

  private static <T extends Block> void registerBlockItem(String name, RegistryObject<T> block) {
    ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
  }

  public static void register(IEventBus eventBus) {
    BLOCKS.register(eventBus);
  }
}
