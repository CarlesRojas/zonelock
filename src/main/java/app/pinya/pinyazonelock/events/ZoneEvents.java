package app.pinya.pinyazonelock.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.pinya.pinyazonelock.ZoneLock;
import app.pinya.pinyazonelock.block.ModBlocks;
import app.pinya.pinyazonelock.world.LockedZones;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ZoneLock.MOD_ID)
public class ZoneEvents {
  private static final Logger LOGGER = LoggerFactory.getLogger(ZoneEvents.class);

  private ZoneEvents() {}

  @SubscribeEvent
  public static void onAnyBreak(BlockEvent.BreakEvent event) {
    if (!(event.getLevel() instanceof ServerLevel level)) return;

    BlockState state = event.getState();

    if (state.is(ModBlocks.ZONE_LOCK_CORE.get())) return;

    BlockPos pos = event.getPos();

    if (LockedZones.get(level).isPosInAnyZone(pos)) {
      if (event.getPlayer() != null && event.getPlayer().isCreative()) return;

      event.setCanceled(true);
      event.getPlayer().displayClientMessage(Component.literal("Zone Locked"), true);
    }
  }

  @SubscribeEvent
  public static void onAnyPlace(BlockEvent.EntityPlaceEvent event) {
    if (!(event.getLevel() instanceof ServerLevel level)) return;

    if (event.getPlacedBlock().is(ModBlocks.ZONE_LOCK_CORE.get())) return;

    BlockPos pos = event.getPos();
    if (LockedZones.get(level).isPosInAnyZone(pos)) {
      if (event.getEntity() instanceof Player player) {
        if (player.isCreative()) return;
        player.displayClientMessage(Component.literal("Zone Locked"), true);
      }

      event.setCanceled(true);
    }
  }
}
