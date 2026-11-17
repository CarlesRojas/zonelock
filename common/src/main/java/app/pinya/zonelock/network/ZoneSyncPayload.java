package app.pinya.zonelock.network;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import app.pinya.zonelock.ZoneLock;
import app.pinya.zonelock.zone.Zone;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Zone changes for the dimension the player is in.
 *
 * @param replace when true the client drops every zone it knew before applying {@code zones}
 */
public record ZoneSyncPayload(boolean replace, List<Zone> zones, List<UUID> removed) implements CustomPacketPayload {
    public static final Type<ZoneSyncPayload> TYPE = new Type<>(ZoneLock.id("zone_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ZoneSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ZoneSyncPayload::replace,
            Zone.STREAM_CODEC.apply(ByteBufCodecs.collection(ArrayList::new)), ZoneSyncPayload::zones,
            UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.collection(ArrayList::new)), ZoneSyncPayload::removed,
            ZoneSyncPayload::new);

    @Override
    public Type<ZoneSyncPayload> type() {
        return TYPE;
    }
}
