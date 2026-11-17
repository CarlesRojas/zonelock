package app.pinya.zonelock.platform;

import java.util.ServiceLoader;

/** Loader-specific implementations, found with {@link ServiceLoader} (see META-INF/services in each loader). */
public final class Services {
    public static final PlatformHelper PLATFORM = load(PlatformHelper.class);
    public static final NetworkHelper NETWORK = load(NetworkHelper.class);

    private Services() {
    }

    public static <T> T load(Class<T> type) {
        return ServiceLoader.load(type, Services.class.getClassLoader()).findFirst()
                .orElseThrow(() -> new IllegalStateException("No implementation of " + type.getName()));
    }
}
