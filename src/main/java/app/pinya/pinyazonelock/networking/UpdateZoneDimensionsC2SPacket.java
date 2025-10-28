package app.pinya.pinyazonelock.networking;

import java.util.function.Supplier;

import app.pinya.pinyazonelock.block.entity.custom.CoreEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.network.CustomPayloadEvent.Context;

public class UpdateZoneDimensionsC2SPacket {
    private final BlockPos pos;
    private final int up, down, north, south, east, west;

    public UpdateZoneDimensionsC2SPacket(BlockPos pos, int up, int down, int north, int south, int east, int west) {
        this.pos = pos;
        this.up = up;
        this.down = down;
        this.north = north;
        this.south = south;
        this.east = east;
        this.west = west;
    }

    public UpdateZoneDimensionsC2SPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.up = buf.readInt();
        this.down = buf.readInt();
        this.north = buf.readInt();
        this.south = buf.readInt();
        this.east = buf.readInt();
        this.west = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(up);
        buf.writeInt(down);
        buf.writeInt(north);
        buf.writeInt(south);
        buf.writeInt(east);
        buf.writeInt(west);
    }

    public void handle(Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerLevel level = context.getSender().serverLevel();
            BlockEntity blockEntity = level.getBlockEntity(pos);

            if (blockEntity instanceof CoreEntity core) {
                core.setZoneDimensionsServer(up, down, north, south, east, west);
            }
        });
    }
}