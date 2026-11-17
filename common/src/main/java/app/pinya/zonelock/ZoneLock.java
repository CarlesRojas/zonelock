package app.pinya.zonelock;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.pinya.zonelock.registry.ModBlocks;
import app.pinya.zonelock.registry.ModItems;
import app.pinya.zonelock.registry.ModMenus;
import app.pinya.zonelock.registry.ModRegistry;
import app.pinya.zonelock.registry.ModSounds;
import app.pinya.zonelock.registry.ModWorldgen;
import app.pinya.zonelock.zone.ZoneManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class ZoneLock {
    public static final String MOD_ID = "zonelock";
    public static final Logger LOG = LoggerFactory.getLogger("Zone Lock");

    /** Every registry the mod adds to, in the order they must be registered. */
    public static final List<ModRegistry<?>> REGISTRIES = List.of(
            ModSounds.SOUNDS,
            ModBlocks.BLOCKS,
            ModItems.ITEMS,
            ModBlocks.BLOCK_ENTITIES,
            ModMenus.MENUS,
            ModWorldgen.PROCESSORS);

    /** Built-in data packs players can toggle when creating a world. Folder: resourcepacks/&lt;name&gt;. */
    public static final String PACK_DUNGEONS = "dungeons";

    private ZoneLock() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static void init() {
        LOG.info("Zone Lock {} ready", "2.0");
    }

    // ------------------------------------------------------------------ events forwarded by each loader

    private static boolean verified;

    /**
     * Development check (system property {@code zonelock.verifyMixins}): loads every mixin target so a broken
     * injection fails the test run instead of surfacing later in someone's game.
     */
    private static void verifyMixinTargets() {
        String[] targets = { "net.minecraft.world.level.ServerExplosion",
                "net.minecraft.world.level.block.piston.PistonStructureResolver",
                "net.minecraft.world.level.material.FlowingFluid", "net.minecraft.world.level.material.LavaFluid",
                "net.minecraft.world.level.block.FireBlock", "net.minecraft.world.level.block.TntBlock",
                "net.minecraft.world.entity.monster.Enderman$EndermanTakeBlockGoal",
                "net.minecraft.world.entity.projectile.Projectile",
                "net.minecraft.world.level.block.FarmlandBlock", "net.minecraft.world.level.block.TurtleEggBlock",
                "net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl",
                "net.minecraft.world.item.TridentItem", "net.minecraft.world.item.BucketItem",
                "net.minecraft.world.item.BoatItem", "net.minecraft.world.level.block.DispenserBlock",
                "net.minecraft.world.entity.decoration.ArmorStand", "net.minecraft.world.entity.decoration.ItemFrame",
                "net.minecraft.world.entity.decoration.BlockAttachedEntity",
                "net.minecraft.world.item.ItemStack",
                "net.minecraft.world.item.BlockItem", "net.minecraft.world.entity.player.Player",
                "net.minecraft.world.entity.LivingEntity", "net.minecraft.world.level.Level",
                "net.minecraft.server.level.ServerLevel" };
        for (String target : targets) {
            try {
                Class.forName(target, false, ZoneLock.class.getClassLoader());
            } catch (Throwable e) {
                throw new IllegalStateException("Mixin target failed to load: " + target, e);
            }
        }
        LOG.info("Verified {} mixin targets", targets.length);
    }

    public static void onServerTickEnd(MinecraftServer server) {
        if (!verified) {
            verified = true;
            if (Boolean.getBoolean("zonelock.verifyMixins"))
                verifyMixinTargets();
        }
        for (ServerLevel level : server.getAllLevels())
            ZoneManager.get(level).flush(level);
    }

    public static void onPlayerJoin(ServerPlayer player) {
        ZoneManager.get(player.level()).sendAll(player);
    }

    public static void onPlayerChangedLevel(ServerPlayer player) {
        ZoneManager.get(player.level()).sendAll(player);
    }
}
