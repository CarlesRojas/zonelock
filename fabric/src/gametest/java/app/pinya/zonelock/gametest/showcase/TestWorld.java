package app.pinya.zonelock.gametest.showcase;

import java.util.List;

import app.pinya.zonelock.block.CoreBlockEntity;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Builds the Zone Lock test world: a platform in the sky with one station per feature, each with a sign saying what to
 * try and a chest with what it needs, and the mod's dungeon on the terrain below. Everything uses the blocks as players
 * get them: the Amethyst Core, the Redstone Core, Amethyst and Redstone Seals. Play it in survival: in creative you are
 * a map maker and can switch any core off.
 */
public final class TestWorld {
    /** Standing height on the platform. */
    public static final int Y = 160;
    /** Distance between stations along +X. */
    public static final int STEP = 24;
    private static final int STATIONS = 11;

    public static final BlockPos STARTER_CORE = new BlockPos(0, Y, 0);
    public static final BlockPos BASIC = station(1);
    public static final BlockPos SEALS_TOUCH = station(2);
    public static final BlockPos SEALS_REDSTONE = station(3);
    public static final BlockPos REDSTONE = station(4);
    public static final BlockPos REDSTONE_SEALED = station(5);
    public static final BlockPos BUILD = station(6);
    public static final BlockPos RULES = station(7);
    public static final BlockPos MAP_LOCKED = station(8);
    public static final BlockPos RECIPES = station(9);
    public static final BlockPos RIPPLES = station(10);
    public static final int DUNGEON_Z = 120;

    public static final String[] DUNGEONS = { "amethyst_tower" };

    private TestWorld() {
    }

    private static BlockPos station(int index) {
        return new BlockPos(index * STEP, Y, 0);
    }

