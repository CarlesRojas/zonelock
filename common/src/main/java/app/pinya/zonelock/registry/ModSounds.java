package app.pinya.zonelock.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;

/**
 * Sound events. assets/zonelock/sounds.json maps them to vanilla amethyst / beacon sounds, so resource packs can
 * replace them without the mod shipping audio.
 */
public final class ModSounds {
    public static final ModRegistry<SoundEvent> SOUNDS = new ModRegistry<>(Registries.SOUND_EVENT);

    public static final RegistryEntry<SoundEvent, SoundEvent> CORE_ARM = sound("block.amethyst_core.arm");
    public static final RegistryEntry<SoundEvent, SoundEvent> CORE_DISARM = sound("block.amethyst_core.disarm");
    public static final RegistryEntry<SoundEvent, SoundEvent> CORE_UNLOCK = sound("block.amethyst_core.unlock");
    public static final RegistryEntry<SoundEvent, SoundEvent> CORE_DENY = sound("block.amethyst_core.deny");
    public static final RegistryEntry<SoundEvent, SoundEvent> SEAL_EXTINGUISH = sound("block.amethyst_seal.extinguish");
    public static final RegistryEntry<SoundEvent, SoundEvent> SEAL_PING = sound("block.amethyst_seal.ping");
    public static final RegistryEntry<SoundEvent, SoundEvent> BARRIER = sound("zone.barrier");
    public static final RegistryEntry<SoundEvent, SoundEvent> WAND_TUNE = sound("item.resonance_wand.tune");

    private static RegistryEntry<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, key -> SoundEvent.createVariableRangeEvent(key.identifier()));
    }

    private ModSounds() {
    }
}
