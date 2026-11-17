package app.pinya.zonelock.zone;

import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A protected area. Immutable snapshot, shared by the server index and the client mirror.
 *
 * @param anchor the position of the core whose zone it is
 * @param rules  bit mask of {@link Rule}s
 */
public record Zone(UUID id, BlockPos anchor, ZoneBox box, boolean active, String name, int rules,
        LockInfo lock) {

    public static final Codec<Zone> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(Zone::id),
            BlockPos.CODEC.fieldOf("anchor").forGetter(Zone::anchor),
            ZoneBox.CODEC.fieldOf("box").forGetter(Zone::box),
            Codec.BOOL.fieldOf("active").forGetter(Zone::active),
            Codec.STRING.optionalFieldOf("name", "").forGetter(Zone::name),
            Codec.INT.fieldOf("rules").forGetter(Zone::rules),
            LockInfo.CODEC.optionalFieldOf("lock", LockInfo.NONE).forGetter(Zone::lock))
            .apply(i, Zone::new));

    public static final StreamCodec<ByteBuf, Zone> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public Zone decode(ByteBuf buf) {
            UUID id = UUIDUtil.STREAM_CODEC.decode(buf);
            BlockPos anchor = BlockPos.STREAM_CODEC.decode(buf);
            ZoneBox box = ZoneBox.STREAM_CODEC.decode(buf);
            boolean active = buf.readBoolean();
            String name = ByteBufCodecs.stringUtf8(64).decode(buf);
            int rules = ByteBufCodecs.VAR_INT.decode(buf);
            LockInfo lock = LockInfo.STREAM_CODEC.decode(buf);
            return new Zone(id, anchor, box, active, name, rules, lock);
        }

        @Override
        public void encode(ByteBuf buf, Zone zone) {
            UUIDUtil.STREAM_CODEC.encode(buf, zone.id);
            BlockPos.STREAM_CODEC.encode(buf, zone.anchor);
            ZoneBox.STREAM_CODEC.encode(buf, zone.box);
            buf.writeBoolean(zone.active);
            ByteBufCodecs.stringUtf8(64).encode(buf, zone.name);
            ByteBufCodecs.VAR_INT.encode(buf, zone.rules);
            LockInfo.STREAM_CODEC.encode(buf, zone.lock);
        }
    };

    public boolean forbids(Rule rule) {
        return active && rule.in(rules);
    }

    public boolean contains(BlockPos pos) {
        return box.contains(pos);
    }

    public Zone withActive(boolean value) {
        return new Zone(id, anchor, box, value, name, rules, lock);
    }

    public Zone withLock(LockInfo value) {
        return new Zone(id, anchor, box, active, name, rules, value);
    }
}
