package app.pinya.zonelock.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import app.pinya.zonelock.zone.Rule;
import app.pinya.zonelock.zone.ZoneGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonStructureResolver;

/**
 * Pistons may not move blocks across a zone border. Contraptions fully inside the zone (dungeon puzzles) keep
 * working.
 */
@Mixin(PistonStructureResolver.class)
abstract class PistonStructureResolverMixin {
    @Shadow @Final private Level level;
    @Shadow @Final private BlockPos pistonPos;
    @Shadow @Final private Direction pushDirection;
    @Shadow @Final private List<BlockPos> toPush;
    @Shadow @Final private List<BlockPos> toDestroy;

    @ModifyReturnValue(method = "resolve", at = @At("RETURN"))
    private boolean zonelock$zones(boolean canMove) {
        if (!canMove || ZoneGuard.index(level).isEmpty())
            return canMove;
        for (BlockPos pos : toPush)
            if (crosses(pos) || crosses(pos.relative(pushDirection)))
                return false;
        for (BlockPos pos : toDestroy)
            if (crosses(pos))
                return false;
        return true;
    }

    private boolean crosses(BlockPos pos) {
        return ZoneGuard.crosses(level, pistonPos, pos, Rule.PISTONS);
    }
}
