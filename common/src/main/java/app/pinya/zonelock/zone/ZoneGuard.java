package app.pinya.zonelock.zone;

import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import app.pinya.zonelock.network.BarrierHitPayload;
import app.pinya.zonelock.platform.Services;
import app.pinya.zonelock.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** The single place every protection hook asks. Works on both sides (the client uses its synced mirror). */
public final class ZoneGuard {
    /** Entity currently being ticked on the server thread, so block changes can be attributed to mobs. */
    public static final ThreadLocal<Entity> TICKING = new ThreadLocal<>();

    private ZoneGuard() {
    }

    public static ZoneIndex index(Level level) {
        if (level instanceof ServerLevel serverLevel)
            return ZoneManager.get(serverLevel).index();
        return level.isClientSide() ? ClientZoneCache.INDEX : EMPTY;
    }

    private static final ZoneIndex EMPTY = new ZoneIndex();

    /** Creative players build freely; everyone else plays by the zone's rules. */
    public static boolean bypass(@Nullable Entity entity) {
        return entity instanceof Player player && (player.isCreative() || player.isSpectator());
    }

    public static boolean forbids(Level level, BlockPos pos, Rule rule) {
        ZoneIndex index = index(level);
        return !index.isEmpty() && index.forbids(pos, rule);
    }

    public static Optional<Zone> forbidding(Level level, BlockPos pos, Rule rule) {
        ZoneIndex index = index(level);
        return index.isEmpty() ? Optional.empty() : index.forbidding(pos, rule);
    }

    /** True when {@code a} and {@code b} are not inside the same zone that forbids the rule. */
    public static boolean crosses(Level level, BlockPos a, BlockPos b, Rule rule) {
        ZoneIndex index = index(level);
        if (index.isEmpty())
            return false;
        Optional<UUID> za = index.forbidding(a, rule).map(Zone::id);
        Optional<UUID> zb = index.forbidding(b, rule).map(Zone::id);
        return !za.equals(zb);
    }

    /** Whether an entity may change something at {@code pos}; gives feedback to players when not. */
    public static boolean denies(@Nullable Entity actor, Level level, BlockPos pos, Rule rule) {
        if (bypass(actor))
            return false;
        Optional<Zone> zone = forbidding(level, pos, rule);
        if (zone.isEmpty())
            return false;
        if (actor instanceof Player player)
            feedback(player, pos, rule);
        return true;
    }

    public static boolean deniesBreak(Player player, Level level, BlockPos pos) {
        if (bypass(player) || !forbids(level, pos, Rule.BREAK))
            return false;
        BlockState state = level.getBlockState(pos);
        if (state.is(ModTags.BREAKABLE_IN_ZONES))
            return false;
        feedback(player, pos, Rule.BREAK);
        return true;
    }

    public static boolean deniesPlace(@Nullable Player player, Level level, BlockPos pos, @Nullable BlockState placed) {
        if (bypass(player) || !forbids(level, pos, Rule.PLACE))
            return false;
        if (placed != null && placed.is(ModTags.PLACEABLE_IN_ZONES))
            return false;
        if (player != null)
            feedback(player, pos, Rule.PLACE);
        return true;
    }

    /** Tells the player why nothing happened (client: flash + message; server: sends a payload). */
    public static void feedback(Player player, BlockPos pos, Rule rule) {
        if (player instanceof ServerPlayer serverPlayer)
            Services.NETWORK.sendToPlayer(serverPlayer, new BarrierHitPayload(pos.immutable(), rule.ordinal()));
        else if (player.level().isClientSide())
            ClientFeedback.HANDLER.accept(pos.immutable(), rule);
    }

    /** Client hook, set by the client initializer. */
    public static final class ClientFeedback {
        public static java.util.function.BiConsumer<BlockPos, Rule> HANDLER = (pos, rule) -> {
        };

        private ClientFeedback() {
        }
    }
}
