package app.pinya.pinyazonelock.datagen;

import java.util.concurrent.CompletableFuture;

import app.pinya.pinyazonelock.ZoneLock;
import app.pinya.pinyazonelock.block.ModBlocks;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {
    public ModRecipeProvider(PackOutput pOutput, CompletableFuture<Provider> pRegistries) {
        super(pOutput, pRegistries);
    }

    @Override
    protected void buildRecipes(RecipeOutput pRecipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.ZONE_LOCK_CORE.get())
                .pattern("AAA")
                .pattern("ABA")
                .pattern("AAA")
                .define('A', Items.DEEPSLATE)
                .define('B', Items.AMETHYST_SHARD)
                .unlockedBy("has_amethyst_shard", has(Items.AMETHYST_SHARD))
                .save(pRecipeOutput, ZoneLock.MOD_ID + ":zone_lock_core");
    }

    public static class Runner extends RecipeProvider {
        private final ModRecipeProvider provider;

        public Runner(PackOutput pOutput, CompletableFuture<Provider> pRegistries) {
            super(pOutput, pRegistries);
            this.provider = new ModRecipeProvider(pOutput, pRegistries);
        }

        @Override
        protected void buildRecipes(RecipeOutput recipeOutput) {
            provider.buildRecipes(recipeOutput);
        }
    }
}
