package app.pinya.zonelock.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import app.pinya.zonelock.ZoneLock;
import app.pinya.zonelock.network.CoreScreenPayload;
import app.pinya.zonelock.network.SealPingPayload;
import app.pinya.zonelock.platform.Services;
import app.pinya.zonelock.registry.ModBlocks;
import app.pinya.zonelock.registry.ModItems;
import app.pinya.zonelock.registry.ModSounds;
import app.pinya.zonelock.registry.ModTags;
import app.pinya.zonelock.zone.Extents;
import app.pinya.zonelock.zone.LockInfo;
import app.pinya.zonelock.zone.Zone;
import app.pinya.zonelock.zone.ZoneBox;
import app.pinya.zonelock.zone.ZoneManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.nbt.CompoundTag;

/**
 * The Amethyst Core. Owns the configuration of its zone and runs the challenge: seals and redstone.
 * The zone itself is mirrored into {@link ZoneManager} so it is enforced even while this chunk is unloaded.
 */
public class CoreBlockEntity extends BlockEntity {
    /**
     * Extent allowed without any focus block, then for each focus tier. The last one fits any vanilla structure
     * (an ancient city reaches about 115 blocks from its core) and any puzzle that still makes sense.
     */
    private static final int[] TIER_EXTENTS = { 4, 8, 16, 32, 64, 128 };
    /** Largest extent of any core, generated ones included. */
    public static final int MAX_EXTENT = TIER_EXTENTS[TIER_EXTENTS.length - 1];

    private CoreConfig config = CoreConfig.DEFAULT;
    private boolean armed;
    /**
     * A shard is on the core but the zone isn't locked yet: it locks once nobody is inside it, whoever placed the shard
     * has been seen outside since, and nobody near it holds a Resonance Wand (still working on it from outside). Until
     * then everything stays as it was while switched off: editable, seals asleep and giving their signal.
     */
    private boolean pending;
    private @Nullable UUID pendingBy;
    private boolean pendingLeft;
    private int pendingNotice;
    private UUID zoneId = UUID.randomUUID();
    private ItemStack focus = ItemStack.EMPTY;
    /** Zone that protected the structure until this core generated; removed once this core publishes its own. */
    private @Nullable UUID replaces;

    private boolean registered;

