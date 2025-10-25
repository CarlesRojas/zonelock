package app.pinya.pinyazonelock.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.pinya.pinyazonelock.ZoneLock;
import app.pinya.pinyazonelock.block.ModBlocks;
import app.pinya.pinyazonelock.world.ClientLockedZones;
import app.pinya.pinyazonelock.world.LockedZones;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ZoneLock.MOD_ID)
public class ZoneEvents {
  private static final Logger LOGGER = LoggerFactory.getLogger(ZoneEvents.class);

  private ZoneEvents() {
  }

  @SubscribeEvent
  public static void onAnyBreak(BlockEvent.BreakEvent event) {
    if (!(event.getLevel() instanceof ServerLevel level))
      return;

    BlockState state = event.getState();

    if (state.is(ModBlocks.ZONE_LOCK_CORE.get()))
      return;

    BlockPos pos = event.getPos();
    Player player = event.getPlayer();
    // if (player.isCreative()) return;

    if (LockedZones.get(level).isPosInAnyZone(pos)) {
      event.setCanceled(true);
      player.displayClientMessage(Component.literal("Zone Locked"), true);
    }
  }

  // @SubscribeEvent
  // public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock
  // event) {
  // if (!(event.getLevel() instanceof ServerLevel level)) return;

  // Player player = event.getEntity();
  // // if (player.isCreative()) return;

  // ItemStack heldItem = event.getItemStack();
  // if (!(heldItem.getItem() instanceof BlockItem blockItem)) return;
  // if (blockItem.getBlock() == ModBlocks.ZONE_LOCK_CORE.get()) return;

  // BlockPos targetPos = event.getPos().relative(event.getFace());

  // if (LockedZones.get(level).isPosInAnyZone(targetPos)) {
  // player.displayClientMessage(Component.literal("Zone Locked"), true);
  // event.setCanceled(true);
  // event.setCancellationResult(InteractionResult.FAIL);
  // }
  // }

  @SubscribeEvent
  public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
    Level level = event.getLevel();
    if (!level.isClientSide)
      return;

    BlockPos targetPos = event.getPos().relative(event.getFace());

    if (ClientLockedZones.getInstance().isPosInAnyZone(targetPos)) {
      LOGGER.info("Zone Locked from client");
      event.setCanceled(true);
      event.setCancellationResult(InteractionResult.FAIL);

      Minecraft.getInstance().player.displayClientMessage(
          Component.translatable("msg.pinya.zonelock.blocked_place"), true);
      Minecraft.getInstance().player.playSound(SoundEvents.UI_BUTTON_CLICK.get(), 0.6f, 0.6f);
    }
  }
}
