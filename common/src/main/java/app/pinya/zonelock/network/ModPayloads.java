package app.pinya.zonelock.network;

import java.util.List;
import java.util.function.BiConsumer;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** Every payload of the mod. Loaders register these; handlers live in common code. */
public final class ModPayloads {

    public record ToClient<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type,
            StreamCodec<RegistryFriendlyByteBuf, T> codec) {
    }

    public record ToServer<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type,
            StreamCodec<RegistryFriendlyByteBuf, T> codec, BiConsumer<T, ServerPlayer> handler) {
    }

    /** Handled on the client by {@code app.pinya.zonelock.client.ClientPayloadHandler}. */
    public static final List<ToClient<?>> TO_CLIENT = List.of(
            new ToClient<>(ZoneSyncPayload.TYPE, ZoneSyncPayload.STREAM_CODEC),
            new ToClient<>(SealPingPayload.TYPE, SealPingPayload.STREAM_CODEC),
            new ToClient<>(BarrierHitPayload.TYPE, BarrierHitPayload.STREAM_CODEC),
            new ToClient<>(CoreScreenPayload.TYPE, CoreScreenPayload.STREAM_CODEC));

    public static final List<ToServer<?>> TO_SERVER = List.of(
            new ToServer<>(CoreEditPayload.TYPE, CoreEditPayload.STREAM_CODEC, ServerPayloadHandler::handleCoreEdit));

    private ModPayloads() {
    }
}
