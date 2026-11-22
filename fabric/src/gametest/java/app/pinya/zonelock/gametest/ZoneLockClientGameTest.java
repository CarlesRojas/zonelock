package app.pinya.zonelock.gametest;

import java.util.Optional;

import app.pinya.zonelock.block.CoreBlock;
import app.pinya.zonelock.block.CoreBlockEntity;
import app.pinya.zonelock.block.SealBlock;
import app.pinya.zonelock.client.screen.CoreScreen;
import app.pinya.zonelock.registry.ModItems;
import app.pinya.zonelock.zone.Zone;
import app.pinya.zonelock.zone.ZoneManager;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * End-to-end test in the real client: builds a sealed zone, plays through it and takes screenshots of every step
 * (run/screenshots). Fails if protection or the unlock flow do not behave.
 */
public class ZoneLockClientGameTest implements FabricClientGameTest {
    private static final BlockPos CORE = new BlockPos(0, 200, 0);
    private static final BlockPos TOUCH_SEAL = new BlockPos(4, 200, 0);
    private static final BlockPos REDSTONE_SEAL = new BlockPos(-4, 200, 0);
    private static final BlockPos BRICKS = new BlockPos(3, 200, 3);

    @Override
    public void runTest(ClientGameTestContext ctx) {
        if (System.getenv("ZONELOCK_SHOWCASE") != null || System.getenv("ZONELOCK_GALLERY") != null
                || System.getenv("ZONELOCK_TRAILER") != null)
            return; // the showcase, gallery and trailer runs only record their own shots
        try (TestSingleplayerContext sp = ctx.worldBuilder().setUseConsistentSettings(false).create()) {
            TestServerContext server = sp.getServer();
            TestServerConnection connection = sp.getConnection();
            System.out.println("[zonelock-test] selected packs: "
                    + server.computeOnServer(s -> s.getPackRepository().getSelectedIds()));
            System.out.println("[zonelock-test] available packs: "
                    + server.computeOnServer(s -> s.getPackRepository().getAvailableIds()));
            server.runCommand("time set noon");
            server.runCommand("weather clear");
            // Spawn can be far from 0,0: load the test area before building in it.
            server.runCommand("forceload add -16 -16 15 15");
            server.runCommand("gamemode spectator @a");
            server.runCommand("tp @a 0.5 200 6.5 180 15");
            connection.waitForChunksRender();
            server.runCommand("fill -14 199 -14 14 199 14 minecraft:polished_deepslate");
            server.runCommand("fill -14 200 -14 14 210 14 minecraft:air");
            server.runCommand("setblock 3 200 3 minecraft:deepslate_bricks");
            server.runCommand("setblock -3 200 3 minecraft:amethyst_block");
            server.runCommand("setblock 0 200 0 zonelock:amethyst_core[armed=true,facing=south]{config:{name:\"Test Vault\","
                    + "extents:{up:6,down:2,north:8,south:8,east:8,west:8},rules:1023,seals:1b,redstone:0b,"
                    + "config_locked:0b},armed:1b}");
            server.runCommand("setblock 4 200 0 zonelock:amethyst_seal[lit=true]");
            server.runCommand("setblock -4 200 0 zonelock:redstone_seal[lit=true]");
            // A wall between the core and the touch seal: the energy pulse must go around it.
            server.runCommand("fill 2 200 -1 2 202 1 minecraft:deepslate_tiles");
            server.runCommand("gamemode survival @a");
            server.runCommand("tp @a 0.5 200 6.5 180 15");
            connection.waitForChunksRender();
            ctx.waitTicks(40);

            Zone zone = server.computeOnServer(s -> ZoneManager.get(s.overworld()).coreZone(CORE)).orElseThrow(
                    () -> new AssertionError("The core did not register its zone"));
            check(zone.active(), "zone should be armed");
            check(zone.lock().sealsTotal() == 2, "zone should see 2 seals, saw " + zone.lock().sealsTotal());
            ctx.takeScreenshot("zonelock_01_scene_hud");
            // The quest card only appears in the inventory.
            ctx.setScreen(() -> new net.minecraft.client.gui.screens.inventory.InventoryScreen(
                    net.minecraft.client.Minecraft.getInstance().player));
            ctx.runOnClient(mc -> net.minecraft.client.gui.components.toasts.SystemToast.add(mc.gui.toastManager(),
                    net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                    net.minecraft.network.chat.Component.literal("Open your inventory"),
                    net.minecraft.network.chat.Component.literal("Press E")));
            ctx.waitTicks(15);
            ctx.takeScreenshot("zonelock_01a_inventory_quest");
            server.runCommand("gamemode creative @a");
            ctx.waitTicks(3);
            ctx.setScreen(() -> new net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen(
                    net.minecraft.client.Minecraft.getInstance().player, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS, true));
            ctx.waitTicks(5);
            ctx.takeScreenshot("zonelock_01c_creative_quest");
            ctx.setScreen(() -> null);
            server.runCommand("gamemode survival @a");
            ctx.setScreen(() -> null);
            server.runCommand("tp @a 0.5 200 2.6 180 32");
            ctx.waitTicks(10);
            ctx.takeScreenshot("zonelock_01b_lock");
            server.runCommand("tp @a 0.5 200 6.5 180 15");
            ctx.waitTicks(5);

            // Survival player tries to break a block inside the zone.
            ctx.getInput().lookAt(BRICKS);
            ctx.waitTicks(5);
            ctx.getInput().holdKeyFor(options -> options.keyAttack, 40);
            ctx.waitTicks(3);
            ctx.takeScreenshot("zonelock_02_break_blocked");
            ctx.waitTicks(20);
            check(server.computeOnServer(s -> s.overworld().getBlockState(BRICKS).is(Blocks.DEEPSLATE_BRICKS)),
                    "protected block was broken");

            // Holding an amethyst shard reveals the border.
            server.runCommand("item replace entity @a weapon.mainhand with minecraft:amethyst_shard");
            server.runCommand("tp @a 0.5 203 14.5 180 20");
            ctx.waitTicks(20);
            ctx.takeScreenshot("zonelock_03_shard_border");
            server.runCommand("tp @a 0.5 200 10.6 180 5");
            ctx.waitTicks(10);
            ctx.takeScreenshot("zonelock_03b_shard_border_close");

            // Right-clicking the locked core: it points at the remaining seals.
            server.runCommand("item replace entity @a weapon.mainhand with minecraft:air");
            server.runCommand("tp @a 0.5 200 3.5 180 25");
            ctx.waitTicks(10);
            server.runOnServer(s -> core(s.overworld()).use(player(s)));
            server.runCommand("gamemode spectator @a");
            server.runCommand("tp @a 0.5 203.5 5.5 180 38");
            for (int frame = 0; frame < 70; frame++) {
                ctx.takeScreenshot(String.format("clip_pulse_%02d", frame));
                ctx.waitTicks(2);
            }
            ctx.takeScreenshot("zonelock_04_seal_ping");
            server.runCommand("gamemode survival @a");

            // Configuration screen (creative = map maker).
            server.runCommand("gamemode creative @a");
            ctx.waitTicks(5);
            server.runOnServer(s -> core(s.overworld()).openScreen(player(s)));
            ctx.waitForScreen(CoreScreen.class);
            ctx.takeScreenshot("zonelock_05_gui_zone");
            ctx.setScreen(() -> null);

            // With the wand in hand the zones show, edges traced in amethyst.
            server.runOnServer(s -> player(s).setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.WAND.get())));
            server.runCommand("tp @a 0.5 202 12.5 180 10");
            ctx.waitTicks(20);
            ctx.takeScreenshot("zonelock_08_wand");

