package net.firstwemine1.weaponry;

import net.firstwemine1.weaponry.ability.Abilities;
import net.firstwemine1.weaponry.bow.ModBows;
import net.firstwemine1.weaponry.crossbow.ModCrossbows;
import net.firstwemine1.weaponry.effect.ModEffects;
import net.firstwemine1.weaponry.entity.ModEntities;
import net.firstwemine1.weaponry.item.ModItems;
import net.firstwemine1.weaponry.testing.TestChest;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Weaponry implements ModInitializer {
	public static final String MOD_ID = "weaponry";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static WeaponryConfig config;

	public static ResourceLocation id(String path) {
		return new ResourceLocation(MOD_ID, path);
	}

	public static WeaponryConfig config() {
		return config;
	}

	@Override
	public void onInitialize() {
		config = WeaponryConfig.load();
		ModItems.register();
		ModBows.register();
		ModCrossbows.register();
		ModEntities.register();
		ModEffects.register();
		Abilities.register();
		LOGGER.info("Registered {} weapons and {} bows", ModItems.all().size(), ModBows.all().size());

		if (config.testChest) {
			LOGGER.info("Test mode on (config/weaponry.json \"testChest\"): test chest will be placed at spawn");
			ServerLifecycleEvents.SERVER_STARTED.register(TestChest::placeOrRefill);
		}
	}
}
