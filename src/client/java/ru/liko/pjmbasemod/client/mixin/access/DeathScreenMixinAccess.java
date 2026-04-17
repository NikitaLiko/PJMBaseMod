package ru.liko.pjmbasemod.client.mixin.access;

/**
 * Bridge-интерфейс для доступа к состоянию DeathScreenMixin из других миксинов.
 * Вынесен из mixin-пакета, чтобы на него можно было ссылаться напрямую.
 */
public interface DeathScreenMixinAccess {
    boolean Pjmbasemod$isBlackScreenEnabled();
    int Pjmbasemod$getDelayTicker();
}

