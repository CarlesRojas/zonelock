package app.pinya.zonelock.block;

import org.jspecify.annotations.Nullable;

import app.pinya.zonelock.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CoreBlock extends BaseEntityBlock {
    public static final BooleanProperty ARMED = BooleanProperty.create("armed");
    /** Transform applied by a structure template; consumed (reset to NONE) by the block entity on its first tick. */
    public static final EnumProperty<Rotation> ROTATION = EnumProperty.create("rotation", Rotation.class);
    public static final EnumProperty<Mirror> MIRROR = EnumProperty.create("mirror", Mirror.class);
    /** Receives a redstone signal: the Redstone Core's redstone glows, like a repeater's torch. */
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    /** Side the keyhole is on: it faces whoever placed the core. */
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    /** How a core is switched off. Each kind is its own block with its own recipe. */
    public enum Lock {
        /** Amethyst Core: every seal in the zone must be put out first (with no seals, reaching it is enough). */
        SEALS,
        /** Redstone Core: switched off by a redstone signal. */
        REDSTONE
    }

    /**
     * The block's exact shape, as the model draws it: a plinth, a column, a capital and the socket's rim around the
     * hole the crystal rises from.
     */
    private static final VoxelShape BODY = Shapes.join(Shapes.or(Block.box(0, 0, 0, 16, 3, 16),
            Block.box(2, 3, 2, 14, 11, 14), Block.box(1, 11, 1, 15, 16, 15)), Block.box(4, 14, 4, 12, 16, 12),
            BooleanOp.ONLY_FIRST);
    private final Lock lock;

    public CoreBlock(Properties properties, Lock lock) {
        super(properties);
        this.lock = lock;
        registerDefaultState(stateDefinition.any().setValue(ARMED, false).setValue(ROTATION, Rotation.NONE)
                .setValue(MIRROR, Mirror.NONE).setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }

    public Lock lock() {
        return lock;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return BODY;
    }

    /** A core can't stand inside another core's zone: zones never cross. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        java.util.List<app.pinya.zonelock.zone.Zone> here = level instanceof ServerLevel serverLevel
                ? app.pinya.zonelock.zone.ZoneManager.get(serverLevel).index().at(pos)
                : app.pinya.zonelock.zone.ClientZoneCache.INDEX.at(pos);
        if (!here.isEmpty())
            return null;
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ARMED, ROTATION, MIRROR, FACING, POWERED);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CoreBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        return level instanceof ServerLevel ? createTickerHelper(type, ModBlocks.CORE_ENTITY.get(),
                CoreBlockEntity::serverTick) : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        super.setPlacedBy(level, pos, state, by, stack);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CoreBlockEntity core)
            core.onPlacedBy(by instanceof Player player ? player : null);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty())
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (stack.getItem() instanceof app.pinya.zonelock.item.ResonanceWandItem)
            return InteractionResult.PASS; // let the wand tune itself to the core instead of opening its screen
        if (level.getBlockEntity(pos) instanceof CoreBlockEntity core) {
            if (core.ignores(player))
                return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (level.isClientSide())
                return InteractionResult.SUCCESS;
            if (stack.is(net.minecraft.world.item.Items.AMETHYST_SHARD) && core.placeCrystal((ServerPlayer) player, stack))
                return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CoreBlockEntity core) || core.ignores(player))
            return InteractionResult.PASS;
        if (!level.isClientSide())
            core.use((ServerPlayer) player);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
            @Nullable Orientation orientation, boolean movedByPiston) {
        if (level.isClientSide())
            return;
        boolean powered = level.hasNeighborSignal(pos);
        if (state.getValue(POWERED) != powered)
            level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
        if (level.getBlockEntity(pos) instanceof CoreBlockEntity core)
            core.onNeighborSignal(powered);
    }

    /** An armed core cannot be mined; switch it off first. */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (state.getValue(ARMED))
            return 0.0F;
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(ROTATION, state.getValue(ROTATION).getRotated(rotation))
                .setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        // Templates mirror before rotating, so a mirror always meets an un-rotated state.
        return state.setValue(MIRROR, mirror).setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    /** Like an enchanting table: glyphs drift into the crystal of an armed core. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ARMED))
            return;
        for (int i = 0; i < 2; i++) {
            if (random.nextInt(3) != 0)
                continue;
            level.addParticle(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.35, pos.getZ() + 0.5,
                    (random.nextFloat() - 0.5) * 2.4, random.nextFloat() * 1.2 - 0.6, (random.nextFloat() - 0.5) * 2.4);
        }
    }
}
