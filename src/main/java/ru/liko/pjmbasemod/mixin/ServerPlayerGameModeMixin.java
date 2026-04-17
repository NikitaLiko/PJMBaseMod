package ru.liko.pjmbasemod.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.liko.pjmbasemod.Config;
import ru.liko.pjmbasemod.common.block.BlockBreakLogger;
import ru.liko.pjmbasemod.common.block.DigDepthChecker;

import java.util.Map;

/**
 * Mixin для блокировки ломания и установки неразрешенных блоков в режиме выживания (анти-гриф)
 */
@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {

    // NeoForge 1.21.1: Use Mojang mappings
    @Shadow
    private ServerPlayer player;

    // NeoForge 1.21.1: Use Mojang mappings
    @Shadow
    private GameType gameModeForPlayer;

    /**
     * Перехватывает попытку сломать блок и блокирует её, если блок не в белом списке
     */
    // NeoForge 1.21.1: Use Mojang mappings
    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void onDestroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        // Проверяем, включена ли защита от грифинга
        if (!Config.isAntiGriefEnabled()) {
            return; // Если защита выключена, ничего не делаем
        }

        // Проверяем, если игрок в режиме выживания
        if (this.gameModeForPlayer == GameType.SURVIVAL && this.player != null) {
            // Операторы (OP level 2+) обходят все ограничения
            if (this.player.hasPermissions(2)) {
                return;
            }
            
            Level level = this.player.level();
            BlockState blockState = level.getBlockState(pos);
            
            // Проверяем, разрешен ли этот блок для ломания
            if (!Config.isBlockBreakable(blockState)) {
                this.player.displayClientMessage(
                    Component.translatable("wrb.antigrief.block_not_breakable"),
                    true
                );
                cir.setReturnValue(false);
                return;
            }
            
            // Проверяем глубину копания
            int maxDepth = Config.getMaxDigDepth();
            if (maxDepth > 0 && !DigDepthChecker.canBreakAtDepth(level, pos, maxDepth)) {
                int currentDepth = DigDepthChecker.getDepthFromSurface(level, pos);
                this.player.displayClientMessage(
                    Component.translatable("wrb.antigrief.too_deep", currentDepth, maxDepth),
                    true
                );
                cir.setReturnValue(false);
                return;
            }
            
            // Проверяем требование инструмента
            ItemStack heldItem = this.player.getMainHandItem();
            if (!checkToolRequirement(blockState, heldItem)) {
                this.player.displayClientMessage(
                    Component.translatable("wrb.antigrief.wrong_tool"),
                    true
                );
                cir.setReturnValue(false);
                return;
            }
            
            // Логируем разрушение блока
            String toolName = getToolName(heldItem);
            BlockBreakLogger.logBlockBreak(this.player, blockState, pos, toolName);
        }
    }
    
    /**
     * Проверяет, соответствует ли инструмент требованиям для ломания блока
     */
    private boolean checkToolRequirement(BlockState blockState, ItemStack heldItem) {
        Map<String, String> toolRequirements = Config.getToolRequiredBlocks();
        if (toolRequirements == null || toolRequirements.isEmpty()) {
            return true; // Нет требований - разрешаем
        }
        
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
        if (blockId == null) {
            return true;
        }
        
        String blockIdStr = blockId.toString();
        String requiredTool = toolRequirements.get(blockIdStr);
        
        if (requiredTool == null) {
            return true; // Для этого блока нет требований
        }
        
        if (heldItem == null || heldItem.isEmpty()) {
            return false; // Требуется инструмент, но руки пусты
        }
        
        // Проверяем тип инструмента
        String toolLower = requiredTool.toLowerCase();
        if (toolLower.equals("pickaxe")) {
            return heldItem.getItem() instanceof PickaxeItem;
        } else if (toolLower.equals("axe")) {
            return heldItem.getItem() instanceof AxeItem;
        } else if (toolLower.equals("shovel")) {
            return heldItem.getItem() instanceof ShovelItem;
        } else if (toolLower.equals("hoe")) {
            return heldItem.getItem() instanceof HoeItem;
        }
        
        // Проверяем конкретный ID инструмента
        ResourceLocation heldItemId = BuiltInRegistries.ITEM.getKey(heldItem.getItem());
        if (heldItemId == null) {
            return false;
        }
        
        return heldItemId.toString().equals(requiredTool);
    }
    
    /**
     * Получает название инструмента для логирования
     */
    private String getToolName(ItemStack heldItem) {
        if (heldItem == null || heldItem.isEmpty()) {
            return "hand";
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(heldItem.getItem());
        return itemId != null ? itemId.toString() : "unknown";
    }

    /**
     * Перехватывает попытку установить блок или взаимодействовать с блоком и блокирует её, если блок не в белом списке
     */
    // NeoForge 1.21.1: Use Mojang mappings
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void onUseItemOn(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        // Проверяем, включена ли защита от грифинга
        if (!Config.isAntiGriefEnabled()) {
            return; // Если защита выключена, ничего не делаем
        }

        // Проверяем, если игрок в режиме выживания
        if (this.gameModeForPlayer == GameType.SURVIVAL && player != null) {
            // Получаем блок, на который игрок кликает
            BlockPos clickedPos = hitResult.getBlockPos();
            BlockState clickedBlockState = level.getBlockState(clickedPos);
            
            // Проверяем, пытается ли игрок поставить блок
            if (stack != null && !stack.isEmpty()) {
                net.minecraft.world.item.Item item = stack.getItem();
                if (item instanceof net.minecraft.world.item.BlockItem blockItem) {
                    net.minecraft.world.level.block.Block blockToPlace = blockItem.getBlock();
                    net.minecraft.resources.ResourceLocation blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(blockToPlace);
                    
                    // Проверяем, разрешен ли этот блок для установки
                    if (!Config.isBlockPlaceable(blockId)) {
                        // Блокируем установку блока
                        player.displayClientMessage(
                            Component.translatable("wrb.antigrief.block_not_placeable"),
                            true
                        );
                        cir.setReturnValue(InteractionResult.FAIL); // Отменяем установку блока
                        return;
                    }
                }
            }
            
            // Проверяем взаимодействие с блоками (сундуки, таблички, печки и т.д.)
            if (Config.isBlockInteractionPreventionEnabled()) {
                // Проверяем, является ли блок интерактивным (имеет метод use)
                net.minecraft.world.level.block.Block clickedBlock = clickedBlockState.getBlock();
                
                // Проверяем, является ли блок интерактивным типом (например, сундук, табличка, печка и т.д.)
                if (isInteractiveBlock(clickedBlock)) {
                    // Проверяем, разрешено ли взаимодействие с этим блоком
                    if (!Config.isBlockInteractable(clickedBlockState)) {
                        // Блокируем взаимодействие с блоком
                        player.displayClientMessage(
                            Component.translatable("wrb.antigrief.block_not_interactable"),
                            true
                        );
                        cir.setReturnValue(InteractionResult.FAIL); // Отменяем взаимодействие
                    }
                }
            }
        }
    }
    
    /**
     * Проверяет, является ли блок интерактивным (с которым можно взаимодействовать)
     */
    private boolean isInteractiveBlock(net.minecraft.world.level.block.Block block) {
        // Список типов интерактивных блоков
        return block instanceof net.minecraft.world.level.block.SignBlock ||
               block instanceof net.minecraft.world.level.block.WallSignBlock ||
               block instanceof net.minecraft.world.level.block.CeilingHangingSignBlock ||
               block instanceof net.minecraft.world.level.block.WallHangingSignBlock ||
               block instanceof net.minecraft.world.level.block.ChestBlock ||
               block instanceof net.minecraft.world.level.block.TrappedChestBlock ||
               block instanceof net.minecraft.world.level.block.EnderChestBlock ||
               block instanceof net.minecraft.world.level.block.BarrelBlock ||
               block instanceof net.minecraft.world.level.block.ShulkerBoxBlock ||
               block instanceof net.minecraft.world.level.block.HopperBlock ||
               block instanceof net.minecraft.world.level.block.DropperBlock ||
               block instanceof net.minecraft.world.level.block.DispenserBlock ||
               block instanceof net.minecraft.world.level.block.FurnaceBlock ||
               block instanceof net.minecraft.world.level.block.BlastFurnaceBlock ||
               block instanceof net.minecraft.world.level.block.SmokerBlock ||
               block instanceof net.minecraft.world.level.block.BrewingStandBlock ||
               block instanceof net.minecraft.world.level.block.BeaconBlock ||
               block instanceof net.minecraft.world.level.block.AnvilBlock ||
               block instanceof net.minecraft.world.level.block.EnchantingTableBlock ||
               block instanceof net.minecraft.world.level.block.CraftingTableBlock ||
               block instanceof net.minecraft.world.level.block.GrindstoneBlock ||
               block instanceof net.minecraft.world.level.block.CartographyTableBlock ||
               block instanceof net.minecraft.world.level.block.LoomBlock ||
               block instanceof net.minecraft.world.level.block.StonecutterBlock ||
               block instanceof net.minecraft.world.level.block.SmithingTableBlock ||
               block instanceof net.minecraft.world.level.block.FletchingTableBlock ||
               block instanceof net.minecraft.world.level.block.LecternBlock ||
               block instanceof net.minecraft.world.level.block.BellBlock ||
               block instanceof net.minecraft.world.level.block.DoorBlock ||
               block instanceof net.minecraft.world.level.block.TrapDoorBlock ||
               block instanceof net.minecraft.world.level.block.FenceGateBlock ||
               block instanceof net.minecraft.world.level.block.ButtonBlock ||
               block instanceof net.minecraft.world.level.block.LeverBlock ||
               block instanceof net.minecraft.world.level.block.DaylightDetectorBlock ||
               block instanceof net.minecraft.world.level.block.NoteBlock ||
               block instanceof net.minecraft.world.level.block.JukeboxBlock ||
               block instanceof net.minecraft.world.level.block.BedBlock ||
               block instanceof net.minecraft.world.level.block.RespawnAnchorBlock ||
               block instanceof net.minecraft.world.level.block.CakeBlock ||
               block instanceof net.minecraft.world.level.block.CandleCakeBlock ||
               block instanceof net.minecraft.world.level.block.ComparatorBlock ||
               block instanceof net.minecraft.world.level.block.RepeaterBlock ||
               block instanceof net.minecraft.world.level.block.CommandBlock ||
               block instanceof net.minecraft.world.level.block.StructureBlock ||
               block instanceof net.minecraft.world.level.block.JigsawBlock ||
               block instanceof net.minecraft.world.level.block.DecoratedPotBlock ||
               block instanceof net.minecraft.world.level.block.ChiseledBookShelfBlock ||
               block instanceof net.minecraft.world.level.block.ComposterBlock ||
               block instanceof net.minecraft.world.level.block.CampfireBlock ||
               block instanceof net.minecraft.world.level.block.CauldronBlock ||
               block instanceof net.minecraft.world.level.block.LayeredCauldronBlock;
    }
}

