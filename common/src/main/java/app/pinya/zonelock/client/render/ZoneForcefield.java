package app.pinya.zonelock.client.render;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import app.pinya.zonelock.ZoneLock;
import app.pinya.zonelock.zone.ZoneBox;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the walls of zones (with the text render type: full bright, no shading). A texture of rows of small runes in
 * vanilla amethyst's tones (thin one-pixel strokes like the enchanting table's glyphs, a grain of darker and lighter
 * texels in each), over a faint glaze; the rows are shifted against each other and don't line up with the blocks.
 * <ul>
 * <li>Armed: every row of runes glides sideways, smoothly, each the opposite way of the rows next to it.</li>
 * <li>Switched off: the same runes, still and dimmer; the wall the wand points at shows them at full colour.</li>
 * </ul>
 * With the wand in hand the walls are drawn whole (only what stands in front hides them). Without it, a patch of
 * {@code radius} blocks of an armed zone's wall shows around the player when they come close, fading out smoothly
 * towards its rim and as they step away (a client setting).
 * <p>
 * The zone's edges are traced along the outermost texels of each wall in the same tones, darker towards the corners.
 * Fed every client tick by {@link app.pinya.zonelock.client.ZoneVisuals} and drawn from each loader's level render
 * event, together with the {@link BlockRipples}.
 */
public final class ZoneForcefield {
    private static final Identifier RUNES = ZoneLock.id("textures/misc/zone_runes.png");
    /**
     * The texture: {@link #BANDS} rows of runes ({@link #ROW} texels each) tiling along their first {@link #TEX_WIDTH}
     * texels, and the edge noise in its last row. One texture, so walls and edges go in one pass, drawn far to near.
     */
    private static final int TEX_WIDTH = 252, ROW = 10, BANDS = 32, ATLAS_WIDTH = 256, ATLAS_HEIGHT = ROW * BANDS + 1;
    private static final double EDGE_T0 = (ROW * BANDS + 0.25) / ATLAS_HEIGHT, EDGE_T1 = (ROW * BANDS + 0.75) / ATLAS_HEIGHT;
    private static final double TEXEL = 1.0 / 16.0;
    /** Walls are drawn this far towards the viewer, so they never fight with block faces in the same plane. */
    private static final double LIFT = 0.002;
    /** A switched-off zone shows its runes at this share of their colour (the wall the wand points at, at full). */
    private static final float DIM = 0.55F;
    /** Texels per tick a row glides while armed. */
    private static final double SLIDE = 0.18;
    /** A patch shows once the player is within this many times its radius of the wall, stronger as they come closer. */
    public static final double NEAR_REACH = 1.4;
    /** A faded patch is cut into pieces this wide, so its fade is smooth (each corner has its own alpha). */
    private static final double FADE_STEP = 0.25;

    /**
     * A zone to draw this frame, seen from {@code focus}: at most {@code strength} opaque, pushed {@code outset} blocks
     * outwards; {@code armed} animates it; {@code lit} is the wall the wand points at (full colour), if any;
     * {@code radius} 0 draws the walls whole, otherwise only a patch of that radius around the focus, fading out.
     */
    public record Patch(ZoneBox box, Vec3 focus, float strength, float outset, boolean armed, @Nullable Direction lit,
            float radius) {
        /** Whole walls (the wand). */
        public static Patch whole(ZoneBox box, Vec3 focus, boolean armed, @Nullable Direction lit) {
            return new Patch(box, focus, 1.0F, 0.0F, armed, lit, 0.0F);
        }

        /** A fading patch around the player (coming close, no wand). */
        public static Patch near(ZoneBox box, Vec3 focus, float strength, float radius) {
            return new Patch(box, focus, strength, 0.0F, true, null, radius);
        }
    }

    private static List<Patch> patches = List.of();

    private ZoneForcefield() {
    }

    public static void setPatches(List<Patch> next) {
        patches = next;
    }

