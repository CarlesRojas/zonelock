package app.pinya.pinyazonelock.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.pinya.pinyazonelock.block.custom.Core;
import app.pinya.pinyazonelock.particle.ModParticles;
import app.pinya.pinyazonelock.world.ClientLockedZones;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(Core.class);
    private static final double STEP = 0.25;
    private static final int RENDER_RADIUS = 48;

    // Purple color with alpha for transparency (R=0.5, G=0, B=1.0)
    // private static final Vec3 PARTICLE_COLOR = new Vec3(0.5, 0, 1.0);
    // private static final float PARTICLE_SIZE = 0.25f;
    // private static final DustParticleOptions PARTICLE_OPTIONS = new
    // DustParticleOptions(
    // new Vector3f((float) PARTICLE_COLOR.x, (float) PARTICLE_COLOR.y, (float)
    // PARTICLE_COLOR.z),
    // PARTICLE_SIZE);

    private static final int PARTICLE_SPAWN_INTERVAL = 20; // 20 ticks = 1 second
    private static int particleTimer = 0;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;

        particleTimer++;
        if (particleTimer < PARTICLE_SPAWN_INTERVAL)
            return;

        particleTimer = 0;

        LOGGER.info("Client tick");

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;

        if (player == null || minecraft.level == null)
            return;

        // boolean holdingAmethyst = player.getMainHandItem().is(Items.AMETHYST_SHARD)
        // ||
        // player.getMainHandItem().is(Items.AMETHYST_BLOCK) ||
        // player.getOffhandItem().is(Items.AMETHYST_SHARD) ||
        // player.getOffhandItem().is(Items.AMETHYST_BLOCK);

        // if (!holdingAmethyst)
        // return;

        ClientLevel level = minecraft.level;
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();

        for (var zone : ClientLockedZones.getInstance().getAllZones()) {
            if (!zone.active())
                continue;

            BlockPos center = zone.center();

            int minX = center.getX() - zone.westExtent();
            int maxX = center.getX() + zone.eastExtent();

            int minY = center.getY() - zone.downExtent();
            int maxY = center.getY() + zone.upExtent();

            int minZ = center.getZ() - zone.northExtent();
            int maxZ = center.getZ() + zone.southExtent();

            Vec3 min = new Vec3(minX, minY, minZ);
            Vec3 max = new Vec3(maxX + 1, maxY + 1, maxZ + 1);
            LOGGER.info("Zone: {} {}", min, max);

            if (camera.distanceToSqr(center.getX(), center.getY(), center.getZ()) > RENDER_RADIUS * RENDER_RADIUS)
                continue;

            // For each of the 6 faces, spawn a few particles at random sampled points
            emitFace(level, camera, min, max, Face.BOTTOM);
            emitFace(level, camera, min, max, Face.TOP);
            emitFace(level, camera, min, max, Face.NORTH);
            emitFace(level, camera, min, max, Face.SOUTH);
            emitFace(level, camera, min, max, Face.WEST);
            emitFace(level, camera, min, max, Face.EAST);
        }
    }

    enum Face {
        TOP, BOTTOM, NORTH, SOUTH, WEST, EAST
    }

    private static void emitFace(ClientLevel level, Vec3 cam, Vec3 min, Vec3 max, Face f) {
        double x, y, z;
        switch (f) {
            case TOP -> {
                y = max.y;
                for (double dx = min.x; dx <= max.x; dx += STEP) {
                    for (double dz = min.z; dz <= max.z; dz += STEP) {
                        level.addParticle(ModParticles.ZONE.get(), dx, y, dz, 0, 0, 0);
                    }
                }
            }
            case BOTTOM -> {
                y = min.y;
                for (double dx = min.x; dx <= max.x; dx += STEP) {
                    for (double dz = min.z; dz <= max.z; dz += STEP) {
                        level.addParticle(ModParticles.ZONE.get(), dx, y, dz, 0, 0, 0);
                    }
                }
            }
            case NORTH -> {
                z = min.z;
                for (double dx = min.x; dx <= max.x; dx += STEP) {
                    for (double dy = min.y; dy <= max.y; dy += STEP) {
                        level.addParticle(ModParticles.ZONE.get(), dx, dy, z, 0, 0, 0);
                    }
                }
            }
            case SOUTH -> {
                z = max.z;
                for (double dx = min.x; dx <= max.x; dx += STEP) {
                    for (double dy = min.y; dy <= max.y; dy += STEP) {
                        level.addParticle(ModParticles.ZONE.get(), dx, dy, z, 0, 0, 0);
                    }
                }
            }
            case WEST -> {
                x = min.x;
                for (double dz = min.z; dz <= max.z; dz += STEP) {
                    for (double dy = min.y; dy <= max.y; dy += STEP) {
                        level.addParticle(ModParticles.ZONE.get(), x, dy, dz, 0, 0, 0);
                    }
                }
            }
            case EAST -> {
                x = max.x;
                for (double dz = min.z; dz <= max.z; dz += STEP) {
                    for (double dy = min.y; dy <= max.y; dy += STEP) {
                        level.addParticle(ModParticles.ZONE.get(), x, dy, dz, 0, 0, 0);
                    }
                }
            }
        }
    }
}