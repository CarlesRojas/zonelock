package app.pinya.zonelock.neoforge;

import app.pinya.zonelock.ZoneLock;
import app.pinya.zonelock.client.ClientPayloadHandler;
import app.pinya.zonelock.client.ZoneLockClient;
import app.pinya.zonelock.client.render.CoreRenderer;
import app.pinya.zonelock.client.render.ZoneForcefield;
import app.pinya.zonelock.network.ModPayloads;
import app.pinya.zonelock.registry.ModBlocks;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = ZoneLock.MOD_ID, dist = Dist.CLIENT)
public class ZoneLockNeoForgeClient {
    public ZoneLockNeoForgeClient(IEventBus modBus, net.neoforged.fml.ModContainer container) {
        ZoneLockClient.init();
        // The "Config" button of the mods list.
        container.registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
                (mod, parent) -> new app.pinya.zonelock.client.screen.ZoneLockConfigScreen(parent));
        modBus.addListener(ZoneLockNeoForgeClient::registerPayloadHandlers);
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> e
                .registerBlockEntityRenderer(ModBlocks.CORE_ENTITY.get(), CoreRenderer::new));
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterGuiLayersEvent e) -> e.registerAboveAll(
                app.pinya.zonelock.ZoneLock.id("quest_card"),
                (graphics, tick) -> app.pinya.zonelock.client.ZoneHud.renderOnHud(graphics)));
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterMenuScreensEvent e) -> e.register(
                app.pinya.zonelock.registry.ModMenus.CORE.get(), app.pinya.zonelock.client.screen.CoreScreen::new));

        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> ZoneLockClient.onClientTick());
        NeoForge.EVENT_BUS.addListener((SubmitCustomGeometryEvent e) -> ZoneForcefield.submit(e.getSubmitNodeCollector(),
                e.getPoseStack(), e.getLevelRenderState()));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> ZoneLockClient.onDisconnect());
    }

    private static void registerPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        for (ModPayloads.ToClient<?> payload : ModPayloads.TO_CLIENT)
            register(event, payload);
    }

    private static <T extends CustomPacketPayload> void register(RegisterClientPayloadHandlersEvent event,
            ModPayloads.ToClient<T> payload) {
        event.register(payload.type(), (received, context) -> ClientPayloadHandler.handle(received));
    }
}
