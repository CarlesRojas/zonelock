package app.pinya.zonelock.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import app.pinya.zonelock.block.SealBlock;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.TrailParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A ball of energy that travels slowly from a locked core to each seal that still burns, leaving a trail of motes. They follow the blocks
 * (straight runs along the axes, turning at corners) through the open air of the dungeon: corridors, doors, bars.
 * A seal walled in where no air reaches it gets no pulse at all.
 */
public final class EnergyPulses {
    private static final int COLOR = 0xC9A0FF;
    private static final DustParticleOptions TRAIL = new DustParticleOptions(0xB57CFF, 0.75F);
    private static final DustParticleOptions TRAIL_LIGHT = new DustParticleOptions(0xE2CCFF, 0.5F);
    /** Blocks per tick; long routes go a little faster so a pulse never takes more than about 8 seconds. */
    private static final double SPEED = 0.08;
    private static final int MAX_TICKS = 160;
    /** Size of the ball of motes at the head of the pulse, in blocks. */
    private static final double BALL = 0.3;
    private static final int MAX_NODES = 40_000;
    private static final int MARGIN = 6;

    private record Route(List<Vec3> points, double[] at, double length, double speed) {
        Vec3 point(double distance) {
            distance = Mth.clamp(distance, 0, length);
            for (int i = 1; i < points.size(); i++) {
                if (distance <= at[i]) {
                    double span = at[i] - at[i - 1];
                    double t = span <= 0 ? 0 : (distance - at[i - 1]) / span;
                    return points.get(i - 1).lerp(points.get(i), t);
                }
            }
            return points.getLast();
        }
    }

    private static final List<Route> ROUTES = new ArrayList<>();
    private static int age;

    private EnergyPulses() {
    }

    public static void start(ClientLevel level, BlockPos core, List<BlockPos> seals) {
        ROUTES.clear();
        age = 0;
        for (BlockPos seal : seals) {
            Route route = route(level, core, seal);
            if (route != null)
                ROUTES.add(route);
        }
    }

    public static void tick(ClientLevel level) {
        if (ROUTES.isEmpty())
            return;
        age++;
        RandomSource random = level.getRandom();
        boolean alive = false;
        for (Route route : ROUTES) {
            double head = age * route.speed();
            if (head > route.length()) {
                // Arrival: a little burst on the seal, once.
                if (head - route.speed() <= route.length()) {
                    Vec3 end = route.points().getLast();
                    for (int i = 0; i < 5; i++)
                        level.addParticle(ParticleTypes.ELECTRIC_SPARK, end.x, end.y, end.z,
                                (random.nextDouble() - 0.5) * 0.2, random.nextDouble() * 0.15,
                                (random.nextDouble() - 0.5) * 0.2);
                }
                continue;
            }
            alive = true;
            // A small ball: a few glowing motes that live for three ticks around the head and drift with it.
            Vec3 at = route.point(head);
            Vec3 next = route.point(head + route.speed() * 3);
            for (int i = 0; i < 6; i++) {
                double ox = (random.nextDouble() - 0.5) * BALL;
                double oy = (random.nextDouble() - 0.5) * BALL;
                double oz = (random.nextDouble() - 0.5) * BALL;
                level.addAlwaysVisibleParticle(
                        new TrailParticleOption(new Vec3(next.x + ox * 0.4, next.y + oy * 0.4, next.z + oz * 0.4),
                                COLOR, 3),
                        true, at.x + ox, at.y + oy, at.z + oz, 0, 0, 0);
            }
            // It sheds motes all along its way, which linger and fade behind it.
            for (int i = 0; i < 3; i++)
                level.addParticle(i == 0 ? TRAIL_LIGHT : TRAIL, at.x + (random.nextDouble() - 0.5) * 0.25,
                        at.y + (random.nextDouble() - 0.5) * 0.25, at.z + (random.nextDouble() - 0.5) * 0.25,
                        0, 0, 0);
        }
        if (!alive)
            ROUTES.clear();
    }

