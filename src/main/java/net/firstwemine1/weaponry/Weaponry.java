package net.firstwemine1.weaponry;

import net.firstwemine1.weaponry.ability.Abilities;
import net.firstwemine1.weaponry.effect.ModEffects;
import net.firstwemine1.weaponry.entity.ModEntities;
import net.firstwemine1.weaponry.item.ModComponents;
import net.firstwemine1.weaponry.item.ModItems;
import net.firstwemine1.weaponry.testing.TestChest;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Weaponry implements ModInitializer {
	public static final String MOD_ID = "weaponry";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static WeaponryConfig config;

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	public static WeaponryConfig config() {
		return config;
	}

	@Override
	public void onInitialize() {
		config = WeaponryConfig.load();
		ModComponents.register();
		ModItems.register();
		ModEntities.register();
		ModEffects.register();
		Abilities.register();
		LOGGER.info("Registered {} weapons", ModItems.all().size());

		if (config.testChest) {
			LOGGER.info("Test mode on (config/weaponry.json \"testChest\"): test chest will be placed at spawn");
			ServerLifecycleEvents.SERVER_STARTED.register(TestChest::placeOrRefill);
		}
	}
}
