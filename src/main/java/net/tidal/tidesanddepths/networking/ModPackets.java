package net.tidal.tidesanddepths.networking;

import net.minecraft.util.Identifier;
import net.tidal.tidesanddepths.TidesAndDepths;

public final class ModPackets {
    public static final Identifier START_MINIGAME = new Identifier(TidesAndDepths.MOD_ID, "start_fishing_minigame");
    public static final Identifier STOP_MINIGAME = new Identifier(TidesAndDepths.MOD_ID, "stop_fishing_minigame");
    public static final Identifier MINIGAME_RESULT = new Identifier(TidesAndDepths.MOD_ID, "fishing_minigame_result");

    private ModPackets() {
    }
}
