package app.pinya.zonelock.client;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import app.pinya.zonelock.ZoneLock;
import app.pinya.zonelock.platform.Services;

/** Client settings, in config/zonelock-client.properties. Changed from the mod's settings screen. */
public final class ZoneLockConfig {
    /** Coming close to an armed zone shows a patch of its wall around you, even without the wand. */
    public static boolean nearbyWalls = true;

    private ZoneLockConfig() {
    }

    private static Path file() {
        return Services.PLATFORM.configDir().resolve("zonelock-client.properties");
    }

    public static void load() {
        Path file = file();
        if (!Files.exists(file))
            return;
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file)) {
            properties.load(reader);
            nearbyWalls = Boolean.parseBoolean(properties.getProperty("nearby_walls", "true"));
        } catch (IOException e) {
            ZoneLock.LOG.warn("Could not read {}", file, e);
        }
    }

    public static void save() {
        Properties properties = new Properties();
        properties.setProperty("nearby_walls", Boolean.toString(nearbyWalls));
        try {
            Files.createDirectories(file().getParent());
            try (Writer writer = Files.newBufferedWriter(file())) {
                properties.store(writer, "Zone Lock client settings");
            }
        } catch (IOException e) {
            ZoneLock.LOG.warn("Could not write {}", file(), e);
        }
    }
}
