package ru.liko.pjmbasemod.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.liko.pjmbasemod.client.util.UrlTextureUtil;
import ru.liko.pjmbasemod.common.customization.CustomizationManager;
import ru.liko.pjmbasemod.common.customization.CustomizationOption;
import ru.liko.pjmbasemod.common.customization.CustomizationType;
import ru.liko.pjmbasemod.common.player.PjmAttachments;
import ru.liko.pjmbasemod.common.player.PjmPlayerData;

/**
 * Mixin для подмены скинов игроков на основе CustomizationOption.
 * Для 1.20.1 используем getSkinLocation() и getModelName().
 */
@Mixin(PlayerInfo.class)
public class PlayerInfoMixin {
    
    // NeoForge 1.21.1: Use Mojang mappings
    @Inject(method = "getSkinLocation", at = @At("RETURN"), cancellable = true)
    private void wrb$getSkinLocation(CallbackInfoReturnable<ResourceLocation> cir) {
        CustomizationOption option = wrb$getActiveSkinOption();
        if (option != null) {
            ResourceLocation texture = UrlTextureUtil.getTexture(option.getValue());
            if (texture != null) {
                cir.setReturnValue(texture);
            }
        }
    }
    
    // NeoForge 1.21.1: Use Mojang mappings
    @Inject(method = "getModelName", at = @At("RETURN"), cancellable = true)
    private void wrb$getModelName(CallbackInfoReturnable<String> cir) {
        CustomizationOption option = wrb$getActiveSkinOption();
        if (option != null) {
            // slim = Alex style (тонкие руки), default = Steve style (широкие руки)
            cir.setReturnValue(option.isSlimModel() ? "slim" : "default");
        }
    }
    
    @Unique
    private CustomizationOption wrb$getActiveSkinOption() {
        PlayerInfo info = (PlayerInfo) (Object) this;
        GameProfile profile = info.getProfile();
        
        if (Minecraft.getInstance().level == null) {
            return null;
        }
        
        // CHECK PREVIEW FOR LOCAL PLAYER
        if (Minecraft.getInstance().screen instanceof ru.liko.pjmbasemod.client.gui.screen.ClassSelectionScreen) {
             if (Minecraft.getInstance().player != null && 
                 profile.getId().equals(Minecraft.getInstance().player.getGameProfile().getId())) {
                 CustomizationOption preview = ru.liko.pjmbasemod.client.gui.screen.ClassSelectionScreen.previewSkin;
                 if (preview != null && preview.getType() == CustomizationType.SKIN) {
                     return preview;
                 }
             }
        }
        
        Player player = Minecraft.getInstance().level.getPlayerByUUID(profile.getId());
        if (player == null) {
            return null;
        }
        
        // NeoForge 1.21.1: Use Data Attachments instead of Capabilities
        PjmPlayerData data = player.getData(PjmAttachments.PLAYER_DATA);
        if (data != null) {
            String skinId = data.getActiveSkinId();
            if (skinId != null && !skinId.isEmpty()) {
                CustomizationOption option = CustomizationManager.getClientInstance().getOption(skinId);
                if (option != null && option.getType() == CustomizationType.SKIN) {
                    return option;
                }
            }
        }
        return null;
    }
}
