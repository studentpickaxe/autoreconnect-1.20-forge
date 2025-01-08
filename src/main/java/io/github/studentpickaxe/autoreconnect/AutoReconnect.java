package io.github.studentpickaxe.autoreconnect;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

@Mod(AutoReconnect.MODID)
public class AutoReconnect {
    public static final String MODID = "autoreconnect";

    static final Logger LOGGER = LogUtils.getLogger();

    public AutoReconnect() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientSetup::new);
    }

    private static class ClientSetup {
        private static final Minecraft mc = Minecraft.getInstance();
        private static final JoinMultiplayerScreen jmScreen = new JoinMultiplayerScreen(new TitleScreen());
        private static final int timeout = 10_000;
        private static final int maxTimes = 5;
        private static ServerData serverData = null;
        // timeFlag = -1 when client player log in
        private static long timeFlag = -1;
        private static int i = 0;

        ClientSetup() {
            MinecraftForge.EVENT_BUS.register(this);
        }

        private static void connectTo(ServerData serverData) {
            LOGGER.info("Reconnecting");
            mc.execute(() -> {
                mc.setScreen(new TitleScreen());
                ConnectScreen.startConnecting(jmScreen, mc, ServerAddress.parseString(serverData.ip),
                                              serverData, false);
            });
        }


        @OnlyIn(Dist.CLIENT)
        @SubscribeEvent
        public void onLogOut(ClientPlayerNetworkEvent.LoggingOut event) {
            if (Config.isAutoReconnectEnabled()) {
                if (mc.getCurrentServer() == null) {                    // Client player connected to a server
                    LOGGER.info("Client player connected to a server");
                    timeFlag = System.currentTimeMillis();

                    CompletableFuture.runAsync(() -> {
                        try {
                            Thread.sleep(timeout);
                            // Timeout
                            if (serverData != null && timeFlag != -1 && System.currentTimeMillis() >= timeFlag + timeout) {
                                if (i < maxTimes) {
                                    i++;
                                    connectTo(serverData);
                                } else {
                                    serverData = null;
                                }
                            }
                        } catch (InterruptedException e) {
                            LOGGER.error(e.getMessage());
                        }
                    });

                } else {                                                // Client player logged out
                    LOGGER.info("Client player logged out");
                    CompletableFuture.runAsync(() -> {
                        try {
                            int delay = Config.getAutoReconnectDelay();
                            Thread.sleep(delay); // wait
                            connectTo(serverData);
                        } catch (InterruptedException e) {
                            LOGGER.error(e.getMessage());
                        }
                    });
                }
            }
        }

        @SubscribeEvent
        public void onLogIn(ClientPlayerNetworkEvent.LoggingIn event) { // Client player logged in
            LOGGER.info("Client player logged in");
            serverData = mc.getCurrentServer();
            timeFlag = -1;
            i = 0;
        }
    }
}