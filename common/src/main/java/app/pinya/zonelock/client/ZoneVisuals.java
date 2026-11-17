package app.pinya.zonelock.client;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import app.pinya.zonelock.client.render.BlockRipples;
import app.pinya.zonelock.client.render.ZoneForcefield;
import app.pinya.zonelock.item.ResonanceWandItem;
import app.pinya.zonelock.registry.ModItems;
import app.pinya.zonelock.registry.ModSounds;
import app.pinya.zonelock.zone.ClientZoneCache;
import app.pinya.zonelock.zone.Rule;
import app.pinya.zonelock.zone.Zone;
import app.pinya.zonelock.zone.WandTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * World feedback in the language of vanilla:
 * <ul>
 * <li>The zone border is drawn like the world border (see {@link ZoneForcefield}): only while holding a Resonance
 * Wand.</li>
 * <li>A block that refuses to change ripples where it was hit, over its own shape (see {@link BlockRipples}), and
 * chips a few amethyst shards, with a low amethyst chime.</li>
 * <li>A locked core sends pulses of energy through the air to its remaining seals.</li>
 * <li>With the wand in hand, the wall it points at lights up (it moves when used) and a tutorial toast explains it.</li>
 * </ul>
 */
public final class ZoneVisuals {
    private static final double REVEAL_RANGE = 96.0;
    /** How wide the patch of an armed zone's wall is that shows without the wand (it shows from 1.4 times as far). */
    private static final double NEAR_RADIUS = 6.0;

    private static @Nullable TutorialToast wandToast;

    private ZoneVisuals() {
    }

    // ------------------------------------------------------------------ events

    /**
     * A block (or decoration) refused to change: rings ripple over it, with the zone's sound and a few chips. Holding
     * the attack key keeps sending hits; sound and chips come only with each new ripple, so all three keep one beat.
     */
    public static void barrierHit(BlockPos pos, Rule rule) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null)
            return;
        boolean started;
        net.minecraft.world.entity.Entity decoration = rule == Rule.DECORATIONS ? decoration(mc, pos) : null;
        if (decoration != null) {
            Vec3 at = mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit
                    && hit.getEntity() == decoration ? hit.getLocation() : decoration.getBoundingBox().getCenter();
            started = BlockRipples.startOnBox(pos, decoration.getBoundingBox(), at, mc.level.getGameTime());
        } else {
            VoxelShape shape = BlockRipples.surface(mc.level.getBlockState(pos), mc.level, pos);
            started = BlockRipples.start(mc.level, pos, hitPoint(mc, pos, shape), mc.level.getGameTime());
        }
        if (!started)
            return;
        mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.BARRIER.get(),
                0.9F + mc.level.getRandom().nextFloat() * 0.25F, 0.6F));
        chips(mc.level, pos);
    }

    public static void pingSeals(BlockPos core, List<BlockPos> seals) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null)
            EnergyPulses.start(level, core, seals);
    }

    /** The item frame, painting or armor stand the player just hit at {@code pos}: the one under the crosshair. */
    private static net.minecraft.world.entity.@Nullable Entity decoration(Minecraft mc, BlockPos pos) {
        if (mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit
                && hit.getEntity().blockPosition().equals(pos))
            return hit.getEntity();
        net.minecraft.world.entity.Entity best = null;
        for (net.minecraft.world.entity.Entity entity : mc.level.getEntities((net.minecraft.world.entity.Entity) null,
                new AABB(pos).inflate(0.5), e -> e.blockPosition().equals(pos)
                        && (e instanceof net.minecraft.world.entity.decoration.HangingEntity
                                || e instanceof net.minecraft.world.entity.decoration.ArmorStand)))
            if (best == null || entity.distanceToSqr(mc.player) < best.distanceToSqr(mc.player))
                best = entity;
        return best;
    }

    /** A few amethyst shards chipping off the face of a block that refused to break. */
    private static void chips(ClientLevel level, BlockPos pos) {
        RandomSource random = level.getRandom();
        BlockParticleOption shard = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_BLOCK.defaultBlockState());
        for (int i = 0; i < 8; i++)
            level.addParticle(shard, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
                    pos.getZ() + random.nextDouble(), 0, 0, 0);
    }

    // ------------------------------------------------------------------ per tick

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null) {
            ZoneForcefield.setPatches(List.of());
            wandToast(mc, false);
            return;
        }
        Vec3 eye = player.getEyePosition();

        List<ZoneForcefield.Patch> patches = new ArrayList<>();
        boolean reveal = holding(player, ModItems.WAND.get());
        // With the wand: armed zones show moving runes, switched-off ones still ones, and the wall the wand points at
        // (of a zone that can be reshaped) lights up.
        if (reveal) {
            WandTarget target = ResonanceWandItem.clientTarget(player, ResonanceWandItem.blockDistance(player))
                    .orElse(null);
            for (Zone zone : ClientZoneCache.INDEX.near(eye, REVEAL_RANGE))
                patches.add(ZoneForcefield.Patch.whole(zone.box(), eye, zone.active(),
                            target != null && target.zone().id().equals(zone.id()) ? target.face() : null));
        } else if (ZoneLockConfig.nearbyWalls) {
            // Without the wand: a patch of an armed zone's wall shows around you as you come close to it.
            for (Zone zone : ClientZoneCache.INDEX.near(eye, NEAR_RADIUS * ZoneForcefield.NEAR_REACH))
                if (zone.active())
                    patches.add(ZoneForcefield.Patch.near(zone.box(), eye, 1.0F, (float) NEAR_RADIUS));
        }
        ZoneForcefield.setPatches(patches);
        wandToast(mc, reveal);

        EnergyPulses.tick(level);
    }

    /** Like vanilla's tutorial hints: while the wand is in hand, a toast says how to use it. */
    private static void wandToast(Minecraft mc, boolean holding) {
        if (holding && wandToast == null) {
            wandToast = new TutorialToast(mc.font, TutorialToast.Icons.RIGHT_CLICK,
                    Component.translatable("tutorial.zonelock.wand.title"),
                    Component.translatable("tutorial.zonelock.wand.description"), false);
            mc.gui.toastManager().addToast(wandToast);
        } else if (!holding && wandToast != null) {
            wandToast.hide();
            wandToast = null;
        }
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Where the player hit the block: the crosshair's point when it is on that block, otherwise where their view
     * would meet the block's shape (or its middle).
     */
    private static Vec3 hitPoint(Minecraft mc, BlockPos pos, VoxelShape shape) {
        if (mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                && hit.getBlockPos().equals(pos))
            return hit.getLocation();
        Vec3 eye = mc.player.getEyePosition();
        BlockHitResult clip = (shape.isEmpty() ? Shapes.block() : shape).clip(eye,
                eye.add(Vec3.atCenterOf(pos).subtract(eye).normalize().scale(8)), pos);
        return clip != null ? clip.getLocation() : Vec3.atCenterOf(pos);
    }

    private static boolean holding(LocalPlayer player, Item item) {
        return player.getMainHandItem().is(item) || player.getOffhandItem().is(item);
    }
}
