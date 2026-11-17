package app.pinya.zonelock.zone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

/**
 * Zones of one dimension, indexed by chunk column so a position lookup only looks at the few zones that can contain
 * it. Not thread safe: use from the owning thread (server thread or client thread).
 */
public final class ZoneIndex {
    private static final List<Zone> NONE = Collections.emptyList();

    private final Map<UUID, Zone> zones = new HashMap<>();
    private final Long2ObjectOpenHashMap<List<Zone>> byChunk = new Long2ObjectOpenHashMap<>();

    public Collection<Zone> all() {
        return Collections.unmodifiableCollection(zones.values());
    }

    public Optional<Zone> get(UUID id) {
        return Optional.ofNullable(zones.get(id));
    }

    public boolean isEmpty() {
        return zones.isEmpty();
    }

    public void clear() {
        zones.clear();
        byChunk.clear();
    }

    public void put(Zone zone) {
        Zone old = zones.put(zone.id(), zone);
        if (old != null)
            unindex(old);
        index(zone);
    }

    public Optional<Zone> remove(UUID id) {
        Zone old = zones.remove(id);
        if (old != null)
            unindex(old);
        return Optional.ofNullable(old);
    }

    /** Zones whose box contains the position, active or not. */
    public List<Zone> at(BlockPos pos) {
        return at(pos.getX(), pos.getY(), pos.getZ());
    }

    public List<Zone> at(int x, int y, int z) {
        List<Zone> candidates = byChunk.get(ChunkPos.pack(SectionPos.blockToSectionCoord(x),
                SectionPos.blockToSectionCoord(z)));
        if (candidates == null)
            return NONE;
        List<Zone> result = null;
        for (Zone zone : candidates) {
            if (zone.box().contains(x, y, z)) {
                if (result == null)
                    result = new ArrayList<>(2);
                result.add(zone);
            }
        }
        return result == null ? NONE : result;
    }

    public List<Zone> at(Vec3 point) {
        return at(BlockPos.containing(point));
    }

    /** First active zone at the position that forbids the rule. */
    public Optional<Zone> forbidding(BlockPos pos, Rule rule) {
        List<Zone> candidates = byChunk.get(ChunkPos.pack(pos));
        if (candidates == null)
            return Optional.empty();
        for (Zone zone : candidates)
            if (zone.forbids(rule) && zone.box().contains(pos))
                return Optional.of(zone);
        return Optional.empty();
    }

    public boolean forbids(BlockPos pos, Rule rule) {
        return forbidding(pos, rule).isPresent();
    }

    /** Active zones within {@code range} blocks of the point. */
    public List<Zone> near(Vec3 point, double range) {
        double rangeSqr = range * range;
        List<Zone> result = new ArrayList<>();
        for (Zone zone : zones.values())
            if (zone.box().distanceSqr(point) <= rangeSqr)
                result.add(zone);
        return result;
    }

    private void index(Zone zone) {
        forEachChunk(zone.box(), key -> byChunk.computeIfAbsent(key, k -> new ArrayList<>(2)).add(zone));
    }

    private void unindex(Zone zone) {
        forEachChunk(zone.box(), key -> {
            List<Zone> list = byChunk.get(key);
            if (list != null) {
                list.removeIf(z -> z.id().equals(zone.id()));
                if (list.isEmpty())
                    byChunk.remove(key);
            }
        });
    }

    private static void forEachChunk(ZoneBox box, java.util.function.LongConsumer action) {
        int minCx = SectionPos.blockToSectionCoord(box.minX());
        int maxCx = SectionPos.blockToSectionCoord(box.maxX());
        int minCz = SectionPos.blockToSectionCoord(box.minZ());
        int maxCz = SectionPos.blockToSectionCoord(box.maxZ());
        for (int cx = minCx; cx <= maxCx; cx++)
            for (int cz = minCz; cz <= maxCz; cz++)
                action.accept(ChunkPos.pack(cx, cz));
    }
}
