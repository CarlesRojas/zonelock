package app.pinya.pinyazonelock.screen.custom;

import com.mojang.blaze3d.systems.RenderSystem;

import app.pinya.pinyazonelock.ZoneLock;
import app.pinya.pinyazonelock.block.custom.Core;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CoreScreen extends AbstractContainerScreen<CoreMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ZoneLock.MOD_ID,
            "textures/gui/core.png");

    private static final ResourceLocation ACTIVE_TEXTURE = ResourceLocation.fromNamespaceAndPath(ZoneLock.MOD_ID,
            "textures/gui/core_active.png");

    private boolean lastActiveState;

    public CoreScreen(CoreMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 247;
        this.inventoryLabelY = this.imageHeight - 94;
        this.lastActiveState = menu.blockEntity.getBlockState().getValue(Core.ACTIVE);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        boolean isActive = menu.blockEntity.getBlockState().getValue(Core.ACTIVE);

        if (isActive != lastActiveState)
            lastActiveState = isActive;

        ResourceLocation texture = isActive ? ACTIVE_TEXTURE : TEXTURE;
        RenderSystem.setShaderTexture(0, texture);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(texture, x, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
