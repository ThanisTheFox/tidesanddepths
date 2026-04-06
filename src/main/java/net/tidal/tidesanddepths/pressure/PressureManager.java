package net.tidal.tidesanddepths.pressure;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.tag.FluidTags;


public final class PressureManager {

    private PressureManager() {}

    private static float currentPressure = 0f;
    private static float targetPressure  = 0f;

    private static float playerY           = 63f;
    private static int   playerBlockY      = 63;
    private static int   depthBelowSurface = 0;

    private static long tickCount = 0L;

    public static void tick(ClientPlayerEntity player) {
        if (player == null) return;

        tickCount++;

        playerY           = (float) player.getY();
        playerBlockY      = (int) playerY;
        depthBelowSurface = Math.max(0, PressureConfig.PRESSURE_START_Y - playerBlockY);


        if (player.isSubmergedIn(FluidTags.WATER)) {
            targetPressure = calculateTarget(playerBlockY);
        } else {
            targetPressure = 0f;
        }

        if (PressureConfig.SMOOTH_PRESSURE) {
            if (currentPressure < targetPressure) {
                currentPressure = Math.min(currentPressure + PressureConfig.BUILDUP_RATE, targetPressure);
            } else {
                currentPressure = Math.max(currentPressure - PressureConfig.RELIEF_RATE, targetPressure);
            }
        } else {
            currentPressure = targetPressure;
        }
    }

    public static float getNormalized() { return currentPressure; }

    public static float getTargetNormalized() { return targetPressure; }

    public static float getDisplayValue() {
        return currentPressure * PressureConfig.MAX_PRESSURE_DISPLAY;
    }

    public static float getPlayerY() { return playerY; }

    public static int getPlayerBlockY() { return playerBlockY; }

    public static int getDepthBelowSurface() { return depthBelowSurface; }

    public static boolean hasAnyPressure() { return currentPressure > 0.001f; }

    public static long getTickCount() { return tickCount; }

    public static float getShakeOffset(int axis) {
        float p = currentPressure;
        if (p <= PressureConfig.SHAKE_THRESHOLD) return 0f;

        float intensity = (p - PressureConfig.SHAKE_THRESHOLD)
                / (1f - PressureConfig.SHAKE_THRESHOLD);

        double ms = System.currentTimeMillis();

        float offset;
        if (axis == 0) {
            offset = (float)(Math.sin(ms * 0.031) * 0.7 + Math.sin(ms * 0.077) * 0.3);
        } else {
            offset = (float)(Math.cos(ms * 0.023 + 1.1) * 0.6 + Math.cos(ms * 0.059) * 0.4);
        }

        return offset * intensity * PressureConfig.SHAKE_MAX_PIXELS;
    }

    private static float calculateTarget(int blockY) {
        int startY = PressureConfig.PRESSURE_START_Y;
        int maxY   = PressureConfig.MAX_PRESSURE_Y;

        if (blockY >= startY) return 0f;
        if (blockY <= maxY)   return 1f;

        float depth    = startY - blockY;
        float maxDepth = startY - maxY;
        return depth / maxDepth;
    }
}