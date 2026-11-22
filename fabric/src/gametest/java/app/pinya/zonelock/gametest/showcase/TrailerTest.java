package app.pinya.zonelock.gametest.showcase;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import app.pinya.zonelock.block.CoreBlockEntity;
import app.pinya.zonelock.zone.Extents;
import app.pinya.zonelock.zone.Zone;
import app.pinya.zonelock.zone.ZoneManager;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import java.util.concurrent.atomic.AtomicReference;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Screenshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Records the trailer: every scene played in the real game with real input (clicks, held keys, hotbar), the camera
 * driven along smooth paths, three frames per tick rendered in between ticks (60 fps). Frames go to
 * {@code fabric/build/trailer/frames}, the sounds the game played and the scene marks to
 * {@code fabric/build/trailer/events.jsonl}; {@code art/trailer/compose.py} turns them into the video.
 * <p>
 * Only runs with {@code ZONELOCK_TRAILER=1} (or {@code preview}: a small frame every 5 ticks, to check the shots).
 * The sets come from {@code art/trailer/sets.py}.
 */
public class TrailerTest implements FabricClientGameTest {
    private static final double EYE = 1.62;
    private static final int SET_Y = 200;
    private static final String PICKAXE = "minecraft:netherite_pickaxe[enchantments={\"minecraft:efficiency\":5}]";

    private ClientGameTestContext ctx;
    private TestServerContext server;
    private TestServerConnection connection;
    private boolean preview;
    /** {@code ZONELOCK_TRAILER=stills}: screenshots for the mod pages instead of the trailer. */
    private boolean stills;
    private Path root;
    private Path frames;
    private Writer events;
    private int frame;
    private int tick;

    // The camera: three cascaded smoothing stages towards a goal, for the eye and for the point looked at.
    private final Vec3[] eye = new Vec3[3];
    private final Vec3[] look = new Vec3[3];
    private Vec3 eyeGoal = Vec3.ZERO, lookGoal = Vec3.ZERO;
    private double rate = 4;

    // Where each set was placed: the room origin (the first floor block of the set's interior).
    private BlockPos hall, sanctum, cistern, bridge, maze;
    private BlockPos towerCore;

