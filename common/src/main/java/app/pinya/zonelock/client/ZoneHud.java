package app.pinya.zonelock.client;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import app.pinya.zonelock.zone.ClientZoneCache;
import app.pinya.zonelock.zone.LockInfo;
import app.pinya.zonelock.zone.Zone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

/**
 * The quest card of a sealed zone (its name and what is left to do), in the top-left corner while the player is inside
 * the zone: on the HUD during play, and in the inventory screen too. That corner is free of toasts (top right), boss
 * bars (top middle) and status effects; in the inventory the card shrinks to one line, or moves to the bottom-left
 * corner, rather than touch the panel, its creative tabs or the recipe book.
 */
public final class ZoneHud {
    private static @Nullable Zone current;

    private ZoneHud() {
    }

    public static void onZonesChanged() {
        if (current == null)
            return;
        Optional<Zone> fresh = ClientZoneCache.INDEX.get(current.id());
        current = fresh.orElse(null);
    }

    public static void tick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            current = null;
            return;
        }
        Zone found = null;
        List<Zone> here = ClientZoneCache.INDEX.at(player.blockPosition());
        for (Zone zone : here) {
            if (!zone.active())
                continue;
            found = zone;
            break;
        }
        if (found == null) {
            current = null;
            return;
        }
        current = found;
    }

    private static final int MARGIN = 4;
    /** Padding on every side; the accent stripe on the left; text heights (a glyph, the smaller title). */
    private static final int PAD = 4, ACCENT = 2, GLYPH = 7, LINE_GAP = 2, BAR = 3, BAR_GAP = 3;
    /** The progress bar: one rounded segment per step, lit (done) or dim (still to do). */
    private static final int SEGMENT_ON = 0xFFB57CFF, SEGMENT_ON_SHINE = 0xFFDDC2FF, SEGMENT_OFF = 0xFF54466A;

    /** On the HUD while playing (no screen open, HUD shown): the full card in the top-left corner. */
    public static void renderOnHud(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() != null || mc.gui.hud.isHidden())
            return;
        renderInInventory(g, g.guiWidth(), 0, 0);
    }

    /**
     * Called at the end of the inventory screens' render (see InventoryScreenMixin). {@code busyLeft/Top/Bottom}: the
     * area taken by the panel, its tabs and the recipe book.
     */
    public static void renderInInventory(GuiGraphicsExtractor g, int busyLeft, int busyTop, int busyBottom) {
        Zone zone = current;
        Minecraft mc = Minecraft.getInstance();
        if (zone == null)
            return;
        Font font = mc.font;
        Component title = title(zone);
        Component goal = goal(zone);
        int accent = accent(zone);
        // The same padding on every side; the title at three quarters size above the goal; the bar under it.
        // The title one step smaller than the GUI scale, so each of its pixels is still a whole number of screen pixels.
        double guiScale = mc.getWindow().getGuiScale();
        float titleScale = guiScale > 1 ? (float) ((guiScale - 1) / guiScale) : 1.0F;
        int titleHeight = Math.round(GLYPH * titleScale);
        int titleWidth = (int) Math.ceil(font.width(title) * titleScale);
        int[] steps = steps(zone);
        int bar = steps != null ? BAR_GAP + BAR : 0;
        int full = PAD + titleHeight + LINE_GAP + GLYPH + bar + PAD;
        int fullWidth = ACCENT + PAD + Math.max(titleWidth, font.width(goal)) + PAD;
        int small = PAD + GLYPH + bar + PAD;
        int smallWidth = ACCENT + PAD + font.width(goal) + PAD;
        int x = MARGIN;
        int y = MARGIN;
        boolean compact;
        if (fits(fullWidth, full, busyLeft, busyTop))
            compact = false;
        else if (fits(smallWidth, small, busyLeft, busyTop))
            compact = true;
        else {
            // No room at the top: the bottom-left corner, under the panel.
            compact = true;
            y = g.guiHeight() - small - MARGIN;
            if (y < busyBottom + 2 && x + smallWidth >= busyLeft - 2)
                y = MARGIN; // nowhere is free: the top corner overlaps least
        }
        int width = compact ? smallWidth : fullWidth;
        int height = compact ? small : full;

        g.fill(x, y, x + width, y + height, ARGB.color(200, 0x18111F));
        g.fill(x, y, x + ACCENT, y + height, ARGB.color(255, accent));
        g.outline(x, y, width, height, ARGB.color(90, accent));
        int textX = x + ACCENT + PAD;
        int goalY = compact ? y + PAD : y + PAD + titleHeight + LINE_GAP;
        if (!compact) {
            g.pose().pushMatrix();
            g.pose().translate(textX, y + PAD);
            g.pose().scale(titleScale, titleScale);
            g.text(font, title, 0, 0, ARGB.color(255, 0xF3EAFF));
            g.pose().popMatrix();
        }
        g.text(font, goal, textX, goalY, ARGB.color(255, compact ? 0xE6D6FF : 0xC9A8FF));
        if (steps != null)
            drawSteps(g, textX, goalY + GLYPH + BAR_GAP, width - ACCENT - 2 * PAD, steps[0], steps[1]);
    }

    /**
     * The progress bar in segments, one per step ({@code total}), each a {@link #BAR}-pixel pill with rounded ends: the
     * first {@code done} lit, the rest dim.
     */
    private static void drawSteps(GuiGraphicsExtractor g, int x, int y, int width, int done, int total) {
        int gap = total > 12 ? 1 : 2;
        int segment = Math.max(2, (width - gap * (total - 1)) / total);
        int used = segment * total + gap * (total - 1);
        int left = x + Math.max(0, (width - used) / 2);
        for (int i = 0; i < total; i++) {
            int sx = left + i * (segment + gap);
            boolean on = i < done;
            int color = on ? SEGMENT_ON : SEGMENT_OFF;
            g.fill(sx + 1, y, sx + segment - 1, y + BAR, color);
            g.fill(sx, y + 1, sx + segment, y + BAR - 1, color);
            if (on && segment > 2)
                g.fill(sx + 1, y, sx + segment - 1, y + 1, SEGMENT_ON_SHINE);
        }
    }

    /** The card in the top-left corner leaves the busy area alone: beside it or above it. */
    private static boolean fits(int width, int height, int busyLeft, int busyTop) {
        return MARGIN + width <= busyLeft - 2 || MARGIN + height <= busyTop - 2;
    }

    private static Component title(Zone zone) {
        return zone.name().isBlank() ? Component.translatable("hud.zonelock.sealed") : Component.literal(zone.name());
    }

    private static Component goal(Zone zone) {
        LockInfo lock = zone.lock();
        if (lock.seals() && lock.sealsLit() > 0)
            return Component.translatable("hud.zonelock.goal.seals", lock.sealsTotal() - lock.sealsLit(), lock.sealsTotal());
        if (lock.redstone())
            return Component.translatable("hud.zonelock.goal.redstone");
        return Component.translatable("hud.zonelock.goal.core");
    }

    /** Steps done and steps in all ({done, total}) of what is left to do, or null when there is nothing to count. */
    private static int @Nullable [] steps(Zone zone) {
        LockInfo lock = zone.lock();
        if (lock.seals() && lock.sealsLit() > 0 && lock.sealsTotal() > 0)
            return new int[] { lock.sealsTotal() - lock.sealsLit(), lock.sealsTotal() };
        return null;
    }

    private static int accent(Zone zone) {
        return 0xB57CFF;
    }
}
