package app.pinya.pinyazonelock.world;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientLockedZones {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientLockedZones.class);
    private static ClientLockedZones INSTANCE;
    private final Map<UUID, LockedZones.Zone> zones = new LinkedHashMap<>();

    private ClientLockedZones() {
    }

    public static ClientLockedZones getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ClientLockedZones();
        }
        return INSTANCE;
    }

    public void clear() {
        zones.clear();
    }

    public void updateFromPacket(CompoundTag tag) {
        clear();
        ListTag list = tag.getList("locked_zones", Tag.TAG_COMPOUND);

        for (int i = 0; i < list.size(); i++) {
            CompoundTag zt = list.getCompound(i);
            LockedZones.Zone z = LockedZones.Zone.loadFromTag(zt);
            zones.putIfAbsent(z.id(), z);
        }
    }

    public List<LockedZones.Zone> getAllZones() {
        return List.copyOf(zones.values());
    }

    public boolean isPosInAnyZone(BlockPos pos) {
        for (LockedZones.Zone z : zones.values()) {
            if (z.active() && z.contains(pos))
                return true;
        }

        return false;
    }
}