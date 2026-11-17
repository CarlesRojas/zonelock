package app.pinya.zonelock.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/** Where the inventory panel is drawn, so the quest card can stay clear of it. */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("leftPos")
    int zonelock$leftPos();

    @Accessor("topPos")
    int zonelock$topPos();

    @Accessor("imageWidth")
    int zonelock$imageWidth();

    @Accessor("imageHeight")
    int zonelock$imageHeight();
}
