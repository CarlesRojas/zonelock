package app.pinya.pinyazonelock.datagen;

import java.util.concurrent.CompletableFuture;

import app.pinya.pinyazonelock.ZoneLock;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.GlobalLootModifierProvider;

public class ModGlobalLootModifierProvider extends GlobalLootModifierProvider {
    public ModGlobalLootModifierProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, ZoneLock.MOD_ID, registries);
    }

    @Override
    protected void start(HolderLookup.Provider registries) {

    }
}
