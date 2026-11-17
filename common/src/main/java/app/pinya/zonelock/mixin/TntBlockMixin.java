package app.pinya.zonelock.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import app.pinya.zonelock.zone.Rule;
import app.pinya.zonelock.zone.ZoneGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TntBlock;

/**
 * Players and mobs cannot light TNT that is part of a zone. Redstone and fire (no source entity) still can, so TNT
 * traps built by the map maker keep working; their explosions never damage the zone anyway.
 */
@Mixin(TntBlock.class)
abstract class TntBlockMixin {
    @Inject(method = "prime(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("HEAD"), cancellable = true)
    private static void zonelock$prime(Level level, BlockPos pos, @Nullable LivingEntity source, ItemStack stack,
            CallbackInfoReturnable<Boolean> cir) {
        if (source != null && !ZoneGuard.bypass(source) && ZoneGuard.forbids(level, pos, Rule.EXPLOSIONS))
            cir.setReturnValue(false);
    }
}
