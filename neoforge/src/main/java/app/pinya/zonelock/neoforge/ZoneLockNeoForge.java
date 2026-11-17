package app.pinya.zonelock.neoforge;

import app.pinya.zonelock.ZoneLock;
import app.pinya.zonelock.command.ZoneLockCommand;
import app.pinya.zonelock.network.ModPayloads;
import app.pinya.zonelock.registry.ModItems;
import app.pinya.zonelock.registry.ModRegistry;
import app.pinya.zonelock.registry.RegistryEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(ZoneLock.MOD_ID)
public class ZoneLockNeoForge {
    public ZoneLockNeoForge(IEventBus modBus) {
        ZoneLock.init();
        modBus.addListener(ZoneLockNeoForge::register);
        modBus.addListener(ZoneLockNeoForge::registerPayloads);
        modBus.addListener(ZoneLockNeoForge::addPacks);

        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> ZoneLock.onServerTickEnd(e.getServer()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer player)
                ZoneLock.onPlayerJoin(player);
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer player)
                ZoneLock.onPlayerChangedLevel(player);
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer player)
                ZoneLock.onPlayerChangedLevel(player);
        });
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> ZoneLockCommand.register(e.getDispatcher()));
    }

    private static void register(RegisterEvent event) {
        for (ModRegistry<?> registry : ZoneLock.REGISTRIES)
            registerAll(event, registry);
        event.register(Registries.CREATIVE_MODE_TAB, helper -> helper.register(ZoneLock.id("main"),
                CreativeModeTab.builder()
                        .title(Component.translatable("itemGroup.zonelock"))
                        .icon(() -> new ItemStack(ModItems.CORE.get()))
                        .displayItems((params, output) -> ModItems.CREATIVE_TAB.forEach(s -> output.accept(s.get())))
                        .build()));
    }

    private static <R> void registerAll(RegisterEvent event, ModRegistry<R> registry) {
        event.register(registry.registryKey(), helper -> {
            for (RegistryEntry<R, ? extends R> entry : registry.entries())
                helper.register(entry.key(), entry.create());
        });
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        for (ModPayloads.ToClient<?> payload : ModPayloads.TO_CLIENT)
            registerToClient(registrar, payload);
        for (ModPayloads.ToServer<?> payload : ModPayloads.TO_SERVER)
            registerToServer(registrar, payload);
    }

    private static <T extends CustomPacketPayload> void registerToClient(PayloadRegistrar registrar,
            ModPayloads.ToClient<T> payload) {
        registrar.playToClient(payload.type(), payload.codec());
    }

    private static <T extends CustomPacketPayload> void registerToServer(PayloadRegistrar registrar,
            ModPayloads.ToServer<T> payload) {
        registrar.playToServer(payload.type(), payload.codec(),
                (received, context) -> payload.handler().accept(received, (ServerPlayer) context.player()));
    }

    private static void addPacks(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA)
            return;
        for (String pack : new String[] { ZoneLock.PACK_DUNGEONS })
            event.addPackFinders(ZoneLock.id("resourcepacks/" + pack), PackType.SERVER_DATA,
                    Component.translatable("pack.zonelock." + pack), PackSource.BUILT_IN, false, Pack.Position.TOP);
    }
}
