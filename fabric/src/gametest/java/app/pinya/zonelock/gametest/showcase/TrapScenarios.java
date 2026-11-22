package app.pinya.zonelock.gametest.showcase;

import java.util.LinkedHashMap;
import java.util.Map;

import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.core.BlockPos;

/**
 * Small dungeons in which whoever sets them up could shut themselves in, all built from the same plain rooms: a row
 * of 5x3x5 rooms with glass roofs, an open doorway at the front (north), iron doors between rooms, the core in the
 * last one. {@code TrapScenarioTest} plays each one through (arming from inside, walking out, solving it); the test
 * world builds them too, to try by hand.
 */
public final class TrapScenarios {
    private TrapScenarios() {
    }

    /**
     * What a scenario built.
     *
     * @param core    the core (switched off, its zone over every room)
     * @param inside  where the builder stands to place the shard (feet)
     * @param outside a spot outside the zone, in front of the entrance (feet)
     * @param parts   named blocks of the scenario (seals, levers, doors...)
     */
    public record Site(String name, BlockPos core, BlockPos inside, BlockPos outside, Map<String, BlockPos> parts) {
        public BlockPos part(String key) {
            BlockPos pos = parts.get(key);
            if (pos == null)
                throw new IllegalArgumentException(name + " has no " + key);
            return pos;
        }
    }

    /** One entry of the catalogue: its name, a line for the sign, and how to build it at an origin. */
    public record Scenario(String name, String title, String description, Builder builder) {
    }

    @FunctionalInterface
    public interface Builder {
        Site build(TestServerContext server, BlockPos origin);
    }

    /** Every scenario, in the order they stand in the test world. */
    public static final Scenario[] ALL = {
            new Scenario("seal_door", "Sello que abre la puerta",
                    "Pon la amatista en el núcleo (sala del fondo) y sal: la puerta se cierra tras de ti."
                            + "\\nPara pasar: apaga el sello de encima de la puerta.", TrapScenarios::sealDoor),
            new Scenario("lever_door", "Palanca: puerta y sello a la vez",
                    "Sube la palanca para pasar, pon la amatista, vuelve, bájala y sal."
                            + "\\nPara pasar: la palanca abre la puerta y apaga el sello de redstone.",
                    TrapScenarios::leverDoor),
            new Scenario("chain", "Tres salas en cadena",
                    "Cada sello abre la puerta siguiente. Arma desde la última sala y sal andando.",
                    TrapScenarios::chain),
            new Scenario("drop_core", "Caída sin vuelta, salida del núcleo",
                    "Se entra saltando por el techo. La única salida la abre el núcleo apagado."
                            + "\\nArma desde dentro: mientras espera, la salida sigue abierta.", TrapScenarios::dropCore),
            new Scenario("drop_seal", "Caída sin vuelta, salida del sello",
                    "Se entra saltando por el techo. La salida la abre el sello de dentro.",
                    TrapScenarios::dropSeal),
            new Scenario("one_sided", "Puerta con botón solo fuera",
                    "Entra con el botón; dentro no hay. Mientras espera, la zona se puede editar:"
                            + "\\nrompe la puerta, sal, vuelve a ponerla con la varita en la mano y guárdala.",
                    TrapScenarios::oneSided),
            new Scenario("outside", "Núcleo en el muro",
                    "El núcleo se alcanza desde fuera: al ponerle la amatista se arma al momento.",
                    TrapScenarios::outside),
            new Scenario("bed", "Cama dentro",
                    "Duerme en la cama, arma y muere: no reapareces dentro de la zona cerrada.",
                    TrapScenarios::bed), };

    // ------------------------------------------------------------------ scenarios

    /** Two rooms; the amethyst seal over the door between them opens it (it gives a signal while not shutting it). */
    public static Site sealDoor(TestServerContext server, BlockPos o) {
        Map<String, BlockPos> parts = rooms(server, o, 2, true);
        BlockPos seal = parts.get("lintel1");
        set(server, seal, "zonelock:amethyst_seal[lit=true]");
        parts.put("seal", seal);
        return withCore(server, o, 2, "Sello y puerta", parts);
    }

