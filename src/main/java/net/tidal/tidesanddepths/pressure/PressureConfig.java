package net.tidal.tidesanddepths.pressure;

public final class PressureConfig {

    private PressureConfig() {}

    public static final int SEA_LEVEL        = 63;
    public static final int PRESSURE_START_Y = SEA_LEVEL - 10;   // Y = 53
    public static final int MAX_PRESSURE_Y   = 0;


    public static final float  MAX_PRESSURE_DISPLAY = 400f;
    public static final String PRESSURE_UNIT        = "atm";

    public static final boolean SMOOTH_PRESSURE = true;
    public static final float   BUILDUP_RATE    = 0.005f;
    public static final float   RELIEF_RATE     = 0.012f;

    public static final int     BAR_WIDTH  = 182;
    public static final int     BAR_HEIGHT = 7;


    public static final int     BAR_Y_OFFSET_FROM_BOTTOM = 75;

    public static final boolean SHOW_LABEL         = true;
    public static final boolean HIDE_WHEN_SURFACE  = true;

    public static final float THRESHOLD_LOW      = 0.33f;
    public static final float THRESHOLD_MEDIUM   = 0.66f;
    public static final float THRESHOLD_CRITICAL = 0.80f;

    public static final float SHAKE_THRESHOLD  = 0.80f;
    public static final float SHAKE_MAX_PIXELS = 3.5f;

    public static final boolean PULSE_ENABLED        = true;
    public static final float   PULSE_THRESHOLD      = 0.66f;
    public static final int     PULSE_PEAK_BRIGHTNESS= 55;
    public static final float   PULSE_SPEED          = 3.5f;

    public static final boolean VIGNETTE_ENABLED   = true;

    public static final float   VIGNETTE_THRESHOLD = 0.25f;

    public static final int     VIGNETTE_MAX_ALPHA = 210;

    public static final int     VIGNETTE_REACH     = 100;

    public static final int     VIGNETTE_STEPS     = 12;

    public static final float TINNITUS_THRESHOLD   = 0.40f;

    public static final int   TINNITUS_INTERVAL_MAX = 220;

    public static final int   TINNITUS_INTERVAL_MIN = 60;

    public static final float NARCOSIS_THRESHOLD    = 0.60f;

    public static final int   NARCOSIS_EPISODE_INTERVAL = 160;

    public static final int   NARCOSIS_EPISODE_DURATION =  80;

    public static final float HALLUCINATION_THRESHOLD   = 0.75f;

    public static final float DAMAGE_THRESHOLD     = 0.85f;

    public static final int   DAMAGE_INTERVAL_TICKS = 80;

    public static final float DAMAGE_BASE          = 1.0f;   // half a heart
}