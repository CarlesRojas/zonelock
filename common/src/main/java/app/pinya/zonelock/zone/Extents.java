package app.pinya.zonelock.zone;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

/** How far a core's zone reaches in each direction, in blocks, not counting the core itself. */
public record Extents(int up, int down, int north, int south, int east, int west) {
    public static final Extents DEFAULT = new Extents(5, 3, 5, 5, 5, 5);

    public static final Codec<Extents> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("up").forGetter(Extents::up),
            Codec.INT.fieldOf("down").forGetter(Extents::down),
            Codec.INT.fieldOf("north").forGetter(Extents::north),
            Codec.INT.fieldOf("south").forGetter(Extents::south),
            Codec.INT.fieldOf("east").forGetter(Extents::east),
            Codec.INT.fieldOf("west").forGetter(Extents::west)).apply(i, Extents::new));

    public static final StreamCodec<ByteBuf, Extents> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Extents::up,
            ByteBufCodecs.VAR_INT, Extents::down,
            ByteBufCodecs.VAR_INT, Extents::north,
            ByteBufCodecs.VAR_INT, Extents::south,
            ByteBufCodecs.VAR_INT, Extents::east,
            ByteBufCodecs.VAR_INT, Extents::west,
            Extents::new);

    public int get(Direction dir) {
        return switch (dir) {
            case UP -> up;
            case DOWN -> down;
            case NORTH -> north;
            case SOUTH -> south;
            case EAST -> east;
            case WEST -> west;
        };
    }

    public Extents with(Direction dir, int value) {
        return switch (dir) {
            case UP -> new Extents(value, down, north, south, east, west);
            case DOWN -> new Extents(up, value, north, south, east, west);
            case NORTH -> new Extents(up, down, value, south, east, west);
            case SOUTH -> new Extents(up, down, north, value, east, west);
            case EAST -> new Extents(up, down, north, south, value, west);
            case WEST -> new Extents(up, down, north, south, east, value);
        };
    }

    public Extents clamp(int max) {
        return new Extents(c(up, max), c(down, max), c(north, max), c(south, max), c(east, max), c(west, max));
    }

    private static int c(int v, int max) {
        return Math.max(0, Math.min(max, v));
    }

    public int largest() {
        return Math.max(Math.max(Math.max(up, down), Math.max(north, south)), Math.max(east, west));
    }

    /** Extents after the structure containing the core was rotated. */
    public Extents rotate(Rotation rotation) {
        return switch (rotation) {
            case NONE -> this;
            case CLOCKWISE_90 -> new Extents(up, down, west, east, north, south);
            case CLOCKWISE_180 -> new Extents(up, down, south, north, west, east);
            case COUNTERCLOCKWISE_90 -> new Extents(up, down, east, west, south, north);
        };
    }

    /** Extents after the structure containing the core was mirrored. */
    public Extents mirror(Mirror mirror) {
        return switch (mirror) {
            case NONE -> this;
            case LEFT_RIGHT -> new Extents(up, down, south, north, east, west);
            case FRONT_BACK -> new Extents(up, down, north, south, west, east);
        };
    }
}