    public static void submit(SubmitNodeCollector collector, PoseStack pose, LevelRenderState state) {
        double now = state.gameTime + (double) state.worldPartialTicks;
        BlockRipples.submit(collector, pose, state.cameraRenderState.pos, now);
        List<Patch> current = patches;
        if (current.isEmpty())
            return;
        Vec3 cam = state.cameraRenderState.pos;
        // The wand's whole walls ease in and out; the patches near the player give way to them meanwhile.
        float wand = app.pinya.zonelock.client.ZoneVisuals.wandLevel(now);
        List<Patch> drawn = new ArrayList<>();
        for (Patch patch : current) {
            float k = patch.radius() == 0 ? wand : 1 - wand;
            if (k > 0.004F)
                drawn.add(new Patch(patch.box(), patch.focus(), patch.strength() * k, patch.outset(), patch.armed(),
                        patch.lit(), patch.radius()));
        }
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        // Far walls first: each wall writes depth, so one drawn before a wall behind it would hide that one.
        List<Drawn> walls = new ArrayList<>();
        for (Patch patch : drawn)
            for (Direction face : Direction.values()) {
                Wall wall = wall(patch, face);
                if (wall != null)
                    walls.add(new Drawn(patch, wall, distance(wall, cam)));
            }
        walls.sort((x, y) -> Double.compare(y.distance, x.distance));
        collector.submitCustomGeometry(pose, RenderTypes.text(RUNES), (p, buffer) -> {
            for (Drawn d : walls) {
                drawRunes(p, buffer, d.patch, d.wall, now);
                drawEdges(p, buffer, d.patch, d.wall);
            }
        });
        pose.popPose();
    }

    /**
     * One wall of the zone: its plane (lifted towards the viewer) and in-plane axes, the part to draw (the whole wall,
     * or the patch around the focus), its colour, and {@code near}: how close the viewer is (1 for whole walls).
     */
    private record Wall(Direction.Axis axis, double plane, Direction.Axis u, Direction.Axis v, double uMin, double uMax,
            double vMin, double vMax, double fromU, double toU, double fromV, double toV, int alpha, int seed,
            float near) {
    }

    private record Drawn(Patch patch, Wall wall, double distance) {
    }

    /** From the camera to the nearest point of the part of the wall that is drawn. */
    private static double distance(Wall w, Vec3 cam) {
        double a = Math.max(w.fromU, Math.min(w.toU, cam.get(w.u)));
        double b = Math.max(w.fromV, Math.min(w.toV, cam.get(w.v)));
        double da = a - cam.get(w.u), db = b - cam.get(w.v), dn = w.plane - cam.get(w.axis);
        return Math.sqrt(da * da + db * db + dn * dn);
    }

    private static @Nullable Wall wall(Patch patch, Direction face) {
        ZoneBox box = patch.box();
        Direction.Axis axis = face.getAxis();
        double raw = box.face(face) + face.getAxisDirection().getStep() * patch.outset();
        Direction.Axis u = axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        Direction.Axis v = axis == Direction.Axis.Y ? Direction.Axis.Z : Direction.Axis.Y;
        double uMin = min(box, u), uMax = max(box, u), vMin = min(box, v), vMax = max(box, v);
        double fromU = uMin, toU = uMax, fromV = vMin, toV = vMax;
        float near = 1.0F;
        if (patch.radius() > 0) {
            double away = Math.abs(patch.focus().get(axis) - raw);
            near = (float) smooth(1 - away / (patch.radius() * NEAR_REACH));
            if (near <= 0)
                return null;
            double fu = patch.focus().get(u), fv = patch.focus().get(v), r = patch.radius();
            fromU = Math.max(uMin, fu - r);
            toU = Math.min(uMax, fu + r);
            fromV = Math.max(vMin, fv - r);
            toV = Math.min(vMax, fv + r);
            if (fromU >= toU || fromV >= toV)
                return null;
        }
        double plane = raw + (patch.focus().get(axis) >= raw ? LIFT : -LIFT);
        float colour = patch.armed() || face == patch.lit() ? 1.0F : DIM;
        // A seed per face, so two faces of one zone don't repeat each other.
        int seed = face.ordinal() * 7919 + (int) Math.floor(box.face(face)) * 31;
        return new Wall(axis, plane, u, v, uMin, uMax, vMin, vMax, fromU, toU, fromV, toV,
                Math.min(255, (int) (255 * colour * patch.strength())), seed, near);
    }

    /** Colour at a point of the wall: full for whole walls; for a patch, fading smoothly out from the focus. */
    private static int colorAt(Patch patch, Wall w, double a, double b) {
        if (patch.radius() <= 0)
            return ARGB.color(w.alpha, 0xFFFFFF);
        double du = a - patch.focus().get(w.u), dv = b - patch.focus().get(w.v);
        double fade = smooth(1 - Math.sqrt(du * du + dv * dv) / patch.radius()) * w.near;
        return ARGB.color((int) (w.alpha * fade), 0xFFFFFF);
    }

    private static double smooth(double x) {
        x = Math.max(0, Math.min(1, x));
        return x * x * (3 - 2 * x);
    }

    // ------------------------------------------------------------------ runes

