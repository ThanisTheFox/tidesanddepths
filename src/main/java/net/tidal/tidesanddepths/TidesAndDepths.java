package net.tidal.tidesanddepths;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.tidal.tidesanddepths.fishing.FishingMinigameManager;
import net.tidal.tidesanddepths.pressure.PressureDamageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TidesAndDepths implements ModInitializer {
	public static final String MOD_ID = "tidesanddepths";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		FishingMinigameManager.registerNetworking();

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (var player : server.getPlayerManager().getPlayerList()) {
				PressureDamageHandler.tick(player);
			}
		});

		LOGGER.info("Exploring the deep oceans...");
	}
}
