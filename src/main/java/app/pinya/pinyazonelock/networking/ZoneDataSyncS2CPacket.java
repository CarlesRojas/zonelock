package app.pinya.pinyazonelock.networking;

import java.util.function.Supplier;

import app.pinya.pinyazonelock.world.ClientLockedZones;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent.Context;
import net.minecraftforge.fml.DistExecutor;

public class ZoneDataSyncS2CPacket {
    private final CompoundTag zoneData;

    public ZoneDataSyncS2CPacket(CompoundTag zoneData) {
        this.zoneData = zoneData;
    }

    public ZoneDataSyncS2CPacket(FriendlyByteBuf buf) {
        this.zoneData = buf.readNbt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(zoneData);
    }

    public void handle(Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientLockedZones.getInstance().updateFromPacket(zoneData));
        });
    }
}