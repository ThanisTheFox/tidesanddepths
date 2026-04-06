package net.tidal.tidesanddepths;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TidesAndDepths implements ModInitializer {
	public static final String MOD_ID = "tidesanddepths";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Exploring the deep oceans...");
	}
}