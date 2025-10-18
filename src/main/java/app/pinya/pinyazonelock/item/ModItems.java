package app.pinya.pinyazonelock.item;

import app.pinya.pinyazonelock.ZoneLock;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModItems {
  public static final DeferredRegister<Item> ITEMS =
      DeferredRegister.create(ForgeRegistries.ITEMS, ZoneLock.MOD_ID);

  public static void register(IEventBus eventBus) {
    ITEMS.register(eventBus);
  }
}
