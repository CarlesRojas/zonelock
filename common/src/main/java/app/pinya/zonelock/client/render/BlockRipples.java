package app.pinya.zonelock.client.render;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Ripples of amethyst light on a block that refused to change: square rings born where the player hit it, running over the
 * block's real surface (every box of its shape, so a pedestal ripples like a pedestal, not like the cube around it)
 * and fading as they spread. Where the surface carries on into the blocks around (a wall, a floor, a corner) the rings
 * carry on too, dimming step by step until they are gone half a block out; a surface that doesn't touch the hit one
 * (a torch on a wall, a fence post apart) stays dark. Pixel art: every texel of the surface is painted flat in one of
 * vanilla amethyst's tones, a hair above each face.
 * <p>
 * Blocks you walk through (crops, flowers, torches, rails...) have no solid surface: their rings run over the quads of
 * their model instead, texel by texel, skipping the transparent ones, so wheat ripples like wheat, not like the box
 * around it. Decorations (item frames, paintings, armor stands) ripple over their own box.
 */
public final class BlockRipples {
    /** Vanilla amethyst's own tones: the bright ring and its highlights, the faint ring and its darker grain. */
    private static final int BRIGHT = 0xA678F1, BRIGHT_GLINT = 0xC890F0, FAINT = 0x7A5BB5, FAINT_GRAIN = 0x8D6ACC;
    /** Ticks the rings last. */
    private static final float DURATION = 22;
    /** Blocks per tick the rings travel. */
    private static final double SPEED = 0.065;
    private static final int RINGS = 2;
    /** How far past the hit block the rings carry on, fading. */
    private static final double SPREAD = 0.5;
    /** Distance between two rings, and half the width of one. */
    private static final double SPACING = 0.38;
    private static final double WIDTH = 0.06;
    /** Faces are cut into texel-sized cells, each lit flat in one of a few steps: pixel art, no gradients. */
    private static final double CELL = 1.0 / 16.0;
    private static final double OUTSET = 0.003;
    private static final double EPSILON = 1.0E-5;
    /** Holding the attack key on a block starts new rings there at most this often, in ticks. */
    private static final long REPEAT = 20;
    private static final int MAX = 8;

    /** One face of one box of a block's shape, in world coordinates. */
    private record Face(Direction side, AABB box) {
        double plane() {
            return side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? box.max(side.getAxis())
                    : box.min(side.getAxis());
        }
    }

    /**
     * One quad of a block's model, in world coordinates, corners in order; {@code u}/{@code v} are where each corner
     * falls on its sprite (0..1), to skip the texels the texture leaves transparent.
     */
    private record Quad(Vector3f[] corners, Direction side, @Nullable TextureAtlasSprite sprite, float[] u, float[] v) {
    }

    /**
     * @param pos   the block (or decoration) that was hit
     * @param home  full brightness inside it, fading over the next block out
     * @param faces the surface the rings may run over: the hit block's and what touches it
     * @param quads model quads of a block with no solid surface (the rings run over these instead)
     * @param start game time the rings were born
     */
    private record Ripple(BlockPos pos, AABB home, List<Face> faces, List<Quad> quads, Vec3 origin, long start) {
    }

    private static final List<Ripple> RIPPLES = new ArrayList<>();

    private BlockRipples() {
    }

    /**
     * The surface the rings run over: what is both seen and solid. Fences and walls collide 1.5 blocks high but only
     * look 1 high; a seal's selection box takes in its crystal, its collision only the stone. Blocks with no collision
     * (torches, flowers) use what is seen.
     */
    public static VoxelShape surface(BlockState state, BlockGetter level, BlockPos pos) {
        VoxelShape seen = state.getShape(level, pos);
        VoxelShape solid = Shapes.join(seen, state.getCollisionShape(level, pos), BooleanOp.AND);
        return !solid.isEmpty() ? solid : seen;
    }

