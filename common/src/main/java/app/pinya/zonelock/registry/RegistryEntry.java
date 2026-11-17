package app.pinya.zonelock.registry;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

/**
 * A lazily created registry object. Common code declares entries; each loader decides when they are
 * actually created and registered (immediately on Fabric, inside RegisterEvent on NeoForge).
 */
public final class RegistryEntry<R, T extends R> implements Supplier<T> {
    private final ResourceKey<R> key;
    private final Function<ResourceKey<R>, T> factory;
    private T value;

    RegistryEntry(ResourceKey<R> key, Function<ResourceKey<R>, T> factory) {
        this.key = key;
        this.factory = factory;
    }

    public ResourceKey<R> key() {
        return key;
    }

    /** Creates the value. Called exactly once by the platform registration code. */
    public T create() {
        if (value == null)
            value = Objects.requireNonNull(factory.apply(key), key::toString);
        return value;
    }

    @Override
    public T get() {
        return Objects.requireNonNull(value, () -> "Registry entry " + key + " accessed before registration");
    }

    public boolean isBound() {
        return value != null;
    }

    public ResourceKey<? extends Registry<R>> registryKey() {
        @SuppressWarnings("unchecked")
        ResourceKey<? extends Registry<R>> registry = (ResourceKey<? extends Registry<R>>) (Object) key.registryKey();
        return registry;
    }
}
