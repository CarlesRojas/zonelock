package app.pinya.pinyazonelock.datagen;

import app.pinya.pinyazonelock.ZoneLock;
import app.pinya.pinyazonelock.block.ModBlocks;
import app.pinya.pinyazonelock.block.custom.Core;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, ZoneLock.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        registerCoreBlock();
    }

    private void registerCoreBlock() {
        ModelFile coreOn = models().cube(
                "core_on",
                modLoc("block/core_on"),
                modLoc("block/core_on"),
                modLoc("block/core_on_north"),
                modLoc("block/core_on_south"),
                modLoc("block/core_on_east"),
                modLoc("block/core_on_west"));

        ModelFile coreOff = models().cube(
                "core_off",
                modLoc("block/core_off"),
                modLoc("block/core_off"),
                modLoc("block/core_off_north"),
                modLoc("block/core_off_south"),
                modLoc("block/core_off_east"),
                modLoc("block/core_off_west"));

        Block coreBlock = ModBlocks.ZONE_LOCK_CORE.get();

        getVariantBuilder(coreBlock)
                .partialState().with(Core.ACTIVE, true)
                .modelForState().modelFile(coreOn).addModel()
                .partialState().with(Core.ACTIVE, false)
                .modelForState().modelFile(coreOff).addModel();

        simpleBlockItem(coreBlock, coreOn);
    }
}