            // Play through it: put out both seals, then switch the core off. The quest card with one seal out first.
            server.runCommand("gamemode survival @a");
            server.runOnServer(s -> {
                ServerLevel level = s.overworld();
                SealBlock.extinguish(level, TOUCH_SEAL, level.getBlockState(TOUCH_SEAL));
            });
            server.runCommand("item replace entity @a weapon.mainhand with minecraft:air");
            server.runCommand("tp @a 0.5 200 3.5 180 10");
            ctx.waitTicks(20);
            ctx.takeScreenshot("zonelock_07c_one_seal_out");
            server.runOnServer(s -> {
                ServerLevel level = s.overworld();
                level.setBlockAndUpdate(REDSTONE_SEAL.below(), Blocks.REDSTONE_BLOCK.defaultBlockState());
            });
            ctx.waitTicks(5);
            check(server.computeOnServer(s -> !s.overworld().getBlockState(REDSTONE_SEAL).getValue(SealBlock.LIT)),
                    "redstone seal did not go out");
            server.runCommand("item replace entity @a weapon.mainhand with minecraft:air");
            // Close-up of the armed crystal, then a clip (one frame every 2 ticks) of the core being unlocked.
            server.runCommand("tp @a 0.5 200 3.2 180 22");
            ctx.waitTicks(20);
            ctx.takeScreenshot("zonelock_08b_crystal_awake");
            server.runOnServer(s -> core(s.overworld()).attemptUnlock(player(s)));
            for (int frame = 0; frame < 40; frame++) {
                ctx.takeScreenshot(String.format("clip_unlock_%02d", frame));
                ctx.waitTicks(2);
            }
            ctx.takeScreenshot("zonelock_08c_crystal_dormant");
            check(server.computeOnServer(s -> !core(s.overworld()).isArmed()), "core should be unlocked");
            // Seals and cores give a signal whenever they aren't holding anything shut: the block beside each is powered.
            check(powered(server, TOUCH_SEAL.east()), "a seal put out should give a signal");
            check(powered(server, CORE.north()), "a switched-off core should give a signal");

