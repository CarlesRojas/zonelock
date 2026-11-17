package app.pinya.zonelock.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import app.pinya.zonelock.zone.Rule;
import app.pinya.zonelock.zone.ZoneGuard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import net.minecraft.world.entity.decoration.ItemFrame;

/** Paintings, item frames and armor stands of a zone survive players and explosions. */
@Mixin({ BlockAttachedEntity.class, ItemFrame.class, ArmorStand.class })
abstract class DecorationMixin {
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void zonelock$protect(ServerLevel level, DamageSource source, float damage,
            CallbackInfoReturnable<Boolean> cir) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))
            return; // /kill
        Entity self = (Entity) (Object) this;
        Entity attacker = source.getEntity();
        if (ZoneGuard.bypass(attacker) || !ZoneGuard.forbids(level, self.blockPosition(), Rule.DECORATIONS))
            return;
        if (attacker instanceof Player player)
            ZoneGuard.feedback(player, self.blockPosition(), Rule.DECORATIONS);
        cir.setReturnValue(false);
    }
}
