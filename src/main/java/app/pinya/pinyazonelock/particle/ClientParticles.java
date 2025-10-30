package app.pinya.pinyazonelock.particle;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientParticles {
    @SubscribeEvent
    public static void onRegisterFactories(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.ZONE.get(), ZoneParticle.Provider::new);
    }
}