    /** Two rooms; one lever opens the door and puts out the redstone seal beside it (both through the lintel). */
    public static Site leverDoor(TestServerContext server, BlockPos o) {
        Map<String, BlockPos> parts = rooms(server, o, 2, true);
        BlockPos lintel = parts.get("lintel1");
        BlockPos lever = lintel.north();
        set(server, lever, "minecraft:lever[face=wall,facing=north,powered=false]");
        BlockPos seal = lintel.east();
        set(server, seal, "zonelock:redstone_seal[lit=true]");
        parts.put("lever", lever);
        parts.put("seal", seal);
        return withCore(server, o, 2, "Palanca y sello", parts);
    }

    /** Three rooms; the seal over each door opens it: put out the first to reach the second, and so on. */
    public static Site chain(TestServerContext server, BlockPos o) {
        Map<String, BlockPos> parts = rooms(server, o, 3, true);
        for (int i = 1; i <= 2; i++) {
            BlockPos seal = parts.get("lintel" + i);
            set(server, seal, "zonelock:amethyst_seal[lit=true]");
            parts.put("seal" + i, seal);
        }
        return withCore(server, o, 3, "Cadena", parts);
    }

    /**
     * One room entered by dropping through a hole in the roof (no way back up); the only way out is an iron door in the
     * back wall that the core opens when it is switched off (the core sits in the wall beside it).
     */
    public static Site dropCore(TestServerContext server, BlockPos o) {
        Map<String, BlockPos> parts = rooms(server, o, 1, false);
        dropHole(server, o, parts);
        BlockPos door = exitDoor(server, o, parts);
        BlockPos core = door.east();
        Site site = coreAt(server, o, 1, core, "Caída y núcleo", parts);
        return new Site(site.name(), site.core(), door.north(), site.outside(), parts);
    }

    /** As {@link #dropCore}, but the way out is opened by an amethyst seal over the exit door. */
    public static Site dropSeal(TestServerContext server, BlockPos o) {
        Map<String, BlockPos> parts = rooms(server, o, 1, false);
        dropHole(server, o, parts);
        BlockPos door = exitDoor(server, o, parts);
        BlockPos seal = door.above(2);
        set(server, seal, "zonelock:amethyst_seal[lit=true]");
        parts.put("seal", seal);
        return withCore(server, o, 1, "Caída y sello", parts);
    }

    /** One room behind an iron door with its button only on the outside: once in, there is no opening it. */
    public static Site oneSided(TestServerContext server, BlockPos o) {
        Map<String, BlockPos> parts = rooms(server, o, 1, false);
        BlockPos door = new BlockPos(o.getX() + 3, o.getY() + 1, o.getZ());
        door(server, door, "north");
        BlockPos button = door.east().north();
        set(server, button, "minecraft:stone_button[face=wall,facing=north,powered=false]");
        parts.put("door", door);
        parts.put("button", button);
        return withCore(server, o, 1, "Botón fuera", parts);
    }

    /** The core set in the front wall beside the doorway: it can be reached from outside the zone. */
    public static Site outside(TestServerContext server, BlockPos o) {
        Map<String, BlockPos> parts = rooms(server, o, 1, true);
        BlockPos core = new BlockPos(o.getX() + 4, o.getY() + 1, o.getZ());
        Site site = coreAt(server, o, 1, core, "Núcleo en el muro", parts);
        return new Site(site.name(), site.core(), site.outside(), site.outside(), parts);
    }

    /** Two rooms with a seal door, and a bed in the core's room. */
    public static Site bed(TestServerContext server, BlockPos o) {
        Site site = sealDoor(server, o);
        BlockPos foot = new BlockPos(o.getX() + 1, o.getY() + 1, o.getZ() + 6 + 4);
        set(server, foot, "minecraft:red_bed[facing=north,part=foot,occupied=false]");
        set(server, foot.north(), "minecraft:red_bed[facing=north,part=head,occupied=false]");
        site.parts().put("bed", foot.north());
        return site;
    }

    // ------------------------------------------------------------------ building blocks

