package app.pinya.zonelock.client.screen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import org.jspecify.annotations.Nullable;

import app.pinya.zonelock.block.CoreConfig;
import app.pinya.zonelock.menu.CoreMenu;
import app.pinya.zonelock.network.CoreEditPayload;
import app.pinya.zonelock.network.CoreEditPayload.Op;
import app.pinya.zonelock.network.CoreScreenPayload;
import app.pinya.zonelock.platform.Services;
import app.pinya.zonelock.zone.Extents;
import app.pinya.zonelock.zone.Rule;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The core's screen, from vanilla's own pieces (the container panel, inventory slots, the beacon's dark box and its
 * buttons), everything on one margin:
 * <ul>
 * <li>The core's name across the top.</li>
 * <li>On the left, what switches the core off and the size of its zone; under them the slot for the focus block, in
 * line with the reach table on the right: every focus with how far it lets the zone reach, the one in the slot
 * marked.</li>
 * <li>The rules in one row, in a box like the beacon's: a light, pressed-in button when protected, a dark one when
 * not; hover for what each does.</li>
 * <li>The player's inventory, centred like vanilla's.</li>
 * </ul>
 * Only opened for players allowed to edit the core (anyone while it is off, map makers always). Every change goes to the
 * server, which answers with a fresh snapshot, so the screen always shows the real state.
 */
public class CoreScreen extends AbstractContainerScreen<CoreMenu> {
    private static final Identifier SLOT = Identifier.withDefaultNamespace("container/slot");
    private static final Identifier BEACON_OFF = Identifier.withDefaultNamespace("container/beacon/button_disabled");
    private static final Identifier BEACON_ON = Identifier.withDefaultNamespace("container/beacon/button_selected");
    private static final Identifier BEACON_HOVER = Identifier.withDefaultNamespace("container/beacon/button_highlighted");
    private static final Identifier BUTTON = Identifier.withDefaultNamespace("widget/button");
    private static final Identifier BUTTON_HIGHLIGHTED = Identifier.withDefaultNamespace("widget/button_highlighted");
    /** Vanilla's label grey, and the zone's amethyst for what matters. */
    private static final int LABEL = 0xFF404040, AMETHYST = 0xFF7A3FC2;
    /** Everything starts on this margin. */
    private static final int M = 8;
    private static final int W = CoreMenu.WIDTH;
    private static final int TABLE_TITLE_Y = 40, TIER_W = 24, RULES_Y = 82, RULES_H = 30;
    /** The focus tiers: none first (no block), then a block of each, and how far each lets the zone reach. */
    private static final List<ItemStack> TIER_ICONS = List.of(ItemStack.EMPTY, new ItemStack(Items.IRON_BLOCK),
            new ItemStack(Items.GOLD_BLOCK), new ItemStack(Items.EMERALD_BLOCK), new ItemStack(Items.DIAMOND_BLOCK),
            new ItemStack(Items.NETHERITE_BLOCK));
    private static final int[] TIER_REACH = { 4, 8, 16, 32, 64, 128 };
    /** What each rule protects, as an item. */
    private static final Map<Rule, ItemStack> ICONS = new HashMap<>();

    static {
        ICONS.put(Rule.BREAK, new ItemStack(Items.IRON_PICKAXE));
        ICONS.put(Rule.PLACE, new ItemStack(Items.BRICKS));
        ICONS.put(Rule.EXPLOSIONS, new ItemStack(Items.TNT));
        ICONS.put(Rule.PISTONS, new ItemStack(Items.PISTON));
        ICONS.put(Rule.FLUIDS_AND_FIRE, new ItemStack(Items.LAVA_BUCKET));
        ICONS.put(Rule.MOB_GRIEFING, new ItemStack(Items.CREEPER_HEAD));
        ICONS.put(Rule.TELEPORT, new ItemStack(Items.ENDER_PEARL));
        ICONS.put(Rule.FLIGHT, new ItemStack(Items.ELYTRA));
        ICONS.put(Rule.DECORATIONS, new ItemStack(Items.ITEM_FRAME));
        ICONS.put(Rule.BLOCK_TOOLS, new ItemStack(Items.IRON_HOE));
    }

