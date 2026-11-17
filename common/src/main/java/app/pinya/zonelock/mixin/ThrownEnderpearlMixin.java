package app.pinya.zonelock.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import app.pinya.zonelock.zone.Rule;
import app.pinya.zonelock.zone.ZoneGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/** An ender pearl that lands inside a zone does nothing; the pearl is given back. */
@Mixin(ThrownEnderpearl.class)
abstract class ThrownEnderpearlMixin extends ThrowableItemProjectile {
    private ThrownEnderpearlMixin(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
    }

    @Inject(method = "onHit", at = @At("HEAD"), cancellable = true)
    private void zonelock$noPearlIn(HitResult hit, CallbackInfo ci) {
        if (!(level() instanceof ServerLevel level) || isRemoved())
            return;
        if (!(getOwner() instanceof ServerPlayer player) || ZoneGuard.bypass(player))
            return;
        BlockPos destination = BlockPos.containing(oldPosition());
        if (!ZoneGuard.forbids(level, destination, Rule.TELEPORT))
            return;
        ZoneGuard.feedback(player, destination, Rule.TELEPORT);
        ItemStack refund = new ItemStack(Items.ENDER_PEARL);
        if (!player.getAbilities().instabuild)
            player.getInventory().placeItemBackInInventory(refund, net.minecraft.util.Prediction.SERVER_ONLY);
        discard();
        ci.cancel();
    }
}
