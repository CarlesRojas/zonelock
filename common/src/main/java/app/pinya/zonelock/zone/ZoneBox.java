package app.pinya.zonelock.zone;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Immutable inclusive block box. */
public record ZoneBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    public static final Codec<ZoneBox> CODEC = BoundingBox.CODEC.xmap(ZoneBox::of, ZoneBox::toBoundingBox);
    public static final StreamCodec<ByteBuf, ZoneBox> STREAM_CODEC = BoundingBox.STREAM_CODEC.map(ZoneBox::of,
            ZoneBox::toBoundingBox);

    public ZoneBox {
        if (minX > maxX || minY > maxY || minZ > maxZ)
            throw new IllegalArgumentException("Inverted zone box");
    }

    public static ZoneBox of(BoundingBox box) {
        return new ZoneBox(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
    }

    /** Box around {@code center} using per-direction extents. */
    public static ZoneBox around(BlockPos center, Extents e) {
        return new ZoneBox(center.getX() - e.west(), center.getY() - e.down(), center.getZ() - e.north(),
                center.getX() + e.east(), center.getY() + e.up(), center.getZ() + e.south());
    }

    public BoundingBox toBoundingBox() {
        return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public boolean contains(BlockPos pos) {
        return contains(pos.getX(), pos.getY(), pos.getZ());
    }

    public boolean contains(int x, int y, int z) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    /** True when the point lies inside the box volume (max faces are at max + 1). */
    public boolean contains(Vec3 point) {
        return point.x >= minX && point.x < maxX + 1 && point.y >= minY && point.y < maxY + 1 && point.z >= minZ
                && point.z < maxZ + 1;
    }

    /** Shares at least one block with {@code other}. Boxes side by side (touching) don't intersect. */
    public boolean intersects(ZoneBox other) {
        return minX <= other.maxX && maxX >= other.minX && minY <= other.maxY && maxY >= other.minY
                && minZ <= other.maxZ && maxZ >= other.minZ;
    }

    public AABB toAabb() {
        return new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
    }

    public ZoneBox inflate(int amount) {
        return new ZoneBox(minX - amount, minY - amount, minZ - amount, maxX + amount, maxY + amount, maxZ + amount);
    }

    public BlockPos center() {
        return new BlockPos(Math.floorDiv(minX + maxX, 2), Math.floorDiv(minY + maxY, 2), Math.floorDiv(minZ + maxZ, 2));
    }

    /** World coordinate of the given face plane. */
    public double face(Direction dir) {
        return switch (dir) {
            case DOWN -> minY;
            case UP -> maxY + 1;
            case NORTH -> minZ;
            case SOUTH -> maxZ + 1;
            case WEST -> minX;
            case EAST -> maxX + 1;
        };
    }

    /** Squared distance from a point to the closest point of the box (0 when inside). */
    public double distanceSqr(Vec3 p) {
        double dx = Math.max(Math.max(minX - p.x, 0), p.x - (maxX + 1));
        double dy = Math.max(Math.max(minY - p.y, 0), p.y - (maxY + 1));
        double dz = Math.max(Math.max(minZ - p.z, 0), p.z - (maxZ + 1));
        return dx * dx + dy * dy + dz * dz;
    }
}
