package app.pinya.zonelock.zone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import app.pinya.zonelock.ZoneLock;
import app.pinya.zonelock.network.ZoneSyncPayload;
import app.pinya.zonelock.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Server-side source of truth for every zone of one dimension, plus the seals that feed their locks. Persisted with the
 * dimension so protection keeps working when the core's chunk is not loaded.
 */
public final class ZoneManager extends SavedData {

    private record Stored(List<Zone> zones, Map<BlockPos, Boolean> seals) {
        static final Codec<Stored> CODEC = RecordCodecBuilder.create(i -> i.group(
                Zone.CODEC.listOf().optionalFieldOf("zones", List.of()).forGetter(Stored::zones),
                Codec.unboundedMap(Codec.STRING.xmap(ZoneManager::parsePos, ZoneManager::posKey), Codec.BOOL)
                        .optionalFieldOf("seals", Map.of()).forGetter(Stored::seals))
                .apply(i, Stored::new));
    }

    private static final Codec<ZoneManager> CODEC = Stored.CODEC.xmap(ZoneManager::new, ZoneManager::store);

    public static final SavedDataType<ZoneManager> TYPE = new SavedDataType<>(ZoneLock.id("zones"),
            ZoneManager::new, CODEC, null);

    private final ZoneIndex index = new ZoneIndex();
    /** Seal position to "still lit". */
    private final Map<BlockPos, Boolean> seals = new HashMap<>();

    private final Set<UUID> pendingSync = new LinkedHashSet<>();
    private final Set<UUID> pendingRemoval = new LinkedHashSet<>();

    public ZoneManager() {
    }

    private ZoneManager(Stored stored) {
        stored.zones.forEach(index::put);
        seals.putAll(stored.seals);
    }

    private Stored store() {
        return new Stored(List.copyOf(index.all()), Map.copyOf(seals));
    }

    public static ZoneManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public ZoneIndex index() {
        return index;
    }

    public Optional<Zone> zone(UUID id) {
        return index.get(id);
    }

    public Optional<Zone> coreZone(BlockPos core) {
        for (Zone zone : index.at(core))
            if (zone.anchor().equals(core))
                return Optional.of(zone);
        return Optional.empty();
    }

    // ---------------------------------------------------------------- zones

    /** Adds or replaces a zone, refreshing its seal counters. */
    public Zone put(Zone zone) {
        Zone counted = withCounters(zone);
        Optional<Zone> old = index.get(zone.id());
        if (old.isPresent() && old.get().equals(counted))
            return counted;
        index.put(counted);
        changed(counted.id());
        return counted;
    }

    public void remove(UUID id) {
        if (index.remove(id).isPresent()) {
            pendingSync.remove(id);
            pendingRemoval.add(id);
            setDirty();
        }
    }

    public void setActive(UUID id, boolean active) {
        index.get(id).ifPresent(zone -> {
            if (zone.active() != active)
                put(zone.withActive(active));
        });
    }

    private Zone withCounters(Zone zone) {
        int total = 0;
        int lit = 0;
        for (Map.Entry<BlockPos, Boolean> seal : seals.entrySet()) {
            if (zone.box().contains(seal.getKey())) {
                total++;
                if (seal.getValue())
                    lit++;
            }
        }
        return zone.withLock(zone.lock().withSeals(total, lit));
    }

    private void refreshZonesAt(BlockPos pos) {
        for (Zone zone : index.at(pos))
            put(zone);
    }

    // ---------------------------------------------------------------- seals

    public void trackSeal(BlockPos pos, boolean lit) {
        BlockPos key = pos.immutable();
        Boolean old = seals.put(key, lit);
        if (old == null || old != lit) {
            setDirty();
            refreshZonesAt(key);
        }
    }

    public void untrackSeal(BlockPos pos) {
        if (seals.remove(pos) != null) {
            setDirty();
            refreshZonesAt(pos);
        }
    }

    public boolean isSealTracked(BlockPos pos) {
        return seals.containsKey(pos);
    }

    /** Seals inside the zone, with whether they are still lit. */
    public Map<BlockPos, Boolean> sealsIn(Zone zone) {
        Map<BlockPos, Boolean> result = new HashMap<>();
        seals.forEach((pos, lit) -> {
            if (zone.box().contains(pos))
                result.put(pos, lit);
        });
        return result;
    }

    // ---------------------------------------------------------------- sync

    private void changed(UUID id) {
        pendingRemoval.remove(id);
        pendingSync.add(id);
        setDirty();
    }

    /** Sends every pending change to the players in this dimension. Called once per server tick. */
    public void flush(ServerLevel level) {
        if (pendingSync.isEmpty() && pendingRemoval.isEmpty())
            return;
        List<Zone> updated = new ArrayList<>(pendingSync.size());
        for (UUID id : pendingSync)
            index.get(id).ifPresent(updated::add);
        ZoneSyncPayload payload = new ZoneSyncPayload(false, updated, List.copyOf(pendingRemoval));
        pendingSync.clear();
        pendingRemoval.clear();
        for (ServerPlayer player : level.players())
            Services.NETWORK.sendToPlayer(player, payload);
    }

    /** Sends the complete zone list of this dimension, replacing whatever the client had. */
    public void sendAll(ServerPlayer player) {
        Services.NETWORK.sendToPlayer(player, new ZoneSyncPayload(true, List.copyOf(index.all()), List.of()));
    }

    // ---------------------------------------------------------------- helpers

    private static String posKey(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private static BlockPos parsePos(String key) {
        String[] parts = key.split(",");
        return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }
}
