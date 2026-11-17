package app.pinya.zonelock.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import app.pinya.zonelock.zone.Rule;
import app.pinya.zonelock.zone.ZoneGuard;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

/**
 * Riptide launches are client-driven, so this runs on both sides (the client uses its synced zone mirror).
 * Throwing tridents normally is unaffected.
 */
@Mixin(TridentItem.class)
abstract class TridentItemMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void zonelock$noRiptideCharge(Level level, Player player, InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir) {
        if (riptideBlocked(player.getItemInHand(hand), player))
            cir.setReturnValue(InteractionResult.FAIL);
    }

    @Inject(method = "releaseUsing", at = @At("HEAD"), cancellable = true)
    private void zonelock$noRiptideLaunch(ItemStack stack, Level level, LivingEntity entity, int remainingTime,
            CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof Player player && riptideBlocked(stack, player))
            cir.setReturnValue(false);
    }

    private static boolean riptideBlocked(ItemStack stack, Player player) {
        if (ZoneGuard.bypass(player) || EnchantmentHelper.getTridentSpinAttackStrength(stack, player) <= 0.0F)
            return false;
        // Check a little around the player: a riptide covers several blocks in one go.
        for (int dy = -2; dy <= 2; dy += 2)
            if (ZoneGuard.forbids(player.level(), player.blockPosition().above(dy), Rule.FLIGHT))
                return true;
        return false;
    }
}
