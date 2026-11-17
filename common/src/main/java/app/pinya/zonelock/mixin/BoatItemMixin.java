package app.pinya.zonelock.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.sugar.Local;

import app.pinya.zonelock.zone.Rule;
import app.pinya.zonelock.zone.ZoneGuard;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.level.Level;

/** Boats would let players skip water puzzles. */
@Mixin(BoatItem.class)
abstract class BoatItemMixin {
    @Inject(method = "use", cancellable = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private void zonelock$boat(Level level, Player player, InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir, @Local AbstractBoat boat) {
        if (ZoneGuard.denies(player, level, boat.blockPosition(), Rule.PLACE))
            cir.setReturnValue(InteractionResult.FAIL);
    }
}