    /**
     * The wall in horizontal bands, one per row of runes ({@link #ROW} texels tall), each mapped to one band of the
     * texture, shifted along it by its own offset plus, while armed, the glide. Where the texture wraps around the band
     * is split in two quads; a fading patch is also split every {@link #FADE_STEP}.
     */
    private static void drawRunes(PoseStack.Pose pose, VertexConsumer buffer, Patch patch, Wall w, double now) {
        double rowHeight = ROW * TEXEL;
        double step = patch.radius() > 0 ? FADE_STEP : Double.MAX_VALUE;
        // The outermost texel all round belongs to the edge trace: the runes stop short of it, so the two never overlap
        // (in one plane they would fight and flicker).
        double fromU = Math.max(w.fromU, w.uMin + TEXEL), toU = Math.min(w.toU, w.uMax - TEXEL);
        double fromV = Math.max(w.fromV, w.vMin + TEXEL), toV = Math.min(w.toV, w.vMax - TEXEL);
        if (fromU >= toU || fromV >= toV)
            return;
        int firstRow = (int) Math.floor(fromV / rowHeight), lastRow = (int) Math.ceil(toV / rowHeight) - 1;
        for (int row = firstRow; row <= lastRow; row++) {
            double b0 = Math.max(fromV, row * rowHeight), b1 = Math.min(toV, (row + 1) * rowHeight);
            if (b1 <= b0)
                continue;
            int h = hash(row, 0, w.seed);
            int band = h % BANDS;
            // Texture v of the band, from its bottom (b at row start) to its top.
            double vTop = (double) band * ROW / ATLAS_HEIGHT;
            double t0 = vTop + (1 - (b0 - row * rowHeight) / rowHeight) * ROW / ATLAS_HEIGHT;
            double t1 = vTop + (1 - (b1 - row * rowHeight) / rowHeight) * ROW / ATLAS_HEIGHT;
            double slide = patch.armed() ? (row % 2 == 0 ? 1 : -1) * now * SLIDE : 0;
            double offset = (h >>> 8) % TEX_WIDTH - slide; // texels
            double a = fromU;
            while (a < toU - 1e-9) {
                double tex = floorMod(a * 16 + offset, TEX_WIDTH); // texel of the texture at a
                if (TEX_WIDTH - tex < 1e-4)
                    tex = 0; // rounding left it a hair short of the wrap: it is the start of the next tile
                double a1 = Math.min(Math.min(toU, a + (TEX_WIDTH - tex) / 16.0), a + step);
                if (a1 <= a)
                    break;
                double s0 = tex / ATLAS_WIDTH, s1 = (tex + (a1 - a) * 16) / ATLAS_WIDTH;
                quad(pose, buffer, w, a, a1, b0, b1, s0, s1, t0, t1, colorAt(patch, w, a, b0),
                        colorAt(patch, w, a1, b0), colorAt(patch, w, a1, b1), colorAt(patch, w, a, b1));
                a = a1;
            }
        }
    }

    // ------------------------------------------------------------------ edges

    /**
     * The outermost texel of the wall all round: four strips, cut per block so each block of edge takes its own window
     * of the noise texture; the windows at the ends of a strip are the texture's darker ends (the corners).
     */
    private static void drawEdges(PoseStack.Pose pose, VertexConsumer buffer, Patch patch, Wall w) {
        // Strips along u, at the bottom and top of the wall.
        for (int side = 0; side < 2; side++) {
            double b0 = side == 0 ? w.vMin : w.vMax - TEXEL;
            if (b0 + TEXEL < w.fromV || b0 > w.toV)
                continue;
            strip(pose, buffer, patch, w, true, w.uMin, w.uMax, b0, side);
        }
        // Strips along v, at the two sides of the wall (between the strips above, so the corners aren't drawn twice).
        for (int side = 0; side < 2; side++) {
            double a0 = side == 0 ? w.uMin : w.uMax - TEXEL;
            if (a0 + TEXEL < w.fromU || a0 > w.toU)
                continue;
            strip(pose, buffer, patch, w, false, w.vMin + TEXEL, w.vMax - TEXEL, a0, 2 + side);
        }
    }

