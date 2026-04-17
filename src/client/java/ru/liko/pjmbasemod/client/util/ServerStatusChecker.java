package ru.liko.pjmbasemod.client.util;

import com.mojang.logging.LogUtils;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

public class ServerStatusChecker {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SERVER_ADDRESS = "mc.pjm.space";

    private static ServerStatus currentStatus = ServerStatus.CHECKING;
    private static long lastCheckTime = 0;
    private static final long CHECK_INTERVAL = 30000; // 30 seconds

    public enum ServerStatus {
        ONLINE("ONLINE", 0xFF00FF00),      // Green
        OFFLINE("OFFLINE", 0xFFFF0000),    // Red
        CHECKING("CHECKING", 0xFFFFAA00);  // Yellow

        private final String displayName;
        private final int color;

        ServerStatus(String displayName, int color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getColor() {
            return color;
        }
    }

    public static ServerStatus getServerStatus() {
        long currentTime = System.currentTimeMillis();

        // Check if we need to refresh status
        if (currentTime - lastCheckTime > CHECK_INTERVAL) {
            checkServerStatusAsync();
        }

        return currentStatus;
    }

    public static void checkServerStatusAsync() {
        lastCheckTime = System.currentTimeMillis();
        currentStatus = ServerStatus.CHECKING;

        CompletableFuture.runAsync(() -> {
            try {
                // Use Minecraft's built-in server pinger
                ServerAddress serverAddress = ServerAddress.parseString(SERVER_ADDRESS);
                ServerData serverData = new ServerData("Project Minecraft", SERVER_ADDRESS, ServerData.Type.OTHER);

                // Create a simple pinger using Minecraft's systems
                boolean online = pingMinecraftServer(serverAddress, serverData);
                currentStatus = online ? ServerStatus.ONLINE : ServerStatus.OFFLINE;

                if (ru.liko.pjmbasemod.Config.isDebugLoggingEnabled()) {
                    LOGGER.info("Server {} status: {}", SERVER_ADDRESS, currentStatus.getDisplayName());
                }
            } catch (Exception e) {
                LOGGER.error("Failed to check server status: {}", e.getMessage());
                currentStatus = ServerStatus.OFFLINE;
            }
        });
    }

    private static boolean pingMinecraftServer(ServerAddress address, ServerData serverData) {
        try {
            // Use basic socket connection test with proper timeout
            String host = address.getHost();
            int port = address.getPort();

            if (ru.liko.pjmbasemod.Config.isDebugLoggingEnabled()) {
                LOGGER.debug("Attempting to ping {}:{}", host, port);
            }

            java.net.InetSocketAddress socketAddress = new java.net.InetSocketAddress(host, port);

            try (java.net.Socket socket = new java.net.Socket()) {
                socket.connect(socketAddress, 5000); // 5 second timeout
                boolean connected = socket.isConnected();
                if (ru.liko.pjmbasemod.Config.isDebugLoggingEnabled()) {
                    LOGGER.debug("Connection result: {}", connected);
                }
                return connected;
            }
        } catch (java.net.UnknownHostException e) {
            LOGGER.warn("Server host not found: {}", e.getMessage());
            return false;
        } catch (java.net.SocketTimeoutException e) {
            LOGGER.warn("Server ping timeout: {}", e.getMessage());
            return false;
        } catch (java.io.IOException e) {
            LOGGER.warn("Server ping failed: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            LOGGER.error("Unexpected error during ping: ", e);
            return false;
        }
    }

    public static void forceCheck() {
        lastCheckTime = 0;
        checkServerStatusAsync();
    }

    public static String getServerAddress() {
        return SERVER_ADDRESS;
    }
}
