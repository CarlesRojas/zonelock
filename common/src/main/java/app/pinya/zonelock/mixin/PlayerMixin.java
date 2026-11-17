package app.pinya.zonelock.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import app.pinya.zonelock.zone.Rule;
import app.pinya.zonelock.zone.ZoneGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

@Mixin(Player.class)
abstract class PlayerMixin {
    /** Breaking: checked by the server and by the client before it starts the crack animation. */
    @ModifyReturnValue(method = "blockActionRestricted", at = @At("RETURN"))
    private boolean zonelock$protectBlocks(boolean restricted, Level level, BlockPos pos, GameType gameType) {
        return restricted || ZoneGuard.deniesBreak((Player) (Object) this, level, pos);
    }

    /** No gliding inside a zone that forbids flight. */
    @ModifyReturnValue(method = "canGlide", at = @At("RETURN"))
    private boolean zonelock$noGliding(boolean canGlide) {
        Player self = (Player) (Object) this;
        return canGlide && (ZoneGuard.bypass(self)
                || !ZoneGuard.forbids(self.level(), self.blockPosition(), Rule.FLIGHT));
    }
}
