package app.pinya.zonelock.client.screen;

import org.jspecify.annotations.Nullable;

import app.pinya.zonelock.client.ZoneLockConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.CommonComponents;

/** The mod's settings (Mod Menu on Fabric, the mods list on NeoForge), in the style of vanilla's option screens. */
public class ZoneLockConfigScreen extends Screen {
    private final @Nullable Screen parent;

    public ZoneLockConfigScreen(@Nullable Screen parent) {
        super(Component.translatable("options.zonelock.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(CycleButton.onOffBuilder(ZoneLockConfig.nearbyWalls)
                .create(width / 2 - 155, height / 6 + 24, 310, 20, Component.translatable("options.zonelock.nearby_walls"),
                        (button, value) -> {
                            ZoneLockConfig.nearbyWalls = value;
                            ZoneLockConfig.save();
                        }))
                .setTooltip(Tooltip.create(Component.translatable("options.zonelock.nearby_walls.tooltip")));
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(width / 2 - 100, height / 6 + 168, 200, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        g.centeredText(font, title, width / 2, 15, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}
