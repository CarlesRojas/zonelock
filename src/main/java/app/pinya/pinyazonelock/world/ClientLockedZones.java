package app.pinya.pinyazonelock.world;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientLockedZones {
    private static volatile ClientLockedZones INSTANCE;
    private static final Object INSTANCE_LOCK = new Object();
    private final Map<UUID, LockedZones.Zone> zones = new LinkedHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private ClientLockedZones() {
    }

    public static ClientLockedZones getInstance() {
        ClientLockedZones result = INSTANCE;
        if (result == null) {
            synchronized (INSTANCE_LOCK) {
                result = INSTANCE;
                if (result == null) {
                    INSTANCE = result = new ClientLockedZones();
                }
            }
        }
        return result;
    }

    public void clear() {
        lock.writeLock().lock();
        try {
            zones.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void updateFromPacket(CompoundTag tag) {
        lock.writeLock().lock();
        try {
            clear();
            ListTag list = tag.getList("locked_zones", Tag.TAG_COMPOUND);

            for (int i = 0; i < list.size(); i++) {
                CompoundTag zt = list.getCompound(i);
                LockedZones.Zone z = LockedZones.Zone.loadFromTag(zt);
                zones.putIfAbsent(z.id(), z);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<LockedZones.Zone> getAllZones() {
        lock.readLock().lock();
        try {
            return List.copyOf(zones.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean isPosInAnyZone(BlockPos pos) {
        lock.readLock().lock();
        try {
            for (LockedZones.Zone z : zones.values()) {
                if (z.active() && z.contains(pos))
                    return true;
            }
            return false;
        } finally {
            lock.readLock().unlock();
        }
    }
}