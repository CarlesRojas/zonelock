package app.pinya.zonelock.gametest.showcase;

import app.pinya.zonelock.block.CoreBlockEntity;
import app.pinya.zonelock.client.render.BlockRipples;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Close-ups of every block and item of the mod (gallery_*.png): each block from a three-quarter view (front, side and
 * top) and from the front, and a close-up clip of the energy pulse (gallery_pulse_*.png, one frame per tick). Only runs
 * with the environment variable {@code ZONELOCK_GALLERY=1}.
 */
public class GalleryTest implements FabricClientGameTest {
    private static final double EYE = 1.62;
    private static final int Y = 201;
    private static final String ARMED = "{config:{name:\"Gallery\",extents:{up:1,down:1,north:1,south:1,east:1,west:1},"
            + "rules:1023,seals:0b,redstone:%s,config_locked:0b},armed:1b}";

    private ClientGameTestContext ctx;
    private TestServerContext server;
    private TestServerConnection connection;

    @Override
    public void runTest(ClientGameTestContext context) {
        if (System.getenv("ZONELOCK_GALLERY") == null)
            return;
        ctx = context;
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext sp = ctx.worldBuilder().setUseConsistentSettings(false).create()) {
            server = sp.getServer();
            connection = sp.getConnection();
            server.runCommand("time set noon");
            server.runCommand("weather clear");
            server.runCommand("gamerule doDaylightCycle false");
            server.runCommand("forceload add -16 -16 100 66");
            server.runCommand("gamemode spectator @a");
            server.runCommand("tp @a 0 210 0");
            connection.waitForChunksRender();
            server.runCommand("fill -16 " + (Y - 1) + " -16 100 " + (Y - 1) + " 66 minecraft:polished_deepslate");
            server.runCommand("fill -16 " + Y + " -16 100 " + (Y + 12) + " 66 minecraft:air");
            ctx.runOnClient(mc -> {
                if (!mc.gui.hud.isHidden())
                    mc.gui.hud.toggle();
            });

            int x = 0;
            String plain = ARMED.formatted("0b");
            block(x, "zonelock:amethyst_core[armed=true,facing=north]" + plain, "01_amethyst_core_armed");
            block(x += 6, "zonelock:amethyst_core[armed=false,facing=north]", "02_amethyst_core_off");
            block(x += 6, "zonelock:redstone_core[armed=true,facing=north]" + ARMED.formatted("1b"),
                    "03_redstone_core_armed");
            block(x += 6, "zonelock:redstone_core[armed=false,facing=north]", "04_redstone_core_off");
            block(x += 6, "zonelock:redstone_core[armed=false,facing=north,powered=true]", "06_redstone_core_powered");
            block(x += 6, "zonelock:amethyst_seal[lit=true]", "08_amethyst_seal_lit");
            block(x += 6, "zonelock:amethyst_seal[lit=false]", "09_amethyst_seal_out");
            block(x += 6, "zonelock:redstone_seal[lit=true]", "10_redstone_seal_lit");
            block(x += 6, "zonelock:redstone_seal[lit=false]", "11_redstone_seal_out");
            block(x += 6, "zonelock:redstone_seal[lit=true,powered=true]", "11b_redstone_seal_powered");

            // The three cores side by side.
            int z = 12;
            server.runCommand("setblock 0 " + Y + " " + z + " zonelock:amethyst_core[armed=true,facing=north]" + plain);
            server.runCommand("setblock 2 " + Y + " " + z + " zonelock:redstone_core[armed=true,facing=north]"
                    + ARMED.formatted("1b"));
            shoot(new Vec3(1.5, Y + 0.5, z + 0.5), new Vec3(3.6, Y + 2.6, z - 3.4), "12_two_cores");

            // Items, as item displays turned towards the camera.
            z = 24;
            item(2, z, "zonelock:resonance_wand", "");
            item(3, z, "zonelock:core_crystal", "");
            shoot(new Vec3(3, Y + 0.6, z + 0.5), new Vec3(3, Y + 0.9, z - 3.2), "13_items");

            // Core shape prototypes, when the run installed them on the facings (east, south) of the Amethyst Core.
            if (System.getenv("ZONELOCK_CORE_SHAPES") != null) {
                String[][] shapes = { { "cube", "north" }, { "stepped", "east" }, { "column", "south" } };
                x = 0;
                for (String[] shape : shapes) {
                    server.runCommand("setblock " + x + " " + Y + " -12 zonelock:amethyst_core[armed=true,facing="
                            + shape[1] + "]" + plain);
                    server.runCommand("setblock " + x + " " + Y + " -16 zonelock:amethyst_core[armed=false,facing="
                            + shape[1] + "]");
                    Vec3 center = new Vec3(x + 0.5, Y + 0.5, -11.5);
                    shoot(center, center.add(1.05, 0.85, -1.05), "30_shape_" + shape[0] + "_iso");
                    shoot(center, center.add(0, 0.35, -1.9), "30_shape_" + shape[0] + "_front");
                    Vec3 off = new Vec3(x + 0.5, Y + 0.5, -15.5);
                    shoot(off, off.add(1.05, 0.85, -1.05), "31_shape_" + shape[0] + "_off_iso");
                    x += 4;
                }
            }

            // The energy pulse, close up: one frame per tick, assembled into a clip afterwards.
            z = 40;
            server.runCommand("setblock 0 " + Y + " " + z + " zonelock:amethyst_core[armed=true,facing=east]{config:{"
                    + "name:\"Pulse\",extents:{up:4,down:1,north:8,south:8,east:8,west:8},rules:1023,seals:1b,"
                    + "redstone:0b,config_locked:0b},armed:1b}");
            server.runCommand("setblock 6 " + Y + " " + z + " zonelock:amethyst_seal[lit=true]");
            server.runCommand("setblock -1 " + Y + " " + (z + 6) + " zonelock:redstone_seal[lit=true]");
            server.runCommand("fill -3 " + Y + " " + (z + 3) + " 1 " + (Y + 1) + " " + (z + 3) + " minecraft:deepslate_tiles");
            server.runCommand("gamemode survival @a");
            server.runCommand("tp @a 2.5 " + Y + " " + (z + 0.5) + " 90 20");
            ctx.waitTicks(20);
            BlockPos core = new BlockPos(0, Y, z);
            server.runOnServer(s -> {
                if (s.overworld().getBlockEntity(core) instanceof CoreBlockEntity entity)
                    entity.use(s.getPlayerList().getPlayers().getFirst());
            });
            server.runCommand("gamemode spectator @a");
            Vec3 view = new Vec3(5.5, Y + 3.6, z + 8.5);
            Vec3 aim = new Vec3(1.5, Y + 0.2, z + 2.5);
            server.runCommand("tp @a " + view.x + " " + (view.y - EYE) + " " + view.z + " facing " + aim.x + " "
                    + (aim.y - EYE) + " " + aim.z);
            for (int frame = 0; frame < 130; frame++) {
                ctx.takeScreenshot(String.format("gallery_pulse_%03d", frame));
                ctx.waitTick();
            }

            // Ripples where a block refused to change, on blocks of every shape, hit from the camera: one frame per tick.
            server.runCommand("gamemode spectator @a");
            String[][] groups = {
                    { "minecraft:torch", "minecraft:oak_fence", "minecraft:stone_brick_stairs[facing=west]",
                            "minecraft:lantern" },
                    { "minecraft:anvil", "minecraft:potted_poppy", "minecraft:cobblestone_wall", "minecraft:brewing_stand" },
                    { "zonelock:amethyst_core[armed=true,facing=north]" + plain, "zonelock:amethyst_seal[lit=true]",
                            "minecraft:cauldron", "minecraft:deepslate_bricks" } };
            for (int g = 0; g < groups.length; g++) {
                int rz = 50 + g * 5;
                for (int i = 0; i < 4; i++)
                    server.runCommand("setblock " + (i * 2) + " " + Y + " " + rz + " " + groups[g][i]);
                Vec3 middle = new Vec3(3.5, Y + 0.4, rz + 0.5);
                Vec3 camera = new Vec3(3.5, Y + 1.6, rz - 3.6);
                server.runCommand("tp @a " + camera.x + " " + (camera.y - EYE) + " " + camera.z + " facing " + middle.x
                        + " " + (middle.y - EYE) + " " + middle.z);
                connection.waitForChunksRender();
                ctx.waitTicks(30);
                int row = rz;
                ctx.runOnClient(mc -> {
                    long now = mc.level.getGameTime();
                    for (int i = 0; i < 4; i++)
                        ripple(mc, new BlockPos(i * 2, Y, row), camera, now);
                });
                long first = ctx.computeOnClient(mc -> mc.level.getGameTime());
                for (int frame = 0; frame < 24; frame++) {
                    ctx.takeScreenshot(String.format("gallery_ripple%d_%03d", g, frame));
                    ctx.waitTick();
                }
                long last = ctx.computeOnClient(mc -> mc.level.getGameTime());
                System.out.println("[zonelock-gallery] ripple clip " + g + ": 24 frames over " + (last - first) + " ticks");
            }

            // Things you walk through ripple over their model (wheat like wheat, not like its box), and an item frame
            // over its own plate.
            {
                int rz = 45;
                server.runCommand("setblock 0 " + (Y - 1) + " " + rz + " minecraft:farmland");
                server.runCommand("fill 2 " + (Y - 1) + " " + rz + " 4 " + (Y - 1) + " " + rz + " minecraft:grass_block");
                server.runCommand("setblock 0 " + Y + " " + rz + " minecraft:wheat[age=7]");
                server.runCommand("setblock 2 " + Y + " " + rz + " minecraft:poppy");
                server.runCommand("setblock 4 " + Y + " " + rz + " minecraft:rail");
                server.runCommand("setblock 6 " + Y + " " + rz + " minecraft:stone_bricks");
                server.runCommand("summon item_frame 6 " + Y + " " + (rz - 1)
                        + " {Facing:2b,Item:{id:\"minecraft:diamond\",count:1}}");
                Vec3 middle = new Vec3(3.5, Y + 0.4, rz + 0.5);
                Vec3 camera = new Vec3(3.5, Y + 1.6, rz - 3.6);
                server.runCommand("tp @a " + camera.x + " " + (camera.y - EYE) + " " + camera.z + " facing " + middle.x
                        + " " + (middle.y - EYE) + " " + middle.z);
                connection.waitForChunksRender();
                ctx.waitTicks(30);
                ctx.runOnClient(mc -> {
                    long now = mc.level.getGameTime();
                    for (int i = 0; i < 3; i++)
                        BlockRipples.start(mc.level, new BlockPos(i * 2, Y, rz), new Vec3(i * 2 + 0.5, Y + 0.4, rz + 0.5), now);
                    for (var frame : mc.level.getEntitiesOfClass(net.minecraft.world.entity.decoration.ItemFrame.class,
                            new net.minecraft.world.phys.AABB(5, Y - 1, rz - 2, 8, Y + 2, rz + 1)))
                        BlockRipples.startOnBox(frame.blockPosition(), frame.getBoundingBox(),
                                frame.getBoundingBox().getCenter(), now);
                });
                for (int frame = 0; frame < 24; frame++) {
                    ctx.takeScreenshot(String.format("gallery_ripple3_%03d", frame));
                    ctx.waitTick();
                }
            }

            // A wall of identical blocks, hit once in the middle; then the same wall with a floor at its foot, hit next to
            // the corner, a torch and a lantern on it, hit beside them (their own surfaces don't touch the wall's).
            server.runCommand("fill -2 " + Y + " 64 8 " + (Y + 4) + " 64 minecraft:stone_bricks");
            Vec3 wallEye = new Vec3(3.5, Y + 2.5, 61.2);
            Vec3 wallAim = new Vec3(3.5, Y + 2.0, 64.0);
            server.runCommand("tp @a " + wallEye.x + " " + (wallEye.y - EYE) + " " + wallEye.z + " facing " + wallAim.x
                    + " " + (wallAim.y - EYE) + " " + wallAim.z);
            connection.waitForChunksRender();
            ctx.waitTicks(30);
            ctx.runOnClient(mc -> ripple(mc, new BlockPos(3, Y + 2, 64), wallEye, mc.level.getGameTime()));
            for (int frame = 0; frame < 16; frame++) {
                ctx.takeScreenshot(String.format("gallery_wall_%03d", frame));
                ctx.waitTick();
            }
            server.runCommand("fill -2 " + Y + " 60 8 " + Y + " 63 minecraft:stone_bricks");
            server.runCommand("setblock 1 " + (Y + 2) + " 63 minecraft:wall_torch[facing=north]");
            server.runCommand("setblock 5 " + (Y + 3) + " 63 minecraft:lantern[hanging=false]");
            server.runCommand("setblock 5 " + (Y + 2) + " 63 minecraft:stone_bricks");
            ctx.waitTicks(10);
            ctx.runOnClient(mc -> {
                long now = mc.level.getGameTime();
                ripple(mc, new BlockPos(3, Y + 1, 64), wallEye, now);       // just above the floor
                ripple(mc, new BlockPos(1, Y + 2, 64), wallEye, now);       // the wall behind the torch
                ripple(mc, new BlockPos(5, Y + 3, 64), wallEye, now);       // the wall behind the lantern
            });
            for (int frame = 0; frame < 16; frame++) {
                ctx.takeScreenshot(String.format("gallery_wall2_%03d", frame));
                ctx.waitTick();
            }

            // The forcefield, one look after another, from the same spot: an armed zone, the wand in hand, a wall and
            // some grass just outside the border so the field shows against blocks and against the sky.
            {
                int cx = 100, cz = 100;
                server.runCommand("forceload add 80 80 130 130");
                server.runCommand("fill " + (cx - 12) + " " + (Y - 1) + " " + (cz - 8) + " " + (cx + 12) + " " + (Y - 1) + " "
                        + (cz + 14) + " minecraft:grass_block");
                server.runCommand("fill " + (cx - 12) + " " + Y + " " + (cz - 8) + " " + (cx + 12) + " " + (Y + 8) + " "
                        + (cz + 14) + " minecraft:air");
                server.runCommand("fill " + (cx - 8) + " " + Y + " " + (cz + 10) + " " + (cx + 8) + " " + (Y + 2) + " "
                        + (cz + 10) + " minecraft:stone_bricks");
                server.runCommand("setblock " + cx + " " + Y + " " + cz + " zonelock:amethyst_core[armed=true,facing=south]"
                        + "{config:{name:\"Campo\",extents:{up:6,down:1,north:6,south:6,east:6,west:6},rules:1023},armed:1b}");
                server.runCommand("gamemode survival @a");
                server.runCommand("item replace entity @a weapon.mainhand with zonelock:resonance_wand");
                server.runCommand("tp @a " + (cx + 2.5) + " " + Y + " " + (cz + 3.5) + " 20 8");
                connection.waitForChunksRender();
                ctx.waitTicks(40);
                // Armed: the runes slide and the curtain rises.
                for (int frame = 0; frame < 40; frame++) {
                    ctx.takeScreenshot(String.format("gallery_field_on_%03d", frame));
                    ctx.waitTicks(2);
                }
                // Without the wand, close to the wall: a patch of it around the player, fading out.
                server.runCommand("item replace entity @a weapon.mainhand with minecraft:air");
                server.runCommand("tp @a " + (cx + 2.5) + " " + Y + " " + (cz + 4.6) + " 15 10");
                ctx.waitTicks(10);
                ctx.takeScreenshot("gallery_field_near");
                server.runCommand("tp @a " + (cx + 2.5) + " " + Y + " " + (cz + 5.6) + " 15 10");
                ctx.waitTicks(10);
                ctx.takeScreenshot("gallery_field_near_close");
                server.runCommand("item replace entity @a weapon.mainhand with zonelock:resonance_wand");
                server.runCommand("tp @a " + (cx + 2.5) + " " + Y + " " + (cz + 3.5) + " 20 8");
                ctx.waitTicks(5);
                // Switched off: the runes stand still.
                server.runOnServer(s -> {
                    if (s.overworld().getBlockEntity(new BlockPos(cx, Y, cz)) instanceof app.pinya.zonelock.block.CoreBlockEntity c)
                        c.disarm();
                });
                ctx.waitTicks(10);
                // Switched off and pointed at by the wand: that wall at full colour, still.
                for (int frame = 0; frame < 4; frame++) {
                    ctx.takeScreenshot(String.format("gallery_field_off_%03d", frame));
                    ctx.waitTicks(2);
                }
                // Switched off, the wand pointing at the ground: every wall dim.
                server.runCommand("tp @a " + (cx + 2.5) + " " + Y + " " + (cz + 3.5) + " 20 55");
                ctx.waitTicks(10);
                ctx.takeScreenshot("gallery_field_off_dim");
                // From outside the zone, looking back at it: the edges traced all round.
                server.runCommand("tp @a " + (cx + 11.5) + " " + (Y + 3) + " " + (cz - 6.5) + " 50 20");
                ctx.waitTicks(10);
                ctx.takeScreenshot("gallery_field_off_outside");
                // The core's screen, as a player sees it: an iron block in the focus slot.
                server.runCommand("tp @a " + (cx + 0.5) + " " + Y + " " + (cz + 2.5) + " 180 30");
                ctx.waitTicks(5);
                server.runOnServer(s -> {
                    if (s.overworld().getBlockEntity(new BlockPos(cx, Y, cz)) instanceof app.pinya.zonelock.block.CoreBlockEntity c) {
                        c.setFocus(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_BLOCK));
                        c.openScreen(s.getPlayerList().getPlayers().getFirst());
                    }
                });
                ctx.waitForScreen(app.pinya.zonelock.client.screen.CoreScreen.class);
                ctx.waitTicks(5);
                ctx.takeScreenshot("gallery_core_screen");
                ctx.setScreen(() -> null);
            }
        }
    }

    /** Rings on the block at {@code pos} where a look from {@code eye} at its middle meets its solid shape. */
    private static void ripple(net.minecraft.client.Minecraft mc, BlockPos pos, Vec3 eye, long now) {
        var shape = BlockRipples.surface(mc.level.getBlockState(pos), mc.level, pos);
        Vec3 aim = shape.isEmpty() ? Vec3.atCenterOf(pos) : shape.bounds().getCenter().add(Vec3.atLowerCornerOf(pos));
        var hit = shape.clip(eye, eye.add(aim.subtract(eye).scale(2)), pos);
        BlockRipples.start(mc.level, pos, hit != null ? hit.getLocation() : aim, now);
    }

    /** A block with a three-quarter view (front, east side and top) and a front view. */
    private void block(int x, String block, String name) {
        server.runCommand("setblock " + x + " " + Y + " 0 " + block);
        Vec3 center = new Vec3(x + 0.5, Y + 0.5, 0.5);
        shoot(center, center.add(1.05, 0.85, -1.05), name + "_iso");
        shoot(center, center.add(0, 0.35, -1.9), name + "_front");
    }

    private void item(int x, int z, String id, String components) {
        server.runCommand("summon minecraft:item_display " + (x + 0.5) + " " + (Y + 0.6) + " " + (z + 0.5)
                + " {item:{id:\"" + id + "\",count:1" + components + "},item_display:\"fixed\",billboard:\"vertical\","
                + "brightness:{sky:15,block:15}}");
    }

    private void shoot(Vec3 target, Vec3 eye, String name) {
        Vec3 feet = eye.subtract(0, EYE, 0);
        Vec3 look = target.subtract(0, EYE, 0);
        server.runCommand("tp @a " + feet.x + " " + feet.y + " " + feet.z + " facing " + look.x + " " + look.y + " "
                + look.z);
        connection.waitForChunksRender();
        ctx.waitTicks(25);
        ctx.takeScreenshot("gallery_" + name);
    }
}