    public static void build(TestServerContext server) {
        target = server;
        for (String rule : new String[] { "advance_time false", "advance_weather false", "spawn_monsters false",
                "spawn_patrols false", "spawn_phantoms false", "keep_inventory true", "send_command_feedback false",
                "show_advancement_messages false", "respawn_radius 0" })
            server.runCommand("gamerule " + rule);
        server.runCommand("time set noon");
        server.runCommand("weather clear");
        server.runCommand("setworldspawn 0 " + Y + " -10");
        int end = STEP * STATIONS + 40;
        server.runCommand("forceload add -48 -48 " + end + " 48");
        // The platform: polished deepslate with a calcite path in front of the stations, cleared air above.
        for (int x = -40; x < end; x += 32) {
            int x2 = Math.min(x + 31, end - 1);
            server.runCommand("fill " + x + " " + Y + " -30 " + x2 + " " + (Y + 12) + " 30 minecraft:air");
            server.runCommand("fill " + x + " " + (Y - 1) + " -30 " + x2 + " " + (Y - 1) + " 30 minecraft:polished_deepslate");
            server.runCommand("fill " + x + " " + (Y - 1) + " 11 " + x2 + " " + (Y - 1) + " 13 minecraft:calcite");
        }

        // 0. Welcome: what to do, and a chest with everything to try to cheat the zones.
        set(new BlockPos(-3, Y, -10), "minecraft:chest[facing=south]" + chest(STARTER_ITEMS));
        // Next to it: amethyst to arm cores and every focus block (they go in the slot of the core's screen).
        set(new BlockPos(-1, Y, -10), "minecraft:chest[facing=south]" + chest(FOCUS_ITEMS));
        label(0, -6, "Zone Lock · mundo de pruebas", "Juega en supervivencia: en creativo eres creador de mapas y lo saltas todo."
                + "\\nCada estación, de izquierda a derecha, prueba una cosa. El cofre tiene herramientas para intentar colarte."
                + "\\nCon la varita en la mano ves el borde de las zonas."
                + "\\nEl segundo cofre: amatista y bloques de enfoque (van en el hueco de la pantalla del núcleo).");

        // 1. The Amethyst Core alone: nothing inside can be changed until you reach it.
        armedCore(BASIC, "amethyst_core", "Núcleo de amatista", 5);
        walls(BASIC);
        label(BASIC, "1 · Núcleo de amatista", "Dentro de la zona no se puede romper, poner ni entrar con perlas."
                + "\\nLlega al núcleo y haz clic derecho: se apaga y la zona queda libre.");

        // 2. Amethyst Seals: put out by touch.
        armedCore(SEALS_TOUCH, "amethyst_core", "Sellos de amatista", 6);
        seal(SEALS_TOUCH.offset(4, 0, 0), false);
        seal(SEALS_TOUCH.offset(-4, 0, 2), false);
        seal(SEALS_TOUCH.offset(0, 0, -4), false);
        label(SEALS_TOUCH, "2 · Sellos de amatista", "Clic derecho a cada sello para apagarlo, luego al núcleo."
                + "\\nSi el núcleo sigue cerrado, clic derecho: lanza un pulso hacia los sellos que quedan.");

        // 3. Redstone Seals: put out by a redstone signal.
        armedCore(SEALS_REDSTONE, "amethyst_core", "Sellos de redstone", 6);
        seal(SEALS_REDSTONE.offset(4, 0, 0), true);
        set(SEALS_REDSTONE.offset(5, 0, 0), "minecraft:lever[face=floor,facing=east]");
        seal(SEALS_REDSTONE.offset(-4, 0, 0), true);
        set(SEALS_REDSTONE.offset(-5, 0, 0), "minecraft:stone_button[face=floor,facing=west]");
        seal(SEALS_REDSTONE.offset(0, 0, -4), true);
        set(SEALS_REDSTONE.offset(0, 0, -3), "minecraft:stone_pressure_plate");
        label(SEALS_REDSTONE, "3 · Sellos de redstone", "Se apagan con una señal: palanca, botón y placa de presión."
                + "\\nCon los tres apagados, clic derecho al núcleo.");

        // 4. The Redstone Core: switched off by a signal.
        armedCore(REDSTONE, "redstone_core", "Mecanismo", 5);
        set(REDSTONE.offset(1, 0, 0), "minecraft:lever[face=floor,facing=east]");
        label(REDSTONE, "4 · Núcleo de redstone", "Se apaga con una señal de redstone, no con la mano."
                + "\\nTira de la palanca que tiene al lado.");

        // 5. A Redstone Core with a seal: the seal first, then the signal.
        armedCore(REDSTONE_SEALED, "redstone_core", "Mecanismo sellado", 6);
        seal(REDSTONE_SEALED.offset(-4, 0, 0), false);
        set(REDSTONE_SEALED.offset(1, 0, 0), "minecraft:lever[face=floor,facing=east]");
        label(REDSTONE_SEALED, "5 · Núcleo de redstone con sello", "Todos los núcleos respetan los sellos de su zona."
                + "\\nPrimero apaga el sello, luego tira de la palanca (antes no hace nada).");

        // 8. Make your own: switched-off cores (anyone can set them up) to shape and arm.
        // Far enough apart that their zones (3 a side) don't touch: zones never cross.
        String small = "{config:{extents:{up:3,down:1,north:3,south:3,east:3,west:3},rules:1023}}";
        set(BUILD.offset(-9, 0, 0), "zonelock:amethyst_core[facing=south]" + small);
        set(BUILD, "zonelock:redstone_core[facing=south]" + small);
        set(BUILD.offset(9, 0, 0), "zonelock:amethyst_core[facing=south]" + small);
        set(BUILD.offset(0, 0, 8), "minecraft:chest[facing=north]" + chest(BUILD_ITEMS));
        label(BUILD, "6 · Crea tu zona", "Clic derecho a un núcleo apagado: cualquiera puede configurarlo (nombre y reglas)."
                + "\\nVarita: apunta a una pared de la zona apagada; clic derecho la aleja de ti, agachado + clic derecho la acerca."
                + "\\nUn bloque de enfoque (hierro, oro, esmeralda, diamante, netherita) en el hueco de la pantalla del núcleo agranda el alcance."
                + "\\nPon un fragmento de amatista encima para armarlo: desde entonces es un reto también para ti.");

        // 9. Rules lab: everything a zone stops, ready to try.
        armedCore(RULES, "amethyst_core", "Laboratorio de reglas", 4);
        set(RULES.offset(2, 0, -2), "minecraft:tnt");
        set(RULES.offset(2, 0, -3), "minecraft:lever[face=floor,facing=north]");
        server.runCommand(fill(RULES.offset(-3, 0, 2), RULES.offset(-1, 0, 3), "minecraft:stone_bricks"));
        set(RULES.offset(6, 0, 0), "minecraft:piston[facing=west]");
        set(RULES.offset(5, 0, 0), "minecraft:oak_planks");
        set(RULES.offset(7, 0, 0), "minecraft:lever[face=floor,facing=east]");
        set(RULES.offset(-6, 0, 0), "minecraft:dispenser[facing=east]{Items:[{Slot:0b,id:\"minecraft:water_bucket\",count:1}]}");
        set(RULES.offset(-7, 0, 0), "minecraft:stone_button[face=floor,facing=west]");
        server.runCommand(fill(RULES.offset(1, -1, 2), RULES.offset(2, -1, 3), "minecraft:farmland"));
        server.runCommand(fill(RULES.offset(1, 0, 2), RULES.offset(2, 0, 3), "minecraft:wheat[age=7]"));
        set(RULES.offset(-2, 0, -3), "minecraft:stone_bricks");
        set(RULES.offset(-2, 1, -3), "minecraft:stone_bricks");
        BlockPos frame = RULES.offset(-2, 1, -2);
        server.runOnServer(s -> {
            var itemFrame = new net.minecraft.world.entity.decoration.ItemFrame(s.overworld(), frame, Direction.SOUTH);
            itemFrame.setItem(new ItemStack(net.minecraft.world.item.Items.DIAMOND));
            s.overworld().addFreshEntity(itemFrame);
        });
        BlockPos stand = RULES.offset(-1, 0, 1);
        server.runCommand("summon armor_stand " + (stand.getX() + 0.5) + " " + stand.getY() + " " + (stand.getZ() + 0.5)
                + " {equipment:{head:{id:\"minecraft:golden_helmet\",count:1}}}");
        label(RULES, "7 · Laboratorio de reglas", "Prueba la TNT, el pistón y el agua que apuntan dentro,"
                + "\\nel marco, el soporte de armadura, los cultivos, las perlas y el élitro (cofre del inicio).");

        // 10. Map lock: a map maker's core; players only get the challenge.
        armedCore(MAP_LOCKED, "amethyst_core", "Cámara del creador", 4, true);
        label(MAP_LOCKED, "8 · Bloqueo de creador", "Nadie salvo un operador en creativo puede cambiarlo:"
                + "\\nsolo queda el reto. Llega al núcleo para apagarlo.");
        // The same lock on a switched-off core: it ignores players, like any other block.
        BlockPos off = MAP_LOCKED.offset(8, 0, 0);
        target.runCommand("setblock " + off.getX() + " " + off.getY() + " " + off.getZ()
                + " zonelock:amethyst_core[armed=false,facing=south]{config:{name:\"Núcleo del creador\",extents:{up:2,"
                + "down:1,north:2,south:2,east:2,west:2},rules:1023,config_locked:1b},armed:0b}");
        label(off, "8b · Bloqueo de creador, apagado", "No reacciona: ni pantalla, ni amatista, ni varita."
                + "\\nUn operador en creativo sí puede abrirlo.");

        // 11. Recipes: a crafting table and every ingredient.
        set(RECIPES, "minecraft:crafting_table");
        set(RECIPES.offset(2, 0, 0), "minecraft:chest[facing=north]" + chest(RECIPE_ITEMS));
        label(RECIPES, "9 · Recetas", "Núcleo: amatista arriba, oro - bloque de amatista - oro, tres piedras negras abajo."
                + "\\nNúcleo + redstone = núcleo de redstone."
                + "\\nSello: amatista sobre oro - piedra negra - oro (2) · sello + redstone = sello de redstone."
                + "\\nVarita: oro, oro y amatista en diagonal.");

        // 12. Ripple lab: every kind of shape inside one sealed zone, to hit them all.
        rippleLab();

        // 11. Ways a builder could shut themselves in while arming a dungeon (TrapScenarios), in a row south of the
        // platform, each with its sign.
        server.runCommand("forceload add -16 30 " + (TrapScenarios.ALL.length * 22) + " 70");
        for (int i = 0; i < TrapScenarios.ALL.length; i++) {
            TrapScenarios.Scenario scenario = TrapScenarios.ALL[i];
            BlockPos origin = new BlockPos(i * 22, Y, 42);
            scenario.builder().build(server, origin);
            label(origin.getX() + 3, origin.getZ() - 7, "11." + (i + 1) + " · " + scenario.title(),
                    scenario.description());
        }

        // The mod's dungeons, generated on the terrain below the platform.
        server.runCommand("forceload add -48 " + (DUNGEON_Z - 48) + " 240 " + (DUNGEON_Z + 64));
        for (int i = 0; i < DUNGEONS.length; i++)
            server.runCommand("place structure zonelock:" + DUNGEONS[i] + " " + (i * 60) + " 100 " + DUNGEON_Z);
        label(0, 24, "Torre de amatista", "La mazmorra del mod, en el suelo al norte de la plataforma (z " + DUNGEON_Z + ")");

    }

