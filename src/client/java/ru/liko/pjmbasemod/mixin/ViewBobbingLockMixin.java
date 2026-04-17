package ru.liko.pjmbasemod.mixin;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin для блокировки отключения View Bobbing (тряски экрана).
 * Форсирует bobView = true после загрузки настроек из файла,
 * чтобы игроки не могли отключить тряску экрана через options.txt.
 */
@Mixin(Options.class)
public abstract class ViewBobbingLockMixin {

    @Shadow
    public abstract OptionInstance<Boolean> bobView();

    /**
     * После загрузки настроек из options.txt принудительно включаем View Bobbing.
     */
    @Inject(method = "load", at = @At("RETURN"))
    private void forceBobViewAfterLoad(CallbackInfo ci) {
        bobView().set(true);
    }
}
