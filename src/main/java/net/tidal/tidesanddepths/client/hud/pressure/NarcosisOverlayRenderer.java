package net.tidal.tidesanddepths.client.hud.pressure;

import net.tidal.tidesanddepths.pressure.PressureConfig;
import net.tidal.tidesanddepths.pressure.PressureManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class NarcosisOverlayRenderer {

    private static final int TINT_R = 180;
    private static final int TINT_G = 210;
    private static final int TINT_B =  60;

    private static final int PHOSPHENE_COUNT = 7;

    private static final double PHI = 1.6180339887;

    private NarcosisOverlayRenderer() {}

    public static void render(DrawContext ctx, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;

        float pressure = PressureManager.getNormalized();
        if (pressure < PressureConfig.NARCOSIS_THRESHOLD) return;

        float intensity = (pressure - PressureConfig.NARCOSIS_THRESHOLD)
                / (1f - PressureConfig.NARCOSIS_THRESHOLD);

        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();

        drawTint(ctx, sw, sh, intensity);
        drawPhosphenes(ctx, sw, sh, intensity);
    }

    private static void drawTint(DrawContext ctx, int sw, int sh, float intensity) {
        double ms = System.currentTimeMillis();

        double pulseSpeed  = 0.6 + intensity * 1.2;
        double pulsePhase  = ms / 1000.0 * pulseSpeed * Math.PI;
        float  pulseAmount = (float)(0.5 + 0.5 * Math.sin(pulsePhase));

        int maxAlpha = (int)(intensity * intensity * 55);
        int alpha    = (int)(maxAlpha * (0.4f + 0.6f * pulseAmount));

        if (alpha < 2) return;

        int color = (alpha << 24) | (TINT_R << 16) | (TINT_G << 8) | TINT_B;
        ctx.fill(0, 0, sw, sh, color);

        if (intensity > 0.6f) {
            double phase2  = ms / 1000.0 * (pulseSpeed * 1.7 + 0.3) * Math.PI + 1.3;
            float  pulse2  = (float)(0.5 + 0.5 * Math.sin(phase2));
            int    alpha2  = (int)((intensity - 0.6f) / 0.4f * 20 * pulse2);
            if (alpha2 > 1) {
                // Slightly shifted hue for the second layer — more red/orange
                int c2 = (alpha2 << 24) | (210 << 16) | (140 << 8) | 20;
                ctx.fill(0, 0, sw, sh, c2);
            }
        }
    }

    private static void drawPhosphenes(DrawContext ctx, int sw, int sh, float intensity) {
        double ms    = System.currentTimeMillis();
        int    maxA  = (int)(intensity * 70);
        if (maxA < 4) return;

        for (int i = 0; i < PHOSPHENE_COUNT; i++) {

            double driftCycle = 12000.0 + i * 1800.0;
            double yFrac      = ((ms / driftCycle + i * (1.0 / PHOSPHENE_COUNT)) % 1.0);


            double xPhase = ms / 5000.0 + i * PHI;
            double xFrac  = 0.15 + 0.7 * (0.5 + 0.5 * Math.sin(xPhase));

            int dotX = (int)(sw * xFrac);
            int dotY = (int)(sh * (1.0 - yFrac));


            double edgeFade = Math.sin(yFrac * Math.PI);
            int alpha = (int)(maxA * edgeFade
                    * (0.5 + 0.5 * Math.sin(ms / 800.0 + i * 2.1)));

            if (alpha < 3) continue;


            int size = 2 + (i % 3);
            int col  = (alpha << 24) | 0xCCFFDD;

            ctx.fill(dotX - size, dotY - size, dotX + size, dotY + size, col);


            int coreAlpha = Math.min(255, alpha * 2);
            ctx.fill(dotX - 1, dotY - 1, dotX + 1, dotY + 1,
                    (coreAlpha << 24) | 0xFFFFFF);
        }
    }
}
