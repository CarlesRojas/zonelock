package app.pinya.zonelock.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import app.pinya.zonelock.zone.Rule;
import app.pinya.zonelock.zone.ZoneGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;

/** Fluids cannot flow across a zone border, in or out. Flowing inside the zone is fine. */
@Mixin(FlowingFluid.class)
abstract class FlowingFluidMixin {
    @ModifyReturnValue(method = "canMaybePassThrough", at = @At("RETURN"))
    private boolean zonelock$border(boolean canPass, BlockGetter level, BlockPos sourcePos, BlockState sourceState,
            Direction direction, BlockPos testPos, BlockState testState, FluidState testFluidState) {
        return canPass && !(level instanceof Level world && !world.isClientSide()
                && ZoneGuard.crosses(world, sourcePos, testPos, Rule.FLUIDS_AND_FIRE));
    }
}
