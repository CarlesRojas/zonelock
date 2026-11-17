package app.pinya.zonelock.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public interface PlatformHelper {
    /** "fabric" or "neoforge". */
    String loaderName();

    boolean isModLoaded(String modId);

    boolean isDevelopmentEnvironment();

    /** The game's config folder. */
    java.nio.file.Path configDir();

    /** Builds a menu on the client from the block position the server sends when it opens. */
    @FunctionalInterface
    interface MenuFactory<T extends AbstractContainerMenu> {
        T create(int id, Inventory inventory, BlockPos pos);
    }

    /** A menu type whose opening carries a block position (each loader has its own way). */
    <T extends AbstractContainerMenu> MenuType<T> menuType(MenuFactory<T> factory);

    /** Opens a menu made by {@link #menuType}, sending {@code pos} along. */
    void openMenu(ServerPlayer player, MenuProvider provider, BlockPos pos);
}
