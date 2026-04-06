package net.tidal.tidesanddepths.pressure;

import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;

public final class PressureDamageHandler {

    private PressureDamageHandler() {}

    private static int damageTimer = 0;

    public static void tick(ServerPlayerEntity player) {
        float p = computePressure(player);

        if (p < PressureConfig.DAMAGE_THRESHOLD) {
            damageTimer = 0;
            return;
        }

        if (++damageTimer < PressureConfig.DAMAGE_INTERVAL_TICKS) return;
        damageTimer = 0;

        float overThreshold = (p - PressureConfig.DAMAGE_THRESHOLD)
                / (1f - PressureConfig.DAMAGE_THRESHOLD);
        float damage = PressureConfig.DAMAGE_BASE * (0.5f + overThreshold * 1.5f);

        player.damage(player.getServerWorld().getDamageSources().generic(), damage);
    }

    private static float computePressure(ServerPlayerEntity player) {
        if (!player.isSubmergedIn(FluidTags.WATER)) return 0f;

        int y      = (int) player.getY();
        int startY = PressureConfig.PRESSURE_START_Y;
        int maxY   = PressureConfig.MAX_PRESSURE_Y;

        if (y >= startY) return 0f;
        if (y <= maxY)   return 1f;
        return (float)(startY - y) / (startY - maxY);
    }
}