package ru.liko.pjmbasemod;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import ru.liko.pjmbasemod.common.init.PjmBlocks;
import ru.liko.pjmbasemod.common.init.PjmSounds;
import ru.liko.pjmbasemod.common.player.PjmAttachments;

/**
 * Main mod class for Pjmbasemod (Project Minecraft).
 * NeoForge 1.21.1 version.
 */
@Mod(Pjmbasemod.MODID)
public class Pjmbasemod {

    public static final String MODID = "pjmbasemod";
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // NeoForge 1.21.1: Use Registries.ITEM instead of ForgeRegistries.ITEMS
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MODID);

    /**
     * NeoForge 1.21.1: Constructor receives IEventBus as parameter
     */
    public Pjmbasemod(IEventBus modEventBus) {
        // Initialize GeckoLib if available (optional dependency)
        if (ModList.get().isLoaded("geckolib")) {
            try {
                Class<?> geckoLibClass = Class.forName("software.bernie.geckolib.GeckoLib");
                geckoLibClass.getMethod("initialize").invoke(null);
            } catch (Exception e) {
                LOGGER.warn("Failed to initialize GeckoLib (optional dependency): {}", e.getMessage());
            }
        }

        // Register lifecycle listeners
        modEventBus.addListener(this::commonSetup);

        // Register Deferred Registers
        PjmBlocks.BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        PjmSounds.REGISTRY.register(modEventBus);
        
        // NeoForge 1.21.1: Register Data Attachments
        PjmAttachments.ATTACHMENT_TYPES.register(modEventBus);

        // Register for game events (NeoForge.EVENT_BUS instead of MinecraftForge.EVENT_BUS)
        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Network registration is now handled via RegisterPayloadHandlersEvent (see PjmNetworking)
            // Initialize configurations early so files are created
            ru.liko.pjmbasemod.common.PjmServerConfig.init();
            ru.liko.pjmbasemod.common.KitsConfig.init();
        });
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        if (Config.isDebugLoggingEnabled()) {
            LOGGER.info("HELLO from server starting");
        }
    }

}