    /**
     * Starts rings at {@code origin} on the block at {@code pos} (a cube when it has no shape), at game time {@code now}.
     * Returns false when that spot rippled too recently (holding the attack key) and nothing started.
     */
    public static boolean start(BlockGetter level, BlockPos pos, Vec3 origin, long now) {
        if (!room(pos, now))
            return false;
        BlockState state = level.getBlockState(pos);
        List<Quad> quads = !state.isAir() && state.getCollisionShape(level, pos).isEmpty() ? modelQuads(state, pos)
                : List.of();
        List<Face> faces = quads.isEmpty() ? surfaceAround(level, pos) : List.of();
        RIPPLES.add(new Ripple(pos.immutable(), new AABB(pos), faces, quads, origin, now));
        return true;
    }

    /** Starts rings on a decoration (item frame, painting, armor stand): over its own box only. Same return as start. */
    public static boolean startOnBox(BlockPos pos, AABB box, Vec3 origin, long now) {
        if (!room(pos, now))
            return false;
        List<Face> faces = new ArrayList<>();
        for (Direction side : Direction.values())
            faces.add(new Face(side, box));
        RIPPLES.add(new Ripple(pos.immutable(), box.inflate(1.0E-3), faces, List.of(), origin, now));
        return true;
    }

    /** Drops finished ripples; false when this spot rippled too recently (holding the attack key). */
    private static boolean room(BlockPos pos, long now) {
        RIPPLES.removeIf(r -> now - r.start > DURATION || now < r.start);
        for (Ripple r : RIPPLES)
            if (r.pos.equals(pos) && now - r.start < REPEAT)
                return false;
        if (RIPPLES.size() >= MAX)
            RIPPLES.removeFirst();
        return true;
    }