    /**
     * A floor all round and {@code count} rooms in a row going south from {@code o} (outer corner), walls of stone
     * bricks, glass roofs; iron doors between rooms ("door1", "door2"...) with a stone block over each ("lintel1"...).
     */
    private static Map<String, BlockPos> rooms(TestServerContext server, BlockPos o, int count, boolean frontDoorway) {
        Map<String, BlockPos> parts = new LinkedHashMap<>();
        int x = o.getX(), y = o.getY(), z = o.getZ();
        int end = z + 6 * count;
        server.runCommand(fill(x - 4, y, z - 6, x + 10, y + 7, end + 4, "minecraft:air"));
        server.runCommand(fill(x - 4, y - 1, z - 6, x + 10, y - 1, end + 4, "minecraft:polished_andesite"));
        for (int i = 0; i < count; i++) {
            int rz = z + 6 * i;
            server.runCommand(fill(x, y - 1, rz, x + 6, y + 4, rz + 6, "minecraft:stone_bricks") + " hollow");
            server.runCommand(fill(x + 1, y + 4, rz + 1, x + 5, y + 4, rz + 5, "minecraft:glass"));
        }
        if (frontDoorway)
            server.runCommand(fill(x + 3, y, z, x + 3, y + 1, z, "minecraft:air"));
        for (int i = 1; i < count; i++) {
            BlockPos door = new BlockPos(x + 3, y, z + 6 * i);
            door(server, door, "south");
            parts.put("door" + i, door);
            parts.put("lintel" + i, door.above(2));
        }
        return parts;
    }

    /** A hole in the roof of the first room and a block to climb onto the roof from outside (a one-way way in). */
    private static void dropHole(TestServerContext server, BlockPos o, Map<String, BlockPos> parts) {
        int x = o.getX(), y = o.getY(), z = o.getZ();
        set(server, new BlockPos(x + 2, y + 4, z + 2), "minecraft:air");
        // Steps along the front wall up to its top, then over the roof to the hole.
        for (int i = 0; i < 4; i++)
            server.runCommand(fill(x - 1 + i, y, z - 1, x - 1 + i, y + i, z - 1, "minecraft:stone_bricks"));
        parts.put("hole", new BlockPos(x + 2, y + 4, z + 2));
    }

    /** An iron door in the back (south) wall of the first room, to the outside. */
    private static BlockPos exitDoor(TestServerContext server, BlockPos o, Map<String, BlockPos> parts) {
        BlockPos door = new BlockPos(o.getX() + 3, o.getY(), o.getZ() + 6);
        door(server, door, "south");
        parts.put("exit", door);
        parts.put("beyond", door.south(2));
        return door;
    }

    private static void door(TestServerContext server, BlockPos lower, String facing) {
        set(server, lower, "minecraft:iron_door[facing=" + facing + ",half=lower,hinge=left,open=false,powered=false]");
        set(server, lower.above(), "minecraft:iron_door[facing=" + facing + ",half=upper,hinge=left,open=false,powered=false]");
    }

    /** The core in the middle of the last room. */
    private static Site withCore(TestServerContext server, BlockPos o, int count, String name, Map<String, BlockPos> parts) {
        BlockPos core = new BlockPos(o.getX() + 3, o.getY(), o.getZ() + 6 * (count - 1) + 3);
        return coreAt(server, o, count, core, name, parts);
    }

    /**
     * A switched-off core at {@code core} whose zone covers every room, walls and roof included, with a netherite focus
     * (without one, arming would cut the zone down to 4 blocks a side).
     */
    private static Site coreAt(TestServerContext server, BlockPos o, int count, BlockPos core, String name,
            Map<String, BlockPos> parts) {
        int x = o.getX(), y = o.getY() - 1, z = o.getZ();
        int west = core.getX() - x, east = x + 6 - core.getX(), down = core.getY() - y, up = y + 5 - core.getY();
        int north = core.getZ() - z, south = z + 6 * count - core.getZ();
        set(server, core, "zonelock:amethyst_core[facing=north]{config:{name:\"" + name + "\",extents:{up:" + up + ",down:"
                + down + ",north:" + north + ",south:" + south + ",east:" + east + ",west:" + west + "},rules:1023},"
                + "focus:{id:\"minecraft:netherite_block\",count:1}}");
        // Doors next to seals and cores look again now that those are in place (and giving their signal).
        for (BlockPos part : parts.values())
            server.runOnServer(s -> s.overworld().updateNeighborsAt(part, s.overworld().getBlockState(part).getBlock()));
        BlockPos inside = new BlockPos(core.getX() + (core.getX() > x + 3 ? -1 : 1), o.getY(), core.getZ());
        BlockPos outside = new BlockPos(x + 3, o.getY(), z - 4);
        return new Site(name, core, inside, outside, parts);
    }

    private static void set(TestServerContext server, BlockPos pos, String state) {
        server.runCommand("setblock " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " " + state);
    }

    private static String fill(int x1, int y1, int z1, int x2, int y2, int z2, String block) {
        return "fill " + x1 + " " + y1 + " " + z1 + " " + x2 + " " + y2 + " " + z2 + " " + block;
    }
}
