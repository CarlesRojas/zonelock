package app.pinya.zonelock.menu;

import app.pinya.zonelock.block.CoreBlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** The focus slot of a core, as a one-slot container over the core itself (server side). */
public final class FocusContainer implements Container {
    private final CoreBlockEntity core;
    private final Player viewer;

    public FocusContainer(CoreBlockEntity core, Player viewer) {
        this.core = core;
        this.viewer = viewer;
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return core.focus().isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return core.focus();
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        return count <= 0 || core.focus().isEmpty() ? ItemStack.EMPTY : core.takeFocus();
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return core.takeFocus();
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        core.setFocus(stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    /** The size the focus allows changed: the screen shows it. */
    @Override
    public void setChanged() {
        core.setChanged();
        if (viewer instanceof ServerPlayer player)
            core.refreshScreen(player);
    }

    @Override
    public boolean stillValid(Player player) {
        return !core.isRemoved() && core.canEdit(player)
                && player.distanceToSqr(Vec3.atCenterOf(core.getBlockPos())) <= 64;
    }

    @Override
    public void clearContent() {
        core.takeFocus();
    }
}
