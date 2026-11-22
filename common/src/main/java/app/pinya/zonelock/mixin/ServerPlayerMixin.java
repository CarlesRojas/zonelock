package app.pinya.zonelock.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import app.pinya.zonelock.zone.Zone;
import app.pinya.zonelock.zone.ZoneManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * A bed or respawn anchor inside a locked zone doesn't bring you back there: dying is always a way out of a dungeon,
 * never a way back into the room you were stuck in. You respawn at the world spawn instead.
 */
@Mixin(ServerPlayer.class)
abstract class ServerPlayerMixin {
    @Inject(method = "findRespawnAndUseSpawnBlock", at = @At("HEAD"), cancellable = true)
    private static void zonelock$notInLockedZone(ServerLevel level, ServerPlayer.RespawnConfig config, boolean useCharge,
            CallbackInfoReturnable<Optional<?>> cir) {
        BlockPos pos = config.respawnData().pos();
        for (Zone zone : ZoneManager.get(level).index().at(pos))
            if (zone.active()) {
                cir.setReturnValue(Optional.empty());
                return;
            }
    }
}
