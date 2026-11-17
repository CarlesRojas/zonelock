package app.pinya.zonelock.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import app.pinya.zonelock.item.ResonanceWandItem;
import app.pinya.zonelock.zone.Rule;
import app.pinya.zonelock.zone.ZoneGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * The item half of right-clicking a block: placing blocks, armor stands, boats on blocks, flint and steel, bone
 * meal, tool stripping... Runs after the block itself had its chance, so doors, buttons and chests keep working.
 */
@Mixin(ItemStack.class)
abstract class ItemStackMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void zonelock$useOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        Item item = ((ItemStack) (Object) this).getItem();
        if (item instanceof ResonanceWandItem)
            return; // it checks permissions itself
        BlockPos clicked = context.getClickedPos();
        BlockPos beside = clicked.relative(context.getClickedFace());
        boolean denied;
        if (item instanceof BlockItem blockItem) {
            BlockPos target = level.getBlockState(clicked).canBeReplaced() ? clicked : beside;
            denied = ZoneGuard.deniesPlace(player, level, target, blockItem.getBlock().defaultBlockState());
        } else {
            denied = ZoneGuard.denies(player, level, clicked, Rule.BLOCK_TOOLS)
                    || ZoneGuard.denies(player, level, beside, Rule.PLACE);
        }
        if (denied) {
            cir.setReturnValue(InteractionResult.FAIL);
            if (player instanceof ServerPlayer serverPlayer)
                serverPlayer.containerMenu.sendAllDataToRemote();
        }
    }
}
