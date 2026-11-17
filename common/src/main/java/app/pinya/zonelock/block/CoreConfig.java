package app.pinya.zonelock.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import app.pinya.zonelock.zone.Extents;
import app.pinya.zonelock.zone.Rule;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Everything a core is set up with. Survives switching the core off, so it can be re-armed as it was.
 *
 * @param name         shown to players entering the zone
 * @param extents      size of the zone around the core
 * @param rules        bit mask of {@link Rule}s
 * @param seals        all seals inside the zone must be put out before the core can be switched off
 * @param redstone     the core switches off when powered (instead of by hand)
 * @param configLocked map-maker lock: nobody but creative operators can change anything
 */
public record CoreConfig(String name, Extents extents, int rules, boolean seals, boolean redstone, boolean configLocked) {

    public static final int MAX_NAME = 32;

    public static final CoreConfig DEFAULT = new CoreConfig("", Extents.DEFAULT, Rule.all(), false, false, false);

    public static final Codec<CoreConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("name", "").forGetter(CoreConfig::name),
            Extents.CODEC.optionalFieldOf("extents", Extents.DEFAULT).forGetter(CoreConfig::extents),
            Codec.INT.optionalFieldOf("rules", Rule.all()).forGetter(CoreConfig::rules),
            Codec.BOOL.optionalFieldOf("seals", false).forGetter(CoreConfig::seals),
            Codec.BOOL.optionalFieldOf("redstone", false).forGetter(CoreConfig::redstone),
            Codec.BOOL.optionalFieldOf("config_locked", false).forGetter(CoreConfig::configLocked))
            .apply(i, CoreConfig::new));

    public static final StreamCodec<ByteBuf, CoreConfig> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CoreConfig decode(ByteBuf buf) {
            String name = ByteBufCodecs.stringUtf8(MAX_NAME * 4).decode(buf);
            Extents extents = Extents.STREAM_CODEC.decode(buf);
            int rules = ByteBufCodecs.VAR_INT.decode(buf);
            byte flags = buf.readByte();
            return new CoreConfig(name, extents, rules, (flags & 1) != 0, (flags & 2) != 0, (flags & 4) != 0);
        }

        @Override
        public void encode(ByteBuf buf, CoreConfig c) {
            ByteBufCodecs.stringUtf8(MAX_NAME * 4).encode(buf, c.name);
            Extents.STREAM_CODEC.encode(buf, c.extents);
            ByteBufCodecs.VAR_INT.encode(buf, c.rules);
            buf.writeByte((c.seals ? 1 : 0) | (c.redstone ? 2 : 0) | (c.configLocked ? 4 : 0));
        }
    };

    public CoreConfig withName(String value) {
        return new CoreConfig(value, extents, rules, seals, redstone, configLocked);
    }

    public CoreConfig withExtents(Extents value) {
        return new CoreConfig(name, value, rules, seals, redstone, configLocked);
    }

    public CoreConfig withRules(int value) {
        return new CoreConfig(name, extents, value, seals, redstone, configLocked);
    }

    /**
     * Seals and redstone are set by the block, never chosen. Every core honours the seals of its zone: a Redstone
     * Core (made from an Amethyst Core) needs its seals put out first, then its signal.
     */
    public CoreConfig withLock(CoreBlock.Lock lock) {
        return new CoreConfig(name, extents, rules, true, lock == CoreBlock.Lock.REDSTONE, configLocked);
    }

    public CoreConfig withConfigLocked(boolean value) {
        return new CoreConfig(name, extents, rules, seals, redstone, value);
    }
}
