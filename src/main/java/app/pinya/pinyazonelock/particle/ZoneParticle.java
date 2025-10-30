package app.pinya.pinyazonelock.particle;

import javax.annotation.Nullable;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public class ZoneParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected ZoneParticle(ClientLevel level, double x, double y, double z,
            double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz);
        this.sprites = sprites;

        // No gravity, fixed lifetime (2s at 20 TPS)
        this.gravity = 0.0F;
        this.lifetime = 40;

        // If you want it perfectly static, zero these:
        // this.xd = this.yd = this.zd = 0.0;

        this.pickSprite(sprites); // pick a sprite frame

        this.hasPhysics = false; // Disable collision physics
        this.gravity = 0; // Disable gravity

        // Make particle not move
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;

        // Set size
        this.quadSize = 0.25F;

        // Set color (purple)
        // this.setColor(0.5F, 0.0F, 1.0F);
        // this.setSize(0.02F, 0.02F); // hitbox/visual scale helper (not mandatory)
        // this.setAlpha(0.8F); // Slightly transparent
    }

    @Override
    public void tick() {
        // Default tick ages and removes at end of lifetime. No alpha changes here,
        // so it disappears instantly when age reaches lifetime.
        super.tick();
    }

    @Override
    public ParticleRenderType getRenderType() {
        // use translucent sheet (typical for smoky/ghosty particles)
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    // Factory
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@Nullable SimpleParticleType type, @Nullable ClientLevel level,
                double x, double y, double z,
                double vx, double vy, double vz) {
            return new ZoneParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}