    private static void strip(PoseStack.Pose pose, VertexConsumer buffer, Patch patch, Wall w, boolean alongU,
            double from, double to, double across, int side) {
        double lo = alongU ? w.fromU : w.fromV, hi = alongU ? w.toU : w.toV;
        for (double t = from; t < to - 1e-9; t++) {
            double t1 = Math.min(to, t + 1);
            if (t1 <= lo || t >= hi)
                continue;
            boolean first = t == from, last = t1 >= to;
            int window = first ? 0 : last ? 15 : 1 + hash((int) Math.floor(t), side, w.seed) % 14;
            double s0 = window * 16 / 256.0, s1 = (window * 16 + (t1 - t) * 16) / 256.0;
            if (last && !first) {
                s1 = 1.0; // end on the texture's dark end
                s0 = 1.0 - (t1 - t) * 16 / 256.0;
            }
            if (alongU)
                quad(pose, buffer, w, t, t1, across, across + TEXEL, s0, s1, EDGE_T0, EDGE_T1, colorAt(patch, w, t, across),
                        colorAt(patch, w, t1, across), colorAt(patch, w, t1, across), colorAt(patch, w, t, across));
            else
                quadAlongV(pose, buffer, w, across, across + TEXEL, t, t1, s0, s1, colorAt(patch, w, across, t),
                        colorAt(patch, w, across, t1));
        }
    }

    // ------------------------------------------------------------------ geometry

    /**
     * A quad on the wall over [a0, a1] x [b0, b1], texture s along a, t along b (t0 at b0), a colour per corner
     * (c00 at a0 b0, c10 at a1 b0...). Both windings.
     */
    private static void quad(PoseStack.Pose pose, VertexConsumer buffer, Wall w, double a0, double a1, double b0,
            double b1, double s0, double s1, double t0, double t1, int c00, int c10, int c11, int c01) {
        if ((c00 | c10 | c11 | c01) >>> 24 == 0)
            return;
        vertex(pose, buffer, w, a0, b0, s0, t0, c00);
        vertex(pose, buffer, w, a1, b0, s1, t0, c10);
        vertex(pose, buffer, w, a1, b1, s1, t1, c11);
        vertex(pose, buffer, w, a0, b1, s0, t1, c01);
        vertex(pose, buffer, w, a0, b1, s0, t1, c01);
        vertex(pose, buffer, w, a1, b1, s1, t1, c11);
        vertex(pose, buffer, w, a1, b0, s1, t0, c10);
        vertex(pose, buffer, w, a0, b0, s0, t0, c00);
    }

    /** A quad whose texture s runs along b (an upright edge strip); colour c0 at b0, c1 at b1. Both windings. */
    private static void quadAlongV(PoseStack.Pose pose, VertexConsumer buffer, Wall w, double a0, double a1, double b0,
            double b1, double s0, double s1, int c0, int c1) {
        if ((c0 | c1) >>> 24 == 0)
            return;
        vertex(pose, buffer, w, a0, b0, s0, EDGE_T0, c0);
        vertex(pose, buffer, w, a1, b0, s0, EDGE_T1, c0);
        vertex(pose, buffer, w, a1, b1, s1, EDGE_T1, c1);
        vertex(pose, buffer, w, a0, b1, s1, EDGE_T0, c1);
        vertex(pose, buffer, w, a0, b1, s1, EDGE_T0, c1);
        vertex(pose, buffer, w, a1, b1, s1, EDGE_T1, c1);
        vertex(pose, buffer, w, a1, b0, s0, EDGE_T1, c0);
        vertex(pose, buffer, w, a0, b0, s0, EDGE_T0, c0);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, Wall w, double a, double b, double s,
            double t, int color) {
        double[] xyz = new double[3];
        xyz[w.axis.ordinal()] = w.plane;
        xyz[w.u.ordinal()] = a;
        xyz[w.v.ordinal()] = b;
        buffer.addVertex(pose, (float) xyz[0], (float) xyz[1], (float) xyz[2])
                .setColor(color)
                .setUv((float) s, (float) t)
                .setLight(0xF000F0);
    }

    private static double floorMod(double x, double m) {
        double r = x % m;
        return r < 0 ? r + m : r;
    }

    private static int hash(int a, int b, int seed) {
        int h = a * 73856093 ^ b * 19349663 ^ seed * 83492791;
        h ^= h >>> 13;
        h *= 0x5BD1E995;
        h ^= h >>> 15;
        return h & 0x7FFFFFFF;
    }

    private static double min(ZoneBox box, Direction.Axis axis) {
        return switch (axis) {
            case X -> box.minX();
            case Y -> box.minY();
            case Z -> box.minZ();
        };
    }

    private static double max(ZoneBox box, Direction.Axis axis) {
        return switch (axis) {
            case X -> box.maxX() + 1;
            case Y -> box.maxY() + 1;
            case Z -> box.maxZ() + 1;
        };
    }
}
