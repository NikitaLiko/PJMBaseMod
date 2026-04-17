package ru.liko.pjmbasemod.client.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;
import ru.liko.pjmbasemod.Pjmbasemod;
import ru.liko.pjmbasemod.client.gui.overlay.ItemSwitchPanel;
import ru.liko.pjmbasemod.common.player.PjmAttachments;

/**
 * Обработчик переключения специальных слотов оружия клавишами 1-4
 * NeoForge 1.21.1: Updated event bus annotations and event classes
 */
@EventBusSubscriber(modid = Pjmbasemod.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ItemSwitchHandler {
    
    /**
     * Обрабатывает нажатия клавиш 1-4 для переключения специальных слотов
     */
    @SubscribeEvent
    public static void onKeyPress(InputEvent.Key event) {
        int key = event.getKey();
        int action = event.getAction();
        
        // Проверяем, что клавиша нажата (не отпущена)
        if (action != GLFW.GLFW_PRESS) {
            return;
        }
        
        // Проверяем клавиши 1-4 (GLFW_GLOB_KEY_1-4)
        int slotIndex;
        if (key == GLFW.GLFW_KEY_1) {
            slotIndex = 0;
        } else if (key == GLFW.GLFW_KEY_2) {
            slotIndex = 1;
        } else if (key == GLFW.GLFW_KEY_3) {
            slotIndex = 2;
        } else if (key == GLFW.GLFW_KEY_4) {
            slotIndex = 3;
        } else {
            // Если клавиша не 1-4, выходим
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        
        // Проверяем, что игрок существует и в игре
        if (player == null || mc.level == null) {
            return;
        }
        
        // Проверяем, что игрок не в меню
        if (mc.screen != null) {
            return;
        }
        
        // Создаем финальную копию для использования в лямбде
        final int finalSlotIndex = slotIndex;
        
        // NeoForge 1.21.1: Use Data Attachments instead of Capabilities
        ru.liko.pjmbasemod.common.player.PjmPlayerData data = player.getData(PjmAttachments.PLAYER_DATA);
        if (data != null) {
            // Получаем предмет из специального слота
            ItemStack slotItem = data.getSpecialSlot(finalSlotIndex);
            
            // Если слот пустой, ничего не делаем
            if (slotItem.isEmpty()) {
                return;
            }
            
            // Показываем панель переключения
            ItemSwitchPanel.show(finalSlotIndex);
            
            // Переключаем предмет в руку
            switchToSlot(player, slotItem, finalSlotIndex, data);
        }
    }
    
    /**
     * Переключает предмет в руку игрока
     */
    private static void switchToSlot(LocalPlayer player, ItemStack slotItem, int slotIndex, 
                                      ru.liko.pjmbasemod.common.player.PjmPlayerData data) {
        ItemStack currentItem = player.getMainHandItem();
        
        // Проверяем, совпадает ли предмет в слоте с текущим предметом в руке
        if (ItemStack.isSameItem(currentItem, slotItem) && ItemStack.isSameItemSameComponents(currentItem, slotItem)) {
            return;
        }
        
        // Сначала ищем предмет в инвентаре
        boolean foundInInventory = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (ItemStack.isSameItem(stack, slotItem) && ItemStack.isSameItemSameComponents(stack, slotItem)) {
                // Нашли предмет в инвентаре - переключаем его
                player.getInventory().selected = i;
                foundInInventory = true;
                break;
            }
        }
        
        // Если предмета нет в инвентаре и в руке есть предмет,
        // меняем местами текущий предмет и предмет из специального слота
        if (!foundInInventory && !currentItem.isEmpty()) {
            // Помещаем текущий предмет обратно в специальный слот
            data.setSpecialSlot(slotIndex, currentItem.copy());
            
            // Кладем предмет из специального слота в руку
            player.setItemInHand(InteractionHand.MAIN_HAND, slotItem.copy());
        }
    }
}

