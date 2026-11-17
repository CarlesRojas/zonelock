package app.pinya.zonelock.client.render;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import app.pinya.zonelock.block.CoreBlockEntity;
import app.pinya.zonelock.registry.ModItems;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import net.minecraft.world.phys.Vec3;

/**
 * The crystal of an armed core: a 3D amethyst cluster (vanilla texture, like the seals) hovering over the socket, turning slowly and glowing. When the
 * core is armed it rises out of the socket; once the core is switched off it is gone (it shattered, or popped off).
 */
public class CoreRenderer implements BlockEntityRenderer<CoreBlockEntity, CoreRenderer.State> {
    public static class State extends BlockEntityRenderState {
        final ItemStackRenderState crystal = new ItemStackRenderState();
        /** 0 = resting in the socket, 1 = fully awake. */
        float lift;
        float spin;
        float time;
    }

    /** Per-core animation: current lift, accumulated spin, last update time. */
    private static final class Motion {
        float lift = -1;
        float spin;
        long lastNanos = System.nanoTime();
    }

    private final ItemModelResolver items;
    private final Map<Long, Motion> motions = new HashMap<>();
    /** Created lazily: item components are not bound yet when renderers are constructed. */
    private ItemStack shard;

    public CoreRenderer(BlockEntityRendererProvider.Context context) {
        this.items = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CoreBlockEntity core, State state, float partialTicks, Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(core, state, partialTicks, cameraPosition, breakProgress);
        if (shard == null)
            shard = new ItemStack(ModItems.CORE_CRYSTAL.get());
        Motion motion = motions.computeIfAbsent(core.getBlockPos().asLong(), k -> new Motion());
        long now = System.nanoTime();
        float dt = Math.min(0.1F, (now - motion.lastNanos) / 1.0E9F);
        motion.lastNanos = now;
        float target = core.isArmed() ? 1.0F : 0.0F;
        if (motion.lift < 0)
            motion.lift = target; // first frame: no animation when the chunk loads
        if (!core.isArmed())
            motion.lift = 0; // gone at once: it shattered or popped off
        motion.lift += (target - motion.lift) * Math.min(1.0F, dt * 2.5F);
        motion.spin = (motion.spin + dt * 40.0F * motion.lift) % 360.0F;

        state.lift = motion.lift;
        state.spin = motion.spin;
        state.time = core.getLevel() == null ? 0 : (core.getLevel().getGameTime() % 72000L) + partialTicks;
        int seed = (int) core.getBlockPos().asLong();
        if (core.isArmed())
            items.updateForTopItem(state.crystal, shard, ItemDisplayContext.FIXED, core.getLevel(), null, seed);
        else
            state.crystal.clear();
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.crystal.isEmpty())
            return;
        float lift = state.lift;
        float bob = Mth.sin(state.time * 0.07F) * 0.05F * lift;
        pose.pushPose();
        pose.translate(0.5F, Mth.lerp(lift, 0.9F, 1.35F) + bob, 0.5F);
        pose.rotateDegrees(Axis.YP, 45.0F + state.spin);
        float scale = Mth.lerp(lift, 0.3F, 0.7F);
        pose.scale(scale, scale, scale);
        state.crystal.submit(pose, collector, 0xF000F0, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
