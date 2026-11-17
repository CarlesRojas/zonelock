package app.pinya.zonelock.fabric;

import app.pinya.zonelock.ZoneLock;
import app.pinya.zonelock.command.ZoneLockCommand;
import app.pinya.zonelock.network.ModPayloads;
import app.pinya.zonelock.registry.ModItems;
import app.pinya.zonelock.registry.ModRegistry;
import app.pinya.zonelock.registry.RegistryEntry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ZoneLockFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ZoneLock.init();
        ZoneLock.REGISTRIES.forEach(ZoneLockFabric::registerAll);
        registerCreativeTab();
        registerPayloads();

        ServerTickEvents.END_SERVER_TICK.register(ZoneLock::onServerTickEnd);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> ZoneLock.onPlayerJoin(handler.player));
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL
                .register((player, from, to) -> ZoneLock.onPlayerChangedLevel(player));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> ZoneLock.onPlayerChangedLevel(newPlayer));
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> ZoneLockCommand.register(dispatcher));

        ModContainer mod = FabricLoader.getInstance().getModContainer(ZoneLock.MOD_ID).orElseThrow();
        ResourceLoader.registerBuiltinPack(ZoneLock.id(ZoneLock.PACK_DUNGEONS), mod,
                Component.translatable("pack.zonelock.dungeons"), PackActivationType.DEFAULT_ENABLED);
    }

    @SuppressWarnings("unchecked")
    private static <R> void registerAll(ModRegistry<R> modRegistry) {
        Registry<R> registry = (Registry<R>) BuiltInRegistries.REGISTRY.getValue(modRegistry.registryKey().identifier());
        if (registry == null)
            throw new IllegalStateException("Unknown registry " + modRegistry.registryKey());
        for (RegistryEntry<R, ? extends R> entry : modRegistry.entries())
            Registry.register(registry, entry.key(), entry.create());
    }

    private static void registerCreativeTab() {
        CreativeModeTab tab = FabricCreativeModeTab.builder()
                .title(Component.translatable("itemGroup.zonelock"))
                .icon(() -> new ItemStack(ModItems.CORE.get()))
                .displayItems((params, output) -> ModItems.CREATIVE_TAB.forEach(s -> output.accept(s.get())))
                .build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ZoneLock.id("main"), tab);
    }

    private static void registerPayloads() {
        for (ModPayloads.ToClient<?> payload : ModPayloads.TO_CLIENT)
            registerToClient(payload);
        for (ModPayloads.ToServer<?> payload : ModPayloads.TO_SERVER)
            registerToServer(payload);
    }

    private static <T extends CustomPacketPayload> void registerToClient(ModPayloads.ToClient<T> payload) {
        PayloadTypeRegistry.clientboundPlay().register(payload.type(), payload.codec());
    }

    private static <T extends CustomPacketPayload> void registerToServer(ModPayloads.ToServer<T> payload) {
        PayloadTypeRegistry.serverboundPlay().register(payload.type(), payload.codec());
        ServerPlayNetworking.registerGlobalReceiver(payload.type(),
                (received, context) -> payload.handler().accept(received, context.player()));
    }
}
