package app.pinya.zonelock.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import app.pinya.zonelock.zone.Rule;
import app.pinya.zonelock.zone.ZoneGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.Enderman;

/** Without this an enderman would get a copy of a protected block in its hands. */
@Mixin(targets = "net.minecraft.world.entity.monster.Enderman$EndermanTakeBlockGoal")
abstract class EndermanTakeBlockGoalMixin {
    @Shadow
    @Final
    private Enderman enderman;

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean zonelock$protected(boolean holdable, @Local BlockPos pos) {
        return holdable && !ZoneGuard.forbids(enderman.level(), pos, Rule.MOB_GRIEFING);
    }
}
