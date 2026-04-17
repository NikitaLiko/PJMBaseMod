package ru.liko.pjmbasemod.common.player;

import java.util.function.Supplier;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import ru.liko.pjmbasemod.Pjmbasemod;

/**
 * Registry for NeoForge Data Attachments.
 * Replaces the old Capability system from Forge 1.20.1.
 */
public class PjmAttachments {
    
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = 
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Pjmbasemod.MODID);
    
    /**
     * Player data attachment - stores rank, team, class, kits, customization.
     * Uses Codec serialization and automatically copies on death.
     */
    public static final Supplier<AttachmentType<PjmPlayerData>> PLAYER_DATA = 
        ATTACHMENT_TYPES.register("player_data", () -> 
            AttachmentType.serializable(PjmPlayerData::new)
                .copyOnDeath()
                .build()
        );
}
