package app.pinya.zonelock.registry;

import com.mojang.serialization.MapCodec;

import app.pinya.zonelock.worldgen.SurfaceSnapProcessor;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;

/** World generation types the dungeons' data packs refer to. */
public final class ModWorldgen {
    public static final ModRegistry<MapCodec<? extends StructureProcessor>> PROCESSORS = new ModRegistry<>(
            Registries.STRUCTURE_PROCESSOR);

    /** {@code zonelock:surface_snap}: lays marked blocks on the real ground (see {@link SurfaceSnapProcessor}). */
    public static final RegistryEntry<MapCodec<? extends StructureProcessor>, MapCodec<SurfaceSnapProcessor>> SURFACE_SNAP = PROCESSORS
            .register("surface_snap", key -> SurfaceSnapProcessor.MAP_CODEC);

    private ModWorldgen() {
    }
}