    public CoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.CORE_ENTITY.get(), pos, state);
        config = fit(config);
    }

    /** Forces the lock that this core's block stands for. */
    private CoreConfig fit(CoreConfig value) {
        return getBlockState().getBlock() instanceof CoreBlock block ? value.withLock(block.lock()) : value;
    }

    // ------------------------------------------------------------------ lifecycle

    public static void serverTick(Level level, BlockPos pos, BlockState state, CoreBlockEntity core) {
        if (!core.registered && level instanceof ServerLevel serverLevel)
            core.register(serverLevel, state);
        if (core.pending && level instanceof ServerLevel serverLevel && level.getGameTime() % 5 == 0)
            core.tickPending(serverLevel);
    }

    /** How far from the zone a player holding the wand keeps it from locking (working on it from outside). */
    private static final int WAND_HOLD = 8;

    /** Locks the zone once nobody is inside, the one who placed the shard has left, and nobody near holds a wand. */
    private void tickPending(ServerLevel level) {
        ZoneBox zone = box();
        ZoneBox near = zone.inflate(WAND_HOLD);
        boolean inside = false;
        ServerPlayer wand = null;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator())
                continue;
            boolean in = zone.contains(player.blockPosition()) || zone.contains(BlockPos.containing(player.getEyePosition()));
            if (!in && player.getUUID().equals(pendingBy))
                pendingLeft = true;
            if (in)
                inside = true;
            else if (near.contains(player.blockPosition()) && (player.getMainHandItem().is(ModItems.WAND.get())
                    || player.getOffhandItem().is(ModItems.WAND.get())))
                wand = player;
        }
        // Whoever placed the shard and logged out inside it keeps it open until they come back and leave.
        if (inside || pendingBy != null && !pendingLeft)
            return;
        if (wand != null) {
            if (pendingNotice++ % 8 == 0)
                wand.sendOverlayMessage(Component.translatable("message.zonelock.pending_wand", displayName()));
            return;
        }
        arm();
    }

    public boolean isPending() {
        return pending;
    }

    /** First tick after loading or placing: apply the structure transform and publish the zone. */
    private void register(ServerLevel level, BlockState state) {
        registered = true;
        Rotation rotation = state.getValue(CoreBlock.ROTATION);
        Mirror mirror = state.getValue(CoreBlock.MIRROR);
        if (rotation != Rotation.NONE || mirror != Mirror.NONE) {
            config = config.withExtents(config.extents().mirror(mirror).rotate(rotation));
            level.setBlock(worldPosition,
                    state.setValue(CoreBlock.ROTATION, Rotation.NONE).setValue(CoreBlock.MIRROR, Mirror.NONE),
                    Block.UPDATE_CLIENTS);
            setChanged();
        }
        // A core copied from a structure template carries the ids of the original: give this copy its own.
        ZoneManager manager = ZoneManager.get(level);
        if (manager.zone(zoneId).filter(z -> !z.anchor().equals(worldPosition)).isPresent()) {
            zoneId = UUID.randomUUID();
            setChanged();
        }
        if (state.getValue(CoreBlock.ARMED) != armed)
            level.setBlock(worldPosition, getBlockState().setValue(CoreBlock.ARMED, armed), Block.UPDATE_ALL);
        changed();
        if (replaces != null) {
            manager.remove(replaces);
            replaces = null;
            setChanged();
        }
    }

    /**
     * Configures a core generated inside a vanilla structure: armed, with a zone over the whole structure (worldgen
     * sets it whatever the focus; once beaten it is an ordinary core). Worldgen thread, before it is ever ticked.
     *
     * @param replaces the structure's provisional zone, dropped when this core registers its own
     */
    public void setupForStructure(CoreConfig value, UUID replaces) {
        config = fit(value);
        armed = true;
        this.replaces = replaces;
        setChanged();
    }

    /** Called when the block is removed for good. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel serverLevel) {
            Optional<Zone> zone = ZoneManager.get(serverLevel).zone(zoneId);
            ZoneManager.get(serverLevel).remove(zoneId);
            zone.ifPresent(z -> updateSeals(serverLevel, z));
            if (!focus.isEmpty())
                Block.popResource(level, pos, focus.copy());
        }
    }

    public void onPlacedBy(@Nullable Player player) {
        armed = false;
        registered = false;
        // Zones never cross: a new core's zone stops wherever it would run into another one.
        config = config.withExtents(fitAmongZones(config.extents().clamp(maxExtent())));
        setChanged();
    }

    /** The largest extents up to {@code target} that don't cross another zone, grown side by side from nothing. */
    private Extents fitAmongZones(Extents target) {
        Extents e = new Extents(0, 0, 0, 0, 0, 0);
        boolean grew = true;
        while (grew) {
            grew = false;
            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values())
                if (e.get(dir) < target.get(dir)) {
                    Extents next = e.with(dir, e.get(dir) + 1);
                    if (!overlapsOther(ZoneBox.around(worldPosition, next))) {
                        e = next;
                        grew = true;
                    }
                }
        }
        return e;
    }

    /**
     * Whether {@code box} would cross another core's zone (armed or not). Zones may touch, never share a block.
     */
    public boolean overlapsOther(ZoneBox box) {
        if (!(level instanceof ServerLevel serverLevel))
            return false;
        for (Zone zone : ZoneManager.get(serverLevel).index().all())
            if (!zone.id().equals(zoneId) && zone.box().intersects(box))
                return true;
        return false;
    }

    /** What happened to a wall the wand pushed. */
    public enum Push {
        MOVED, LIMIT, BLOCKED
    }

    /**
     * Moves one wall of the zone a block: {@code planeStep} +1 moves its plane towards growing coordinates, -1 the
     * other way (so it grows or shrinks depending on the wall). Stops at the core, at the focus limit and at other zones.
     */
    public Push push(net.minecraft.core.Direction face, int planeStep) {
        int outward = planeStep * face.getAxisDirection().getStep();
        Extents e = config.extents();
        int value = e.get(face) + outward;
        // Growing stops at what the focus allows; shrinking always works (even a zone bigger than its focus allows).
        if (value < 0 || outward > 0 && value > maxExtent())
            return Push.LIMIT;
        Extents next = e.with(face, value);
        // Only growing can run into another zone; shrinking is always allowed (it only takes the zones apart).
        if (outward > 0 && overlapsOther(ZoneBox.around(worldPosition, next)))
            return Push.BLOCKED;
        setExtents(next);
        return Push.MOVED;
    }

    // ------------------------------------------------------------------ state

    public CoreConfig config() {
        return config;
    }

    public boolean isArmed() {
        return armed;
    }

    public UUID zoneId() {
        return zoneId;
    }

    /** Biggest extent the focus in the core allows, for everyone (map makers put a focus in too). */
    public int maxExtent() {
        for (int tier = ModTags.FOCUS_TIERS.length - 1; tier >= 0; tier--)
            if (focus.is(ModTags.FOCUS_TIERS[tier]))
                return TIER_EXTENTS[tier + 1];
        return TIER_EXTENTS[0];
    }

    public ZoneBox box() {
        return ZoneBox.around(worldPosition, config.extents());
    }

    public Zone toZone() {
        LockInfo lock = new LockInfo(config.seals(), config.redstone(), 0, 0);
        return new Zone(zoneId, worldPosition, box(), armed, config.name(), config.rules(), lock, pending);
    }

    private Optional<Zone> publish() {
        if (!(level instanceof ServerLevel serverLevel))
            return Optional.empty();
        return Optional.of(ZoneManager.get(serverLevel).put(toZone()));
    }

    private void changed() {
        setChanged();
        if (level != null)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        publish();
    }

    /** Current lock state including live seal counters. */
    public LockInfo lockInfo() {
        if (level instanceof ServerLevel serverLevel)
            return ZoneManager.get(serverLevel).zone(zoneId).map(Zone::lock).orElse(toZone().lock());
        return toZone().lock();
    }

    // ------------------------------------------------------------------ permissions

    public static boolean isMapMaker(Player player) {
        return player.isCreative() && (player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)
                || player.level().getServer() != null && player.level().getServer().isSingleplayer());
    }

    /**
     * Whether the player may change this core. Cores belong to nobody: a switched-off core can be set up by anyone,
     * an armed core is a challenge for everyone. Creative map makers are the only exception, and the map lock keeps
     * everyone else out even while it is off.
     */
    public boolean canEdit(Player player) {
        if (isMapMaker(player))
            return true;
        return !armed && !config.configLocked();
    }

    /**
     * A switched-off core under the map lock does not react to anyone but map makers, as if it were any other block.
     * Both sides: the client cannot see operator rights, so it lets every creative player try.
     */
    public boolean ignores(Player player) {
        if (armed || !config.configLocked())
            return false;
        return level != null && level.isClientSide() ? !player.isCreative() : !isMapMaker(player);
    }

    /** May switch an armed core off without beating it: creative map makers only. */
    public boolean canDisarm(Player player) {
        return isMapMaker(player);
    }

    // ------------------------------------------------------------------ interaction

    /** Right click with an empty hand (or an item that did nothing). */
    public void use(ServerPlayer player) {
        // Changed your mind before leaving: sneak and use takes the shard back off.
        if (pending && player.isShiftKeyDown() && canEdit(player)) {
            cancelPending(player);
            return;
        }
        if (canEdit(player) && !player.isShiftKeyDown()) {
            openScreen(player);
            return;
        }
        if (armed)
            attemptUnlock(player);
    }

    /** A challenger tries to switch the core off. Explains what is missing otherwise. */
    public void attemptUnlock(@Nullable ServerPlayer player) {
        if (!armed || !(level instanceof ServerLevel serverLevel))
            return;
        LockInfo lock = lockInfo();
        if (!lock.sealsDone()) {
            if (player != null) {
                pingSeals(serverLevel, player);
                deny(player, Component.translatable("message.zonelock.seals_left", lock.sealsLit()));
            }
            return;
        }
        if (config.redstone() && !serverLevel.hasNeighborSignal(worldPosition)) {
            if (player != null)
                deny(player, Component.translatable("message.zonelock.redstone_needed"));
            return;
        }
        unlock(serverLevel, player);
    }

    /** Redstone changed next to the core. */
    public void onNeighborSignal(boolean powered) {
        if (powered && armed && config.redstone())
            attemptUnlock(null);
    }

    private void unlock(ServerLevel level, @Nullable ServerPlayer by) {
        armed = false;
        level.setBlock(worldPosition, getBlockState().setValue(CoreBlock.ARMED, false), Block.UPDATE_ALL);
        changed();
        updateSeals(level, toZone());
        play(ModSounds.CORE_UNLOCK.get(), 1.0F);
        // The crystal shatters and is gone: amethyst shards like a cluster breaking, a few sparks rising.
        double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + 1.3, z = worldPosition.getZ() + 0.5;
        level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK,
                net.minecraft.world.level.block.Blocks.AMETHYST_CLUSTER.defaultBlockState()), x, y, z, 40, 0.25, 0.3,
                0.25, 0.15);
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 10, 0.2, 0.3, 0.2, 0.05);
        Component title = Component.translatable("message.zonelock.unlocked", displayName())
                .withStyle(ChatFormatting.LIGHT_PURPLE);
        for (ServerPlayer player : level.players())
            if (box().inflate(8).contains(player.blockPosition()))
                player.sendOverlayMessage(title);
        ZoneLock.LOG.debug("Core at {} unlocked by {}", worldPosition, by == null ? "redstone" : by.getName().getString());
    }

    private void pingSeals(ServerLevel level, ServerPlayer player) {
        ZoneManager manager = ZoneManager.get(level);
        Optional<Zone> zone = manager.zone(zoneId);
        if (zone.isEmpty())
            return;
        List<BlockPos> lit = new ArrayList<>();
        for (Map.Entry<BlockPos, Boolean> seal : manager.sealsIn(zone.get()).entrySet())
            if (seal.getValue())
                lit.add(seal.getKey());
        Services.NETWORK.sendToPlayer(player, new SealPingPayload(worldPosition, lit));
        for (BlockPos pos : lit)
            level.playSound(null, pos, ModSounds.SEAL_PING.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    // ------------------------------------------------------------------ editing

    public void arm() {
        if (armed || !(level instanceof ServerLevel serverLevel))
            return;
        pending = false;
        pendingBy = null;
        // A challenge always starts complete: every seal of the zone burns again.
        for (BlockPos seal : ZoneManager.get(serverLevel).sealsIn(toZone()).keySet())
            SealBlock.relight(serverLevel, seal);
        // A zone bigger than the focus allows (a beaten structure core armed again where it stands) shrinks to fit.
        config = config.withExtents(config.extents().clamp(maxExtent()));
        armed = true;
        serverLevel.setBlock(worldPosition, getBlockState().setValue(CoreBlock.ARMED, true)
                .setValue(CoreBlock.PENDING, false), Block.UPDATE_ALL);
        play(ModSounds.CORE_ARM.get(), 1.0F);
        changed();
        updateSeals(serverLevel, toZone());
    }

    /**
     * Seals give a redstone signal whenever they aren't holding anything shut, so when the zone is armed, switched off
     * or removed, the redstone round each seal is told to look again.
     */
    private static void updateSeals(ServerLevel level, Zone zone) {
        for (BlockPos seal : ZoneManager.get(level).sealsIn(zone).keySet())
            if (level.isLoaded(seal))
                level.updateNeighborsAt(seal, level.getBlockState(seal).getBlock());
    }

    public void disarm() {
        if (pending) {
            cancelPending(null);
            return;
        }
        if (!armed || !(level instanceof ServerLevel serverLevel))
            return;
        armed = false;
        serverLevel.setBlock(worldPosition, getBlockState().setValue(CoreBlock.ARMED, false), Block.UPDATE_ALL);
        play(ModSounds.CORE_DISARM.get(), 1.0F);
        // Switched off by hand, the crystal pops off whole: it is needed to arm the core again.
        Block.popResourceFromFace(serverLevel, worldPosition, net.minecraft.core.Direction.UP,
                new ItemStack(net.minecraft.world.item.Items.AMETHYST_SHARD));
        changed();
        updateSeals(serverLevel, toZone());
    }

    /**
     * Placing an amethyst shard on a switched-off core puts its crystal back; the zone locks (every seal burns again)
     * as soon as everyone has left it, so nobody is ever shut in by arming it from inside.
     */
    public boolean placeCrystal(ServerPlayer player, ItemStack stack) {
        if (armed || pending || !canEdit(player))
            return false;
        if (!player.hasInfiniteMaterials())
            stack.shrink(1);
        pending = true;
        pendingBy = player.getUUID();
        pendingLeft = false;
        pendingNotice = 0;
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.setBlock(worldPosition, getBlockState().setValue(CoreBlock.PENDING, true), Block.UPDATE_ALL);
            play(ModSounds.CORE_ARM.get(), 0.8F);
            changed();
            player.sendOverlayMessage(Component.translatable("message.zonelock.pending", displayName()));
            tickPending(serverLevel);
        }
        return true;
    }

    /** The shard comes back off a core waiting to lock; the zone stays as it was. */
    public void cancelPending(@Nullable ServerPlayer by) {
        if (!pending || !(level instanceof ServerLevel serverLevel))
            return;
        pending = false;
        pendingBy = null;
        serverLevel.setBlock(worldPosition, getBlockState().setValue(CoreBlock.PENDING, false), Block.UPDATE_ALL);
        play(ModSounds.CORE_DISARM.get(), 1.0F);
        Block.popResourceFromFace(serverLevel, worldPosition, net.minecraft.core.Direction.UP,
                new ItemStack(net.minecraft.world.item.Items.AMETHYST_SHARD));
        changed();
        if (by != null)
            by.sendOverlayMessage(Component.translatable("message.zonelock.pending_cancelled", displayName()));
    }

    public void setConfig(CoreConfig value) {
        CoreConfig clamped = fit(value).withExtents(limitGrowth(config.extents(), value.extents(), maxExtent()))
                .withName(value.name().length() > CoreConfig.MAX_NAME ? value.name().substring(0, CoreConfig.MAX_NAME)
                        : value.name());
        if (grows(config.extents(), clamped.extents())
                && overlapsOther(ZoneBox.around(worldPosition, clamped.extents())))
            clamped = clamped.withExtents(config.extents()); // zones never cross (shrinking is always fine)
        if (!clamped.equals(config)) {
            config = clamped;
            changed();
        }
    }

    /**
     * The sides of {@code next} that grew past {@code max} stay where they were; the others are taken as they are, so
     * shrinking one side of a zone bigger than its focus allows doesn't cut every other side down at once.
     */
    private static Extents limitGrowth(Extents old, Extents next, int max) {
        Extents out = next;
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
            int n = Math.max(0, next.get(dir));
            if (n > max && n > old.get(dir))
                n = Math.max(old.get(dir), max);
            out = out.with(dir, n);
        }
        return out;
    }

    private static boolean grows(Extents old, Extents next) {
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values())
            if (next.get(dir) > old.get(dir))
                return true;
        return false;
    }

    public void setExtents(Extents extents) {
        setConfig(config.withExtents(extents));
    }

    /** The focus block in the core's screen (see {@link app.pinya.zonelock.menu.CoreMenu}). */
    public ItemStack focus() {
        return focus;
    }

    /** Puts a focus block in (from the core's screen): the zone may reach as far as it allows. */
    public void setFocus(ItemStack stack) {
        focus = stack;
        config = config.withExtents(config.extents().clamp(maxExtent()));
        changed();
    }

    public ItemStack takeFocus() {
        ItemStack old = focus;
        focus = ItemStack.EMPTY;
        config = config.withExtents(config.extents().clamp(maxExtent()));
        changed();
        return old;
    }

    /** Opens the core's screen: its state first, then the menu (focus slot and inventory). */
    public void openScreen(ServerPlayer player) {
        refreshScreen(player);
        Services.PLATFORM.openMenu(player, new net.minecraft.world.SimpleMenuProvider(
                (id, inventory, opener) -> new app.pinya.zonelock.menu.CoreMenu(id, inventory, worldPosition,
                        new app.pinya.zonelock.menu.FocusContainer(this, opener)),
                displayName()), worldPosition);
    }

    /** Sends the fresh state to a player whose screen of this core may be open. */
    public void refreshScreen(ServerPlayer player) {
        Services.NETWORK.sendToPlayer(player, new CoreScreenPayload(worldPosition, config, armed, canEdit(player),
                isMapMaker(player), focus.copy(), maxExtent(), lockInfo()));
    }

    // ------------------------------------------------------------------ helpers

    public Component displayName() {
        return config.name().isBlank() ? getBlockState().getBlock().getName()
                : Component.literal(config.name());
    }

    public static boolean isFocus(ItemStack stack) {
        for (var tag : ModTags.FOCUS_TIERS)
            if (stack.is(tag))
                return true;
        return false;
    }

    private void deny(ServerPlayer player, Component message) {
        player.sendOverlayMessage(message);
        level.playSound(null, worldPosition, ModSounds.CORE_DENY.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
    }

    private void play(SoundEvent sound, float pitch) {
        if (level != null)
            level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, 1.0F, pitch);
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("config", CoreConfig.CODEC, config);
        output.putBoolean("armed", armed);
        if (pending) {
            output.putBoolean("pending", true);
            output.storeNullable("pending_by", UUIDUtil.CODEC, pendingBy);
            output.putBoolean("pending_left", pendingLeft);
        }
        output.store("zone_id", UUIDUtil.CODEC, zoneId);
        output.storeNullable("replaces", UUIDUtil.CODEC, replaces);
        if (!focus.isEmpty())
            output.store("focus", ItemStack.CODEC, focus);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        config = fit(input.read("config", CoreConfig.CODEC).orElse(CoreConfig.DEFAULT));
        armed = input.getBooleanOr("armed", false);
        pending = !armed && input.getBooleanOr("pending", false);
        pendingBy = input.read("pending_by", UUIDUtil.CODEC).orElse(null);
        pendingLeft = input.getBooleanOr("pending_left", false);
        zoneId = input.read("zone_id", UUIDUtil.CODEC).orElseGet(UUID::randomUUID);
        replaces = input.read("replaces", UUIDUtil.CODEC).orElse(null);
        focus = input.read("focus", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        registered = false;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }
}
