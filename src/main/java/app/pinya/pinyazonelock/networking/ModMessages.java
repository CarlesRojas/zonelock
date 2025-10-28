package app.pinya.pinyazonelock.networking;

import app.pinya.pinyazonelock.ZoneLock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public class ModMessages {
    private static final ResourceLocation CHANNEL_ID = ResourceLocation.tryParse(ZoneLock.MOD_ID + ":messages");
    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(CHANNEL_ID)
            .networkProtocolVersion(1)
            .optionalServer()
            .clientAcceptedVersions((status, version) -> true)
            .serverAcceptedVersions((status, version) -> true)
            .simpleChannel();

    private static int packetId = 0;

    public static void register() {
        CHANNEL.messageBuilder(ZoneDataSyncS2CPacket.class, packetId++)
                .encoder((packet, buffer) -> packet.toBytes(buffer))
                .decoder(ZoneDataSyncS2CPacket::new)
                .consumerMainThread((packet, context) -> {
                    packet.handle(() -> context);
                    context.setPacketHandled(true);
                })
                .add();

        CHANNEL.messageBuilder(UpdateZoneDimensionsC2SPacket.class, packetId++)
                .encoder((packet, buffer) -> packet.toBytes(buffer))
                .decoder(UpdateZoneDimensionsC2SPacket::new)
                .consumerMainThread((packet, context) -> {
                    packet.handle(() -> context);
                    context.setPacketHandled(true);
                })
                .add();
    }

    public static void sendToPlayer(ZoneDataSyncS2CPacket packet, ServerPlayer player) {
        CHANNEL.send(packet, PacketDistributor.PLAYER.with(player));
    }

    public static void sendToAllPlayers(ZoneDataSyncS2CPacket packet) {
        CHANNEL.send(packet, PacketDistributor.ALL.noArg());
    }
}