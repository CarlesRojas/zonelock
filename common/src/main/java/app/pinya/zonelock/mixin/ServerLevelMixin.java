package app.pinya.zonelock.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import app.pinya.zonelock.zone.ZoneGuard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/** Remembers which entity is ticking, so block changes made by mob AI can be traced back to the mob. */
@Mixin(ServerLevel.class)
abstract class ServerLevelMixin {
    @WrapMethod(method = "tickNonPassenger")
    private void zonelock$trackTickingEntity(Entity entity, Operation<Void> original) {
        Entity previous = ZoneGuard.TICKING.get();
        ZoneGuard.TICKING.set(entity);
        try {
            original.call(entity);
        } finally {
            ZoneGuard.TICKING.set(previous);
        }
    }
}
