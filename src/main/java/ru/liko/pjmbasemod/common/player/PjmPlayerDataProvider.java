package ru.liko.pjmbasemod.common.player;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import ru.liko.pjmbasemod.Pjmbasemod;

/**
 * NeoForge 1.21.1: This class is now a utility wrapper for the Data Attachments system.
 * The old Forge Capabilities API has been replaced with NeoForge Data Attachments.
 * 
 * Usage: Instead of player.getCapability(WRB_PLAYER_DATA_CAPABILITY), use:
 *   - PjmPlayerDataProvider.get(player) - returns PjmPlayerData
 *   - player.getData(PjmAttachments.PLAYER_DATA) - direct access
 * 
 * @see PjmAttachments#PLAYER_DATA
 */
public class PjmPlayerDataProvider {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "player_data");

    private PjmPlayerDataProvider() {}

    /**
     * Gets the PjmPlayerData for a player using the new Data Attachments system.
     * This is the preferred way to access player data in NeoForge 1.21.1.
     * 
     * @param player The player to get data for
     * @return The player's PjmPlayerData
     */
    public static PjmPlayerData get(Player player) {
        return player.getData(PjmAttachments.PLAYER_DATA);
    }

    /**
     * Checks if a player has PjmPlayerData attached.
     * In the Data Attachments system, data is always available via getData().
     * 
     * @param player The player to check
     * @return true (data is always available in Data Attachments)
     */
    public static boolean has(Player player) {
        return player != null;
    }
}
