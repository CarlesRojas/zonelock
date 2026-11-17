package app.pinya.zonelock.mixin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import app.pinya.zonelock.zone.Rule;
import app.pinya.zonelock.zone.ZoneGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Explosions still hurt, but leave protected blocks alone (and light no fires there).
 * <p>
 * Entities are hurt as if the protected blocks the blast would have destroyed were already gone: otherwise a block
 * that survives only thanks to the zone would shield them, and TNT traps (the desert temple's, under its floor)
 * would stop hurting anyone.
 */
@Mixin(ServerExplosion.class)
abstract class ServerExplosionMixin {
    /** Protected blocks the explosion going off on this thread would have destroyed. */
    @Unique
    private static final ThreadLocal<Set<BlockPos>> zonelock$spared = new ThreadLocal<>();

    @Shadow
    @Final
    private ServerLevel level;

    @ModifyReturnValue(method = "calculateExplodedPositions", at = @At("RETURN"))
    private List<BlockPos> zonelock$filter(List<BlockPos> positions) {
        if (ZoneGuard.index(level).isEmpty())
            return positions;
        Set<BlockPos> spared = zonelock$spared.get();
        positions.removeIf(pos -> {
            if (!ZoneGuard.forbids(level, pos, Rule.EXPLOSIONS))
                return false;
            if (spared != null)
                spared.add(pos);
            return true;
        });
        return positions;
    }

    @WrapMethod(method = "explode")
    private int zonelock$explode(Operation<Integer> original) {
        Set<BlockPos> previous = zonelock$spared.get();
        zonelock$spared.set(new HashSet<>());
        try {
            return original.call();
        } finally {
            zonelock$spared.set(previous);
        }
    }

    @WrapOperation(method = "getSeenPercent", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;clip(Lnet/minecraft/world/level/ClipContext;)Lnet/minecraft/world/phys/BlockHitResult;"))
    private static BlockHitResult zonelock$seeThroughSpared(Level level, ClipContext context,
            Operation<BlockHitResult> original) {
        Set<BlockPos> spared = zonelock$spared.get();
        if (spared == null || spared.isEmpty())
            return original.call(level, context);
        // BlockGetter.clip without fluids (explosions ignore them), skipping the spared blocks.
        return BlockGetter.traverseBlocks(context.getFrom(), context.getTo(), context, (c, pos) -> {
            if (spared.contains(pos))
                return null;
            BlockState state = level.getBlockState(pos);
            return level.clipWithInteractionOverride(c.getFrom(), c.getTo(), pos, c.getBlockShape(state, level, pos),
                    state);
        }, c -> {
            Vec3 delta = c.getFrom().subtract(c.getTo());
            return BlockHitResult.miss(c.getTo(), Direction.getApproximateNearest(delta.x, delta.y, delta.z),
                    BlockPos.containing(c.getTo()));
        });
    }
}
