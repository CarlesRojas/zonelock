package app.pinya.pinyazonelock.world;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.pinya.pinyazonelock.networking.ModMessages;
import app.pinya.pinyazonelock.networking.ZoneDataSyncS2CPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

public class LockedZones extends SavedData {
    private static final Logger LOGGER = LoggerFactory.getLogger(LockedZones.class);
    private static final String DATA_NAME = "pinya_locked_zones";

    private final Map<UUID, Zone> zones = new LinkedHashMap<>();

    public LockedZones() {
    }

    public static LockedZones create(CompoundTag tag, HolderLookup.Provider lookup) {
        LockedZones data = new LockedZones();
        ListTag list = tag.getList("locked_zones", Tag.TAG_COMPOUND);

        for (int i = 0; i < list.size(); i++) {
            CompoundTag zt = list.getCompound(i);
            Zone z = Zone.loadFromTag(zt);
            data.zones.putIfAbsent(z.id(), z);
        }

        return data;
    }

    public static LockedZones get(ServerLevel level) {
        return level
                .getDataStorage()
                .computeIfAbsent(
                        new SavedData.Factory<>(LockedZones::new, LockedZones::create, DataFixTypes.LEVEL),
                        DATA_NAME);
    }

    public List<Zone> getAllZones() {
        return List.copyOf(zones.values());
    }

    public Optional<Zone> getZone(BlockPos center) {
        return zones.values().stream().filter(z -> z.center().equals(center)).findFirst();
    }

    public List<Zone> getZonesAffecting(BlockPos pos) {
        if (zones.isEmpty())
            return List.of();
        return zones.values().stream().filter(Zone::active).filter(z -> z.contains(pos)).toList();
    }

    public boolean isPosInAnyZone(BlockPos pos) {
        LOGGER.info("----------------------------------------------------");
        for (Zone z : zones.values()) {
            LOGGER.info("Checking Zone {}, UP: {}. {}", z.center, z.upExtent, z.active() && z.contains(pos));
            if (z.active() && z.contains(pos))
                return true;
        }
        LOGGER.info("----------------------------------------------------");

        return false;
    }

    public void addZone(
            BlockPos center,
            int upExtent,
            int downExtent,
            int northExtent,
            int southExtent,
            int eastExtent,
            int westExtent) {

        Zone zone = new Zone(
                UUID.randomUUID(),
                center.immutable(),
                upExtent,
                downExtent,
                northExtent,
                southExtent,
                eastExtent,
                westExtent,
                false);

        zones.put(zone.id(), zone);
        setDirty();
        syncToClients();
    }

    public void removeZone(
            BlockPos center) {
        Optional<Zone> foundZone = getZone(center);

        if (foundZone.isPresent()) {
            Zone zone = foundZone.get();
            zones.remove(zone.id());
            setDirty();
            syncToClients();
        }
    }

    public void updateZone(
            BlockPos center,
            int newUpExtent,
            int newDownExtent,
            int newNorthExtent,
            int newSouthExtent,
            int newEastExtent,
            int newWestExtent) {

        Optional<Zone> foundZone = getZone(center);

        if (foundZone.isPresent()) {
            Zone zone = foundZone.get();
            Zone updated = new Zone(
                    zone.id(),
                    zone.center(),
                    newUpExtent,
                    newDownExtent,
                    newNorthExtent,
                    newSouthExtent,
                    newEastExtent,
                    newWestExtent,
                    zone.active());
            zones.put(zone.id(), updated);
            setDirty();
            syncToClients();
        }
    }

    public void setActive(BlockPos center, boolean active) {

        Optional<Zone> foundZone = getZone(center);

        if (foundZone.isPresent()) {
            Zone zone = foundZone.get();

            if (zone.active() != active) {
                Zone updatedZone = new Zone(
                        zone.id(),
                        zone.center(),
                        zone.upExtent(),
                        zone.downExtent(),
                        zone.northExtent(),
                        zone.southExtent(),
                        zone.eastExtent(),
                        zone.westExtent(),
                        active);

                zones.put(zone.id(), updatedZone);

                setDirty();
                syncToClients();
            }
        }
    }

    public void syncToClients() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (Zone z : zones.values())
            list.add(z.saveToTag());
        tag.put("locked_zones", list);

        ModMessages.sendToAllPlayers(new ZoneDataSyncS2CPacket(tag));
    }

    @Override
    public @NotNull CompoundTag save(
            @NotNull CompoundTag tag, HolderLookup.@NotNull Provider lookup) {

        ListTag list = new ListTag();
        for (Zone z : zones.values())
            list.add(z.saveToTag());
        tag.put("locked_zones", list);

        tag.putInt("version", 1);

        return tag;
    }

    public record Zone(
            UUID id,
            BlockPos center,
            int upExtent,
            int downExtent,
            int northExtent,
            int southExtent,
            int eastExtent,
            int westExtent,
            boolean active) {

        public Zone(
                UUID id,
                BlockPos center,
                int upExtent,
                int downExtent,
                int northExtent,
                int southExtent,
                int eastExtent,
                int westExtent,
                boolean active) {

            if (upExtent < 0
                    || downExtent < 0
                    || northExtent < 0
                    || southExtent < 0
                    || eastExtent < 0
                    || westExtent < 0) {
                throw new IllegalArgumentException("all extents must be >= 0");
            }

            this.id = Objects.requireNonNull(id, "id");
            this.center = Objects.requireNonNull(center, "center");
            this.upExtent = upExtent;
            this.downExtent = downExtent;
            this.northExtent = northExtent;
            this.southExtent = southExtent;
            this.eastExtent = eastExtent;
            this.westExtent = westExtent;
            this.active = active;
        }

        public static Zone loadFromTag(CompoundTag tag) {
            UUID id = tag.getUUID("id");
            int x = tag.getInt("x");
            int y = tag.getInt("y");
            int z = tag.getInt("z");
            int up = tag.getInt("upExtent");
            int down = tag.getInt("downExtent");
            int north = tag.getInt("northExtent");
            int south = tag.getInt("southExtent");
            int east = tag.getInt("eastExtent");
            int west = tag.getInt("westExtent");
            boolean active = !tag.contains("active") || tag.getBoolean("active");

            return new Zone(id, new BlockPos(x, y, z), up, down, north, south, east, west, active);
        }

        public boolean contains(BlockPos pos) {
            int dx = pos.getX() - center.getX();
            int dy = pos.getY() - center.getY();
            int dz = pos.getZ() - center.getZ();

            return dy <= upExtent
                    && dy >= -downExtent
                    && dz >= -northExtent
                    && dz <= southExtent
                    && dx <= eastExtent
                    && dx >= -westExtent;
        }

        public CompoundTag saveToTag() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("id", id);
            tag.putInt("x", center.getX());
            tag.putInt("y", center.getY());
            tag.putInt("z", center.getZ());
            tag.putInt("upExtent", upExtent);
            tag.putInt("downExtent", downExtent);
            tag.putInt("northExtent", northExtent);
            tag.putInt("southExtent", southExtent);
            tag.putInt("eastExtent", eastExtent);
            tag.putInt("westExtent", westExtent);
            tag.putBoolean("active", active);
            return tag;
        }
    }
}
