package ru.liko.pjmbasemod.common.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredRegister;
import ru.liko.pjmbasemod.Pjmbasemod;

/**
 * Block registration for the mod.
 * NeoForge 1.21.1 format.
 */
public final class PjmBlocks {

    // NeoForge 1.21.1: Use Registries.BLOCK instead of ForgeRegistries.BLOCKS
    public static final DeferredRegister<Block> BLOCKS = 
        DeferredRegister.create(Registries.BLOCK, Pjmbasemod.MODID);

    private PjmBlocks() {
    }
}

