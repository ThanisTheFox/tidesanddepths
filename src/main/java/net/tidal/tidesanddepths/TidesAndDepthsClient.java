package net.tidal.tidesanddepths;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.tidal.tidesanddepths.client.hud.NarcosisOverlayRenderer;
import net.tidal.tidesanddepths.client.hud.PressureHudRenderer;
import net.tidal.tidesanddepths.client.hud.PressureScreenEffects;
import net.tidal.tidesanddepths.pressure.PressureEffects;
import net.tidal.tidesanddepths.pressure.PressureManager;

public class TidesAndDepthsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {


        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            PressureManager.tick(client.player);
            PressureEffects.tick(client.player);
        });

        HudRenderCallback.EVENT.register(PressureScreenEffects::render);
        HudRenderCallback.EVENT.register(NarcosisOverlayRenderer::render);
        HudRenderCallback.EVENT.register(PressureHudRenderer::render);


        System.out.println("Exploring the deep oceans...");

    }
}