    // ------------------------------------------------------------------ helpers

    private static TestServerContext target;

    private static void armedCore(BlockPos pos, String block, String name, int side) {
        armedCore(pos, block, name, side, false);
    }

    /** An armed, unowned core (a challenge for everyone) with a zone of {@code side} blocks around it. */
    private static void armedCore(BlockPos pos, String block, String name, int side, boolean mapLocked) {
        target.runCommand("setblock " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " zonelock:" + block
                + "[armed=true,facing=south]{config:{name:\"" + name + "\",extents:{up:4,down:1,north:" + side
                + ",south:" + side + ",east:" + side + ",west:" + side + "},rules:1023,config_locked:" + (mapLocked ? "1b" : "0b") + "},armed:1b}");
    }

    private static void lab(int dx, int dy, int dz, String state) {
        set(RIPPLES.offset(dx, dy, dz), "minecraft:" + state);
    }

    /**
     * Station 12: one sealed zone full of every kind of shape, alone and in odd combinations, so the ripple can be
     * checked everywhere: a wall with things hung on it, stairs/slabs/fences/walls/panes, utility blocks, plants on
     * their soil, rails and flat things, and blocks stuck together in strange ways.
     */
    private static void rippleLab() {
        armedCore(RIPPLES, "amethyst_core", "Laboratorio de ondas", 10);
        target.runCommand(fill(RIPPLES.offset(-10, 0, -10), RIPPLES.offset(10, 3, -10), "minecraft:stone_bricks"));
        // Hung on the wall (z -9, facing south).
        for (int dy = 0; dy < 3; dy++) {
            lab(-9, dy, -9, "ladder[facing=south]");
            lab(-5, dy, -9, "vine[north=true]");
        }
        lab(-8, 1, -9, "wall_torch[facing=south]");
        lab(-4, 1, -9, "oak_wall_sign[facing=south]");
        lab(-3, 1, -9, "stone_button[face=wall,facing=south]");
        lab(-2, 1, -9, "lever[face=wall,facing=south]");
        lab(-1, 2, -9, "white_wall_banner[facing=south]");
        lab(0, 1, -9, "tripwire_hook[facing=south]");
        lab(1, 1, -9, "redstone_wall_torch[facing=south]");
        lab(2, 1, -10, "jungle_log");
        lab(2, 1, -9, "cocoa[facing=north,age=2]");
        lab(3, 1, -9, "oak_trapdoor[facing=south,half=top,open=true]");
        lab(4, 1, -9, "iron_bars");
        lab(5, 1, -9, "amethyst_cluster[facing=south]");
        lab(7, 1, -9, "glow_lichen[north=true]");
        lab(8, 1, -9, "skeleton_wall_skull[facing=south]");
        lab(9, 1, -9, "end_rod[facing=south]");
        BlockPos framed = RIPPLES.offset(-7, 1, -9), empty = RIPPLES.offset(-7, 2, -9);
        BlockPos floorFrame = RIPPLES.offset(-9, 1, 5);
        lab(-9, 0, 5, "stone_bricks");
        summonFrame(framed, 3, "minecraft:diamond_sword");
        summonFrame(empty, 3, null);
        summonFrame(floorFrame, 1, "minecraft:amethyst_shard");
        BlockPos painting = RIPPLES.offset(6, 2, -9);
        target.runCommand("summon painting " + painting.getX() + " " + painting.getY() + " " + painting.getZ()
                + " {facing:0b,variant:\"minecraft:kebab\"}");
        BlockPos stand = RIPPLES.offset(6, 0, 5);
        target.runCommand("summon armor_stand " + (stand.getX() + 0.5) + " " + stand.getY() + " " + (stand.getZ() + 0.5)
                + " {ShowArms:1b,equipment:{head:{id:\"minecraft:iron_helmet\",count:1}}}");

        // Shapes (z -6): a block beside a stair, stairs stacked, slabs at three heights, fences into a gate, walls,
        // panes into bars.
        lab(-9, 0, -6, "stone_bricks");
        lab(-8, 0, -6, "stone_brick_stairs[facing=east,half=bottom]");
        lab(-7, 0, -6, "stone_brick_stairs[facing=west,half=top]");
        lab(-7, 1, -6, "stone_brick_slab[type=bottom]");
        lab(-5, 0, -6, "stone_brick_slab[type=bottom]");
        lab(-4, 0, -6, "stone_brick_slab[type=top]");
        lab(-3, 0, -6, "stone_brick_slab[type=double]");
        lab(-1, 0, -6, "oak_fence[east=true]");
        lab(0, 0, -6, "oak_fence[west=true,east=true]");
        lab(1, 0, -6, "oak_fence_gate[facing=south]");
        lab(3, 0, -6, "cobblestone_wall[up=true,east=low]");
        lab(4, 0, -6, "cobblestone_wall[up=false,west=low,east=low]");
        lab(5, 0, -6, "cobblestone_wall[up=true,west=low]");
        lab(7, 0, -6, "glass_pane[east=true]");
        lab(8, 0, -6, "glass_pane[west=true,east=true]");
        lab(9, 0, -6, "iron_bars[west=true]");

        // Utility blocks (z -4).
        String[] utility = { "chest[facing=south]", "ender_chest[facing=south]", "anvil[facing=south]",
                "enchanting_table", "cauldron", "hopper", "brewing_stand", "bell[attachment=floor,facing=south]",
                "campfire[lit=false]", "lectern[facing=south]", "grindstone[face=floor,facing=south]", "stonecutter",
                "composter", "cake", "candle_cake", "potted_poppy", "oak_door[half=lower,facing=south]",
                "oak_trapdoor[half=bottom]", "scaffolding" };
        for (int i = 0; i < utility.length; i++)
            lab(-9 + i, 0, -4, utility[i]);
        lab(7, 1, -4, "oak_door[half=upper,facing=south]");

        // Plants on their soil and flat things (z -2).
        target.runCommand(fill(RIPPLES.offset(-9, -1, -2), RIPPLES.offset(-7, -1, -2), "minecraft:farmland"));
        lab(-9, 0, -2, "wheat[age=7]");
        lab(-8, 0, -2, "carrots[age=7]");
        lab(-7, 0, -2, "potatoes[age=7]");
        target.runCommand(fill(RIPPLES.offset(-6, -1, -2), RIPPLES.offset(-2, -1, -2), "minecraft:grass_block"));
        lab(-6, 0, -2, "poppy");
        lab(-5, 0, -2, "dandelion");
        lab(-4, 0, -2, "short_grass");
        lab(-3, 0, -2, "tall_grass[half=lower]");
        lab(-3, 1, -2, "tall_grass[half=upper]");
        lab(-2, 0, -2, "sweet_berry_bush[age=3]");
        target.runCommand(fill(RIPPLES.offset(-1, -1, -2), RIPPLES.offset(0, -1, -2), "minecraft:sand"));
        lab(-1, 0, -2, "sugar_cane");
        lab(-1, 1, -2, "sugar_cane");
        lab(0, 0, -2, "cactus");
        lab(2, 0, -2, "rail[shape=north_south]");
        lab(3, 0, -2, "rail[shape=south_east]");
        lab(4, 0, -2, "white_carpet");
        lab(5, 0, -2, "snow[layers=3]");
        lab(6, 0, -2, "stone_pressure_plate");
        lab(7, 0, -2, "redstone_wire[north=side,south=side]");
        lab(8, 0, -2, "torch");
        lab(9, 0, -2, "lily_pad");
        lab(9, -1, -2, "water");

        // Odd combinations (z 3): carpet on a block, a torch on a fence, a lantern under a slab, a stair against a
        // slab, solid blocks of every finish side by side, and things that stand on others.
        lab(-9, 0, 3, "stone_bricks");
        lab(-9, 1, 3, "red_carpet");
        lab(-8, 0, 3, "oak_fence");
        lab(-8, 1, 3, "torch");
        lab(-7, 1, 3, "stone_brick_slab[type=top]");
        lab(-7, 0, 3, "lantern[hanging=true]");
        lab(-6, 0, 3, "stone_brick_stairs[facing=east]");
        lab(-5, 0, 3, "stone_brick_slab[type=bottom]");
        String[] solids = { "oak_leaves[persistent=true]", "glass", "slime_block", "honey_block", "soul_sand",
                "dirt_path", "mud" };
        for (int i = 0; i < solids.length; i++)
            lab(-3 + i + (i >= 3 ? 1 : 0), 0, 3, solids[i]);
        lab(5, 0, 3, "pointed_dripstone[vertical_direction=up,thickness=tip]");
        lab(6, 0, 3, "lightning_rod");
        lab(7, 0, 3, "amethyst_cluster[facing=up]");
        lab(8, 0, 3, "player_head");
        lab(9, 0, 3, "red_bed[part=foot,facing=south]");
        lab(9, 0, 4, "red_bed[part=head,facing=south]");
        lab(-4, 0, 5, "stone_bricks");
        lab(-4, 1, 5, "flower_pot");
        lab(-3, 0, 5, "chest[facing=south,type=right]");
        lab(-2, 0, 5, "chest[facing=south,type=left]");
        lab(0, 0, 5, "stone_brick_stairs[facing=north]");
        lab(0, 1, 5, "stone_brick_stairs[facing=south,half=top]");
        lab(2, 0, 5, "stone_brick_wall[up=true]");
        lab(2, 1, 5, "lantern");
        label(RIPPLES.getX(), RIPPLES.getZ() + 9, "10 · Laboratorio de ondas", "Toda la zona está sellada: golpea"
                + " cualquier cosa (bloques, plantas, marcos, cosas colgadas, combinaciones raras)."
                + "\\nLa onda debe seguir la forma real de lo que golpeas y pasar solo a lo que lo toca de verdad.");
    }

