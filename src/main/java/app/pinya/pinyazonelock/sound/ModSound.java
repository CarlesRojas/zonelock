package app.pinya.pinyazonelock.sound;

import app.pinya.pinyazonelock.ZoneLock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSound {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,
            ZoneLock.MOD_ID);

    public static final RegistryObject<SoundEvent> ACTIVATE_ZONE = registerSoundEvent("activate_zone");
    public static final RegistryObject<SoundEvent> DEACTIVATE_ZONE = registerSoundEvent("deactivate_zone");
    public static final RegistryObject<SoundEvent> LOCKED = registerSoundEvent("locked");

    public static RegistryObject<SoundEvent> registerSoundEvent(String name) {
        return SOUNDS.register(name, () -> SoundEvent
                .createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZoneLock.MOD_ID, name)));
    }

    public static void register(IEventBus eventBus) {
        SOUNDS.register(eventBus);
    }
}