            // The wand on the switched-off zone: from inside, looking at its south wall, use pushes it away (grows the
            // zone), sneak + use pulls it back; past what the focus allows it doesn't move.
            server.runCommand("tp @a 0.5 200 1.5 0 0");
            ctx.waitTicks(5);
            int south = server.computeOnServer(s -> core(s.overworld()).config().extents().south());
            server.runOnServer(s -> {
                player(s).setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.DIAMOND_BLOCK));
                core(s.overworld()).setFocus(player(s).getMainHandItem().split(1)); // focus: up to 64 a side
                player(s).setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.WAND.get()));
                ModItems.WAND.get().use(s.overworld(), player(s), InteractionHand.MAIN_HAND);
            });
            check(server.computeOnServer(s -> core(s.overworld()).config().extents().south()) == south + 1,
                    "use with the wand did not push the wall away");
            server.runOnServer(s -> {
                player(s).setShiftKeyDown(true);
                ModItems.WAND.get().use(s.overworld(), player(s), InteractionHand.MAIN_HAND);
                ModItems.WAND.get().use(s.overworld(), player(s), InteractionHand.MAIN_HAND);
                player(s).setShiftKeyDown(false);
            });
            check(server.computeOnServer(s -> core(s.overworld()).config().extents().south()) == south - 1,
                    "sneak + use with the wand did not pull the wall in");
            server.runOnServer(s -> ModItems.WAND.get().use(s.overworld(), player(s), InteractionHand.MAIN_HAND));

            // The same with real input, as a player does it. Inside, looking at the south wall: right click pushes it
            // away (grows), sneak + right click pulls it back. Outside, looking at it: right click pushes it away
            // (shrinks), sneak + right click pulls it back out.
            server.runCommand("tp @a 0.5 200 1.5 0 0");
            ctx.waitTicks(5);
            int wall = server.computeOnServer(s -> core(s.overworld()).config().extents().south());
            ctx.getInput().pressKey(options -> options.keyUse);
            ctx.waitTicks(5);
            check(server.computeOnServer(s -> core(s.overworld()).config().extents().south()) == wall + 1,
                    "right click with the wand, from inside, did not push the wall away");
            ctx.getInput().holdKey(options -> options.keyShift);
            ctx.waitTicks(3);
            ctx.getInput().pressKey(options -> options.keyUse);
            ctx.waitTicks(5);
            ctx.getInput().releaseKey(options -> options.keyShift);
            check(server.computeOnServer(s -> core(s.overworld()).config().extents().south()) == wall,
                    "sneak + right click with the wand, from inside, did not pull the wall in");
            server.runCommand("tp @a 0.5 200 12.5 180 0");
            ctx.waitTicks(5);
            ctx.getInput().pressKey(options -> options.keyUse);
            ctx.waitTicks(5);
            check(server.computeOnServer(s -> core(s.overworld()).config().extents().south()) == wall - 1,
                    "right click with the wand, from outside, did not push the wall away (shrink)");
            ctx.getInput().holdKey(options -> options.keyShift);
            ctx.waitTicks(3);
            ctx.getInput().pressKey(options -> options.keyUse);
            ctx.waitTicks(5);
            ctx.getInput().releaseKey(options -> options.keyShift);
            check(server.computeOnServer(s -> core(s.overworld()).config().extents().south()) == wall,
                    "sneak + right click with the wand, from outside, did not pull the wall back");

            // Arming it again: an amethyst shard placed on top, the crystal rises and every seal burns again.
            server.runCommand("gamemode creative @a");
            ctx.waitTicks(3);
            server.runOnServer(s -> core(s.overworld()).placeCrystal(player(s),
                    new ItemStack(net.minecraft.world.item.Items.AMETHYST_SHARD)));
            ctx.waitTicks(10);
            // Placed from inside, it waits for everyone (and any wand nearby) to be gone before it locks.
            check(server.computeOnServer(s -> core(s.overworld()).isPending() && !core(s.overworld()).isArmed()),
                    "a shard placed from inside should wait for the builder to leave");
            server.runCommand("item replace entity @a weapon.mainhand with minecraft:air");
            server.runCommand("setblock 0 199 -15 minecraft:stone_bricks");
            server.runCommand("tp @a 0.5 200 -14.5 0 25");
            for (int frame = 0; frame < 20; frame++) {
                ctx.takeScreenshot(String.format("clip_rearm_%02d", frame));
                ctx.waitTicks(2);
            }
            check(server.computeOnServer(s -> core(s.overworld()).isArmed()), "a shard should arm the core again");
            check(server.computeOnServer(s -> s.overworld().getBlockState(TOUCH_SEAL).getValue(SealBlock.LIT)),
                    "arming should relight the seals");
            ctx.waitTicks(6);
            check(!powered(server, TOUCH_SEAL.east()), "a lit seal of an armed zone should give no signal");
            check(!powered(server, CORE.north()), "an armed core should give no signal");
            ctx.takeScreenshot("zonelock_08d_rearmed");
            server.runOnServer(s -> core(s.overworld()).disarm());
            server.runCommand("gamemode survival @a");
            ctx.waitTicks(10);
            check(powered(server, TOUCH_SEAL.east()), "a lit seal of a switched-off zone should give a signal");

            // The three cores side by side.
            server.runCommand("setblock -4 200 7 zonelock:amethyst_core[facing=south]");
            server.runCommand("setblock -6 200 7 zonelock:redstone_core[facing=south]");
            server.runCommand("setblock -8 200 7 zonelock:redstone_core[facing=south,powered=true]");
            server.runCommand("tp @a -6 200.4 10.6 180 18");
            ctx.waitTicks(15);
            ctx.takeScreenshot("zonelock_09_three_cores");

            server.runCommand("tp @a 0.5 200 4.5 180 20");

            // Cores belong to nobody: a switched-off core opens its screen for anyone.
            server.runOnServer(s -> core(s.overworld()).use(player(s)));
            ctx.waitForScreen(app.pinya.zonelock.client.screen.CoreScreen.class);
            ctx.setScreen(() -> null);

            // Armed, it is a challenge for everyone, whoever armed it included: no screen, no switching it off by hand.
            server.runOnServer(s -> core(s.overworld()).arm());
            ctx.waitTicks(5);
            server.runOnServer(s -> core(s.overworld()).use(player(s)));
            ctx.waitTicks(10);
            check(!ctx.computeOnClient(mc -> mc.gui.screen() instanceof CoreScreen), "an armed core opened its screen");
            check(server.computeOnServer(s -> core(s.overworld()).isArmed()), "an armed core was switched off by hand");
            server.runOnServer(s -> core(s.overworld()).disarm());
            ctx.waitTicks(5);

            // Map lock on a switched-off core: it ignores players altogether (no screen, no shard, no message).
            server.runOnServer(s -> core(s.overworld()).setConfig(core(s.overworld()).config().withConfigLocked(true)));
            check(server.computeOnServer(s -> core(s.overworld()).ignores(player(s))), "a map-locked core reacts to players");
            check(!server.computeOnServer(s -> core(s.overworld()).placeCrystal(player(s),
                    new ItemStack(net.minecraft.world.item.Items.AMETHYST_SHARD))), "a map-locked core took a shard");
            check(!server.computeOnServer(s -> core(s.overworld()).isArmed()), "a map-locked core was armed by a player");
            server.runOnServer(s -> core(s.overworld()).setConfig(core(s.overworld()).config().withConfigLocked(false)));

            // Dungeons: place each template and check its core registers a zone.
            String[] dungeons = { "amethyst_tower" };
            for (int i = 0; i < dungeons.length; i++) {
                int x = 200 + i * 120;
                String id = dungeons[i];
                check(server.computeOnServer(s -> s.getStructureTemplateManager()
                        .get(app.pinya.zonelock.ZoneLock.id(id)).isPresent()), "template missing: " + id);
                server.runCommand("gamemode spectator @a");
                server.runCommand("forceload add " + (x - 16) + " -16 " + (x + 32) + " 48"); // cores tick to register
                server.runCommand("tp @a " + (x + 6) + " 172 -22 0 30");
                connection.waitForChunksRender();
                int px = x;
                String result = server.computeOnServer(s -> {
                    ServerLevel level = s.overworld();
                    var template = s.getStructureTemplateManager().get(app.pinya.zonelock.ZoneLock.id(id)).orElseThrow();
                    BlockPos origin = new BlockPos(px, 150, 0);
                    boolean placed = template.placeInWorld(level, origin, origin,
                            new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),
                            level.getRandom(), 2);
                    int here = 0;
                    for (BlockPos p : BlockPos.betweenClosed(px, 150, 0, px + 10, 177, 10))
                        if (!level.getBlockState(p).isAir()) here++;
                    var igloo = s.getStructureTemplateManager().get(net.minecraft.resources.Identifier.withDefaultNamespace("igloo/top")).orElseThrow();
                    BlockPos o2 = new BlockPos(px, 150, 30);
                    boolean p2 = igloo.placeInWorld(level, o2, o2,
                            new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),
                            level.getRandom(), 2);
                    int igl = 0;
                    for (BlockPos p : BlockPos.betweenClosed(px, 150, 30, px + 10, 160, 40))
                        if (!level.getBlockState(p).isAir()) igl++;
                    var settings0 = new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings();
                    String filt = "cores=" + template.filterBlocks(BlockPos.ZERO, settings0, app.pinya.zonelock.registry.ModBlocks.CORE.get()).size()
                            + " deepslate=" + template.filterBlocks(BlockPos.ZERO, settings0, Blocks.DEEPSLATE_TILES).size()
                            + " air=" + template.filterBlocks(BlockPos.ZERO, settings0, Blocks.AIR).size();
                    return filt + " placed=" + placed + " blocksNow=" + here + " igloo=" + p2 + "/" + igl + " size=" + template.getSize();
                });
                System.out.println("[zonelock-test] " + id + ": " + result);
                ctx.waitTicks(60);
                System.out.println("[zonelock-test] after: " + server.computeOnServer(s -> {
                    ServerLevel level = s.overworld();
                    StringBuilder out = new StringBuilder();
                    for (BlockPos p : BlockPos.betweenClosed(px, 150, 0, px + 20, 190, 20))
                        if (level.getBlockEntity(p) instanceof CoreBlockEntity c)
                            out.append("core@").append(p.toShortString()).append(" armed=").append(c.isArmed())
                                    .append(" state=").append(level.getBlockState(p)).append("; ");
                    out.append("zones=").append(ZoneManager.get(level).index().all().stream()
                            .map(z -> z.anchor().toShortString() + "/" + z.active()).toList());
                    int solid = 0;
                    java.util.Map<String, Integer> kinds = new java.util.TreeMap<>();
                    for (BlockPos p : BlockPos.betweenClosed(px - 2, 140, -10, px + 20, 190, 20)) {
                        var st = level.getBlockState(p);
                        if (!st.isAir()) {
                            solid++;
                            kinds.merge(st.getBlock().toString(), 1, Integer::sum);
                        }
                    }
                    out.append(" solid=").append(solid).append(" kinds=").append(kinds.size() > 12 ? kinds.keySet().stream().limit(12).toList() : kinds);
                    var tpl = s.getStructureTemplateManager().get(app.pinya.zonelock.ZoneLock.id(id)).orElseThrow();
                    out.append(" tplSize=").append(tpl.getSize());
                    out.append(" ticking=").append(level.shouldTickBlocksAt(new BlockPos(px + 5, 171, 5)));
                    return out.toString();
                }));
                ctx.takeScreenshot("zonelock_1" + i + "_" + dungeons[i]);
                int fx = x;
                boolean hasZone = server.computeOnServer(s -> ZoneManager.get(s.overworld()).index().all().stream()
                        .anyMatch(z -> z.active() && Math.abs(z.anchor().getX() - fx) < 64));
                check(hasZone, dungeons[i] + " core did not register an armed zone");
            }

            // Worldgen: real structure generation through the data pack JSON.
            server.runCommand("gamemode spectator @a");
            server.runCommand("tp @a 3000 140 0");
            connection.waitForChunksRender();
            server.runOnServer(s -> {
                var source = s.createCommandSourceStack().withSource(new net.minecraft.commands.CommandSource() {
                    @Override
                    public void sendSystemMessage(net.minecraft.network.chat.Component message) {
                        System.out.println("[zonelock-test] command: " + message.getString());
                    }

                    @Override
                    public boolean acceptsSuccess() {
                        return true;
                    }

                    @Override
                    public boolean acceptsFailure() {
                        return true;
                    }

                    @Override
                    public boolean shouldInformAdmins() {
                        return false;
                    }
                });
                s.getCommands().performPrefixedCommand(source, "forceload add 2950 -60 3170 60");
                s.getCommands().performPrefixedCommand(source, "place structure zonelock:amethyst_tower 3000 100 0");
            });
            server.runCommand("tp @a 3008 140 -12 0 40");
            connection.waitForChunksRender();
            ctx.waitTicks(100);
            String summary = server.computeOnServer(s -> ZoneManager.get(s.overworld()).index().all().stream()
                    .filter(z -> z.anchor().getX() > 2900)
                    .map(z -> z.name() + " active=" + z.active())
                    .toList().toString());
            System.out.println("[zonelock-test] worldgen zones: " + summary);
            check(summary.contains("Amethyst Tower active=true"), "placed tower has no armed zone");
            ctx.takeScreenshot("zonelock_20_worldgen");
        }
    }

    private static CoreBlockEntity core(ServerLevel level) {
        return Optional.ofNullable(level.getBlockEntity(CORE)).filter(CoreBlockEntity.class::isInstance)
                .map(CoreBlockEntity.class::cast).orElseThrow(() -> new AssertionError("no core block entity"));
    }

    private static ServerPlayer player(net.minecraft.server.MinecraftServer server) {
        return server.getPlayerList().getPlayers().getFirst();
    }

    /** Whether a redstone signal reaches {@code pos} from any side. */
    private static boolean powered(TestServerContext server, BlockPos pos) {
        return server.computeOnServer(s -> s.overworld().hasNeighborSignal(pos));
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }
}
