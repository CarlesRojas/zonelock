package app.pinya.zonelock.zone;

import java.util.Optional;
import java.util.function.Predicate;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * The wall of a zone a Resonance Wand points at: the first face of an editable zone along the player's view, within
 * {@link #REACH}. The same pick runs on the client (to light the wall up) and on the server (to move it). When two
 * walls lie in one plane the first one found wins.
 *
 * @param distance along the view, from the eye
 */
public record WandTarget(Zone zone, Direction face, double distance) {
    /** As far as the walls are drawn. */
    public static final double REACH = 12.0;

    public static Optional<WandTarget> pick(Iterable<Zone> zones, Vec3 eye, Vec3 look, double maxDistance,
            Predicate<Zone> editable) {
        WandTarget best = null;
        double limit = Math.min(maxDistance, REACH);
        for (Zone zone : zones) {
            if (!editable.test(zone))
                continue;
            ZoneBox box = zone.box();
            for (Direction face : Direction.values()) {
                Direction.Axis axis = face.getAxis();
                double d = look.get(axis);
                if (Math.abs(d) < 1e-6)
                    continue;
                double t = (box.face(face) - eye.get(axis)) / d;
                if (t <= 1e-6 || t > limit || best != null && t >= best.distance)
                    continue;
                Vec3 hit = eye.add(look.scale(t));
                boolean inside = true;
                for (Direction.Axis other : Direction.Axis.values())
                    if (other != axis && (hit.get(other) < min(box, other) || hit.get(other) > max(box, other)))
                        inside = false;
                if (inside)
                    best = new WandTarget(zone, face, t);
            }
        }
        return Optional.ofNullable(best);
    }

    /**
     * Which way the wall moves: away from the player, or towards them when sneaking. +1 when the wall's plane
     * coordinate grows. On the inside of a zone "away" grows it; on the outside it shrinks it.
     */
    public int planeStep(Vec3 eye, boolean towards) {
        double plane = zone.box().face(face);
        int away = plane >= eye.get(face.getAxis()) ? 1 : -1;
        return towards ? -away : away;
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
