package app.pinya.pinyazonelock.screen.custom;

import com.mojang.blaze3d.systems.RenderSystem;

import app.pinya.pinyazonelock.ZoneLock;
import app.pinya.pinyazonelock.block.custom.Core;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
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

    private Button upPlusButton;
    private Button upMinusButton;
    private Button downPlusButton;
    private Button downMinusButton;
    private Button northPlusButton;
    private Button northMinusButton;
    private Button southPlusButton;
    private Button southMinusButton;
    private Button eastPlusButton;
    private Button eastMinusButton;
    private Button westPlusButton;
    private Button westMinusButton;

    private int HALF_BUTTON = 5;
    private int TEXT_DISPL = 2;
    private int SEPARATION_X = 18;
    private int DISPL_X = 56;
    private int TOP_Y = 23;
    private int MID_TOP_Y = 43;
    private int MID_BOT_Y = 78;
    private int BOT_Y = 98;

    public CoreScreen(CoreMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 247;
        this.imageWidth = 176;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        int middle = (int) (x + Math.ceil(imageWidth / 2.0));

        // UP
        this.upPlusButton = TexturedCoreButton.createPlusButton(middle + SEPARATION_X - HALF_BUTTON, y + TOP_Y,
                button -> {
                    menu.incrementUpBlocks();
                    updateUpButtonState();
                });
        addRenderableWidget(upPlusButton);

        this.upMinusButton = TexturedCoreButton.createMinusButton(middle - SEPARATION_X - HALF_BUTTON, y + TOP_Y,
                button -> {
                    menu.decrementUpBlocks();
                    updateUpButtonState();
                });
        addRenderableWidget(upMinusButton);

        // DOWN
        this.downPlusButton = TexturedCoreButton.createPlusButton(middle + SEPARATION_X - HALF_BUTTON, y + BOT_Y,
                button -> {
                    menu.incrementDownBlocks();
                    updateDownButtonState();
                });
        addRenderableWidget(downPlusButton);

        this.downMinusButton = TexturedCoreButton.createMinusButton(middle - SEPARATION_X - HALF_BUTTON, y + BOT_Y,
                button -> {
                    menu.decrementDownBlocks();
                    updateDownButtonState();
                });
        addRenderableWidget(downMinusButton);

        // NORTH
        this.northPlusButton = TexturedCoreButton.createPlusButton(middle + SEPARATION_X - HALF_BUTTON + DISPL_X,
                y + MID_TOP_Y,
                button -> {
                    menu.incrementNorthBlocks();
                    updateNorthButtonState();
                });
        addRenderableWidget(northPlusButton);

        this.northMinusButton = TexturedCoreButton.createMinusButton(middle - SEPARATION_X - HALF_BUTTON + DISPL_X,
                y + MID_TOP_Y,
                button -> {
                    menu.decrementNorthBlocks();
                    updateNorthButtonState();
                });
        addRenderableWidget(northMinusButton);

        // SOUTH
        this.southPlusButton = TexturedCoreButton.createPlusButton(middle + SEPARATION_X - HALF_BUTTON - DISPL_X,
                y + MID_BOT_Y,
                button -> {
                    menu.incrementSouthBlocks();
                    updateSouthButtonState();
                });
        addRenderableWidget(southPlusButton);

        this.southMinusButton = TexturedCoreButton.createMinusButton(middle - SEPARATION_X - HALF_BUTTON - DISPL_X,
                y + MID_BOT_Y,
                button -> {
                    menu.decrementSouthBlocks();
                    updateSouthButtonState();
                });
        addRenderableWidget(southMinusButton);

        // EAST
        this.eastPlusButton = TexturedCoreButton.createPlusButton(middle + SEPARATION_X - HALF_BUTTON + DISPL_X,
                y + MID_BOT_Y,
                button -> {
                    menu.incrementEastBlocks();
                    updateEastButtonState();
                });
        addRenderableWidget(eastPlusButton);

        this.eastMinusButton = TexturedCoreButton.createMinusButton(middle - SEPARATION_X - HALF_BUTTON + DISPL_X,
                y + MID_BOT_Y,
                button -> {
                    menu.decrementEastBlocks();
                    updateEastButtonState();
                });
        addRenderableWidget(eastMinusButton);

        // WEST
        this.westPlusButton = TexturedCoreButton.createPlusButton(middle + SEPARATION_X - HALF_BUTTON - DISPL_X,
                y + MID_TOP_Y,
                button -> {
                    menu.incrementWestBlocks();
                    updateWestButtonState();
                });
        addRenderableWidget(westPlusButton);

        this.westMinusButton = TexturedCoreButton.createMinusButton(middle - SEPARATION_X - HALF_BUTTON - DISPL_X,
                y + MID_TOP_Y,
                button -> {
                    menu.decrementWestBlocks();
                    updateWestButtonState();
                });
        addRenderableWidget(westMinusButton);

        updateUpButtonState();
        updateDownButtonState();
        updateNorthButtonState();
        updateSouthButtonState();
        updateEastButtonState();
        updateWestButtonState();
    }

    private void updateUpButtonState() {
        int currentValue = menu.blockEntity.getUpBlocks();
        upPlusButton.active = currentValue < CoreMenu.MAX_SIDE;
        upMinusButton.active = currentValue > CoreMenu.MIN_SIDE;
    }

    private void updateDownButtonState() {
        int currentValue = menu.blockEntity.getDownBlocks();
        downPlusButton.active = currentValue < CoreMenu.MAX_SIDE;
        downMinusButton.active = currentValue > CoreMenu.MIN_SIDE;
    }

    private void updateNorthButtonState() {
        int currentValue = menu.blockEntity.getNorthBlocks();
        northPlusButton.active = currentValue < CoreMenu.MAX_SIDE;
        northMinusButton.active = currentValue > CoreMenu.MIN_SIDE;
    }

    private void updateSouthButtonState() {
        int currentValue = menu.blockEntity.getSouthBlocks();
        southPlusButton.active = currentValue < CoreMenu.MAX_SIDE;
        southMinusButton.active = currentValue > CoreMenu.MIN_SIDE;
    }

    private void updateEastButtonState() {
        int currentValue = menu.blockEntity.getEastBlocks();
        eastPlusButton.active = currentValue < CoreMenu.MAX_SIDE;
        eastMinusButton.active = currentValue > CoreMenu.MIN_SIDE;
    }

    private void updateWestButtonState() {
        int currentValue = menu.blockEntity.getWestBlocks();
        westPlusButton.active = currentValue < CoreMenu.MAX_SIDE;
        westMinusButton.active = currentValue > CoreMenu.MIN_SIDE;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        boolean isActive = menu.blockEntity.getBlockState().getValue(Core.ACTIVE);

        ResourceLocation texture = isActive ? ACTIVE_TEXTURE : TEXTURE;
        RenderSystem.setShaderTexture(0, texture);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(texture, x, y, 0, 0, imageWidth, imageHeight);

        // UP
        String upBlocksText = Integer.toString(menu.blockEntity.getUpBlocks());
        int upTextWidth = font.width(upBlocksText);
        int upTextX = (int) (x + Math.ceil(imageWidth / 2.0) - Math.floor(upTextWidth / 2.0)) + 1;
        guiGraphics.drawString(font, upBlocksText, upTextX, y + TOP_Y + TEXT_DISPL, 0x404040, false);

        // DOWN
        String downBlocksText = Integer.toString(menu.blockEntity.getDownBlocks());
        int downTextWidth = font.width(downBlocksText);
        int downTextX = (int) (x + Math.ceil(imageWidth / 2.0) - Math.floor(downTextWidth / 2.0)) + 1;
        guiGraphics.drawString(font, downBlocksText, downTextX, y + BOT_Y + TEXT_DISPL, 0x404040, false);

        // NORTH
        String northBlocksText = Integer.toString(menu.blockEntity.getNorthBlocks());
        int northTextWidth = font.width(northBlocksText);
        int northTextX = (int) (x + Math.ceil(imageWidth / 2.0) - Math.floor(northTextWidth / 2.0)) + 1 + DISPL_X;
        guiGraphics.drawString(font, northBlocksText, northTextX, y + MID_TOP_Y + TEXT_DISPL, 0x404040, false);

        // SOUTH
        String southBlocksText = Integer.toString(menu.blockEntity.getSouthBlocks());
        int southTextWidth = font.width(southBlocksText);
        int southTextX = (int) (x + Math.ceil(imageWidth / 2.0) - Math.floor(southTextWidth / 2.0)) + 1 - DISPL_X;
        guiGraphics.drawString(font, southBlocksText, southTextX, y + MID_BOT_Y + TEXT_DISPL, 0x404040, false);

        // EAST
        String eastBlocksText = Integer.toString(menu.blockEntity.getEastBlocks());
        int eastTextWidth = font.width(eastBlocksText);
        int eastTextX = (int) (x + Math.ceil(imageWidth / 2.0) - Math.floor(eastTextWidth / 2.0)) + 1 + DISPL_X;
        guiGraphics.drawString(font, eastBlocksText, eastTextX, y + MID_BOT_Y + TEXT_DISPL, 0x404040, false);

        // WEST
        String westBlocksText = Integer.toString(menu.blockEntity.getWestBlocks());
        int westTextWidth = font.width(westBlocksText);
        int westTextX = (int) (x + Math.ceil(imageWidth / 2.0) - Math.floor(westTextWidth / 2.0)) + 1 - DISPL_X;
        guiGraphics.drawString(font, westBlocksText, westTextX, y + MID_TOP_Y + TEXT_DISPL, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
