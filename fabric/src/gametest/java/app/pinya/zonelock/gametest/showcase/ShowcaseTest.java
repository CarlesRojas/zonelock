package app.pinya.zonelock.gametest.showcase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Builds the test world (see {@link TestWorld}) and copies it to the dev run folders of both loaders, standing on the
 * platform in survival, ready to play. Only runs with the environment variable {@code ZONELOCK_SHOWCASE=world}.
 */
public class ShowcaseTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        if (!"world".equals(System.getenv("ZONELOCK_SHOWCASE")))
            return;
        ctx.runOnClient(mc -> {
            mc.options.languageCode = "es_es";
            mc.getLanguageManager().setSelected("es_es");
            mc.reloadResourcePacks();
        });
        ctx.waitTicks(40);
        try (TestSingleplayerContext sp = ctx.worldBuilder().setUseConsistentSettings(false)
                .adjustSettings(settings -> settings.setAllowCommands(true)).create()) {
            TestServerContext server = sp.getServer();
            server.runOnServer(s -> player(s).setGameMode(GameType.SPECTATOR));
            TestWorld.build(server);
            ctx.waitTicks(200);
            server.runOnServer(s -> player(s).setGameMode(GameType.SURVIVAL));
            server.runCommand("tp @a 0.5 " + TestWorld.Y + " -9.5 -60 10");
            ctx.waitTicks(20);
            server.runOnServer(s -> s.saveEverything(true, true, true));
            ctx.waitTicks(20);
            copyWorld(server.computeOnServer(s -> s.getWorldPath(LevelResource.ROOT)));
        }
    }

    /** Copies the freshly built world into the dev run folders of both loaders, ready to play. */
    private static void copyWorld(Path source) {
        Path root = Path.of("").toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("settings.gradle")))
            root = root.getParent();
        if (root == null)
            return;
        for (String loader : new String[] { "fabric", "neoforge" }) {
            Path target = root.resolve(loader).resolve("runs/client/saves/ZoneLock Test World");
            if (Files.exists(target)) {
                try (Stream<Path> old = Files.walk(target)) {
                    for (Path file : old.sorted(java.util.Comparator.reverseOrder()).toList())
                        Files.delete(file);
                } catch (IOException e) {
                    throw new RuntimeException("Could not clear the previous test world " + target, e);
                }
            }
            try (Stream<Path> files = Files.walk(source)) {
                for (Path file : (Iterable<Path>) files::iterator) {
                    if (file.getFileName().toString().equals("session.lock"))
                        continue;
                    Path dest = target.resolve(source.relativize(file).toString());
                    if (Files.isDirectory(file))
                        Files.createDirectories(dest);
                    else
                        Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not copy the test world to " + target, e);
            }
        }
    }

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayers().getFirst();
    }
}