    /** The latest snapshot of each core, sent just before its screen opens and whenever it changes. */
    private static final Map<BlockPos, CoreScreenPayload> LATEST = new HashMap<>();

    private @Nullable CoreScreenPayload view;
    private @Nullable EditBox nameBox;

    public CoreScreen(CoreMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, CoreMenu.WIDTH, CoreMenu.HEIGHT);
        view = LATEST.get(menu.pos());
        inventoryLabelX = CoreMenu.INVENTORY_X - 1;
        inventoryLabelY = CoreMenu.INVENTORY_Y - 11;
    }

    /** A snapshot arrived: kept for when the screen opens, and shown at once if it is open. */
    public static void receive(CoreScreenPayload view) {
        LATEST.put(view.pos(), view);
        if (Minecraft.getInstance().gui.screen() instanceof CoreScreen screen && screen.pos().equals(view.pos()))
            screen.update(view);
    }

    public BlockPos pos() {
        return menu.pos();
    }

    public void update(CoreScreenPayload fresh) {
        String typed = nameBox != null ? nameBox.getValue() : null;
        boolean focused = nameBox != null && nameBox.isFocused();
        this.view = fresh;
        rebuildWidgets();
        if (focused && typed != null && nameBox != null) {
            nameBox.setValue(typed);
            setFocused(nameBox);
        }
    }

    /** Index of the focus tier the core is at, from the reach it allows. */
    private int tier() {
        if (view == null)
            return 0;
        for (int i = TIER_REACH.length - 1; i >= 0; i--)
            if (view.maxExtent() >= TIER_REACH[i])
                return i;
        return 0;
    }

    /** Left edge of the reach table: its columns end on the right margin. */
    private static int tableX() {
        return W - M - TIER_ICONS.size() * TIER_W;
    }

    @Override
    protected void init() {
        super.init();
        if (view == null)
            return;
        CoreConfig config = view.config();
        // The name on the margin; map makers get two small buttons at its end: switch off, and the map lock.
        int nameWidth = W - 2 * M - (view.mapMaker() ? 40 : 0);
        nameBox = new EditBox(font, leftPos + M, topPos + 7, nameWidth, 16, Component.translatable("gui.zonelock.name"));
        nameBox.setMaxLength(CoreConfig.MAX_NAME);
        nameBox.setValue(config.name());
        nameBox.setHint(Component.translatable("gui.zonelock.name_hint").withStyle(ChatFormatting.DARK_GRAY));
        addRenderableWidget(nameBox);
        if (view.mapMaker()) {
            if (view.armed())
                addRenderableWidget(new IconButton(leftPos + W - M - 38, topPos + 6, 18,
                        new ItemStack(Items.AMETHYST_SHARD), 0, () -> send(Op.DISARM),
                        Component.translatable("gui.zonelock.disarm")));
            addRenderableWidget(new IconButton(leftPos + W - M - 18, topPos + 6, 18, new ItemStack(Items.IRON_BARS), 0,
                    () -> send(Op.TOGGLE_CONFIG_LOCK),
                    Component.translatable(config.configLocked() ? "gui.zonelock.maplock.on" : "gui.zonelock.maplock.off")
                            .append("\n").append(Component.translatable("gui.zonelock.maplock.tooltip")
                                    .withStyle(ChatFormatting.GRAY))));
        }

        // The rules, one row of beacon buttons inside the beacon's box.
        Rule[] rules = Rule.values();
        int x0 = leftPos + (W - rules.length * 22) / 2;
        for (int i = 0; i < rules.length; i++) {
            Rule rule = rules[i];
            boolean on = rule.in(config.rules());
            Component tip = Component.translatable(rule.translationKey()).withStyle(ChatFormatting.WHITE)
                    .append("\n").append(Component.translatable(rule.translationKey() + ".tooltip")
                            .withStyle(ChatFormatting.GRAY))
                    .append("\n").append(Component.translatable(on ? "gui.zonelock.rule.on" : "gui.zonelock.rule.off")
                            .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED));
            addRenderableWidget(new RuleButton(x0 + i * 22, topPos + RULES_Y + 4, ICONS.get(rule), () -> on,
                    () -> send(Op.TOGGLE_RULE, rule.ordinal()), tip));
        }
    }

    private Component how(CoreConfig config) {
        return config.redstone() ? Component.translatable("gui.zonelock.lock.redstone.desc")
                : view != null && view.lock().sealsTotal() > 0
                        ? Component.translatable("gui.zonelock.lock.seals.desc", view.lock().sealsTotal())
                        : Component.translatable("gui.zonelock.lock.basic");
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int left = leftPos, top = topPos;
        panel(g, left, top, imageWidth, imageHeight);
        for (Slot slot : menu.slots)
            g.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT, left + slot.x - 1, top + slot.y - 1, 18, 18);
        beaconBox(g, left + M, top + RULES_Y, W - 2 * M, RULES_H);
        if (view == null)
            return;
        CoreConfig config = view.config();

        // What switches it off, then the size of the zone.
        g.text(font, how(config), left + M, top + 29, LABEL, false);
        Extents e = config.extents();
        g.text(font, Component.translatable("gui.zonelock.size", e.east() + e.west() + 1, e.up() + e.down() + 1,
                e.north() + e.south() + 1), left + M, top + 41, AMETHYST, false);

        // The reach table: a title, then a column per focus (none first), the one in the slot marked; its icons in
        // line with the focus slot.
        int tx = left + tableX(), iconY = top + CoreMenu.FOCUS_Y;
        g.text(font, Component.translatable("gui.zonelock.reach_title"), tx + 2, top + TABLE_TITLE_Y, LABEL, false);
        int current = tier();
        for (int i = 0; i < TIER_ICONS.size(); i++) {
            int cx = tx + i * TIER_W;
            if (i == current) {
                g.fill(cx + 1, iconY - 2, cx + TIER_W - 1, iconY + 28, 0xFFFFFFFF);
                g.fill(cx + 2, iconY - 1, cx + TIER_W - 2, iconY + 27, 0xFFE7DCF7);
            }
            ItemStack icon = TIER_ICONS.get(i);
            if (icon.isEmpty())
                g.text(font, Component.literal("—"), cx + (TIER_W - font.width("—")) / 2, iconY + 4, LABEL, false);
            else
                g.item(icon, cx + (TIER_W - 16) / 2, iconY);
            Component reach = Component.literal(String.valueOf(TIER_REACH[i]));
            g.text(font, reach, cx + (TIER_W - font.width(reach)) / 2 + 1, iconY + 18, i == current ? AMETHYST : LABEL,
                    false);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        // What each column of the table means.
        int tx = leftPos + tableX(), iconY = topPos + CoreMenu.FOCUS_Y;
        for (int i = 0; i < TIER_ICONS.size(); i++) {
            int cx = tx + i * TIER_W;
            if (mouseX >= cx && mouseX < cx + TIER_W && mouseY >= iconY - 2 && mouseY < iconY + 28)
                g.setTooltipForNextFrame(Component.translatable("gui.zonelock.tier." + i, TIER_REACH[i]), mouseX, mouseY);
        }
    }

    /** Vanilla's container panel: light grey, a black rim with cut corners, lit top-left edge, shaded bottom-right. */
    private static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x + 1, y, x + w - 1, y + h, 0xFF000000);
        g.fill(x, y + 1, x + w, y + h - 1, 0xFF000000);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFC6C6C6);
        g.fill(x + 1, y + 1, x + w - 3, y + 3, 0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + 3, y + h - 3, 0xFFFFFFFF);
        g.fill(x + 3, y + h - 3, x + w - 1, y + h - 1, 0xFF555555);
        g.fill(x + w - 3, y + 3, x + w - 1, y + h - 1, 0xFF555555);
    }

    /**
     * The beacon's dark box, as its texture draws it: a black outline with rounded corners, a two-pixel grey border
     * (its inner corners rounded too), and the near-black inside.
     */
    private static void beaconBox(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        roundRect(g, x, y, w, h, 2, 0xFF000000);
        roundRect(g, x + 1, y + 1, w - 2, h - 2, 1, 0xFF555555);
        roundRect(g, x + 3, y + 3, w - 6, h - 6, 1, 0xFF212121);
    }

    /** A filled rectangle with its corners cut in {@code r} one-pixel steps (r = 1 or 2). */
    private static void roundRect(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int color) {
        g.fill(x + r, y, x + w - r, y + h, color);
        g.fill(x, y + r, x + w, y + h - r, color);
        if (r == 2)
            g.fill(x + 1, y + 1, x + w - 1, y + h - 1, color);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LABEL, false);
    }

    /** A beacon button with an item on it: light and pressed in when on, dark when off, bluish under the mouse. */
    private static final class RuleButton extends AbstractButton {
        private final ItemStack icon;
        private final BooleanSupplier on;
        private final Runnable action;

        RuleButton(int x, int y, ItemStack icon, BooleanSupplier on, Runnable action, Component tooltip) {
            super(x, y, 22, 22, Component.empty());
            this.icon = icon;
            this.on = on;
            this.action = action;
            setTooltip(Tooltip.create(tooltip));
        }

        @Override
        public void onPress(InputWithModifiers input) {
            action.run();
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
            Identifier sprite = on.getAsBoolean() ? BEACON_ON : isHoveredOrFocused() ? BEACON_HOVER : BEACON_OFF;
            g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, getX(), getY(), width, height);
            g.item(icon, getX() + 3, getY() + 3);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    /** A small vanilla button showing an item (or, with no item, a swatch of {@code swatch}). */
    private static final class IconButton extends AbstractButton {
        private final ItemStack icon;
        private final int swatch;
        private final Runnable action;

        IconButton(int x, int y, int size, ItemStack icon, int swatch, Runnable action, Component tooltip) {
            super(x, y, size, size, Component.empty());
            this.icon = icon;
            this.swatch = swatch;
            this.action = action;
            setTooltip(Tooltip.create(tooltip));
        }

        @Override
        public void onPress(InputWithModifiers input) {
            action.run();
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
            g.blitSprite(RenderPipelines.GUI_TEXTURED, isHoveredOrFocused() ? BUTTON_HIGHLIGHTED : BUTTON, getX(), getY(),
                    width, height);
            if (!icon.isEmpty())
                g.item(icon, getX() + (width - 16) / 2, getY() + (height - 16) / 2);
            else
                g.fill(getX() + 2, getY() + 2, getX() + width - 2, getY() + height - 2, swatch);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    // ------------------------------------------------------------------ actions

    private void send(Op op) {
        commitName();
        Services.NETWORK.sendToServer(CoreEditPayload.of(menu.pos(), op));
    }

    private void send(Op op, int a) {
        commitName();
        Services.NETWORK.sendToServer(CoreEditPayload.of(menu.pos(), op, a));
    }

    private void commitName() {
        if (nameBox != null && view != null && !nameBox.getValue().strip().equals(view.config().name()))
            Services.NETWORK.sendToServer(new CoreEditPayload(menu.pos(), Op.SET_NAME, 0, 0, nameBox.getValue()));
    }

    /** Like an anvil: while typing a name, keys go to the name (the inventory key too); Escape still closes. */
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (nameBox != null && nameBox.isFocused() && event.key() != 256) {
            if (event.isConfirmation()) {
                commitName();
                nameBox.setFocused(false);
                return true;
            }
            if (nameBox.keyPressed(event) || nameBox.canConsumeInput())
                return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void removed() {
        commitName();
        super.removed();
    }
}
