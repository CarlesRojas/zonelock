package app.pinya.pinyazonelock.datagen;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import app.pinya.pinyazonelock.ZoneLock;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;

public class ModDatapackEntries extends DatapackBuiltinEntriesProvider {
    public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder();

    // TODO add these
    // .add(Registries.CONFIGURED_FEATURE, ModConfiguredFeatures::bootstrap)
    // .add(Registries.PLACED_FEATURE, ModPlacedFeatures::bootstrap)
    // .add(ForgeRegistries.Keys.BIOME_MODIFIERS, ModBiomeModifiers::bootstrap);

    public ModDatapackEntries(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, BUILDER, Set.of(ZoneLock.MOD_ID));
    }
}
