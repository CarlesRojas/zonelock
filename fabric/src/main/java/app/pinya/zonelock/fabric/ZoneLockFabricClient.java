package app.pinya.zonelock.fabric;

import app.pinya.zonelock.client.ClientPayloadHandler;
import app.pinya.zonelock.client.ZoneLockClient;
import app.pinya.zonelock.client.render.CoreRenderer;
import app.pinya.zonelock.client.render.ZoneForcefield;
import app.pinya.zonelock.network.ModPayloads;
import app.pinya.zonelock.registry.ModBlocks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class ZoneLockFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ZoneLockClient.init();
        for (ModPayloads.ToClient<?> payload : ModPayloads.TO_CLIENT)
            registerReceiver(payload);
        ClientTickEvents.END_CLIENT_TICK.register(client -> ZoneLockClient.onClientTick());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ZoneLockClient.onDisconnect());
        BlockEntityRenderers.register(ModBlocks.CORE_ENTITY.get(), CoreRenderer::new);
        LevelRenderEvents.COLLECT_SUBMITS.register(
                context -> ZoneForcefield.submit(context.submitNodeCollector(), context.poseStack(), context.levelState()));
        net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.addLast(
                app.pinya.zonelock.ZoneLock.id("quest_card"),
                (graphics, tick) -> app.pinya.zonelock.client.ZoneHud.renderOnHud(graphics));
        net.minecraft.client.gui.screens.MenuScreens.register(app.pinya.zonelock.registry.ModMenus.CORE.get(),
                app.pinya.zonelock.client.screen.CoreScreen::new);
    }

    private static <T extends CustomPacketPayload> void registerReceiver(ModPayloads.ToClient<T> payload) {
        ClientPlayNetworking.registerGlobalReceiver(payload.type(),
                (received, context) -> ClientPayloadHandler.handle(received));
    }
}
