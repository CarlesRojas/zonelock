package app.pinya.pinyazonelock.block.custom;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import app.pinya.pinyazonelock.world.LockedZones;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class ZoneLockCore extends Block {
  public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

  public ZoneLockCore(Properties properties) {
    super(properties);
    this.registerDefaultState(this.defaultBlockState().setValue(ACTIVE, false));
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
    pBuilder.add(ACTIVE);
  }

  @Override
  public void setPlacedBy(
      Level level,
      @NotNull BlockPos pos,
      @NotNull BlockState state,
      @Nullable LivingEntity placer,
      @NotNull ItemStack stack) {

    if (!level.isClientSide && level instanceof ServerLevel sLevel) {
      LockedZones lockedZones = LockedZones.get(sLevel);
      lockedZones.addZone(pos, 8, 8, 8, 8, 8, 8);

      // TODO activate zone when a metal block is placed in the UI
      lockedZones.setActive(pos, true);
      level.setBlockAndUpdate(pos, state.setValue(ACTIVE, true));
    }

    super.setPlacedBy(level, pos, state, placer, stack);
  }

  @Override
  public void onRemove(
      @NotNull BlockState oldState,
      Level level,
      @NotNull BlockPos pos,
      @NotNull BlockState newState,
      boolean isMoving) {

    if (!level.isClientSide
        && oldState.getBlock() != newState.getBlock()
        && level instanceof ServerLevel sLevel) LockedZones.get(sLevel).removeZone(pos);

    super.onRemove(oldState, level, pos, newState, isMoving);
  }
}
