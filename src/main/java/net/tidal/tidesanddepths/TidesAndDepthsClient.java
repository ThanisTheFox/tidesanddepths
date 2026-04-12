package net.tidal.tidesanddepths;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.tidal.tidesanddepths.client.hud.pressure.fishing.ClientFishingMinigame;

public class TidesAndDepthsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientFishingMinigame.registerPackets();
        ClientTickEvents.END_CLIENT_TICK.register(ClientFishingMinigame::tick);
        HudRenderCallback.EVENT.register(ClientFishingMinigame::render);
    }
}
