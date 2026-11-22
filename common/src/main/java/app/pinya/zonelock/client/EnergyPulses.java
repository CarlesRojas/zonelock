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
 * <p>
 * With more than one seal, the pulses leave the core by different faces when the way allows it (opposite faces best),
 * so each can be followed by eye: a face is taken if its way is at most {@link #SLACK} blocks longer than the shortest.
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
    /** How many blocks longer than the shortest a way may be to leave by a face of its own. */
    private static final int SLACK = 2;

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
        List<BlockPos> reached = new ArrayList<>();
        List<Map<BlockPos, Integer>> maps = new ArrayList<>();
        for (BlockPos seal : seals) {
            Map<BlockPos, Integer> distance = distances(level, core, seal);
            if (distance != null) {
                reached.add(seal);
                maps.add(distance);
            }
        }
        Direction[] exits = exits(core, maps);
        for (int i = 0; i < reached.size(); i++) {
            Route route = route(core, reached.get(i), maps.get(i), exits[i]);
            if (route != null)
                ROUTES.add(route);
        }
    }

    /** Blocks a pulse leaving the core by {@code face} goes to reach the seal of {@code distance}; MAX_VALUE if none. */
    private static int cost(BlockPos core, Map<BlockPos, Integer> distance, Direction face) {
        Integer d = distance.get(core.relative(face));
        return d == null ? Integer.MAX_VALUE : d + 1;
    }

    /**
     * The face each pulse leaves the core by: among the faces whose way is at most {@link #SLACK} longer than its
     * shortest, the choice that spreads the pulses most (different faces, opposite ones best), then the shortest.
     */
    private static Direction[] exits(BlockPos core, List<Map<BlockPos, Integer>> maps) {
        int n = maps.size();
        List<List<Direction>> options = new ArrayList<>();
        long combinations = 1;
        for (Map<BlockPos, Integer> distance : maps) {
            int best = Integer.MAX_VALUE;
            for (Direction face : Direction.values())
                best = Math.min(best, cost(core, distance, face));
            List<Direction> faces = new ArrayList<>();
            for (Direction face : Direction.values())
                if (best != Integer.MAX_VALUE && cost(core, distance, face) <= best + SLACK)
                    faces.add(face);
            // Shortest first, so ties and the greedy fallback lean to the shortest way.
            faces.sort((a, b) -> Integer.compare(cost(core, distance, a), cost(core, distance, b)));
            options.add(faces);
            combinations *= Math.max(1, faces.size());
        }
        Direction[] chosen = new Direction[n];
        if (n == 0)
            return chosen;
        if (combinations <= 4096) {
            search(core, maps, options, 0, new Direction[n], chosen, new double[] { Double.NEGATIVE_INFINITY });
        } else {
            for (int i = 0; i < n; i++) {
                double best = Double.NEGATIVE_INFINITY;
                for (Direction face : options.get(i)) {
                    double score = -0.75 * extra(core, maps.get(i), options.get(i), face);
                    for (int j = 0; j < i; j++)
                        score += spread(face, chosen[j]);
                    if (score > best) {
                        best = score;
                        chosen[i] = face;
                    }
                }
            }
        }
        return chosen;
    }

    private static void search(BlockPos core, List<Map<BlockPos, Integer>> maps, List<List<Direction>> options, int i,
            Direction[] current, Direction[] chosen, double[] best) {
        if (i == current.length) {
            double score = 0;
            for (int a = 0; a < current.length; a++) {
                if (current[a] == null)
                    continue;
                score -= 0.75 * extra(core, maps.get(a), options.get(a), current[a]);
                for (int b = 0; b < a; b++)
                    if (current[b] != null)
                        score += spread(current[a], current[b]);
            }
            if (score > best[0] + 1e-9) {
                best[0] = score;
                System.arraycopy(current, 0, chosen, 0, current.length);
            }
            return;
        }
        if (options.get(i).isEmpty()) {
            current[i] = null;
            search(core, maps, options, i + 1, current, chosen, best);
            return;
        }
        for (Direction face : options.get(i)) {
            current[i] = face;
            search(core, maps, options, i + 1, current, chosen, best);
        }
    }

    /** Blocks longer than the shortest way the pulse goes leaving by {@code face}. */
    private static int extra(BlockPos core, Map<BlockPos, Integer> distance, List<Direction> options, Direction face) {
        return cost(core, distance, face) - cost(core, distance, options.getFirst());
    }

    /** Two pulses leaving by the same face are hard to tell apart; by opposite faces, easiest. */
    private static double spread(Direction a, Direction b) {
        return a == b ? 0 : a == b.getOpposite() ? 2 : 1;
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
    /**
     * Steps from every open block to the seal (breadth-first from the seal), far enough to know the way from each face
     * of the core up to {@link #SLACK} blocks past the shortest; null if no air reaches the core.
     */
    private static @Nullable Map<BlockPos, Integer> distances(ClientLevel level, BlockPos core, BlockPos seal) {
        int minX = Math.min(core.getX(), seal.getX()) - MARGIN, maxX = Math.max(core.getX(), seal.getX()) + MARGIN;
        int minY = Math.min(core.getY(), seal.getY()) - MARGIN, maxY = Math.max(core.getY(), seal.getY()) + MARGIN;
        int minZ = Math.min(core.getZ(), seal.getZ()) - MARGIN, maxZ = Math.max(core.getZ(), seal.getZ()) + MARGIN;
        Map<BlockPos, Integer> distance = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        distance.put(seal, 0);
        queue.add(seal);
        int limit = Integer.MAX_VALUE;
        while (!queue.isEmpty() && distance.size() < MAX_NODES) {
            BlockPos pos = queue.poll();
            int d = distance.get(pos);
            if (d > limit)
                break;
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (next.getX() < minX || next.getX() > maxX || next.getY() < minY || next.getY() > maxY
                        || next.getZ() < minZ || next.getZ() > maxZ || distance.containsKey(next))
                    continue;
                if (next.equals(core)) {
                    // The core is reached but not crossed; go on a little to learn the ways to its other faces.
                    distance.put(next, d + 1);
                    limit = Math.min(limit, d + SLACK + 1);
                    continue;
                }
                if (!open(level, next))
                    continue;
                distance.put(next, d + 1);
                queue.add(next);
            }
        }
        return distance.containsKey(core) ? distance : null;
    }

    /**
     * The way from the core (leaving by {@code exit}) to the seal, walked downhill on the distances, keeping the same
     * direction while it stays on a shortest path, so the pulse turns as few times as possible. Only the corners are
     * kept.
     */
    private static @Nullable Route route(BlockPos core, BlockPos seal, Map<BlockPos, Integer> distance,
            @Nullable Direction exit) {
        if (exit == null || !distance.containsKey(core.relative(exit)))
            return null;
        List<BlockPos> corners = new ArrayList<>();
        List<Direction> turns = new ArrayList<>();
        corners.add(core);
        turns.add(exit);
        Direction heading = exit;
        BlockPos pos = core.relative(exit);
        while (!pos.equals(seal)) {
            int d = distance.get(pos);
            Direction pick = null;
            if (heading != null && !pos.relative(heading).equals(core)
                    && distance.getOrDefault(pos.relative(heading), Integer.MAX_VALUE) == d - 1)
                pick = heading;
            else
                for (Direction dir : Direction.values())
                    if (!pos.relative(dir).equals(core)
                            && distance.getOrDefault(pos.relative(dir), Integer.MAX_VALUE) == d - 1) {
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
