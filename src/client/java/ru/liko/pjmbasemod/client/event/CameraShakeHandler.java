package ru.liko.pjmbasemod.client.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import ru.liko.pjmbasemod.Config;
import ru.liko.pjmbasemod.Pjmbasemod;

/**
 * Реалистичная тряска камеры (Head Bob & Camera Shake).
 * Включает:
 * - Smooth Head Bob (плавный шаг)
 * - Idle Breathing (дыхание)
 * - Landing Impact (удар при приземлении)
 */
@EventBusSubscriber(modid = Pjmbasemod.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class CameraShakeHandler {

    private CameraShakeHandler() {}

    // --- Настройки Head Bob (Шаги) ---
    // Частоты пересчитаны под скорость игрока (0.21 блок/тик).
    // Значение ~1.3 дает реальные ~1.8 шага в секунду.
    private static final float WALK_FREQUENCY = 1.3f; // Было 8.5
    private static final float SPRINT_FREQUENCY = 1.6f; // Было 10.5
    private static final float SNEAK_FREQUENCY = 1.0f; // Было 5.5

    // Амплитуды (Pitch = вертикаль, Yaw = горизонт, Roll = наклон)
    // Уменьшены для естественности
    private static final float WALK_PITCH_AMP = 0.05f; // Было 0.07
    private static final float WALK_YAW_AMP = 0.02f;   // Было 0.035
    private static final float WALK_ROLL_AMP = 0.015f; // Было 0.025

    private static final float SPRINT_PITCH_AMP = 0.09f; // Было 0.14
    private static final float SPRINT_YAW_AMP = 0.04f;   // Было 0.07
    private static final float SPRINT_ROLL_AMP = 0.03f;  // Было 0.05

    // --- Настройки Breathing (Дыхание) ---
    private static final float BREATH_FREQUENCY = 0.3f; // Еще медленнее
    private static final float BREATH_AMP = 0.02f; 

    // --- Настройки Landing (Приземление) ---
    private static final float LANDING_RECOVERY_SPEED = 0.1f;
    private static final float MAX_LANDING_OFFSET = 6.0f;
    private static final float MIN_FALL_SPEED = 0.45f; // Чуть поднял порог (0.45), чтобы прыжки на месте точно не триггерили

    private static final float AMPLITUDE_LERP_SPEED = 0.05f;

    // --- Состояние ---
    private static float bobProgress = 0.0f;
    private static float prevBobProgress = 0.0f;
    private static float currentFrequency = WALK_FREQUENCY;
    
    private static float currentPitchAmp = 0.0f;
    private static float currentYawAmp = 0.0f;
    private static float currentRollAmp = 0.0f;

    private static float targetPitchAmp = 0.0f;
    private static float targetYawAmp = 0.0f;
    private static float targetRollAmp = 0.0f;

    private static float breathProgress = 0.0f;
    private static float prevBreathProgress = 0.0f;
    private static float breathAmp = 0.0f;

    private static float landingOffset = 0.0f;
    private static float prevLandingOffset = 0.0f;
    private static boolean wasOnGround = true;
    private static double prevMotionY = 0.0;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!Config.isEnableCameraHeadBob()) {
            resetState();
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.isPaused()) {
            return;
        }

        // Принудительно включаем View Bobbing, чтобы игрок не мог отключить через настройки
        if (!mc.options.bobView().get()) {
            mc.options.bobView().set(true);
        }

        LocalPlayer player = mc.player;

        // 1. Landing Logic (Приземление)
        boolean onGround = player.onGround();
        double motionY = player.getDeltaMovement().y;
        
        if (!wasOnGround && onGround && prevMotionY < -MIN_FALL_SPEED) {
            float fallSpeed = (float) Math.abs(prevMotionY);
            float impact = (fallSpeed - MIN_FALL_SPEED) * 6.0f; // Чуть усилил множитель, т.к. порог выше
            
            impact = Mth.clamp(impact, 0.0f, 4.0f);
            
            landingOffset += impact; 
            if (landingOffset > MAX_LANDING_OFFSET) landingOffset = MAX_LANDING_OFFSET;
        }

        wasOnGround = onGround;
        prevMotionY = motionY;

        prevLandingOffset = landingOffset;
        landingOffset = Mth.lerp(LANDING_RECOVERY_SPEED, landingOffset, 0.0f);


        // 2. Movement Logic (Bob & Breath)
        if (player.isDeadOrDying() || player.isPassenger() || player.getAbilities().flying 
                || player.isSwimming() || player.isFallFlying() || player.isUnderWater()) {
            targetPitchAmp = 0; targetYawAmp = 0; targetRollAmp = 0; breathAmp = 0;
            lerpAmplitudes();
            return;
        }

        float speed = (float) player.getDeltaMovement().horizontalDistance();
        boolean isMoving = speed > 0.01f && player.onGround();

        float targetBreath = isMoving ? 0.0f : BREATH_AMP;
        breathAmp = Mth.lerp(0.05f, breathAmp, targetBreath);
        
        prevBreathProgress = breathProgress;
        breathProgress += BREATH_FREQUENCY * 0.1f; 

        if (isMoving) {
            float targetFreq;
            
            if (player.isCrouching()) {
                targetPitchAmp = 0.02f; // Совсем чуть-чуть в приседе
                targetYawAmp = 0.01f;
                targetRollAmp = 0.005f;
                targetFreq = SNEAK_FREQUENCY;
            } else if (player.isSprinting()) {
                targetPitchAmp = SPRINT_PITCH_AMP;
                targetYawAmp = SPRINT_YAW_AMP;
                targetRollAmp = SPRINT_ROLL_AMP;
                targetFreq = SPRINT_FREQUENCY;
            } else {
                targetPitchAmp = WALK_PITCH_AMP;
                targetYawAmp = WALK_YAW_AMP;
                targetRollAmp = WALK_ROLL_AMP;
                targetFreq = WALK_FREQUENCY;
            }

            currentFrequency = Mth.lerp(0.05f, currentFrequency, targetFreq);
            
            prevBobProgress = bobProgress;
            bobProgress += speed * currentFrequency;
        } else {
            targetPitchAmp = 0; targetYawAmp = 0; targetRollAmp = 0;
            prevBobProgress = bobProgress;
        }

        lerpAmplitudes();
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!Config.isEnableCameraHeadBob()) return;

        float partialTick = (float) event.getPartialTick();

        // --- 1. LANDING ---
        float landing = Mth.lerp(partialTick, prevLandingOffset, landingOffset);
        if (Math.abs(landing) > 0.001f) {
            event.setPitch(event.getPitch() + landing); 
        }

        // --- 2. BREATHING ---
        if (breathAmp > 0.001f) {
            float breathT = Mth.lerp(partialTick, prevBreathProgress, breathProgress);
            
            float breathPitch = (float) Math.sin(breathT) * breathAmp;
            float breathYaw = (float) Math.cos(breathT * 0.5f) * breathAmp * 0.5f;
            float breathRoll = (float) Math.sin(breathT * 0.3f) * breathAmp * 0.2f;
            
            event.setPitch(event.getPitch() + breathPitch);
            event.setYaw(event.getYaw() + breathYaw);
            event.setRoll(event.getRoll() + breathRoll);
        }

        // --- 3. WALKING BOB ---
        if (currentPitchAmp < 0.001f && currentYawAmp < 0.001f) return;

        float t = Mth.lerp(partialTick, prevBobProgress, bobProgress);

        float sinT = (float) Math.sin(t);
        // Формула: sin^2 дает 2 пика на период 2PI.
        // При частоте 1.3 и скорости 0.22, период 2PI проходится за ~20-22 тика (1 сек).
        // Это дает 2 шага в секунду - идеально.
        float smoothBob = sinT * sinT; 
        float bobPitch = (smoothBob * -1.0f + 0.5f) * currentPitchAmp * 2.0f;
        
        float bobYaw = sinT * currentYawAmp;
        float bobRoll = sinT * currentRollAmp;

        event.setPitch(event.getPitch() + bobPitch);
        event.setYaw(event.getYaw() + bobYaw);
        event.setRoll(event.getRoll() + bobRoll);
    }

    private static void lerpAmplitudes() {
        currentPitchAmp = Mth.lerp(AMPLITUDE_LERP_SPEED, currentPitchAmp, targetPitchAmp);
        currentYawAmp = Mth.lerp(AMPLITUDE_LERP_SPEED, currentYawAmp, targetYawAmp);
        currentRollAmp = Mth.lerp(AMPLITUDE_LERP_SPEED, currentRollAmp, targetRollAmp);
    }

    private static void resetState() {
        bobProgress = 0; prevBobProgress = 0;
        landingOffset = 0; prevLandingOffset = 0;
        breathProgress = 0; prevBreathProgress = 0;
        breathAmp = 0;
        currentPitchAmp = 0; targetPitchAmp = 0;
    }
}
