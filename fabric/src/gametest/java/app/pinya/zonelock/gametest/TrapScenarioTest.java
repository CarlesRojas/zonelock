package app.pinya.zonelock.gametest;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import app.pinya.zonelock.block.CoreBlockEntity;
import app.pinya.zonelock.block.SealBlock;
import app.pinya.zonelock.gametest.showcase.TrapScenarios;
import app.pinya.zonelock.gametest.showcase.TrapScenarios.Site;
import app.pinya.zonelock.registry.ModItems;
import app.pinya.zonelock.zone.Rule;
import app.pinya.zonelock.zone.ZoneGuard;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Every way we know of to shut yourself in while setting up a dungeon (see {@link TrapScenarios}), played through: the
 * builder arms the core from inside, and must always be able to walk out (checked by a walk search through the real
 * blocks); once out, the zone locks and the dungeon stands as it should; a challenger can then solve it and walk out.
 */
public class TrapScenarioTest implements FabricClientGameTest {
    private static final int Y = 150;

    private ClientGameTestContext ctx;
    private TestServerContext server;
    private int failures;

    @Override
    public void runTest(ClientGameTestContext context) {
        ctx = context;
        try (TestSingleplayerContext sp = ctx.worldBuilder().setUseConsistentSettings(false)
                .adjustSettings(s -> s.setAllowCommands(true)).create()) {
            server = sp.getServer();
            for (String rule : new String[] { "advance_time false", "spawn_monsters false", "send_command_feedback false",
                    "immediate_respawn true", "keep_inventory true", "fall_damage false" })
                server.runCommand("gamerule " + rule);
            server.runCommand("forceload add -16 -16 32 400");
            server.runCommand("gamemode survival @a");
            server.runCommand("setworldspawn 8 " + Y + " -40");

            int z = 0;
            sealDoor(TrapScenarios.sealDoor(server, origin(z)));
            leverDoor(TrapScenarios.leverDoor(server, origin(z += 30)));
            leverLeftOn(TrapScenarios.leverDoor(server, origin(z += 30)));
            chain(TrapScenarios.chain(server, origin(z += 30)));
            dropCore(TrapScenarios.dropCore(server, origin(z += 30)));
            dropSeal(TrapScenarios.dropSeal(server, origin(z += 30)));
            oneSided(TrapScenarios.oneSided(server, origin(z += 30)));
            outside(TrapScenarios.outside(server, origin(z += 30)));
            loggedOutInside(TrapScenarios.sealDoor(server, origin(z += 30)));
            cancel(TrapScenarios.sealDoor(server, origin(z += 30)));
            bed(TrapScenarios.bed(server, origin(z += 30)));
            if (failures > 0)
                throw new AssertionError(failures + " trap scenario check(s) failed, see [zonelock-traps] lines");
            log("all trap scenarios passed");
        }
    }

    private static BlockPos origin(int z) {
        return new BlockPos(0, Y, z);
    }

    // ------------------------------------------------------------------ scenarios

    /** Arm from the core's room: the seal over the door holds it open until the builder is out. */
    private void sealDoor(Site site) {
        settle(site);
        armFromInside(site);
        check(site, doorOpen(site.part("door1")), "while pending the seal keeps the door open");
        walkOut(site);
        check(site, armed(site), "locks once the builder is out");
        check(site, !doorOpen(site.part("door1")), "the door shuts when the zone locks");
        // The challenger: the seal (reachable from the first room), then the core.
        enter(site, site.outside(), new BlockPos(site.part("seal").getX(), Y, site.part("seal").getZ() - 1));
        touch(site.part("seal"));
        check(site, doorOpen(site.part("door1")), "putting the seal out opens the door");
        reachAndUnlock(site);
    }

    /** The user's case: one lever opens the door and puts the redstone seal out; the builder leaves it down. */
    private void leverDoor(Site site) {
        settle(site);
        BlockPos lever = site.part("lever");
        check(site, !doorOpen(site.part("door1")), "the lever is down, the door shut");
        touch(lever); // up: through the door
        check(site, doorOpen(site.part("door1")), "the lever opens the door");
        check(site, sealLit(site.part("seal")), "while not armed the lever doesn't put the seal out");
        armFromInside(site);
        // Back to the first room, lever down, out.
        check(site, canWalk(site.inside(), lever.below(2)), "from the core back to the lever");
        teleport(lever.below(2));
        touch(lever);
        check(site, !doorOpen(site.part("door1")), "lever down: door shut");
        walkOutFrom(site, lever.below(2), site.outside());
        check(site, armed(site), "locks once the builder is out");
        check(site, sealLit(site.part("seal")), "the seal burns");
        enter(site, site.outside(), lever.below(2));
        touch(lever);
        check(site, doorOpen(site.part("door1")) && !sealLit(site.part("seal")),
                "the lever opens the door and puts the seal out");
        reachAndUnlock(site);
    }

