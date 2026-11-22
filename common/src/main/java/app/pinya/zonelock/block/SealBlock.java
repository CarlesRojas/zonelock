package app.pinya.zonelock.block;

import org.jspecify.annotations.Nullable;

import app.pinya.zonelock.registry.ModBlocks;
import app.pinya.zonelock.registry.ModSounds;
import app.pinya.zonelock.zone.Zone;
import app.pinya.zonelock.zone.ZoneManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A seal joins the lock of every core whose zone contains it: while it burns, those cores cannot be switched off.
 * The Amethyst Seal is put out by touching it; the Redstone Seal by a redstone signal. They only react while an
 * armed core guards them, so building and wiring a dungeon never puts them out by accident.
 * <p>
 * A seal gives a redstone signal (15, like a redstone block) whenever it isn't holding anything shut: once it is put
 * out, and while its zone isn't armed. So a door wired to a seal stands open while the dungeon is being built, shuts
 * when the core is armed, and opens again when a challenger puts the seal out: one room leads to the next.
 */
public class SealBlock extends BaseEntityBlock {
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    /** Receives a redstone signal: the Redstone Seal's redstone glows, like a repeater's torch. */
    public static final BooleanProperty POWERED = net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED;

    /** What you bump into: the base, the neck and the cap, as the model draws them. */
    private static final VoxelShape BODY = net.minecraft.world.phys.shapes.Shapes.or(Block.box(2, 0, 2, 14, 3, 14),
            Block.box(5, 3, 5, 11, 5, 11), Block.box(4, 5, 4, 12, 7, 12));
    /** What you aim at: the body and the crystal standing on it, which is what you touch to put the seal out. */
    private static final VoxelShape SHAPE = net.minecraft.world.phys.shapes.Shapes.or(BODY, Block.box(5, 7, 5, 11, 15, 11));

    private final boolean redstone;

    public SealBlock(Properties properties, boolean redstone) {
        super(properties);
        this.redstone = redstone;
        registerDefaultState(stateDefinition.any().setValue(LIT, true).setValue(POWERED, false));
    }

    /** Put out by redstone (Redstone Seal) rather than by touch (Amethyst Seal). */
    public boolean isRedstone() {
        return redstone;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState().setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SealBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        return level instanceof ServerLevel ? createTickerHelper(type, ModBlocks.SEAL_ENTITY.get(),
                SealBlockEntity::serverTick) : null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.core.Direction direction) {
        return holdsShut(state, level, pos) ? 0 : 15;
    }

    /** Lit and guarded by an armed core: then, and only then, the seal gives no signal. */
    private static boolean holdsShut(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(LIT) && level instanceof ServerLevel serverLevel && isGuarded(serverLevel, pos);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return BODY;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer))
            return InteractionResult.SUCCESS;
        if (!isGuarded(serverLevel, pos))
            return InteractionResult.PASS; // nothing to do until a core arms the zone
        if (!state.getValue(LIT))
            serverPlayer.sendOverlayMessage(Component.translatable("message.zonelock.seal_out"));
        else if (!redstone)
            extinguish(serverLevel, pos, state);
        else
            serverPlayer.sendOverlayMessage(Component.translatable("message.zonelock.seal_redstone"));
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
            @Nullable Orientation orientation, boolean movedByPiston) {
        if (!(level instanceof ServerLevel serverLevel))
            return;
        boolean powered = level.hasNeighborSignal(pos);
        if (state.getValue(POWERED) != powered) {
            state = state.setValue(POWERED, powered);
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        }
        if (redstone && powered && state.getValue(LIT) && isGuarded(serverLevel, pos))
            extinguish(serverLevel, pos, state);
    }

    /** Inside the zone of an armed core. */
    public static boolean isGuarded(ServerLevel level, BlockPos pos) {
        for (Zone zone : ZoneManager.get(level).index().at(pos))
            if (zone.active())
                return true;
        return false;
    }

    public static void extinguish(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_ALL);
        ZoneManager manager = ZoneManager.get(level);
        manager.trackSeal(pos, false);
        level.playSound(null, pos, ModSounds.SEAL_EXTINGUISH.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        // Like a candle being blown out, plus the crystal cracking.
        level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 8, 0.1, 0.15,
                0.1, 0.01);
        level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK,
                net.minecraft.world.level.block.Blocks.AMETHYST_CLUSTER.defaultBlockState()), pos.getX() + 0.5,
                pos.getY() + 0.8, pos.getZ() + 0.5, 12, 0.15, 0.2, 0.15, 0.05);

        for (Zone zone : manager.index().at(pos)) {
            if (!zone.active() || !zone.lock().seals())
                continue;
            Zone updated = manager.zone(zone.id()).orElse(zone);
            int left = updated.lock().sealsLit();
            Component message = left == 0 ? Component.translatable("message.zonelock.seals_done")
                    : Component.translatable("message.zonelock.seal_extinguished", left);
            for (ServerPlayer player : level.players())
                if (zone.box().inflate(4).contains(player.blockPosition()))
                    player.sendOverlayMessage(message);
            if (left == 0 && level.getBlockEntity(zone.anchor()) instanceof CoreBlockEntity core)
                core.onNeighborSignal(level.hasNeighborSignal(zone.anchor()));
        }
    }

    public static void relight(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof SealBlock))
            return;
        if (!state.getValue(LIT)) {
            level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
            level.playSound(null, pos, ModSounds.SEAL_PING.get(), SoundSource.BLOCKS, 1.0F, 1.2F);
        }
        ZoneManager.get(level).trackSeal(pos, true);
    }

    /** Lit seals draw glyphs into their crystal; Redstone Seals also give off redstone dust, like a torch. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT))
            return;
        if (random.nextInt(3) == 0)
            level.addParticle(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 0.85, pos.getZ() + 0.5,
                    (random.nextFloat() - 0.5) * 1.6, random.nextFloat() * 0.8, (random.nextFloat() - 0.5) * 1.6);
        if (redstone && random.nextInt(4) == 0)
            level.addParticle(DustParticleOptions.REDSTONE, pos.getX() + 0.3 + random.nextDouble() * 0.4,
                    pos.getY() + 0.25, pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0, 0);
    }
}