    /** An item frame at {@code pos}; {@code facing} as the entity stores it (1 up, 3 south), with an item or empty. */
    private static void summonFrame(BlockPos pos, int facing, @org.jspecify.annotations.Nullable String item) {
        target.runCommand("summon item_frame " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " {Facing:" + facing
                + "b" + (item == null ? "" : ",Item:{id:\"" + item + "\",count:1}") + "}");
    }

    /** Things to try to break or build on inside a basic zone. */
    private static void walls(BlockPos core) {
        target.runCommand(fill(core.offset(2, 0, 2), core.offset(3, 1, 2), "minecraft:deepslate_bricks"));
        target.runCommand(fill(core.offset(-3, 0, 1), core.offset(-3, 2, 1), "minecraft:amethyst_block"));
        target.runCommand(fill(core.offset(-1, 0, -3), core.offset(1, 0, -3), "minecraft:stone_bricks"));
    }

    private static void seal(BlockPos pos, boolean redstone) {
        set(pos, (redstone ? "zonelock:redstone_seal" : "zonelock:amethyst_seal") + "[lit=true]");
    }

    private static void set(BlockPos pos, String state) {
        target.runCommand("setblock " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " " + state);
    }

    private static String fill(BlockPos a, BlockPos c, String block) {
        return "fill " + a.getX() + " " + a.getY() + " " + a.getZ() + " " + c.getX() + " " + c.getY() + " " + c.getZ()
                + " " + block;
    }

