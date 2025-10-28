package app.pinya.pinyazonelock.screen.custom;

import app.pinya.pinyazonelock.ZoneLock;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class TexturedCoreButton extends Button {
    private static final ResourceLocation PLUS_TEXTURE = ResourceLocation.fromNamespaceAndPath(ZoneLock.MOD_ID,
            "textures/gui/sprites/plus.png");
    private static final ResourceLocation PLUS_HIGHLIGHTED_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ZoneLock.MOD_ID,
            "textures/gui/sprites/plus_highlighted.png");
    private static final ResourceLocation PLUS_DISABLED_TEXTURE = ResourceLocation.fromNamespaceAndPath(ZoneLock.MOD_ID,
            "textures/gui/sprites/plus_disabled.png");

    private static final ResourceLocation MINUS_TEXTURE = ResourceLocation.fromNamespaceAndPath(ZoneLock.MOD_ID,
            "textures/gui/sprites/minus.png");
    private static final ResourceLocation MINUS_HIGHLIGHTED_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ZoneLock.MOD_ID,
            "textures/gui/sprites/minus_highlighted.png");
    private static final ResourceLocation MINUS_DISABLED_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ZoneLock.MOD_ID,
            "textures/gui/sprites/minus_disabled.png");

    private final ResourceLocation normalTexture;
    private final ResourceLocation highlightedTexture;
    private final ResourceLocation disabledTexture;
    public final int textureWidth;
    public final int textureHeight;

    public static TexturedCoreButton createPlusButton(int x, int y, OnPress onPress) {
        return new TexturedCoreButton(x, y, PLUS_TEXTURE, PLUS_HIGHLIGHTED_TEXTURE, PLUS_DISABLED_TEXTURE, onPress);
    }

    public static TexturedCoreButton createMinusButton(int x, int y, OnPress onPress) {
        return new TexturedCoreButton(x, y, MINUS_TEXTURE, MINUS_HIGHLIGHTED_TEXTURE, MINUS_DISABLED_TEXTURE, onPress);
    }

    private TexturedCoreButton(int x, int y, ResourceLocation normalTexture, ResourceLocation highlightedTexture,
            ResourceLocation disabledTexture, OnPress onPress) {
        super(Button.builder(Component.empty(), onPress)
                .pos(x, y)
                .size(11, 11));
        this.normalTexture = normalTexture;
        this.highlightedTexture = highlightedTexture;
        this.disabledTexture = disabledTexture;
        this.textureWidth = 11;
        this.textureHeight = 11;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation textureToUse;

        if (!this.active)
            textureToUse = disabledTexture;
        else if (this.isHovered)
            textureToUse = highlightedTexture;
        else
            textureToUse = normalTexture;

        guiGraphics.blit(textureToUse, getX(), getY(), 0, 0, width, height, textureWidth, textureHeight);
    }
}