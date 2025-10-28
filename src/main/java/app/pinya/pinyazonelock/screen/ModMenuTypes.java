package app.pinya.pinyazonelock.screen;

import app.pinya.pinyazonelock.ZoneLock;
import app.pinya.pinyazonelock.screen.custom.CoreMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ZoneLock.MOD_ID);

    public static final RegistryObject<MenuType<CoreMenu>> ZONE_LOCK_CORE_MENU = MENUS
            .register("zone_lock_core_menu", () -> IForgeMenuType.create(CoreMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
