package net.tidal.tidesanddepths.client.hud;

import net.tidal.tidesanddepths.pressure.PressureConfig;
import net.tidal.tidesanddepths.pressure.PressureManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;


public final class PressureHudRenderer {


    private static final int C_OUTER_SHADOW  = 0xFF111111;
    private static final int C_INNER_HI      = 0x33FFFFFF;
    private static final int C_INNER_SHADOW  = 0x44000000;
    private static final int C_BG            = 0xFF0D1B2A;
    private static final int C_TICK          = 0x33FFFFFF;


    private static final int C_LOW      = 0xFF1A6FFF;   // ocean blue
    private static final int C_MID      = 0xFF00DDCC;   // teal / cyan
    private static final int C_HIGH     = 0xFFFFAA00;   // amber
    private static final int C_CRITICAL = 0xFFFF2200;   // danger red

    private static final int C_LABEL    = 0xFFDDDDDD;
    private static final int C_LABEL_DIM= 0xFF888888;

    private PressureHudRenderer() {}

    public static void render(DrawContext ctx, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        if (PressureConfig.HIDE_WHEN_SURFACE && !PressureManager.hasAnyPressure()) return;

        float pressure = PressureManager.getNormalized();

        int screenW = ctx.getScaledWindowWidth();
        int screenH = ctx.getScaledWindowHeight();

        int barW = PressureConfig.BAR_WIDTH;
        int barH = PressureConfig.BAR_HEIGHT;

        int baseX = (screenW - barW) / 2;
        int baseY = screenH - PressureConfig.BAR_Y_OFFSET_FROM_BOTTOM;

        int shakeX = Math.round(PressureManager.getShakeOffset(0));
        int shakeY = Math.round(PressureManager.getShakeOffset(1));
        int x = baseX + shakeX;
        int y = baseY + shakeY;

        int fillColor = computeFillColor(pressure);

        drawBar(ctx, x, y, barW, barH, pressure, fillColor);

        if (PressureConfig.SHOW_LABEL) {
            drawLabel(ctx, mc.textRenderer, x, y, barW, pressure);
        }
    }

    private static void drawBar(DrawContext ctx, int x, int y,
                                int barW, int barH, float pressure, int fillColor) {

        ctx.fill(x - 1, y - 1, x + barW + 1, y + barH + 1, C_OUTER_SHADOW);

        ctx.fill(x, y, x + barW, y + barH, C_BG);

        int fillW = Math.round(pressure * barW);
        if (fillW > 0) {
            ctx.fill(x, y, x + fillW, y + barH, fillColor);

            ctx.fill(x, y, x + fillW, y + 1, C_INNER_HI);

            ctx.fill(x, y + barH - 1, x + fillW, y + barH, C_INNER_SHADOW);

            if (fillW < barW) {
                int edgeColor = brighten(fillColor, 80);
                ctx.fill(x + fillW - 1, y, x + fillW, y + barH, edgeColor);
            }
        }

        for (int i = 1; i <= 3; i++) {
            int tickX = x + (barW * i / 4);
            ctx.fill(tickX, y + 1, tickX + 1, y + barH - 1, C_TICK);
        }
    }

    private static void drawLabel(DrawContext ctx, TextRenderer font,
                                  int barX, int barY, int barW, float pressure) {

        int blockY    = PressureManager.getPlayerBlockY();
        int depth     = PressureManager.getDepthBelowSurface();
        float display = PressureManager.getDisplayValue();

        String yPart  = "Y: " + blockY;
        String sep    = "  |  ";
        String atmPart = String.format("%.0f %s", display, PressureConfig.PRESSURE_UNIT);
        String full   = yPart + sep + atmPart;

        int textW = font.getWidth(full);
        int textX = barX + (barW - textW) / 2;
        int textY = barY - font.fontHeight - 2;

        ctx.fill(textX - 2, textY - 1,
                textX + textW + 2, textY + font.fontHeight + 1,
                0x66000000);

        int xCursor = textX;
        xCursor = drawColoured(ctx, font, yPart,  xCursor, textY, C_LABEL);
        xCursor = drawColoured(ctx, font, sep,     xCursor, textY, C_LABEL_DIM);
        xCursor = drawColoured(ctx, font, atmPart, xCursor, textY, C_LABEL_DIM);

        if (pressure >= PressureConfig.THRESHOLD_CRITICAL) {
            drawCriticalWarning(ctx, font, barX, barY, barW);
        }
    }

    private static void drawCriticalWarning(DrawContext ctx, TextRenderer font,
                                            int barX, int barY, int barW) {
        boolean visible = (System.currentTimeMillis() % 800) < 500;
        if (!visible) return;

        String warn = "! CRITICAL !";
        int warnX = barX + barW + 5;
        int warnY = barY + (PressureConfig.BAR_HEIGHT - font.fontHeight) / 2;

        // Make it pulse brightness using sin
        double phase = (System.currentTimeMillis() % 1000) / 1000.0 * Math.PI * 2;
        float bright = (float)(0.65 + 0.35 * Math.sin(phase));
        int r = Math.min(255, (int)(255 * bright));
        int color = 0xFF000000 | (r << 16);

        ctx.drawText(font, warn, warnX, warnY, color, true);
    }

    private static int computeFillColor(float pressure) {
        float low = PressureConfig.THRESHOLD_LOW;
        float mid = PressureConfig.THRESHOLD_MEDIUM;

        int base;
        if (pressure <= low) {
            base = lerpARGB(C_LOW, C_MID, pressure / low);
        } else if (pressure <= mid) {
            base = lerpARGB(C_MID, C_HIGH, (pressure - low) / (mid - low));
        } else {
            base = lerpARGB(C_HIGH, C_CRITICAL, (pressure - mid) / (1f - mid));
        }

        // Pulse brightening
        if (PressureConfig.PULSE_ENABLED && pressure >= PressureConfig.PULSE_THRESHOLD) {
            float pulseStrength = (pressure - PressureConfig.PULSE_THRESHOLD)
                    / (1f - PressureConfig.PULSE_THRESHOLD);

            double phase = System.currentTimeMillis() / 1000.0
                    * PressureConfig.PULSE_SPEED * Math.PI;
            float pulse  = (float)(0.5 + 0.5 * Math.sin(phase));

            int peakBrightness = (int)(pulse * pulseStrength * PressureConfig.PULSE_PEAK_BRIGHTNESS);
            base = brighten(base, peakBrightness);
        }

        return base;
    }

    private static int lerpARGB(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aA = (a >> 24) & 0xFF; int bA = (b >> 24) & 0xFF;
        int aR = (a >> 16) & 0xFF; int bR = (b >> 16) & 0xFF;
        int aG = (a >>  8) & 0xFF; int bG = (b >>  8) & 0xFF;
        int aB =  a        & 0xFF; int bB =  b        & 0xFF;
        return ((int)(aA + (bA - aA) * t) << 24)
                | ((int)(aR + (bR - aR) * t) << 16)
                | ((int)(aG + (bG - aG) * t) <<  8)
                |  (int)(aB + (bB - aB) * t);
    }

    private static int brighten(int color, int amount) {
        int r = Math.min(((color >> 16) & 0xFF) + amount, 255);
        int g = Math.min(((color >>  8) & 0xFF) + amount, 255);
        int b = Math.min(( color        & 0xFF) + amount, 255);
        return (color & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    private static int drawColoured(DrawContext ctx, TextRenderer font,
                                    String text, int x, int y, int color) {
        ctx.drawText(font, text, x, y, color, false);
        return x + font.getWidth(text);
    }
}