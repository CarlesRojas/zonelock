package app.pinya.zonelock.block;

import app.pinya.zonelock.registry.ModBlocks;
import app.pinya.zonelock.zone.ZoneManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Only exists to register the seal with its dimension's {@link ZoneManager}, including seals placed by worldgen. */
public class SealBlockEntity extends BlockEntity {
    private boolean registered;

    public SealBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.SEAL_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SealBlockEntity seal) {
        if (!seal.registered && level instanceof ServerLevel serverLevel) {
            seal.registered = true;
            ZoneManager.get(serverLevel).trackSeal(pos, state.getValue(SealBlock.LIT));
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel serverLevel)
            ZoneManager.get(serverLevel).untrackSeal(pos);
    }
}
