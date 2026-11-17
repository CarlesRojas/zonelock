package app.pinya.zonelock.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import app.pinya.zonelock.zone.Rule;
import app.pinya.zonelock.zone.ZoneGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.Projectile;

/** Arrows and other projectiles cannot light TNT, break pots or chorus flowers inside zones. */
@Mixin(Projectile.class)
abstract class ProjectileMixin {
    @ModifyReturnValue(method = "mayInteract", at = @At("RETURN"))
    private boolean zonelock$protected(boolean may, ServerLevel level, BlockPos pos) {
        return may && (ZoneGuard.bypass(((Projectile) (Object) this).getOwner())
                || !ZoneGuard.forbids(level, pos, Rule.BREAK));
    }
}
