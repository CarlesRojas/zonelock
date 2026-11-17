package app.pinya.zonelock.network;

import app.pinya.zonelock.ZoneLock;
import app.pinya.zonelock.block.CoreConfig;
import app.pinya.zonelock.zone.LockInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/**
 * A snapshot of a core for its screen: sent just before the screen opens (see
 * {@link app.pinya.zonelock.menu.CoreMenu}) and again whenever it changes while open.
 *
 * @param editor      the player may change the configuration
 * @param mapMaker    the player is a creative operator (can lock the configuration for others)
 * @param maxExtent   biggest extent allowed by the focus in the core
 */
public record CoreScreenPayload(BlockPos pos, CoreConfig config, boolean armed, boolean editor,
        boolean mapMaker, ItemStack focus, int maxExtent, LockInfo lock) implements CustomPacketPayload {

    public static final Type<CoreScreenPayload> TYPE = new Type<>(ZoneLock.id("core_screen"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CoreScreenPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, CoreScreenPayload::pos,
            CoreConfig.STREAM_CODEC, CoreScreenPayload::config,
            ByteBufCodecs.BOOL, CoreScreenPayload::armed,
            ByteBufCodecs.BOOL, CoreScreenPayload::editor,
            ByteBufCodecs.BOOL, CoreScreenPayload::mapMaker,
            ItemStack.OPTIONAL_STREAM_CODEC, CoreScreenPayload::focus,
            ByteBufCodecs.VAR_INT, CoreScreenPayload::maxExtent,
            LockInfo.STREAM_CODEC, CoreScreenPayload::lock,
            CoreScreenPayload::new);

    @Override
    public Type<CoreScreenPayload> type() {
        return TYPE;
    }
}
