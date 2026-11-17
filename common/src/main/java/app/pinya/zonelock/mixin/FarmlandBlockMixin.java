package app.pinya.zonelock.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import app.pinya.zonelock.zone.Rule;
import app.pinya.zonelock.zone.ZoneGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Nobody tramples the farmland of a zone (drying out without water still happens). */
@Mixin(FarmlandBlock.class)
abstract class FarmlandBlockMixin {
    @Inject(method = "turnToBaseBlock", at = @At("HEAD"), cancellable = true)
    private void zonelock$noTrample(@Nullable Entity source, BlockState state, Level level, BlockPos pos,
            CallbackInfo ci) {
        if (source != null && !ZoneGuard.bypass(source)
                && ZoneGuard.forbids(level, pos, source instanceof net.minecraft.world.entity.player.Player
                        ? Rule.BREAK : Rule.MOB_GRIEFING))
            ci.cancel();
    }
}
