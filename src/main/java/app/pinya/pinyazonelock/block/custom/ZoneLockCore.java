package app.pinya.pinyazonelock.block.custom;

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
//
//  @Override
//  public void setPlacedBy(
//      Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
//
//    if (!level.isClientSide) LockedZones.get(level).addAnchor(pos);
//
//    super.setPlacedBy(level, pos, state, placer, stack);
//  }
//
//  @Override
//  public void onRemove(
//      BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
//
//    if (!level.isClientSide && oldState.getBlock() != newState.getBlock())
//      LockedZones.get(level).removeAnchor(pos);
//
//    super.onRemove(oldState, level, pos, newState, isMoving);
//  }
}
