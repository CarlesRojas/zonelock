package app.pinya.zonelock.registry;

import app.pinya.zonelock.menu.CoreMenu;
import app.pinya.zonelock.platform.Services;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
    public static final ModRegistry<MenuType<?>> MENUS = new ModRegistry<>(Registries.MENU);

    /** The core's screen: its settings, a slot for the focus block, and the player's inventory. */
    public static final RegistryEntry<MenuType<?>, MenuType<CoreMenu>> CORE = MENUS.register("amethyst_core",
            key -> Services.PLATFORM.menuType(CoreMenu::new));

    private ModMenus() {
    }
}
