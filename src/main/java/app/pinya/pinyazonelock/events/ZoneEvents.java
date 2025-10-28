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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ZoneLock.MOD_ID)
public class ZoneEvents {
  private static final Logger LOGGER = LoggerFactory.getLogger(ZoneEvents.class);

  private ZoneEvents() {
  }

  @SubscribeEvent
  public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      LockedZones zones = LockedZones.get(level);
      zones.syncToClients();
    }
  }

  @SubscribeEvent
  public static void onAnyBreak(BlockEvent.BreakEvent event) {
    Player player = event.getPlayer();
    if (player.isCreative())
      return;

    if (!(event.getLevel() instanceof ServerLevel level))
      return;

    BlockState state = event.getState();

    if (state.is(ModBlocks.ZONE_LOCK_CORE.get()))
      return;

    BlockPos pos = event.getPos();

    if (LockedZones.get(level).isPosInAnyZone(pos)) {
      event.setCanceled(true);
      showFeedback();
    }
  }

  @SubscribeEvent
  public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
    if (!(event.getLevel() instanceof ServerLevel level))
      return;

    event.getAffectedBlocks().removeIf((BlockPos pos) -> LockedZones.get(level).isPosInAnyZone(pos));
  }

  private static boolean isPlacementItem(Item item) {
    if (item instanceof BlockItem)
      return true;

    if (item instanceof BucketItem)
      return true;

    if (item instanceof SpawnEggItem)
      return true;

    // if (item instanceof HangingEntityItem)
    // return true;

    // if (item instanceof ArmorStandItem)
    // return true;

    // if (item instanceof BoatItem)
    // return true;

    // if (item instanceof MinecartItem)
    // return true;

    return false;
  }

  private static boolean hasMenu(Level level, BlockPos pos, BlockState state) {
    MenuProvider prov = state.getMenuProvider(level, pos);
    return prov != null;
  }

  @SubscribeEvent
  public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
    Player player = event.getEntity();
    if (player.isCreative() || event.getItemStack().isEmpty())
      return;

    BlockPos pos = event.getPos();
    BlockState state = event.getLevel().getBlockState(pos);
    Level level = event.getLevel();
    if (hasMenu(level, pos, state)) {
      event.setUseItem(Event.Result.DENY);
      return;
    }

    Item handItem = event.getItemStack().getItem();
    if (!isPlacementItem(handItem))
      return;

    if (event.getLevel() instanceof ServerLevel serverLevel)
      onRightClickBlockServer(event, serverLevel);
    else
      onRightClickBlockClient(event);
  }

  private static void onRightClickBlockServer(PlayerInteractEvent.RightClickBlock event,
      ServerLevel level) {
    BlockPos targetPos = event.getPos().relative(event.getFace());

    if (LockedZones.get(level).isPosInAnyZone(targetPos)) {
      event.setCanceled(true);
      event.setCancellationResult(InteractionResult.FAIL);
    }
  }

  private static void onRightClickBlockClient(PlayerInteractEvent.RightClickBlock event) {
    BlockPos targetPos = event.getPos().relative(event.getFace());

    if (ClientLockedZones.getInstance().isPosInAnyZone(targetPos)) {
      event.setCanceled(true);
      event.setCancellationResult(InteractionResult.FAIL);
      showFeedback();
    }
  }

  private static void showFeedback() {
    Minecraft.getInstance().player.displayClientMessage(
        Component.translatable("msg.pinya.zonelock.blocked_place"), true);
    Minecraft.getInstance().player.playSound(SoundEvents.UI_BUTTON_CLICK.get(),
        0.6f, 0.6f);
  }
}
