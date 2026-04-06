package net.tidal.tidesanddepths.pressure;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.sound.SoundEvents;

public final class PressureEffects {

    private PressureEffects() {}

    // Timers
    private static int tinnitusCooldown    = 0;
    private static int narcosisCooldown    = 0;   // ticks until next nausea episode
    private static int hallucinationCooldown = 0;

    public static void tick(ClientPlayerEntity player) {
        float p = PressureManager.getNormalized();

        tickTinnitus(player, p);
        tickNarcosis(player, p);
        tickHallucinations(player, p);
        tickImpairedMotor(player, p);
    }

    private static void tickTinnitus(ClientPlayerEntity player, float p) {
        if (p < PressureConfig.TINNITUS_THRESHOLD) {
            tinnitusCooldown = 0;
            return;
        }
        if (--tinnitusCooldown > 0) return;

        float intensity = (p - PressureConfig.TINNITUS_THRESHOLD)
                / (1f - PressureConfig.TINNITUS_THRESHOLD);

        int interval = (int)(PressureConfig.TINNITUS_INTERVAL_MAX
                - intensity * (PressureConfig.TINNITUS_INTERVAL_MAX - PressureConfig.TINNITUS_INTERVAL_MIN));
        tinnitusCooldown = interval + (int)(Math.random() * 35);

        float volume = 0.12f + intensity * 0.22f;

        float ringPitch = 0.25f + (float)(Math.random() * 0.15);
        player.playSound(SoundEvents.BLOCK_BELL_USE, volume, ringPitch);

        if (p >= 0.70f) {
            player.playSound(
                    SoundEvents.AMBIENT_UNDERWATER_LOOP_ADDITIONS_RARE,
                    volume * 0.4f, 0.55f);
        }
    }

    private static void tickNarcosis(ClientPlayerEntity player, float p) {
        if (p < PressureConfig.NARCOSIS_THRESHOLD) {
            narcosisCooldown = 0;
            return;
        }
        if (--narcosisCooldown > 0) return;

        float intensity = (p - PressureConfig.NARCOSIS_THRESHOLD)
                / (1f - PressureConfig.NARCOSIS_THRESHOLD);

        int nauseaAmp = intensity > 0.6f ? 1 : 0;
        applyEffect(player, StatusEffects.NAUSEA,
                PressureConfig.NARCOSIS_EPISODE_DURATION, nauseaAmp);

        if (intensity > 0.3f) {
            applyEffect(player, StatusEffects.WEAKNESS, 100, 0);
        }

        int gap = (int)(PressureConfig.NARCOSIS_EPISODE_INTERVAL * (1f - intensity * 0.5f));
        narcosisCooldown = gap + (int)(Math.random() * 40);
    }

    private static void tickHallucinations(ClientPlayerEntity player, float p) {
        if (p < PressureConfig.HALLUCINATION_THRESHOLD) {
            hallucinationCooldown = 0;
            return;
        }
        if (--hallucinationCooldown > 0) return;

        float intensity = (p - PressureConfig.HALLUCINATION_THRESHOLD)
                / (1f - PressureConfig.HALLUCINATION_THRESHOLD);

        float vol   = 0.08f + intensity * 0.14f;
        float pitch = 0.5f + (float)(Math.random() * 0.3);
        player.playSound(SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, vol, pitch);

        hallucinationCooldown = 200 + (int)(Math.random() * 120);
    }

    private static void tickImpairedMotor(ClientPlayerEntity player, float p) {
        if (p < PressureConfig.NARCOSIS_THRESHOLD) return;

        float intensity = (p - PressureConfig.NARCOSIS_THRESHOLD)
                / (1f - PressureConfig.NARCOSIS_THRESHOLD);
        int amp = intensity > 0.6f ? 1 : 0;

        applyEffect(player, StatusEffects.MINING_FATIGUE, 60, amp);
    }

    private static void applyEffect(ClientPlayerEntity player,
                                    net.minecraft.entity.effect.StatusEffect effect,
                                    int durationTicks, int amplifier) {
        player.addStatusEffect(
                new StatusEffectInstance(effect, durationTicks, amplifier,
                         false,
                         false,
                         false));
    }
}