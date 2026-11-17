package app.pinya.zonelock.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import app.pinya.zonelock.zone.ZoneGuard;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;

/** Backup for placements that skip ItemStack#useOn (lily pads, dispensed shulker boxes...). */
@Mixin(BlockItem.class)
abstract class BlockItemMixin {
    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void zonelock$place(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        BlockItem self = (BlockItem) (Object) this;
        if (ZoneGuard.deniesPlace(context.getPlayer(), context.getLevel(), context.getClickedPos(),
                self.getBlock().defaultBlockState()))
            cir.setReturnValue(InteractionResult.FAIL);
    }
}
