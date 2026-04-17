package ru.liko.pjmbasemod.common.block;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import ru.liko.pjmbasemod.Config;

/**
 * Утилита для проверки глубины копания.
 * Предотвращает создание туннелей и окопов глубже указанного лимита от поверхности.
 * Учитывает ландшафт - поверхность определяется как ближайший твёрдый блок сверху.
 */
public class DigDepthChecker {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Проверяет, можно ли сломать блок на указанной позиции с учётом ограничения глубины.
     * 
     * @param level Мир
     * @param pos Позиция блока для разрушения
     * @param maxDepth Максимальная глубина копания от поверхности
     * @return true если блок можно сломать, false если слишком глубоко
     */
    public static boolean canBreakAtDepth(Level level, BlockPos pos, int maxDepth) {
        if (maxDepth <= 0) {
            return true; // Если лимит 0 или меньше - ограничение отключено
        }
        
        // Находим высоту поверхности над этой позицией
        int surfaceY = findSurfaceY(level, pos);
        
        // Если поверхность не найдена (например, в пустоте), разрешаем копать
        if (surfaceY == Integer.MIN_VALUE) {
            if (Config.isDebugLoggingEnabled()) {
                LOGGER.info("DigDepth: поверхность не найдена для {} - разрешаем", pos);
            }
            return true;
        }
        
        // Вычисляем глубину от поверхности
        // depth = 1 означает первый блок под поверхностью
        int depth = surfaceY - pos.getY();
        
        // Используем < чтобы maxDepth=3 означало ровно 3 блока (depth 1, 2, 3)
        boolean canBreak = depth < maxDepth;
        
        if (Config.isDebugLoggingEnabled()) {
            LOGGER.info("DigDepth: pos={}, surfaceY={}, depth={}, maxDepth={}, canBreak={}", 
                pos, surfaceY, depth, maxDepth, canBreak);
        }
        
        // Разрешаем копать если глубина не превышает лимит
        return canBreak;
    }
    
    /**
     * Находит Y-координату поверхности для указанной позиции.
     * Ищет поверхность ПРЯМО НАД блоком - это работает и для вертикальных ям,
     * и для горизонтальных туннелей.
     * 
     * @param level Мир
     * @param pos Позиция для проверки
     * @return Y-координата поверхности или Integer.MIN_VALUE если не найдена
     */
    public static int findSurfaceY(Level level, BlockPos pos) {
        int x = pos.getX();
        int z = pos.getZ();
        
        // Ищем поверхность ПРЯМО НАД блоком (в той же колонке X, Z)
        // Это правильно работает для горизонтальных туннелей - 
        // поверхность над туннелем не меняется когда копаешь в сторону
        int directSurfaceY = findColumnSurfaceY(level, x, z);
        
        // Если над блоком есть поверхность - используем её
        if (directSurfaceY != Integer.MIN_VALUE && directSurfaceY > pos.getY()) {
            return directSurfaceY;
        }
        
        // Fallback: проверяем соседние колонки (для случая когда копаем яму вниз)
        int maxSurfaceY = directSurfaceY;
        
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue; // Уже проверили
                int surfaceY = findColumnSurfaceY(level, x + dx, z + dz);
                if (surfaceY > maxSurfaceY) {
                    maxSurfaceY = surfaceY;
                }
            }
        }
        
        return maxSurfaceY;
    }
    
    /**
     * Находит Y-координату поверхности для одной колонки (X, Z).
     */
    private static int findColumnSurfaceY(Level level, int x, int z) {
        int maxY = level.getMaxBuildHeight();
        int minY = level.getMinBuildHeight();
        
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos(x, 0, z);
        
        // Ищем поверхность СВЕРХУ ВНИЗ - первый твёрдый блок с воздухом над ним
        for (int y = maxY - 1; y >= minY; y--) {
            mutablePos.setY(y);
            BlockState state = level.getBlockState(mutablePos);
            
            if (isSolidGround(state)) {
                // Проверяем, есть ли воздух/прозрачный блок над этим блоком
                mutablePos.setY(y + 1);
                BlockState aboveState = level.getBlockState(mutablePos);
                if (isAirOrTransparent(aboveState)) {
                    // Это поверхность!
                    return y;
                }
            }
        }
        
        return Integer.MIN_VALUE;
    }
    
    /**
     * Вычисляет глубину от поверхности для указанной позиции.
     * 
     * @param level Мир
     * @param pos Позиция для проверки
     * @return Глубина от поверхности (положительное число = под землёй)
     */
    public static int getDepthFromSurface(Level level, BlockPos pos) {
        int surfaceY = findSurfaceY(level, pos);
        if (surfaceY == Integer.MIN_VALUE) {
            return 0;
        }
        return surfaceY - pos.getY();
    }
    
    /**
     * Проверяет, является ли блок твёрдой землёй (не воздух, не жидкость, не растения)
     */
    private static boolean isSolidGround(BlockState state) {
        if (state.isAir()) {
            return false;
        }
        
        // Проверяем, что блок не жидкость
        if (!state.getFluidState().isEmpty()) {
            return false;
        }
        
        // Проверяем, что блок имеет коллизию (твёрдый)
        // Используем проверку на полный куб или наличие коллизии
        return state.isSolid() || state.blocksMotion();
    }
    
    /**
     * Проверяет, является ли блок воздухом или прозрачным (не твёрдым)
     */
    private static boolean isAirOrTransparent(BlockState state) {
        if (state.isAir()) {
            return true;
        }
        
        // Жидкости тоже считаем "прозрачными" для определения поверхности
        if (!state.getFluidState().isEmpty()) {
            return true;
        }
        
        // Растительность - папоротники, трава, цветы, кусты и т.д.
        if (isPlant(state)) {
            return true;
        }
        
        // Блоки без коллизии (трава, цветы и т.д.)
        return !state.blocksMotion();
    }
    
    /**
     * Проверяет, является ли блок растительностью (трава, папоротники, цветы, кусты и т.д.)
     */
    private static boolean isPlant(BlockState state) {
        // Проверяем через классы блоков
        if (state.getBlock() instanceof BushBlock) {
            return true; // Все кусты, цветы, саженцы, грибы
        }
        if (state.getBlock() instanceof TallGrassBlock) {
            return true; // Высокая трава
        }
        if (state.getBlock() instanceof DoublePlantBlock) {
            return true; // Двойные растения (высокие цветы, папоротники)
        }
        if (state.getBlock() instanceof VineBlock) {
            return true; // Лианы
        }
        
        // Проверяем через теги
        if (state.is(BlockTags.FLOWERS)) {
            return true; // Все цветы
        }
        if (state.is(BlockTags.SAPLINGS)) {
            return true; // Саженцы
        }
        if (state.is(BlockTags.REPLACEABLE)) {
            return true; // Заменяемые блоки (трава, снег и т.д.)
        }
        if (state.is(BlockTags.CROPS)) {
            return true; // Культуры (пшеница, морковь и т.д.)
        }
        
        return false;
    }
}
