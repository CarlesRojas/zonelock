package app.pinya.zonelock.network;

import app.pinya.zonelock.ZoneLock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Something the player tried was blocked by a zone: the client flashes the seal at that spot and explains why.
 *
 * @param reason index of {@link app.pinya.zonelock.zone.Rule}
 */
public record BarrierHitPayload(BlockPos pos, int reason) implements CustomPacketPayload {
    public static final Type<BarrierHitPayload> TYPE = new Type<>(ZoneLock.id("barrier_hit"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BarrierHitPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, BarrierHitPayload::pos,
            ByteBufCodecs.VAR_INT, BarrierHitPayload::reason,
            BarrierHitPayload::new);

    @Override
    public Type<BarrierHitPayload> type() {
        return TYPE;
    }
}
