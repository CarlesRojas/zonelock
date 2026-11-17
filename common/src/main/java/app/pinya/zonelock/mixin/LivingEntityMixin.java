package app.pinya.zonelock.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import app.pinya.zonelock.zone.Rule;
import app.pinya.zonelock.zone.ZoneGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Chorus fruit (and any random teleport of a player) never lands inside a zone. */
@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
    @ModifyReturnValue(method = "canRandomlyTeleportTo", at = @At("RETURN"))
    private boolean zonelock$noRandomTeleportIn(boolean can, double x, double y, double z) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!can || !(self instanceof Player) || self.level().isClientSide() || ZoneGuard.bypass(self))
            return can;
        return !ZoneGuard.forbids(self.level(), BlockPos.containing(x, y, z), Rule.TELEPORT);
    }
}