    // ------------------------------------------------------------------ path finding

    /**
     * Shortest path through open blocks (breadth-first from the seal), walked from the core preferring to keep going
     * straight, so the pulse turns as few times as possible. Only the corners are kept.
     */
    private static @Nullable Route route(ClientLevel level, BlockPos core, BlockPos seal) {
        int minX = Math.min(core.getX(), seal.getX()) - MARGIN, maxX = Math.max(core.getX(), seal.getX()) + MARGIN;
        int minY = Math.min(core.getY(), seal.getY()) - MARGIN, maxY = Math.max(core.getY(), seal.getY()) + MARGIN;
        int minZ = Math.min(core.getZ(), seal.getZ()) - MARGIN, maxZ = Math.max(core.getZ(), seal.getZ()) + MARGIN;
        Map<BlockPos, Integer> distance = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        distance.put(seal, 0);
        queue.add(seal);
        while (!queue.isEmpty() && distance.size() < MAX_NODES && !distance.containsKey(core)) {
            BlockPos pos = queue.poll();
            int d = distance.get(pos);
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (next.getX() < minX || next.getX() > maxX || next.getY() < minY || next.getY() > maxY
                        || next.getZ() < minZ || next.getZ() > maxZ || distance.containsKey(next))
                    continue;
                if (!next.equals(core) && !open(level, next))
                    continue;
                distance.put(next, d + 1);
                queue.add(next);
            }
        }
        if (!distance.containsKey(core))
            return null;

        // Walk downhill from the core, keeping the same direction while it stays on a shortest path.
        List<BlockPos> corners = new ArrayList<>();
        List<Direction> turns = new ArrayList<>();
        BlockPos pos = core;
        Direction heading = null;
        corners.add(core);
        while (!pos.equals(seal)) {
            int d = distance.get(pos);
            Direction pick = null;
            if (heading != null && distance.getOrDefault(pos.relative(heading), Integer.MAX_VALUE) == d - 1)
                pick = heading;
            else
                for (Direction dir : Direction.values())
                    if (distance.getOrDefault(pos.relative(dir), Integer.MAX_VALUE) == d - 1) {
                        pick = dir;
                        break;
                    }
            if (pick == null)
                return null;
            if (pick != heading && heading != null)
                corners.add(pos);
            if (pick != heading)
                turns.add(pick);
            heading = pick;
            pos = pos.relative(pick);
        }
        corners.add(seal);

        List<Vec3> points = new ArrayList<>();
        for (BlockPos corner : corners)
            points.add(Vec3.atCenterOf(corner));
        // Leave from the face of the core and arrive on the face of the seal, not from inside them.
        Direction first = turns.getFirst();
        points.set(0, Vec3.atCenterOf(core).add(first.getStepX() * 0.55, first.getStepY() * 0.55,
                first.getStepZ() * 0.55));
        Direction last = heading;
        points.set(points.size() - 1, Vec3.atCenterOf(seal).add(-last.getStepX() * 0.45, -last.getStepY() * 0.45,
                -last.getStepZ() * 0.45));
        double[] at = new double[points.size()];
        for (int k = 1; k < points.size(); k++)
            at[k] = at[k - 1] + points.get(k - 1).distanceTo(points.get(k));
        double length = at[points.size() - 1];
        return new Route(points, at, length, Math.max(SPEED, length / MAX_TICKS));
    }

    /** Energy flows through anything a player could see or pass through: air, plants, doors, bars and panes. */
    private static boolean open(ClientLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof SealBlock)
            return false;
        return state.getCollisionShape(level, pos).isEmpty() || state.getBlock() instanceof DoorBlock
                || state.getBlock() instanceof TrapDoorBlock || state.getBlock() instanceof FenceGateBlock
                || state.getBlock() instanceof IronBarsBlock;
    }
}
