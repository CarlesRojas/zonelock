package app.pinya.zonelock.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import app.pinya.zonelock.zone.Rule;
import app.pinya.zonelock.zone.ZoneGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Dispensers outside a zone cannot fire buckets, fire or blocks into it (ones inside it keep working). */
@Mixin(DispenserBlock.class)
abstract class DispenserBlockMixin {
    @Inject(method = "dispenseFrom", at = @At("HEAD"), cancellable = true)
    private void zonelock$dispense(ServerLevel level, BlockState state, BlockPos pos, CallbackInfo ci) {
        BlockPos target = pos.relative(state.getValue(DispenserBlock.FACING));
        if (ZoneGuard.crosses(level, pos, target, Rule.PLACE))
            ci.cancel();
    }
}
