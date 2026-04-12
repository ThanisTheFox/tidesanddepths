package net.tidal.tidesanddepths.client.hud.pressure;

import net.tidal.tidesanddepths.pressure.PressureConfig;
import net.tidal.tidesanddepths.pressure.PressureManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class PressureScreenEffects {

    private static final float[] COL_OCEAN    = {  0f, 15f,  70f };  // deep blue
    private static final float[] COL_DANGER   = { 10f,  0f,  35f };  // dark purple
    private static final float[] COL_CRITICAL = { 80f,  0f,   0f };  // blood red

    private PressureScreenEffects() {}

    public static void render(DrawContext ctx, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        if (!PressureConfig.VIGNETTE_ENABLED) return;

        float pressure  = PressureManager.getNormalized();
        float threshold = PressureConfig.VIGNETTE_THRESHOLD;
        if (pressure <= threshold) return;

        float t = (pressure - threshold) / (1f - threshold);

        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();

        float[] rgb;
        if (t < 0.5f) {
            rgb = lerpRGB(COL_OCEAN, COL_DANGER, t * 2f);
        } else {
            rgb = lerpRGB(COL_DANGER, COL_CRITICAL, (t - 0.5f) * 2f);
        }
        int r = (int) rgb[0];
        int g = (int) rgb[1];
        int b = (int) rgb[2];

        int baseAlpha = (int)(t * t * PressureConfig.VIGNETTE_MAX_ALPHA);

        if (pressure >= PressureConfig.THRESHOLD_CRITICAL) {
            double phase = System.currentTimeMillis() / 1000.0 * 1.6 * Math.PI;
            float  pulse = (float)(0.70 + 0.30 * Math.sin(phase));
            baseAlpha = Math.min(255, (int)(baseAlpha * pulse));
        }

        int steps = PressureConfig.VIGNETTE_STEPS;
        int reach = PressureConfig.VIGNETTE_REACH;

        for (int i = 0; i < steps; i++) {
            float layerFrac = 1f - (float) i / steps;

            float alphaMult = layerFrac * layerFrac * layerFrac;
            int   alpha     = Math.max(0, (int)(baseAlpha * alphaMult));
            if (alpha < 2) continue;

            int color = (alpha << 24) | (r << 16) | (g << 8) | b;
            int inset = (int)(reach * (float) i / steps);

            ctx.fill(0, 0, sw, inset + (reach / steps), color);
            ctx.fill(0, sh - inset - (reach / steps), sw, sh, color);
            ctx.fill(0, 0, inset + (reach / steps), sh, color);
            ctx.fill(sw - inset - (reach / steps), 0, sw, sh, color);
        }

        if (t > 0.75f) {
            float centerIntensity = (t - 0.75f) / 0.25f;
            int centerAlpha = (int)(centerIntensity * centerIntensity * 35);
            if (centerAlpha > 1) {
                int centerColor = (centerAlpha << 24) | (r << 16) | (g << 8) | b;
                ctx.fill(0, 0, sw, sh, centerColor);
            }
        }
    }

    private static float[] lerpRGB(float[] a, float[] b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new float[]{
                a[0] + (b[0] - a[0]) * t,
                a[1] + (b[1] - a[1]) * t,
                a[2] + (b[2] - a[2]) * t
        };
    }
}