package app.pinya.pinyazonelock.world;

import java.util.*;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

public class LockedZones extends SavedData {
  private static final String DATA_NAME = "pinya_locked_zones";

  private final Map<UUID, Zone> zones = new LinkedHashMap<>();

  public LockedZones() {}

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

  public Optional<Zone> getZone(UUID id) {
    return Optional.ofNullable(zones.get(id));
  }

  public List<Zone> getZonesAffecting(BlockPos pos) {
    if (zones.isEmpty()) return List.of();
    return zones.values().stream().filter(z -> z.contains(pos)).collect(Collectors.toList());
  }

  public boolean isPosInAnyZone(BlockPos pos) {
    for (Zone z : zones.values()) if (z.contains(pos)) return true;
    return false;
  }

  public Zone addZone(
      BlockPos center,
      int upExtent,
      int downExtent,
      int northExtent,
      int southExtent,
      int eastExtent,
      int westExtent) {
    Zone z =
        new Zone(
            UUID.randomUUID(),
            center.immutable(),
            upExtent,
            downExtent,
            northExtent,
            southExtent,
            eastExtent,
            westExtent);

    zones.put(z.id(), z);
    setDirty();
    return z;
  }

  public boolean removeZone(UUID id) {
    Zone removed = zones.remove(id);
    if (removed != null) {
      setDirty();
      return true;
    }
    return false;
  }

  public Optional<UUID> removeZoneByCenter(BlockPos center) {
    UUID found = null;

    for (Zone z : zones.values()) {
      if (z.center().equals(center)) {
        found = z.id();
        break;
      }
    }

    if (found != null) {
      zones.remove(found);
      setDirty();
      return Optional.of(found);
    }

    return Optional.empty();
  }

  public Optional<Zone> updateZone(
      UUID id,
      BlockPos newCenter,
      int newUpExtent,
      int newDownExtent,
      int newNorthExtent,
      int newSouthExtent,
      int newEastExtent,
      int newWestExtent) {
    Zone old = zones.get(id);
    if (old == null) return Optional.empty();

    Zone updated =
        new Zone(
            id,
            newCenter.immutable(),
            newUpExtent,
            newDownExtent,
            newNorthExtent,
            newSouthExtent,
            newEastExtent,
            newWestExtent);
    zones.put(id, updated);
    setDirty();
    return Optional.of(updated);
  }

  @Override
  public @NotNull CompoundTag save(
      @NotNull CompoundTag tag, HolderLookup.@NotNull Provider lookup) {

    ListTag list = new ListTag();
    for (Zone z : zones.values()) list.add(z.saveToTag());
    tag.put("locked_zones", list);

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
      int westExtent) {

    public Zone(
        UUID id,
        BlockPos center,
        int upExtent,
        int downExtent,
        int northExtent,
        int southExtent,
        int eastExtent,
        int westExtent) {

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
      return new Zone(id, new BlockPos(x, y, z), up, down, north, south, east, west);
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
      return tag;
    }

    //    @Override
    //    public @NotNull String toString() {
    //      return String.format(
    //          "Zone{id=%s, center=%s, extents=[u=%d,d=%d,n=%d,s=%d,e=%d,w=%d]}",
    //          id, center, upExtent, downExtent, northExtent, southExtent, eastExtent, westExtent);
    //    }
    //
    //    @Override
    //    public boolean equals(Object o) {
    //      if (this == o) return true;
    //      if (!(o instanceof Zone)) return false;
    //
    //      Zone zone = (Zone) o;
    //      return upExtent == zone.upExtent
    //          && downExtent == zone.downExtent
    //          && northExtent == zone.northExtent
    //          && southExtent == zone.southExtent
    //          && eastExtent == zone.eastExtent
    //          && westExtent == zone.westExtent
    //          && id.equals(zone.id)
    //          && center.equals(zone.center);
    //    }
  }
}
