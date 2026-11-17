package app.pinya.zonelock.network;

import app.pinya.zonelock.ZoneLock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * A change requested from the core screen or the wand. The server re-checks permissions and limits.
 *
 * @param a first argument (direction ordinal, rule ordinal, colour...)
 * @param b second argument (new extent, delta...)
 */
public record CoreEditPayload(BlockPos pos, Op op, int a, int b, String text) implements CustomPacketPayload {
    public enum Op {
        /** a = direction, b = new extent */
        SET_EXTENT,
        /** a = rule ordinal */
        TOGGLE_RULE,
        /** text = name */
        SET_NAME,
        DISARM,
        TOGGLE_CONFIG_LOCK
    }

    public static final Type<CoreEditPayload> TYPE = new Type<>(ZoneLock.id("core_edit"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CoreEditPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, CoreEditPayload::pos,
            ByteBufCodecs.VAR_INT.map(i -> Op.values()[Math.floorMod(i, Op.values().length)], Op::ordinal),
            CoreEditPayload::op,
            ByteBufCodecs.VAR_INT, CoreEditPayload::a,
            ByteBufCodecs.VAR_INT, CoreEditPayload::b,
            ByteBufCodecs.stringUtf8(128), CoreEditPayload::text,
            CoreEditPayload::new);

    public static CoreEditPayload of(BlockPos pos, Op op) {
        return new CoreEditPayload(pos, op, 0, 0, "");
    }

    public static CoreEditPayload of(BlockPos pos, Op op, int a) {
        return new CoreEditPayload(pos, op, a, 0, "");
    }

    @Override
    public Type<CoreEditPayload> type() {
        return TYPE;
    }
}
