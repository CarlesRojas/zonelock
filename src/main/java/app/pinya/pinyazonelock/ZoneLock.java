package app.pinya.pinyazonelock;

import app.pinya.pinyazonelock.block.ModBlocks;
import app.pinya.pinyazonelock.block.entity.ModBlocksEntities;
import app.pinya.pinyazonelock.item.ModItems;
import app.pinya.pinyazonelock.networking.ModMessages;
import app.pinya.pinyazonelock.particle.ModParticles;
import app.pinya.pinyazonelock.particle.ZoneParticle;
import app.pinya.pinyazonelock.screen.ModMenuTypes;
import app.pinya.pinyazonelock.screen.custom.CoreScreen;
import app.pinya.pinyazonelock.sound.ModSound;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ZoneLock.MOD_ID)
public class ZoneLock {

    public static final String MOD_ID = "pinyazonelock";

    public ZoneLock() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);

        MinecraftForge.EVENT_BUS.register(app.pinya.pinyazonelock.events.ZoneEvents.class);
        MinecraftForge.EVENT_BUS.register(app.pinya.pinyazonelock.events.ClientEventHandler.class);

        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlocksEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModParticles.register(modEventBus);
        ModSound.register(modEventBus);

        modEventBus.addListener(this::addCreative);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModMessages.register();
        });
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModBlocks.ZONE_LOCK_CORE);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            MenuScreens.register(ModMenuTypes.ZONE_LOCK_CORE_MENU.get(), CoreScreen::new);
        }

        @SubscribeEvent
        public static void registerParticleProvider(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(ModParticles.ZONE.get(), ZoneParticle.Provider::new);
        }
    }
}
