package app.pinya.zonelock.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Lays marked template blocks on the real ground of their column instead of at the template's height, so a
 * dungeon can mark the land around it (the Amethyst Tower turns the ground black and grows crystals on it)
 * on hills as well as on flat ground. Every other block of the template is left alone.
 *
 * A block is marked with the int tag {@value #TAG} in its template data: 0 replaces the top block of the
 * ground, 1 sits on it, -1 goes under it. Columns whose ground is more than {@code max_step} blocks away from
 * the template's ground level (a cliff, a ravine) are skipped, and nothing is put on top where the ground is
 * under water.
 */
public record SurfaceSnapProcessor(int maxStep) implements StructureProcessor {

    public static final String TAG = "zonelock_surface";

    public static final MapCodec<SurfaceSnapProcessor> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.optionalFieldOf("max_step", 6).forGetter(SurfaceSnapProcessor::maxStep))
            .apply(i, SurfaceSnapProcessor::new));

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pivot,
            BlockPos localPos, StructureTemplate.StructureBlockInfo info, StructurePlaceSettings settings) {
        CompoundTag tag = info.nbt();
        if (tag == null || !tag.contains(TAG))
            return info;
        int layer = tag.getIntOr(TAG, 0);
        BlockPos pos = info.pos();
        // During world generation only the "WG" heightmaps exist; placed later (a command) trees already stand,
        // so their leaves must not count as ground.
        Heightmap.Types heightmap = level instanceof ServerLevel ? Heightmap.Types.MOTION_BLOCKING_NO_LEAVES
                : Heightmap.Types.OCEAN_FLOOR_WG;
        int ground = level.getHeight(heightmap, pos.getX(), pos.getZ()) - 1;
        if (Math.abs(ground - (pos.getY() - layer)) > maxStep)
            return null;
        BlockPos target = new BlockPos(pos.getX(), ground + layer, pos.getZ());
        if (layer > 0 && !level.getFluidState(target).isEmpty())
            return null;
        return new StructureTemplate.StructureBlockInfo(target, info.state(), null);
    }

    @Override
    public MapCodec<SurfaceSnapProcessor> codec() {
        return MAP_CODEC;
    }
}
