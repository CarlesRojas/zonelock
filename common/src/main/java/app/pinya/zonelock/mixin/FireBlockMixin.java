package app.pinya.zonelock.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;

import app.pinya.zonelock.zone.Rule;
import app.pinya.zonelock.zone.ZoneGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Fire does not spread into zones and does not burn their blocks away. */
@Mixin(FireBlock.class)
abstract class FireBlockMixin {
    @ModifyReturnValue(method = "getIgniteOdds(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)I",
            at = @At("RETURN"))
    private int zonelock$noSpread(int odds, LevelReader level, BlockPos pos) {
        return level instanceof Level world && ZoneGuard.forbids(world, pos, Rule.FLUIDS_AND_FIRE) ? 0 : odds;
    }

    // Name-only target: NeoForge adds a Direction parameter to checkBurnOut.
    @WrapWithCondition(method = "checkBurnOut", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean zonelock$noBurnToFire(Level level, BlockPos pos, BlockState state) {
        return !ZoneGuard.forbids(level, pos, Rule.FLUIDS_AND_FIRE);
    }

    @WrapWithCondition(method = "checkBurnOut", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    private boolean zonelock$noBurnAway(Level level, BlockPos pos, boolean movedByPiston) {
        return !ZoneGuard.forbids(level, pos, Rule.FLUIDS_AND_FIRE);
    }
}
