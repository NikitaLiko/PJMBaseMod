package ru.liko.pjmbasemod.common.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import ru.liko.pjmbasemod.Pjmbasemod;

/**
 * Sound registration for WRB Base Mod.
 * NeoForge 1.21.1 format with DeferredHolder.
 */
public final class PjmSounds {

    // NeoForge 1.21.1: Use Registries.SOUND_EVENT instead of
    // ForgeRegistries.SOUND_EVENTS
    public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(Registries.SOUND_EVENT,
            Pjmbasemod.MODID);

    // Menu Sounds - NeoForge 1.21.1: Use DeferredHolder instead of RegistryObject
    public static final DeferredHolder<SoundEvent, SoundEvent> MENU_LOADING = REGISTRY.register("menu.loading",
            () -> SoundEvent.createVariableRangeEvent(loc("menu.loading")));

    public static final DeferredHolder<SoundEvent, SoundEvent> MENU_MUSIC = REGISTRY.register("menu.music",
            () -> SoundEvent.createVariableRangeEvent(loc("menu.music")));

    // Radio Sounds
    public static final DeferredHolder<SoundEvent, SoundEvent> RADIO_START = REGISTRY.register("radio.start",
            () -> SoundEvent.createVariableRangeEvent(loc("radio.start")));

    public static final DeferredHolder<SoundEvent, SoundEvent> RADIO_END = REGISTRY.register("radio.end",
            () -> SoundEvent.createVariableRangeEvent(loc("radio.end")));

    public static final DeferredHolder<SoundEvent, SoundEvent> RADIO_BACKGROUND = REGISTRY.register("radio.background",
            () -> SoundEvent.createVariableRangeEvent(loc("radio.background")));

    private static ResourceLocation loc(String path) {
        return ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, path);
    }

    private PjmSounds() {
    }
}
