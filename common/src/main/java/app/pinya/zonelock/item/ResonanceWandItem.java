package app.pinya.zonelock.item;

import java.util.Optional;
import java.util.function.Consumer;

import app.pinya.zonelock.block.CoreBlockEntity;
import app.pinya.zonelock.registry.ModSounds;
import app.pinya.zonelock.zone.ClientZoneCache;
import app.pinya.zonelock.zone.WandTarget;
import app.pinya.zonelock.zone.Zone;
import app.pinya.zonelock.zone.ZoneManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Shapes zones in the world. Holding it shows the zones around; the wall of a switched-off zone it points at lights up.
 * <ul>
 * <li>Use: that wall moves a block away from you.</li>
 * <li>Sneak + use: it moves a block towards you.</li>
 * </ul>
 * So from inside a zone use grows it, from outside it shrinks it. Holding use keeps the wall moving. A wall stops at the
 * core, at what the core's focus allows, and against another zone (zones never cross). Using it on a block does nothing
 * else.
 */
public class ResonanceWandItem extends Item {
    public ResonanceWandItem(Properties properties) {
        super(properties);
    }

    /** The wall the player points at, from the client's copy of the zones (what is lit up). */
    public static Optional<WandTarget> clientTarget(Player player, double blockDistance) {
        return WandTarget.pick(ClientZoneCache.INDEX.near(player.getEyePosition(), WandTarget.REACH + 1),
                player.getEyePosition(), player.getViewVector(1.0F), blockDistance,
                zone -> !zone.active() || player.isCreative());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null)
            return InteractionResult.PASS;
        // A wall in front of the block clicked moves; the block itself does nothing.
        double block = context.getClickLocation().distanceTo(player.getEyePosition());
        return pushWall(context.getLevel(), player, block);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return pushWall(level, player, blockDistance(player));
    }

    /** How far the player's view runs before meeting a block: walls behind a block can't be pushed. */
    public static double blockDistance(Player player) {
        var hit = player.pick(WandTarget.REACH, 1.0F, false);
        return hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                ? hit.getLocation().distanceTo(player.getEyePosition()) : Double.MAX_VALUE;
    }

    private static InteractionResult pushWall(Level level, Player player, double blockDistance) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer))
            return clientTarget(player, blockDistance).isPresent() ? InteractionResult.SUCCESS : InteractionResult.PASS;
        Vec3 eye = player.getEyePosition();
        Optional<WandTarget> target = WandTarget.pick(ZoneManager.get(serverLevel).index().near(eye, WandTarget.REACH + 1),
                eye, player.getViewVector(1.0F), blockDistance, zone -> serverLevel.isLoaded(zone.anchor())
                        && serverLevel.getBlockEntity(zone.anchor()) instanceof CoreBlockEntity core
                        && core.canEdit(player));
        if (target.isEmpty())
            return InteractionResult.PASS;
        CoreBlockEntity core = (CoreBlockEntity) serverLevel.getBlockEntity(target.get().zone().anchor());
        int step = target.get().planeStep(eye, player.isShiftKeyDown());
        CoreBlockEntity.Push result = core.push(target.get().face(), step);
        int outward = step * target.get().face().getAxisDirection().getStep();
        switch (result) {
            case MOVED -> serverLevel.playSound(null, player.blockPosition(), ModSounds.WAND_TUNE.get(), SoundSource.PLAYERS,
                    0.6F, outward > 0 ? 1.35F : 0.85F);
            case LIMIT -> {
                serverLevel.playSound(null, player.blockPosition(), ModSounds.CORE_DENY.get(), SoundSource.PLAYERS, 0.5F,
                        1.2F);
                if (outward > 0)
                    serverPlayer.sendOverlayMessage(Component.translatable("message.zonelock.too_big", core.maxExtent()));
            }
            case BLOCKED -> {
                serverLevel.playSound(null, player.blockPosition(), ModSounds.CORE_DENY.get(), SoundSource.PLAYERS, 0.5F,
                        1.2F);
                serverPlayer.sendOverlayMessage(Component.translatable("message.zonelock.zone_in_the_way"));
            }
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> builder, TooltipFlag flag) {
        for (int i = 1; i <= 3; i++)
            builder.accept(Component.translatable("tooltip.zonelock.wand.help." + i).withStyle(ChatFormatting.GRAY));
    }
}
