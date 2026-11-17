package app.pinya.zonelock.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import app.pinya.zonelock.zone.Rule;
import app.pinya.zonelock.zone.ZoneGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Mob griefing: any block change made while a non-player entity is ticking (endermen, withers, ravagers, sheep,
 * silverfish, snow golems, zombies breaking doors...) is refused inside zones. Changes that keep the same block
 * (villagers opening doors, crops growing) are still allowed.
 */
@Mixin(Level.class)
abstract class LevelMixin {
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD"), cancellable = true)
    private void zonelock$mobSetBlock(BlockPos pos, BlockState state, int flags, int limit,
            CallbackInfoReturnable<Boolean> cir) {
        Entity actor = ZoneGuard.TICKING.get();
        if (actor == null || actor instanceof Player)
            return;
        Level self = (Level) (Object) this;
        if (self.isClientSide() || !ZoneGuard.forbids(self, pos, Rule.MOB_GRIEFING))
            return;
        if (actor instanceof FallingBlockEntity falling
                && !ZoneGuard.crosses(self, falling.getStartPos(), pos, Rule.MOB_GRIEFING))
            return; // sand and gravel falling inside the zone (traps)
        BlockState old = self.getBlockState(pos);
        if (old.getBlock() == state.getBlock())
            return;
        if (old.getBlock() instanceof TntBlock && state.isAir())
            return; // a mob on a pressure plate set off a TNT trap: the block became primed TNT, let it go
        cir.setReturnValue(false);
    }

    @Inject(method = "destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z",
            at = @At("HEAD"), cancellable = true)
    private void zonelock$mobDestroyBlock(BlockPos pos, boolean drop, @Nullable Entity breaker, int limit,
            CallbackInfoReturnable<Boolean> cir) {
        Entity actor = breaker != null ? breaker : ZoneGuard.TICKING.get();
        if (actor == null || actor instanceof Player)
            return;
        Level self = (Level) (Object) this;
        if (!self.isClientSide() && ZoneGuard.forbids(self, pos, Rule.MOB_GRIEFING))
            cir.setReturnValue(false);
    }
}
