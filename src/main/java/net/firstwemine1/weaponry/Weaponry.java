package net.firstwemine1.weaponry;

import com.mojang.logging.LogUtils;
import net.firstwemine1.weaponry.ability.Abilities;
import net.firstwemine1.weaponry.bow.ModBows;
import net.firstwemine1.weaponry.crossbow.ModCrossbows;
import net.firstwemine1.weaponry.effect.ModEffects;
import net.firstwemine1.weaponry.entity.ModEntities;
import net.firstwemine1.weaponry.item.ModComponents;
import net.firstwemine1.weaponry.item.ModItems;
import net.firstwemine1.weaponry.testing.TestChest;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

/**
 * NeoForge entry point: registries on the mod bus, abilities on the game bus. The melee hooks are the
 * same mixins as on Fabric ({@code weaponry.mixins.json}); NeoForge runs with Mojang names, so they apply as is.
 */
@Mod(Weaponry.MOD_ID)
public final class Weaponry {
	public static final String MOD_ID = "weaponry";
	public static final Logger LOGGER = LogUtils.getLogger();

	private static WeaponryConfig config;

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}

	public static WeaponryConfig config() {
		return config;
	}

	public Weaponry(IEventBus modBus) {
		config = WeaponryConfig.load();
		ModComponents.register(modBus);
		ModItems.register(modBus);
		ModBows.register(modBus);
		ModCrossbows.register(modBus);
		ModEntities.register(modBus);
		ModEffects.register(modBus);
		Abilities.register();

		if (config.testChest) {
			LOGGER.info("Test mode on (config/weaponry.json \"testChest\"): test chest will be placed at spawn");
			NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> TestChest.placeOrRefill(event.getServer()));
		}
	}
}
