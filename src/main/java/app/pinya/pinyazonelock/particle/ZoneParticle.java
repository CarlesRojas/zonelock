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

    protected ZoneParticle(ClientLevel pLevel, double pX, double pY, double pZ, SpriteSet spriteSet, double pXSpeed,
            double pYSpeed,
            double pZSpeed) {
        super(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
        this.friction = 1.0F;
        this.gravity = 0F;
        this.xd = this.yd = this.zd = 0;
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        this.lifetime = 4;
        this.hasPhysics = false;
        this.quadSize = 0.1F;

        this.setSpriteFromAge(spriteSet);

        this.rCol = 0.435f;
        this.gCol = 0.310f;
        this.bCol = 0.671f;

        this.alpha = 0.8F;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        @Nullable
        public Particle createParticle(@Nullable SimpleParticleType pType, @Nullable ClientLevel pLevel, double pX,
                double pY, double pZ,
                double pXSpeed, double pYSpeed, double pZSpeed) {
            return new ZoneParticle(pLevel, pX, pY, pZ, this.sprites, pXSpeed, pYSpeed, pZSpeed);
        }
    }

}