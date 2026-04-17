package ru.liko.pjmbasemod.common.player;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

/**
 * Represents the military ranks that are available to players.
 * The enum keeps the display component as well as a normalized identifier that can be used in commands.
 * 
 * Ranks (from lowest to highest):
 * - NOT_ENLISTED (Не состоит) - default state
 * - PRIVATE (Рядовой)
 * - CORPORAL (Капрал)
 * - SERGEANT (Сержант)
 * - LIEUTENANT (Лейтенант)
 * - CAPTAIN (Капитан)
 * - MAJOR (Майор)
 */
public enum PjmRank {
    NOT_ENLISTED("not_enlisted", Component.literal("Не состоит").withStyle(ChatFormatting.DARK_GRAY), 0),
    PRIVATE("private", Component.literal("Рядовой").withStyle(ChatFormatting.GRAY), 0),
    CORPORAL("corporal", Component.literal("Капрал").withStyle(ChatFormatting.BLUE), 500),
    SERGEANT("sergeant", Component.literal("Сержант").withStyle(ChatFormatting.DARK_AQUA), 1500),
    LIEUTENANT("lieutenant", Component.literal("Лейтенант").withStyle(ChatFormatting.YELLOW), 4000),
    CAPTAIN("captain", Component.literal("Капитан").withStyle(ChatFormatting.RED), 8000),
    MAJOR("major", Component.literal("Майор").withStyle(ChatFormatting.GOLD), 15000);

    private final String id;
    private final MutableComponent displayName;
    private final Set<String> aliases;
    private final int minPoints;

    PjmRank(String id, MutableComponent displayName, int minPoints) {
        this.id = id;
        this.displayName = displayName;
        this.minPoints = minPoints;
        this.aliases = Set.of(id, displayName.getString().toLowerCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_'));
    }

    public String getId() {
        return id;
    }

    public MutableComponent getDisplayName() {
        return displayName.copy();
    }
    
    public int getMinPoints() {
        return minPoints;
    }

    /**
     * @return a dedicated key that is safe to store in persistent data.
     */
    public String getPersistenceKey() {
        return id;
    }
    
    public PjmRank getNext() {
        int nextOrdinal = this.ordinal() + 1;
        if (nextOrdinal < values().length) {
            return values()[nextOrdinal];
        }
        return this;
    }
    
    public PjmRank getPrevious() {
        int prevOrdinal = this.ordinal() - 1;
        if (prevOrdinal >= 0) {
            return values()[prevOrdinal];
        }
        return this;
    }

    /**
     * Parses the provided input to a rank. The method accepts english identifiers, russian names,
     * and ignores case as well as spaces and hyphens.
     */
    public static Optional<PjmRank> fromString(String input) {
        if (input == null || input.isEmpty()) {
            return Optional.empty();
        }
        String normalized = normalize(input);
        return Arrays.stream(values())
                .filter(rank -> rank.aliases.contains(normalized))
                .findFirst();
    }

    public static PjmRank fromKeyOrDefault(String key) {
        PjmRank rank = fromString(key).orElse(NOT_ENLISTED);
        // Если ранг NOT_ENLISTED, возвращаем PRIVATE по умолчанию
        return rank == NOT_ENLISTED ? PRIVATE : rank;
    }

    private static String normalize(String raw) {
        return raw.toLowerCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
    }

    /**
     * Возвращает ResourceLocation для иконки звания.
     * Иконки находятся в assets/Pjmbasemod/textures/rangs/
     */
    public ResourceLocation getIconLocation() {
        String iconName = switch (this) {
            case NOT_ENLISTED -> null; // Нет иконки для "Не состоит"
            case PRIVATE -> "private.png";
            case CORPORAL -> "corporal.png";
            case SERGEANT -> "sergeant.png";
            case LIEUTENANT -> "leutenant.png";
            case CAPTAIN -> "capitain.png";
            case MAJOR -> "major.png";
        };
        
        if (iconName == null) {
            return null;
        }
        
        return ResourceLocation.fromNamespaceAndPath("pjmbasemod", "textures/rangs/" + iconName);
    }
    
    /**
     * Возвращает Unicode символ для отображения иконки звания в чате и TAB-списке.
     * Использует Private Use Area (U+E000-U+E005) для custom font.
     */
    public String getIconChar() {
        return switch (this) {
            case NOT_ENLISTED -> ""; // Нет иконки
            case PRIVATE -> "\uE000"; // Unicode Private Use Area
            case CORPORAL -> "\uE001";
            case SERGEANT -> "\uE002";
            case LIEUTENANT -> "\uE003";
            case CAPTAIN -> "\uE004";
            case MAJOR -> "\uE005";
        };
    }
}
