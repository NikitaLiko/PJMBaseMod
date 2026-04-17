package ru.liko.pjmbasemod.client.util;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.resources.DefaultPlayerSkin;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class UrlTextureUtil {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, ResourceLocation> TEXTURE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> LOADING_STATUS = new ConcurrentHashMap<>();

    public static ResourceLocation getTexture(String pathOrUrl) {
        if (pathOrUrl == null || pathOrUrl.isEmpty()) {
            return getMissingTexture();
        }

        // If it's a normal resource location (contains ':' and no http)
        if (!pathOrUrl.startsWith("http") && pathOrUrl.contains(":")) {
            return ResourceLocation.parse(pathOrUrl);
        }

        // If it's a URL
        if (TEXTURE_CACHE.containsKey(pathOrUrl)) {
            return TEXTURE_CACHE.get(pathOrUrl);
        }

        if (!LOADING_STATUS.containsKey(pathOrUrl)) {
            loadTextureAsync(pathOrUrl);
        }

        return getMissingTexture(); // Return loading/missing while downloading
    }

    private static ResourceLocation getMissingTexture() {
        // NeoForge 1.21.1: DefaultPlayerSkin API changed - use getDefaultTexture instead
        return DefaultPlayerSkin.getDefaultTexture();
    }

    private static void loadTextureAsync(String urlString) {
        LOADING_STATUS.put(urlString, true);
        
        CompletableFuture.runAsync(() -> {
            try {
                File cacheDir = new File(Minecraft.getInstance().gameDirectory, "pjm_skin_cache");
                if (!cacheDir.exists()) cacheDir.mkdirs();

                String hash = getSha1(urlString);
                File cacheFile = new File(cacheDir, hash + ".png");

                if (!cacheFile.exists()) {
                    // Download
                    java.net.URL url = URI.create(urlString).toURL();
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                    connection.connect();
                    
                    try (InputStream in = connection.getInputStream()) {
                        NativeImage image = NativeImage.read(in);
                        image.writeToFile(cacheFile);
                    }
                }

                // Register on main thread
                Minecraft.getInstance().execute(() -> {
                    try {
                        NativeImage image = NativeImage.read(new FileInputStream(cacheFile));
                        DynamicTexture texture = new DynamicTexture(image);
                        String id = "pjm_url_" + hash;
                        ResourceLocation location = ResourceLocation.fromNamespaceAndPath("pjmbasemod", id);
                        Minecraft.getInstance().getTextureManager().register(location, texture);
                        TEXTURE_CACHE.put(urlString, location);
                    } catch (Exception e) {
                        LOGGER.error("Failed to register URL texture: {}", e.getMessage());
                    }
                });

            } catch (Exception e) {
                LOGGER.error("Failed to download URL texture: {}", e.getMessage());
                LOADING_STATUS.remove(urlString);
            }
        });
    }

    private static String getSha1(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] result = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : result) {
                sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }
}
