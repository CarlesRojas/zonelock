package app.pinya.zonelock.client;

import app.pinya.zonelock.zone.ClientZoneCache;
import app.pinya.zonelock.zone.ZoneGuard;

/** Client entry points, called by each loader's client initializer and events. */
public final class ZoneLockClient {
    private ZoneLockClient() {
    }

    public static void init() {
        ZoneGuard.ClientFeedback.HANDLER = ZoneVisuals::barrierHit;
        ZoneLockConfig.load();
    }

    /** End of every client tick (a gizmo collector is active there). */
    public static void onClientTick() {
        ZoneHud.tick();
        ZoneVisuals.tick();
    }

    public static void onDisconnect() {
        ClientZoneCache.INDEX.clear();
    }
}