    /** The quads the block's model draws, where they are drawn (with the random offset of flowers and grass). */
    private static List<Quad> modelQuads(BlockState state, BlockPos pos) {
        List<BlockStateModelPart> parts = new ArrayList<>();
        Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state)
                .collectParts(RandomSource.create(state.getSeed(pos)), parts);
        Vec3 offset = state.getOffset(pos);
        float ox = (float) (pos.getX() + offset.x), oy = (float) (pos.getY() + offset.y);
        float oz = (float) (pos.getZ() + offset.z);
        List<Quad> out = new ArrayList<>();
        Direction[] culls = { null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST,
                Direction.EAST };
        for (BlockStateModelPart part : parts)
            for (Direction cull : culls)
                for (BakedQuad quad : part.getQuads(cull)) {
                    TextureAtlasSprite sprite = quad.materialInfo().sprite();
                    Vector3f[] corners = new Vector3f[4];
                    float[] u = new float[4], v = new float[4];
                    for (int i = 0; i < 4; i++) {
                        Vector3fc p = quad.position(i);
                        corners[i] = new Vector3f(p.x() + ox, p.y() + oy, p.z() + oz);
                        long uv = quad.packedUV(i);
                        if (sprite != null) {
                            u[i] = (UVPair.unpackU(uv) - sprite.getU0()) / (sprite.getU1() - sprite.getU0());
                            v[i] = (UVPair.unpackV(uv) - sprite.getV0()) / (sprite.getV1() - sprite.getV0());
                        }
                    }
                    out.add(new Quad(corners, quad.direction(), sprite, u, v));
                }
        return out;
    }

    /**
     * The faces of the hit block, and those of the blocks around it (one block out in every direction) that connect
     * to them edge to edge, directly or through one another.
     */
    private static List<Face> surfaceAround(BlockGetter level, BlockPos pos) {
        List<Face> hit = new ArrayList<>();
        List<Face> around = new ArrayList<>();
        for (BlockPos at : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            boolean centre = at.equals(pos);
            VoxelShape shape = surface(level.getBlockState(at), level, at);
            if (shape.isEmpty()) {
                if (!centre)
                    continue;
                shape = Shapes.block();
            }
            for (AABB box : shape.toAabbs())
                for (Direction side : Direction.values())
                    (centre ? hit : around).add(new Face(side, box.move(at)));
        }
        List<Face> faces = new ArrayList<>(hit);
        ArrayDeque<Face> queue = new ArrayDeque<>(hit);
        while (!queue.isEmpty()) {
            Face face = queue.poll();
            for (int i = around.size() - 1; i >= 0; i--)
                if (touches(face, around.get(i))) {
                    Face next = around.remove(i);
                    faces.add(next);
                    queue.add(next);
                }
        }
        return faces;
    }

    /**
     * Whether two faces are one surface: side by side in one plane facing the same way, or meeting along an edge that
     * bounds both (the corner of a block, a wall standing on a floor). A torch's sides end in the middle of the wall's
     * face, not on its edge, so they don't count.
     */
    private static boolean touches(Face a, Face b) {
        Direction.Axis axisA = a.side.getAxis(), axisB = b.side.getAxis();
        if (axisA == axisB) {
            // Side by side, facing the same way. Two faces pressed together (a torch's back on a wall) are not one
            // surface.
            if (a.side != b.side || Math.abs(a.plane() - b.plane()) > EPSILON)
                return false;
            double[] overlap = new double[2];
            int k = 0;
            for (Direction.Axis other : Direction.Axis.values())
                if (other != axisA)
                    overlap[k++] = overlap(a.box, b.box, other);
            return overlap[0] >= -EPSILON && overlap[1] > EPSILON || overlap[1] >= -EPSILON && overlap[0] > EPSILON;
        }
        Direction.Axis third = Direction.Axis.values()[3 - axisA.ordinal() - axisB.ordinal()];
        return onEdge(b.plane(), a.box, axisB) && onEdge(a.plane(), b.box, axisA) && overlap(a.box, b.box, third) > EPSILON;
    }

    private static double overlap(AABB a, AABB b, Direction.Axis axis) {
        return Math.min(a.max(axis), b.max(axis)) - Math.max(a.min(axis), b.min(axis));
    }

    private static boolean onEdge(double value, AABB box, Direction.Axis axis) {
        return Math.abs(value - box.min(axis)) < EPSILON || Math.abs(value - box.max(axis)) < EPSILON;
    }

    /** @param now game time of this frame, with the partial tick */
    public static void submit(SubmitNodeCollector collector, PoseStack pose, Vec3 camera, double now) {
        if (RIPPLES.isEmpty())
            return;
        RIPPLES.removeIf(r -> now - r.start > DURATION || now < r.start - 1);
        if (RIPPLES.isEmpty())
            return;
        List<Ripple> drawn = new ArrayList<>(RIPPLES);
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        collector.submitCustomGeometry(pose, RenderTypes.debugQuads(), (p, buffer) -> {
            for (Ripple ripple : drawn) {
                float age = (float) Mth.clamp((now - ripple.start) / DURATION, 0, 1);
                for (Face face : ripple.faces)
                    drawFace(p, buffer, ripple, ripple.home, face, age);
                for (Quad quad : ripple.quads)
                    drawQuad(p, buffer, ripple, quad, age);
            }
        });
        pose.popPose();
    }

    private static void drawFace(PoseStack.Pose pose, VertexConsumer buffer, Ripple ripple, AABB home, Face face,
            float age) {
        Direction.Axis axis = face.side.getAxis();
        boolean positive = face.side.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        double plane = face.plane() + (positive ? OUTSET : -OUTSET);
        Direction.Axis u = axis == Direction.Axis.X ? Direction.Axis.Y : Direction.Axis.X;
        Direction.Axis v = axis == Direction.Axis.Z ? Direction.Axis.Y : Direction.Axis.Z;
        AABB box = face.box;
        double u0 = box.min(u), u1 = box.max(u), v0 = box.min(v), v1 = box.max(v);
        int nu = Math.max(1, (int) Math.ceil((u1 - u0) / CELL - 1e-6));
        int nv = Math.max(1, (int) Math.ceil((v1 - v0) / CELL - 1e-6));
        double ticks = age * DURATION;
        // The whole ripple dims in steps too.
        float fade = (float) Math.ceil((1 - age) * 4) / 4;
        for (int i = 0; i < nu; i++)
            for (int j = 0; j < nv; j++) {
                double a0 = Mth.lerp(i / (double) nu, u0, u1), a1 = Mth.lerp((i + 1) / (double) nu, u0, u1);
                double b0 = Mth.lerp(j / (double) nv, v0, v1), b1 = Mth.lerp((j + 1) / (double) nv, v0, v1);
                Vec3 centre = point(axis, plane, u, (a0 + a1) / 2, v, (b0 + b1) / 2);
                float spread = spread(home, centre);
                if (spread == 0)
                    continue;
                float glow = intensity(squareDistance(centre, ripple.origin), ticks, age);
                boolean bright = glow >= 0.55F;
                if (!bright && glow < 0.22F)
                    continue;
                // A texel's own tone: the same texel always gets the same one, like a texture.
                boolean grain = Math.floorMod((int) Math.floor(centre.x * 16) * 73 + (int) Math.floor(centre.y * 16) * 31
                        + (int) Math.floor(centre.z * 16) * 17, 3) == 0;
                int rgb = bright ? grain ? BRIGHT_GLINT : BRIGHT : grain ? FAINT_GRAIN : FAINT;
                float alpha = (bright ? 0.85F : 0.6F) * fade * spread;
                vertex(pose, buffer, point(axis, plane, u, a0, v, b0), alpha, rgb);
                vertex(pose, buffer, point(axis, plane, u, a1, v, b0), alpha, rgb);
                vertex(pose, buffer, point(axis, plane, u, a1, v, b1), alpha, rgb);
                vertex(pose, buffer, point(axis, plane, u, a0, v, b1), alpha, rgb);
            }
    }

    /** A model quad cut into texels; the texels its sprite leaves transparent stay dark. */
    private static void drawQuad(PoseStack.Pose pose, VertexConsumer buffer, Ripple ripple, Quad quad, float age) {
        Vector3f[] c = quad.corners;
        Vec3 p0 = new Vec3(c[0]), p1 = new Vec3(c[1]), p2 = new Vec3(c[2]), p3 = new Vec3(c[3]);
        int ns = Math.max(1, (int) Math.round(p1.distanceTo(p0) / CELL));
        int nt = Math.max(1, (int) Math.round(p3.distanceTo(p0) / CELL));
        Vec3 out = new Vec3(quad.side.getStepX(), quad.side.getStepY(), quad.side.getStepZ()).scale(OUTSET);
        double ticks = age * DURATION;
        float fade = (float) Math.ceil((1 - age) * 4) / 4;
        for (int i = 0; i < ns; i++)
            for (int j = 0; j < nt; j++) {
                double s0 = i / (double) ns, s1 = (i + 1) / (double) ns, t0 = j / (double) nt, t1 = (j + 1) / (double) nt;
                double sm = (s0 + s1) / 2, tm = (t0 + t1) / 2;
                if (quad.sprite != null && transparent(quad, sm, tm))
                    continue;
                Vec3 centre = at(p0, p1, p2, p3, sm, tm);
                float spread = spread(ripple.home, centre);
                if (spread == 0)
                    continue;
                float glow = intensity(squareDistance(centre, ripple.origin), ticks, age);
                boolean bright = glow >= 0.55F;
                if (!bright && glow < 0.22F)
                    continue;
                boolean grain = Math.floorMod((int) Math.floor(centre.x * 16) * 73 + (int) Math.floor(centre.y * 16) * 31
                        + (int) Math.floor(centre.z * 16) * 17, 3) == 0;
                int rgb = bright ? grain ? BRIGHT_GLINT : BRIGHT : grain ? FAINT_GRAIN : FAINT;
                float alpha = (bright ? 0.85F : 0.6F) * fade * spread;
                vertex(pose, buffer, at(p0, p1, p2, p3, s0, t0).add(out), alpha, rgb);
                vertex(pose, buffer, at(p0, p1, p2, p3, s1, t0).add(out), alpha, rgb);
                vertex(pose, buffer, at(p0, p1, p2, p3, s1, t1).add(out), alpha, rgb);
                vertex(pose, buffer, at(p0, p1, p2, p3, s0, t1).add(out), alpha, rgb);
            }
    }

    /** Point of the quad at {@code s} along its first edge and {@code t} along its last. */
    private static Vec3 at(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double s, double t) {
        return p0.lerp(p1, s).lerp(p3.lerp(p2, s), t);
    }

    private static boolean transparent(Quad quad, double s, double t) {
        double u = Mth.lerp(t, Mth.lerp(s, quad.u[0], quad.u[1]), Mth.lerp(s, quad.u[3], quad.u[2]));
        double v = Mth.lerp(t, Mth.lerp(s, quad.v[0], quad.v[1]), Mth.lerp(s, quad.v[3], quad.v[2]));
        var contents = quad.sprite.contents();
        int x = Mth.clamp((int) Math.floor(u * contents.width()), 0, contents.width() - 1);
        int y = Mth.clamp((int) Math.floor(v * contents.height()), 0, contents.height() - 1);
        return contents.isTransparent(0, x, y);
    }

    /** Full inside the hit block; outside it, dimmer in three steps until nothing half a block out. */
    private static float spread(AABB home, Vec3 at) {
        double dx = Math.max(0, Math.max(home.minX - at.x, at.x - home.maxX));
        double dy = Math.max(0, Math.max(home.minY - at.y, at.y - home.maxY));
        double dz = Math.max(0, Math.max(home.minZ - at.z, at.z - home.maxZ));
        double out = Math.sqrt(dx * dx + dy * dy + dz * dz) - OUTSET;
        if (out <= 0)
            return 1;
        return out >= SPREAD ? 0 : (float) Math.ceil((1 - out / SPREAD) * 3) / 4;
    }

    /**
     * Distance measured as a square, not a circle (the largest of the three axes), so the rings grow as squares: blocky,
     * like everything else in the game.
     */
    private static double squareDistance(Vec3 a, Vec3 b) {
        return Math.max(Math.abs(a.x - b.x), Math.max(Math.abs(a.y - b.y), Math.abs(a.z - b.z)));
    }

    /** Rings spreading from the hit, the first the brightest, and a short flash where the hit landed. */
    private static float intensity(double distance, double ticks, float age) {
        double total = 0;
        for (int k = 0; k < RINGS; k++) {
            double radius = SPEED * ticks - k * SPACING;
            if (radius < 0)
                continue;
            double x = (distance - radius) / WIDTH;
            total += Math.exp(-x * x) * (1.0 - k * 0.3);
        }
        total += Math.max(0, 1 - distance / 0.2) * Math.max(0, 1 - age * 5);
        return (float) Math.min(1.0, total);
    }

    private static Vec3 point(Direction.Axis axis, double plane, Direction.Axis u, double a, Direction.Axis v, double b) {
        double[] xyz = new double[3];
        xyz[axis.ordinal()] = plane;
        xyz[u.ordinal()] = a;
        xyz[v.ordinal()] = b;
        return new Vec3(xyz[0], xyz[1], xyz[2]);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, Vec3 at, float alpha, int rgb) {
        buffer.addVertex(pose, (float) at.x, (float) at.y, (float) at.z).setColor(ARGB.color((int) (alpha * 255), rgb));
    }
}
