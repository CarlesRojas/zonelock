package app.pinya.zonelock.zone;

/**
 * Client mirror of the zones of the dimension the local player is in. Lives in common code (it has no client-only
 * dependencies) so shared mixins can predict protection on the client and avoid flicker.
 */
public final class ClientZoneCache {
    public static final ZoneIndex INDEX = new ZoneIndex();

    private ClientZoneCache() {
    }
}
