package app.pinya.zonelock.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import app.pinya.zonelock.zone.Rule;
import app.pinya.zonelock.zone.ZoneGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.Level;

/** Filling or emptying buckets (water, lava, fish, powder snow) inside a zone. */
@Mixin(BucketItem.class)
abstract class BucketItemMixin {
    @WrapOperation(method = "use", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;mayInteract(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean zonelock$bucket(Level level, Entity entity, BlockPos pos, Operation<Boolean> original,
            @Local Direction direction) {
        if (ZoneGuard.denies(entity, level, pos, Rule.PLACE)
                || ZoneGuard.denies(entity, level, pos.relative(direction), Rule.PLACE))
            return false;
        return original.call(level, entity, pos);
    }
}
