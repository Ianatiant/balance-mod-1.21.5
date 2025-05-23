package net.ian.balmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.ian.balmod.command.BalCommandHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//Mod Main
public class Balancemod implements ModInitializer {
	public static final String MOD_ID = "balance-mod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			BalCommandHandler.register(dispatcher);
			LOGGER.info("[IanBank] Balance commands loaded successfully");
		});
	}
}