package app.pinya.zonelock.network;

import app.pinya.zonelock.block.CoreBlockEntity;
import app.pinya.zonelock.block.CoreConfig;
import app.pinya.zonelock.zone.Rule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;

/** Server side of the client to server payloads. Never trusts the client: re-checks reach and permissions. */
public final class ServerPayloadHandler {
    private static final double MAX_DISTANCE_SQR = 96 * 96;

    private ServerPayloadHandler() {
    }

    public static void handleCoreEdit(CoreEditPayload payload, ServerPlayer player) {
        BlockPos pos = payload.pos();
        if (!player.level().isLoaded(pos) || player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) > MAX_DISTANCE_SQR)
            return;
        if (!(player.level().getBlockEntity(pos) instanceof CoreBlockEntity core))
            return;
        if (payload.op() == CoreEditPayload.Op.DISARM && core.canDisarm(player)) {
            core.disarm();
            core.refreshScreen(player);
            return;
        }
        if (!core.canEdit(player))
            return;

        CoreConfig config = core.config();
        Direction[] dirs = Direction.values();
        switch (payload.op()) {
            case SET_EXTENT -> {
                Direction dir = dirs[Math.floorMod(payload.a(), dirs.length)];
                core.setExtents(config.extents().with(dir, Math.max(0, payload.b())));
            }
            case TOGGLE_RULE -> {
                Rule[] rules = Rule.values();
                core.setConfig(config.withRules(rules[Math.floorMod(payload.a(), rules.length)].toggle(config.rules())));
            }
            case SET_NAME -> core.setConfig(config.withName(payload.text().strip()));
            case DISARM -> core.disarm();
            case TOGGLE_CONFIG_LOCK -> {
                if (CoreBlockEntity.isMapMaker(player))
                    core.setConfig(config.withConfigLocked(!config.configLocked()));
            }
        }
        core.refreshScreen(player);
    }
}
