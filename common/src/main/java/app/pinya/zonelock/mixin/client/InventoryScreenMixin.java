package app.pinya.zonelock.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import app.pinya.zonelock.client.ZoneHud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

/** The zone quest card only shows in the inventory (survival and creative), never over the game. */
@Mixin({ InventoryScreen.class, CreativeModeInventoryScreen.class })
abstract class InventoryScreenMixin {
    /** Width of the recipe book panel and how far it pushes the inventory to the right when open. */
    private static final int BOOK_WIDTH = 147;
    private static final int BOOK_OFFSET = 86;
    /** Creative tabs stick out of the panel above and below. */
    private static final int TAB_HEIGHT = 32;

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void zonelock$questCard(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        AbstractContainerScreenAccessor panel = (AbstractContainerScreenAccessor) this;
        boolean creative = (Object) this instanceof CreativeModeInventoryScreen;
        int left = panel.zonelock$leftPos();
        int top = panel.zonelock$topPos();
        int bottom = top + panel.zonelock$imageHeight();
        if (creative) {
            top -= TAB_HEIGHT;
            bottom += TAB_HEIGHT;
        } else {
            // The recipe book can open at any time: keep clear of where it would be.
            left = Math.min(left, (graphics.guiWidth() - BOOK_WIDTH) / 2 - BOOK_OFFSET);
        }
        ZoneHud.renderInInventory(graphics, left, top, bottom);
    }
}
