package app.pinya.zonelock.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import app.pinya.zonelock.zone.Rule;
import app.pinya.zonelock.zone.ZoneGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.TurtleEggBlock;

@Mixin(TurtleEggBlock.class)
abstract class TurtleEggBlockMixin {
    @ModifyReturnValue(method = "canDestroyEgg", at = @At("RETURN"))
    private boolean zonelock$protected(boolean can, ServerLevel level, BlockPos pos, Entity entity) {
        return can && (ZoneGuard.bypass(entity) || !ZoneGuard.forbids(level, pos, Rule.BREAK));
    }
}
