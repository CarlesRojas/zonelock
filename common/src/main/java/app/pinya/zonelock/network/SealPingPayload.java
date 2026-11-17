package app.pinya.zonelock.network;

import java.util.ArrayList;
import java.util.List;

import app.pinya.zonelock.ZoneLock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Shows the player which seals still keep a core locked: the client draws trails from the core to each one. */
public record SealPingPayload(BlockPos core, List<BlockPos> seals) implements CustomPacketPayload {
    public static final Type<SealPingPayload> TYPE = new Type<>(ZoneLock.id("seal_ping"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SealPingPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SealPingPayload::core,
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.collection(ArrayList::new)), SealPingPayload::seals,
            SealPingPayload::new);

    @Override
    public Type<SealPingPayload> type() {
        return TYPE;
    }
}