    @Override
    public void runTest(ClientGameTestContext context) {
        String mode = System.getenv("ZONELOCK_TRAILER");
        if (mode == null)
            return;
        preview = "preview".equals(mode);
        stills = "stills".equals(mode);
        ctx = context;
        root = Path.of("").toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("settings.gradle")))
            root = root.getParent();
        frames = root.resolve("fabric/build/trailer/" + (preview ? "preview" : stills ? "stills" : "frames"));
        try {
            if (Files.exists(frames))
                try (Stream<Path> old = Files.list(frames)) {
                    for (Path file : old.toList())
                        Files.delete(file);
                }
            Files.createDirectories(frames);
            events = Files.newBufferedWriter(root.resolve("fabric/build/trailer/" + (preview ? "preview" : "events")
                    + ".jsonl"), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 1440p: the GUI at scale 4 is as big as at scale 3 in 1080p. Stills for the pages: 1920x1080, scale 3.
        ctx.getInput().resizeWindow(preview ? 960 : stills ? 1920 : 2560, preview ? 540 : stills ? 1080 : 1440);
        ctx.runOnClient(mc -> {
            Options o = mc.options;
            o.guiScale().set(preview ? 2 : stills ? 3 : 4);
            o.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
            o.gamma().set(1.0);
            o.bobView().set(false);
            o.fov().set(72);
            o.chatVisibility().set(ChatVisiblity.HIDDEN);
            o.particles().set(ParticleStatus.ALL);
            o.renderDistance().set(12);
            o.entityShadows().set(true);
            mc.getSoundManager().addListener(new Recorder());
        });

        try (TestSingleplayerContext sp = ctx.worldBuilder().setUseConsistentSettings(false)
                .adjustSettings(s -> {
                    s.setSeed("zonelock-trailer");
                    s.setAllowCommands(true);
                }).create()) {
            server = sp.getServer();
            connection = sp.getConnection();
            setUp();
            if (stills)
                stills();
            else
                record();
        } finally {
            try {
                events.close();
            } catch (IOException ignored) {
            }
        }
    }

    // ------------------------------------------------------------------ the world

    private void setUp() {
        for (String rule : new String[] { "advance_time false", "advance_weather false", "spawn_monsters false",
                "spawn_patrols false", "spawn_phantoms false", "spawn_wandering_traders false",
                "send_command_feedback false", "show_advancement_messages false", "keep_inventory true",
                "fall_damage false" })
            server.runCommand("gamerule " + rule);
        server.runCommand("time set 11700");
        server.runCommand("weather clear");
        server.runCommand("gamemode spectator @a");

        // A forest for the tower and the maze.
        BlockPos forest = server.computeOnServer(s -> {
            var found = s.overworld().findClosestBiome3d(b -> b.is(Biomes.FOREST) || b.is(Biomes.FLOWER_FOREST)
                    || b.is(Biomes.BIRCH_FOREST), new BlockPos(0, 80, 0), 3200, 32, 64);
            return found == null ? new BlockPos(0, 80, 0) : found.getFirst();
        });
        int fx = forest.getX(), fz = forest.getZ();
        log("forest " + fx + " " + fz);
        server.runCommand("forceload add " + (fx - 80) + " " + (fz - 80) + " " + (fx + 80) + " " + (fz + 80));
        server.runCommand("tp @a " + fx + " 160 " + fz);
        ctx.waitTicks(40);
        int ground = surface(fx, fz);
        server.runCommand("place structure zonelock:amethyst_tower " + fx + " " + ground + " " + fz);
        ctx.waitTicks(20);
        towerCore = server.computeOnServer(s -> {
            BlockPos best = null;
            for (Zone zone : ZoneManager.get(s.overworld()).index().near(Vec3.atCenterOf(new BlockPos(fx, ground, fz)), 96))
                if (best == null || zone.anchor().distSqr(new BlockPos(fx, ground, fz)) < best.distSqr(new BlockPos(fx, ground, fz)))
                    best = zone.anchor();
            return best;
        });
        log("tower core " + towerCore);

        // The maze out in the open: a flat, dry stretch of plains or meadow, levelled into the ground at the median
        // height of its footprint.
        BlockPos field = flatMeadow(new BlockPos(fx, 80, fz));
        int mx = field.getX(), mz = field.getZ();
        server.runCommand("forceload add " + (mx - 48) + " " + (mz - 48) + " " + (mx + 48) + " " + (mz + 64));
        List<Integer> heights = new ArrayList<>();
        for (int dx = -12; dx <= 12; dx += 3)
            for (int dz = -12; dz <= 12; dz += 3)
                heights.add(surface(mx + dx, mz + dz));
        heights.sort(null);
        int lawn = heights.get(heights.size() / 2) - 1;
        maze = placeSet("maze", new BlockPos(mx - 12, lawn - 5, mz - 12));
        blendGround(mx, mz, lawn, 12, 12);

        // The rooms, far away and high up, closed all round.
        int rx = fx + 900;
        server.runCommand("forceload add " + (rx - 16) + " " + (fz - 16) + " " + (rx + 200) + " " + (fz + 48));
        hall = placeSet("hall", new BlockPos(rx, SET_Y, fz));
        sanctum = placeSet("sanctum", new BlockPos(rx + 40, SET_Y, fz));
        cistern = placeSet("cistern", new BlockPos(rx + 80, SET_Y, fz));
        bridge = placeSet("bridge", new BlockPos(rx + 120, SET_Y, fz));

        // The cores and seals, as a map maker would set them (named, zones round each room).
        setBlock(sanctum.offset(5, 2, 3), "zonelock:amethyst_core[armed=true,facing=south]" + core("Sanctum of Seals",
                5, 3, 5, 11, 7, 7));
        setBlock(sanctum.offset(2, 2, 3), "zonelock:amethyst_seal[lit=true]");
        setBlock(sanctum.offset(8, 2, 3), "zonelock:redstone_seal[lit=true]");
        setBlock(sanctum.offset(9, 2, 3), "minecraft:lever[face=floor,facing=west,powered=false]");
        setBlock(cistern.offset(4, 8, 0), "zonelock:amethyst_core[armed=true,facing=south]" + core("Drowned Aqueduct",
                5, 9, 2, 24, 6, 6));
        setBlock(bridge.offset(5, 2, 2), "zonelock:amethyst_core[armed=true,facing=south]" + core("Lava Forge",
                7, 5, 4, 20, 7, 7));
        setBlock(bridge.offset(3, 2, 15), "minecraft:stone_button[face=floor,facing=south,powered=false]");
        setBlock(maze.offset(9, 2, 9), "zonelock:amethyst_core[armed=true,facing=south]" + core("Hedge Maze",
                5, 1, 12, 12, 12, 12));
        ctx.waitTicks(40);
    }

    private static String core(String name, int up, int down, int north, int south, int east, int west) {
        return "{config:{name:\"" + name + "\",extents:{up:" + up + ",down:" + down + ",north:" + north + ",south:" + south
                + ",east:" + east + ",west:" + west + "},rules:1023},armed:1b}";
    }

    /** Places a set's template with its corner at {@code corner}; returns its room origin. */
    private BlockPos placeSet(String name, BlockPos corner) {
        Path file = root.resolve("art/trailer/build/" + name + ".nbt");
        server.runCommand("forceload add " + (corner.getX() - 2) + " " + (corner.getZ() - 2) + " " + (corner.getX() + 40)
                + " " + (corner.getZ() + 40));
        ctx.waitTicks(5);
        int[] o = server.computeOnServer(s -> {
            try {
                CompoundTag tag = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
                StructureTemplate template = new StructureTemplate();
                ServerLevel level = s.overworld();
                template.load(level.holderLookup(Registries.BLOCK), tag);
                template.placeInWorld(level, corner, corner, new StructurePlaceSettings(), level.getRandom(),
                        Block.UPDATE_CLIENTS);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return name.equals("bridge") ? new int[] { 2, 4, 2 } : name.equals("maze") ? new int[] { 3, 5, 3 }
                    : new int[] { 2, 1, 2 };
        });
        log("set " + name + " at " + corner);
        return corner.offset(o[0], o[1], o[2]);
    }

    /**
     * The middle of a flat, dry, treeless patch of plains or meadow near {@code from}: the ground under the maze and
     * the open grass the crane shot looks over (south of it) both within a few blocks of the same height.
     */
    private BlockPos flatMeadow(BlockPos from) {
        // Plains found from 25 starting points round the forest, each tried as it is and a little to every side.
        List<BlockPos> plains = new ArrayList<>();
        for (int ox = -2; ox <= 2; ox++)
            for (int oz = -2; oz <= 2; oz++) {
                BlockPos origin = from.offset(ox * 600, 0, oz * 600);
                BlockPos found = server.computeOnServer(s -> {
                    var f = s.overworld().findClosestBiome3d(b -> b.is(Biomes.PLAINS) || b.is(Biomes.SUNFLOWER_PLAINS),
                            origin, 600, 16, 64);
                    return f == null ? null : f.getFirst();
                });
                if (found != null && plains.stream().noneMatch(p -> p.distManhattan(found) < 96))
                    plains.add(found);
            }
        log("plains " + plains.size());
        // First a cheap pass on the biomes alone (no chunk is generated for it): green country round the site and
        // behind it. Then, for the best ten, the ground itself: flatness and water, and the height of what's behind.
        List<int[]> scored = new ArrayList<>();
        for (BlockPos p : plains)
            for (int ox = -1; ox <= 1; ox++)
                for (int oz = -1; oz <= 1; oz++) {
                    int cx = p.getX() + ox * 40, cz = p.getZ() + oz * 40;
                    scored.add(new int[] { cx, cz, biomeCost(cx, cz) });
                }
        scored.sort(java.util.Comparator.comparingInt(c -> c[2]));
        BlockPos best = null;
        int bestScore = Integer.MAX_VALUE;
        for (int i = 0; i < Math.min(10, scored.size()); i++) {
            int[] c = scored.get(i);
            int ground = spread(c[0], c[1]);
            if (ground >= 1000) {
                log("maze site " + c[0] + " " + c[1] + " biomes " + c[2] + ": water");
                continue;
            }
            int score = 3 * ground + c[2] + backdrop(c[0], c[1]);
            log("maze site " + c[0] + " " + c[1] + " biomes " + c[2] + " ground " + ground + " total " + score);
            if (score < bestScore) {
                bestScore = score;
                best = new BlockPos(c[0], 0, c[1]);
            }
        }
        log("maze field " + best + " score " + bestScore);
        if (best == null || bestScore >= 1000)
            throw new AssertionError("no dry, flat field for the maze near " + from);
        return best;
    }

    /**
     * Biomes round a site (the maze, its lawn to the south) and behind it (north, where the crane shot looks): every
     * sample that isn't green low country costs, the ones near the maze most. Noise lookups only, no chunk loads.
     */
    private int biomeCost(int cx, int cz) {
        return server.computeOnServer(s -> {
            var level = s.overworld();
            int cost = 0;
            for (int dz = -200; dz <= 60; dz += 20)
                for (int dx = -100; dx <= 100; dx += 20) {
                    var biome = level.getBiome(new BlockPos(cx + dx, 72, cz + dz));
                    boolean green = biome.is(Biomes.PLAINS) || biome.is(Biomes.SUNFLOWER_PLAINS)
                            || biome.is(Biomes.FOREST) || biome.is(Biomes.BIRCH_FOREST) || biome.is(Biomes.FLOWER_FOREST)
                            || biome.is(Biomes.DARK_FOREST);
                    if (!green)
                        cost += Math.abs(dx) <= 40 && dz >= -40 ? 30 : 10;
                }
            return cost;
        });
    }

    /**
     * What lies behind the maze in the crane shot (north of it, where the camera looks): snow or ice and high ground
     * (hills, mountains) cost; a plain green horizon costs nothing.
     */
    private int backdrop(int cx, int cz) {
        return server.computeOnServer(s -> {
            var level = s.overworld();
            int base = level.getChunk(cx >> 4, cz >> 4).getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx & 15,
                    cz & 15);
            int cost = 0, snow = 0, foreign = 0, highest = Integer.MIN_VALUE;
            for (int dz = -40; dz >= -180; dz -= 20)
                for (int dx = -100; dx <= 100; dx += 25) {
                    int x = cx + dx, z = cz + dz;
                    var chunk = level.getChunk(x >> 4, z >> 4);
                    int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15) - 1;
                    var block = level.getBlockState(new BlockPos(x, top, z));
                    if (block.is(net.minecraft.world.level.block.Blocks.SNOW)
                            || block.is(net.minecraft.world.level.block.Blocks.SNOW_BLOCK)
                            || block.is(net.minecraft.world.level.block.Blocks.POWDER_SNOW)
                            || block.is(net.minecraft.tags.BlockTags.ICE))
                        snow++;
                    // Green, low country behind: plains and the woods next to them; anything else (snow, slopes,
                    // mountains, desert) stands out behind a hedge maze.
                    var biome = level.getBiome(new BlockPos(x, top, z));
                    if (!(biome.is(Biomes.PLAINS) || biome.is(Biomes.SUNFLOWER_PLAINS) || biome.is(Biomes.FOREST)
                            || biome.is(Biomes.BIRCH_FOREST) || biome.is(Biomes.FLOWER_FOREST)
                            || biome.is(Biomes.RIVER) || biome.is(Biomes.DARK_FOREST)))
                        foreign++;
                    highest = Math.max(highest, top);
                    cost += Math.max(0, top - base - 12) * 2;
                }
            log("  backdrop " + cx + " " + cz + ": snow " + snow + ", other biomes " + foreign + ", highest " + highest
                    + " (ground " + base + ")");
            return cost + 25 * snow;
        });
    }

    /**
     * Eases the natural ground into the maze's lawn: within {@code reach} blocks of its footprint ({@code half} blocks
     * each side of the middle) each column's surface slides smoothly from the lawn's height at the garden wall to its
     * own height further out, so the maze sits in the meadow instead of on a cut-out terrace. Plants, flowers and the
     * ground further away stay as the world made them.
     */
    private void blendGround(int mx, int mz, int lawn, int half, int reach) {
        server.runOnServer(s -> {
            ServerLevel level = s.overworld();
            net.minecraft.world.level.block.state.BlockState grass = net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState();
            net.minecraft.world.level.block.state.BlockState dirt = net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState();
            net.minecraft.world.level.block.state.BlockState air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
            for (int x = mx - half - reach; x <= mx + half + reach; x++)
                for (int z = mz - half - reach; z <= mz + half + reach; z++) {
                    int dx = Math.max(Math.max(mx - half - x, x - (mx + half)), 0);
                    int dz = Math.max(Math.max(mz - half - z, z - (mz + half)), 0);
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d == 0) {
                        // Over the maze itself the set clears only so high: whatever ground rose above it goes too.
                        for (int y = lawn + 9; y <= lawn + 48; y++)
                            if (!level.getBlockState(new BlockPos(x, y, z)).isAir())
                                level.setBlock(new BlockPos(x, y, z), air, Block.UPDATE_CLIENTS);
                        continue;
                    }
                    if (d > reach)
                        continue;
                    double t = d / reach;
                    t = t * t * (3 - 2 * t);
                    // The natural ground: the top block that isn't a plant, a log or leaves.
                    int g = level.getChunk(x >> 4, z >> 4).getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            x & 15, z & 15) - 1;
                    while (g > lawn - 24) {
                        net.minecraft.world.level.block.state.BlockState st = level.getBlockState(new BlockPos(x, g, z));
                        if (st.isAir() || st.canBeReplaced() || st.is(net.minecraft.tags.BlockTags.LOGS)
                                || st.is(net.minecraft.tags.BlockTags.LEAVES))
                            g--;
                        else
                            break;
                    }
                    int h = (int) Math.round(lawn + (g - lawn) * t);
                    if (h == g)
                        continue;
                    if (h < g)
                        for (int y = h + 1; y <= g + 2; y++)
                            level.setBlock(new BlockPos(x, y, z), air, Block.UPDATE_CLIENTS);
                    else
                        for (int y = g + 1; y < h; y++)
                            level.setBlock(new BlockPos(x, y, z), dirt, Block.UPDATE_CLIENTS);
                    level.setBlock(new BlockPos(x, h, z), grass, Block.UPDATE_CLIENTS);
                    // A raised column loses the grass or flower it had on top; give some of it back.
                    if (h > g && ((x * 31 + z * 17) & 7) == 0)
                        level.setBlock(new BlockPos(x, h + 1, z),
                                net.minecraft.world.level.block.Blocks.SHORT_GRASS.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
        });
        ctx.waitTicks(10);
    }

    /**
     * How unfit a maze site is: the height range of the ground round it, its camera lawn to the south and a margin
     * (hills and mountains), plus the trees; any water at all rules it out (it floods the paths and its current pushes
     * the player into the hedges).
     */
    private int spread(int cx, int cz) {
        return server.computeOnServer(s -> {
            var level = s.overworld();
            int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE, trees = 0;
            for (int dx = -30; dx <= 30; dx += 3)
                for (int dz = -36; dz <= 46; dz += 3) {
                    int x = cx + dx, z = cz + dz;
                    var chunk = level.getChunk(x >> 4, z >> 4);
                    int ground = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x & 15, z & 15);
                    int top = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x & 15, z & 15);
                    if (Math.abs(dx) <= 32 && dz >= -28 && dz <= 56
                            && !level.getFluidState(new BlockPos(x, ground - 1, z)).isEmpty())
                        return 1000;
                    if (top > ground + 1)
                        trees++;
                    lo = Math.min(lo, ground);
                    hi = Math.max(hi, ground);
                }
            // A tree trunk counts as ground too (no leaves heightmap), so both show up as spread; leaves add a bit.
            return hi - lo + trees / 2;
        });
    }

    private int surface(int x, int z) {
        return server.computeOnServer(s -> s.overworld().getChunk(x >> 4, z >> 4)
                .getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x & 15, z & 15) + 1);
    }

    private int canopy(int x, int z) {
        return server.computeOnServer(s -> s.overworld().getChunk(x >> 4, z >> 4)
                .getHeight(Heightmap.Types.MOTION_BLOCKING, x & 15, z & 15) + 1);
    }

    private void setBlock(BlockPos pos, String state) {
        server.runCommand("setblock " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " " + state);
    }

    // ------------------------------------------------------------------ stills for the mod pages

    /** A few pictures of what the trailer doesn't show: the core's screen, the wand at work, inside the tower. */
    private void stills() {
        // The core's screen, over the crypt: named, a gold focus in its slot.
        survival();
        cinematic(false);
        server.runCommand("clear @a");
        BlockPos core = hall.offset(6, 2, 9);
        setBlock(core, "zonelock:amethyst_core[facing=south]{config:{name:\"Crypt of Echoes\",extents:{up:7,down:3,"
                + "north:11,south:12,east:8,west:8},rules:1023},focus:{id:\"minecraft:gold_block\",count:1}}");
        give(0, "zonelock:resonance_wand", 1);
        give(1, "minecraft:amethyst_shard", 16);
        give(2, "minecraft:diamond_block", 2);
        give(3, PICKAXE, 1);
        select(0);
        Vec3 view = at(hall, 6.5, 1 + EYE, 12.4);
        cut(view, at(hall, 6.5, 2.4, 9.5));
        settle(view);
        server.runOnServer(s -> {
            if (s.overworld().getBlockEntity(core) instanceof CoreBlockEntity c)
                c.openScreen(s.getPlayerList().getPlayers().getFirst());
        });
        ctx.waitTicks(10);
        still("core_screen");
        ctx.setScreen(() -> null);
        ctx.waitTicks(5);

        // The wand: the switched-off zone's walls, the one it points at lit, from the side of the hall.
        server.runOnServer(s -> {
            if (s.overworld().getBlockEntity(core) instanceof CoreBlockEntity c)
                c.setConfig(c.config().withExtents(new Extents(5, 3, 5, 5, 5, 5)));
        });
        Vec3 side = at(hall, 8.6, 1 + EYE, 12.6);
        cut(side, at(hall, 5.0, 3.6, 4.0));
        settle(side);
        ctx.waitTicks(20);
        still("wand_walls");

        // The sanctum, armed: both seals burning beside the core, the card in the corner.
        server.runCommand("clear @a");
        Vec3 shrine = at(sanctum, 8.8, 1 + EYE, 9.6);
        cut(shrine, at(sanctum, 4.6, 2.2, 3.0));
        settle(shrine);
        ctx.waitTicks(10);
        still("sanctum");

        // The lava forge after its puzzle: the stones up, every lamp lit.
        for (int z = 5; z <= 13; z++)
            setBlock(bridge.offset(5, -3, z), "minecraft:redstone_block");
        for (int z = 5; z <= 13; z += 2) {
            setBlock(bridge.offset(-2, 3, z), "minecraft:redstone_block");
            setBlock(bridge.offset(12, 3, z), "minecraft:redstone_block");
        }
        cinematic(true);
        Vec3 forge = at(bridge, 5.5, 4.6, 19.6);
        cut(forge, at(bridge, 5.5, 1.4, 6.0));
        settle(forge);
        ctx.waitTicks(30);
        still("lava_forge");
        recipes();
        log("stills written to " + frames);
    }

    /** Each recipe laid out in a crafting table, one still each (compose.py's recipes sheet crops them). */
    private void recipes() {
        survival();
        cinematic(false);
        server.runCommand("clear @a");
        BlockPos table = sanctum.offset(5, 1, 8);
        setBlock(table, "minecraft:crafting_table");
        Vec3 view = at(sanctum, 5.5, 1 + EYE, 10.5);
        cut(view, at(sanctum, 5.5, 1.6, 8.5));
        settle(view);
        String shard = "minecraft:amethyst_shard", gold = "minecraft:gold_ingot", stone = "minecraft:blackstone";
        String[][] recipes = {
                { "recipe_core", "", shard, "", gold, "minecraft:amethyst_block", gold, stone, stone, stone },
                { "recipe_seal", "", shard, "", gold, stone, gold, "", "", "" },
                { "recipe_wand", "", "", shard, "", gold, "", gold, "", "" },
                { "recipe_redstone_core", "zonelock:amethyst_core", "minecraft:redstone", "", "", "", "", "", "", "" },
                { "recipe_redstone_seal", "zonelock:amethyst_seal", "minecraft:redstone", "", "", "", "", "", "", "" } };
        for (String[] recipe : recipes) {
            server.runOnServer(s -> {
                var level = s.overworld();
                var player = s.getPlayerList().getPlayers().getFirst();
                level.getBlockState(table).useWithoutItem(level, player, new net.minecraft.world.phys.BlockHitResult(
                        Vec3.atCenterOf(table), Direction.UP, table, false));
                for (int i = 0; i < 9; i++) {
                    String id = recipe[i + 1];
                    ItemStack stack = id.isEmpty() ? ItemStack.EMPTY
                            : new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
                                    net.minecraft.resources.Identifier.parse(id)));
                    player.containerMenu.getSlot(i + 1).set(stack);
                }
                player.containerMenu.slotsChanged(player.containerMenu.getSlot(1).container);
            });
            ctx.waitTicks(6);
            still(recipe[0]);
            server.runOnServer(s -> s.getPlayerList().getPlayers().getFirst().closeContainer());
            ctx.waitTicks(3);
        }
    }

    private void still(String name) {
        ctx.runOnClient(mc -> mc.gui.toastManager().clear());
        ctx.waitTick();
        ctx.runOnClient(mc -> mc.gui.toastManager().clear());
        take(name, 1.0F);
    }

    // ------------------------------------------------------------------ the shots

    private void record() {
        opening();
        hallScenes();
        sanctumScene();
        bridgeScene();
        cisternScene();
        mazeScene();
        ending();
        mark("end");
        log("recorded " + frame + " frames over " + tick + " ticks");
    }

    private Vec3 at(BlockPos origin, double x, double y, double z) {
        return new Vec3(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
    }

    /** The tower from the air: a slow orbit on the sunlit side, the title beside it. No wand: no zones show. */
    private void opening() {
        spectator();
        cinematic(true);
        server.runCommand("clear @a");
        Vec3 center = new Vec3(towerCore.getX() + 0.5, towerCore.getY() - 13, towerCore.getZ() + 2.5);
        orbit("opening", center, 36, 195, 228, 9, 150, 3.5, -1);
    }

    /** The tower again; the wand comes into the hand and its zone fades in round it, under the end card. */
    private void ending() {
        creative();
        cinematic(false);
        server.runCommand("clear @a");
        give(1, "zonelock:resonance_wand", 1);
        select(0);
        Vec3 center = new Vec3(towerCore.getX() + 0.5, towerCore.getY() - 12, towerCore.getZ() + 2.5);
        orbit("ending", center, 46, 248, 222, 14, 170, 2, 26);
    }

    /**
     * A slow orbit round {@code center}, from {@code from} to {@code to} degrees (0 = camera south of it), at one
     * height that clears every tree along the way (no bumps). At tick {@code wandAt} (if not -1) the wand is taken.
     */
    private void orbit(String name, Vec3 center, double radius, double from, double to, double height, int ticks,
            double lookUp, int wandAt) {
        double clear = center.y + height;
        for (int t = 0; t <= 20; t++) {
            double a = from + (to - from) * t / 20.0;
            for (double r : new double[] { radius, radius - 4 }) {
                double rad = Math.toRadians(a);
                int x = (int) Math.floor(center.x + Math.sin(rad) * r), z = (int) Math.floor(center.z + Math.cos(rad) * r);
                for (int dx = -2; dx <= 2; dx += 2)
                    for (int dz = -2; dz <= 2; dz += 2)
                        clear = Math.max(clear, canopy(x + dx, z + dz) + 4);
            }
        }
        double base = clear - center.y;
        Vec3 first = orbitEye(center, radius, from, base);
        cut(first, aside(first, center.add(0, lookUp, 0), radius));
        settle(first);
        mark(name);
        for (int t = 0; t < ticks; t++) {
            if (t == wandAt)
                select(1);
            double k = t / (double) (ticks - 1);
            double eased = 0.5 - 0.5 * Math.cos(Math.PI * k);
            double a = from + (to - from) * (0.25 * k + 0.75 * eased);
            Vec3 e = orbitEye(center, radius - 4 * eased, a, base + 2 * eased);
            exact(e, aside(e, center.add(0, lookUp, 0), radius));
            step();
        }
    }

    /**
     * A point beside {@code target}, so it sits about three quarters of the way across the frame: halfway between the
     * end of the title (which takes the left half) and the right edge.
     */
    private static Vec3 aside(Vec3 eye, Vec3 target, double radius) {
        Vec3 f = target.subtract(eye).multiply(1, 0, 1).normalize();
        Vec3 right = new Vec3(-f.z, 0, f.x);
        return target.subtract(right.scale(radius * 0.84));
    }

    private static Vec3 orbitEye(Vec3 center, double radius, double degrees, double height) {
        double r = Math.toRadians(degrees);
        return new Vec3(center.x + Math.sin(r) * radius, center.y + height, center.z + Math.cos(r) * radius);
    }

    /** The crypt hall: place a core, shape its zone, seal it, and find it holds. */
    private void hallScenes() {
        survival();
        cinematic(false);
        server.runCommand("clear @a");
        give(0, "zonelock:amethyst_core", 1);
        give(1, "zonelock:resonance_wand", 1);
        give(2, "minecraft:amethyst_shard", 8);
        give(3, PICKAXE, 1);
        give(4, "minecraft:deepslate_bricks", 16);
        select(0);
        Vec3 start = at(hall, 6.5, 1 + EYE, 17.6);
        cut(start, at(hall, 6.5, 3.4, 0));
        settle(start);

        // Walking in, the eyes already on the dais; the core goes down as soon as the top of it is under the cross.
        mark("place");
        go(at(hall, 6.5, 1 + EYE, 12.9), at(hall, 6.5, 2.6, 9.5), 2.2);
        run(34);
        go(at(hall, 6.5, 1 + EYE, 12.7), at(hall, 6.5, 2.0, 9.5), 3.4);
        clickOn(hall.offset(6, 1, 9), Direction.UP, 30);
        run(6);
        // Off camera: a gold focus, a name, and the walls behind the player and over the vault already out of the
        // way (the shot shows the three in front being pushed out to the walls of the hall).
        BlockPos core = hall.offset(6, 2, 9);
        server.runOnServer(s -> {
            if (s.overworld().getBlockEntity(core) instanceof CoreBlockEntity c) {
                c.setFocus(new ItemStack(Items.GOLD_BLOCK));
                c.setConfig(c.config().withName("Crypt of Echoes").withExtents(new Extents(7, 3, 5, 12, 5, 5)));
            }
        });
        run(14);

        // The wand comes up while the eyes rise to the far wall; each click moves the wall a block away.
        mark("wand");
        select(1);
        go(eyeGoal, at(hall, 6.5, 4.6, 4.0), 3.4);
        run(22);
        push(6, k -> at(hall, 6.5, 4.6, 4.0 - k));
        go(eyeGoal, at(hall, 1.0, 3.2, 11.5), 3.6);
        run(20);
        push(3, k -> at(hall, 1.0 - k, 3.2, 11.5));
        go(eyeGoal, at(hall, 12.0, 3.2, 11.5), 3.2);
        run(28);
        push(3, k -> at(hall, 12.0 + k, 3.2, 11.5));
        run(6);

        // Sealed with a shard: the crystal sets, the challenge shows in the corner.
        mark("arm");
        select(2);
        go(at(hall, 6.5, 1 + EYE, 12.3), at(hall, 6.5, 2.6, 9.5), 4.4);
        run(12);
        // Off camera (the walls have faded with the wand put away): the walls one block further, inside the stone,
        // so they don't lie on the back of the niches and the window.
        server.runOnServer(s -> {
            if (s.overworld().getBlockEntity(core) instanceof CoreBlockEntity c)
                c.setConfig(c.config().withExtents(new Extents(7, 3, 11, 12, 8, 8)));
        });
        clickOn(core, null, 20);
        run(8);
        // Off camera: the zone only locks once everyone has left it, so the player steps out (onto the roof of the
        // hall), waits for the crystal to set, and comes back to the very same spot. No frames are taken meanwhile:
        // the shot carries on as if nothing happened.
        leaveUntilArmed(core, hall.offset(6, 14, 9));
        go(at(hall, 6.5, 1 + EYE, 12.6), at(hall, 6.5, 3.0, 9.5), 2.0);
        run(30);

        // Nothing inside gives: the pickaxe on a pillar, a block against the floor.
        mark("deny");
        select(3);
        go(at(hall, 3.4, 1 + EYE, 12.1), at(hall, 1.0, 3.4, 13.5), 4.2);
        run(14);
        hold(true, 24);
        select(4);
        go(eyeGoal, at(hall, 2.6, 1.0, 11.3), 5.5);
        clickOn(hall.offset(2, 0, 11), Direction.UP, 16);
        run(4);
        go(eyeGoal, at(hall, 3.6, 1.0, 10.4), 6.5);
        clickOn(hall.offset(3, 0, 10), Direction.UP, 14);
        run(14);
    }

    /**
     * Takes the player out of the zone of {@code core} to {@code outside} without filming, waits until the core has
     * armed (it waits for everyone to leave), and puts them back exactly where the camera was.
     */
    private void leaveUntilArmed(BlockPos core, BlockPos outside) {
        Vec3 back = eye[2];
        server.runCommand(String.format(Locale.ROOT, "tp @a %.3f %.3f %.3f", outside.getX() + 0.5, (double) outside.getY(),
                outside.getZ() + 0.5));
        boolean armed = false;
        for (int i = 0; i < 100 && !armed; i++) {
            ctx.waitTick();
            armed = server.computeOnServer(s -> s.overworld().getBlockEntity(core) instanceof CoreBlockEntity c
                    && c.isArmed());
        }
        log("hall core armed: " + armed);
        if (!armed)
            throw new AssertionError("the hall core did not arm after the player left");
        server.runCommand(String.format(Locale.ROOT, "tp @a %.3f %.3f %.3f", back.x, back.y - EYE, back.z));
        ctx.waitTicks(3);
        apply(true);
        ctx.waitTicks(2);
        apply(true);
    }

    /** Clicks with the wand {@code moves} times at a person's pace, the eyes following the wall as it moves. */
    private void push(int moves, java.util.function.IntFunction<Vec3> wall) {
        for (int k = 1; k <= moves; k++) {
            click(false);
            lookGoal = wall.apply(k);
            rate = 5;
            run(k == moves ? 6 : 7);
        }
    }

    /**
     * The sanctum: the floor won't break; the core asks for its seals, both go out, the core lets go, and now the
     * same floor breaks.
     */
    private void sanctumScene() {
        server.runCommand("clear @a");
        give(1, PICKAXE, 1);
        select(1);
        Vec3 start = at(sanctum, 5.5, 1 + EYE, 12.2);
        cut(start, at(sanctum, 5.5, 2.8, 3.5));
        settle(start);
        mark("sanctum");
        BlockPos floor = sanctum.offset(5, 0, 5);
        go(at(sanctum, 5.5, 1 + EYE, 8.3), at(sanctum, 5.5, 2.4, 3.5), 3.0);
        run(16);
        go(at(sanctum, 5.5, 1 + EYE, 8.1), at(sanctum, 5.5, 1.0, 5.5), 4.4);
        run(10);
        hold(true, 22); // the pickaxe on the floor: it only ripples
        select(0);
        go(at(sanctum, 5.5, 1 + EYE, 7.6), at(sanctum, 5.5, 2.5, 3.5), 4.4);
        clickOn(sanctum.offset(5, 2, 3), null, 24); // the core: not yet, a pulse to each seal
        go(eyeGoal, at(sanctum, 5.5, 2.4, 4.2), 1.4);
        run(40);
        mark("seal");
        go(at(sanctum, 4.2, 1 + EYE, 6.1), at(sanctum, 2.5, 2.45, 3.5), 4.2);
        clickOn(sanctum.offset(2, 2, 3), null, 30);
        run(14);
        go(at(sanctum, 6.8, 1 + EYE, 6.1), at(sanctum, 9.5, 2.15, 3.5), 3.8);
        clickOn(sanctum.offset(9, 2, 3), null, 40); // the lever
        run(18);
        mark("unlock");
        go(at(sanctum, 5.5, 1 + EYE, 6.8), at(sanctum, 5.5, 2.5, 3.5), 4.2);
        clickOn(sanctum.offset(5, 2, 3), null, 30);
        run(30);
        // The same floor again: now it breaks.
        mark("yours");
        select(1);
        go(at(sanctum, 5.5, 1 + EYE, 7.4), at(sanctum, 5.5, 1.0, 5.5), 4.2);
        run(14);
        hold(true, 10);
        // One block is enough: a second of stillness on the hole, and the scene ends.
        run(20);
        log("floor after: " + server.computeOnServer(s -> s.overworld().getBlockState(floor).toString()));
    }

    /**
     * Puzzle: a lava chasm before the core. A block won't bridge it; the button raises a path of stones out of the
     * lava, one after another, and the lamps along the walls light up with them.
     */
    private void bridgeScene() {
        server.runCommand("clear @a");
        give(0, "minecraft:polished_blackstone_bricks", 16);
        select(0);
        Vec3 start = at(bridge, 5.5, 1 + EYE, 19.8);
        cut(start, at(bridge, 5.5, 2.6, 2.5));
        settle(start);
        mark("puzzle");
        go(at(bridge, 5.5, 1 + EYE, 15.4), at(bridge, 5.5, 2.2, 3.0), 3.2);
        run(18);
        go(eyeGoal, at(bridge, 5.5, 0.55, 14.0), 5);
        clickOn(bridge.offset(5, 0, 14), Direction.NORTH, 20); // a block over the lava: it ripples
        run(6);
        select(1);
        // A person's turn to the button, not a snap: slow, settled before the press.
        go(at(bridge, 4.7, 1 + EYE, 15.9), at(bridge, 3.5, 2.1, 15.5), 2.0);
        run(30);
        clickOn(bridge.offset(3, 2, 15), null, 40); // the button
        // The stones rise, nearest first; the lamps follow; the eyes run along the path to the core.
        int stones = 9, lamps = 5;
        for (int t = 0; t < stones * 3 + 14; t++) {
            if (t % 3 == 0 && t / 3 < stones) {
                int z = 13 - t / 3;
                setBlock(bridge.offset(5, -3, z), "minecraft:redstone_block");
            }
            if (t % 6 == 2 && t / 6 < lamps) {
                int z = 13 - 2 * (t / 6);
                setBlock(bridge.offset(-2, 3, z), "minecraft:redstone_block");
                setBlock(bridge.offset(12, 3, z), "minecraft:redstone_block");
            }
            double k = Math.min(1, t / (double) (stones * 3));
            go(at(bridge, 5.0 + 0.5 * k, 1 + EYE, 15.6 - 0.4 * k), at(bridge, 5.5, 1.2 + 1.0 * k, 12.5 - 9.5 * k), 3.2);
            run(1);
        }
        go(at(bridge, 5.5, 1 + EYE, 10.8), at(bridge, 5.5, 2.4, 2.5), 2.4);
        run(34);
    }

    /** Parkour: the piers of the collapsed walkway; halfway, a block to bridge the gap won't go in. */
    private void cisternScene() {
        server.runCommand("clear @a");
        give(0, "minecraft:stone_bricks", 16);
        select(0);
        Vec3 start = at(cistern, 4.5, 6.0 + EYE, 22.4);
        cut(start, at(cistern, 4.5, 6.6, 17.5));
        settle(start);
        mark("parkour");
        walkTo(at(cistern, 4.5, 6.5, 20.6), at(cistern, 4.5, 6.4, 16.5), 8);
        jumpTo(at(cistern, 4.5, 6.0, 17.5), at(cistern, 2.5, 7.2, 13.5));
        jumpTo(at(cistern, 2.5, 7.0, 14.5), at(cistern, 5.0, 6.9, 12.0));
        // Why not a block across the gap? Against the next pier's side: it ripples and stays in hand. Once more, then
        // the jump after all.
        go(eyeGoal, at(cistern, 5.3, 6.4, 13.0), 3.4);
        clickOn(cistern.offset(5, 6, 12), Direction.SOUTH, 24);
        run(16);
        go(eyeGoal, at(cistern, 5.7, 6.7, 13.0), 3.4);
        clickOn(cistern.offset(5, 6, 12), Direction.SOUTH, 16);
        run(18);
        go(eyeGoal, at(cistern, 5.5, 7.2, 11.0), 3.4);
        run(12);
        jumpTo(at(cistern, 5.5, 7.0, 12.5), at(cistern, 6.5, 8.2, 8.5));
        jumpTo(at(cistern, 6.5, 8.0, 9.5), at(cistern, 3.5, 8.2, 6.5));
        run(4);
    }

    /** Walks the feet to {@code feet} in {@code ticks}, eased, looking towards {@code ahead}. */
    private void walkTo(Vec3 feet, Vec3 ahead, int ticks) {
        Vec3 from = eye[2].subtract(0, EYE, 0);
        lookGoal = ahead;
        rate = 5;
        for (int t = 1; t <= ticks; t++) {
            double k = t / (double) ticks;
            exactEye(from.lerp(feet, k * k * (3 - 2 * k)).add(0, EYE, 0));
            step();
        }
    }

    /** One jump to {@code feet} with the game's jump curve, a small dip on landing, then a breath. */
    private void jumpTo(Vec3 feet, Vec3 ahead) {
        Vec3 from = eye[2].subtract(0, EYE, 0);
        lookGoal = ahead;
        rate = 4;
        for (Vec3 p : jump(from, feet)) {
            exactEye(p.add(0, EYE, 0));
            step();
        }
        double dip = 0.07;
        for (int t = 0; t < 5; t++) {
            exactEye(feet.add(0, EYE - dip, 0));
            dip *= 0.5;
            step();
        }
    }

    /** Positions of a jump from {@code a} to {@code b} (feet), tick by tick, with the game's jump velocity and gravity. */
    private static List<Vec3> jump(Vec3 a, Vec3 b) {
        List<Double> ys = new ArrayList<>();
        double y = 0, vy = 0.42;
        double rise = b.y - a.y;
        for (int t = 0; t < 40; t++) {
            y += vy;
            vy = (vy - 0.08) * 0.98;
            if (vy < 0 && y <= rise) {
                ys.add(rise);
                break;
            }
            ys.add(y);
        }
        List<Vec3> out = new ArrayList<>();
        int n = ys.size();
        for (int t = 0; t < n; t++) {
            double k = (t + 1) / (double) n;
            // Horizontal speed eases a little at both ends, like a body leaving and meeting the ground.
            double h = 0.15 * k + 0.85 * (k * k * (3 - 2 * k));
            out.add(new Vec3(a.x + (b.x - a.x) * h, a.y + ys.get(t), a.z + (b.z - a.z) * h));
        }
        return out;
    }

    /**
     * Mazes: in through the gate, round the first turn, and the hedge between the path and the middle won't break;
     * then, flying up from beyond the gate, the wand comes into the hand and the zone fades in round the whole maze.
     */
    private void mazeScene() {
        server.runCommand("clear @a");
        give(0, PICKAXE, 1);
        select(0);
        Vec3 start = at(maze, 9.5, 1 + EYE, 21.7);
        cut(start, at(maze, 9.5, 2.2, 12.0));
        settle(start);
        mark("maze");
        // In through the gate, two steps up to the first hedge across the path, which turns west. The eyes lead the
        // body: a glance down the side passage at the first junction, then ahead and already round the coming turn.
        go(at(maze, 9.5, 1 + EYE, 15.6), at(maze, 8.0, 2.2, 13.0), 1.6);
        run(18);
        lookGoal = at(maze, 5.0, 2.3, 17.5);
        rate = 1.6;
        run(14);
        lookGoal = at(maze, 6.0, 2.2, 15.5);
        run(16);
        // A couple of steps along the passage...
        go(at(maze, 7.6, 1 + EYE, 15.5), at(maze, 4.5, 2.2, 15.2), 1.6);
        run(14);
        // ...the eyes drifting to the hedge on the right: why not straight through to the middle? It only ripples.
        lookGoal = at(maze, 7.5, 2.1, 14.4);
        rate = 1.8;
        run(26);
        hold(true, 26);
        go(eyeGoal, at(maze, 7.5, 2.6, 12.0), 1.8);
        run(20);

        creative();
        server.runCommand("clear @a");
        give(1, "zonelock:resonance_wand", 1);
        select(0);
        int above = Math.max(maze.getY() + 6, canopy(maze.getX() + 9, maze.getZ() + 32) + 2) - maze.getY();
        Vec3 high = at(maze, 9.5, above, 32.0);
        cut(high, at(maze, 9.5, 2.0, 12.0));
        settle(high);
        mark("crane");
        go(at(maze, 9.5, above + 15.0, 34.0), at(maze, 9.5, 0.5, 8.0), 1.05);
        run(22);
        select(1);
        run(92);
    }

    // ------------------------------------------------------------------ the camera

    private void cut(Vec3 e, Vec3 l) {
        for (int i = 0; i < 3; i++) {
            eye[i] = e;
            look[i] = l;
        }
        eyeGoal = e;
        lookGoal = l;
    }

    /** Moves there for real (chunks around load, the light settles) before the shot starts. */
    private void settle(Vec3 e) {
        server.runCommand(String.format(Locale.ROOT, "tp @a %.3f %.3f %.3f", e.x, e.y - EYE, e.z));
        ctx.waitTicks(4);
        apply(true);
        // Waiting for every chunk in view never ends here (the far edge keeps loading); a bounded wait, then time.
        try {
            connection.waitForChunksRender(false, 300);
        } catch (AssertionError timeout) {
            log("chunks still loading round the camera");
        }
        ctx.waitTicks(preview ? 40 : 80);
        apply(true);
        ctx.waitTick();
    }

    private void go(Vec3 e, Vec3 l, double r) {
        eyeGoal = e;
        lookGoal = l;
        rate = r;
    }

    /** Puts the camera exactly there (all stages), for shots driven by their own path. */
    private void exact(Vec3 e, Vec3 l) {
        for (int i = 0; i < 3; i++) {
            eye[i] = e;
            look[i] = l;
        }
        eyeGoal = e;
        lookGoal = l;
    }

    private void exactEye(Vec3 e) {
        for (int i = 0; i < 3; i++)
            eye[i] = e;
        eyeGoal = e;
        smoothLook();
    }

    private void smoothLook() {
        double k = 1 - Math.exp(-rate * 3 / 20.0);
        look[0] = look[0].lerp(lookGoal, k);
        look[1] = look[1].lerp(look[0], k);
        look[2] = look[2].lerp(look[1], k);
    }

    private void advance() {
        double k = 1 - Math.exp(-rate * 3 / 20.0);
        eye[0] = eye[0].lerp(eyeGoal, k);
        eye[1] = eye[1].lerp(eye[0], k);
        eye[2] = eye[2].lerp(eye[1], k);
        smoothLook();
    }

    private void run(int ticks) {
        for (int i = 0; i < ticks; i++) {
            advance();
            step();
        }
    }

    /** Sets this tick's camera (the frames in between interpolate from the last one), shoots, ticks. */
    private void step() {
        apply(false);
        server.runOnServer(s -> {
            for (var player : s.getPlayerList().getPlayers())
                player.noPhysics = true;
        });
        // Nothing but the game on screen: no toasts (the wand's tutorial, recipes), the chat is hidden.
        ctx.runOnClient(mc -> {
            mc.gui.toastManager().clear();
            try {
                var timer = net.minecraft.client.gui.Hud.class.getDeclaredField("toolHighlightTimer");
                timer.setAccessible(true);
                timer.setInt(mc.gui.hud, 0);
            } catch (ReflectiveOperationException ignored) {
            }
        });
        shoot();
        ctx.waitTick();
        tick++;
    }

    private void apply(boolean snap) {
        Vec3 e = eye[2], l = look[2];
        Vec3 d = l.subtract(e);
        float yaw = (float) (Math.toDegrees(Math.atan2(-d.x, d.z)));
        float pitch = (float) (-Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z))));
        ctx.runOnClient(mc -> {
            LocalPlayer p = mc.player;
            if (p == null)
                return;
            float last = p.getYRot();
            float y = last + net.minecraft.util.Mth.wrapDegrees(yaw - last);
            p.setPos(e.x, e.y - EYE, e.z);
            p.setYRot(y);
            p.setXRot(pitch);
            p.setYHeadRot(y);
            p.yBodyRot = y;
            p.setDeltaMovement(Vec3.ZERO);
            if (snap)
                p.setOldPosAndRot();
        });
    }

    private void shoot() {
        if (preview) {
            if (tick % 5 == 0)
                take(String.format("p%05d", tick), 1.0F);
            frame++;
            return;
        }
        for (int k = 1; k <= 3; k++)
            take(String.format("f%05d", frame++), k / 3.0F);
    }

    /**
     * Renders the frame {@code delta} of the way from the last tick to this one and saves it. Not with
     * {@code takeScreenshot}: that waits for the picture by letting the game tick (one or more ticks per picture,
     * which sped the whole world up); this waits for the GPU on the client thread, so the game stands still.
     */
    private void take(String name, float delta) {
        Path out = frames.resolve(name + ".png");
        ctx.runOnClient(mc -> {
            DeltaTracker tracker = fixedDelta(delta);
            mc.gameRenderer.update(tracker);
            mc.gameRenderer.extract(tracker, true);
            mc.gameRenderer.render();
            RenderSystem.getDevice().createCommandEncoder().submit();
            AtomicReference<NativeImage> image = new AtomicReference<>();
            Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image::set);
            long until = System.currentTimeMillis() + 10_000;
            while (image.get() == null && System.currentTimeMillis() < until) {
                RenderSystem.getDevice().createCommandEncoder().submit();
                RenderSystem.executePendingTasks();
                Thread.onSpinWait();
            }
            if (image.get() == null) {
                log("frame " + name + " lost");
                return;
            }
            try (NativeImage picture = image.get()) {
                picture.writeToFile(out);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static DeltaTracker fixedDelta(float delta) {
        try {
            var make = DeltaTracker.DefaultValue.class.getDeclaredConstructor(float.class);
            make.setAccessible(true);
            return make.newInstance(delta);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    // ------------------------------------------------------------------ input

    /** A click of use (or attack): pressed and released before the next tick, which handles it. */
    private void click(boolean attack) {
        ctx.runOnClient(mc -> {
            HitResult hit = mc.hitResult;
            log("click at tick " + tick + " -> " + (hit == null ? "nothing" : hit.getType() + " " + hit.getLocation()));
        });
        java.util.function.Function<Options, KeyMapping> key = attack ? o -> o.keyAttack : o -> o.keyUse;
        ctx.getInput().holdKey(key);
        ctx.getInput().releaseKey(key);
    }

    /**
     * Keeps the camera going until the crosshair is on {@code target} (on that face, if given), as a player clicks
     * the moment they're on it, then clicks use. Gives up and clicks anyway after {@code max} ticks.
     */
    private void clickOn(BlockPos target, @org.jspecify.annotations.Nullable Direction face, int max) {
        for (int t = 0; t < max; t++) {
            boolean on = ctx.computeOnClient(mc -> mc.hitResult instanceof BlockHitResult hit
                    && hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(target)
                    && (face == null || hit.getDirection() == face));
            if (on)
                break;
            run(1);
        }
        click(false);
    }

    private void hold(boolean attack, int ticks) {
        java.util.function.Function<Options, KeyMapping> key = attack ? o -> o.keyAttack : o -> o.keyUse;
        ctx.getInput().holdKey(key);
        run(ticks);
        ctx.getInput().releaseKey(key);
    }

    private void select(int slot) {
        ctx.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(slot));
    }

    private void give(int slot, String item, int count) {
        server.runCommand("item replace entity @a hotbar." + slot + " with " + item + " " + count);
    }

    private void survival() {
        server.runCommand("gamemode survival @a");
    }

    private void spectator() {
        server.runCommand("gamemode spectator @a");
    }

    private void creative() {
        server.runCommand("gamemode creative @a");
    }

    private void cinematic(boolean on) {
        ctx.runOnClient(mc -> {
            if (mc.gui.hud.isHidden() != on)
                mc.gui.hud.toggle();
        });
    }

    // ------------------------------------------------------------------ the log

    private void mark(String name) {
        long client = ctx.computeOnClient(mc -> mc.level.getGameTime());
        long serverTime = server.computeOnServer(s -> s.overworld().getGameTime());
        log("mark " + name + " tick " + tick + " client time " + client + " server time " + serverTime);
        write("{\"mark\":\"" + name + "\",\"frame\":" + frame + ",\"tick\":" + tick + "}");
    }

    private void log(String line) {
        System.out.println("[zonelock-trailer] " + line);
    }

    private void write(String line) {
        try {
            events.write(line);
            events.write('\n');
            events.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** Every sound the game starts, with where the camera was, for the soundtrack. */
    private final class Recorder implements SoundEventListener {
        @Override
        public void onPlaySound(SoundInstance sound, WeighedSoundEvents set, float range) {
            SoundSource source = sound.getSource();
            if (source == SoundSource.MUSIC || source == SoundSource.RECORDS || source == SoundSource.AMBIENT
                    || sound.getSound() == null)
                return;
            Minecraft mc = Minecraft.getInstance();
            Vec3 cam = mc.gameRenderer.mainCamera().position();
            float yaw = mc.player == null ? 0 : mc.player.getYRot();
            write(String.format(Locale.ROOT,
                    "{\"frame\":%d,\"sound\":\"%s\",\"event\":\"%s\",\"volume\":%.3f,\"pitch\":%.3f,\"x\":%.2f,\"y\":%.2f,"
                            + "\"z\":%.2f,\"relative\":%b,\"range\":%.1f,\"cx\":%.2f,\"cy\":%.2f,\"cz\":%.2f,\"yaw\":%.1f}",
                    frame, sound.getSound().getPath(), sound.getIdentifier(), sound.getVolume(), sound.getPitch(),
                    sound.getX(), sound.getY(), sound.getZ(), sound.isRelative(), range, cam.x, cam.y, cam.z, yaw));
        }
    }
}
