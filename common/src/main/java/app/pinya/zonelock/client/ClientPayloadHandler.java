package app.pinya.zonelock.client;

import app.pinya.zonelock.client.screen.CoreScreen;
import app.pinya.zonelock.network.BarrierHitPayload;
import app.pinya.zonelock.network.CoreScreenPayload;
import app.pinya.zonelock.network.SealPingPayload;
import app.pinya.zonelock.network.ZoneSyncPayload;
import app.pinya.zonelock.zone.ClientZoneCache;
import app.pinya.zonelock.zone.Rule;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client side of every server to client payload. Always called on the client thread. */
public final class ClientPayloadHandler {
    private ClientPayloadHandler() {
    }

    public static void handle(CustomPacketPayload payload) {
        switch (payload) {
            case ZoneSyncPayload sync -> {
                if (sync.replace())
                    ClientZoneCache.INDEX.clear();
                sync.removed().forEach(ClientZoneCache.INDEX::remove);
                sync.zones().forEach(ClientZoneCache.INDEX::put);
                ZoneHud.onZonesChanged();
            }
            case SealPingPayload ping -> ZoneVisuals.pingSeals(ping.core(), ping.seals());
            case BarrierHitPayload hit -> ZoneVisuals.barrierHit(hit.pos(),
                    Rule.values()[Math.floorMod(hit.reason(), Rule.values().length)]);
            case CoreScreenPayload view -> CoreScreen.receive(view);
            default -> {
            }
        }
    }
}