    private static void label(BlockPos core, String title, String lines) {
        label(core.getX(), core.getZ() + 8, title, lines);
    }

    private static void label(int x, int z, String title, String lines) {
        target.runCommand("summon text_display " + (x + 0.5) + " " + (Y + 3.2) + " " + (z + 0.5)
                + " {billboard:\"center\",background:-1878982352,line_width:320,text:[{text:\"" + title
                + "\",color:\"light_purple\",bold:true},{text:\"\\n" + lines + "\",color:\"white\",bold:false}]}");
    }

    private static final String[] STARTER_ITEMS = { "zonelock:resonance_wand\",count:1",
            "minecraft:diamond_pickaxe\",count:1", "minecraft:diamond_axe\",count:1", "minecraft:diamond_hoe\",count:1",
            "minecraft:ender_pearl\",count:16", "minecraft:chorus_fruit\",count:16", "minecraft:elytra\",count:1",
            "minecraft:firework_rocket\",count:64",
            "minecraft:trident\",count:1,components:{\"minecraft:enchantments\":{\"minecraft:riptide\":3}}",
            "minecraft:water_bucket\",count:1", "minecraft:lava_bucket\",count:1", "minecraft:tnt\",count:16",
            "minecraft:flint_and_steel\",count:1", "minecraft:bow\",count:1", "minecraft:arrow\",count:64",
            "minecraft:bone_meal\",count:32", "minecraft:oak_boat\",count:1", "minecraft:ladder\",count:64",
            "minecraft:scaffolding\",count:64", "minecraft:cobblestone\",count:64", "minecraft:redstone\",count:64",
            "minecraft:lever\",count:8", "minecraft:cooked_beef\",count:32" };
    private static final String[] FOCUS_ITEMS = { "minecraft:amethyst_shard\",count:64",
            "zonelock:resonance_wand\",count:1",
            "minecraft:iron_block\",count:4", "minecraft:copper_block\",count:4", "minecraft:gold_block\",count:4",
            "minecraft:lapis_block\",count:4", "minecraft:emerald_block\",count:4", "minecraft:amethyst_block\",count:4",
            "minecraft:diamond_block\",count:4", "minecraft:netherite_block\",count:4" };
    private static final String[] BUILD_ITEMS = { "minecraft:amethyst_shard\",count:32",
            "zonelock:resonance_wand\",count:1", "zonelock:amethyst_seal\",count:8", "zonelock:redstone_seal\",count:8",
            "zonelock:amethyst_core\",count:4", "minecraft:iron_block\",count:2",
            "minecraft:gold_block\",count:2", "minecraft:emerald_block\",count:2", "minecraft:diamond_block\",count:2",
            "minecraft:netherite_block\",count:1", "minecraft:lever\",count:8", "minecraft:redstone\",count:32",
            "minecraft:stone_bricks\",count:64" };
    private static final String[] RECIPE_ITEMS = { "minecraft:amethyst_shard\",count:64",
            "minecraft:gold_ingot\",count:64", "minecraft:amethyst_block\",count:16", "minecraft:blackstone\",count:64",
            "minecraft:polished_deepslate\",count:64", "minecraft:iron_ingot\",count:32",
            "minecraft:redstone\",count:64" };

    private static String chest(String[] items) {
        StringBuilder out = new StringBuilder("{Items:[");
        for (int i = 0; i < items.length; i++) {
            if (i > 0)
                out.append(',');
            out.append("{Slot:").append(i).append("b,id:\"").append(items[i]).append('}');
        }
        return out.append("]}").toString();
    }
}