    /** The builder walks out with the lever left up: the door stands open, but the dungeon can still be beaten. */
    private void leverLeftOn(Site site) {
        settle(site);
        BlockPos lever = site.part("lever");
        touch(lever);
        armFromInside(site);
        walkOut(site);
        check(site, armed(site), "locks once the builder is out");
        check(site, sealLit(site.part("seal")), "a seal already powered when the zone locks stays lit");
        enter(site, site.outside(), lever.below(2));
        touch(lever);
        touch(lever);
        check(site, !sealLit(site.part("seal")), "down and up again puts the seal out");
        reachAndUnlock(site);
    }

    /** Three rooms, each seal opening the next door: all open while pending, all shut once locked, then in order. */
    private void chain(Site site) {
        settle(site);
        armFromInside(site);
        check(site, doorOpen(site.part("door1")) && doorOpen(site.part("door2")), "while pending every door is open");
        walkOut(site);
        check(site, armed(site), "locks once the builder is out");
        check(site, !doorOpen(site.part("door1")), "the first door shuts");
        check(site, !doorOpen(site.part("door2")), "the second door shuts");
        BlockPos seal1 = site.part("seal1"), seal2 = site.part("seal2");
        enter(site, site.outside(), new BlockPos(seal1.getX(), Y, seal1.getZ() - 1));
        touch(seal1);
        check(site, doorOpen(site.part("door1")) && !doorOpen(site.part("door2")), "the first seal opens the first door");
        enter(site, new BlockPos(seal1.getX(), Y, seal1.getZ() - 1), new BlockPos(seal2.getX(), Y, seal2.getZ() - 1));
        touch(seal2);
        check(site, doorOpen(site.part("door2")), "the second seal opens the second door");
        reachAndUnlock(site);
    }

    /** Dropped in through the roof; the only exit is opened by the switched-off core: open while pending. */
    private void dropCore(Site site) {
        settle(site);
        armFromInside(site);
        check(site, doorOpen(site.part("exit")), "while pending the core keeps the exit open");
        walkOutTo(site, site.part("beyond"));
        check(site, armed(site), "locks once the builder is out");
        check(site, !doorOpen(site.part("exit")), "the exit shuts when the zone locks");
        teleport(site.inside());
        unlock(site);
        check(site, doorOpen(site.part("exit")), "beating the core opens the exit");
        check(site, canWalk(site.inside(), site.part("beyond")), "the challenger walks out");
    }

    /** As above with a seal: its door is the way out for the builder (pending) and for the challenger (put out). */
    private void dropSeal(Site site) {
        settle(site);
        armFromInside(site);
        check(site, doorOpen(site.part("exit")), "while pending the seal keeps the exit open");
        walkOutTo(site, site.part("beyond"));
        check(site, armed(site), "locks once the builder is out");
        teleport(site.inside());
        check(site, !canWalk(site.inside(), site.part("beyond")), "a challenger who drops in is shut in until the seal");
        touch(site.part("seal"));
        check(site, canWalk(site.inside(), site.part("beyond")), "putting the seal out opens the way out");
        unlock(site);
    }

