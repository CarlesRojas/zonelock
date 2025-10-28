package app.pinya.pinyazonelock.block.custom;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.mojang.serialization.MapCodec;

import app.pinya.pinyazonelock.block.entity.custom.ZoneLockCoreEntity;
import app.pinya.pinyazonelock.world.LockedZones;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class ZoneLockCore extends BaseEntityBlock {
  public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
  public static final MapCodec<ZoneLockCore> CODEC = simpleCodec(ZoneLockCore::new);

  public ZoneLockCore(Properties properties) {
    super(properties);
    this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, false));
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

      lockedZones.setActive(pos, false);
      level.setBlockAndUpdate(pos, state.setValue(ACTIVE, false));
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
        && level instanceof ServerLevel sLevel)
      LockedZones.get(sLevel).removeZone(pos);

    if (oldState.getBlock() != newState.getBlock()
        && level.getBlockEntity(pos) instanceof ZoneLockCoreEntity zonelockCoreEntity) {
      zonelockCoreEntity.dropContents();
      level.updateNeighbourForOutputSignal(pos, this);
    }

    super.onRemove(oldState, level, pos, newState, isMoving);
  }

  @Override
  protected ItemInteractionResult useItemOn(
      ItemStack pStack, BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand,
      BlockHitResult pHitResult) {

    if (pLevel.getBlockEntity(pPos) instanceof ZoneLockCoreEntity entity) {
      if (!pLevel.isClientSide()) {
        ((ServerPlayer) pPlayer)
            .openMenu(new SimpleMenuProvider(entity, Component.translatable("block.pinyazonelock.zonelockcore")), pPos);
      }
    }

    return ItemInteractionResult.SUCCESS;
  }

  @Override
  public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
    return new ZoneLockCoreEntity(pPos, pState);
  }

  @Override
  protected RenderShape getRenderShape(BlockState pState) {
    return RenderShape.MODEL;
  }

  @Override
  protected MapCodec<? extends BaseEntityBlock> codec() {
    return CODEC;
  }
}
