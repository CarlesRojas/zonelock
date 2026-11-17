package app.pinya.zonelock.command;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import app.pinya.zonelock.block.CoreBlockEntity;
import app.pinya.zonelock.zone.Zone;
import app.pinya.zonelock.zone.ZoneManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;

/**
 * {@code /zonelock list | here | unlock | lock}: admin tools for servers and map makers (game master level).
 */
public final class ZoneLockCommand {
    private ZoneLockCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("zonelock")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("list").executes(ZoneLockCommand::list))
                .then(Commands.literal("here").executes(ZoneLockCommand::here))
                .then(Commands.literal("unlock").executes(ctx -> setActive(ctx, false)))
                .then(Commands.literal("lock").executes(ctx -> setActive(ctx, true))));
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        List<Zone> zones = ZoneManager.get(level).index().all().stream()
                .sorted(Comparator.comparingDouble(z -> z.box().distanceSqr(source.getPosition())))
                .toList();
        if (zones.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.zonelock.none"), false);
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("command.zonelock.count", zones.size()), false);
        zones.stream().limit(20).forEach(zone -> source.sendSuccess(() -> describe(zone), false));
        return zones.size();
    }

    private static int here(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        List<Zone> zones = ZoneManager.get(source.getLevel()).index().at(BlockPos.containing(source.getPosition()));
        if (zones.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.zonelock.none_here"), false);
            return 0;
        }
        zones.forEach(zone -> source.sendSuccess(() -> describe(zone), false));
        return zones.size();
    }

    private static int setActive(CommandContext<CommandSourceStack> ctx, boolean active) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        ZoneManager manager = ZoneManager.get(level);
        Optional<Zone> target = manager.index().at(BlockPos.containing(source.getPosition())).stream().findFirst();
        if (target.isEmpty()) {
            source.sendFailure(Component.translatable("command.zonelock.none_here"));
            return 0;
        }
        Zone zone = target.get();
        if (level.getBlockEntity(zone.anchor()) instanceof CoreBlockEntity core) {
            if (active)
                core.arm();
            else
                core.disarm();
        } else {
            manager.setActive(zone.id(), active);
        }
        source.sendSuccess(() -> Component.translatable(active ? "command.zonelock.locked" : "command.zonelock.unlocked",
                displayName(zone)), true);
        return 1;
    }

    private static MutableComponent describe(Zone zone) {
        BlockPos a = zone.anchor();
        return Component.literal(zone.active() ? "● " : "○ ")
                .withStyle(zone.active() ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY)
                .append(displayName(zone).copy().withStyle(ChatFormatting.WHITE))
                .append(Component.literal("  " + a.getX() + " " + a.getY() + " " + a.getZ())
                        .withStyle(ChatFormatting.DARK_GRAY));
    }

    private static Component displayName(Zone zone) {
        return zone.name().isBlank() ? Component.translatable("block.zonelock.amethyst_core")
                : Component.literal(zone.name());
    }
}