    /**
     * A door with its button only outside: shut in after arming. While pending the zone can still be edited, so the
     * builder breaks the door, walks out, puts it back with the wand in hand (the zone waits), and puts the wand away.
     */
    private void oneSided(Site site) {
        settle(site);
        BlockPos door = site.part("door");
        teleport(site.outside());
        touch(site.part("button"));
        check(site, doorOpen(door), "the button outside opens the door");
        check(site, canWalk(site.outside(), site.inside()), "and lets the builder in");
        teleport(site.inside());
        ctx.waitTicks(30); // the button springs back, the door shuts
        armFromInside(site);
        check(site, !canWalk(site.inside(), site.outside()), "shut in: no button inside");
        check(site, !server.computeOnServer(s -> ZoneGuard.forbids(s.overworld(), door, Rule.BREAK)),
                "a pending zone can still be edited");
        server.runOnServer(s -> s.overworld().destroyBlock(door, false));
        check(site, canWalk(site.inside(), site.outside()), "with the door broken the builder walks out");
        hold(ModItems.WAND.get().getDefaultInstance());
        teleport(site.outside());
        ctx.waitTicks(15);
        check(site, pending(site) && !armed(site), "holding the wand near it, the zone waits");
        server.runCommand("setblock " + door.getX() + " " + door.getY() + " " + door.getZ()
                + " minecraft:iron_door[facing=north,half=lower,hinge=left,open=false,powered=false]");
        server.runCommand("setblock " + door.getX() + " " + (door.getY() + 1) + " " + door.getZ()
                + " minecraft:iron_door[facing=north,half=upper,hinge=left,open=false,powered=false]");
        hold(ItemStack.EMPTY);
        ctx.waitTicks(15);
        check(site, armed(site), "wand put away: the zone locks with the door back");
        touch(site.part("button"));
        check(site, canWalk(site.outside(), site.inside()), "a challenger gets in with the button");
        teleport(site.inside());
        unlock(site);
    }

    /** The core reached from outside the zone: nobody is inside, so the shard arms it at once. */
    private void outside(Site site) {
        settle(site);
        teleport(site.outside().east());
        server.runOnServer(s -> core(s, site).placeCrystal(player(s), new ItemStack(Items.AMETHYST_SHARD)));
        check(site, armed(site), "placed from outside, it locks at once");
    }

    /** The builder placed the shard and left the game inside: the zone waits for them to come back and leave. */
    private void loggedOutInside(Site site) {
        settle(site);
        armFromInside(site);
        server.runOnServer(s -> {
            CoreBlockEntity core = core(s, site);
            try {
                var by = CoreBlockEntity.class.getDeclaredField("pendingBy");
                by.setAccessible(true);
                by.set(core, UUID.randomUUID());
                var left = CoreBlockEntity.class.getDeclaredField("pendingLeft");
                left.setAccessible(true);
                left.setBoolean(core, false);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        });
        teleport(site.outside());
        ctx.waitTicks(15);
        check(site, pending(site) && !armed(site), "whoever placed the shard and left the game inside keeps it open");
    }

    /** Changed your mind: sneak and use takes the shard back off. */
    private void cancel(Site site) {
        settle(site);
        armFromInside(site);
        server.runOnServer(s -> {
            ServerPlayer player = player(s);
            player.setShiftKeyDown(true);
            core(s, site).use(player);
            player.setShiftKeyDown(false);
        });
        teleport(site.outside());
        ctx.waitTicks(15);
        check(site, !pending(site) && !armed(site), "sneak + use takes the shard back: the zone stays open");
    }

    /** A bed inside the zone: dying after it locks brings you back at the world spawn, not inside it. */
    private void bed(Site site) {
        settle(site);
        BlockPos bed = site.part("bed");
        server.runOnServer(s -> player(s).setRespawnPosition(new ServerPlayer.RespawnConfig(
                LevelData.RespawnData.of(s.overworld().dimension(), bed, 0, 0), false), false));
        armFromInside(site);
        walkOut(site);
        check(site, armed(site), "locks once the builder is out");
        server.runCommand("kill @a");
        ctx.waitTicks(30);
        BlockPos at = server.computeOnServer(s -> player(s).blockPosition());
        check(site, !server.computeOnServer(s -> core(s, site).box().contains(at)),
                "respawned at " + at + ", outside the locked zone");
    }

    // ------------------------------------------------------------------ steps

    private void settle(Site site) {
        teleport(site.outside());
        hold(ItemStack.EMPTY);
        ctx.waitTicks(10);
        log("-- " + site.name());
    }

    /** The builder walks to the core and places the shard: the zone waits (pending), nothing locks with them inside. */
    private void armFromInside(Site site) {
        teleport(site.inside());
        server.runOnServer(s -> core(s, site).placeCrystal(player(s), new ItemStack(Items.AMETHYST_SHARD)));
        ctx.waitTicks(10);
        check(site, pending(site) && !armed(site), "placed from inside, the zone waits for the builder to leave");
        check(site, !server.computeOnServer(s -> SealBlock.isGuarded(s.overworld(), site.core())),
                "while pending the seals are not guarded");
    }

    private void walkOut(Site site) {
        walkOutTo(site, site.outside());
    }

    /** The builder must be able to walk from the core to {@code to}; then they're moved there and the zone ticks. */
    private void walkOutTo(Site site, BlockPos to) {
        walkOutFrom(site, site.inside(), to);
    }

    private void walkOutFrom(Site site, BlockPos from, BlockPos to) {
        check(site, canWalk(from, to), "the builder can walk out");
        teleport(to);
        ctx.waitTicks(15);
    }

    private void enter(Site site, BlockPos from, BlockPos to) {
        check(site, canWalk(from, to), "the challenger walks from " + from + " to " + to);
        teleport(to);
    }

    private void reachAndUnlock(Site site) {
        check(site, canWalk(site.outside(), site.inside()), "the challenger reaches the core");
        teleport(site.inside());
        unlock(site);
        check(site, canWalk(site.inside(), site.outside()), "the challenger walks out");
    }

    private void unlock(Site site) {
        server.runOnServer(s -> core(s, site).use(player(s)));
        ctx.waitTicks(5);
        check(site, !armed(site), "the core switches off");
    }

    /** Uses the block as the player (lever, button, seal), as a right click does. */
    private void touch(BlockPos pos) {
        server.runOnServer(s -> {
            BlockState state = s.overworld().getBlockState(pos);
            state.useWithoutItem(s.overworld(), player(s), new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos,
                    false));
        });
        ctx.waitTicks(4);
    }

