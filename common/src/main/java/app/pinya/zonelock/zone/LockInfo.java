package app.pinya.zonelock.zone;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What stands between a player and switching a zone off. Shared with clients so the HUD can explain what to do.
 *
 * @param seals      every seal inside the zone must be extinguished first
 * @param redstone   the core only switches off when it receives a redstone signal
 * @param sealsTotal seals found inside the zone
 * @param sealsLit   seals still burning
 */
public record LockInfo(boolean seals, boolean redstone, int sealsTotal, int sealsLit) {

    public static final LockInfo NONE = new LockInfo(false, false, 0, 0);

    public static final Codec<LockInfo> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("seals", false).forGetter(LockInfo::seals),
            Codec.BOOL.optionalFieldOf("redstone", false).forGetter(LockInfo::redstone),
            Codec.INT.optionalFieldOf("seals_total", 0).forGetter(LockInfo::sealsTotal),
            Codec.INT.optionalFieldOf("seals_lit", 0).forGetter(LockInfo::sealsLit))
            .apply(i, LockInfo::new));

    public static final StreamCodec<ByteBuf, LockInfo> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public LockInfo decode(ByteBuf buf) {
            byte flags = buf.readByte();
            return new LockInfo((flags & 1) != 0, (flags & 2) != 0, ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf));
        }

        @Override
        public void encode(ByteBuf buf, LockInfo lock) {
            buf.writeByte((lock.seals ? 1 : 0) | (lock.redstone ? 2 : 0));
            ByteBufCodecs.VAR_INT.encode(buf, lock.sealsTotal);
            ByteBufCodecs.VAR_INT.encode(buf, lock.sealsLit);
        }
    };

    public LockInfo withSeals(int total, int lit) {
        return new LockInfo(seals, redstone, total, lit);
    }

    public boolean sealsDone() {
        return !seals || sealsLit == 0;
    }
}
