package app.pinya.pinyazonelock.screen.custom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.blaze3d.systems.RenderSystem;

import app.pinya.pinyazonelock.ZoneLock;
import app.pinya.pinyazonelock.block.custom.Core;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class CoreScreen extends AbstractContainerScreen<CoreMenu> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Core.class);
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

    private static final int BUTTON_HOLD_DELAY = 500; // Initial delay before rapid increment (in milliseconds)
    private static final int BUTTON_REPEAT_INTERVAL = 50; // Interval between increments while holding (in milliseconds)

    private long buttonHoldStartTime = 0;
    private long lastAutoPressTime = 0;
    private Button heldButton = null;

    private int HALF_BUTTON = 5;
    private int TEXT_DISPL = 2;
    private int LABEL_DISPL_TOP = 7;
    private int LABEL_DISPL_BOT = 10;
    private int SEPARATION_X = 17;
    private int DISPL_X = 56;
    private int TOP_Y = 23;
    private int MID_TOP_Y = 43;
    private int MID_BOT_Y = 78;
    private int BOT_Y = 98;

    private int ITEMS_Y = 127;
    private int ITEMS_X = 13;
    private int ITEMS_SEPARATION = 22;

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
        int maxSide = menu.blockEntity.getMaxSideBlocks();
        upPlusButton.active = currentValue < maxSide;
        upMinusButton.active = currentValue > CoreMenu.MIN_SIDE && maxSide > 0;
    }

    private void updateDownButtonState() {
        int currentValue = menu.blockEntity.getDownBlocks();
        int maxSide = menu.blockEntity.getMaxSideBlocks();
        downPlusButton.active = currentValue < maxSide;
        downMinusButton.active = currentValue > CoreMenu.MIN_SIDE && maxSide > 0;
    }

    private void updateNorthButtonState() {
        int currentValue = menu.blockEntity.getNorthBlocks();
        int maxSide = menu.blockEntity.getMaxSideBlocks();
        northPlusButton.active = currentValue < maxSide;
        northMinusButton.active = currentValue > CoreMenu.MIN_SIDE && maxSide > 0;
    }

    private void updateSouthButtonState() {
        int currentValue = menu.blockEntity.getSouthBlocks();
        int maxSide = menu.blockEntity.getMaxSideBlocks();
        southPlusButton.active = currentValue < maxSide;
        southMinusButton.active = currentValue > CoreMenu.MIN_SIDE && maxSide > 0;
    }

    private void updateEastButtonState() {
        int currentValue = menu.blockEntity.getEastBlocks();
        int maxSide = menu.blockEntity.getMaxSideBlocks();
        eastPlusButton.active = currentValue < maxSide;
        eastMinusButton.active = currentValue > CoreMenu.MIN_SIDE && maxSide > 0;
    }

    private void updateWestButtonState() {
        int currentValue = menu.blockEntity.getWestBlocks();
        int maxSide = menu.blockEntity.getMaxSideBlocks();
        westPlusButton.active = currentValue < maxSide;
        westMinusButton.active = currentValue > CoreMenu.MIN_SIDE && maxSide > 0;
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
        int upTextX = getStringWidth(upBlocksText, x, 0);
        guiGraphics.drawString(font, upBlocksText, upTextX, y + TOP_Y + TEXT_DISPL, 0x404040, false);

        // DOWN
        String downBlocksText = Integer.toString(menu.blockEntity.getDownBlocks());
        int downTextX = getStringWidth(downBlocksText, x, 0);
        guiGraphics.drawString(font, downBlocksText, downTextX, y + BOT_Y + TEXT_DISPL, 0x404040, false);

        // NORTH
        String northBlocksText = Integer.toString(menu.blockEntity.getNorthBlocks());
        int northTextX = getStringWidth(northBlocksText, x, DISPL_X);
        guiGraphics.drawString(font, northBlocksText, northTextX, y + MID_TOP_Y + TEXT_DISPL, 0x404040, false);

        // SOUTH
        String southBlocksText = Integer.toString(menu.blockEntity.getSouthBlocks());
        int southTextX = getStringWidth(southBlocksText, x, -DISPL_X);
        guiGraphics.drawString(font, southBlocksText, southTextX, y + MID_BOT_Y + TEXT_DISPL, 0x404040, false);

        // EAST
        String eastBlocksText = Integer.toString(menu.blockEntity.getEastBlocks());
        int eastTextX = getStringWidth(eastBlocksText, x, DISPL_X);
        guiGraphics.drawString(font, eastBlocksText, eastTextX, y + MID_BOT_Y + TEXT_DISPL, 0x404040, false);

        // WEST
        String westBlocksText = Integer.toString(menu.blockEntity.getWestBlocks());
        int westTextX = getStringWidth(westBlocksText, x, -DISPL_X);
        guiGraphics.drawString(font, westBlocksText, westTextX, y + MID_TOP_Y + TEXT_DISPL, 0x404040, false);

        float scale = 0.5f;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scale, scale, 1.0f);

        MutableComponent upLabelText = Component.translatable("gui.pinyazonelock.up").withStyle(ChatFormatting.BOLD);
        int upLabelX = getStringWidth(upLabelText, x, 0);
        guiGraphics.drawString(font, upLabelText,
                Math.round(upLabelX / scale),
                Math.round((y + TOP_Y + TEXT_DISPL - LABEL_DISPL_TOP) / scale), 0x8d6acc, false);

        MutableComponent downLabelText = Component.translatable("gui.pinyazonelock.down")
                .withStyle(ChatFormatting.BOLD);
        int downLabelX = getStringWidth(downLabelText, x, 0);
        guiGraphics.drawString(font, downLabelText,
                Math.round(downLabelX / scale),
                Math.round((y + BOT_Y + TEXT_DISPL + LABEL_DISPL_BOT) / scale), 0x8d6acc, false);

        MutableComponent northLabelText = Component.translatable("gui.pinyazonelock.north")
                .withStyle(ChatFormatting.BOLD);
        int northLabelX = getStringWidth(northLabelText, x, DISPL_X);
        guiGraphics.drawString(font, northLabelText,
                Math.round(northLabelX / scale),
                Math.round((y + MID_TOP_Y + TEXT_DISPL - LABEL_DISPL_TOP) / scale), 0x8d6acc, false);

        MutableComponent southLabelText = Component.translatable("gui.pinyazonelock.south")
                .withStyle(ChatFormatting.BOLD);
        int southLabelX = getStringWidth(southLabelText, x, -DISPL_X);
        guiGraphics.drawString(font, southLabelText,
                Math.round(southLabelX / scale),
                Math.round((y + MID_BOT_Y + TEXT_DISPL + LABEL_DISPL_BOT) / scale), 0x8d6acc, false);

        MutableComponent eastLabelText = Component.translatable("gui.pinyazonelock.east")
                .withStyle(ChatFormatting.BOLD);
        int eastLabelX = getStringWidth(eastLabelText, x, DISPL_X);
        guiGraphics.drawString(font, eastLabelText,
                Math.round(eastLabelX / scale),
                Math.round((y + MID_BOT_Y + TEXT_DISPL + LABEL_DISPL_BOT) / scale), 0x8d6acc, false);

        MutableComponent westLabelText = Component.translatable("gui.pinyazonelock.west")
                .withStyle(ChatFormatting.BOLD);
        int westLabelX = getStringWidth(westLabelText, x, -DISPL_X);
        guiGraphics.drawString(font, westLabelText,
                Math.round(westLabelX / scale),
                Math.round((y + MID_TOP_Y + TEXT_DISPL - LABEL_DISPL_TOP) / scale), 0x8d6acc, false);

        guiGraphics.pose().popPose();

        // Render Items
        ItemStack ironBlock = new ItemStack(Items.IRON_BLOCK);
        guiGraphics.renderItem(ironBlock, x + ITEMS_X, y + ITEMS_Y);
        ItemStack goldBlock = new ItemStack(Items.GOLD_BLOCK);
        guiGraphics.renderItem(goldBlock, x + ITEMS_X + ITEMS_SEPARATION, y + ITEMS_Y);
        ItemStack emeraldBlock = new ItemStack(Items.EMERALD_BLOCK);
        guiGraphics.renderItem(emeraldBlock, x + ITEMS_X + ITEMS_SEPARATION * 2, y + ITEMS_Y);
        ItemStack lapisBlock = new ItemStack(Items.LAPIS_BLOCK);
        guiGraphics.renderItem(lapisBlock, x + ITEMS_X + ITEMS_SEPARATION * 3, y + ITEMS_Y);
        ItemStack diamondBlock = new ItemStack(Items.DIAMOND_BLOCK);
        guiGraphics.renderItem(diamondBlock, x + ITEMS_X + ITEMS_SEPARATION * 4, y + ITEMS_Y);

    }

    private int getStringWidth(String text, int x, int mod) {
        int textWidth = font.width(text);
        return (int) (x + Math.ceil(imageWidth / 2.0) - Math.floor(textWidth / 2.0)) + 1 + mod;
    }

    private int getStringWidth(Component text, int x, int mod) {
        int textWidth = font.width(text);
        return (int) (x + Math.ceil(imageWidth / 2.0) - Math.floor(textWidth / 4.0)) + 1 + mod;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        updateUpButtonState();
        updateDownButtonState();
        updateNorthButtonState();
        updateSouthButtonState();
        updateEastButtonState();
        updateWestButtonState();

        if (heldButton != null) {
            long currentTime = System.currentTimeMillis();
            long holdDuration = currentTime - buttonHoldStartTime;
            long timeSinceLastPress = currentTime - lastAutoPressTime;

            if (holdDuration >= BUTTON_HOLD_DELAY && timeSinceLastPress >= BUTTON_REPEAT_INTERVAL)
                handleButtonPress(heldButton);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Button clickedButton = getClickedButton(mouseX, mouseY);
        if (clickedButton != null) {
            heldButton = clickedButton;
            buttonHoldStartTime = System.currentTimeMillis();
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        heldButton = null;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private Button getClickedButton(double mouseX, double mouseY) {
        if (upPlusButton.isMouseOver(mouseX, mouseY))
            return upPlusButton;
        if (upMinusButton.isMouseOver(mouseX, mouseY))
            return upMinusButton;
        if (downPlusButton.isMouseOver(mouseX, mouseY))
            return downPlusButton;
        if (downMinusButton.isMouseOver(mouseX, mouseY))
            return downMinusButton;
        if (northPlusButton.isMouseOver(mouseX, mouseY))
            return northPlusButton;
        if (northMinusButton.isMouseOver(mouseX, mouseY))
            return northMinusButton;
        if (southPlusButton.isMouseOver(mouseX, mouseY))
            return southPlusButton;
        if (southMinusButton.isMouseOver(mouseX, mouseY))
            return southMinusButton;
        if (eastPlusButton.isMouseOver(mouseX, mouseY))
            return eastPlusButton;
        if (eastMinusButton.isMouseOver(mouseX, mouseY))
            return eastMinusButton;
        if (westPlusButton.isMouseOver(mouseX, mouseY))
            return westPlusButton;
        if (westMinusButton.isMouseOver(mouseX, mouseY))
            return westMinusButton;
        return null;
    }

    private void handleButtonPress(Button button) {
        lastAutoPressTime = System.currentTimeMillis();
        if (!button.active)
            return;

        if (button == upPlusButton)
            menu.incrementUpBlocks();
        else if (button == upMinusButton)
            menu.decrementUpBlocks();
        else if (button == downPlusButton)
            menu.incrementDownBlocks();
        else if (button == downMinusButton)
            menu.decrementDownBlocks();
        else if (button == northPlusButton)
            menu.incrementNorthBlocks();
        else if (button == northMinusButton)
            menu.decrementNorthBlocks();
        else if (button == southPlusButton)
            menu.incrementSouthBlocks();
        else if (button == southMinusButton)
            menu.decrementSouthBlocks();
        else if (button == eastPlusButton)
            menu.incrementEastBlocks();
        else if (button == eastMinusButton)
            menu.decrementEastBlocks();
        else if (button == westPlusButton)
            menu.incrementWestBlocks();
        else if (button == westMinusButton)
            menu.decrementWestBlocks();
    }
}
