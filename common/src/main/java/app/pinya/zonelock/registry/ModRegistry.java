package app.pinya.zonelock.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import app.pinya.zonelock.ZoneLock;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

/** An ordered list of entries for a single vanilla registry. */
public final class ModRegistry<R> {
    private final ResourceKey<? extends Registry<R>> registryKey;
    private final List<RegistryEntry<R, ? extends R>> entries = new ArrayList<>();

    public ModRegistry(ResourceKey<? extends Registry<R>> registryKey) {
        this.registryKey = registryKey;
    }

    public <T extends R> RegistryEntry<R, T> register(String name, Function<ResourceKey<R>, T> factory) {
        ResourceKey<R> key = ResourceKey.create(registryKey, ZoneLock.id(name));
        RegistryEntry<R, T> entry = new RegistryEntry<>(key, factory);
        entries.add(entry);
        return entry;
    }

    public ResourceKey<? extends Registry<R>> registryKey() {
        return registryKey;
    }

    public List<RegistryEntry<R, ? extends R>> entries() {
        return Collections.unmodifiableList(entries);
    }
}