    private void hold(ItemStack stack) {
        server.runOnServer(s -> player(s).setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack.copy()));
    }

    private void teleport(BlockPos feet) {
        server.runCommand("tp @a " + (feet.getX() + 0.5) + " " + feet.getY() + " " + (feet.getZ() + 0.5));
        ctx.waitTicks(2);
    }

    // ------------------------------------------------------------------ the walk search

    /**
     * Whether a survival player who can't place or break anything gets from {@code from} to {@code to} (feet): walking,
     * a block up, drops of up to 4, wooden doors and gates opened by hand, open iron doors.
     */
    private boolean canWalk(BlockPos from, BlockPos to) {
        return server.computeOnServer(s -> {
            ServerLevel level = s.overworld();
            Set<BlockPos> seen = new HashSet<>();
            ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            queue.add(from);
            seen.add(from);
            while (!queue.isEmpty() && seen.size() < 20_000) {
                BlockPos pos = queue.poll();
                if (pos.equals(to))
                    return true;
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    BlockPos next = pos.relative(dir);
                    BlockPos land = null;
                    if (standable(level, next))
                        land = next;
                    else if (standable(level, next.above()) && open(level, pos.above(2)))
                        land = next.above();
                    else if (open(level, next) && open(level, next.above()))
                        for (int drop = 1; drop <= 4 && land == null; drop++) {
                            BlockPos down = next.below(drop);
                            if (standable(level, down))
                                land = down;
                            else if (!open(level, down))
                                break;
                        }
                    if (land != null && Math.abs(land.getX() - from.getX()) < 40 && seen.add(land))
                        queue.add(land);
                }
            }
            return false;
        });
    }

    private static boolean standable(ServerLevel level, BlockPos feet) {
        return open(level, feet) && open(level, feet.above()) && !open(level, feet.below())
                && !level.getBlockState(feet.below()).getCollisionShape(level, feet.below()).isEmpty();
    }

    private static boolean open(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof DoorBlock)
            return state.getValue(DoorBlock.OPEN) || DoorBlock.isWoodenDoor(state);
        if (state.getBlock() instanceof FenceGateBlock)
            return true;
        return state.getCollisionShape(level, pos).isEmpty();
    }

    // ------------------------------------------------------------------ state

    private boolean armed(Site site) {
        return server.computeOnServer(s -> core(s, site).isArmed());
    }

    private boolean pending(Site site) {
        return server.computeOnServer(s -> core(s, site).isPending());
    }

    private boolean doorOpen(BlockPos door) {
        ctx.waitTicks(2);
        return server.computeOnServer(s -> s.overworld().getBlockState(door).getValue(DoorBlock.OPEN));
    }

    private boolean sealLit(BlockPos seal) {
        return server.computeOnServer(s -> s.overworld().getBlockState(seal).getValue(SealBlock.LIT));
    }

    private static CoreBlockEntity core(MinecraftServer server, Site site) {
        return (CoreBlockEntity) server.overworld().getBlockEntity(site.core());
    }

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayers().getFirst();
    }

    private void check(Site site, boolean condition, String what) {
        log((condition ? "ok   " : "FAIL ") + site.name() + ": " + what);
        if (!condition)
            failures++;
    }

    private static void log(String line) {
        System.out.println("[zonelock-traps] " + line);
    }
}
